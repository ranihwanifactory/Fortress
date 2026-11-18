package com.example.game.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.game.model.AppLanguage
import com.example.game.model.BattleHudState
import com.example.game.model.BattlePropState
import com.example.game.model.BattleRenderSnapshot
import com.example.game.model.DamagePopup
import com.example.game.model.Difficulty
import com.example.game.model.ExplosionEffect
import com.example.game.model.GameMode
import com.example.game.model.GasCloudZone
import com.example.game.model.ItemType
import com.example.game.model.MapTheme
import com.example.game.model.ParticleState
import com.example.game.model.ProjectileState
import com.example.game.model.PropType
import com.example.game.model.SceneryKind
import com.example.game.model.SceneryProp
import com.example.game.model.TankExpression
import com.example.game.model.TankState
import com.example.game.model.TurnPhase
import com.example.game.model.WeaponType
import com.example.game.model.WeatherParticle
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EnemyCoral
import com.example.ui.theme.EnemyCoralLight
import com.example.ui.theme.PlayerBlue
import com.example.ui.theme.PlayerBlueLight
import com.example.ui.theme.PrimaryGold
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

sealed interface EngineAudioEvent {
    data class Fire(val heavy: Boolean) : EngineAudioEvent
    data class WeaponImpact(val weapon: WeaponType, val hitTank: Boolean, val isSubMunition: Boolean = false) : EngineAudioEvent
    data object Ricochet : EngineAudioEvent
    data object ItemUse : EngineAudioEvent
    data object TurnStart : EngineAudioEvent
    data class GameOver(val playerWon: Boolean) : EngineAudioEvent
}

/**
 * 7종 무기 + 파괴 가능한 성벽/공중발판 + 인터랙티브 지형지물(TNT 화약통, 보급 상자, 도탄 범퍼, 상승 기류)을 지원하는 전투 엔진.
 */
class BattleEngine {

    val terrain = DestructibleTerrain()

    private var gameMode = GameMode.AI_VS_PLAYER
    private var difficulty = Difficulty.NORMAL
    private var mapTheme = MapTheme.GRASSLAND
    private var language = AppLanguage.KO
    private var turnPhase = TurnPhase.TITLE
    private var isPaused = false
    private var showTutorial = false
    private var showSettings = false

    private var activeTankIndex = 0
    private var turnNumber = 1
    private var windForce = 0
    private var turnTimeRemaining = TURN_DURATION_SEC
    private var selectedWeapon = WeaponType.STANDARD
    private var winnerId: Int? = null
    private var statusBanner: String? = null

    private var moveDirInput = 0
    private var angleDirInput = 0
    private var isChargingInput = false
    private var chargingPower = 0f
    private var chargeIncreasing = true

    private var pendingBurstShots = 0
    private var burstTimer = 0f
    private var burstAngleRad = 0f
    private var burstSpeed = 0f
    private var burstWeapon = WeaponType.STANDARD
    private var burstOwnerId = 0

    private val tanks = mutableListOf<TankState>()
    private val projectiles = mutableListOf<ProjectileState>()
    private val gasClouds = mutableListOf<GasCloudZone>()
    private val props = mutableListOf<BattlePropState>()
    private val sceneryProps = mutableListOf<SceneryProp>()
    private val explosions = mutableListOf<ExplosionEffect>()
    private val particles = mutableListOf<ParticleState>()
    private val weatherParticles = mutableListOf<WeatherParticle>()
    private val popups = mutableListOf<DamagePopup>()
    private val pendingAudioEvents = mutableListOf<EngineAudioEvent>()

    private var nextProjectileId = 1
    private var nextGasId = 1
    private var nextPropId = 1
    private var nextSceneryId = 1
    private var nextPopupId = 1

    private var animTime = 0f
    private var stinkParticleTimer = 0f
    private var cameraX = 400f
    private var cameraY = 480f
    private var manualPanTimer = 0f
    private var shakeTimer = 0f
    private var shakeIntensity = 0f
    private var shakeOffsetX = 0f
    private var shakeOffsetY = 0f
    private var postImpactTimer = 0f
    private var muzzleFlashTimer = 0f
    private var muzzleFlashPos = Offset.Zero
    private var fireworkTimer = 0f

    private var aiStateTimer = 0f
    private var aiTargetAngleDeg = 45f
    private var aiTargetPower = 0.6f
    private var aiTargetWeapon = WeaponType.STANDARD
    private var aiDesiredMoveDir = 0
    private var aiMoveDuration = 0f
    private var aiHasPlanned = false

    init {
        terrain.generate(MapTheme.GRASSLAND)
        setupTanks(GameMode.AI_VS_PLAYER)
        setupBattlefieldProps(MapTheme.GRASSLAND)
        initWeatherParticles(MapTheme.GRASSLAND)
    }

    fun setLanguage(lang: AppLanguage) {
        language = lang
    }

    fun previewMapTheme(theme: MapTheme) {
        if (turnPhase == TurnPhase.TITLE && mapTheme != theme) {
            mapTheme = theme
            terrain.generate(theme, System.currentTimeMillis())
            setupTanks(gameMode)
            setupBattlefieldProps(theme)
            initWeatherParticles(theme)
        } else {
            mapTheme = theme
        }
    }

    fun startNewMatch(mode: GameMode, diff: Difficulty, theme: MapTheme = mapTheme) {
        gameMode = mode
        difficulty = diff
        mapTheme = theme
        isPaused = false
        showTutorial = false
        showSettings = false
        winnerId = null
        turnNumber = 1
        activeTankIndex = 0
        selectedWeapon = WeaponType.STANDARD
        projectiles.clear()
        gasClouds.clear()
        explosions.clear()
        particles.clear()
        popups.clear()
        pendingAudioEvents.clear()
        pendingBurstShots = 0
        moveDirInput = 0
        angleDirInput = 0
        isChargingInput = false
        chargingPower = 0f
        manualPanTimer = 0f
        shakeTimer = 0f
        fireworkTimer = 0f

        terrain.generate(theme, System.currentTimeMillis())
        setupTanks(mode)
        setupBattlefieldProps(theme)
        initWeatherParticles(theme)

        cameraX = tanks[0].x
        cameraY = tanks[0].y - 80f
        beginTurn(0, isFirstTurn = true)
    }

    private fun setupTanks(mode: GameMode) {
        tanks.clear()
        val p1X = 270f + Random.nextFloat() * 130f
        val p2X = terrain.worldWidth - (270f + Random.nextFloat() * 130f)
        val p1Y = terrain.findTopSurfaceY(p1X)
        val p2Y = terrain.findTopSurfaceY(p2X)

        tanks.add(
            TankState(
                id = 0,
                name = if (mode == GameMode.AI_VS_PLAYER) {
                    if (language == AppLanguage.KO) "블루 캡틴" else "Blue Captain"
                } else "1P Blue",
                isAi = false,
                x = p1X,
                y = p1Y,
                slopeAngleDeg = terrain.computeSlopeAngleDeg(p1X, p1Y),
                aimAngleDeg = 45f,
                facingRight = true,
                primaryColor = PlayerBlue,
                accentColor = PlayerBlueLight
            )
        )
        tanks.add(
            TankState(
                id = 1,
                name = if (mode == GameMode.AI_VS_PLAYER) {
                    if (language == AppLanguage.KO) "레드 불독 (AI)" else "Red Bulldog"
                } else "2P Red",
                isAi = (mode == GameMode.AI_VS_PLAYER),
                x = p2X,
                y = p2Y,
                slopeAngleDeg = terrain.computeSlopeAngleDeg(p2X, p2Y),
                aimAngleDeg = 45f,
                facingRight = false,
                primaryColor = EnemyCoral,
                accentColor = EnemyCoralLight
            )
        )
    }

