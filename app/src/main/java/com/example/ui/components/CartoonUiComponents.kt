package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepNavyOutline
import com.example.ui.theme.PrimaryGold

/**
 * 카툰 아케이드 게임 스타일의 3D 입체 푸시 버튼.
 * 두꺼운 다크 네이비 외곽선, 하단 3D 입체 그림자 턱(lip), 상단 글로시 광택 하이라이트,
 * 누를 때 실제로 아래로 푹 들어가는 스프링 물리 애니메이션을 제공합니다.
 *
 * 고정 크기(fillMaxWidth/weight/width)와 콘텐츠 크기(wrapContentWidth) 모두 정확하게 측정됩니다.
 */
@Composable
fun CartoonActionButton(
    onClick: () -> Unit,
    topColor: Color,
    bottomColor: Color,
    lipColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = DeepNavyOutline,
    cornerRadius: Dp = 16.dp,
    depth: Dp = 4.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) depth - 1.dp else 0.dp,
        animationSpec = spring(stiffness = 700f, dampingRatio = 0.65f),
        label = "cartoon_btn_offset"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = spring(stiffness = 700f, dampingRatio = 0.7f),
        label = "cartoon_btn_scale"
    )
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(if (enabled) 1f else 0.48f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        propagateMinConstraints = true
    ) {
        // 하단 3D 입체 턱(Extruded bottom lip) — 측정된 버튼 전체 크기에 맞춰 배치
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = depth)
                .clip(shape)
                .background(lipColor)
                .border(2.5.dp, borderColor, shape)
        )

        // 상단 실제 버튼 캡 — 콘텐츠 크기를 직접 측정하므로 wrapContentWidth에서도 절대 0dp로 줄어들지 않음
        Box(
            modifier = Modifier
                .padding(bottom = depth)
                .offset(y = pressOffset)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
                .drawWithContent {
                    val glossHeight = (size.height * 0.42f).coerceAtLeast(0f)
                    val hPad = 5.dp.toPx()
                    val vPad = 3.dp.toPx()
                    if (size.width > hPad * 2f && glossHeight > vPad) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.42f),
                                    Color.White.copy(alpha = 0.04f)
                                ),
                                startY = vPad,
                                endY = vPad + glossHeight
                            ),
                            topLeft = Offset(hPad, vPad),
                            size = Size(size.width - hPad * 2f, glossHeight),
                            cornerRadius = CornerRadius((cornerRadius * 0.65f).toPx())
                        )
                    }
                    if (size.width > 24.dp.toPx() && size.height > 14.dp.toPx()) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.6f),
                            topLeft = Offset(9.dp.toPx(), 5.dp.toPx()),
                            size = Size(10.dp.toPx(), 4.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }
                    drawContent()
                }
                .border(2.5.dp, borderColor, shape),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/**
 * 카툰 스타일의 정사각형/원형 3D 아이콘 버튼 (설정, 일시정지, 화살표, 닫기 등).
 */
@Composable
fun CartoonIconButton(
    onClick: () -> Unit,
    topColor: Color,
    bottomColor: Color,
    lipColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = DeepNavyOutline,
    shape: Shape = RoundedCornerShape(13.dp),
    depth: Dp = 3.5.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) depth - 1.dp else 0.dp,
        animationSpec = spring(stiffness = 750f, dampingRatio = 0.65f),
        label = "cartoon_icon_offset"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.94f else 1f,
        animationSpec = spring(stiffness = 750f, dampingRatio = 0.65f),
        label = "cartoon_icon_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        propagateMinConstraints = true
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = depth)
                .clip(shape)
                .background(lipColor)
                .border(2.2.dp, borderColor, shape)
        )
        Box(
            modifier = Modifier
                .padding(bottom = depth)
                .offset(y = pressOffset)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
                .drawWithContent {
                    val glossWidth = size.width * 0.78f
                    val glossHeight = size.height * 0.36f
                    val vPad = 2.dp.toPx()
                    if (glossWidth > 4f && glossHeight > 4f) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.34f),
                            topLeft = Offset((size.width - glossWidth) / 2f, vPad),
                            size = Size(glossWidth, glossHeight),
                            cornerRadius = CornerRadius(glossHeight / 2f)
                        )
                    }
                    drawContent()
                }
                .border(2.2.dp, borderColor, shape),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/**
 * 길게 누르기(이동 · 각도 조절 · 발사 파워 충전)를 지원하는 3D 카툰 아케이드 버튼.
 */
