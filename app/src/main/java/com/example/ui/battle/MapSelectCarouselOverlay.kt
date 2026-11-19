package com.example.ui.battle

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.model.AppLanguage
import com.example.game.model.MapTheme
import com.example.game.model.TankType
import com.example.ui.components.CartoonActionButton
import com.example.ui.components.CartoonIconButton
import com.example.ui.components.CartoonStrokeText
import com.example.ui.tank.TankAvatar
import com.example.ui.theme.DeepNavyOutline
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.JuaFontFamily
import com.example.ui.theme.PrimaryGold
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@DrawableRes
fun MapTheme.previewDrawableRes(): Int = when (this) {
    MapTheme.GRASSLAND -> R.drawable.img_map_grassland
    MapTheme.DESERT -> R.drawable.img_map_desert
    MapTheme.SNOW -> R.drawable.img_map_snow
    MapTheme.VOLCANO -> R.drawable.img_map_volcano
    MapTheme.ISLAND -> R.drawable.img_map_island
    MapTheme.SPACE -> R.drawable.img_map_space
}

fun MapTheme.description(lang: AppLanguage): String = when (this) {
    MapTheme.GRASSLAND -> if (lang == AppLanguage.KO) "푸른 언덕과 중앙 돌성벽이 맞서는 클래식 포격 전장!" else "Sunny rolling hills & classic central stone fortress!"
    MapTheme.DESERT -> if (lang == AppLanguage.KO) "고대 사암 협곡과 현수교 위를 가르는 황금빛 모래 폭풍!" else "Golden sandstone canyons, cacti & desert sky bridges!"
    MapTheme.SNOW -> if (lang == AppLanguage.KO) "눈보라가 휘날리는 설산 빙벽과 아찔한 얼음 다리!" else "Snowy alpine peaks, frosty ramparts & glistening ice!"
    MapTheme.VOLCANO -> if (lang == AppLanguage.KO) "용암 강과 흑요석 다리가 불타오르는 화산 칼데라 격전지!" else "Molten lava caldera with glowing crystals & fiery embers!"
    MapTheme.ISLAND -> if (lang == AppLanguage.KO) "에메랄드 바다 위 해적 목교와 야자수 섬들의 포격 대결!" else "Tropical turquoise islands linked by pirate rope bridges!"
    MapTheme.SPACE -> if (lang == AppLanguage.KO) "저중력 외계 행성 기지, 네온 에너지 브릿지 & 우주 신호탑!" else "Low-gravity alien moon base with neon bridges & stars!"
}

fun MapTheme.featureTags(lang: AppLanguage): List<String> = when (this) {
    MapTheme.GRASSLAND -> if (lang == AppLanguage.KO) listOf("🏰 중앙 성벽", "💥 TNT 화약통", "🌀 도탄 범퍼") else listOf("🏰 Castle Wall", "💥 TNT Barrels", "🌀 Bumpers")
    MapTheme.DESERT -> if (lang == AppLanguage.KO) listOf("🌵 사막 협곡", "💨 상승 기류", "📦 전술 보급") else listOf("🌵 Canyon Dunes", "💨 Updraft", "📦 Supply")
    MapTheme.SNOW -> if (lang == AppLanguage.KO) listOf("❄️ 눈보라 기상", "🧊 얼음 발판", "💥 TNT 연쇄") else listOf("❄️ Snowfall", "🧊 Ice Bridge", "💥 TNT Chain")
    MapTheme.VOLCANO -> if (lang == AppLanguage.KO) listOf("🌋 불티 상승", "💎 수정 지형", "🔥 고난도 언덕") else listOf("🌋 Ash Embers", "💎 Lava Crystals", "🔥 Steep Hills")
    MapTheme.ISLAND -> if (lang == AppLanguage.KO) listOf("🏝️ 해적 목교", "🌴 야자수 섬", "🌊 추락 주의") else listOf("🏝️ Rope Bridge", "🌴 Palm Island", "🌊 Sea Drop")
    MapTheme.SPACE -> if (lang == AppLanguage.KO) listOf("🪐 우주 저중력", "⚡ 네온 발판", "📡 우주 비콘") else listOf("🪐 Space Base", "⚡ Neon Bridge", "📡 Beacons")
}

