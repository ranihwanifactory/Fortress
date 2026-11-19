package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
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
import com.example.notifications.InviteNotificationManager
import com.example.ui.battle.BattleCanvas
import com.example.ui.battle.BattleHud
import com.example.ui.battle.BattleViewModel
import com.example.ui.battle.GameOverOverlay
import com.example.ui.battle.PauseOverlay
import com.example.ui.battle.SettingsOverlay
import com.example.ui.battle.TitleOverlay
import com.example.ui.battle.TutorialOverlay
import com.example.ui.online.AuthScreen
import com.example.ui.online.InvitePushBanner
import com.example.ui.online.LobbyTab
import com.example.ui.online.OnlineLobbyOverlay
import com.example.ui.online.OnlineViewModel
import com.example.ui.online.PushOptInPromptDialog
import com.example.ui.online.WaitingRoomOverlay
import com.example.ui.splash.SplashScreen
import com.example.ui.tank.TankSelectorOverlay
import com.example.ui.theme.CannonBattleTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {
    private val _openInvitesSignal = MutableStateFlow(0L)
    private val openInvitesSignal: StateFlow<Long> = _openInvitesSignal.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        InviteNotificationManager.ensureChannel(this)
        handleNotificationIntent(intent)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            CannonBattleTheme {
                CannonBattleRoot(openInvitesSignal = openInvitesSignal)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(InviteNotificationManager.EXTRA_OPEN_INVITES, false) == true) {
            intent.removeExtra(InviteNotificationManager.EXTRA_OPEN_INVITES)
            _openInvitesSignal.value = System.currentTimeMillis()
        }
    }
}

@Composable
fun CannonBattleRoot(
    openInvitesSignal: StateFlow<Long>
) {
    val authFlow = remember { Firebase.auth.authStateFlow() }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
    var showSplash by rememberSaveable { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (currentUser == null) {
            AuthScreen(onAuthSuccess = {})
        } else {
            key(currentUser!!.uid) {
                AuthenticatedCannonBattleScreen(
                    currentUser = currentUser!!,
                    showSplash = showSplash,
                    openInvitesSignal = openInvitesSignal
                )
            }
        }

        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(animationSpec = tween(durationMillis = 420))
        ) {
            SplashScreen(onFinished = { showSplash = false })
        }
    }
}