    private fun setupBattlefieldProps(theme: MapTheme) {
        props.clear()
        sceneryProps.clear()

        // 1) TNT 폭발 화약통 2개 (양측 언덕 중간 배치)
        listOf(640f + Random.nextFloat() * 50f, 1520f + Random.nextFloat() * 50f).forEach { px ->
            val py = terrain.findTopSurfaceY(px)
            props.add(
                BattlePropState(
                    id = nextPropId++,
                    type = PropType.TNT_BARREL,
                    x = px,
                    y = py,
                    radius = 22f,
                    width = 36f,
                    height = 44f
                )
            )
        }

        // 2) 전술 보급 상자 2개 (부유 발판 위 또는 언덕에 배치)
        listOf(770f, 1430f).forEach { px ->
            val py = terrain.findTopSurfaceY(px)
            props.add(
                BattlePropState(
                    id = nextPropId++,
                    type = PropType.SUPPLY_CRATE,
                    x = px,
                    y = py,
                    radius = 22f,
                    width = 38f,
                    height = 38f
                )
            )
        }

        // 3) 공중 도탄 범퍼 2개 (포탄을 튕겨내어 트릭샷 가능)
        listOf(880f, 1320f).forEach { bx ->
            val surface = terrain.findTopSurfaceY(bx)
            props.add(
                BattlePropState(
                    id = nextPropId++,
                    type = PropType.RICOCHET_BUMPER,
                    x = bx,
                    y = (surface - 210f).coerceIn(180f, 430f),
                    radius = 28f,
                    width = 56f,
                    height = 56f
                )
            )
        }

        // 4) 상승 기류 간헐천 1개 (중앙 근처 지면에서 위로 솟구치는 바람 기둥)
        val ventX = if (Random.nextBoolean()) 955f else 1245f
        val ventY = terrain.findTopSurfaceY(ventX)
        props.add(
            BattlePropState(
                id = nextPropId++,
                type = PropType.UPDRAFT_VENT,
                x = ventX,
                y = ventY,
                radius = 36f,
                width = 76f,
                height = 380f
            )
        )

        // 5) 테마별 장식 지형지물 (나무, 선인장, 소나무, 수정, 야자수, 우주 안테나, 깃발)
        val sceneryXs = listOf(160f, 490f, 710f, 840f, 1020f, 1180f, 1360f, 1620f, 1990f)
        sceneryXs.forEachIndexed { idx, sx ->
            if (tanks.any { abs(it.x - sx) < 75f }) return@forEachIndexed
            val sy = terrain.findTopSurfaceY(sx)
            val kind = when (theme) {
                MapTheme.GRASSLAND -> if (idx % 3 == 0) SceneryKind.BATTLE_FLAG else SceneryKind.OAK_TREE
                MapTheme.DESERT -> SceneryKind.CACTUS
                MapTheme.SNOW -> SceneryKind.PINE_TREE
                MapTheme.VOLCANO -> SceneryKind.CRYSTAL
                MapTheme.ISLAND -> SceneryKind.PALM_TREE
                MapTheme.SPACE -> if (idx % 2 == 0) SceneryKind.SPACE_BEACON else SceneryKind.CRYSTAL
            }
            sceneryProps.add(
                SceneryProp(
                    id = nextSceneryId++,
                    x = sx,
                    y = sy,
                    kind = kind,
                    scale = 0.85f + (idx % 3) * 0.14f
                )
            )
        }
    }

    private fun initWeatherParticles(theme: MapTheme) {
        weatherParticles.clear()
        val count = when (theme) {
            MapTheme.SNOW -> 65
            MapTheme.DESERT -> 50
            MapTheme.VOLCANO -> 45
            MapTheme.SPACE -> 70
            MapTheme.GRASSLAND -> 28
            MapTheme.ISLAND -> 32
        }
        repeat(count) {
            weatherParticles.add(
                WeatherParticle(
                    x = Random.nextFloat() * terrain.worldWidth,
                    y = Random.nextFloat() * terrain.worldHeight,
                    vx = (Random.nextFloat() - 0.5f) * 35f,
                    vy = when (theme) {
                        MapTheme.VOLCANO -> -(25f + Random.nextFloat() * 45f)
                        MapTheme.SPACE -> 0f
                        else -> 24f + Random.nextFloat() * 36f
                    },
                    size = when (theme) {
                        MapTheme.SPACE -> 1.5f + Random.nextFloat() * 2.5f
                        MapTheme.SNOW -> 3f + Random.nextFloat() * 4f
                        else -> 2.5f + Random.nextFloat() * 3f
                    },
                    alpha = 0.35f + Random.nextFloat() * 0.55f
                )
            )
        }
    }

    private fun beginTurn(tankIdx: Int, isFirstTurn: Boolean = false) {
        activeTankIndex = tankIdx
        turnTimeRemaining = TURN_DURATION_SEC
        windForce = Random.nextInt(-10, 11)
        chargingPower = 0f
        isChargingInput = false
        moveDirInput = 0
        angleDirInput = 0
        manualPanTimer = 0f

        var t = tanks[tankIdx]
        val updatedCooldowns = t.weaponCooldowns.mapValues { (_, cd) -> max(0, cd - 1) }
            .filterValues { it > 0 }
        t = t.copy(
            fuel = t.maxFuel,
            weaponCooldowns = updatedCooldowns,
            expression = if (t.dizzyTimer > 0f) TankExpression.DIZZY else TankExpression.NORMAL
        )

        if (t.poisonTurns > 0 && t.isAlive) {
            val poisonDmg = 8
            val newHp = max(0, t.hp - poisonDmg)
            t = t.copy(
                hp = newHp,
                poisonTurns = t.poisonTurns - 1,
                expression = TankExpression.DIZZY,
                dizzyTimer = 1.0f
            )
            spawnPopup(
                t.x,
                t.y - 62f,
                if (language == AppLanguage.KO) "악취! -$poisonDmg" else "Stink! -$poisonDmg",
                Color(0xFF8D5524)
            )
            if (newHp <= 0) {
                tanks[tankIdx] = t
                killTank(tankIdx, if (language == AppLanguage.KO) "악취 기절!" else "Knocked Out!")
                return
            }
        }

        if (t.isAlive) {
            val inGas = gasClouds.any { hypot(t.x - it.x, (t.y - 18f) - it.y) <= it.radius + 18f }
            if (inGas) {
                val gasDmg = 10
                val newHp = max(0, t.hp - gasDmg)
                t = t.copy(
                    hp = newHp,
                    expression = TankExpression.DIZZY,
                    dizzyTimer = 1.0f
                )
                spawnPopup(
                    t.x,
                    t.y - 80f,
                    if (language == AppLanguage.KO) "독가스! -$gasDmg" else "Gas! -$gasDmg",
                    Color(0xFF70E000)
                )
                if (newHp <= 0) {
                    tanks[tankIdx] = t
                    killTank(tankIdx, if (language == AppLanguage.KO) "독가스 질식!" else "Gassed!")
                    return
                }
            }
        }

        if (!isFirstTurn) {
            val gasIt = gasClouds.listIterator()
            while (gasIt.hasNext()) {
                val g = gasIt.next()
                val nextTurns = g.turnsRemaining - 1
                if (nextTurns <= 0) gasIt.remove() else gasIt.set(g.copy(turnsRemaining = nextTurns))
            }
            turnNumber++
            pendingAudioEvents.add(EngineAudioEvent.TurnStart)
        }

        tanks[tankIdx] = t
        selectedWeapon = WeaponType.STANDARD

        if (t.isAi) {
            turnPhase = TurnPhase.AI_THINKING
            aiStateTimer = 0f
            aiHasPlanned = false
            statusBanner = if (language == AppLanguage.KO) "AI 조준 중…" else "AI Aiming…"
        } else {
            turnPhase = TurnPhase.PLAYER_AIMING
            statusBanner = when {
                gameMode == GameMode.LOCAL_2P -> if (language == AppLanguage.KO) "${t.name} 턴!" else "${t.name}'s Turn!"
                language == AppLanguage.KO -> "내 턴! 지형지물과 무기를 활용하세요"
                else -> "Your Turn! Use Terrain Props & Fire"
            }
        }
    }

    // --- 입력 제어 ---

    fun setMoveInput(dir: Int) {
        if (turnPhase == TurnPhase.PLAYER_AIMING && !isPaused) {
            moveDirInput = dir
            if (dir != 0) manualPanTimer = 0f
        } else {
            moveDirInput = 0
        }
    }

    fun setAngleInput(dir: Int) {
        if (turnPhase == TurnPhase.PLAYER_AIMING && !isPaused) {
            angleDirInput = dir
            if (dir != 0) manualPanTimer = 0f
        } else {
            angleDirInput = 0
        }
    }

    fun toggleFacing() {
        if (turnPhase != TurnPhase.PLAYER_AIMING || isPaused) return
        val t = tanks[activeTankIndex]
        tanks[activeTankIndex] = t.copy(facingRight = !t.facingRight)
        manualPanTimer = 0f
    }

