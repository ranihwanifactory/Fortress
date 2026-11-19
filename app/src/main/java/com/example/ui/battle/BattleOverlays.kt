package com.example.ui.battle

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.firebase.MatchProgressionResult
import com.example.firebase.PlayerProfile
import com.example.game.model.AppLanguage
import com.example.game.model.AudioSettingsState
import com.example.game.model.BattleHudState
import com.example.game.model.Difficulty
import com.example.game.model.GameMode
import com.example.game.model.MapTheme
import com.example.game.model.TankType
import com.example.ui.tank.TankAvatar
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepNavyOutline
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.HudSurfaceDark
import com.example.ui.theme.JuaFontFamily
import com.example.ui.theme.PlayerBlue
import com.example.ui.theme.PrimaryGold
import com.example.ui.theme.PrimaryGoldDark
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 레퍼런스 일러스트의 밝고 박진감 넘치는 카툰 스타일(푸른 하늘 + 성탑 + 구름 + 포탄 궤적 + 입체 3D 타이틀)을 반영한
 * 오리지널 '캐논 대전' 타이틀 화면.
 */
@Composable
fun TitleOverlay(
    selectedDifficulty: Difficulty,
    selectedMap: MapTheme,
    selectedTank: TankType,
    language: AppLanguage,
    profile: PlayerProfile?,
    pendingInviteCount: Int,
    onSelectDifficulty: (Difficulty) -> Unit,
    onSelectMap: (MapTheme) -> Unit,
    onOpenTankSelector: () -> Unit,
    onStartAiMatch: () -> Unit,
    onStartLocalMatch: () -> Unit,
    onOpenOnlineLobby: () -> Unit,
    onOpenRankings: () -> Unit,
    onSignOut: () -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isKo = language == AppLanguage.KO
    val infiniteTransition = rememberInfiniteTransition(label = "title_anim")
    val shellProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shell_arc"
    )
    val cloudDrift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloud_drift"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // 1) 애니메이션 카툰 일러스트 배경 (성탑 + 하늘 + 흰 포물선 꼬리 + 폭발)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(selectedMap.skyTop, selectedMap.skyMid, selectedMap.skyBottom)
                ),
                size = size
            )

            // 구름 레이어
            val driftPx = cloudDrift * 120f
            listOf(0.14f to 0.16f, 0.48f to 0.12f, 0.72f to 0.22f).forEachIndexed { idx, (fx, fy) ->
                val cx = ((w * fx + driftPx * (if (idx % 2 == 0) 1f else -0.6f)) % (w + 180f))
                val cy = h * fy
                drawCircle(Color.White.copy(alpha = 0.88f), radius = 34f, center = Offset(cx, cy))
                drawCircle(Color.White.copy(alpha = 0.88f), radius = 44f, center = Offset(cx + 38f, cy - 12f))
                drawCircle(Color.White.copy(alpha = 0.88f), radius = 32f, center = Offset(cx + 76f, cy + 4f))
            }

            // 우측 성곽 망루 실루엣 (레퍼런스 이미지 구도 오마주)
            val towerX = w * 0.81f
            val towerTop = h * 0.24f
            val towerColor = Color(0xFF9C8E75).copy(alpha = 0.55f)
            drawRect(color = towerColor, topLeft = Offset(towerX, towerTop), size = Size(w * 0.16f, h * 0.65f))
            for (b in 0..2) {
                drawRect(
                    color = towerColor,
                    topLeft = Offset(towerX + b * (w * 0.056f), towerTop - 26f),
                    size = Size(w * 0.036f, 26f)
                )
            }

            // 언덕 지형
            val hillPath = Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.72f)
                quadraticTo(w * 0.28f, h * 0.58f, w * 0.55f, h * 0.76f)
                quadraticTo(w * 0.8f, h * 0.86f, w, h * 0.68f)
                lineTo(w, h)
                close()
            }
            drawPath(path = hillPath, color = selectedMap.midHillColor)

            // 아치형 포탄 흰 연기 궤적 & 날아가는 불꽃 포탄
            val p0 = Offset(w * 0.84f, h * 0.34f)
            val ctrl = Offset(w * 0.52f, h * 0.08f)
            val p2 = Offset(w * 0.27f, h * 0.63f)

            val arcPath = Path().apply {
                moveTo(p0.x, p0.y)
                quadraticTo(ctrl.x, ctrl.y, p2.x, p2.y)
            }
            drawPath(
                path = arcPath,
                color = Color.White.copy(alpha = 0.75f),
                style = Stroke(width = 16f, cap = StrokeCap.Round)
            )

            // 착탄 지점 카툰 폭발 스타버스트
            val burstCenter = Offset(w * 0.27f, h * 0.63f)
            val burstPath = Path()
            for (i in 0 until 16) {
                val r = if (i % 2 == 0) 68f else 32f
                val a = (i / 16f) * (2f * PI.toFloat())
                val px = burstCenter.x + cos(a) * r
                val py = burstCenter.y + sin(a) * r
                if (i == 0) burstPath.moveTo(px, py) else burstPath.lineTo(px, py)
            }
            burstPath.close()
            drawPath(burstPath, color = Color(0xFFFF7B00))

            // 궤적을 따라 날아가는 불꽃 탄환
            val t = shellProgress
            val omt = 1f - t
            val sx = omt * omt * p0.x + 2 * omt * t * ctrl.x + t * t * p2.x
            val sy = omt * omt * p0.y + 2 * omt * t * ctrl.y + t * t * p2.y
            drawCircle(color = Color(0xFFFF9F1C).copy(alpha = 0.45f), radius = 18f, center = Offset(sx, sy))
            drawCircle(color = PrimaryGold, radius = 9f, center = Offset(sx, sy))
            drawCircle(color = Color.White, radius = 4f, center = Offset(sx, sy))
        }

        // 2) Safe Area 내부 좌/우 2열 타이틀 UI
        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 좌측: 사령관 프로필 배지 + 입체 카툰 로고 + 대표 탱크 엠블럼
            Column(
                modifier = Modifier.weight(0.95f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 사령관 프로필 & 레벨/랭크 카드
                if (profile != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xDD0D1B2A))
                            .border(2.dp, profile.tier.badgeColor, RoundedCornerShape(16.dp))
                            .clickable { onOpenRankings() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("commander_profile_pill"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryGold)
                                .border(1.5.dp, DeepNavyOutline, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "LV.${profile.level}",
                                fontFamily = FredokaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = DeepNavyOutline
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = profile.nickname,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${profile.tier.label(language)} · ${profile.rankPoints}RP",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = profile.tier.badgeColor,
                                    maxLines = 1
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val xpRatio = (profile.xp.toFloat() / profile.xpNeededForNextLevel.coerceAtLeast(1L).toFloat())
                                    .coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { xpRatio },
                                    color = SuccessGreen,
                                    trackColor = Color(0xFF1F3452),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50))
                                )
                                Text(
                                    text = if (isKo) "${profile.wins}승 ${profile.losses}패" else "${profile.wins}W ${profile.losses}L",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF23395B))
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = if (isKo) "로그아웃" else "Sign out",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 입체 3D 카툰 로고 ("캐논 대전" + 리벳 그린 V2 배지)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        // 외곽 짙은 갈색 3D 그림자 레이어
                        Text(
                            text = if (isKo) "캐논 대전" else "CANNON",
                            style = TextStyle(
                                fontFamily = JuaFontFamily,
                                fontSize = 42.sp,
                                color = Color(0xFF4A1500)
                            ),
                            modifier = Modifier.offset(x = 3.dp, y = 4.dp)
                        )
                        // 흰색 스티커 아웃라인 느낌 레이어
                        Text(
                            text = if (isKo) "캐논 대전" else "CANNON",
                            style = TextStyle(
                                fontFamily = JuaFontFamily,
                                fontSize = 42.sp,
                                color = Color.White
                            ),
                            modifier = Modifier.offset(x = (-1.5).dp, y = (-1.5).dp)
                        )
                        // 메인 오렌지-골드 그라데이션 텍스트
                        Text(
                            text = if (isKo) "캐논 대전" else "CANNON",
                            style = TextStyle(
                                fontFamily = JuaFontFamily,
                                fontSize = 42.sp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFFE600), Color(0xFFFF7B00), Color(0xFFE63946))
                                )
                            )
                        )
                    }

                    // 레퍼런스 이미지 우측 리벳 그린 배지 스타일 ("V2")
                    Box(
                        modifier = Modifier
                            .rotate(-6f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF38F9D7), Color(0xFF00B45A))
                                )
                            )
                            .border(3.dp, DeepNavyOutline, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "V2",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = DeepNavyOutline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 내 출격 탱크 쇼케이스 카드 (터치 시 7종 포탄별 탱크 격납고 열기)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xDD0D1B2A))
                        .border(2.dp, selectedTank.trimColor, RoundedCornerShape(18.dp))
                        .clickable { onOpenTankSelector() }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("open_tank_selector_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TankAvatar(
                        tankType = selectedTank,
                        size = 64.dp,
                        showPedestal = true,
                        highlighted = true
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = selectedTank.label(language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${selectedTank.signatureWeapon.label(language)} · ${selectedTank.perk(language)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = selectedTank.trimColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryGold)
                            .border(1.5.dp, DeepNavyOutline, RoundedCornerShape(10.dp))
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isKo) "탱크 변경" else "Tanks",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavyOutline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xDD132238))
                        .border(1.5.dp, PrimaryGold, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                        .testTag("creator_credit_badge")
                ) {
                    Text(
                        text = if (isKo) "제작 · 구지초도형캠프" else "Created by 구지초도형캠프",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 우측: 맵 6종 선택 + 난이도 + 플레이 버튼 카드
            Column(
                modifier = Modifier
                    .weight(1.12f)
                    .widthIn(max = 460.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(HudSurfaceDark)
                    .border(3.dp, Color.White, RoundedCornerShape(26.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 상단: 타이틀 & 설정 아이콘 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isKo) "전장 맵 선택 (6종)" else "Select Battlefield (6)",
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryGold
                    )
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF23395B))
                            .border(2.dp, DeepNavyOutline, RoundedCornerShape(12.dp))
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = if (isKo) "설정" else "Settings",
                            tint = Color.White
                        )
                    }
                }

                // 6종 맵 가로 스크롤 선택기
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MapTheme.entries.forEach { map ->
                        val selected = map == selectedMap
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) PrimaryGold else Color(0xFF23395B))
                                .border(
                                    width = 2.dp,
                                    color = if (selected) Color.White else DeepNavyOutline,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectMap(map) }
                                .padding(horizontal = 10.dp)
                                .testTag("map_${map.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(map.skyTop)
                                        .border(1.5.dp, DeepNavyOutline, CircleShape)
                                )
                                Text(
                                    text = map.label(language),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selected) DeepNavyOutline else Color.White
                                )
                            }
                        }
                    }
                }

                // AI 난이도 선택
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Difficulty.entries.forEach { diff ->
                        val selected = diff == selectedDifficulty
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) SuccessGreen else Color(0xFF23395B))
                                .border(
                                    width = 2.dp,
                                    color = if (selected) Color.White else DeepNavyOutline,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectDifficulty(diff) }
                                .testTag("difficulty_${diff.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = diff.label(language),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selected) DeepNavyOutline else Color.White
                            )
                        }
                    }
                }

                // 온라인 멀티 대기실 & 랭킹 버튼 (핵심 멀티플레이 진입점)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenOnlineLobby,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen,
                            contentColor = DeepNavyOutline
                        ),
                        shape = RoundedCornerShape(15.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .border(2.5.dp, DeepNavyOutline, RoundedCornerShape(15.dp))
                            .testTag("open_online_lobby_button")
                    ) {
                        Icon(Icons.Rounded.Public, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isKo) "온라인 멀티 대전" else "Online PvP",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        if (pendingInviteCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(DangerRed)
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "$pendingInviteCount",
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onOpenRankings,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7B2CBF),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(15.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(50.dp)
                            .border(2.dp, DeepNavyOutline, RoundedCornerShape(15.dp))
                            .testTag("open_rankings_button")
                    ) {
                        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = PrimaryGold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isKo) "랭킹·레벨" else "Rankings",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1
                        )
                    }
                }

                // AI 대전 & 로컬 2인 대전 & 조작법
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onStartAiMatch,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGoldDark,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(48.dp)
                            .border(2.dp, DeepNavyOutline, RoundedCornerShape(14.dp))
                            .testTag("start_ai_button")
                    ) {
                        Icon(Icons.Rounded.SmartToy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isKo) "AI 대전" else "vs AI",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = onStartLocalMatch,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PlayerBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("start_local_button")
                    ) {
                        Icon(Icons.Rounded.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isKo) "2인 대전" else "Local 2P",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenTutorial,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("tutorial_button")
                    ) {
                        Icon(Icons.Rounded.HelpOutline, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isKo) "가이드" else "Guide",
                            style = MaterialTheme.typography.labelLarge,
                            color = PrimaryGold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsOverlay(
    settings: AudioSettingsState,
    onToggleBgm: (Boolean) -> Unit,
    onBgmVolumeChange: (Float) -> Unit,
    onToggleSfx: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onToggleInvitePush: (Boolean) -> Unit,
    pushPermissionHint: String? = null,
    onSelectLanguage: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isKo = settings.language == AppLanguage.KO
    val volumePct = (settings.bgmVolume * 100f).roundToInt().coerceIn(0, 100)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xBB08101C))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 450.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(HudSurfaceDark)
                .border(3.dp, PrimaryGold, RoundedCornerShape(24.dp))
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isKo) "게임 설정" else "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PrimaryGold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            SettingsSwitchRow(
                label = if (isKo) "배경음악 (BGM)" else "Background Music",
                checked = settings.bgmEnabled,
                onCheckedChange = onToggleBgm
            )

            // 배경음악 볼륨 슬라이더
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF102036))
                    .border(1.5.dp, Color(0xFF284B63), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = if (isKo) "배경음악 볼륨" else "BGM Volume",
                            tint = if (settings.bgmEnabled) PrimaryGold else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isKo) "배경음악 볼륨" else "BGM Volume",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (settings.bgmEnabled) Color.White else TextMuted
                        )
                    }
                    Text(
                        text = if (settings.bgmEnabled) "$volumePct%" else (if (isKo) "음소거" else "Muted"),
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (settings.bgmEnabled) PrimaryGold else TextMuted
                    )
                }
                Slider(
                    value = if (settings.bgmEnabled) settings.bgmVolume else 0f,
                    onValueChange = onBgmVolumeChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryGold,
                        activeTrackColor = PrimaryGold,
                        inactiveTrackColor = Color(0xFF23395B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bgm_volume_slider")
                )
            }

            SettingsSwitchRow(
                label = if (isKo) "효과음 (SFX)" else "Sound Effects",
                checked = settings.sfxEnabled,
                onCheckedChange = onToggleSfx
            )
            SettingsSwitchRow(
                label = if (isKo) "진동 피드백" else "Vibration Haptics",
                checked = settings.vibrationEnabled,
                onCheckedChange = onToggleVibration
            )

            // 친구 초대 푸시 알림 선택 토글 카드
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF102036))
                    .border(
                        width = 1.5.dp,
                        color = if (settings.invitePushEnabled) PrimaryGold.copy(alpha = 0.75f) else Color(0xFF284B63),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (settings.invitePushEnabled) {
                                Icons.Rounded.NotificationsActive
                            } else {
                                Icons.Rounded.NotificationsOff
                            },
                            contentDescription = if (isKo) "친구 초대 푸시 알림" else "Friend Invite Push Alerts",
                            tint = if (settings.invitePushEnabled) PrimaryGold else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (isKo) "친구 초대 푸시 알림" else "Friend Invite Push Alerts",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            Text(
                                text = if (settings.invitePushEnabled) {
                                    if (isKo) "친구가 초대하면 푸시 알림으로 즉시 안내" else "Instant push alert when friends invite you"
                                } else {
                                    if (isKo) "꺼짐 · 초대함에서 직접 확인할 수 있어요" else "Off · Check invites manually in Lobby"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (settings.invitePushEnabled) Color(0xFF90E0EF) else TextMuted
                            )
                        }
                    }
                    Switch(
                        checked = settings.invitePushEnabled,
                        onCheckedChange = onToggleInvitePush,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepNavyOutline,
                            checkedTrackColor = PrimaryGold
                        ),
                        modifier = Modifier.testTag("settings_invite_push_switch")
                    )
                }
                if (!pushPermissionHint.isNullOrBlank()) {
                    Text(
                        text = pushPermissionHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFB703)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isKo) "언어 / Language" else "Language",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        val selected = settings.language == lang
                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) PrimaryGold else Color(0xFF23395B))
                                .border(2.dp, DeepNavyOutline, RoundedCornerShape(10.dp))
                                .clickable { onSelectLanguage(lang) }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lang.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selected) DeepNavyOutline else Color.White
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF102036))
                    .border(1.5.dp, PrimaryGold.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isKo) "제작자" else "Creator",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
                Text(
                    text = "구지초도형캠프",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGold
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, color = Color.White)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DeepNavyOutline,
                checkedTrackColor = PrimaryGold
            )
        )
    }
}

