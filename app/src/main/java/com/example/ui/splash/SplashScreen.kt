package com.example.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DeepNavyOutline
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.JuaFontFamily
import com.example.ui.theme.PrimaryGold
import com.example.ui.theme.PrimaryGoldDark
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgScale = remember { Animatable(1.10f) }
    val emblemScale = remember { Animatable(0.35f) }
    val titleScale = remember { Animatable(0.65f) }
    val contentAlpha = remember { Animatable(0f) }
    val loadProgress = remember { Animatable(0f) }
    val shellFlight = remember { Animatable(0f) }
    var canSkip by remember { mutableStateOf(false) }
    var finishedSent by remember { mutableStateOf(false) }

    fun finishOnce() {
        if (!finishedSent) {
            finishedSent = true
            onFinished()
        }
    }

    LaunchedEffect(Unit) {
        launch {
            bgScale.animateTo(1.0f, animationSpec = tween(durationMillis = 2400, easing = FastOutSlowInEasing))
        }
        launch {
            contentAlpha.animateTo(1f, animationSpec = tween(durationMillis = 420))
        }
        launch {
            emblemScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            delay(120L)
            titleScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            delay(200L)
            shellFlight.animateTo(1f, animationSpec = tween(durationMillis = 1550, easing = FastOutSlowInEasing))
        }
        launch {
            delay(480L)
            canSkip = true
        }
        loadProgress.animateTo(1f, animationSpec = tween(durationMillis = 2150, easing = FastOutSlowInEasing))
        delay(260L)
        finishOnce()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_fx")
    val sunburstAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sunburst_angle"
    )
    val sparklePulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_pulse"
    )

    val statusText = when {
        loadProgress.value < 0.35f -> "포탄 & 무기 장전 중…"
        loadProgress.value < 0.72f -> "전장 지형 & 바람 측정 중…"
        else -> "출격 준비 완료!"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A1B33))
            .testTag("splash_screen")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = canSkip
            ) {
                finishOnce()
            }
    ) {
        // 1. 고해상도 일러스트 배경 + 부드러운 줌아웃 효과
        Image(
            painter = painterResource(id = R.drawable.img_splash_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(bgScale.value)
        )

        // 2. 중앙 가독성을 높이는 시네마틱 비네팅 오버레이
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x550C2142),
                            Color(0xA6081428),
                            Color(0xD9050D1A)
                        )
                    )
                )
        )

        // 3. 회전 햇살 빔 + 포물선 포탄 궤적 + 반짝이 이펙트
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.42f

            // 은은한 골든 선버스트 광선
            rotate(degrees = sunburstAngle, pivot = Offset(cx, cy)) {
                val rayCount = 14
                val rayLength = maxOf(w, h) * 0.85f
                for (i in 0 until rayCount) {
                    val startAngle = (i * (360f / rayCount)) * (PI / 180.0)
                    val halfWidth = 7.5f * (PI / 180.0)
                    val rayPath = Path().apply {
                        moveTo(cx, cy)
                        lineTo(
                            cx + (cos(startAngle - halfWidth) * rayLength).toFloat(),
                            cy + (sin(startAngle - halfWidth) * rayLength).toFloat()
                        )
                        lineTo(
                            cx + (cos(startAngle + halfWidth) * rayLength).toFloat(),
                            cy + (sin(startAngle + halfWidth) * rayLength).toFloat()
                        )
                        close()
                    }
                    drawPath(
                        path = rayPath,
                        color = Color(0xFFFFD166).copy(alpha = 0.085f)
                    )
                }
            }

            // 날아가는 포탄 포물선 궤적 (좌측 언덕 -> 우측 성채)
            val t = shellFlight.value.coerceIn(0f, 1f)
            val p0 = Offset(w * 0.14f, h * 0.64f)
            val p1 = Offset(w * 0.50f, h * 0.05f)
            val p2 = Offset(w * 0.86f, h * 0.50f)

            fun quadBezier(u: Float): Offset {
                val inv = 1f - u
                val x = inv * inv * p0.x + 2f * inv * u * p1.x + u * u * p2.x
                val y = inv * inv * p0.y + 2f * inv * u * p1.y + u * u * p2.y
                return Offset(x, y)
            }

            if (t > 0.01f) {
                val trailPath = Path()
                val steps = 36
                val maxStep = (steps * t).toInt().coerceAtLeast(1)
                val first = quadBezier(0f)
                trailPath.moveTo(first.x, first.y)
                for (s in 1..maxStep) {
                    val u = (s.toFloat() / steps).coerceAtMost(t)
                    val pt = quadBezier(u)
                    trailPath.lineTo(pt.x, pt.y)
                }
                drawPath(
                    path = trailPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color(0xFFFFE599).copy(alpha = 0.45f),
                            Color(0xFFFF7A1A).copy(alpha = 0.88f)
                        )
                    ),
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                val head = quadBezier(t)
                if (t < 0.96f) {
                    drawCircle(
                        color = Color(0xFFFFB703).copy(alpha = 0.35f),
                        radius = 18.dp.toPx(),
                        center = head
                    )
                    drawCircle(
                        color = Color(0xFFFF6D00),
                        radius = 8.dp.toPx(),
                        center = head
                    )
                    drawCircle(
                        color = Color(0xFFFFF8D6),
                        radius = 4.dp.toPx(),
                        center = head
                    )
                } else {
                    // 도착 폭발 플래시
                    val burstProgress = ((t - 0.96f) / 0.04f).coerceIn(0f, 1f)
                    val radius = (22f + burstProgress * 34f).dp.toPx()
                    drawCircle(
                        color = Color(0xFFFFD166).copy(alpha = (1f - burstProgress * 0.55f) * 0.65f),
                        radius = radius,
                        center = p2
                    )
                    drawCircle(
                        color = Color(0xFFFF6D00).copy(alpha = (1f - burstProgress * 0.5f) * 0.85f),
                        radius = radius * 0.52f,
                        center = p2
                    )
                }
            }

            // 화면 곳곳의 은은한 황금 반짝이 입자
            val starCoords = listOf(
                Offset(0.18f, 0.24f),
                Offset(0.32f, 0.16f),
                Offset(0.68f, 0.18f),
                Offset(0.82f, 0.26f),
                Offset(0.24f, 0.76f),
                Offset(0.76f, 0.74f)
            )
            starCoords.forEachIndexed { idx, norm ->
                val phase = if (idx % 2 == 0) sparklePulse else (1.65f - sparklePulse)
                val sx = w * norm.x
                val sy = h * norm.y
                val r = (3.5f + phase * 3f).dp.toPx()
                drawLine(
                    color = Color(0xFFFFE066).copy(alpha = 0.7f * phase),
                    start = Offset(sx - r, sy),
                    end = Offset(sx + r, sy),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFFFE066).copy(alpha = 0.7f * phase),
                    start = Offset(sx, sy - r),
                    end = Offset(sx, sy + r),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // 4. 중앙 로고 & 하단 로딩바 + 제작자 배지
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .alpha(contentAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 상단 소형 시즌 태그
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xAA0C1D35))
                    .border(1.5.dp, PrimaryGold.copy(alpha = 0.75f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 16.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "ONLINE ARTILLERY DUEL · RANKED SEASON",
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = PrimaryGold
                )
            }

            // 중앙 엠블럼 + 3D 카툰 타이틀 로고
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .scale(emblemScale.value)
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(PrimaryGold, PrimaryGoldDark)
                            )
                        )
                        .border(3.dp, Color.White, CircleShape)
                        .padding(4.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.cannon_app_icon),
                        contentDescription = "캐논 대전 아이콘",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier.scale(titleScale.value),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CartoonOutlinedTitle(text = "캐논 대전")

                    // Fortress V2 스타일의 메탈 에메랄드 V2 배지
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF3BF58A), Color(0xFF0FB65B))
                                )
                            )
                            .border(3.dp, DeepNavyOutline, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "V2",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp,
                            color = DeepNavyOutline
                        )
                    }
                }

                Text(
                    text = "바람을 읽고! 각도를 맞춰! 승리를 쏴라!",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFF1F6FF)
                )
            }

            // 하단 로딩 게이지 + 제작자 배지
            Column(
                modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFE0ECFF)
                    )
                    Text(
                        text = "${(loadProgress.value * 100).toInt()}%",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PrimaryGold
                    )
                }

                // 광택 로딩 게이지
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xCC0D1B2A))
                        .border(2.dp, PrimaryGold.copy(alpha = 0.85f), RoundedCornerShape(7.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(loadProgress.value.coerceIn(0.03f, 1f))
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFFF7A1A),
                                        PrimaryGold,
                                        SuccessGreen
                                    )
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xCC10223A))
                            .border(1.5.dp, PrimaryGold, RoundedCornerShape(999.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "제작 · 구지초도형캠프",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = if (canSkip) "화면을 터치하면 바로 시작" else "LOADING…",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFB9CCE6).copy(alpha = if (canSkip) sparklePulse else 0.65f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CartoonOutlinedTitle(text: String) {
    val baseStyle = TextStyle(
        fontFamily = JuaFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 48.sp,
        letterSpacing = 1.sp
    )
    Box(contentAlignment = Alignment.Center) {
        // 3D 하단 그림자
        Text(
            text = text,
            style = baseStyle.copy(color = Color(0xDD071222)),
            modifier = Modifier.offset(x = 3.dp, y = 5.dp)
        )
        // 굵은 네이비 외곽선
        Text(
            text = text,
            style = baseStyle.copy(
                color = DeepNavyOutline,
                drawStyle = Stroke(width = 14f, join = StrokeJoin.Round)
            )
        )
        // 밝은 화이트 내부 하이라이트 림
        Text(
            text = text,
            style = baseStyle.copy(
                color = Color.White,
                drawStyle = Stroke(width = 6f, join = StrokeJoin.Round)
            )
        )
        // 불꽃 골드-오렌지 그라디언트 채움
        Text(
            text = text,
            style = baseStyle.copy(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF275),
                        Color(0xFFFFB703),
                        Color(0xFFFF6D00)
                    )
                )
            )
        )
    }
}
