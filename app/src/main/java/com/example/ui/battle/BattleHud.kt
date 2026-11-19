package com.example.ui.battle

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LooksTwo
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PestControl
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.AppLanguage
import com.example.game.model.BattleHudState
import com.example.game.model.ItemType
import com.example.game.model.TankState
import com.example.game.model.TurnPhase
import com.example.game.model.WeaponType
import com.example.ui.components.CartoonGlossyBar
import com.example.ui.components.CartoonHoldButton
import com.example.ui.components.CartoonIconButton
import com.example.ui.components.CartoonPillChip
import com.example.ui.components.CartoonStrokeText
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepNavyOutline
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.HudSurfaceDark
import com.example.ui.theme.JuaFontFamily
import com.example.ui.theme.PrimaryGold
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
        // 1) 상단 카툰 HUD: 양쪽 탱크 HP + 중앙 턴 타이머 & 바람 계기판 + 3D 일시정지 버튼
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
                Spacer(modifier = Modifier.width(6.dp))
                CartoonIconButton(
                    onClick = onPauseClick,
                    topColor = Color(0xFF476C9B),
                    bottomColor = Color(0xFF28466B),
                    lipColor = Color(0xFF13243B),
                    shape = RoundedCornerShape(12.dp),
                    depth = 3.5.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = if (isKo) "일시정지" else "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }

        // 상태 배너 (카툰 리본 스타일)
        state.statusBanner?.let { banner ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 52.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xEE10233D), Color(0xEE1B375E), Color(0xEE10233D))
                        )
                    )
                    .border(2.dp, PrimaryGold, RoundedCornerShape(50))
                    .padding(horizontal = 13.dp, vertical = 3.dp)
            ) {
                Text(
                    text = banner,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGold
                )
            }
        }

        // 2) 하단 카툰 아케이드 조작 HUD (컴팩트 크기 + 입체 글로시 조작감)
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
                    // 전술 아이템 3종 (카툰 3D 컬러 칩)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        ItemType.entries.forEach { item ->
                            val used = item in activeTank.usedItems
                            val (icon, topC, botC, lipC) = when (item) {
                                ItemType.HEAL -> Quadruple(
                                    Icons.Rounded.Favorite,
                                    Color(0xFF39E5C9), Color(0xFF15A897), Color(0xFF0A665C)
                                )
                                ItemType.FUEL_BOOST -> Quadruple(
                                    Icons.Rounded.LocalGasStation,
                                    Color(0xFF66DBFF), Color(0xFF1E9AD6), Color(0xFF0E5C8A)
                                )
                                ItemType.SHIELD -> Quadruple(
                                    Icons.Rounded.Shield,
                                    Color(0xFFC4B5FD), Color(0xFF7C5CE6), Color(0xFF472FA6)
                                )
                            }
                            CartoonPillChip(
                                selected = !used,
                                enabled = canControl && !used,
                                onClick = { onUseItem(item) },
                                selectedTopColor = topC,
                                selectedBottomColor = botC,
                                selectedLipColor = lipC,
                                borderColor = DeepNavyOutline,
                                cornerRadius = 11.dp,
                                depth = 3.dp,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("item_${item.name.lowercase()}")
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = item.label(language),
                                    tint = if (used) TextMuted else DeepNavyOutline,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = item.label(language),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (used) TextMuted else DeepNavyOutline
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // 이동 & 글로시 연료 콘솔
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.verticalGradient(listOf(Color(0xEB1A3152), Color(0xF20D1B2E)))
                                )
                                .border(2.5.dp, DeepNavyOutline, RoundedCornerShape(16.dp))
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                CartoonHoldButton(
                                    enabled = canControl && activeTank.fuel > 0f,
                                    onPressStart = { onMoveInput(-1) },
                                    onPressEnd = { onMoveInput(0) },
                                    topColor = Color(0xFF5CC8FF),
                                    bottomColor = Color(0xFF1E88E5),
                                    lipColor = Color(0xFF0D4E8C),
                                    cornerRadius = 12.dp,
                                    depth = 3.5.dp,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .testTag("move_left_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = if (isKo) "왼쪽 이동" else "Move Left",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                CartoonHoldButton(
                                    enabled = canControl && activeTank.fuel > 0f,
                                    onPressStart = { onMoveInput(1) },
                                    onPressEnd = { onMoveInput(0) },
                                    topColor = Color(0xFF5CC8FF),
                                    bottomColor = Color(0xFF1E88E5),
                                    lipColor = Color(0xFF0D4E8C),
                                    cornerRadius = 12.dp,
                                    depth = 3.5.dp,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .testTag("move_right_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = if (isKo) "오른쪽 이동" else "Move Right",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val fuelFrac = (activeTank.fuel / activeTank.maxFuel.coerceAtLeast(1f)).coerceIn(0f, 1f)
                            val fuelPct = (fuelFrac * 100f).roundToInt()
                            val fuelColor = when {
                                fuelFrac > 0.35f -> SuccessGreen
                                fuelFrac > 0.15f -> WarningAmber
                                else -> DangerRed
                            }
                            val fuelBrush = when {
                                fuelFrac > 0.35f -> Brush.horizontalGradient(listOf(Color(0xFF06D6A0), Color(0xFF52F2C8)))
                                fuelFrac > 0.15f -> Brush.horizontalGradient(listOf(Color(0xFFFF9F1C), Color(0xFFFFD166)))
                                else -> Brush.horizontalGradient(listOf(Color(0xFFD90429), Color(0xFFFF595E)))
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.width(88.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isKo) "⛽ 연료" else "⛽ Fuel",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC4DAFA)
                                    )
                                    Text(
                                        text = "$fuelPct%",
                                        fontFamily = FredokaFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = fuelColor
                                    )
                                }
                                CartoonGlossyBar(
                                    progress = fuelFrac,
                                    fillBrush = fuelBrush,
                                    height = 9.dp,
                                    cornerRadius = 5.dp,
                                    modifier = Modifier.width(88.dp)
                                )
                            }
                        }

                        // 각도 조절 & 방향 전환 카툰 콘솔
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.verticalGradient(listOf(Color(0xEB1A3152), Color(0xF20D1B2E)))
                                )
                                .border(2.5.dp, DeepNavyOutline, RoundedCornerShape(16.dp))
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                CartoonHoldButton(
                                    enabled = canControl,
                                    onPressStart = { onAngleInput(1) },
                                    onPressEnd = { onAngleInput(0) },
                                    topColor = Color(0xFF42E2B8),
                                    bottomColor = Color(0xFF14A88B),
                                    lipColor = Color(0xFF0A6352),
                                    cornerRadius = 11.dp,
                                    depth = 3.dp,
                                    modifier = Modifier
                                        .size(37.dp)
                                        .testTag("angle_up_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowUp,
                                        contentDescription = if (isKo) "각도 올리기" else "Angle Up",
                                        tint = DeepNavyOutline,
                                        modifier = Modifier.size(23.dp)
                                    )
                                }
                                CartoonHoldButton(
                                    enabled = canControl,
                                    onPressStart = { onAngleInput(-1) },
                                    onPressEnd = { onAngleInput(0) },
                                    topColor = Color(0xFF42E2B8),
                                    bottomColor = Color(0xFF14A88B),
                                    lipColor = Color(0xFF0A6352),
                                    cornerRadius = 11.dp,
                                    depth = 3.dp,
                                    modifier = Modifier
                                        .size(37.dp)
                                        .testTag("angle_down_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = if (isKo) "각도 내리기" else "Angle Down",
                                        tint = DeepNavyOutline,
                                        modifier = Modifier.size(23.dp)
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = if (isKo) "각도" else "Angle",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC4DAFA)
                                )
                                CartoonStrokeText(
                                    text = "${activeTank.aimAngleDeg.roundToInt()}°",
                                    style = TextStyle(
                                        fontFamily = FredokaFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    color = PrimaryGold,
                                    shadowOffset = 1.5.dp
                                )
                                CartoonIconButton(
                                    onClick = onToggleFacing,
                                    enabled = canControl,
                                    topColor = Color(0xFFB388FF),
                                    bottomColor = Color(0xFF7C4DFF),
                                    lipColor = Color(0xFF472599),
                                    shape = RoundedCornerShape(10.dp),
                                    depth = 3.dp,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("flip_facing_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SwapHoriz,
                                        contentDescription = if (isKo) "좌우 방향 전환" else "Flip Direction",
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 우측 조작부: 무기 효과 뱃지 + 7종 무기 3D 카툰 슬롯 + 파워 게이지 & 3D 발사 트리거
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // 선택된 무기 효과 안내 배지
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xEB0E1E33))
                            .border(2.dp, state.selectedWeapon.shellColor, RoundedCornerShape(50))
                            .padding(horizontal = 9.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${state.selectedWeapon.label(language)} · ${state.selectedWeapon.effect(language)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // 7종 무기 가로 스크롤 3D 카툰 칩 바
                    Row(
                        modifier = Modifier
                            .widthIn(max = 276.dp)
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
                            CartoonPillChip(
                                selected = isSelected,
                                enabled = canControl && isReady,
                                onClick = { onSelectWeapon(weapon) },
                                selectedTopColor = Color(0xFFFFEA70),
                                selectedBottomColor = Color(0xFFFFB703),
                                selectedLipColor = Color(0xFFAD6800),
                                unselectedTopColor = Color(0xFF28466B),
                                unselectedBottomColor = Color(0xFF182F4D),
                                unselectedLipColor = Color(0xFF0B182A),
                                borderColor = if (isSelected) Color.White else DeepNavyOutline,
                                cornerRadius = 11.dp,
                                depth = 3.dp,
                                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(35.dp)
                                    .testTag("weapon_${weapon.name.lowercase()}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(weapon.shellColor)
                                        .border(1.2.dp, DeepNavyOutline, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = weaponIcon,
                                        contentDescription = null,
                                        tint = DeepNavyOutline,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isReady) weapon.label(language) else "${weapon.label(language)}($cd)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) DeepNavyOutline else Color.White
                                )
                            }
                        }
                    }

                    // 파워 게이지 + 3D 카툰 아케이드 발사(FIRE) 버튼 콘솔
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(17.dp))
                            .background(
                                Brush.verticalGradient(listOf(Color(0xEB1A3152), Color(0xF20D1B2E)))
                            )
                            .border(2.5.dp, DeepNavyOutline, RoundedCornerShape(17.dp))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                        state.turnPhase == TurnPhase.CHARGING && isKo -> "⚡ 충전 중!"
                                        state.turnPhase == TurnPhase.CHARGING -> "⚡ Charging!"
                                        isKo -> "파워"
                                        else -> "Power"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.turnPhase == TurnPhase.CHARGING) PrimaryGold else Color(0xFFC4DAFA)
                                )
                                Text(
                                    text = "$displayPower%",
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = PrimaryGold
                                )
                            }

                            BoxWithConstraints(modifier = Modifier.width(114.dp)) {
                                val barWidth = maxWidth
                                val currentFill = if (state.turnPhase == TurnPhase.CHARGING) {
                                    state.chargingPower.coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                                CartoonGlossyBar(
                                    progress = currentFill,
                                    fillBrush = Brush.horizontalGradient(
                                        colors = listOf(SuccessGreen, PrimaryGold, DangerRed)
                                    ),
                                    height = 14.dp,
                                    cornerRadius = 7.dp,
                                    modifier = Modifier.fillMaxWidth(),
                                    overlayContent = {
                                        activeTank.lastPowerRatio?.let { prev ->
                                            Box(
                                                modifier = Modifier
                                                    .offset(x = barWidth * prev.coerceIn(0.05f, 0.96f) - 1.5.dp)
                                                    .width(3.dp)
                                                    .fillMaxHeight()
                                                    .background(Color.White)
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        val isCharging = state.turnPhase == TurnPhase.CHARGING
                        CartoonHoldButton(
                            enabled = canControl || canFireRelease,
                            onPressStart = onStartCharge,
                            onPressEnd = onReleaseFire,
                            topColor = if (isCharging) Color(0xFFFF6B6B) else Color(0xFFFFB703),
                            bottomColor = if (isCharging) Color(0xFFD90429) else Color(0xFFFF5400),
                            lipColor = if (isCharging) Color(0xFF7A0016) else Color(0xFF9C2D00),
                            borderColor = if (isCharging) Color.White else DeepNavyOutline,
                            cornerRadius = 15.dp,
                            depth = 4.5.dp,
                            modifier = Modifier
                                .width(88.dp)
                                .height(48.dp)
                                .testTag("fire_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                CartoonStrokeText(
                                    text = when {
                                        isCharging && isKo -> "발사!"
                                        isCharging -> "FIRE!"
                                        isKo -> "발사"
                                        else -> "FIRE"
                                    },
                                    style = TextStyle(
                                        fontFamily = JuaFontFamily,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White,
                                    outlineColor = Color(0xFF571300),
                                    shadowOffset = 1.5.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

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
            .clip(RoundedCornerShape(13.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xEB1A3152), Color(0xF20D1B2E)))
            )
            .border(2.5.dp, borderColor, RoundedCornerShape(13.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
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
                    fontWeight = FontWeight.Bold,
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
        CartoonGlossyBar(
            progress = hpFrac,
            fillBrush = Brush.horizontalGradient(listOf(tank.primaryColor, tank.accentColor)),
            height = 8.dp,
            cornerRadius = 5.dp,
            modifier = Modifier.fillMaxWidth()
        )
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
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xF01C3559), Color(0xF50E1E33)))
            )
            .border(2.5.dp, if (isUrgent) DangerRed else DeepNavyOutline, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "TURN $turnNumber",
                fontFamily = FredokaFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF90E0EF)
            )
            CartoonStrokeText(
                text = "${seconds}s",
                style = TextStyle(
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = if (isUrgent) DangerRed else PrimaryGold,
                shadowOffset = 1.dp
            )
        }

        Box(
            modifier = Modifier
                .width(1.5.dp)
                .height(24.dp)
                .background(Color.White.copy(alpha = 0.2f))
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
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC4DAFA)
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
