package com.example.game.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.EnemyCoral
import com.example.ui.theme.EnemyCoralLight
import com.example.ui.theme.PlayerBlue
import com.example.ui.theme.PlayerBlueLight
import com.example.ui.theme.PrimaryGold

enum class AppLanguage(val label: String) {
    KO("한국어"),
    EN("English")
}

enum class Difficulty(val labelKo: String, val labelEn: String) {
    EASY("쉬움", "Easy"),
    NORMAL("보통", "Normal"),
    HARD("어려움", "Hard");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.KO) labelKo else labelEn
}

enum class GameMode {
    AI_VS_PLAYER,
    LOCAL_2P,
    ONLINE_PVP
}

enum class MapTheme(
    val labelKo: String,
    val labelEn: String,
    val skyTop: Color,
    val skyMid: Color,
    val skyBottom: Color,
    val farHillColor: Color,
    val midHillColor: Color,
    val grassTopHex: Int,
    val grassBodyHex: Int,
    val grassShadowHex: Int,
    val soilA: Int,
    val soilB: Int,
    val soilC: Int,
    val rockHex: Int,
    val scorchHex: Int,
    val hasCastleSilhouettes: Boolean = false,
    val isNightSpace: Boolean = false
) {
    GRASSLAND(
        labelKo = "초원 요새",
        labelEn = "Fortress Hills",
        skyTop = Color(0xFF1E96FC),
        skyMid = Color(0xFF68D8FF),
        skyBottom = Color(0xFFD9F6FF),
        farHillColor = Color(0xFF6EC1E4),
        midHillColor = Color(0xFF68B684),
        grassTopHex = 0xFF80ED99.toInt(),
        grassBodyHex = 0xFF52B788.toInt(),
        grassShadowHex = 0xFF40916C.toInt(),
        soilA = 0xFF9C6644.toInt(),
        soilB = 0xFF7F5539.toInt(),
        soilC = 0xFF68412A.toInt(),
        rockHex = 0xFF6B705C.toInt(),
        scorchHex = 0xFF3E2414.toInt(),
        hasCastleSilhouettes = true
    ),
    DESERT(
        labelKo = "황금 사막",
        labelEn = "Golden Dunes",
        skyTop = Color(0xFFFB8500),
        skyMid = Color(0xFFFFB703),
        skyBottom = Color(0xFFFFF3B0),
        farHillColor = Color(0xFFE09F3E),
        midHillColor = Color(0xFFCA8A2E),
        grassTopHex = 0xFFFFE066.toInt(),
        grassBodyHex = 0xFFF4A261.toInt(),
        grassShadowHex = 0xFFE76F51.toInt(),
        soilA = 0xFFD98A4E.toInt(),
        soilB = 0xFFB56538.toInt(),
        soilC = 0xFF8C4A27.toInt(),
        rockHex = 0xFF7A5C58.toInt(),
        scorchHex = 0xFF432818.toInt()
    ),
    SNOW(
        labelKo = "만년 설원",
        labelEn = "Frost Peak",
        skyTop = Color(0xFF3A86FF),
        skyMid = Color(0xFFA2D2FF),
        skyBottom = Color(0xFFF0F7FF),
        farHillColor = Color(0xFF98B9E2),
        midHillColor = Color(0xFF6D9DC5),
        grassTopHex = 0xFFFFFFFF.toInt(),
        grassBodyHex = 0xFFDDF0FF.toInt(),
        grassShadowHex = 0xFFA9D6E5.toInt(),
        soilA = 0xFF5C6B73.toInt(),
        soilB = 0xFF4A5759.toInt(),
        soilC = 0xFF343A40.toInt(),
        rockHex = 0xFF2B2D42.toInt(),
        scorchHex = 0xFF1D2D44.toInt()
    ),
    VOLCANO(
        labelKo = "화산 협곡",
        labelEn = "Lava Canyon",
        skyTop = Color(0xFF370617),
        skyMid = Color(0xFF9D0208),
        skyBottom = Color(0xFFF48C06),
        farHillColor = Color(0xFF5A1818),
        midHillColor = Color(0xFF3D1313),
        grassTopHex = 0xFFFF7B00.toInt(),
        grassBodyHex = 0xFF5C3A3A.toInt(),
        grassShadowHex = 0xFF3E2723.toInt(),
        soilA = 0xFF4A2C2A.toInt(),
        soilB = 0xFF332020.toInt(),
        soilC = 0xFF211616.toInt(),
        rockHex = 0xFFDC2F02.toInt(),
        scorchHex = 0xFF140B0B.toInt()
    ),
    ISLAND(
        labelKo = "바다 섬",
        labelEn = "Coral Island",
        skyTop = Color(0xFF0077B6),
        skyMid = Color(0xFF48CAE4),
        skyBottom = Color(0xFFCAF0F8),
        farHillColor = Color(0xFF0096C7),
        midHillColor = Color(0xFF00B4D8),
        grassTopHex = 0xFFFFE6A7.toInt(),
        grassBodyHex = 0xFFF2CC8F.toInt(),
        grassShadowHex = 0xFFE07A5F.toInt(),
        soilA = 0xFFBC6C25.toInt(),
        soilB = 0xFF9C5221.toInt(),
        soilC = 0xFF6F3E1D.toInt(),
        rockHex = 0xFF6D6875.toInt(),
        scorchHex = 0xFF3D2614.toInt()
    ),
    SPACE(
        labelKo = "우주 기지",
        labelEn = "Cosmic Moon",
        skyTop = Color(0xFF0B091A),
        skyMid = Color(0xFF1D1B4B),
        skyBottom = Color(0xFF3B2870),
        farHillColor = Color(0xFF2E295E),
        midHillColor = Color(0xFF433D7A),
        grassTopHex = 0xFF9BF6FF.toInt(),
        grassBodyHex = 0xFF7B8CDE.toInt(),
        grassShadowHex = 0xFF5C67B0.toInt(),
        soilA = 0xFF4A4E69.toInt(),
        soilB = 0xFF383B53.toInt(),
        soilC = 0xFF22223B.toInt(),
        rockHex = 0xFFCDB4DB.toInt(),
        scorchHex = 0xFF141426.toInt(),
        isNightSpace = true
    );

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.KO) labelKo else labelEn
}

