package com.example.ui.battle

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.firebase.MatchRoom
import com.example.game.audio.BgmTrack
import com.example.game.audio.GameAudioManager
import com.example.game.audio.SfxType
import com.example.game.engine.BattleEngine
import com.example.game.engine.EngineAudioEvent
import com.example.game.model.AppLanguage
import com.example.game.model.AudioSettingsState
import com.example.game.model.BattleHudState
import com.example.game.model.BattleRenderSnapshot
import com.example.game.model.Difficulty
import com.example.game.model.GameMode
import com.example.game.model.ItemType
import com.example.game.model.MapTheme
import com.example.game.model.TurnPhase
import com.example.game.model.WeaponType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BattleViewModel(application: Application) : AndroidViewModel(application) {

    val engine = BattleEngine()
    val audio = GameAudioManager(application)

    private val prefs = application.getSharedPreferences("cannon_game_prefs", Context.MODE_PRIVATE)

    private var lastPlayerNickname: String? = null
    private var lastPlayerHpBonus: Int = 0

    private val _hudState = MutableStateFlow(engine.buildHudState())
    val hudState: StateFlow<BattleHudState> = _hudState.asStateFlow()

    private val _renderSnapshot = MutableStateFlow(engine.buildRenderSnapshot())
    val renderSnapshot: StateFlow<BattleRenderSnapshot> = _renderSnapshot.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow(Difficulty.NORMAL)
    val selectedDifficulty: StateFlow<Difficulty> = _selectedDifficulty.asStateFlow()

    private val _selectedMap = MutableStateFlow(MapTheme.GRASSLAND)
    val selectedMap: StateFlow<MapTheme> = _selectedMap.asStateFlow()

    private val _settings = MutableStateFlow(
        AudioSettingsState(
            bgmEnabled = audio.bgmEnabled,
            sfxEnabled = audio.sfxEnabled,
            vibrationEnabled = audio.vibrationEnabled,
            language = if (prefs.getString("lang", "KO") == "EN") AppLanguage.EN else AppLanguage.KO
        )
    )
    val settings: StateFlow<AudioSettingsState> = _settings.asStateFlow()

    init {
        engine.setLanguage(_settings.value.language)
        audio.playBgm(BgmTrack.MENU)
    }

    fun selectDifficulty(difficulty: Difficulty) {
        audio.playSfx(SfxType.BUTTON)
        _selectedDifficulty.value = difficulty
    }

    fun selectMap(theme: MapTheme) {
        audio.playSfx(SfxType.BUTTON, pitch = 1.08f)
        _selectedMap.value = theme
        engine.previewMapTheme(theme)
        syncStates()
    }

    fun startMatch(
        mode: GameMode,
        playerNickname: String? = null,
        playerHpBonus: Int = 0
    ) {
        lastPlayerNickname = playerNickname
        lastPlayerHpBonus = playerHpBonus
        audio.playSfx(SfxType.BUTTON, pitch = 1.15f)
        engine.startNewMatch(
            mode = mode,
            diff = _selectedDifficulty.value,
            theme = _selectedMap.value,
            playerNickname = playerNickname,
            playerHpBonus = playerHpBonus
        )
        audio.playBgm(BgmTrack.BATTLE)
        syncStates()
    }

    fun startOnlineMatch(room: MatchRoom, localUid: String) {
        audio.playSfx(SfxType.BUTTON, pitch = 1.15f)
        val theme = room.parsedMapTheme()
        _selectedMap.value = theme
        engine.startOnlineMatch(
            seed = room.terrainSeed,
            theme = theme,
            hostName = room.hostName.ifBlank { "1P" },
            guestName = room.guestName.ifBlank { "2P" },
            localIsHost = room.hostUid == localUid,
            initialWindForce = room.windForce.toInt()
        )
        audio.playBgm(BgmTrack.BATTLE)
        syncStates()
    }

    fun applyRemoteTurn(room: MatchRoom) {
        engine.applyRemoteTurnAction(
            actionType = room.lastActionType,
            tankX = room.lastTankX.toFloat(),
            tankY = room.lastTankY.toFloat(),
            facingRight = room.lastFacingRight,
            aimAngleDeg = room.lastAimAngle.toFloat(),
            power = room.lastPower.toFloat(),
            weapon = room.parsedWeapon(),
            usedItem = room.parsedUsedItem(),
            nextWindForce = room.windForce.toInt()
        )
        processAudioEvents()
        syncStates()
    }

    fun syncOnlineWind(nextWindForce: Int) {
        engine.syncOnlineWind(nextWindForce)
        syncStates()
    }

    fun forceOnlineGameOver(winnerTankIndex: Int, reason: String? = null) {
        engine.forceOnlineGameOver(winnerTankIndex, reason)
        processAudioEvents()
        syncStates()
    }

    fun restartMatch() {
        audio.playSfx(SfxType.BUTTON)
        val current = _hudState.value
        if (current.gameMode == GameMode.ONLINE_PVP) return
        engine.startNewMatch(
            mode = current.gameMode,
            diff = current.difficulty,
            theme = current.mapTheme,
            playerNickname = lastPlayerNickname,
            playerHpBonus = lastPlayerHpBonus
        )
        audio.playBgm(BgmTrack.BATTLE)
        syncStates()
    }

    fun returnToTitle() {
        audio.playSfx(SfxType.BUTTON)
        engine.returnToTitle()
        audio.playBgm(BgmTrack.MENU)
        syncStates()
    }

    fun setMoveInput(dir: Int) {
        engine.setMoveInput(dir)
    }

    fun setAngleInput(dir: Int) {
        if (dir != 0) audio.playSfx(SfxType.ANGLE_TICK, volume = 0.45f)
        engine.setAngleInput(dir)
    }

    fun toggleFacing() {
        audio.playSfx(SfxType.BUTTON, volume = 0.6f, pitch = 1.2f)
        engine.toggleFacing()
        syncStates()
    }

    fun selectWeapon(weapon: WeaponType) {
        audio.playSfx(SfxType.BUTTON, volume = 0.7f, pitch = 1.1f)
        engine.selectWeapon(weapon)
        syncStates()
    }

    fun useItem(item: ItemType) {
        engine.useItem(item)
        processAudioEvents()
        syncStates()
    }

    fun startCharging() {
        engine.startCharging()
        syncStates()
    }

    fun releaseAndFire() {
        engine.releaseAndFire()
        processAudioEvents()
        syncStates()
    }

    fun panCameraBy(worldDx: Float) {
        engine.panCameraBy(worldDx)
        _renderSnapshot.value = engine.buildRenderSnapshot()
    }

    fun togglePause(paused: Boolean? = null) {
        audio.playSfx(SfxType.BUTTON, volume = 0.7f)
        engine.togglePause(paused)
        syncStates()
    }

    fun setShowTutorial(show: Boolean) {
        audio.playSfx(SfxType.BUTTON, volume = 0.65f)
        engine.setShowTutorial(show)
        syncStates()
    }

    fun setShowSettings(show: Boolean) {
        audio.playSfx(SfxType.BUTTON, volume = 0.65f)
        engine.setShowSettings(show)
        syncStates()
    }

    fun setBgmEnabled(enabled: Boolean) {
        audio.setBgmEnabled(enabled)
        _settings.value = _settings.value.copy(bgmEnabled = enabled)
        audio.playSfx(SfxType.BUTTON, volume = 0.65f)
    }

    fun setSfxEnabled(enabled: Boolean) {
        audio.setSfxEnabled(enabled)
        _settings.value = _settings.value.copy(sfxEnabled = enabled)
        if (enabled) audio.playSfx(SfxType.BUTTON, volume = 0.75f)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        audio.setVibrationEnabled(enabled)
        _settings.value = _settings.value.copy(vibrationEnabled = enabled)
        if (enabled) audio.vibrate(40L, 120)
        audio.playSfx(SfxType.BUTTON, volume = 0.65f)
    }

    fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString("lang", lang.name).apply()
        engine.setLanguage(lang)
        _settings.value = _settings.value.copy(language = lang)
        audio.playSfx(SfxType.BUTTON, volume = 0.7f)
    }

    fun onAppPause() {
        audio.onLifecyclePause()
        val state = _hudState.value
        if (state.turnPhase != TurnPhase.TITLE &&
            state.turnPhase != TurnPhase.GAME_OVER &&
            state.gameMode != GameMode.ONLINE_PVP
        ) {
            engine.togglePause(true)
            syncStates()
        }
    }

    fun onAppResume() {
        audio.onLifecycleResume()
    }

    fun tick(dt: Float) {
        engine.step(dt)
        processAudioEvents()
        syncStates()
    }

    private fun processAudioEvents() {
        val events = engine.consumeAudioEvents()
        for (event in events) {
            when (event) {
                is EngineAudioEvent.Fire -> {
                    audio.playSfx(if (event.heavy) SfxType.FIRE_POWER else SfxType.FIRE_STANDARD)
                    audio.vibrate(if (event.heavy) 75L else 45L, if (event.heavy) 210 else 140)
                }
                is EngineAudioEvent.WeaponImpact -> {
                    val sfx = when {
                        event.weapon == WeaponType.WATER -> SfxType.SPLASH
                        event.weapon == WeaponType.POOP -> SfxType.SPLAT
                        event.weapon == WeaponType.GAS -> SfxType.GAS_HISS
                        event.weapon == WeaponType.TRASH -> SfxType.TRASH_CLATTER
                        event.weapon == WeaponType.POWER -> SfxType.EXPLOSION_BIG
                        else -> SfxType.EXPLOSION_SMALL
                    }
                    val vol = if (event.isSubMunition) 0.55f else 0.92f
                    audio.playSfx(sfx, volume = vol)
                    if (event.weapon == WeaponType.TRASH && !event.isSubMunition) {
                        audio.playSfx(SfxType.EXPLOSION_SMALL, volume = 0.65f)
                    }
                    if (event.hitTank) {
                        audio.playSfx(SfxType.HIT_CLANK, volume = 0.78f)
                    }
                    val big = event.weapon == WeaponType.POWER
                    audio.vibrate(
                        durationMs = when {
                            event.isSubMunition -> 45L
                            big -> 180L
                            else -> 100L
                        },
                        amplitude = if (big) 255 else 185
                    )
                }
                EngineAudioEvent.Ricochet -> {
                    audio.playSfx(SfxType.BOING, volume = 0.88f)
                    audio.vibrate(40L, 140)
                }
                EngineAudioEvent.ItemUse -> {
                    audio.playSfx(SfxType.ITEM_USE)
                    audio.vibrate(35L, 110)
                }
                EngineAudioEvent.TurnStart -> {
                    audio.playSfx(SfxType.TURN_START, volume = 0.55f)
                }
                is EngineAudioEvent.GameOver -> {
                    audio.playBgm(BgmTrack.VICTORY)
                    audio.playSfx(if (event.playerWon) SfxType.VICTORY_FANFARE else SfxType.DEFEAT_TONE)
                }
            }
        }
    }

    private fun syncStates() {
        _renderSnapshot.value = engine.buildRenderSnapshot()
        _hudState.value = engine.buildHudState()
    }

    override fun onCleared() {
        super.onCleared()
        audio.release()
    }
}