@Composable
fun TutorialOverlay(
    language: AppLanguage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isKo = language == AppLanguage.KO
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xBB08101C))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(HudSurfaceDark)
                .border(3.dp, PlayerBlue, RoundedCornerShape(24.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isKo) "전투 조작법 가이드" else "How to Play",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PrimaryGold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (isKo) {
                TutorialBullet("1. 이동 & 연료", "좌측 ◀ ▶ 버튼으로 언덕을 오르내릴 수 있습니다. 매 턴 연료가 충전됩니다.")
                TutorialBullet("2. 각도 & 방향", "▲ ▼ 버튼으로 포신 각도(0°~90°)를 맞추고, ⇄ 버튼으로 좌우를 전환합니다.")
                TutorialBullet("3. 발사 파워 & 바람", "발사 버튼을 길게 눌러 게이지를 채운 뒤 손을 떼세요! 상단 바람 화살표를 참고하세요.")
                TutorialBullet("4. 전술 아이템 & 지형 파괴", "수리·연료+·방어막 아이템을 전투당 1회씩 사용할 수 있으며, 지형을 부숴 상대를 추락시킬 수 있습니다!")
                TutorialBullet("5. 지형지물 & 기믹 활용", "중앙 성벽과 부유 발판을 부수고, TNT 화약통(연쇄 폭발)·보급 상자·도탄 범퍼·상승 기류를 전략적으로 활용하세요!")
            } else {
                TutorialBullet("1. Move & Fuel", "Hold ◀ ▶ on the bottom left to drive across hills. Fuel refills each turn.")
                TutorialBullet("2. Angle & Facing", "Adjust cannon angle (0°~90°) with ▲ ▼ and flip direction with ⇄.")
                TutorialBullet("3. Hold to Fire & Wind", "Hold FIRE to charge power and release to shoot! Account for the top wind arrow.")
                TutorialBullet("4. Tactical Items & Craters", "Use Heal, Fuel+, or Shield once per match. Carve terrain under foes to deal fall damage!")
                TutorialBullet("5. Battlefield Props & Obstacles", "Destroy the central castle wall and sky bridges! Hit TNT barrels, collect Supply Crates, bounce off Bumpers, and ride Updraft Vents.")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGoldDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (isKo) "확인했어요!" else "Got it!",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun TutorialBullet(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color(0xFF90E0EF))
        Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = Color.White)
    }
}