enum class WeaponType(
    val labelKo: String,
    val labelEn: String,
    val effectKo: String,
    val effectEn: String,
    val damage: Int,
    val blastRadius: Float,
    val craterRadius: Float,
    val cooldownTurns: Int,
    val shotCount: Int,
    val speedMultiplier: Float,
    val shellColor: Color
) {
    STANDARD(
        labelKo = "기본탄",
        labelEn = "Normal",
        effectKo = "균형 잡힌 기본 포탄",
        effectEn = "Balanced shell",
        damage = 32,
        blastRadius = 72f,
        craterRadius = 46f,
        cooldownTurns = 0,
        shotCount = 1,
        speedMultiplier = 1.0f,
        shellColor = Color(0xFFFFD166)
    ),
    DOUBLE(
        labelKo = "2연발",
        labelEn = "Double",
        effectKo = "연속 2발 타격",
        effectEn = "Fires 2 shells",
        damage = 21,
        blastRadius = 56f,
        craterRadius = 34f,
        cooldownTurns = 2,
        shotCount = 2,
        speedMultiplier = 1.02f,
        shellColor = Color(0xFF4CC9F0)
    ),
    POWER(
        labelKo = "강력탄",
        labelEn = "Heavy",
        effectKo = "대형 폭발 + 깊은 분화구",
        effectEn = "Mega blast & crater",
        damage = 50,
        blastRadius = 96f,
        craterRadius = 68f,
        cooldownTurns = 3,
        shotCount = 1,
        speedMultiplier = 0.94f,
        shellColor = Color(0xFFFF4D6D)
    ),
    WATER(
        labelKo = "물포탄",
        labelEn = "Water",
        effectKo = "넉백 + 적 연료 50% 감소",
        effectEn = "Knockback + drains fuel",
        damage = 26,
        blastRadius = 86f,
        craterRadius = 36f,
        cooldownTurns = 1,
        shotCount = 1,
        speedMultiplier = 1.04f,
        shellColor = Color(0xFF00BBF9)
    ),
    POOP(
        labelKo = "똥포탄",
        labelEn = "Poop",
        effectKo = "악취! 2턴간 추가 피해",
        effectEn = "Stink DoT for 2 turns",
        damage = 28,
        blastRadius = 76f,
        craterRadius = 42f,
        cooldownTurns = 2,
        shotCount = 1,
        speedMultiplier = 0.98f,
        shellColor = Color(0xFF8D5524)
    ),
    GAS(
        labelKo = "가스포탄",
        labelEn = "Gas",
        effectKo = "3턴간 지속 독가스 구름",
        effectEn = "3-turn toxic gas cloud",
        damage = 18,
        blastRadius = 84f,
        craterRadius = 24f,
        cooldownTurns = 3,
        shotCount = 1,
        speedMultiplier = 1.0f,
        shellColor = Color(0xFF70E000)
    ),
    TRASH(
        labelKo = "쓰레기탄",
        labelEn = "Trash",
        effectKo = "착탄 시 고철 3개 확산 폭발",
        effectEn = "Splits into 3 junk bombs",
        damage = 20,
        blastRadius = 64f,
        craterRadius = 36f,
        cooldownTurns = 2,
        shotCount = 1,
        speedMultiplier = 0.97f,
        shellColor = Color(0xFFADB5BD)
    );

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.KO) labelKo else labelEn
    fun effect(lang: AppLanguage): String = if (lang == AppLanguage.KO) effectKo else effectEn
}