    fun selectWeapon(weapon: WeaponType) {
        if (turnPhase != TurnPhase.PLAYER_AIMING || isPaused) return
        val t = tanks[activeTankIndex]
        if ((t.weaponCooldowns[weapon] ?: 0) == 0) {
            selectedWeapon = weapon
        }
    }

    fun useItem(item: ItemType) {
        if (turnPhase != TurnPhase.PLAYER_AIMING || isPaused) return
        val idx = activeTankIndex
        val t = tanks[idx]
        if (t.isAi || item in t.usedItems) return

        val updatedUsed = t.usedItems + item
        when (item) {
            ItemType.HEAL -> {
                val healed = min(t.maxHp, t.hp + 30)
                tanks[idx] = t.copy(hp = healed, usedItems = updatedUsed)
                spawnPopup(t.x, t.y - 54f, "+30 HP", SuccessGreen)
            }
            ItemType.FUEL_BOOST -> {
                tanks[idx] = t.copy(fuel = t.maxFuel, usedItems = updatedUsed)
                spawnPopup(t.x, t.y - 54f, if (language == AppLanguage.KO) "연료 완충!" else "Full Fuel!", Color(0xFF4CC9F0))
            }
            ItemType.SHIELD -> {
                tanks[idx] = t.copy(hasShield = true, usedItems = updatedUsed)
                spawnPopup(t.x, t.y - 54f, if (language == AppLanguage.KO) "방어막 ON!" else "Shield ON!", Color(0xFFA0C4FF))
            }
        }
        pendingAudioEvents.add(EngineAudioEvent.ItemUse)
    }

    fun startCharging() {
        if (turnPhase != TurnPhase.PLAYER_AIMING || isPaused) return
        val t = tanks[activeTankIndex]
        if (t.isAi) return
        moveDirInput = 0
        angleDirInput = 0
        manualPanTimer = 0f
        isChargingInput = true
        chargingPower = 0.08f
        chargeIncreasing = true
        tanks[activeTankIndex] = t.copy(expression = TankExpression.CHARGING)
        turnPhase = TurnPhase.CHARGING
    }

    fun releaseAndFire() {
        if (turnPhase != TurnPhase.CHARGING) return
        isChargingInput = false
        val power = chargingPower.coerceIn(0.08f, 1.0f)
        val t = tanks[activeTankIndex]
        tanks[activeTankIndex] = t.copy(expression = TankExpression.NORMAL)
        fireActiveTank(power, selectedWeapon)
    }

    fun panCameraBy(worldDx: Float) {
        if (turnPhase == TurnPhase.PLAYER_AIMING && !isPaused) {
            cameraX = (cameraX + worldDx).coerceIn(220f, terrain.worldWidth - 220f)
            manualPanTimer = 2.5f
        }
    }

    fun togglePause(paused: Boolean? = null) {
        if (turnPhase == TurnPhase.TITLE || turnPhase == TurnPhase.GAME_OVER) return
        isPaused = paused ?: !isPaused
        if (isPaused) {
            moveDirInput = 0
            angleDirInput = 0
        }
    }

    fun setShowTutorial(show: Boolean) {
        showTutorial = show
    }

    fun setShowSettings(show: Boolean) {
        showSettings = show
    }

    fun returnToTitle() {
        isPaused = false
        showTutorial = false
        showSettings = false
        turnPhase = TurnPhase.TITLE
        projectiles.clear()
        gasClouds.clear()
        explosions.clear()
        particles.clear()
        popups.clear()
        statusBanner = null
    }

    fun consumeAudioEvents(): List<EngineAudioEvent> {
        if (pendingAudioEvents.isEmpty()) return emptyList()
        val copy = pendingAudioEvents.toList()
        pendingAudioEvents.clear()
        return copy
    }

    // --- 물리 & 이펙트 스텝 ---

    fun step(rawDt: Float) {
        val dt = rawDt.coerceIn(0.001f, 0.033f)
        animTime += dt
        updateWeather(dt)

        if (isPaused || turnPhase == TurnPhase.TITLE) return

        updateEffects(dt)
        updateAmbientStatusParticles(dt)
        updatePropsAndScenery(dt)
        updateTankPhysicsAndFalling(dt)
        updateShake(dt)

        when (turnPhase) {
            TurnPhase.PLAYER_AIMING -> {
                updatePlayerAiming(dt)
                updateCameraToActiveTank(dt)
            }
            TurnPhase.CHARGING -> {
                updateCharging(dt)
                updateCameraToActiveTank(dt)
            }
            TurnPhase.AI_THINKING -> {
                updateAiTurn(dt)
                updateCameraToActiveTank(dt)
            }
            TurnPhase.PROJECTILE_FLIGHT -> {
                updateBurstQueue(dt)
                updateProjectiles(dt)
                updateCameraToProjectile(dt)
            }
            TurnPhase.POST_IMPACT -> {
                postImpactTimer -= dt
                if (postImpactTimer <= 0f && tanks.none { it.isFalling }) {
                    advanceTurnOrEnd()
                }
            }
            TurnPhase.GAME_OVER -> {
                updateVictoryFireworks(dt)
            }
            TurnPhase.TITLE -> Unit
        }
    }

    private fun updatePropsAndScenery(dt: Float) {
        // 범퍼 바운스 타이머 감소 및 지면 지지 확인
        for (i in props.indices) {
            var p = props[i]
            if (!p.isActive) continue
            if (p.bounceAnimTimer > 0f) {
                p = p.copy(bounceAnimTimer = max(0f, p.bounceAnimTimer - dt))
            }
            if (p.type == PropType.TNT_BARREL || p.type == PropType.SUPPLY_CRATE || p.type == PropType.UPDRAFT_VENT) {
                val groundY = terrain.findGroundY(p.x, p.y - 4f, maxStepUp = 6f)
                if (groundY == null || groundY > terrain.worldHeight + 20f) {
                    p = p.copy(isActive = false)
                } else if (groundY > p.y + 4f) {
                    if (p.type == PropType.TNT_BARREL) {
                        // 발밑이 무너지면 화약통 폭발!
                        props[i] = p.copy(isActive = false)
                        triggerTntExplosion(p, activeTankIndex)
                        continue
                    } else {
                        p = p.copy(y = groundY)
                    }
                }
            }
            props[i] = p
        }

        // 장식 나무/바위 아래 지형이 파괴되면 파편이 되며 사라짐
        for (i in sceneryProps.indices) {
            val s = sceneryProps[i]
            if (!s.isAlive) continue
            if (!terrain.isSolidWorld(s.x, s.y + 8f)) {
                sceneryProps[i] = s.copy(isAlive = false)
                repeat(6) {
                    particles.add(
                        ParticleState(
                            x = s.x + (Random.nextFloat() - 0.5f) * 20f,
                            y = s.y - 14f,
                            vx = (Random.nextFloat() - 0.5f) * 90f,
                            vy = -40f - Random.nextFloat() * 70f,
                            radius = 4f,
                            color = Color(mapTheme.grassBodyHex),
                            lifetime = 0.55f,
                            gravityScale = 0.6f
                        )
                    )
                }
            }
        }
    }

    private fun updateAmbientStatusParticles(dt: Float) {
        stinkParticleTimer -= dt
        if (stinkParticleTimer <= 0f) {
            stinkParticleTimer = 0.16f
            tanks.forEach { t ->
                if (t.isAlive && t.poisonTurns > 0) {
                    particles.add(
                        ParticleState(
                            x = t.x + (Random.nextFloat() - 0.5f) * 28f,
                            y = t.y - 32f,
                            vx = (Random.nextFloat() - 0.5f) * 14f,
                            vy = -28f - Random.nextFloat() * 18f,
                            radius = 3.5f + Random.nextFloat() * 2.5f,
                            color = Color(0xFF8D5524),
                            lifetime = 0.65f,
                            gravityScale = -0.05f,
                            isSmoke = true
                        )
                    )
                }
            }
            gasClouds.forEach { g ->
                val ang = Random.nextFloat() * (2f * PI.toFloat())
                val rad = Random.nextFloat() * g.radius * 0.75f
                particles.add(
                    ParticleState(
                        x = g.x + cos(ang) * rad,
                        y = g.y + sin(ang) * rad * 0.6f,
                        vx = windForce * 3f,
                        vy = -18f - Random.nextFloat() * 14f,
                        radius = 4f + Random.nextFloat() * 4f,
                        color = Color(0xFF70E000),
                        lifetime = 0.7f,
                        gravityScale = -0.04f,
                        isSmoke = true
                    )
                )
            }
            // 상승 기류 간헐천 기포/바람 입자
            props.forEach { p ->
                if (p.isActive && p.type == PropType.UPDRAFT_VENT) {
                    particles.add(
                        ParticleState(
                            x = p.x + (Random.nextFloat() - 0.5f) * (p.width * 0.75f),
                            y = p.y - 8f,
                            vx = (Random.nextFloat() - 0.5f) * 16f,
                            vy = -240f - Random.nextFloat() * 120f,
                            radius = 3.5f + Random.nextFloat() * 2.5f,
                            color = Color(0xFFCAF0F8),
                            lifetime = 0.9f,
                            gravityScale = -0.18f,
                            isSmoke = true
                        )
                    )
                }
            }
        }
    }