@Composable
fun PauseOverlay(
    language: AppLanguage,
    isOnline: Boolean = false,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onOpenSettings: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isKo = language == AppLanguage.KO
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xAA08101C))
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(350.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(HudSurfaceDark)
                .border(3.dp, PrimaryGold, RoundedCornerShape(24.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isOnline) {
                    if (isKo) "온라인 전투 메뉴" else "Online Match Menu"
                } else {
                    if (isKo) "일시정지" else "Paused"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = PrimaryGold
            )
            Button(
                onClick = onResume,
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("resume_button")
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = DeepNavyOutline)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isKo) "계속하기" else "Resume", style = MaterialTheme.typography.titleMedium, color = DeepNavyOutline)
            }
            if (!isOnline) {
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = PlayerBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("restart_button")
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isKo) "다시 시작" else "Restart", style = MaterialTheme.typography.titleMedium)
                }
            }
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF335C67)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isKo) "사운드 · 언어 설정" else "Audio & Language", style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(
                onClick = onMainMenu,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("main_menu_button")
            ) {
                Text(
                    text = if (isOnline) {
                        if (isKo) "기권 및 나가기" else "Forfeit & Exit"
                    } else {
                        if (isKo) "메인 화면으로" else "Main Menu"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isOnline) DangerRed else Color.White
                )
            }
        }
    }
}