/**
 * 7종 포탄(기본탄·2연발·강력탄·물포탄·똥포탄·가스포탄·쓰레기탄)에 1:1로 대응하는 개성 넘치는 전용 탱크 라인업.
 */
enum class TankType(
    val signatureWeapon: WeaponType,
    val labelKo: String,
    val labelEn: String,
    val titleKo: String,
    val titleEn: String,
    val perkKo: String,
    val perkEn: String,
    val primaryColor: Color,
    val accentColor: Color,
    val trimColor: Color,
    val bonusHp: Int = 0,
    val bonusFuel: Float = 0f,
    val signatureDamageMultiplier: Float = 1.15f,
    val signatureCooldownReduction: Int = 0
) {
    CLASSIC(
        signatureWeapon = WeaponType.STANDARD,
        labelKo = "캐논 캡틴",
        labelEn = "Cannon Captain",
        titleKo = "기본탄 특화 · 정통 만능 전차",
        titleEn = "Normal Shell · Balanced Ace",
        perkKo = "기본탄 피해 +15% · HP +5",
        perkEn = "Normal +15% DMG · +5 HP",
        primaryColor = Color(0xFF1E88E5),
        accentColor = Color(0xFF64B5F6),
        trimColor = Color(0xFFFFD166),
        bonusHp = 5,
        bonusFuel = 5f
    ),
    TWIN_STRIKER(
        signatureWeapon = WeaponType.DOUBLE,
        labelKo = "트윈 스트라이커",
        labelEn = "Twin Striker",
        titleKo = "2연발 특화 · 쌍포신 고속 전차",
        titleEn = "Double Shell · Dual-Barrel Jet",
        perkKo = "2연발 쿨타임 -1턴 · 연료 +15%",
        perkEn = "Double CD -1 · +15% Fuel",
        primaryColor = Color(0xFF0096C7),
        accentColor = Color(0xFF48CAE4),
        trimColor = Color(0xFF90E0EF),
        bonusFuel = 15f,
        signatureCooldownReduction = 1
    ),
    MEGA_TITAN(
        signatureWeapon = WeaponType.POWER,
        labelKo = "메가 타이탄",
        labelEn = "Mega Titan",
        titleKo = "강력탄 특화 · 중장갑 거포 전차",
        titleEn = "Heavy Shell · Siege Dreadnought",
        perkKo = "강력탄 피해 +15% · HP +15",
        perkEn = "Heavy +15% DMG · +15 HP",
        primaryColor = Color(0xFFD62828),
        accentColor = Color(0xFFF77F00),
        trimColor = Color(0xFFFFD166),
        bonusHp = 15
    ),
    AQUA_HYDRO(
        signatureWeapon = WeaponType.WATER,
        labelKo = "아쿠아 하이드로",
        labelEn = "Aqua Hydro",
        titleKo = "물포탄 특화 · 수압 제트 전차",
        titleEn = "Water Shell · Hydro-Jet Tank",
        perkKo = "물포탄 피해 +15% · 연료 +10%",
        perkEn = "Water +15% DMG · +10% Fuel",
        primaryColor = Color(0xFF00B4D8),
        accentColor = Color(0xFF90E0EF),
        trimColor = Color(0xFFCAF0F8),
        bonusFuel = 10f
    ),
    POOP_KING(
        signatureWeapon = WeaponType.POOP,
        labelKo = "응가 킹",
        labelEn = "Poop King",
        titleKo = "똥포탄 특화 · 황금 왕관 장난꾸러기",
        titleEn = "Poop Shell · Golden Crown Prankster",
        perkKo = "똥포탄 피해 +15% · 쿨타임 -1턴",
        perkEn = "Poop +15% DMG · CD -1",
        primaryColor = Color(0xFF8D5524),
        accentColor = Color(0xFFC68642),
        trimColor = Color(0xFFFFD166),
        bonusHp = 5,
        signatureCooldownReduction = 1
    ),
    TOXIC_CHEMIST(
        signatureWeapon = WeaponType.GAS,
        labelKo = "독가스 케미스트",
        labelEn = "Toxic Chemist",
        titleKo = "가스포탄 특화 · 네온 화학 탱크",
        titleEn = "Gas Shell · Bio-Chem Tank",
        perkKo = "가스포탄 피해 +15% · 쿨타임 -1턴",
        perkEn = "Gas +15% DMG · CD -1",
        primaryColor = Color(0xFF38B000),
        accentColor = Color(0xFF70E000),
        trimColor = Color(0xFFCCFF33),
        signatureCooldownReduction = 1
    ),
    JUNK_SCRAPPER(
        signatureWeapon = WeaponType.TRASH,
        labelKo = "고철 스크래퍼",
        labelEn = "Junk Scrapper",
        titleKo = "쓰레기탄 특화 · 리벳 강철 투석기",
        titleEn = "Trash Shell · Scrap Catapult",
        perkKo = "쓰레기탄 피해 +15% · HP +10",
        perkEn = "Trash +15% DMG · +10 HP",
        primaryColor = Color(0xFF6C757D),
        accentColor = Color(0xFFADB5BD),
        trimColor = Color(0xFFFFB703),
        bonusHp = 10,
        bonusFuel = 8f
    );

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.KO) labelKo else labelEn
    fun title(lang: AppLanguage): String = if (lang == AppLanguage.KO) titleKo else titleEn
    fun perk(lang: AppLanguage): String = if (lang == AppLanguage.KO) perkKo else perkEn

    companion object {
        fun fromNameOrDefault(raw: String?): TankType =
            entries.firstOrNull { it.name == raw } ?: CLASSIC

        fun forWeapon(weapon: WeaponType): TankType =
            entries.firstOrNull { it.signatureWeapon == weapon } ?: CLASSIC
    }
}