@Composable
fun CartoonHoldButton(
    enabled: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    topColor: Color,
    bottomColor: Color,
    lipColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color = DeepNavyOutline,
    cornerRadius: Dp = 13.dp,
    depth: Dp = 4.dp,
    content: @Composable BoxScope.() -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val latestEnabled by rememberUpdatedState(enabled)
    val latestStart by rememberUpdatedState(onPressStart)
    val latestEnd by rememberUpdatedState(onPressEnd)

    val pressOffset by animateDpAsState(
        targetValue = if (pressed && enabled) depth - 1.dp else 0.dp,
        animationSpec = spring(stiffness = 800f, dampingRatio = 0.65f),
        label = "hold_btn_offset"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.95f else 1f,
        animationSpec = spring(stiffness = 800f, dampingRatio = 0.65f),
        label = "hold_btn_scale"
    )
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(if (enabled) 1f else 0.42f)
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
        propagateMinConstraints = true
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = depth)
                .clip(shape)
                .background(lipColor)
                .border(2.2.dp, if (pressed) Color.White else borderColor, shape)
        )
        Box(
            modifier = Modifier
                .padding(bottom = depth)
                .offset(y = pressOffset)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
                .drawWithContent {
                    val glossWidth = size.width * 0.84f
                    val glossHeight = size.height * 0.38f
                    val vPad = 2.5.dp.toPx()
                    if (glossWidth > 4f && glossHeight > 4f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.44f),
                                    Color.White.copy(alpha = 0.04f)
                                ),
                                startY = vPad,
                                endY = vPad + glossHeight
                            ),
                            topLeft = Offset((size.width - glossWidth) / 2f, vPad),
                            size = Size(glossWidth, glossHeight),
                            cornerRadius = CornerRadius((cornerRadius * 0.6f).toPx())
                        )
                    }
                    drawContent()
                }
                .border(2.2.dp, if (pressed) Color.White else borderColor, shape),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/**
 * 카툰 스타일의 선택형 3D 칩/배지 (난이도 · 무기 슬롯 · 전술 아이템).
 */
@Composable
fun CartoonPillChip(
    selected: Boolean,
    onClick: () -> Unit,
    selectedTopColor: Color,
    selectedBottomColor: Color,
    selectedLipColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    unselectedTopColor: Color = Color(0xFF28466B),
    unselectedBottomColor: Color = Color(0xFF1A3150),
    unselectedLipColor: Color = Color(0xFF0E1D33),
    borderColor: Color = if (selected) Color.White else DeepNavyOutline,
    cornerRadius: Dp = 12.dp,
    depth: Dp = 3.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
    content: @Composable RowScope.() -> Unit
) {
    CartoonActionButton(
        onClick = onClick,
        topColor = if (selected) selectedTopColor else unselectedTopColor,
        bottomColor = if (selected) selectedBottomColor else unselectedBottomColor,
        lipColor = if (selected) selectedLipColor else unselectedLipColor,
        borderColor = borderColor,
        cornerRadius = cornerRadius,
        depth = depth,
        enabled = enabled,
        contentPadding = contentPadding,
        modifier = modifier,
        content = content
    )
}

/**
 * 게임 HUD용 글로시 게이지 바 (HP · 연료 · 발사 파워).
 */
@Composable
fun CartoonGlossyBar(
    progress: Float,
    fillBrush: Brush,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    cornerRadius: Dp = 6.dp,
    trackColor: Color = Color(0xFF0A1424),
    overlayContent: (@Composable BoxScope.() -> Unit)? = null
) {
    val clamped = progress.coerceIn(0f, 1f)
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(trackColor)
            .border(1.8.dp, DeepNavyOutline, shape)
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped)
                    .clip(shape)
                    .background(fillBrush)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.42f)
                        .background(Color.White.copy(alpha = 0.36f))
                        .align(Alignment.TopCenter)
                )
            }
        }
        overlayContent?.invoke(this)
    }
}

