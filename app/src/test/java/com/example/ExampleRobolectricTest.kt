package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.firebase.LevelProgressionCalculator
import com.example.firebase.PlayerProfile
import com.example.firebase.RankTier
import com.example.game.engine.BattleEngine
import com.example.game.model.Difficulty
import com.example.game.model.GameMode
import com.example.game.model.ItemType
import com.example.game.model.MapTheme
import com.example.game.model.PropType
import com.example.game.model.TurnPhase
import com.example.game.model.WeaponType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("캐논 대전", appName)
    }

    @Test
    fun `terrain crater carves solid pixels and special weapons and props spawn properly`() {
        val engine = BattleEngine()
        engine.startNewMatch(GameMode.AI_VS_PLAYER, Difficulty.NORMAL, MapTheme.DESERT)
        val initialHud = engine.buildHudState()
        assertEquals(TurnPhase.PLAYER_AIMING, initialHud.turnPhase)
        assertEquals(MapTheme.DESERT, initialHud.mapTheme)
        assertEquals(20f, initialHud.turnTimeRemaining, 0.01f)

        val snapshot = engine.buildRenderSnapshot()
        val propTypes = snapshot.props.map { it.type }.toSet()
        assertTrue(PropType.TNT_BARREL in propTypes)
        assertTrue(PropType.SUPPLY_CRATE in propTypes)
        assertTrue(PropType.RICOCHET_BUMPER in propTypes)
        assertTrue(PropType.UPDRAFT_VENT in propTypes)
        assertTrue(snapshot.sceneryProps.isNotEmpty())

        val tank0 = initialHud.tanks[0]
        val groundY = tank0.y + 8f
        assertTrue(engine.terrain.isSolidWorld(tank0.x, groundY))

        engine.terrain.carveCrater(tank0.x, groundY, 40f)
        assertFalse(engine.terrain.isSolidWorld(tank0.x, groundY))

        engine.useItem(ItemType.SHIELD)
        assertTrue(engine.buildHudState().tanks[0].hasShield)

        listOf(WeaponType.WATER, WeaponType.POOP, WeaponType.GAS, WeaponType.TRASH).forEach { w ->
            engine.selectWeapon(w)
            assertEquals(w, engine.buildHudState().selectedWeapon)
        }

        engine.startCharging()
        engine.step(0.5f)
        engine.releaseAndFire()
        assertEquals(TurnPhase.PROJECTILE_FLIGHT, engine.buildHudState().turnPhase)
        assertEquals(WeaponType.TRASH, engine.buildRenderSnapshot().projectiles.first().weapon)
    }

    @Test
    fun `level progression calculator awards XP level up wins and rank tier accurately`() {
        val starter = PlayerProfile(
            id = "uid_1",
            userId = "uid_1",
            nickname = "포병대장",
            level = 1L,
            xp = 20L,
            wins = 0L,
            losses = 0L,
            rankPoints = 1020L,
            winStreak = 1L
        )
        val winResult = LevelProgressionCalculator.calculate(
            current = starter,
            won = true,
            isOnline = true,
            difficulty = Difficulty.NORMAL,
            damageDealt = 180
        )

        assertTrue(winResult.leveledUp)
        assertEquals(2L, winResult.newLevel)
        assertEquals(1L, winResult.updatedProfile.wins)
        assertEquals(1L, winResult.updatedProfile.onlineWins)
        assertEquals(2L, winResult.updatedProfile.winStreak)
        assertEquals(1055L, winResult.newRankPoints)
        assertEquals(RankTier.SILVER, winResult.updatedProfile.tier)

        val lossResult = LevelProgressionCalculator.calculate(
            current = winResult.updatedProfile,
            won = false,
            isOnline = true,
            difficulty = Difficulty.NORMAL,
            damageDealt = 45
        )
        assertEquals(1L, lossResult.updatedProfile.losses)
        assertEquals(1L, lossResult.updatedProfile.onlineLosses)
        assertEquals(0L, lossResult.updatedProfile.winStreak)
        assertTrue(lossResult.xpGained > 0L)
        assertTrue(lossResult.rpDelta < 0L)
    }

    @Test
    fun `online match deterministic seed produces identical battlefield for host and guest`() {
        val hostEngine = BattleEngine()
        val guestEngine = BattleEngine()
        val sharedSeed = 987654321L

        hostEngine.startOnlineMatch(
            seed = sharedSeed,
            theme = MapTheme.VOLCANO,
            hostName = "호스트",
            guestName = "게스트",
            localIsHost = true,
            initialWindForce = -4
        )
        guestEngine.startOnlineMatch(
            seed = sharedSeed,
            theme = MapTheme.VOLCANO,
            hostName = "호스트",
            guestName = "게스트",
            localIsHost = false,
            initialWindForce = -4
        )

        val hostHud = hostEngine.buildHudState()
        val guestHud = guestEngine.buildHudState()

        assertEquals(GameMode.ONLINE_PVP, hostHud.gameMode)
        assertEquals(0, hostHud.localTankIndex)
        assertEquals(1, guestHud.localTankIndex)
        assertTrue(hostHud.isLocalPlayerTurn)
        assertFalse(guestHud.isLocalPlayerTurn)
        assertEquals(-4, hostHud.windForce)
        assertEquals(-4, guestHud.windForce)
        assertEquals(hostHud.tanks[0].x, guestHud.tanks[0].x, 0.01f)
        assertEquals(hostHud.tanks[0].y, guestHud.tanks[0].y, 0.01f)
        assertEquals(hostHud.tanks[1].x, guestHud.tanks[1].x, 0.01f)
        assertEquals(hostHud.tanks[1].y, guestHud.tanks[1].y, 0.01f)

        val hostProps = hostEngine.buildRenderSnapshot().props
        val guestProps = guestEngine.buildRenderSnapshot().props
        assertEquals(hostProps.size, guestProps.size)
        hostProps.zip(guestProps).forEach { (hp, gp) ->
            assertEquals(hp.type, gp.type)
            assertEquals(hp.x, gp.x, 0.01f)
            assertEquals(hp.y, gp.y, 0.01f)
        }
    }
}
