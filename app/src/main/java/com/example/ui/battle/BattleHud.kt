package com.example.ui.battle

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LooksTwo
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PestControl
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.AppLanguage
import com.example.game.model.BattleHudState
import com.example.game.model.ItemType
import com.example.game.model.TankState
import com.example.game.model.TurnPhase
import com.example.game.model.WeaponType
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepNavyOutline
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.HudSurfaceDark
import com.example.ui.theme.PrimaryGold
import com.example.ui.theme.PrimaryGoldDark
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarningAmber
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun BattleHud(
    state: BattleHudState,
    language: AppLanguage,
    onMoveInput: (Int) -> Unit,
    onAngleInput: (Int) -> Unit,
    onToggleFacing: () -> Unit,
    onSelectWeapon: (WeaponType) -> Unit,
    onUseItem: (ItemType) -> Unit,
    onStartCharge: () -> Unit,
    onReleaseFire: () -> Unit,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTank = state.tanks.getOrNull(state.activeTankIndex)
    val canControl = state.turnPhase == TurnPhase.PLAYER_AIMING && activeTank?.isAi == false && state.isLocalPlayerTurn && !state.isPaused
    val canFireRelease = state.turnPhase == TurnPhase.CHARGING && activeTank?.isAi == false && state.isLocalPlayerTurn && !state.isPaused
    val isKo = language == AppLanguage.KO

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // 1) 상단 HUD: 양쪽 탱크 HP + 중앙 턴 타이머 & 바람 계기판 + 일시정지
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            state.tanks.getOrNull(0)?.let { t0 ->
                TankHpHeaderCard(
                    tank = t0,
                    isActive = state.activeTankIndex == 0,
                    alignEnd = false,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            CenterWindAndTimerPanel(
                turnNumber = state.turnNumber,
                timeRemaining = state.turnTimeRemaining,
                windForce = state.windForce,
                language = language
            )

            Spacer(modifier = Modifier.width(6.dp))

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Top
            ) {
                state.tanks.getOrNull(1)?.let { t1 ->
                    TankHpHeaderCard(
                        tank = t1,
                        isActive = state.activeTankIndex == 1,
                        alignEnd = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(HudSurfaceDark)
                        .border(2.dp, DeepNavyOutline, RoundedCornerShape(11.dp))
                        .testTag("pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = if (isKo) "일시정지" else "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 상태 배너
        state.statusBanner?.let { banner ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 50.dp)
                    .clip(RoundedCornerShape(50))
                    .background(HudSurfaceDark)
                    .border(1.5.dp, PrimaryGold, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            ) {
                Text(
                    text = banner,
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryGold
                )
            }
        }

        // 2) 하단 조작 HUD
        if (activeTank != null && !activeTank.isAi && state.isLocalPlayerTurn &&
            (state.turnPhase == TurnPhase.PLAYER_AIMING || state.turnPhase == TurnPhase.CHARGING)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // 좌측: 전술 아이템 3종 + 이동/연료 + 각도/방향
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // 전술 아이템 3종 (수리, 연료+, 방어막)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ItemType.entries.forEach { item ->
                            val used = item in activeTank.usedItems
                            val icon = when (item) {
                                ItemType.HEAL -> Icons.Rounded.Favorite
                                ItemType.FUEL_BOOST -> Icons.Rounded.LocalGasStation
                                ItemType.SHIELD -> Icons.Rounded.Shield
                            }
                            Box(
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (used) Color(0x88131E30) else HudSurfaceDark)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (used) Color(0x44FFFFFF) else item.tintColor,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable(enabled = canControl && !used) { onUseItem(item) }
                                    .padding(horizontal = 7.dp)
                                    .testTag("item_${item.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = item.label(language),
                                        tint = if (used) TextMuted.copy(alpha = 0.45f) else item.tintColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = item.label(language),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (used) TextMuted.copy(alpha = 0.45f) else Color.White
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // 이동 & 연료 카드
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(HudSurfaceDark)
                                .border(2.dp, DeepNavyOutline, RoundedCornerShape(14.dp))
                                .padding(5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                HoldableGameButton(
                                    enabled = canControl && activeTank.fuel > 0f,
                                    onPressStart = { onMoveInput(-1) },
                                    onPressEnd = { onMoveInput(0) },
                                    containerColor = Color(0xFF284B63),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("move_left_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = if (isKo) "왼쪽 이동" else "Move Left",
                                        tint = Color.White,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                                HoldableGameButton(
                                    enabled = canControl && activeTank.fuel > 0f,
                                    onPressStart = { onMoveInput(1) },
                                    onPressEnd = { onMoveInput(0) },
                                    containerColor = Color(0xFF284B63),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("move_right_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = if (isKo) "오른쪽 이동" else "Move Right",
                                        tint = Color.White,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val fuelFrac = (activeTank.fuel / activeTank.maxFuel.coerceAtLeast(1f)).coerceIn(0f, 1f)
                            val fuelPct = (fuelFrac * 100f).roundToInt()
                            val fuelBarColor = when {
                                fuelFrac > 0.35f -> SuccessGreen
                                fuelFrac > 0.15f -> WarningAmber
                                else -> DangerRed
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.width(84.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isKo) "연료" else "Fuel",
                                        fontSize = 9.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "$fuelPct%",
                                        fontFamily = FredokaFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = fuelBarColor
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(84.dp)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF0B1320))
                                        .border(1.dp, DeepNavyOutline, RoundedCornerShape(4.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fuelFrac)
                                            .background(fuelBarColor)
                                    )
                                }
                            }
                        }

                        // 각도 조절 & 방향 전환 카드
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(HudSurfaceDark)
                                .border(2.dp, DeepNavyOutline, RoundedCornerShape(14.dp))
                                .padding(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                HoldableGameButton(
                                    enabled = canControl,
                                    onPressStart = { onAngleInput(1) },
                                    onPressEnd = { onAngleInput(0) },
                                    containerColor = Color(0xFF284B63),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("angle_up_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowUp,
                                        contentDescription = if (isKo) "각도 올리기" else "Angle Up",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                HoldableGameButton(
                                    enabled = canControl,
                                    onPressStart = { onAngleInput(-1) },
                                    onPressEnd = { onAngleInput(0) },
                                    containerColor = Color(0xFF284B63),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("angle_down_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = if (isKo) "각도 내리기" else "Angle Down",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = if (isKo) "각도" else "Angle",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "${activeTank.aimAngleDeg.roundToInt()}°",
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = PrimaryGold
                                )
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(Color(0xFF335C67))
                                        .border(1.5.dp, DeepNavyOutline, RoundedCornerShape(9.dp))
                                        .clickable(enabled = canControl, onClick = onToggleFacing)
                                        .testTag("flip_facing_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SwapHoriz,
                                        contentDescription = if (isKo) "좌우 방향 전환" else "Flip Direction",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 우측 조작부: 무기 효과 뱃지 + 7종 무기 스크롤 선택 바 + 파워 게이지/발사
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 선택된 무기 효과 안내 배지
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(HudSurfaceDark)
                            .border(1.5.dp, state.selectedWeapon.shellColor, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${state.selectedWeapon.label(language)} · ${state.selectedWeapon.effect(language)}",
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }

                    // 7종 무기 가로 스크롤 바
                    Row(
                        modifier = Modifier
                            .widthIn(max = 264.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        WeaponType.entries.forEach { weapon ->
                            val cd = activeTank.weaponCooldowns[weapon] ?: 0
                            val isSelected = state.selectedWeapon == weapon
                            val isReady = cd == 0
                            val weaponIcon = when (weapon) {
                                WeaponType.STANDARD -> Icons.Rounded.Bolt
                                WeaponType.DOUBLE -> Icons.Rounded.LooksTwo
                                WeaponType.POWER -> Icons.Rounded.Whatshot
                                WeaponType.WATER -> Icons.Rounded.WaterDrop
                                WeaponType.POOP -> Icons.Rounded.PestControl
                                WeaponType.GAS -> Icons.Rounded.Cloud
                                WeaponType.TRASH -> Icons.Rounded.DeleteOutline
                            }
                            Box(
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            !isReady -> Color(0xAA1B263B)
                                            isSelected -> PrimaryGold
                                            else -> HudSurfaceDark
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.5.dp,
                                        color = if (isSelected) Color.White else DeepNavyOutline,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable(enabled = canControl && isReady) {
                                        onSelectWeapon(weapon)
                                    }
                                    .padding(horizontal = 7.dp)
                                    .testTag("weapon_${weapon.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(weapon.shellColor)
                                            .border(1.dp, DeepNavyOutline, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = weaponIcon,
                                            contentDescription = null,
                                            tint = DeepNavyOutline,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                    Text(
                                        text = if (isReady) weapon.label(language) else "${weapon.label(language)}($cd)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) DeepNavyOutline else Color.White
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(15.dp))
                            .background(HudSurfaceDark)
                            .border(2.dp, DeepNavyOutline, RoundedCornerShape(15.dp))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            val displayPower = if (state.turnPhase == TurnPhase.CHARGING) {
                                (state.chargingPower * 100).roundToInt()
                            } else {
                                ((activeTank.lastPowerRatio ?: 0f) * 100).roundToInt()
                            }
                            Row(
                                modifier = Modifier.width(114.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = when {
                                        state.turnPhase == TurnPhase.CHARGING && isKo -> "파워 충전"
                                        state.turnPhase == TurnPhase.CHARGING -> "Charging"
                                        isKo -> "파워"
                                        else -> "Power"
                                    },
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "$displayPower%",
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = PrimaryGold
                                )
                            }

                            BoxWithConstraints(
                                modifier = Modifier
                                    .width(114.dp)
                                    .height(13.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Color(0xFF0B1320))
                                    .border(1.5.dp, DeepNavyOutline, RoundedCornerShape(7.dp))
                            ) {
                                val barWidth = maxWidth
                                val currentFill = if (state.turnPhase == TurnPhase.CHARGING) {
                                    state.chargingPower.coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(currentFill)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(SuccessGreen, PrimaryGold, DangerRed)
                                            )
                                        )
                                )
                                activeTank.lastPowerRatio?.let { prev ->
                                    Box(
                                        modifier = Modifier
                                            .offset(x = barWidth * prev.coerceIn(0.05f, 0.96f) - 1.dp)
                                            .width(2.5.dp)
                                            .fillMaxHeight()
                                            .background(Color.White)
                                    )
                                }
                            }
                        }

                        HoldableGameButton(
                            enabled = canControl || canFireRelease,
                            onPressStart = onStartCharge,
                            onPressEnd = onReleaseFire,
                            containerColor = if (state.turnPhase == TurnPhase.CHARGING) DangerRed else PrimaryGoldDark,
                            modifier = Modifier
                                .width(82.dp)
                                .height(46.dp)
                                .testTag("fire_button")
                        ) {
                            Text(
                                text = when {
                                    state.turnPhase == TurnPhase.CHARGING && isKo -> "발사!"
                                    state.turnPhase == TurnPhase.CHARGING -> "FIRE!"
                                    isKo -> "발사"
                                    else -> "FIRE"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TankHpHeaderCard(
    tank: TankState,
    isActive: Boolean,
    alignEnd: Boolean,
    modifier: Modifier = Modifier
) {
    val hpFrac = (tank.hp.toFloat() / tank.maxHp.toFloat()).coerceIn(0f, 1f)
    val borderColor by animateColorAsState(
        targetValue = if (isActive) PrimaryGold else DeepNavyOutline,
        label = "hp_card_border"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(HudSurfaceDark)
            .border(2.dp, borderColor, RoundedCornerShape(11.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = tank.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                if (tank.hasShield) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = "Shield",
                        tint = Color(0xFF4CC9F0),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Text(
                text = "${tank.hp}/${tank.maxHp}",
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = tank.accentColor
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF0B1320))
                .border(1.dp, DeepNavyOutline, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(hpFrac)
                    .background(tank.primaryColor)
            )
        }
    }
}

@Composable
private fun CenterWindAndTimerPanel(
    turnNumber: Int,
    timeRemaining: Float,
    windForce: Int,
    language: AppLanguage
) {
    val seconds = ceil(timeRemaining).toInt().coerceIn(0, 20)
    val isUrgent = seconds <= 5
    val isKo = language == AppLanguage.KO
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(HudSurfaceDark)
            .border(2.dp, DeepNavyOutline, RoundedCornerShape(12.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "TURN $turnNumber",
                fontFamily = FredokaFontFamily,
                fontSize = 9.sp,
                color = TextMuted
            )
            Text(
                text = "${seconds}s",
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (isUrgent) DangerRed else PrimaryGold
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(22.dp)
                .background(Color.White.copy(alpha = 0.18f))
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Air,
                    contentDescription = if (isKo) "바람" else "Wind",
                    tint = Color(0xFF90E0EF),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = if (isKo) "바람" else "Wind",
                    fontSize = 9.sp,
                    color = TextMuted
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (windForce < 0) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Wind Left",
                        tint = Color(0xFF4CC9F0),
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = if (windForce == 0) (if (isKo) "무풍 0" else "Calm 0") else "${abs(windForce)}",
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (windForce == 0) Color.White else Color(0xFF90E0EF)
                )
                if (windForce > 0) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Wind Right",
                        tint = Color(0xFF4CC9F0),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HoldableGameButton(
    enabled: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val latestEnabled by rememberUpdatedState(enabled)
    val latestStart by rememberUpdatedState(onPressStart)
    val latestEnd by rememberUpdatedState(onPressEnd)

    Box(
        modifier = modifier
            .scale(if (pressed) 0.94f else 1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.4f))
            .border(
                width = 2.dp,
                color = if (pressed) Color.White else DeepNavyOutline,
                shape = RoundedCornerShape(10.dp)
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    if (!latestEnabled) {
                        waitForUpOrCancellation()
                        return@awaitEachGesture
                    }
                    pressed = true
                    latestStart()
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        pressed = false
                        latestEnd()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