@Composable
fun GameOverOverlay(
    state: BattleHudState,
    language: AppLanguage,
    progression: MatchProgressionResult? = null,
    onPlayAgain: () -> Unit,
    onBackToLobby: (() -> Unit)? = null,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isKo = language == AppLanguage.KO
    val winner = state.tanks.firstOrNull { it.id == state.winnerId }
    val localTank = state.tanks.getOrNull(state.localTankIndex) ?: state.tanks.getOrNull(0)
    val localWon = when (state.gameMode) {
        GameMode.AI_VS_PLAYER -> winner?.id == 0
        GameMode.ONLINE_PVP -> winner?.id == state.localTankIndex
        GameMode.LOCAL_2P -> true
    }
    val titleText = when {
        state.gameMode == GameMode.AI_VS_PLAYER && localWon -> if (isKo) "승리!" else "VICTORY!"
        state.gameMode == GameMode.AI_VS_PLAYER -> if (isKo) "패배…" else "DEFEAT…"
        state.gameMode == GameMode.ONLINE_PVP && localWon -> if (isKo) "온라인 승리!" else "ONLINE VICTORY!"
        state.gameMode == GameMode.ONLINE_PVP -> if (isKo) "온라인 패배…" else "ONLINE DEFEAT…"
        winner != null -> if (isKo) "${winner.name} 승리!" else "${winner.name} WINS!"
        else -> if (isKo) "무승부!" else "DRAW!"
    }
    val accent = if (localWon) PrimaryGold else DangerRed

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x9908101C))
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(26.dp))
                .background(HudSurfaceDark)
                .border(3.dp, accent, RoundedCornerShape(26.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.headlineLarge,
                    color = accent
                )
            }

            // 레벨업 & 경험치/랭킹 포인트 보상 배너
            if (progression != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF152946))
                        .border(
                            width = 2.dp,
                            color = if (progression.leveledUp) PrimaryGold else PlayerBlue,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (progression.leveledUp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryGold)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = DeepNavyOutline,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isKo) {
                                    "LEVEL UP!  LV.${progression.oldLevel} → LV.${progression.newLevel}"
                                } else {
                                    "LEVEL UP!  LV.${progression.oldLevel} → LV.${progression.newLevel}"
                                },
                                fontFamily = FredokaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = DeepNavyOutline
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LV.${progression.newLevel}  (+${progression.xpGained} EXP)",
                            style = MaterialTheme.typography.titleSmall,
                            color = SuccessGreen
                        )
                        val rpSign = if (progression.rpDelta >= 0L) "+" else ""
                        Text(
                            text = "${rpSign}${progression.rpDelta} RP  (${progression.newRankPoints} RP)",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (progression.rpDelta >= 0L) PrimaryGold else DangerRed
                        )
                    }

                    val xpFrac = (progression.newXp.toFloat() / progression.xpNeededNext.coerceAtLeast(1L).toFloat())
                        .coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { xpFrac },
                        color = SuccessGreen,
                        trackColor = Color(0xFF0C1829),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.dp)
                            .clip(RoundedCornerShape(50))
                    )
                    Text(
                        text = "${progression.newXp} / ${progression.xpNeededNext} EXP",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF102036))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatChip(if (isKo) "진행 턴" else "Turns", "${state.turnNumber}")
                StatChip(if (isKo) "입힌 피해" else "Damage", "${localTank?.totalDamageDealt ?: 0}")
                StatChip(if (isKo) "직격 횟수" else "Direct Hits", "${localTank?.directHits ?: 0}")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.gameMode == GameMode.ONLINE_PVP && onBackToLobby != null) {
                    Button(
                        onClick = onBackToLobby,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("back_to_lobby_button")
                    ) {
                        Text(
                            text = if (isKo) "온라인 대기실로" else "Online Lobby",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepNavyOutline
                        )
                    }
                } else {
                    Button(
                        onClick = onPlayAgain,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGoldDark),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("play_again_button")
                    ) {
                        Text(if (isKo) "다시 대전" else "Play Again", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                }
                OutlinedButton(
                    onClick = onMainMenu,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("result_main_menu_button")
                ) {
                    Text(if (isKo) "메인으로" else "Main Menu", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        Text(
            text = value,
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White
        )
    }
}