/**
 * 카툰 스타일의 짙은 그림자 외곽선이 적용된 강조 텍스트.
 */
@Composable
fun CartoonStrokeText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    outlineColor: Color = DeepNavyOutline,
    shadowOffset: Dp = 2.dp,
    maxLines: Int = 1,
    textAlign: TextAlign = TextAlign.Start
) {
    Box(modifier = modifier) {
        Text(
            text = text,
            style = style,
            color = outlineColor,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
            modifier = Modifier.offset(x = shadowOffset, y = shadowOffset)
        )
        Text(
            text = text,
            style = style,
            color = outlineColor.copy(alpha = 0.85f),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
            modifier = Modifier.offset(x = -shadowOffset * 0.45f, y = -shadowOffset * 0.45f)
        )
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign
        )
    }
}

/**
 * 카툰 아케이드 게임 스타일의 이중 잉크 테두리 + 3D 하단 입체 그림자 + 모서리 리벳 볼트 +
 * 코믹 점선 장식이 들어간 게임 보드 패널.
 */
@Composable
fun CartoonGamePanel(
    modifier: Modifier = Modifier,
    frameColor: Color = PrimaryGold,
    topBgColor: Color = Color(0xF01B3558),
    bottomBgColor: Color = Color(0xF50C1B2E),
    lipColor: Color = Color(0xFF07111E),
    cornerRadius: Dp = 22.dp,
    depth: Dp = 5.dp,
    showRivets: Boolean = true,
    showComicDash: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val outerShape = RoundedCornerShape(cornerRadius)
    val innerRadius = (cornerRadius - 3.dp).coerceAtLeast(6.dp)

    Box(
        modifier = modifier,
        propagateMinConstraints = true
    ) {
        // 1) 하단 3D 입체 그림자 판(Extruded comic drop shadow plate)
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = depth)
                .clip(outerShape)
                .background(lipColor)
                .border(3.dp, DeepNavyOutline, outerShape)
        )

        // 2) 메인 카툰 보드 본체 (이중 외곽선 + 상단 광택 + 점선 스티치 + 코너 리벳)
        Box(
            modifier = Modifier
                .padding(bottom = depth)
                .clip(outerShape)
                .background(Brush.verticalGradient(listOf(topBgColor, bottomBgColor)))
                .drawWithContent {
                    val crOuter = cornerRadius.toPx()
                    val frameStroke = 3.2.dp.toPx()
                    val outerInk = 2.8.dp.toPx()

                    // 내부 상단 은은한 광택 띠
                    val glossH = (size.height * 0.16f).coerceIn(8.dp.toPx(), 24.dp.toPx())
                    val gInset = 6.dp.toPx()
                    if (size.width > gInset * 2f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.14f),
                                    Color.Transparent
                                ),
                                startY = gInset,
                                endY = gInset + glossH
                            ),
                            topLeft = Offset(gInset, gInset),
                            size = Size(size.width - gInset * 2f, glossH),
                            cornerRadius = CornerRadius(innerRadius.toPx())
                        )
                    }

                    // 코믹 점선 스티치 장식 테두리
                    if (showComicDash && size.width > 40.dp.toPx() && size.height > 32.dp.toPx()) {
                        val dashInset = 7.dp.toPx()
                        drawRoundRect(
                            color = frameColor.copy(alpha = 0.32f),
                            topLeft = Offset(dashInset, dashInset),
                            size = Size(size.width - dashInset * 2f, size.height - dashInset * 2f),
                            cornerRadius = CornerRadius((crOuter - dashInset).coerceAtLeast(4f)),
                            style = Stroke(
                                width = 1.4.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
                                    0f
                                )
                            )
                        )
                    }

                    // 실제 자식 콘텐츠 먼저 그리기
                    drawContent()

                    // 컬러 카툰 메인 프레임 링
                    val ringInset = outerInk * 0.9f
                    drawRoundRect(
                        color = frameColor,
                        topLeft = Offset(ringInset, ringInset),
                        size = Size(size.width - ringInset * 2f, size.height - ringInset * 2f),
                        cornerRadius = CornerRadius((crOuter - ringInset * 0.6f).coerceAtLeast(4f)),
                        style = Stroke(width = frameStroke)
                    )

                    // 내부 얇은 다크 잉크 엣지 (이중 카툰 라인 느낌 극대화)
                    val innerInkInset = ringInset + frameStroke * 0.55f
                    drawRoundRect(
                        color = DeepNavyOutline.copy(alpha = 0.9f),
                        topLeft = Offset(innerInkInset, innerInkInset),
                        size = Size(size.width - innerInkInset * 2f, size.height - innerInkInset * 2f),
                        cornerRadius = CornerRadius((crOuter - innerInkInset).coerceAtLeast(3f)),
                        style = Stroke(width = 1.4.dp.toPx())
                    )

                    // 최외곽 두꺼운 다크 네이비 코믹 잉크선
                    val outerHalf = outerInk * 0.5f
                    drawRoundRect(
                        color = DeepNavyOutline,
                        topLeft = Offset(outerHalf, outerHalf),
                        size = Size(size.width - outerInk, size.height - outerInk),
                        cornerRadius = CornerRadius(crOuter),
                        style = Stroke(width = outerInk)
                    )

                    // 4개 모서리 금속 리벳 볼트 장식
                    if (showRivets && size.width > 64.dp.toPx() && size.height > 44.dp.toPx()) {
                        val rivetMargin = 10.5.dp.toPx()
                        val rivetR = 3.2.dp.toPx()
                        val corners = listOf(
                            Offset(rivetMargin, rivetMargin),
                            Offset(size.width - rivetMargin, rivetMargin),
                            Offset(rivetMargin, size.height - rivetMargin),
                            Offset(size.width - rivetMargin, size.height - rivetMargin)
                        )
                        corners.forEach { c ->
                            // 볼트 그림자/외곽선
                            drawCircle(
                                color = DeepNavyOutline,
                                radius = rivetR + 1.3.dp.toPx(),
                                center = c
                            )
                            // 볼트 본체
                            drawCircle(
                                color = frameColor,
                                radius = rivetR,
                                center = c
                            )
                            // 좌상단 광택 점
                            drawCircle(
                                color = Color.White.copy(alpha = 0.85f),
                                radius = rivetR * 0.42f,
                                center = Offset(c.x - rivetR * 0.3f, c.y - rivetR * 0.3f)
                            )
                        }
                    }
                }
        ) {
            Box(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

/**
 * 구획을 나누는 가로 카툰 구분선 (두꺼운 잉크 외곽선 + 점선/리벳 엔드캡 + 중앙 별/라벨 배지).
 */
@Composable
fun CartoonSectionDivider(
    modifier: Modifier = Modifier,
    label: String? = null,
    accentColor: Color = PrimaryGold
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (label != null) 18.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CartoonDividerHalfLine(
            accentColor = accentColor,
            dotOnLeft = true,
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
        )

        if (label != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF10223A))
                    .border(1.8.dp, accentColor, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    maxLines = 1
                )
            }
        } else {
            // 중앙 카툰 다이아몬드 별 리벳 엠블럼
            Canvas(modifier = Modifier.height(12.dp).width(16.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val rx = size.width * 0.44f
                val ry = size.height * 0.44f
                val diamond = Path().apply {
                    moveTo(cx, cy - ry)
                    lineTo(cx + rx, cy)
                    lineTo(cx, cy + ry)
                    lineTo(cx - rx, cy)
                    close()
                }
                drawPath(diamond, color = DeepNavyOutline, style = Stroke(width = 2.6.dp.toPx()))
                drawPath(diamond, color = accentColor)
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = 1.5.dp.toPx(),
                    center = Offset(cx - 1.dp.toPx(), cy - 1.dp.toPx())
                )
            }
        }

        CartoonDividerHalfLine(
            accentColor = accentColor,
            dotOnLeft = false,
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
        )
    }
}

