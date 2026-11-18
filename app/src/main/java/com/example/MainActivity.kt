package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.firebase.OnlineGameRepository
import com.example.firebase.OnlineUiState
import com.example.firebase.authStateFlow
import com.example.firebase.performGoogleSignOut
import com.example.game.model.AppLanguage
import com.example.game.model.GameMode
import com.example.game.model.TurnPhase
import com.example.ui.battle.BattleCanvas
import com.example.ui.battle.BattleHud
import com.example.ui.battle.BattleViewModel
import com.example.ui.battle.GameOverOverlay
import com.example.ui.battle.PauseOverlay
import com.example.ui.battle.SettingsOverlay
import com.example.ui.battle.TitleOverlay
import com.example.ui.battle.TutorialOverlay
import com.example.ui.online.AuthScreen
import com.example.ui.online.LobbyTab
import com.example.ui.online.OnlineLobbyOverlay
import com.example.ui.online.OnlineViewModel
import com.example.ui.online.WaitingRoomOverlay
import com.example.ui.theme.CannonBattleTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            CannonBattleTheme {
                CannonBattleRoot()
            }
        }
    }
}

@Composable
fun CannonBattleRoot() {
    val authFlow = remember { Firebase.auth.authStateFlow() }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    if (currentUser == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(currentUser!!.uid) {
            AuthenticatedCannonBattleScreen(currentUser = currentUser!!)
        }
    }
}