/**
 * 게임 시작 직전에 6종 전장 맵의 풀스크린 카툰 일러스트를 좌우 슬라이드로 감상하며 선택하는 화면.
 */
@Composable
fun MapSelectCarouselOverlay(
    initialMap: MapTheme,
    selectedTank: TankType,
    modeLabel: String,
    confirmLabel: String,
    isBattleLaunch: Boolean,
    language: AppLanguage,
    onMapFocused: (MapTheme) -> Unit,
    onConfirmMap: (MapTheme) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isKo = language == AppLanguage.KO
    val maps = MapTheme.entries
    val initialPage = maps.indexOf(initialMap).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { maps.size })
    val scope = rememberCoroutineScope()

    val currentMap = maps[pagerState.currentPage]

    LaunchedEffect(pagerState.currentPage) {
        onMapFocused(maps[pagerState.currentPage])
    }

    BackHandler(onBack = onDismiss)

    val pulseTransition = rememberInfiniteTransition(label = "launch_pulse")
    val ctaPulse by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cta_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07111F))
            .testTag("map_select_carousel_overlay")
    ) {
        // 1) 전체화면 16:9 카툰 전장 일러스트 가로 슬라이드 (HorizontalPager)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("map_select_pager")
        ) { page ->
            val map = maps[page]
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                .absoluteValue
                .coerceIn(0f, 1f)
            val zoom = 1.05f - (pageOffset * 0.09f)

            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = map.previewDrawableRes()),
                    contentDescription = map.label(language),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            alpha = 1f - pageOffset * 0.25f
                        }
                )
            }
        }

        // 2) 상단/하단 가독성 시네마틱 그라디언트 비네팅 (중앙 일러스트는 선명하게 유지)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xCC06101E), Color(0x4406101E), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xAA071222), Color(0xEE071222))
                    )
                )
        )

        // 3) Safe Area 내부 카툰 컨트롤 오버레이
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // 상단 바: 뒤로가기 + 모드/슬라이드 안내 + 내 출격 탱크 배지
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CartoonActionButton(
                    onClick = onDismiss,
                    topColor = Color(0xFF426A99),
                    bottomColor = Color(0xFF28466B),
                    lipColor = Color(0xFF152740),
                    cornerRadius = 13.dp,
                    depth = 3.5.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(46.dp)
                        .testTag("map_select_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = if (isKo) "뒤로가기" else "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isKo) "뒤로" else "Back",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 중앙: 전장 선택 배너 & 스와이프 힌트
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xDD0C1C30))
                        .border(2.5.dp, PrimaryGold, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Swipe,
                        contentDescription = null,
                        tint = PrimaryGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "$modeLabel · ${pagerState.currentPage + 1}/${maps.size}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGold
                    )
                    Text(
                        text = if (isKo) "좌우 슬라이드!" else "Swipe!",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFD6E6FF)
                    )
                }

                // 우측: 현재 출격 탱크 배지
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xDD0C1C30))
                        .border(2.dp, selectedTank.trimColor, RoundedCornerShape(14.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TankAvatar(
                        tankType = selectedTank,
                        size = 34.dp,
                        highlighted = true
                    )
                    Column {
                        Text(
                            text = if (isKo) "출격 탱크" else "My Tank",
                            fontSize = 9.sp,
                            color = Color(0xFFB5CCE6)
                        )
                        Text(
                            text = selectedTank.label(language),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 좌측 카툰 슬라이드 화살표 버튼
            if (pagerState.currentPage > 0) {
                CartoonIconButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                        }
                    },
                    topColor = Color(0xFFFFE066),
                    bottomColor = Color(0xFFFFB703),
                    lipColor = Color(0xFFB86B00),
                    shape = CircleShape,
                    depth = 4.dp,
                    modifier = Modifier
                        .size(54.dp)
                        .align(Alignment.CenterStart)
                        .testTag("map_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = if (isKo) "이전 맵" else "Previous Map",
                        tint = DeepNavyOutline,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            // 우측 카툰 슬라이드 화살표 버튼
            if (pagerState.currentPage < maps.lastIndex) {
                CartoonIconButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(maps.lastIndex))
                        }
                    },
                    topColor = Color(0xFFFFE066),
                    bottomColor = Color(0xFFFFB703),
                    lipColor = Color(0xFFB86B00),
                    shape = CircleShape,
                    depth = 4.dp,
                    modifier = Modifier
                        .size(54.dp)
                        .align(Alignment.CenterEnd)
                        .testTag("map_next_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = if (isKo) "다음 맵" else "Next Map",
                        tint = DeepNavyOutline,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            // 하단 정보 & 퀵 썸네일 스트립 + 대형 카툰 출격 버튼
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // 좌측: 맵 이름 + 한줄 소개 + 지형 기믹 태그
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CartoonStrokeText(
                                text = currentMap.label(language),
                                style = TextStyle(
                                    fontFamily = JuaFontFamily,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = PrimaryGold,
                                shadowOffset = 2.5.dp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(currentMap.skyTop.copy(alpha = 0.85f))
                                    .border(1.8.dp, DeepNavyOutline, RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "STAGE 0${pagerState.currentPage + 1}",
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Text(
                            text = currentMap.description(language),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFF1F7FF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            currentMap.featureTags(language).forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xCC11233A))
                                        .border(1.5.dp, PrimaryGold.copy(alpha = 0.7f), RoundedCornerShape(50))
                                        .padding(horizontal = 9.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // 우측: 대형 3D 카툰 출격/확정 버튼
                    CartoonActionButton(
                        onClick = { onConfirmMap(currentMap) },
                        topColor = if (isBattleLaunch) Color(0xFFFFB703) else Color(0xFF4CE3A3),
                        bottomColor = if (isBattleLaunch) Color(0xFFFF6D00) else Color(0xFF00B87C),
                        lipColor = if (isBattleLaunch) Color(0xFF9C3000) else Color(0xFF006B46),
                        borderColor = Color.White,
                        cornerRadius = 18.dp,
                        depth = 5.dp,
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(58.dp)
                            .widthIn(min = 184.dp)
                            .scale(ctaPulse)
                            .testTag("confirm_map_and_start_button")
                    ) {
                        Icon(
                            imageVector = if (isBattleLaunch) Icons.Rounded.PlayArrow else Icons.Rounded.Check,
                            contentDescription = null,
                            tint = if (isBattleLaunch) Color.White else DeepNavyOutline,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        CartoonStrokeText(
                            text = confirmLabel,
                            style = TextStyle(
                                fontFamily = JuaFontFamily,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isBattleLaunch) Color.White else DeepNavyOutline,
                            outlineColor = if (isBattleLaunch) Color(0xFF5A1800) else Color.White.copy(alpha = 0.4f),
                            shadowOffset = 1.5.dp
                        )
                    }
                }

                // 하단 6종 맵 퀵 썸네일 슬라이드 바
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    maps.forEachIndexed { idx, map ->
                        val selected = idx == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .width(if (selected) 108.dp else 88.dp)
                                .height(42.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .border(
                                    width = if (selected) 2.8.dp else 1.5.dp,
                                    color = if (selected) PrimaryGold else Color(0x88FFFFFF),
                                    shape = RoundedCornerShape(11.dp)
                                )
                                .clickable {
                                    scope.launch { pagerState.animateScrollToPage(idx) }
                                }
                                .testTag("map_slide_thumb_${map.name.lowercase()}")
                        ) {
                            Image(
                                painter = painterResource(id = map.previewDrawableRes()),
                                contentDescription = map.label(language),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (selected) Color(0x44000000) else Color(0x88081222)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = PrimaryGold,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Text(
                                        text = map.label(language),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) PrimaryGold else Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
