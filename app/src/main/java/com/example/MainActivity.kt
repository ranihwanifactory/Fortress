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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.example.ui.theme.CannonBattleTheme

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
                CannonBattleScreen()
            }
        }
    }
}

@Composable
fun CannonBattleScreen(
    viewModel: BattleViewModel = viewModel()
) {
    val hudState by viewModel.hudState.collectAsStateWithLifecycle()
    val renderSnapshot by viewModel.renderSnapshot.collectAsStateWithLifecycle()
    val selectedDifficulty by viewModel.selectedDifficulty.collectAsStateWithLifecycle()
    val selectedMap by viewModel.selectedMap.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.onAppPause()
                Lifecycle.Event.ON_RESUME -> viewModel.onAppResume()
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
            viewModel.tick(dt)
        }
    }

    BackHandler(enabled = hudState.turnPhase != TurnPhase.TITLE || hudState.showSettings || hudState.showTutorial) {
        when {
            hudState.showSettings -> viewModel.setShowSettings(false)
            hudState.showTutorial -> viewModel.setShowTutorial(false)
            hudState.turnPhase == TurnPhase.GAME_OVER -> viewModel.returnToTitle()
            hudState.turnPhase != TurnPhase.TITLE -> viewModel.togglePause()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BattleCanvas(
            snapshot = renderSnapshot,
            terrain = viewModel.engine.terrain,
            onPanCamera = viewModel::panCameraBy
        )

        if (hudState.turnPhase != TurnPhase.TITLE && hudState.turnPhase != TurnPhase.GAME_OVER) {
            BattleHud(
                state = hudState,
                language = settings.language,
                onMoveInput = viewModel::setMoveInput,
                onAngleInput = viewModel::setAngleInput,
                onToggleFacing = viewModel::toggleFacing,
                onSelectWeapon = viewModel::selectWeapon,
                onUseItem = viewModel::useItem,
                onStartCharge = viewModel::startCharging,
                onReleaseFire = viewModel::releaseAndFire,
                onPauseClick = { viewModel.togglePause(true) }
            )
        }

        if (hudState.turnPhase == TurnPhase.TITLE) {
            TitleOverlay(
                selectedDifficulty = selectedDifficulty,
                selectedMap = selectedMap,
                language = settings.language,
                onSelectDifficulty = viewModel::selectDifficulty,
                onSelectMap = viewModel::selectMap,
                onStartAiMatch = { viewModel.startMatch(GameMode.AI_VS_PLAYER) },
                onStartLocalMatch = { viewModel.startMatch(GameMode.LOCAL_2P) },
                onOpenTutorial = { viewModel.setShowTutorial(true) },
                onOpenSettings = { viewModel.setShowSettings(true) }
            )
        }

        if (hudState.showTutorial) {
            TutorialOverlay(
                language = settings.language,
                onDismiss = { viewModel.setShowTutorial(false) }
            )
        }

        if (hudState.isPaused && !hudState.showSettings) {
            PauseOverlay(
                language = settings.language,
                onResume = { viewModel.togglePause(false) },
                onRestart = viewModel::restartMatch,
                onOpenSettings = { viewModel.setShowSettings(true) },
                onMainMenu = viewModel::returnToTitle
            )
        }

        if (hudState.showSettings) {
            SettingsOverlay(
                settings = settings,
                onToggleBgm = viewModel::setBgmEnabled,
                onToggleSfx = viewModel::setSfxEnabled,
                onToggleVibration = viewModel::setVibrationEnabled,
                onSelectLanguage = viewModel::setLanguage,
                onDismiss = { viewModel.setShowSettings(false) }
            )
        }

        if (hudState.turnPhase == TurnPhase.GAME_OVER) {
            GameOverOverlay(
                state = hudState,
                language = settings.language,
                onPlayAgain = viewModel::restartMatch,
                onMainMenu = viewModel::returnToTitle
            )
        }
    }
}