    private fun updateWeather(dt: Float) {
        val windDx = windForce * 10f
        for (i in weatherParticles.indices) {
            val wp = weatherParticles[i]
            var nx = wp.x + (wp.vx + windDx) * dt
            var ny = wp.y + wp.vy * dt
            if (nx < 0f) nx += terrain.worldWidth
            if (nx > terrain.worldWidth) nx -= terrain.worldWidth
            if (ny > terrain.worldHeight) ny -= terrain.worldHeight
            if (ny < 0f) ny += terrain.worldHeight
            weatherParticles[i] = wp.copy(x = nx, y = ny)
        }
    }

    private fun updateVictoryFireworks(dt: Float) {
        fireworkTimer -= dt
        if (fireworkTimer <= 0f) {
            fireworkTimer = 0.55f + Random.nextFloat() * 0.45f
            val fx = (cameraX - 320f + Random.nextFloat() * 640f).coerceIn(120f, terrain.worldWidth - 120f)
            val fy = (cameraY - 220f + Random.nextFloat() * 140f).coerceIn(80f, 450f)
            val fwColor = listOf(
                PrimaryGold,
                Color(0xFF4CC9F0),
                Color(0xFFFF4D6D),
                Color(0xFF80ED99)
            ).random()
            explosions.add(ExplosionEffect(x = fx, y = fy, maxRadius = 64f, duration = 0.55f, coreColor = fwColor))
            repeat(22) {
                val ang = Random.nextFloat() * (2f * PI.toFloat())
                val spd = 90f + Random.nextFloat() * 190f
                particles.add(
                    ParticleState(
                        x = fx,
                        y = fy,
                        vx = cos(ang) * spd,
                        vy = sin(ang) * spd,
                        radius = 3.5f + Random.nextFloat() * 3f,
                        color = fwColor,
                        lifetime = 0.7f + Random.nextFloat() * 0.4f,
                        gravityScale = 0.35f
                    )
                )
            }
        }
    }

    private fun updatePlayerAiming(dt: Float) {
        val idx = activeTankIndex
        var t = tanks[idx]

        if (moveDirInput != 0 && t.fuel > 0f && !t.isFalling) {
            val newFacing = moveDirInput > 0
            val nextX = (t.x + moveDirInput * TANK_MOVE_SPEED * dt).coerceIn(36f, terrain.worldWidth - 36f)
            val groundY = terrain.findGroundY(nextX, t.y, maxStepUp = 18f)
            if (groundY != null) {
                val fuelSpent = FUEL_BURN_PER_SEC * dt
                val slope = terrain.computeSlopeAngleDeg(nextX, groundY)
                t = t.copy(
                    x = nextX,
                    y = groundY,
                    facingRight = newFacing,
                    slopeAngleDeg = lerp(t.slopeAngleDeg, slope, min(1f, dt * 12f)),
                    fuel = max(0f, t.fuel - fuelSpent)
                )
            } else {
                t = t.copy(facingRight = newFacing)
            }
        }

        if (angleDirInput != 0) {
            val nextAngle = (t.aimAngleDeg + angleDirInput * ANGLE_CHANGE_SPEED * dt).coerceIn(0f, 90f)
            t = t.copy(aimAngleDeg = nextAngle)
        }

        tanks[idx] = t

        turnTimeRemaining -= dt
        if (turnTimeRemaining <= 0f) {
            turnTimeRemaining = 0f
            tanks[idx] = tanks[idx].copy(fuel = 0f)
            spawnPopup(t.x, t.y - 68f, if (language == AppLanguage.KO) "시간 초과!" else "Time Out!", WarningAmber)
            postImpactTimer = 0.65f
            turnPhase = TurnPhase.POST_IMPACT
        }
    }

    private fun updateCharging(dt: Float) {
        val speed = 0.72f
        if (chargeIncreasing) {
            chargingPower += speed * dt
            if (chargingPower >= 1.0f) {
                chargingPower = 1.0f
                chargeIncreasing = false
            }
        } else {
            chargingPower -= speed * dt
            if (chargingPower <= 0.10f) {
                chargingPower = 0.10f
                chargeIncreasing = true
            }
        }

        turnTimeRemaining -= dt
        if (turnTimeRemaining <= 0f) {
            turnTimeRemaining = 0f
            releaseAndFire()
        }
    }

    private fun updateAiTurn(dt: Float) {
        val idx = activeTankIndex
        var t = tanks[idx]
        val enemy = tanks.firstOrNull { it.id != t.id && it.isAlive } ?: return

        if (!aiHasPlanned) {
            planAiTurn(t, enemy)
            aiHasPlanned = true
        }

        aiStateTimer += dt
        turnTimeRemaining = max(0f, turnTimeRemaining - dt)

        if (aiStateTimer < 0.7f) {
            val shouldFaceRight = enemy.x > t.x
            if (aiDesiredMoveDir != 0 && aiMoveDuration > 0f && t.fuel > 10f && !t.isFalling) {
                val nextX = (t.x + aiDesiredMoveDir * TANK_MOVE_SPEED * dt).coerceIn(45f, terrain.worldWidth - 45f)
                val groundY = terrain.findGroundY(nextX, t.y, maxStepUp = 18f)
                if (groundY != null) {
                    t = t.copy(
                        x = nextX,
                        y = groundY,
                        facingRight = shouldFaceRight,
                        slopeAngleDeg = terrain.computeSlopeAngleDeg(nextX, groundY),
                        fuel = max(0f, t.fuel - FUEL_BURN_PER_SEC * dt)
                    )
                }
                aiMoveDuration -= dt
            } else {
                t = t.copy(facingRight = shouldFaceRight)
            }
            tanks[idx] = t
            return
        }

        if (aiStateTimer < 1.45f) {
            val diff = aiTargetAngleDeg - t.aimAngleDeg
            val step = diff.coerceIn(-ANGLE_CHANGE_SPEED * 1.4f * dt, ANGLE_CHANGE_SPEED * 1.4f * dt)
            t = t.copy(aimAngleDeg = (t.aimAngleDeg + step).coerceIn(0f, 90f))
            tanks[idx] = t
            return
        }

        if (aiStateTimer < 2.0f) {
            val progress = ((aiStateTimer - 1.45f) / 0.55f).coerceIn(0f, 1f)
            chargingPower = aiTargetPower * progress
            tanks[idx] = t.copy(expression = TankExpression.CHARGING)
            return
        }

        tanks[idx] = t.copy(aimAngleDeg = aiTargetAngleDeg, expression = TankExpression.NORMAL)
        selectedWeapon = aiTargetWeapon
        chargingPower = 0f
        fireActiveTank(aiTargetPower, aiTargetWeapon)
    }

