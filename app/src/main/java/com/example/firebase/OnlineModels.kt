package com.example.firebase

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.example.game.model.AppLanguage
import com.example.game.model.Difficulty
import com.example.game.model.ItemType
import com.example.game.model.MapTheme
import com.example.game.model.WeaponType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.max

enum class RankTier(
    val labelKo: String,
    val labelEn: String,
    val minPoints: Long,
    val badgeColor: Color
) {
    BRONZE("브론즈", "Bronze", 0L, Color(0xFFCD7F32)),
    SILVER("실버", "Silver", 1050L, Color(0xFFC0C0C0)),
    GOLD("골드", "Gold", 1180L, Color(0xFFFFD166)),
    PLATINUM("플래티넘", "Platinum", 1350L, Color(0xFF4CC9F0)),
    DIAMOND("다이아", "Diamond", 1550L, Color(0xFFA78BFA)),
    MASTER("그랜드마스터", "Master", 1800L, Color(0xFFFF4D6D));

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.KO) labelKo else labelEn

    companion object {
        fun fromPoints(points: Long): RankTier {
            return entries.lastOrNull { points >= it.minPoints } ?: BRONZE
        }
    }
}

@Keep
@IgnoreExtraProperties
data class PlayerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val nickname: String = "전차병",
    val listedInRanking: Boolean = true,
    val level: Long = 1L,
    val xp: Long = 0L,
    val totalXp: Long = 0L,
    val wins: Long = 0L,
    val losses: Long = 0L,
    val onlineWins: Long = 0L,
    val onlineLosses: Long = 0L,
    val rankPoints: Long = 1000L,
    val winStreak: Long = 0L,
    val bestWinStreak: Long = 0L,
    val totalDamage: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val winRatePercent: Int
        get() {
            val total = wins + losses
            return if (total <= 0L) 0 else ((wins * 100L) / total).toInt()
        }

    val tier: RankTier
        get() = RankTier.fromPoints(rankPoints)

    val xpNeededForNextLevel: Long
        get() = xpRequiredForLevel(level)

    val levelHpBonus: Int
        get() = ((level - 1L).coerceAtLeast(0L) * 3L).coerceAtMost(45L).toInt()

    companion object {
        fun xpRequiredForLevel(level: Long): Long = 100L + (level.coerceAtLeast(1L) - 1L) * 60L
    }
}

data class MatchProgressionResult(
    val won: Boolean,
    val isOnline: Boolean,
    val oldLevel: Long,
    val newLevel: Long,
    val oldXp: Long,
    val newXp: Long,
    val xpNeededNext: Long,
    val xpGained: Long,
    val rpDelta: Long,
    val newRankPoints: Long,
    val updatedProfile: PlayerProfile
) {
    val leveledUp: Boolean get() = newLevel > oldLevel
}

object LevelProgressionCalculator {
    fun calculate(
        current: PlayerProfile,
        won: Boolean,
        isOnline: Boolean,
        difficulty: Difficulty,
        damageDealt: Int
    ): MatchProgressionResult {
        val baseXp = when {
            isOnline && won -> 125L
            isOnline && !won -> 45L
            won && difficulty == Difficulty.HARD -> 105L
            won && difficulty == Difficulty.NORMAL -> 80L
            won -> 60L
            else -> 25L
        }
        val damageBonusXp = (damageDealt / 15).toLong().coerceIn(0L, 35L)
        val nextStreak = if (won) current.winStreak + 1L else 0L
        val streakBonusXp = if (won && nextStreak >= 2L) (nextStreak * 10L).coerceAtMost(40L) else 0L
        val xpGained = baseXp + damageBonusXp + streakBonusXp

        val rpDelta = when {
            isOnline && won -> 35L + if (nextStreak >= 3L) 5L else 0L
            isOnline && !won -> -18L
            won && difficulty == Difficulty.HARD -> 20L
            won && difficulty == Difficulty.NORMAL -> 14L
            won -> 10L
            else -> -8L
        }
        val newRankPoints = max(0L, current.rankPoints + rpDelta).coerceAtMost(999999L)

        var lv = current.level.coerceAtLeast(1L)
        var curXp = current.xp + xpGained
        while (lv < 999L && curXp >= PlayerProfile.xpRequiredForLevel(lv)) {
            curXp -= PlayerProfile.xpRequiredForLevel(lv)
            lv++
        }

        val updated = current.copy(
            level = lv,
            xp = curXp.coerceAtMost(1000000L),
            totalXp = (current.totalXp + xpGained).coerceAtMost(100000000L),
            wins = current.wins + if (won) 1L else 0L,
            losses = current.losses + if (!won) 1L else 0L,
            onlineWins = current.onlineWins + if (isOnline && won) 1L else 0L,
            onlineLosses = current.onlineLosses + if (isOnline && !won) 1L else 0L,
            rankPoints = newRankPoints,
            winStreak = nextStreak,
            bestWinStreak = max(current.bestWinStreak, nextStreak),
            totalDamage = (current.totalDamage + damageDealt.coerceAtLeast(0).toLong()).coerceAtMost(1000000000L)
        )

        return MatchProgressionResult(
            won = won,
            isOnline = isOnline,
            oldLevel = current.level,
            newLevel = lv,
            oldXp = current.xp,
            newXp = updated.xp,
            xpNeededNext = PlayerProfile.xpRequiredForLevel(lv),
            xpGained = xpGained,
            rpDelta = rpDelta,
            newRankPoints = newRankPoints,
            updatedProfile = updated
        )
    }
}

@Keep
@IgnoreExtraProperties
data class MatchRoom(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val roomTitle: String = "",
    val hostUid: String = "",
    val hostName: String = "",
    val hostLevel: Long = 1L,
    val guestUid: String = "",
    val guestName: String = "",
    val guestLevel: Long = 0L,
    val guestReady: Boolean = false,
    val participantUids: List<String> = emptyList(),
    val mapTheme: String = MapTheme.GRASSLAND.name,
    val terrainSeed: Long = 1L,
    val status: String = "WAITING",
    val activeTurnUid: String = "",
    val turnSeq: Long = 0L,
    val windForce: Long = 0L,
    val lastActionType: String = "NONE",
    val lastActionUid: String = "",
    val lastTankX: Double = 320.0,
    val lastTankY: Double = 500.0,
    val lastFacingRight: Boolean = true,
    val lastAimAngle: Double = 45.0,
    val lastPower: Double = 0.5,
    val lastWeapon: String = WeaponType.STANDARD.name,
    val lastUsedItem: String = "",
    val winnerUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedMapTheme(): MapTheme =
        MapTheme.entries.firstOrNull { it.name == mapTheme } ?: MapTheme.GRASSLAND

    fun parsedWeapon(): WeaponType =
        WeaponType.entries.firstOrNull { it.name == lastWeapon } ?: WeaponType.STANDARD

    fun parsedUsedItem(): ItemType? =
        ItemType.entries.firstOrNull { it.name == lastUsedItem }
}

@Keep
@IgnoreExtraProperties
data class RoomInvite(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val roomCode: String = "",
    val roomTitle: String = "",
    val mapTheme: String = MapTheme.GRASSLAND.name,
    val fromUid: String = "",
    val fromName: String = "",
    val toUid: String = "",
    val toName: String = "",
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedMapTheme(): MapTheme =
        MapTheme.entries.firstOrNull { it.name == mapTheme } ?: MapTheme.GRASSLAND
}

sealed interface OnlineUiState<out T> {
    data object Loading : OnlineUiState<Nothing>
    data class Success<T>(val data: T) : OnlineUiState<T>
    data class Error(val message: String) : OnlineUiState<Nothing>
}