enum class ItemType(
    val labelKo: String,
    val labelEn: String,
    val tintColor: Color
) {
    HEAL("수리", "Heal", Color(0xFF2EC4B6)),
    FUEL_BOOST("연료+", "Fuel+", Color(0xFF4CC9F0)),
    SHIELD("방어막", "Shield", Color(0xFFA0C4FF));

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.KO) labelKo else labelEn
}

enum class TankExpression {
    NORMAL,
    CHARGING,
    DIZZY,
    HAPPY
}

enum class TurnPhase {
    TITLE,
    PLAYER_AIMING,
    CHARGING,
    AI_THINKING,
    PROJECTILE_FLIGHT,
    POST_IMPACT,
    GAME_OVER
}

data class TankState(
    val id: Int,
    val name: String,
    val isAi: Boolean,
    val x: Float,
    val y: Float,
    val vy: Float = 0f,
    val slopeAngleDeg: Float = 0f,
    val aimAngleDeg: Float = 45f,
    val facingRight: Boolean = true,
    val hp: Int = 100,
    val maxHp: Int = 100,
    val fuel: Float = 100f,
    val maxFuel: Float = 100f,
    val tankType: TankType = TankType.CLASSIC,
    val primaryColor: Color = PlayerBlue,
    val accentColor: Color = PlayerBlueLight,
    val recoilOffset: Float = 0f,
    val weaponCooldowns: Map<WeaponType, Int> = emptyMap(),
    val usedItems: Set<ItemType> = emptySet(),
    val hasShield: Boolean = false,
    val poisonTurns: Int = 0,
    val expression: TankExpression = TankExpression.NORMAL,
    val dizzyTimer: Float = 0f,
    val lastPowerRatio: Float? = null,
    val isFalling: Boolean = false,
    val fallStartY: Float = y,
    val isAlive: Boolean = true,
    val totalDamageDealt: Int = 0,
    val directHits: Int = 0
)

data class ProjectileState(
    val id: Int,
    val ownerId: Int,
    val weapon: WeaponType,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float = 7f,
    val isSubMunition: Boolean = false,
    val spinDeg: Float = 0f,
    val trail: List<Offset> = emptyList(),
    val active: Boolean = true
)