    private fun planAiTurn(aiTank: TankState, target: TankState) {
        val facingRight = target.x >= aiTank.x
        val availSpecial = WeaponType.entries.filter {
            it != WeaponType.STANDARD && (aiTank.weaponCooldowns[it] ?: 0) == 0
        }

        aiTargetWeapon = when (difficulty) {
            Difficulty.EASY -> if (WeaponType.WATER in availSpecial && Random.nextFloat() < 0.25f) {
                WeaponType.WATER
            } else {
                WeaponType.STANDARD
            }
            Difficulty.NORMAL -> if (availSpecial.isNotEmpty() && Random.nextFloat() < 0.55f) {
                availSpecial.random()
            } else {
                WeaponType.STANDARD
            }
            Difficulty.HARD -> if (availSpecial.isNotEmpty() && Random.nextFloat() < 0.75f) {
                availSpecial.random()
            } else {
                WeaponType.STANDARD
            }
        }

        var bestAngle = 45f
        var bestPower = 0.6f
        var bestDist = Float.MAX_VALUE

        val angleCandidates = listOf(28f, 36f, 44f, 52f, 60f, 68f, 75f)
        for (angle in angleCandidates) {
            for (pStep in 5..20) {
                val power = pStep * 0.05f
                val dist = simulateTrajectoryMinDistance(
                    tank = aiTank.copy(facingRight = facingRight, aimAngleDeg = angle),
                    power = power,
                    weapon = aiTargetWeapon,
                    targetX = target.x,
                    targetY = target.y - 16f
                )
                if (dist < bestDist) {
                    bestDist = dist
                    bestAngle = angle
                    bestPower = power
                }
            }
        }

        val baseA = bestAngle
        val baseP = bestPower
        for (da in -4..4 step 2) {
            for (dp in -3..3) {
                val candA = (baseA + da).coerceIn(10f, 82f)
                val candP = (baseP + dp * 0.015f).coerceIn(0.18f, 1.0f)
                val dist = simulateTrajectoryMinDistance(
                    tank = aiTank.copy(facingRight = facingRight, aimAngleDeg = candA),
                    power = candP,
                    weapon = aiTargetWeapon,
                    targetX = target.x,
                    targetY = target.y - 16f
                )
                if (dist < bestDist) {
                    bestDist = dist
                    bestAngle = candA
                    bestPower = candP
                }
            }
        }

        val (angleNoise, powerNoise) = when (difficulty) {
            Difficulty.EASY -> 7.5f to 0.13f
            Difficulty.NORMAL -> 3.5f to 0.065f
            Difficulty.HARD -> 1.2f to 0.025f
        }
        aiTargetAngleDeg = (bestAngle + (Random.nextFloat() * 2f - 1f) * angleNoise).coerceIn(8f, 85f)
        aiTargetPower = (bestPower + (Random.nextFloat() * 2f - 1f) * powerNoise).coerceIn(0.18f, 1.0f)

        if (Random.nextFloat() < 0.45f) {
            aiDesiredMoveDir = if (Random.nextBoolean()) 1 else -1
            aiMoveDuration = 0.3f + Random.nextFloat() * 0.35f
        } else {
            aiDesiredMoveDir = 0
            aiMoveDuration = 0f
        }
    }

    private fun simulateTrajectoryMinDistance(
        tank: TankState,
        power: Float,
        weapon: WeaponType,
        targetX: Float,
        targetY: Float
    ): Float {
        val (muzzle, worldAngleRad) = computeMuzzleAndAngle(tank)
        val speed = MAX_LAUNCH_SPEED * power * weapon.speedMultiplier
        var x = muzzle.x
        var y = muzzle.y
        var vx = cos(worldAngleRad) * speed
        var vy = sin(worldAngleRad) * speed

        var minDist = Float.MAX_VALUE
        val dt = 0.025f
        for (step in 0 until 240) {
            vx += windForce * WIND_ACCEL * dt
            vy += GRAVITY * dt
            // 상승 기류 반영
            for (prop in props) {
                if (prop.isActive && prop.type == PropType.UPDRAFT_VENT &&
                    abs(x - prop.x) <= prop.width * 0.5f &&
                    y in (prop.y - prop.height)..prop.y
                ) {
                    vy -= UPDRAFT_LIFT_ACCEL * dt
                }
            }
            x += vx * dt
            y += vy * dt

            val d = hypot(x - targetX, y - targetY)
            if (d < minDist) minDist = d

            if (x < -80f || x > terrain.worldWidth + 80f || y > terrain.worldHeight + 80f) break
            if (step > 2 && terrain.isSolidWorld(x, y)) break
        }
        return minDist
    }

    private fun fireActiveTank(power: Float, weapon: WeaponType) {
        val idx = activeTankIndex
        val t = tanks[idx]
        val (muzzle, worldAngleRad) = computeMuzzleAndAngle(t)
        val speed = MAX_LAUNCH_SPEED * power * weapon.speedMultiplier

        val updatedCooldowns = t.weaponCooldowns.toMutableMap()
        if (weapon.cooldownTurns > 0) {
            updatedCooldowns[weapon] = weapon.cooldownTurns + 1
        }
        tanks[idx] = t.copy(
            lastPowerRatio = power,
            recoilOffset = 10f,
            weaponCooldowns = updatedCooldowns
        )

        spawnProjectile(t.id, weapon, muzzle.x, muzzle.y, worldAngleRad, speed)

        if (weapon.shotCount > 1) {
            pendingBurstShots = weapon.shotCount - 1
            burstTimer = 0.22f
            burstAngleRad = worldAngleRad
            burstSpeed = speed * 1.03f
            burstWeapon = weapon
            burstOwnerId = t.id
        } else {
            pendingBurstShots = 0
        }

        turnPhase = TurnPhase.PROJECTILE_FLIGHT
        statusBanner = null
    }

    private fun spawnProjectile(
        ownerId: Int,
        weapon: WeaponType,
        x: Float,
        y: Float,
        worldAngleRad: Float,
        speed: Float
    ) {
        val vx = cos(worldAngleRad) * speed
        val vy = sin(worldAngleRad) * speed
        val radius = when (weapon) {
            WeaponType.POWER -> 10f
            WeaponType.POOP, WeaponType.TRASH, WeaponType.WATER -> 9f
            else -> 7.5f
        }
        projectiles.add(
            ProjectileState(
                id = nextProjectileId++,
                ownerId = ownerId,
                weapon = weapon,
                x = x,
                y = y,
                vx = vx,
                vy = vy,
                radius = radius,
                trail = listOf(Offset(x, y))
            )
        )
        muzzleFlashTimer = 0.16f
        muzzleFlashPos = Offset(x, y)
        spawnMuzzleSmoke(x, y, worldAngleRad)
        pendingAudioEvents.add(EngineAudioEvent.Fire(heavy = weapon == WeaponType.POWER))
    }

    private fun updateBurstQueue(dt: Float) {
        if (pendingBurstShots > 0) {
            burstTimer -= dt
            if (burstTimer <= 0f) {
                val owner = tanks.firstOrNull { it.id == burstOwnerId } ?: return
                val (muzzle, _) = computeMuzzleAndAngle(owner)
                tanks[activeTankIndex] = owner.copy(recoilOffset = 9f)
                spawnProjectile(burstOwnerId, burstWeapon, muzzle.x, muzzle.y, burstAngleRad, burstSpeed)
                pendingBurstShots--
                burstTimer = 0.22f
            }
        }
    }