@Composable
fun AuthenticatedCannonBattleScreen(
    currentUser: FirebaseUser,
    showSplash: Boolean,
    openInvitesSignal: StateFlow<Long>
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
    val selectedTank by battleViewModel.selectedTank.collectAsStateWithLifecycle()
    val settings by battleViewModel.settings.collectAsStateWithLifecycle()

    val myProfileState by onlineViewModel.myProfileState.collectAsStateWithLifecycle()
    val leaderboardState by onlineViewModel.leaderboardState.collectAsStateWithLifecycle()
    val waitingRoomsState by onlineViewModel.waitingRoomsState.collectAsStateWithLifecycle()
    val incomingInvitesState by onlineViewModel.incomingInvitesState.collectAsStateWithLifecycle()
    val activeRoom by onlineViewModel.activeRoomState.collectAsStateWithLifecycle()
    val lastProgression by onlineViewModel.lastProgression.collectAsStateWithLifecycle()
    val onlineStatusMessage by onlineViewModel.statusMessage.collectAsStateWithLifecycle()
    val onlineBusy by onlineViewModel.isBusy.collectAsStateWithLifecycle()
    val latestPushInvite by onlineViewModel.latestPushInvite.collectAsStateWithLifecycle()
    val openInvitesTimestamp by openInvitesSignal.collectAsStateWithLifecycle()

    val myProfile = (myProfileState as? OnlineUiState.Success)?.data
    val pendingInviteCount = (incomingInvitesState as? OnlineUiState.Success)?.data?.size ?: 0

    var lobbyTabOpen by remember { mutableStateOf<LobbyTab?>(null) }
    var showTankSelector by remember { mutableStateOf(false) }
    var pushPermissionHint by remember { mutableStateOf<String?>(null) }
    var lastHandledInviteTapTs by remember { mutableLongStateOf(0L) }
    var startedOnlineRoomId by remember { mutableStateOf<String?>(null) }
    var lastAppliedTurnSeq by remember { mutableLongStateOf(0L) }
    var matchRecordedForCurrentGame by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            InviteNotificationManager.ensureChannel(context, settings.language)
            battleViewModel.setInvitePushEnabled(enabled = true, markPrompted = true)
            onlineViewModel.updateInvitePushEnabled(true)
            pushPermissionHint = null
        } else {
            battleViewModel.setInvitePushEnabled(enabled = false, markPrompted = true)
            onlineViewModel.updateInvitePushEnabled(false)
            pushPermissionHint = if (settings.language == AppLanguage.KO) {
                "기기 알림 권한이 거부되어 푸시 알림을 켤 수 없습니다. 시스템 설정에서 알림 권한을 허용해 주세요."
            } else {
                "Notification permission denied. Please enable it in Android Settings."
            }
        }
    }

    val handleToggleInvitePush: (Boolean) -> Unit = { enable ->
        if (!enable) {
            battleViewModel.setInvitePushEnabled(enabled = false, markPrompted = true)
            onlineViewModel.updateInvitePushEnabled(false)
            InviteNotificationManager.cancelAllInviteNotifications(context)
            onlineViewModel.dismissPushInvite()
            pushPermissionHint = null
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !InviteNotificationManager.hasRuntimeNotificationPermission(context)
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                InviteNotificationManager.ensureChannel(context, settings.language)
                battleViewModel.setInvitePushEnabled(enabled = true, markPrompted = true)
                onlineViewModel.updateInvitePushEnabled(true)
                pushPermissionHint = if (!InviteNotificationManager.canPostSystemNotification(context)) {
                    if (settings.language == AppLanguage.KO) {
                        "안드로이드 시스템 알림이 꺼져 있어 앱 내 배너로만 안내됩니다."
                    } else {
                        "System notifications are muted; in-app invite banners remain active."
                    }
                } else {
                    null
                }
            }
        }
    }

    LaunchedEffect(currentUser.uid) {
        onlineViewModel.ensureProfile(
            defaultNickname = fallbackNickname,
            initialPushEnabled = settings.invitePushEnabled
        )
    }

    // 알림 탭 시 초대함으로 즉시 이동
    LaunchedEffect(openInvitesTimestamp) {
        if (openInvitesTimestamp > 0L && openInvitesTimestamp != lastHandledInviteTapTs) {
            lastHandledInviteTapTs = openInvitesTimestamp
            if (hudState.turnPhase == TurnPhase.TITLE) {
                lobbyTabOpen = LobbyTab.INVITES
            }
        }
    }

    // 친구 초대 실시간 도착 시 -> 사용자가 푸시 알림을 켠 경우 시스템 푸시 + 알림음 발송
    LaunchedEffect(latestPushInvite?.id, settings.invitePushEnabled) {
        val invite = latestPushInvite ?: return@LaunchedEffect
        if (settings.invitePushEnabled) {
            InviteNotificationManager.showInviteNotification(
                context = context,
                invite = invite,
                language = settings.language
            )
            battleViewModel.playInviteAlertChime()
        }
    }

    LaunchedEffect(myProfile?.selectedTank) {
        myProfile?.let { profile ->
            battleViewModel.syncSelectedTankFromProfile(profile.parsedTankType())
        }
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
            showTankSelector ||
            isInWaitingRoom ||
            isLobbyVisible
    ) {
        when {
            showTankSelector -> showTankSelector = false
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
                selectedTank = selectedTank,
                language = settings.language,
                profile = myProfile,
                pendingInviteCount = pendingInviteCount,
                onSelectDifficulty = battleViewModel::selectDifficulty,
                onSelectMap = battleViewModel::selectMap,
                onOpenTankSelector = { showTankSelector = true },
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
                    InviteNotificationManager.cancelAllInviteNotifications(context)
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
                selectedTank = selectedTank,
                invitePushEnabled = settings.invitePushEnabled,
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
                    InviteNotificationManager.cancelInviteNotification(context, invite.id)
                    onlineViewModel.acceptInvite(invite, fallbackNickname)
                },
                onDeclineInvite = { invite ->
                    InviteNotificationManager.cancelInviteNotification(context, invite.id)
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
                onOpenTankSelector = { showTankSelector = true },
                onToggleInvitePush = handleToggleInvitePush,
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
                onOpenTankSelector = { showTankSelector = true },
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
                onBgmVolumeChange = battleViewModel::setBgmVolume,
                onToggleSfx = battleViewModel::setSfxEnabled,
                onToggleVibration = battleViewModel::setVibrationEnabled,
                onToggleInvitePush = handleToggleInvitePush,
                pushPermissionHint = pushPermissionHint,
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

        if (showTankSelector) {
            TankSelectorOverlay(
                currentTank = selectedTank,
                language = settings.language,
                onSelectTank = { tank ->
                    battleViewModel.selectTank(tank)
                    onlineViewModel.updateSelectedTank(tank)
                },
                onDismiss = { showTankSelector = false }
            )
        }

        // 최초 로그인 시 1회성 친구 초대 푸시 알림 수신 선택 다이얼로그
        if (!showSplash &&
            !settings.pushOptInPrompted &&
            hudState.turnPhase == TurnPhase.TITLE &&
            !isInWaitingRoom &&
            !isLobbyVisible &&
            !showTankSelector &&
            !hudState.showSettings &&
            !hudState.showTutorial
        ) {
            PushOptInPromptDialog(
                language = settings.language,
                onAllowPush = { handleToggleInvitePush(true) },
                onDeclinePush = { handleToggleInvitePush(false) }
            )
        }

        // 실시간 친구 초대 상단 푸시 배너 (사용자가 푸시 알림을 켠 경우에만 표시)
        InvitePushBanner(
            invite = if (settings.invitePushEnabled) latestPushInvite else null,
            language = settings.language,
            isBusy = onlineBusy,
            onAccept = { invite ->
                InviteNotificationManager.cancelInviteNotification(context, invite.id)
                if (hudState.turnPhase != TurnPhase.TITLE) {
                    startedOnlineRoomId = null
                    battleViewModel.returnToTitle()
                }
                lobbyTabOpen = null
                onlineViewModel.acceptInvite(invite, fallbackNickname)
            },
            onDecline = { invite ->
                InviteNotificationManager.cancelInviteNotification(context, invite.id)
                onlineViewModel.declineInvite(invite)
            },
            onDismiss = { onlineViewModel.dismissPushInvite() },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
