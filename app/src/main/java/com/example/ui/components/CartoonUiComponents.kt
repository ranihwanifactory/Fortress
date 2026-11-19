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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepNavyOutline

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