    private fun updateProjectiles(dt: Float) {
        val newSubMunitions = mutableListOf<ProjectileState>()
        val iterator = projectiles.listIterator()
        var anyExploded = false

        while (iterator.hasNext()) {
            val p = iterator.next()
            if (!p.active) continue

            var newVx = p.vx + windForce * WIND_ACCEL * dt
            var newVy = p.vy + GRAVITY * dt

            // 상승 기류 간헐천 통과 시 위로 부양!
            for (prop in props) {
                if (prop.isActive && prop.type == PropType.UPDRAFT_VENT &&
                    abs(p.x - prop.x) <= prop.width * 0.5f &&
                    p.y in (prop.y - prop.height)..prop.y
                ) {
                    newVy -= UPDRAFT_LIFT_ACCEL * dt
                }
            }

            val dx = newVx * dt
            val dy = newVy * dt

            val dist = hypot(dx, dy)
            val steps = max(1, (dist / 4f).toInt())
            var hitX = p.x + dx
            var hitY = p.y + dy
            var collided = false
            var bounced = false

            for (s in 1..steps) {
                val frac = s.toFloat() / steps.toFloat()
                val sx = p.x + dx * frac
                val sy = p.y + dy * frac

                // 1) 공중 도탄 범퍼 충돌 검사
                val bumperIdx = props.indexOfFirst { prop ->
                    prop.isActive && prop.type == PropType.RICOCHET_BUMPER &&
                        hypot(sx - prop.x, sy - prop.y) <= prop.radius + p.radius
                }
                if (bumperIdx >= 0) {
                    val bumper = props[bumperIdx]
                    val nxRaw = sx - bumper.x
                    val nyRaw = sy - bumper.y
                    val len = max(0.01f, hypot(nxRaw, nyRaw))
                    val nx = nxRaw / len
                    val ny = nyRaw / len
                    val dot = newVx * nx + newVy * ny
                    val refVx = (newVx - 2f * dot * nx) * 1.04f
                    val refVy = (newVy - 2f * dot * ny) * 1.04f
                    val pushOut = bumper.radius + p.radius + 4f

                    hitX = bumper.x + nx * pushOut
                    hitY = bumper.y + ny * pushOut
                    newVx = refVx
                    newVy = refVy
                    props[bumperIdx] = bumper.copy(bounceAnimTimer = 0.35f)
                    bounced = true
                    spawnPopup(bumper.x, bumper.y - 36f, if (language == AppLanguage.KO) "도탄!" else "Bounce!", Color(0xFF4CC9F0))
                    pendingAudioEvents.add(EngineAudioEvent.Ricochet)
                    break
                }

                // 2) 지면 화약통 / 보급 상자 / 탱크 / 지형 충돌 검사
                if (checkPropImpact(sx, sy) || checkTankCollision(sx, sy, p.ownerId, p.isSubMunition) || terrain.isSolidWorld(sx, sy)) {
                    hitX = sx
                    hitY = sy
                    collided = true
                    break
                }
            }

            if (collided) {
                explodeAt(hitX, hitY, p.weapon, p.ownerId, p.isSubMunition)
                if (p.weapon == WeaponType.TRASH && !p.isSubMunition) {
                    val spreadVx = listOf(-170f, 0f, 170f)
                    spreadVx.forEachIndexed { idx, svx ->
                        newSubMunitions.add(
                            ProjectileState(
                                id = nextProjectileId++,
                                ownerId = p.ownerId,
                                weapon = WeaponType.TRASH,
                                x = hitX,
                                y = hitY - 12f,
                                vx = svx + (idx - 1) * 15f,
                                vy = -250f - (idx % 2) * 55f,
                                radius = 5.5f,
                                isSubMunition = true,
                                trail = listOf(Offset(hitX, hitY - 12f))
                            )
                        )
                    }
                }
                iterator.remove()
                anyExploded = true
                continue
            }

            if (hitX < -140f || hitX > terrain.worldWidth + 140f || hitY > terrain.worldHeight + 120f) {
                iterator.remove()
                continue
            }

            if (bounced || Random.nextFloat() < 0.65f) {
                val trailColor = when (p.weapon) {
                    WeaponType.WATER -> Color(0xFF90E0EF)
                    WeaponType.POOP -> Color(0xFFA98467)
                    WeaponType.GAS -> Color(0xFF9EF01A)
                    WeaponType.TRASH -> Color(0xFFCED4DA)
                    else -> Color.White
                }
                particles.add(
                    ParticleState(
                        x = hitX,
                        y = hitY,
                        vx = (Random.nextFloat() - 0.5f) * 18f,
                        vy = -10f - Random.nextFloat() * 14f,
                        radius = p.radius * 0.85f,
                        color = trailColor.copy(alpha = 0.78f),
                        lifetime = 0.42f,
                        gravityScale = -0.08f,
                        isSmoke = true
                    )
                )
            }

            val updatedTrail = (p.trail + Offset(hitX, hitY)).takeLast(24)
            iterator.set(
                p.copy(
                    x = hitX,
                    y = hitY,
                    vx = newVx,
                    vy = newVy,
                    spinDeg = (p.spinDeg + 540f * dt) % 360f,
                    trail = updatedTrail
                )
            )
        }

        if (newSubMunitions.isNotEmpty()) {
            projectiles.addAll(newSubMunitions)
        }

        if (projectiles.isEmpty() && pendingBurstShots == 0) {
            postImpactTimer = if (anyExploded) 0.9f else 0.45f
            turnPhase = TurnPhase.POST_IMPACT
        }
    }

    private fun checkPropImpact(px: Float, py: Float): Boolean {
        for (prop in props) {
            if (!prop.isActive) continue
            if (prop.type == PropType.TNT_BARREL || prop.type == PropType.SUPPLY_CRATE) {
                val cy = prop.y - prop.height * 0.5f
                if (hypot(px - prop.x, py - cy) <= prop.radius + 3f) {
                    return true
                }
            }
        }
        return false
    }

    private fun checkTankCollision(px: Float, py: Float, ownerId: Int, isSubMunition: Boolean): Boolean {
        for (t in tanks) {
            if (!t.isAlive) continue
            val centerY = t.y - 18f
            val hitRadius = if (t.id == ownerId && !isSubMunition) 14f else 26f
            if (hypot(px - t.x, py - centerY) <= hitRadius) {
                return true
            }
        }
        return false
    }

    private fun explodeAt(
        x: Float,
        y: Float,
        weapon: WeaponType,
        attackerId: Int,
        isSubMunition: Boolean = false
    ) {
        val craterRadius = if (isSubMunition) 24f else weapon.craterRadius
        val blastRadius = if (isSubMunition) 48f else weapon.blastRadius
        val baseDamage = if (isSubMunition) 12 else weapon.damage

        terrain.carveCrater(x, y, craterRadius)

        explosions.add(
            ExplosionEffect(
                x = x,
                y = y,
                maxRadius = blastRadius,
                coreColor = weapon.shellColor
            )
        )
        spawnWeaponImpactParticles(x, y, blastRadius, weapon)
        triggerShake(
            intensity = when {
                isSubMunition -> 6f
                weapon == WeaponType.POWER -> 15f
                else -> 9.5f
            },
            duration = if (isSubMunition) 0.18f else 0.32f
        )

        if (weapon == WeaponType.GAS && !isSubMunition) {
            gasClouds.add(
                GasCloudZone(
                    id = nextGasId++,
                    x = x,
                    y = y - 16f,
                    radius = 96f,
                    turnsRemaining = 3,
                    ownerId = attackerId
                )
            )
            spawnPopup(x, y - 50f, if (language == AppLanguage.KO) "독가스 구름!" else "Toxic Cloud!", Color(0xFF70E000))
        }

        // 화약통 연쇄 폭발 & 보급 상자 파괴 획득 체크
        for (pi in props.indices) {
            val prop = props[pi]
            if (!prop.isActive) continue
            val propCy = prop.y - prop.height * 0.5f
            val dProp = hypot(x - prop.x, y - propCy)
            if (dProp <= blastRadius + 14f) {
                if (prop.type == PropType.TNT_BARREL) {
                    props[pi] = prop.copy(isActive = false)
                    triggerTntExplosion(prop, attackerId)
                } else if (prop.type == PropType.SUPPLY_CRATE) {
                    props[pi] = prop.copy(isActive = false)
                    grantSupplyCrateReward(attackerId, prop.x, propCy)
                }
            }
        }

        var anyTankHit = false
        val directRadius = 28f
        for (i in tanks.indices) {
            var t = tanks[i]
            if (!t.isAlive) continue
            val tankCenterY = t.y - 18f
            val dist = hypot(x - t.x, y - tankCenterY)
            if (dist <= blastRadius) {
                anyTankHit = true
                val isDirect = dist <= directRadius
                val factor = if (isDirect) {
                    1.0f
                } else {
                    (1f - (dist - directRadius) / (blastRadius - directRadius)).coerceIn(0.18f, 0.92f)
                }
                var dmg = max(4, (baseDamage * factor).toInt())
                var shieldConsumed = false
                if (t.hasShield) {
                    dmg = max(2, dmg / 2)
                    shieldConsumed = true
                }
                val newHp = max(0, t.hp - dmg)

                var updatedX = t.x
                var updatedFuel = t.fuel
                var updatedPoison = t.poisonTurns
                var extraLabel: String? = null

                if (weapon == WeaponType.WATER && !isSubMunition) {
                    val pushDir = if (t.x >= x) 1f else -1f
                    val pushedX = (t.x + pushDir * 36f).coerceIn(36f, terrain.worldWidth - 36f)
                    if (terrain.findGroundY(pushedX, t.y, maxStepUp = 22f) != null) {
                        updatedX = pushedX
                    }
                    updatedFuel = max(0f, t.fuel - t.maxFuel * 0.5f)
                    extraLabel = if (language == AppLanguage.KO) "미끄덩!" else "Splash!"
                } else if (weapon == WeaponType.POOP && !isSubMunition) {
                    updatedPoison = max(updatedPoison, 2)
                    extraLabel = if (language == AppLanguage.KO) "으악 똥!" else "Stinky!"
                }

                t = t.copy(
                    x = updatedX,
                    hp = newHp,
                    fuel = updatedFuel,
                    poisonTurns = updatedPoison,
                    hasShield = if (shieldConsumed) false else t.hasShield,
                    expression = TankExpression.DIZZY,
                    dizzyTimer = if (weapon == WeaponType.POOP) 2.5f else 1.6f
                )
                tanks[i] = t

                val attackerIdx = tanks.indexOfFirst { it.id == attackerId }
                if (attackerIdx >= 0 && attackerIdx != i) {
                    val att = tanks[attackerIdx]
                    tanks[attackerIdx] = att.copy(
                        totalDamageDealt = att.totalDamageDealt + dmg,
                        directHits = att.directHits + if (isDirect) 1 else 0
                    )
                }

                val prefix = when {
                    shieldConsumed -> if (language == AppLanguage.KO) "방어! -" else "Shield! -"
                    isDirect -> if (language == AppLanguage.KO) "직격! -" else "Direct! -"
                    else -> "-"
                }
                val color = if (isDirect) PrimaryGold else DangerRed
                spawnPopup(t.x, t.y - 56f, "$prefix$dmg", color)
                if (extraLabel != null) {
                    spawnPopup(t.x, t.y - 82f, extraLabel, weapon.shellColor)
                }

                if (newHp <= 0) {
                    killTank(i, if (language == AppLanguage.KO) "파괴!" else "Destroyed!")
                }
            }
        }

        pendingAudioEvents.add(
            EngineAudioEvent.WeaponImpact(
                weapon = weapon,
                hitTank = anyTankHit,
                isSubMunition = isSubMunition
            )
        )
    }