@Composable
private fun CartoonDividerHalfLine(
    accentColor: Color,
    dotOnLeft: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cy = size.height / 2f
        val endMargin = 4.dp.toPx()
        val startX = if (dotOnLeft) endMargin * 2.2f else 0f
        val endX = if (dotOnLeft) size.width else size.width - endMargin * 2.2f

        if (endX > startX) {
            // 두꺼운 다크 잉크 외곽선
            drawLine(
                color = DeepNavyOutline,
                start = Offset(startX, cy),
                end = Offset(endX, cy),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
            // 카툰 대시 코어 라인
            drawLine(
                color = accentColor.copy(alpha = 0.88f),
                start = Offset(startX + 1.dp.toPx(), cy),
                end = Offset(endX - 1.dp.toPx(), cy),
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 5.dp.toPx()), 0f)
            )
        }

        // 끝단 카툰 리벳 점
        val dotX = if (dotOnLeft) endMargin else size.width - endMargin
        drawCircle(color = DeepNavyOutline, radius = 3.4.dp.toPx(), center = Offset(dotX, cy))
        drawCircle(color = accentColor, radius = 2.1.dp.toPx(), center = Offset(dotX, cy))
    }
}

/**
 * 메인 화면의 좌측(사령관·로고·탱크)과 우측(전투 출격 본부) 구획을 나누는 세로 카툰 장식선.
 */