@Composable
fun AuthenticatedCannonBattleScreen(
    currentUser: FirebaseUser
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val battleViewModel: BattleViewModel = viewModel(key = "battle_${currentUser.uid}")
    val onlineViewModel: OnlineViewModel = viewModel(
        key = "online_${currentUser.uid}",
        factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: throw IllegalStateException("Application missing from ViewModel extras")
                OnlineViewModel(
                    repository = OnlineGameRepository(app),
                    currentUserId = currentUser.uid
                )
            }
        }
    )

    val fallbackNickname = remember(currentUser.uid, currentUser.displayName) {
        currentUser.displayName?.trim()?.takeIf { it.isNotBlank() }?.take(16) ?: "전차병"
    }

    val hudState by battleViewModel.hudState.collectAsStateWithLifecycle()
    val renderSnapshot by battleViewModel.renderSnapshot.collectAsStateWithLifecycle()
    val selectedDifficulty by battleViewModel.selectedDifficulty.collectAsStateWithLifecycle()
    val selectedMap by battleViewModel.selectedMap.collectAsStateWithLifecycle()
    val settings by battleViewModel.settings.collectAsStateWithLifecycle()

    val myProfileState by onlineViewModel.myProfileState.collectAsStateWithLifecycle()
    val leaderboardState by onlineViewModel.leaderboardState.collectAsStateWithLifecycle()
    val waitingRoomsState by onlineViewModel.waitingRoomsState.collectAsStateWithLifecycle()
    val incomingInvitesState by onlineViewModel.incomingInvitesState.collectAsStateWithLifecycle()
    val activeRoom by onlineViewModel.activeRoomState.collectAsStateWithLifecycle()
    val lastProgression by onlineViewModel.lastProgression.collectAsStateWithLifecycle()
    val onlineStatusMessage by onlineViewModel.statusMessage.collectAsStateWithLifecycle()
    val onlineBusy by onlineViewModel.isBusy.collectAsStateWithLifecycle()

    val myProfile = (myProfileState as? OnlineUiState.Success)?.data
    val pendingInviteCount = (incomingInvitesState as? OnlineUiState.Success)?.data?.size ?: 0

    var lobbyTabOpen by remember { mutableStateOf<LobbyTab?>(null) }
    var startedOnlineRoomId by remember { mutableStateOf<String?>(null) }
    var lastAppliedTurnSeq by remember { mutableLongStateOf(0L) }
    var matchRecordedForCurrentGame by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser.uid) {
        onlineViewModel.ensureProfile(fallbackNickname)
    }

    // BattleEngine -> Firestore 턴 전송 및 승패/경험치/레벨업 자동 기록 연결
    DisposableEffect(battleViewModel, onlineViewModel) {
        battleViewModel.engine.onOnlineTurnSubmitted = { submission ->
            onlineViewModel.submitOnlineTurn(submission)
        }
        battleViewModel.engine.onMatchEnded = { winnerId, localWon, damageDealt, mode, diff ->
            if (!matchRecordedForCurrentGame) {
                matchRecordedForCurrentGame = true
                if (mode == GameMode.ONLINE_PVP) {
                    onlineViewModel.finishOnlineMatchIfNeeded(winnerId)
                    onlineViewModel.recordMatchProgression(
                        won = localWon,
                        isOnline = true,
                        difficulty = diff,
                        damageDealt = damageDealt
                    )
                } else if (mode == GameMode.AI_VS_PLAYER) {
                    onlineViewModel.recordMatchProgression(
                        won = localWon,
                        isOnline = false,
                        difficulty = diff,
                        damageDealt = damageDealt
                    )
                }
            }
        }
        onDispose {
            battleViewModel.engine.onOnlineTurnSubmitted = null
            battleViewModel.engine.onMatchEnded = null
        }
    }

    // Firestore 대기실 -> 실시간 온라인 배틀 시작 · 상대 턴 반영 · 기권/종료 동기화
    LaunchedEffect(activeRoom) {
        val room = activeRoom ?: return@LaunchedEffect
        when (room.status) {
            "PLAYING" -> {
                if (startedOnlineRoomId != room.id) {
                    startedOnlineRoomId = room.id
                    lastAppliedTurnSeq = room.turnSeq
                    matchRecordedForCurrentGame = false
                    lobbyTabOpen = null
                    onlineViewModel.clearProgression()
                    battleViewModel.startOnlineMatch(room, currentUser.uid)
                } else if (room.turnSeq > lastAppliedTurnSeq) {
                    lastAppliedTurnSeq = room.turnSeq
                    if (room.lastActionUid != currentUser.uid && room.lastActionType != "NONE") {
                        battleViewModel.applyRemoteTurn(room)
                    } else {
                        battleViewModel.syncOnlineWind(room.windForce.toInt())
                    }
                }
            }
            "FINISHED" -> {
                if (startedOnlineRoomId == room.id && hudState.turnPhase != TurnPhase.GAME_OVER) {
                    val winnerIdx = if (room.winnerUid == room.hostUid) 0 else 1
                    val reason = if (room.lastActionType == "FORFEIT") {
                        if (settings.language == AppLanguage.KO) "상대가 기권했습니다!" else "Opponent forfeited!"
                    } else {
                        null
                    }
                    battleViewModel.forceOnlineGameOver(winnerIdx, reason)
                }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> battleViewModel.onAppPause()
                Lifecycle.Event.ON_RESUME -> battleViewModel.onAppResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        var lastNanos = withFrameNanos { it }
        while (true) {
            val nowNanos = withFrameNanos { it }
            val dt = (nowNanos - lastNanos) / 1_000_000_000f
            lastNanos = nowNanos
            battleViewModel.tick(dt)
        }
    }

    val isInWaitingRoom = activeRoom != null && activeRoom?.status == "WAITING"
    val isLobbyVisible = lobbyTabOpen != null && !isInWaitingRoom && hudState.turnPhase == TurnPhase.TITLE

    BackHandler(
        enabled = hudState.turnPhase != TurnPhase.TITLE ||
            hudState.showSettings ||
            hudState.showTutorial ||
            isInWaitingRoom ||
            isLobbyVisible
    ) {
        when {
            hudState.showSettings -> battleViewModel.setShowSettings(false)
            hudState.showTutorial -> battleViewModel.setShowTutorial(false)
            isInWaitingRoom -> {
                onlineViewModel.leaveOrCloseActiveRoom()
                lobbyTabOpen = LobbyTab.ROOMS
            }
            isLobbyVisible -> {
                lobbyTabOpen = null
                onlineViewModel.clearStatusMessage()
            }
            hudState.turnPhase == TurnPhase.GAME_OVER -> {
                startedOnlineRoomId = null
                onlineViewModel.clearActiveRoomLocal()
                onlineViewModel.clearProgression()
                battleViewModel.returnToTitle()
            }
            hudState.turnPhase != TurnPhase.TITLE -> battleViewModel.togglePause()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BattleCanvas(
            snapshot = renderSnapshot,
            terrain = battleViewModel.engine.terrain,
            onPanCamera = battleViewModel::panCameraBy
        )

        if (hudState.turnPhase != TurnPhase.TITLE && hudState.turnPhase != TurnPhase.GAME_OVER) {
            BattleHud(
                state = hudState,
                language = settings.language,
                onMoveInput = battleViewModel::setMoveInput,
                onAngleInput = battleViewModel::setAngleInput,
                onToggleFacing = battleViewModel::toggleFacing,
                onSelectWeapon = battleViewModel::selectWeapon,
                onUseItem = battleViewModel::useItem,
                onStartCharge = battleViewModel::startCharging,
                onReleaseFire = battleViewModel::releaseAndFire,
                onPauseClick = { battleViewModel.togglePause(true) }
            )
        }

        if (hudState.turnPhase == TurnPhase.TITLE && !isInWaitingRoom && !isLobbyVisible) {
            TitleOverlay(
                selectedDifficulty = selectedDifficulty,
                selectedMap = selectedMap,
                language = settings.language,
                profile = myProfile,
                pendingInviteCount = pendingInviteCount,
                onSelectDifficulty = battleViewModel::selectDifficulty,
                onSelectMap = battleViewModel::selectMap,
                onStartAiMatch = {
                    matchRecordedForCurrentGame = false
                    onlineViewModel.clearProgression()
                    battleViewModel.startMatch(
                        mode = GameMode.AI_VS_PLAYER,
                        playerNickname = myProfile?.nickname ?: fallbackNickname,
                        playerHpBonus = myProfile?.levelHpBonus ?: 0
                    )
                },
                onStartLocalMatch = {
                    matchRecordedForCurrentGame = false
                    onlineViewModel.clearProgression()
                    battleViewModel.startMatch(
                        mode = GameMode.LOCAL_2P,
                        playerNickname = myProfile?.nickname ?: fallbackNickname,
                        playerHpBonus = 0
                    )
                },
                onOpenOnlineLobby = {
                    onlineViewModel.clearStatusMessage()
                    lobbyTabOpen = LobbyTab.ROOMS
                },
                onOpenRankings = {
                    onlineViewModel.clearStatusMessage()
                    lobbyTabOpen = LobbyTab.RANKING
                },
                onSignOut = {
                    onlineViewModel.leaveOrCloseActiveRoom()
                    performGoogleSignOut(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                },
                onOpenTutorial = { battleViewModel.setShowTutorial(true) },
                onOpenSettings = { battleViewModel.setShowSettings(true) }
            )
        }

        if (isLobbyVisible) {
            OnlineLobbyOverlay(
                initialTab = lobbyTabOpen ?: LobbyTab.ROOMS,
                language = settings.language,
                currentUserId = currentUser.uid,
                myProfileState = myProfileState,
                leaderboardState = leaderboardState,
                waitingRoomsState = waitingRoomsState,
                incomingInvitesState = incomingInvitesState,
                isBusy = onlineBusy,
                statusMessage = onlineStatusMessage,
                onCreateRoom = { title, map ->
                    onlineViewModel.createRoom(title, map, fallbackNickname)
                },
                onJoinRoom = { roomId ->
                    onlineViewModel.joinRoomById(roomId, fallbackNickname)
                },
                onJoinByCode = { code ->
                    onlineViewModel.joinRoomByCode(code, fallbackNickname)
                },
                onAcceptInvite = { invite ->
                    onlineViewModel.acceptInvite(invite, fallbackNickname)
                },
                onDeclineInvite = { invite ->
                    onlineViewModel.declineInvite(invite)
                },
                onInvitePlayer = { target ->
                    onlineViewModel.invitePlayerToActiveOrNewRoom(
                        targetProfile = target,
                        fallbackName = fallbackNickname,
                        preferredMap = selectedMap
                    )
                },
                onUpdateNickname = { newName ->
                    onlineViewModel.updateNickname(newName)
                },
                onDismiss = {
                    lobbyTabOpen = null
                    onlineViewModel.clearStatusMessage()
                }
            )
        }

        if (isInWaitingRoom && activeRoom != null) {
            WaitingRoomOverlay(
                room = activeRoom!!,
                currentUserId = currentUser.uid,
                language = settings.language,
                leaderboardState = leaderboardState,
                isBusy = onlineBusy,
                statusMessage = onlineStatusMessage,
                onChangeMap = onlineViewModel::changeRoomMap,
                onToggleReady = onlineViewModel::toggleGuestReady,
                onStartMatch = onlineViewModel::startOnlineBattle,
                onInvitePlayer = { target ->
                    onlineViewModel.invitePlayerToActiveOrNewRoom(
                        targetProfile = target,
                        fallbackName = fallbackNickname,
                        preferredMap = activeRoom?.parsedMapTheme() ?: selectedMap
                    )
                },
                onLeaveRoom = {
                    onlineViewModel.leaveOrCloseActiveRoom()
                    lobbyTabOpen = LobbyTab.ROOMS
                }
            )
        }

        if (hudState.showTutorial) {
            TutorialOverlay(
                language = settings.language,
                onDismiss = { battleViewModel.setShowTutorial(false) }
            )
        }

        if (hudState.isPaused && !hudState.showSettings) {
            PauseOverlay(
                language = settings.language,
                isOnline = hudState.gameMode == GameMode.ONLINE_PVP,
                onResume = { battleViewModel.togglePause(false) },
                onRestart = {
                    matchRecordedForCurrentGame = false
                    onlineViewModel.clearProgression()
                    battleViewModel.restartMatch()
                },
                onOpenSettings = { battleViewModel.setShowSettings(true) },
                onMainMenu = {
                    if (hudState.gameMode == GameMode.ONLINE_PVP) {
                        onlineViewModel.leaveOrCloseActiveRoom()
                        startedOnlineRoomId = null
                    }
                    onlineViewModel.clearProgression()
                    battleViewModel.returnToTitle()
                }
            )
        }

        if (hudState.showSettings) {
            SettingsOverlay(
                settings = settings,
                onToggleBgm = battleViewModel::setBgmEnabled,
                onToggleSfx = battleViewModel::setSfxEnabled,
                onToggleVibration = battleViewModel::setVibrationEnabled,
                onSelectLanguage = battleViewModel::setLanguage,
                onDismiss = { battleViewModel.setShowSettings(false) }
            )
        }

        if (hudState.turnPhase == TurnPhase.GAME_OVER) {
            GameOverOverlay(
                state = hudState,
                language = settings.language,
                progression = lastProgression,
                onPlayAgain = {
                    matchRecordedForCurrentGame = false
                    onlineViewModel.clearProgression()
                    battleViewModel.restartMatch()
                },
                onBackToLobby = if (hudState.gameMode == GameMode.ONLINE_PVP) {
                    {
                        startedOnlineRoomId = null
                        onlineViewModel.clearActiveRoomLocal()
                        onlineViewModel.clearProgression()
                        battleViewModel.returnToTitle()
                        lobbyTabOpen = LobbyTab.ROOMS
                    }
                } else {
                    null
                },
                onMainMenu = {
                    startedOnlineRoomId = null
                    onlineViewModel.clearActiveRoomLocal()
                    onlineViewModel.clearProgression()
                    battleViewModel.returnToTitle()
                }
            )
        }
    }
}