    private fun triggerTntExplosion(barrel: BattlePropState, attackerId: Int) {
        val bx = barrel.x
        val by = barrel.y - barrel.height * 0.5f
        spawnPopup(bx, by - 36f, if (language == AppLanguage.KO) "연쇄 폭발! TNT!" else "TNT BOOM!", Color(0xFFFF4D6D))
        explodeAt(bx, by, WeaponType.POWER, attackerId, isSubMunition = true)
        terrain.carveCrater(bx, by, 54f)
        explosions.add(ExplosionEffect(x = bx, y = by, maxRadius = 96f, coreColor = DangerRed))
        triggerShake(16f, 0.36f)
    }

    private fun grantSupplyCrateReward(tankId: Int, cx: Float, cy: Float) {
        val idx = tanks.indexOfFirst { it.id == tankId && it.isAlive }
        if (idx >= 0) {
            val t = tanks[idx]
            val healed = min(t.maxHp, t.hp + 25)
            tanks[idx] = t.copy(
                hp = healed,
                hasShield = true,
                weaponCooldowns = emptyMap()
            )
            spawnPopup(
                cx,
                cy - 32f,
                if (language == AppLanguage.KO) "보급! HP+25 · 방어막 · 쿨초기화" else "Supply! HP+25 · Shield",
                PrimaryGold
            )
            pendingAudioEvents.add(EngineAudioEvent.ItemUse)
        }
    }

    private fun updateTankPhysicsAndFalling(dt: Float) {
        for (i in tanks.indices) {
            var t = tanks[i]
            if (!t.isAlive) continue

            // 보급 상자 위로 탱크가 지나가면 즉시 획득
            for (pi in props.indices) {
                val prop = props[pi]
                if (prop.isActive && prop.type == PropType.SUPPLY_CRATE) {
                    if (hypot(t.x - prop.x, (t.y - 16f) - (prop.y - 16f)) <= 36f) {
                        props[pi] = prop.copy(isActive = false)
                        grantSupplyCrateReward(t.id, prop.x, prop.y - 20f)
                        t = tanks[i]
                    }
                }
            }

            if (t.recoilOffset > 0f) {
                t = t.copy(recoilOffset = max(0f, t.recoilOffset - 36f * dt))
            }
            if (t.dizzyTimer > 0f) {
                val nextDizzy = max(0f, t.dizzyTimer - dt)
                t = t.copy(
                    dizzyTimer = nextDizzy,
                    expression = if (nextDizzy > 0f) TankExpression.DIZZY else TankExpression.NORMAL
                )
            }

            val groundY = terrain.findGroundY(t.x, t.y, maxStepUp = 8f)
            if (groundY == null || groundY > t.y + 3f) {
                val startY = if (t.isFalling) t.fallStartY else t.y
                val newVy = t.vy + GRAVITY * dt
                val newY = t.y + newVy * dt

                if (groundY != null && newY >= groundY) {
                    val fallDistance = groundY - startY
                    var updatedHp = t.hp
                    var nextDizzy = t.dizzyTimer
                    if (fallDistance > 45f) {
                        val fallDmg = ((fallDistance - 45f) * 0.24f).toInt().coerceIn(3, 35)
                        updatedHp = max(0, t.hp - fallDmg)
                        nextDizzy = 1.2f
                        val label = if (language == AppLanguage.KO) "낙하! -$fallDmg" else "Fall! -$fallDmg"
                        spawnPopup(t.x, groundY - 52f, label, WarningAmber)
                        triggerShake(5f, 0.16f)
                        pendingAudioEvents.add(EngineAudioEvent.WeaponImpact(WeaponType.STANDARD, hitTank = true))
                    }
                    val slope = terrain.computeSlopeAngleDeg(t.x, groundY)
                    t = t.copy(
                        y = groundY,
                        vy = 0f,
                        isFalling = false,
                        slopeAngleDeg = slope,
                        hp = updatedHp,
                        dizzyTimer = nextDizzy,
                        expression = if (nextDizzy > 0f) TankExpression.DIZZY else t.expression
                    )
                    tanks[i] = t
                    if (updatedHp <= 0) {
                        killTank(i, if (language == AppLanguage.KO) "추락 파손!" else "Crashed!")
                    }
                } else if (newY > terrain.worldHeight + 45f) {
                    tanks[i] = t.copy(y = newY, hp = 0, isFalling = false)
                    killTank(i, if (language == AppLanguage.KO) "추락!" else "Ring Out!")
                } else {
                    tanks[i] = t.copy(
                        y = newY,
                        vy = newVy,
                        isFalling = true,
                        fallStartY = startY
                    )
                }
            } else {
                val slope = terrain.computeSlopeAngleDeg(t.x, groundY)
                tanks[i] = t.copy(
                    y = groundY,
                    vy = 0f,
                    isFalling = false,
                    slopeAngleDeg = lerp(t.slopeAngleDeg, slope, min(1f, dt * 10f))
                )
            }
        }
    }

    private fun killTank(index: Int, reason: String) {
        val t = tanks[index]
        if (!t.isAlive) return
        tanks[index] = t.copy(isAlive = false, hp = 0)
        explosions.add(ExplosionEffect(x = t.x, y = t.y - 18f, maxRadius = 120f, duration = 0.85f, coreColor = PrimaryGold))
        spawnWeaponImpactParticles(t.x, t.y - 18f, 115f, WeaponType.POWER)
        spawnPopup(t.x, t.y - 72f, reason, DangerRed)
        triggerShake(17f, 0.45f)
        pendingAudioEvents.add(EngineAudioEvent.WeaponImpact(WeaponType.POWER, hitTank = true))

        val aliveTanks = tanks.filter { it.isAlive }
        if (aliveTanks.size <= 1) {
            val winner = aliveTanks.firstOrNull()
            winnerId = winner?.id
            if (winner != null) {
                val wIdx = tanks.indexOfFirst { it.id == winner.id }
                if (wIdx >= 0) {
                    tanks[wIdx] = tanks[wIdx].copy(expression = TankExpression.HAPPY)
                }
            }
            turnPhase = TurnPhase.GAME_OVER
            statusBanner = null
            val playerWon = (gameMode == GameMode.LOCAL_2P) || (winner?.id == 0)
            pendingAudioEvents.add(EngineAudioEvent.GameOver(playerWon))
        }
    }

    private fun advanceTurnOrEnd() {
        val aliveTanks = tanks.filter { it.isAlive }
        if (aliveTanks.size <= 1) {
            winnerId = aliveTanks.firstOrNull()?.id
            turnPhase = TurnPhase.GAME_OVER
            return
        }
        val nextIndex = (activeTankIndex + 1) % tanks.size
        beginTurn(nextIndex)
    }