@Composable
fun CartoonVerticalGameDivider(
    modifier: Modifier = Modifier,
    accentColor: Color = PrimaryGold
) {
    Canvas(modifier = modifier.width(16.dp)) {
        val cx = size.width / 2f
        val capMargin = 10.dp.toPx()
        val cy = size.height / 2f
        val badgeGap = 15.dp.toPx()

        // 상단 점선 구간
        if (cy - badgeGap > capMargin) {
            drawLine(
                color = DeepNavyOutline,
                start = Offset(cx, capMargin),
                end = Offset(cx, cy - badgeGap),
                strokeWidth = 5.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = accentColor.copy(alpha = 0.9f),
                start = Offset(cx, capMargin),
                end = Offset(cx, cy - badgeGap),
                strokeWidth = 2.4.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()), 0f)
            )
        }

        // 하단 점선 구간
        if (size.height - capMargin > cy + badgeGap) {
            drawLine(
                color = DeepNavyOutline,
                start = Offset(cx, cy + badgeGap),
                end = Offset(cx, size.height - capMargin),
                strokeWidth = 5.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = accentColor.copy(alpha = 0.9f),
                start = Offset(cx, cy + badgeGap),
                end = Offset(cx, size.height - capMargin),
                strokeWidth = 2.4.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()), 0f)
            )
        }

        // 상단/하단 볼트 캡
        listOf(capMargin, size.height - capMargin).forEach { y ->
            drawCircle(color = DeepNavyOutline, radius = 4.6.dp.toPx(), center = Offset(cx, y))
            drawCircle(color = accentColor, radius = 2.8.dp.toPx(), center = Offset(cx, y))
            drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 1.dp.toPx(), center = Offset(cx - 0.8.dp.toPx(), y - 0.8.dp.toPx()))
        }

        // 중앙 조준경/별 카툰 메달 장식
        drawCircle(color = DeepNavyOutline, radius = 9.5.dp.toPx(), center = Offset(cx, cy))
        drawCircle(color = Color(0xFF1B3558), radius = 7.dp.toPx(), center = Offset(cx, cy))
        val star = Path().apply {
            val r = 5.dp.toPx()
            moveTo(cx, cy - r)
            lineTo(cx + r * 0.42f, cy - r * 0.42f)
            lineTo(cx + r, cy)
            lineTo(cx + r * 0.42f, cy + r * 0.42f)
            lineTo(cx, cy + r)
            lineTo(cx - r * 0.42f, cy + r * 0.42f)
            lineTo(cx - r, cy)
            lineTo(cx - r * 0.42f, cy - r * 0.42f)
            close()
        }
        drawPath(star, color = accentColor)
        drawCircle(color = Color.White, radius = 1.6.dp.toPx(), center = Offset(cx, cy))
    }
}