data class GasCloudZone(
    val id: Int,
    val x: Float,
    val y: Float,
    val radius: Float,
    val turnsRemaining: Int,
    val ownerId: Int
)

data class ExplosionEffect(
    val x: Float,
    val y: Float,
    val maxRadius: Float,
    val age: Float = 0f,
    val duration: Float = 0.6f,
    val coreColor: Color = PrimaryGold
)

data class ParticleState(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val color: Color,
    val age: Float = 0f,
    val lifetime: Float = 0.65f,
    val gravityScale: Float = 0.6f,
    val isSmoke: Boolean = false
)

data class WeatherParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val alpha: Float
)

data class DamagePopup(
    val id: Int,
    val x: Float,
    val y: Float,
    val text: String,
    val color: Color,
    val age: Float = 0f,
    val duration: Float = 1.3f
)

enum class PropType {
    TNT_BARREL,
    SUPPLY_CRATE,
    RICOCHET_BUMPER,
    UPDRAFT_VENT
}

data class BattlePropState(
    val id: Int,
    val type: PropType,
    val x: Float,
    val y: Float,
    val radius: Float = 22f,
    val width: Float = 44f,
    val height: Float = 48f,
    val bounceAnimTimer: Float = 0f,
    val isActive: Boolean = true
)

enum class SceneryKind {
    OAK_TREE,
    PINE_TREE,
    CACTUS,
    PALM_TREE,
    CRYSTAL,
    SPACE_BEACON,
    BATTLE_FLAG
}

data class SceneryProp(
    val id: Int,
    val x: Float,
    val y: Float,
    val kind: SceneryKind,
    val scale: Float = 1f,
    val isAlive: Boolean = true
)

data class BattleRenderSnapshot(
    val worldWidth: Float,
    val worldHeight: Float,
    val mapTheme: MapTheme,
    val cameraX: Float,
    val cameraY: Float,
    val shakeOffsetX: Float,
    val shakeOffsetY: Float,
    val animTime: Float,
    val windForce: Int,
    val turnPhase: TurnPhase,
    val activeTankIndex: Int,
    val tanks: List<TankState>,
    val projectiles: List<ProjectileState>,
    val gasClouds: List<GasCloudZone>,
    val props: List<BattlePropState>,
    val sceneryProps: List<SceneryProp>,
    val explosions: List<ExplosionEffect>,
    val particles: List<ParticleState>,
    val weatherParticles: List<WeatherParticle>,
    val popups: List<DamagePopup>,
    val chargingPower: Float,
    val muzzleFlashTimer: Float,
    val muzzleFlashPos: Offset,
    val terrainRevision: Int
)

data class AudioSettingsState(
    val bgmEnabled: Boolean = true,
    val bgmVolume: Float = 0.7f,
    val sfxEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val invitePushEnabled: Boolean = false,
    val pushOptInPrompted: Boolean = false,
    val language: AppLanguage = AppLanguage.KO
)

data class BattleHudState(
    val turnPhase: TurnPhase = TurnPhase.TITLE,
    val gameMode: GameMode = GameMode.AI_VS_PLAYER,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val mapTheme: MapTheme = MapTheme.GRASSLAND,
    val isPaused: Boolean = false,
    val showTutorial: Boolean = false,
    val showSettings: Boolean = false,
    val activeTankIndex: Int = 0,
    val turnNumber: Int = 1,
    val windForce: Int = 0,
    val turnTimeRemaining: Float = 20f,
    val chargingPower: Float = 0f,
    val selectedWeapon: WeaponType = WeaponType.STANDARD,
    val tanks: List<TankState> = listOf(
        TankState(
            id = 0,
            name = "블루 캡틴",
            isAi = false,
            x = 340f,
            y = 500f,
            facingRight = true,
            tankType = TankType.CLASSIC,
            primaryColor = PlayerBlue,
            accentColor = PlayerBlueLight
        ),
        TankState(
            id = 1,
            name = "레드 불독",
            isAi = true,
            x = 1860f,
            y = 500f,
            facingRight = false,
            tankType = TankType.MEGA_TITAN,
            primaryColor = EnemyCoral,
            accentColor = EnemyCoralLight
        )
    ),
    val localTankIndex: Int = 0,
    val isLocalPlayerTurn: Boolean = true,
    val winnerId: Int? = null,
    val statusBanner: String? = null
)