    private fun updateEffects(dt: Float) {
        if (muzzleFlashTimer > 0f) {
            muzzleFlashTimer = max(0f, muzzleFlashTimer - dt)
        }

        val expIt = explosions.listIterator()
        while (expIt.hasNext()) {
            val e = expIt.next()
            val nextAge = e.age + dt
            if (nextAge >= e.duration) expIt.remove() else expIt.set(e.copy(age = nextAge))
        }

        val partIt = particles.listIterator()
        while (partIt.hasNext()) {
            val p = partIt.next()
            val nextAge = p.age + dt
            if (nextAge >= p.lifetime) {
                partIt.remove()
            } else {
                val nvy = p.vy + GRAVITY * p.gravityScale * dt
                partIt.set(
                    p.copy(
                        x = p.x + p.vx * dt,
                        y = p.y + nvy * dt,
                        vy = nvy,
                        age = nextAge
                    )
                )
            }
        }

        val popIt = popups.listIterator()
        while (popIt.hasNext()) {
            val pop = popIt.next()
            val nextAge = pop.age + dt
            if (nextAge >= pop.duration) {
                popIt.remove()
            } else {
                popIt.set(pop.copy(y = pop.y - 34f * dt, age = nextAge))
            }
        }
    }

    private fun updateCameraToActiveTank(dt: Float) {
        if (manualPanTimer > 0f) {
            manualPanTimer = max(0f, manualPanTimer - dt)
            return
        }
        val t = tanks.getOrNull(activeTankIndex) ?: return
        val lookAhead = if (t.facingRight) 120f else -120f
        val targetX = (t.x + lookAhead).coerceIn(220f, terrain.worldWidth - 220f)
        val targetY = (t.y - 90f).coerceIn(220f, terrain.worldHeight - 160f)
        cameraX = lerp(cameraX, targetX, min(1f, dt * 4.2f))
        cameraY = lerp(cameraY, targetY, min(1f, dt * 4.2f))
    }

    private fun updateCameraToProjectile(dt: Float) {
        val lead = projectiles.firstOrNull() ?: return
        val targetX = lead.x.coerceIn(200f, terrain.worldWidth - 200f)
        val targetY = lead.y.coerceIn(140f, terrain.worldHeight - 160f)
        cameraX = lerp(cameraX, targetX, min(1f, dt * 7.5f))
        cameraY = lerp(cameraY, targetY, min(1f, dt * 6.5f))
    }

    private fun triggerShake(intensity: Float, duration: Float) {
        shakeIntensity = max(shakeIntensity, intensity)
        shakeTimer = max(shakeTimer, duration)
    }

    private fun updateShake(dt: Float) {
        if (shakeTimer > 0f) {
            shakeTimer = max(0f, shakeTimer - dt)
            val currentAmp = shakeIntensity * (shakeTimer / 0.35f).coerceIn(0f, 1f)
            shakeOffsetX = (Random.nextFloat() * 2f - 1f) * currentAmp
            shakeOffsetY = (Random.nextFloat() * 2f - 1f) * currentAmp
            if (shakeTimer <= 0f) {
                shakeIntensity = 0f
                shakeOffsetX = 0f
                shakeOffsetY = 0f
            }
        }
    }

    private fun spawnMuzzleSmoke(x: Float, y: Float, angleRad: Float) {
        repeat(10) {
            val spread = angleRad + (Random.nextFloat() - 0.5f) * 0.75f
            val speed = 65f + Random.nextFloat() * 125f
            particles.add(
                ParticleState(
                    x = x,
                    y = y,
                    vx = cos(spread) * speed,
                    vy = sin(spread) * speed,
                    radius = 5f + Random.nextFloat() * 6f,
                    color = if (it % 3 == 0) Color(0xFFFFD166) else Color(0xFFF1FAEE),
                    lifetime = 0.42f + Random.nextFloat() * 0.25f,
                    gravityScale = -0.15f,
                    isSmoke = it % 3 != 0
                )
            )
        }
    }

    private fun spawnWeaponImpactParticles(x: Float, y: Float, blastRadius: Float, weapon: WeaponType) {
        val count = (blastRadius / 2.8f).toInt().coerceIn(22, 38)
        repeat(count) { i ->
            val ang = Random.nextFloat() * (2f * PI.toFloat())
            val spd = 85f + Random.nextFloat() * (blastRadius * 3.6f)
            val color = when (weapon) {
                WeaponType.WATER -> if (i % 3 == 0) Color.White else Color(0xFF00BBF9)
                WeaponType.POOP -> when (i % 3) {
                    0 -> Color(0xFF7F4F24)
                    1 -> Color(0xFF582F0E)
                    else -> Color(0xFF9EF01A)
                }
                WeaponType.GAS -> if (i % 2 == 0) Color(0xFF70E000) else Color(0xFF38B000)
                WeaponType.TRASH -> when (i % 3) {
                    0 -> Color(0xFFADB5BD)
                    1 -> Color(0xFFFFD166)
                    else -> Color(0xFF6C757D)
                }
                else -> when (i % 4) {
                    0 -> weapon.shellColor
                    1 -> Color(0xFFFF9F1C)
                    2 -> Color(mapTheme.soilA)
                    else -> Color(0xFFF8F9FA)
                }
            }
            particles.add(
                ParticleState(
                    x = x,
                    y = y,
                    vx = cos(ang) * spd,
                    vy = sin(ang) * spd - 70f,
                    radius = 4f + Random.nextFloat() * 6f,
                    color = color,
                    lifetime = 0.5f + Random.nextFloat() * 0.48f,
                    gravityScale = if (weapon == WeaponType.GAS) -0.06f else 0.75f,
                    isSmoke = weapon == WeaponType.GAS || i % 4 == 3
                )
            )
        }
    }

    private fun spawnPopup(x: Float, y: Float, text: String, color: Color) {
        popups.add(
            DamagePopup(
                id = nextPopupId++,
                x = x,
                y = y,
                text = text,
                color = color
            )
        )
    }

    fun computeMuzzleAndAngle(tank: TankState): Pair<Offset, Float> {
        val slopeRad = Math.toRadians(tank.slopeAngleDeg.toDouble()).toFloat()
        val turretPivotX = tank.x + sin(slopeRad) * 20f
        val turretPivotY = tank.y - cos(slopeRad) * 20f

        val totalDeg = if (tank.facingRight) {
            -tank.aimAngleDeg + tank.slopeAngleDeg * 0.55f
        } else {
            -(180f - tank.aimAngleDeg) + tank.slopeAngleDeg * 0.55f
        }
        val rad = Math.toRadians(totalDeg.toDouble()).toFloat()
        val barrelLen = 34f - tank.recoilOffset
        val muzzleX = turretPivotX + cos(rad) * barrelLen
        val muzzleY = turretPivotY + sin(rad) * barrelLen
        return Offset(muzzleX, muzzleY) to rad
    }

    fun buildRenderSnapshot(): BattleRenderSnapshot {
        return BattleRenderSnapshot(
            worldWidth = terrain.worldWidth,
            worldHeight = terrain.worldHeight,
            mapTheme = mapTheme,
            cameraX = cameraX,
            cameraY = cameraY,
            shakeOffsetX = shakeOffsetX,
            shakeOffsetY = shakeOffsetY,
            animTime = animTime,
            windForce = windForce,
            turnPhase = turnPhase,
            activeTankIndex = activeTankIndex,
            tanks = tanks.toList(),
            projectiles = projectiles.toList(),
            gasClouds = gasClouds.toList(),
            props = props.toList(),
            sceneryProps = sceneryProps.toList(),
            explosions = explosions.toList(),
            particles = particles.toList(),
            weatherParticles = weatherParticles.toList(),
            popups = popups.toList(),
            chargingPower = chargingPower,
            muzzleFlashTimer = muzzleFlashTimer,
            muzzleFlashPos = muzzleFlashPos,
            terrainRevision = terrain.revision
        )
    }

    fun buildHudState(): BattleHudState {
        return BattleHudState(
            turnPhase = turnPhase,
            gameMode = gameMode,
            difficulty = difficulty,
            mapTheme = mapTheme,
            isPaused = isPaused,
            showTutorial = showTutorial,
            showSettings = showSettings,
            activeTankIndex = activeTankIndex,
            turnNumber = turnNumber,
            windForce = windForce,
            turnTimeRemaining = turnTimeRemaining,
            chargingPower = chargingPower,
            selectedWeapon = selectedWeapon,
            tanks = tanks.toList(),
            winnerId = winnerId,
            statusBanner = statusBanner
        )
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    companion object {
        const val TURN_DURATION_SEC = 20f
        const val GRAVITY = 520f
        const val WIND_ACCEL = 18f
        const val UPDRAFT_LIFT_ACCEL = 740f
        const val MAX_LAUNCH_SPEED = 980f
        const val TANK_MOVE_SPEED = 115f
        const val FUEL_BURN_PER_SEC = 34f
        const val ANGLE_CHANGE_SPEED = 48f
    }
}
