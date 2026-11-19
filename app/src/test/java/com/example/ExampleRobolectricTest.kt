package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.firebase.LevelProgressionCalculator
import com.example.firebase.PlayerProfile
import com.example.firebase.RankTier
import com.example.firebase.RoomInvite
import com.example.game.engine.BattleEngine
import com.example.game.model.AppLanguage
import com.example.game.model.Difficulty
import com.example.game.model.GameMode
import com.example.game.model.ItemType
import com.example.game.model.MapTheme
import com.example.game.model.PropType
import com.example.game.model.TankType
import com.example.game.model.TurnPhase
import com.example.game.model.WeaponType
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.notifications.InviteNotificationManager
import com.example.ui.battle.MapSelectCarouselOverlay
import com.example.ui.battle.description
import com.example.ui.battle.featureTags
import com.example.ui.battle.previewDrawableRes
import com.example.ui.online.renderRankingSnapshotBitmap
import com.example.ui.theme.CannonBattleTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h360dp-land-xhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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

    @Test
    fun `ranking snapshot renderer creates 1280x720 bitmap for TOP 1 and standard ranks`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val topOnePlayer = PlayerProfile(
            id = "uid_champ",
            userId = "uid_champ",
            nickname = "구지초챔피언",
            level = 9L,
            wins = 24L,
            losses = 3L,
            onlineWins = 15L,
            onlineLosses = 2L,
            rankPoints = 1860L,
            bestWinStreak = 8L,
            selectedTank = TankType.POOP_KING.name
        )
        val runnerUp = PlayerProfile(
            id = "uid_2",
            userId = "uid_2",
            nickname = "포탄장인",
            level = 7L,
            wins = 18L,
            losses = 6L,
            rankPoints = 1520L,
            selectedTank = TankType.AQUA_HYDRO.name
        )
        val thirdPlace = PlayerProfile(
            id = "uid_3",
            userId = "uid_3",
            nickname = "도형캠프",
            level = 5L,
            wins = 12L,
            losses = 5L,
            rankPoints = 1310L,
            selectedTank = TankType.TOXIC_CHEMIST.name
        )
        val fourthPlace = PlayerProfile(
            id = "uid_4",
            userId = "uid_4",
            nickname = "강철타이탄",
            level = 4L,
            wins = 9L,
            losses = 4L,
            rankPoints = 1210L,
            selectedTank = TankType.MEGA_TITAN.name
        )
        val fifthPlace = PlayerProfile(
            id = "uid_5",
            userId = "uid_5",
            nickname = "고철대장",
            level = 3L,
            wins = 7L,
            losses = 5L,
            rankPoints = 1120L,
            selectedTank = TankType.JUNK_SCRAPPER.name
        )
        val leaderboard = listOf(topOnePlayer, runnerUp, thirdPlace, fourthPlace, fifthPlace)

        val topOneBitmap = renderRankingSnapshotBitmap(
            context = context,
            language = AppLanguage.KO,
            myProfile = topOnePlayer,
            myRank = 1,
            topPlayers = leaderboard
        )
        assertNotNull(topOneBitmap)
        assertEquals(1280, topOneBitmap.width)
        assertEquals(720, topOneBitmap.height)
        assertFalse(topOneBitmap.isRecycled)
        topOneBitmap.recycle()

        val standardBitmap = renderRankingSnapshotBitmap(
            context = context,
            language = AppLanguage.EN,
            myProfile = thirdPlace,
            myRank = 3,
            topPlayers = leaderboard
        )
        assertEquals(1280, standardBitmap.width)
        assertEquals(720, standardBitmap.height)
        standardBitmap.recycle()
    }

    @Test
    fun `every weapon type has a dedicated tank type with passives applied in battle`() {
        val weaponSet = WeaponType.entries.toSet()
        val tankWeaponSet = TankType.entries.map { it.signatureWeapon }.toSet()
        assertEquals(weaponSet, tankWeaponSet)
        assertEquals(7, TankType.entries.size)

        WeaponType.entries.forEach { weapon ->
            val mappedTank = TankType.forWeapon(weapon)
            assertEquals(weapon, mappedTank.signatureWeapon)
            assertEquals(mappedTank, TankType.fromNameOrDefault(mappedTank.name))
        }

        val engine = BattleEngine()
        engine.startNewMatch(
            mode = GameMode.AI_VS_PLAYER,
            diff = Difficulty.NORMAL,
            theme = MapTheme.GRASSLAND,
            p1TankType = TankType.MEGA_TITAN
        )
        val hud = engine.buildHudState()
        assertEquals(TankType.MEGA_TITAN, hud.tanks[0].tankType)
        assertEquals(100 + TankType.MEGA_TITAN.bonusHp, hud.tanks[0].maxHp)
        assertEquals(WeaponType.POWER, hud.selectedWeapon)
    }

    @Test
    fun `invite notification channel is created with high importance and cancels cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        InviteNotificationManager.ensureChannel(context, AppLanguage.KO)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = nm.getNotificationChannel(InviteNotificationManager.CHANNEL_ID_INVITES)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)

        val invite = RoomInvite(
            id = "inv_test_1",
            roomId = "room_123",
            roomCode = "A1B2C3",
            roomTitle = "친선 한판!",
            mapTheme = MapTheme.ISLAND.name,
            fromUid = "uid_friend",
            fromName = "포탄친구",
            toUid = "uid_me",
            toName = "나"
        )
        // Should not throw even when runtime permission is not granted in test environment
        InviteNotificationManager.showInviteNotification(context, invite, AppLanguage.KO)
        InviteNotificationManager.cancelInviteNotification(context, invite.id)
    }

    @Test
    fun `every map theme resolves a unique full-screen preview drawable and carousel metadata`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawableIds = MapTheme.entries.map { it.previewDrawableRes() }
        assertEquals(6, MapTheme.entries.size)
        assertEquals(6, drawableIds.toSet().size)

        MapTheme.entries.forEach { map ->
            val resId = map.previewDrawableRes()
            assertTrue(resId != 0)
            assertNotNull(context.resources.getResourceEntryName(resId))
            assertTrue(map.description(AppLanguage.KO).isNotBlank())
            assertTrue(map.description(AppLanguage.EN).isNotBlank())
            assertTrue(map.featureTags(AppLanguage.KO).size >= 3)
            assertTrue(map.featureTags(AppLanguage.EN).size >= 3)
        }
    }

    @Test
    fun `map select carousel displays non-zero cartoon start and back buttons and launches game on click`() {
        var confirmedMap: MapTheme? = null
        composeTestRule.setContent {
            CannonBattleTheme {
                MapSelectCarouselOverlay(
                    initialMap = MapTheme.DESERT,
                    selectedTank = TankType.MEGA_TITAN,
                    modeLabel = "AI 대전 · 보통",
                    confirmLabel = "게임 시작!",
                    isBattleLaunch = true,
                    language = AppLanguage.KO,
                    onMapFocused = {},
                    onConfirmMap = { confirmedMap = it },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("map_select_back_button")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)

        composeTestRule.onNodeWithTag("confirm_map_and_start_button")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(150.dp)
            .performClick()

        composeTestRule.waitForIdle()
        assertEquals(MapTheme.DESERT, confirmedMap)
    }
}
