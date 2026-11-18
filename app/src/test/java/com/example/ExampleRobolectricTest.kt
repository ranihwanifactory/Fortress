package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.engine.BattleEngine
import com.example.game.model.Difficulty
import com.example.game.model.GameMode
import com.example.game.model.ItemType
import com.example.game.model.MapTheme
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
    fun `terrain crater carves solid pixels and special weapons can be selected and fired`() {
        val engine = BattleEngine()
        engine.startNewMatch(GameMode.AI_VS_PLAYER, Difficulty.NORMAL, MapTheme.DESERT)
        val initialHud = engine.buildHudState()
        assertEquals(TurnPhase.PLAYER_AIMING, initialHud.turnPhase)
        assertEquals(MapTheme.DESERT, initialHud.mapTheme)
        assertEquals(20f, initialHud.turnTimeRemaining, 0.01f)

        val tank0 = initialHud.tanks[0]
        val groundY = tank0.y + 8f
        assertTrue(engine.terrain.isSolidWorld(tank0.x, groundY))

        engine.terrain.carveCrater(tank0.x, groundY, 40f)
        assertFalse(engine.terrain.isSolidWorld(tank0.x, groundY))

        engine.useItem(ItemType.SHIELD)
        assertTrue(engine.buildHudState().tanks[0].hasShield)

        // 신규 특수 무기 선택 및 발사 검증
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
}
