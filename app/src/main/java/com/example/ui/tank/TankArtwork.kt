package com.example.ui.tank

import android.graphics.Canvas as NativeCanvas
import android.graphics.LinearGradient
import android.graphics.Paint as NativePaint
import android.graphics.Path as NativePath
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.game.model.TankExpression
import com.example.game.model.TankType
import com.example.ui.theme.DeepNavyOutline
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 7종 포탄별 커스텀 카툰 탱크를 Compose DrawScope(전장/프로필/랭킹/대기실/격납고)에 일관되게 렌더링합니다.
 * 기준 좌표계: (0, 0)이 탱크 바닥 중심(바퀴 접지점)이며, 기본 폭 약 56px, 높이 약 44px 스케일입니다.
 */
fun DrawScope.drawThemedTankBody(
    tankType: TankType,
    aimAngleDeg: Float = 38f,
    facingRight: Boolean = true,
    recoil: Float = 0f,
    expression: TankExpression = TankExpression.NORMAL,
    animTime: Float = 0f,
    hasShield: Boolean = false,
    poisonTurns: Int = 0
) {
    val barrelRad = if (facingRight) {
        -aimAngleDeg * (PI.toFloat() / 180f)
    } else {
        (180f + aimAngleDeg) * (PI.toFloat() / 180f)
    }
    val dir = if (facingRight) 1f else -1f
    val turretCenter = Offset(0f, -24f)

    // 1. 후방/외부 특수 장비 (포신·터렛 뒤에 배치)
    when (tankType) {
        TankType.TWIN_STRIKER -> {
            // 트윈 제트 부스터 파이프
            val bx = -dir * 21f
            drawRoundRect(
                color = DeepNavyOutline,
                topLeft = Offset(bx - 7f, -26f),
                size = Size(14f, 12f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = Color(0xFF023E8A),
                topLeft = Offset(bx - 5f, -24f),
                size = Size(10f, 8f),
                cornerRadius = CornerRadius(3f, 3f)
            )
            val flameLen = 5f + sin(animTime * 12f) * 2f
            drawCircle(
                color = Color(0xFF90E0EF),
                radius = flameLen.coerceAtLeast(3f),
                center = Offset(bx - dir * 8f, -20f)
            )
        }
        TankType.TOXIC_CHEMIST -> {
            // 등쪽 네온 독가스 탱크 캐니스터 2기
            val tankX = -dir * 18f
            drawRoundRect(
                color = DeepNavyOutline,
                topLeft = Offset(tankX - 8f, -38f),
                size = Size(16f, 22f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = Color(0xFF70E000),
                topLeft = Offset(tankX - 5.5f, -35f),
                size = Size(11f, 16f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            val bubbleY = -24f - ((animTime * 14f) % 9f)
            drawCircle(color = Color(0xFFCCFF33), radius = 2.4f, center = Offset(tankX - 1f, bubbleY))
            drawCircle(color = Color.White.copy(alpha = 0.7f), radius = 1.6f, center = Offset(tankX + 2f, -31f))
        }
        TankType.JUNK_SCRAPPER -> {
            // 삐딱한 고철 안테나 + 톱니바퀴 장식
            val ax = -dir * 17f
            drawLine(
                color = DeepNavyOutline,
                start = Offset(ax, -22f),
                end = Offset(ax - dir * 6f, -43f),
                strokeWidth = 3.5f,
                cap = StrokeCap.Round
            )
            drawCircle(color = DeepNavyOutline, radius = 4.5f, center = Offset(ax - dir * 6f, -43f))
            drawCircle(color = Color(0xFFFFB703), radius = 2.7f, center = Offset(ax - dir * 6f, -43f))
        }
        TankType.AQUA_HYDRO -> {
            // 후방 물탱크 지느러미
            val fx = -dir * 19f
            val finPath = Path().apply {
                moveTo(fx, -22f)
                lineTo(fx - dir * 10f, -34f)
                lineTo(fx - dir * 3f, -15f)
                close()
            }
            drawPath(finPath, color = Color(0xFF0077B6))
            drawPath(finPath, color = DeepNavyOutline, style = Stroke(width = 2.6f, join = StrokeJoin.Round))
        }
        else -> Unit
    }

    // 2. 방어막 & 악취 오라
    if (hasShield) {
        drawCircle(color = Color(0x44A0C4FF), radius = 38f, center = Offset(0f, -20f))
        drawCircle(color = Color(0xFFA0C4FF), radius = 38f, center = Offset(0f, -20f), style = Stroke(width = 3.2f))
    }
    if (poisonTurns > 0) {
        for (i in 0..2) {
            val ox = -12f + i * 12f + sin(animTime * 4f + i) * 3f
            val oy = -42f - ((animTime * 18f + i * 7f) % 14f)
            drawCircle(color = Color(0xAA70E000), radius = 4.2f, center = Offset(ox, oy))
        }
    }

    // 3. 탱크 종류별 시그니처 포신!
    val cosB = cos(barrelRad)
    val sinB = sin(barrelRad)
    val perpX = -sinB
    val perpY = cosB

    when (tankType) {
        TankType.TWIN_STRIKER -> {
            // 쌍포신 (위/아래 평행 2열 고속 포신)
            listOf(-4.2f, 4.2f).forEach { offset ->
                val start = Offset(
                    turretCenter.x + cosB * (6f - recoil * 0.5f) + perpX * offset,
                    turretCenter.y + sinB * (6f - recoil * 0.5f) + perpY * offset
                )
                val end = Offset(
                    turretCenter.x + cosB * (36f - recoil) + perpX * offset,
                    turretCenter.y + sinB * (36f - recoil) + perpY * offset
                )
                drawLine(DeepNavyOutline, start, end, strokeWidth = 9.5f, cap = StrokeCap.Round)
                drawLine(tankType.accentColor, start, end, strokeWidth = 5.4f, cap = StrokeCap.Round)
                drawCircle(DeepNavyOutline, radius = 4.6f, center = end)
                drawCircle(Color(0xFF90E0EF), radius = 2.6f, center = end)
            }
        }
        TankType.MEGA_TITAN -> {
            // 초대형 공성 거포 (굵고 묵직한 황금 링 포신)
            val start = Offset(turretCenter.x + cosB * (4f - recoil * 0.5f), turretCenter.y + sinB * (4f - recoil * 0.5f))
            val end = Offset(turretCenter.x + cosB * (38f - recoil), turretCenter.y + sinB * (38f - recoil))
            drawLine(DeepNavyOutline, start, end, strokeWidth = 20f, cap = StrokeCap.Round)
            drawLine(tankType.primaryColor, start, end, strokeWidth = 13.5f, cap = StrokeCap.Round)
            val mid = Offset(turretCenter.x + cosB * (24f - recoil * 0.7f), turretCenter.y + sinB * (24f - recoil * 0.7f))
            drawCircle(DeepNavyOutline, radius = 9.5f, center = mid)
            drawCircle(tankType.trimColor, radius = 6.5f, center = mid)
            drawCircle(DeepNavyOutline, radius = 10.5f, center = end)
            drawCircle(Color(0xFFFF7B00), radius = 7.0f, center = end)
            drawCircle(Color(0xFF3A0CA3), radius = 3.4f, center = end)
        }
        TankType.AQUA_HYDRO -> {
            // 투명 물줄기 코어가 보이는 하이드로 노즐
            val start = Offset(turretCenter.x + cosB * (6f - recoil * 0.5f), turretCenter.y + sinB * (6f - recoil * 0.5f))
            val end = Offset(turretCenter.x + cosB * (36f - recoil), turretCenter.y + sinB * (36f - recoil))
            drawLine(DeepNavyOutline, start, end, strokeWidth = 15f, cap = StrokeCap.Round)
            drawLine(Color(0xFF0077B6), start, end, strokeWidth = 10f, cap = StrokeCap.Round)
            drawLine(Color(0xFFCAF0F8), start, end, strokeWidth = 4.5f, cap = StrokeCap.Round)
            drawCircle(DeepNavyOutline, radius = 8.5f, center = end)
            drawCircle(Color(0xFF00BBF9), radius = 5.6f, center = end)
            drawCircle(Color.White, radius = 2.4f, center = Offset(end.x - 1.5f, end.y - 1.5f))
        }
        TankType.POOP_KING -> {
            // 뚫어뻥(플런저) 스타일 유쾌한 똥포탄 발사기!
            val start = Offset(turretCenter.x + cosB * (6f - recoil * 0.5f), turretCenter.y + sinB * (6f - recoil * 0.5f))
            val end = Offset(turretCenter.x + cosB * (34f - recoil), turretCenter.y + sinB * (34f - recoil))
            drawLine(DeepNavyOutline, start, end, strokeWidth = 14f, cap = StrokeCap.Round)
            drawLine(tankType.accentColor, start, end, strokeWidth = 8.5f, cap = StrokeCap.Round)
            // 끝단 나팔 모양 컵
            drawCircle(DeepNavyOutline, radius = 10f, center = end)
            drawCircle(Color(0xFFD97706), radius = 7f, center = end)
            drawCircle(Color(0xFF582F0E), radius = 3.8f, center = end)
        }
        TankType.TOXIC_CHEMIST -> {
            // 독가스 분사 캐논 + 방사능 옐로우 링
            val start = Offset(turretCenter.x + cosB * (6f - recoil * 0.5f), turretCenter.y + sinB * (6f - recoil * 0.5f))
            val end = Offset(turretCenter.x + cosB * (36f - recoil), turretCenter.y + sinB * (36f - recoil))
            drawLine(DeepNavyOutline, start, end, strokeWidth = 15.5f, cap = StrokeCap.Round)
            drawLine(Color(0xFF2B2D42), start, end, strokeWidth = 10f, cap = StrokeCap.Round)
            drawLine(Color(0xFF70E000), start, end, strokeWidth = 4f, cap = StrokeCap.Round)
            drawCircle(DeepNavyOutline, radius = 8.8f, center = end)
            drawCircle(Color(0xFFCCFF33), radius = 5.8f, center = end)
        }
        TankType.JUNK_SCRAPPER -> {
            // 나팔형 고철 투석 깔때기 포신
            val start = Offset(turretCenter.x + cosB * (5f - recoil * 0.5f), turretCenter.y + sinB * (5f - recoil * 0.5f))
            val end = Offset(turretCenter.x + cosB * (35f - recoil), turretCenter.y + sinB * (35f - recoil))
            val funnel = Path().apply {
                moveTo(start.x + perpX * 5f, start.y + perpY * 5f)
                lineTo(end.x + perpX * 10.5f, end.y + perpY * 10.5f)
                lineTo(end.x - perpX * 10.5f, end.y - perpY * 10.5f)
                lineTo(start.x - perpX * 5f, start.y - perpY * 5f)
                close()
            }
            drawPath(funnel, color = tankType.accentColor)
            drawPath(funnel, color = DeepNavyOutline, style = Stroke(width = 3.5f, join = StrokeJoin.Round))
            drawCircle(DeepNavyOutline, radius = 7.5f, center = end)
            drawCircle(Color(0xFFFFB703), radius = 4.6f, center = end)
        }
        TankType.CLASSIC -> {
            val start = Offset(turretCenter.x + cosB * (6f - recoil * 0.5f), turretCenter.y + sinB * (6f - recoil * 0.5f))
            val end = Offset(turretCenter.x + cosB * (36f - recoil), turretCenter.y + sinB * (36f - recoil))
            drawLine(DeepNavyOutline, start, end, strokeWidth = 14.5f, cap = StrokeCap.Round)
            drawLine(tankType.accentColor, start, end, strokeWidth = 9f, cap = StrokeCap.Round)
            drawCircle(DeepNavyOutline, radius = 7.6f, center = end)
            drawCircle(tankType.trimColor, radius = 4.8f, center = end)
        }
    }

    // 4. 탱크 종류별 커스텀 돔 터렛 & 상단 장식
    when (tankType) {
        TankType.AQUA_HYDRO -> {
            // 반투명 물방울 유리 돔 + 내부 수면 파동
            drawCircle(DeepNavyOutline, radius = 18.5f, center = turretCenter)
            drawCircle(Color(0xFF0077B6), radius = 15.5f, center = turretCenter)
            drawCircle(Color(0xFF90E0EF), radius = 12f, center = Offset(turretCenter.x, turretCenter.y - 2f))
            drawCircle(Color.White.copy(alpha = 0.65f), radius = 4.5f, center = Offset(turretCenter.x - 5f, turretCenter.y - 7f))
        }
        TankType.MEGA_TITAN -> {
            // 각진 중장갑 장갑판 터렛
            drawRoundRect(
                color = DeepNavyOutline,
                topLeft = Offset(-20f, -42f),
                size = Size(40f, 24f),
                cornerRadius = CornerRadius(9f, 9f)
            )
            drawRoundRect(
                color = tankType.primaryColor,
                topLeft = Offset(-17f, -39f),
                size = Size(34f, 18f),
                cornerRadius = CornerRadius(7f, 7f)
            )
            // 황금 방패 띠
            drawRect(color = tankType.trimColor, topLeft = Offset(-16f, -36f), size = Size(32f, 4f))
        }
        TankType.JUNK_SCRAPPER -> {
            // 리벳 패치워크 철제 돔 + 찌그러진 뚜껑
            drawRoundRect(
                color = DeepNavyOutline,
                topLeft = Offset(-18f, -40f),
                size = Size(36f, 22f),
                cornerRadius = CornerRadius(7f, 7f)
            )
            drawRoundRect(
                color = tankType.primaryColor,
                topLeft = Offset(-15f, -37f),
                size = Size(30f, 16f),
                cornerRadius = CornerRadius(5f, 5f)
            )
            // 용접 패치 & 리벳 점
            drawRoundRect(
                color = Color(0xFFFFB703),
                topLeft = Offset(-12f, -35f),
                size = Size(10f, 7f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            drawCircle(DeepNavyOutline, radius = 1.4f, center = Offset(-10f, -33f))
            drawCircle(DeepNavyOutline, radius = 1.4f, center = Offset(-4f, -30f))
        }
        else -> {
            drawCircle(DeepNavyOutline, radius = 17.5f, center = turretCenter)
            drawCircle(tankType.primaryColor, radius = 14.5f, center = turretCenter)
            drawCircle(
                color = Color.White.copy(alpha = 0.30f),
                radius = 5f,
                center = Offset(turretCenter.x - 4f, turretCenter.y - 6f)
            )
        }
    }

    // 5. 터렛 꼭대기 아이코닉 장식!
    when (tankType) {
        TankType.POOP_KING -> {
            // 3단 소용돌이 카툰 응가 크레스트 + 미니 황금 왕관!
            drawOval(DeepNavyOutline, Offset(-12f, -44f), Size(24f, 10f))
            drawOval(Color(0xFF7F4F24), Offset(-10f, -42.5f), Size(20f, 7f))
            drawOval(DeepNavyOutline, Offset(-8.5f, -50f), Size(17f, 9f))
            drawOval(Color(0xFFA98467), Offset(-6.8f, -48.5f), Size(13.6f, 6.2f))
            // 미니 황금 왕관
            val crown = Path().apply {
                moveTo(-5.5f, -50f)
                lineTo(-7f, -57f)
                lineTo(-2.5f, -53.5f)
                lineTo(0f, -59f)
                lineTo(2.5f, -53.5f)
                lineTo(7f, -57f)
                lineTo(5.5f, -50f)
                close()
            }
            drawPath(crown, color = Color(0xFFFFD166))
            drawPath(crown, color = DeepNavyOutline, style = Stroke(width = 1.8f, join = StrokeJoin.Round))
        }
        TankType.CLASSIC -> {
            // 정통 황금 별 헬멧 배지
            drawRoundRect(
                color = DeepNavyOutline,
                topLeft = Offset(-9f, -43.5f),
                size = Size(18f, 6.5f),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawRoundRect(
                color = tankType.trimColor,
                topLeft = Offset(-7f, -42f),
                size = Size(14f, 3.5f),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }
        TankType.TOXIC_CHEMIST -> {
            // 방사능 경고 캡
            drawCircle(DeepNavyOutline, radius = 6.5f, center = Offset(0f, -42f))
            drawCircle(Color(0xFFCCFF33), radius = 4.4f, center = Offset(0f, -42f))
            drawCircle(DeepNavyOutline, radius = 1.8f, center = Offset(0f, -42f))
        }
        TankType.AQUA_HYDRO -> {
            // 물방울 뿔 안테나
            drawCircle(DeepNavyOutline, radius = 5.5f, center = Offset(0f, -44f))
            drawCircle(Color(0xFFCAF0F8), radius = 3.5f, center = Offset(0f, -44f))
        }
        else -> Unit
    }

    // 6. 차체 몸통 (탱크별 스타일링)
    val hullW = if (tankType == TankType.MEGA_TITAN) 56f else 50f
    drawRoundRect(
        color = DeepNavyOutline,
        topLeft = Offset(-hullW * 0.5f, -23f),
        size = Size(hullW, 16f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = tankType.primaryColor,
        topLeft = Offset(-hullW * 0.5f + 3f, -20f),
        size = Size(hullW - 6f, 11f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    // 차체 포인트 스트라이프
    when (tankType) {
        TankType.TOXIC_CHEMIST -> {
            // 노랑-검정 위험 줄무늬
            for (sx in listOf(-14f, -4f, 6f, 14f)) {
                drawRect(color = Color(0xFFFFD166), topLeft = Offset(sx, -19.5f), size = Size(4.5f, 9f))
            }
        }
        TankType.TWIN_STRIKER -> {
            // 고속 화이트 레이싱 라인
            drawRect(color = Color.White.copy(alpha = 0.85f), topLeft = Offset(-18f, -16f), size = Size(36f, 3.2f))
        }
        TankType.MEGA_TITAN -> {
            // 전면 스파이크 범퍼
            val sx = dir * (hullW * 0.5f)
            val spike = Path().apply {
                moveTo(sx, -20f)
                lineTo(sx + dir * 7f, -15f)
                lineTo(sx, -10f)
                close()
            }
            drawPath(spike, color = tankType.trimColor)
            drawPath(spike, color = DeepNavyOutline, style = Stroke(width = 2.2f, join = StrokeJoin.Round))
        }
        else -> {
            drawRect(color = tankType.trimColor.copy(alpha = 0.85f), topLeft = Offset(-16f, -15.5f), size = Size(32f, 2.8f))
        }
    }

    // 7. 무한궤도 & 바퀴
    val trackW = if (tankType == TankType.MEGA_TITAN) 60f else 54f
    drawRoundRect(
        color = DeepNavyOutline,
        topLeft = Offset(-trackW * 0.5f, -11f),
        size = Size(trackW, 12f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = Color(0xFF4A5759),
        topLeft = Offset(-trackW * 0.5f + 3f, -8.5f),
        size = Size(trackW - 6f, 7f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    val wheelXs = if (tankType == TankType.MEGA_TITAN) listOf(-19f, -6.5f, 6.5f, 19f) else listOf(-15f, 0f, 15f)
    wheelXs.forEach { wx ->
        drawCircle(color = tankType.trimColor, radius = 2.9f, center = Offset(wx, -5f))
    }

    // 8. 큰 카툰 눈 표정
    val eyeOffsetX = dir * 6f
    val pupilOffsetX = dir * 2.2f
    val eyeCenter = Offset(turretCenter.x + eyeOffsetX, turretCenter.y - 2f)
    drawCircle(DeepNavyOutline, radius = 7.2f, center = eyeCenter)
    drawCircle(Color.White, radius = 5.8f, center = eyeCenter)

    when (expression) {
        TankExpression.DIZZY -> {
            drawLine(DeepNavyOutline, Offset(eyeCenter.x - 3.2f, eyeCenter.y - 3.2f), Offset(eyeCenter.x + 3.2f, eyeCenter.y + 3.2f), 2.5f, StrokeCap.Round)
            drawLine(DeepNavyOutline, Offset(eyeCenter.x + 3.2f, eyeCenter.y - 3.2f), Offset(eyeCenter.x - 3.2f, eyeCenter.y + 3.2f), 2.5f, StrokeCap.Round)
        }
        TankExpression.HAPPY -> {
            drawArc(
                color = DeepNavyOutline,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(eyeCenter.x - 3.5f, eyeCenter.y - 2f),
                size = Size(7f, 5f),
                style = Stroke(width = 2.2f, cap = StrokeCap.Round)
            )
        }
        TankExpression.CHARGING, TankExpression.NORMAL -> {
            drawCircle(
                color = DeepNavyOutline,
                radius = if (expression == TankExpression.CHARGING) 2.1f else 2.9f,
                center = Offset(eyeCenter.x + pupilOffsetX, eyeCenter.y)
            )
            drawCircle(
                color = Color.White,
                radius = 1.1f,
                center = Offset(eyeCenter.x + pupilOffsetX - 0.8f, eyeCenter.y - 1.0f)
            )
        }
    }
}

/**
 * 프로필, 타이틀, 격납고, 대기실, 글로벌 랭킹 카드에 탱크 일러스트를 멋지게 표시하는 재사용 Compose 컴포넌트.
 */
@Composable
fun TankAvatar(
    tankType: TankType,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    facingRight: Boolean = true,
    aimAngleDeg: Float = 32f,
    showPedestal: Boolean = false,
    circularFrame: Boolean = false,
    highlighted: Boolean = false
) {
    val shape = if (circularFrame) CircleShape else RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        tankType.accentColor.copy(alpha = 0.42f),
                        tankType.primaryColor.copy(alpha = 0.24f),
                        Color(0xFF0E1D33)
                    )
                )
            )
            .border(
                width = if (highlighted) 2.5.dp else 1.5.dp,
                color = if (highlighted) tankType.trimColor else tankType.accentColor.copy(alpha = 0.65f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scaleFactor = (minOf(this.size.width, this.size.height) / 86f)
            val baseY = if (showPedestal) this.size.height * 0.74f else this.size.height * 0.72f
            val centerX = this.size.width * (if (facingRight) 0.46f else 0.54f)

            if (showPedestal) {
                drawOval(
                    color = tankType.trimColor.copy(alpha = 0.22f),
                    topLeft = Offset(this.size.width * 0.14f, baseY - 6f * scaleFactor),
                    size = Size(this.size.width * 0.72f, 15f * scaleFactor)
                )
                drawOval(
                    color = tankType.trimColor.copy(alpha = 0.7f),
                    topLeft = Offset(this.size.width * 0.18f, baseY - 4f * scaleFactor),
                    size = Size(this.size.width * 0.64f, 10f * scaleFactor),
                    style = Stroke(width = 1.8f * scaleFactor)
                )
            }

            withTransform({
                translate(left = centerX, top = baseY)
                scale(scaleX = scaleFactor, scaleY = scaleFactor, pivot = Offset.Zero)
            }) {
                drawThemedTankBody(
                    tankType = tankType,
                    aimAngleDeg = aimAngleDeg,
                    facingRight = facingRight,
                    expression = TankExpression.NORMAL
                )
            }
        }
    }
}

/**
 * 명예의 전당 PNG 캡쳐 이미지(android.graphics.Canvas)에서도 동일한 7종 커스텀 탱크를 선명하게 렌더링합니다.
 */
fun drawThemedTankOnNativeCanvas(
    canvas: NativeCanvas,
    tankType: TankType,
    cx: Float,
    baseY: Float,
    scale: Float,
    facingRight: Boolean = true,
    aimAngleDeg: Float = 32f,
    showSpotlight: Boolean = false
) {
    val outline = 0xFF14213D.toInt()
    val primary = tankType.primaryColor.toArgb()
    val accent = tankType.accentColor.toArgb()
    val trim = tankType.trimColor.toArgb()
    val dir = if (facingRight) 1f else -1f

    canvas.save()
    canvas.translate(cx, baseY)
    canvas.scale(scale, scale)

    val p = NativePaint(NativePaint.ANTI_ALIAS_FLAG)

    if (showSpotlight) {
        p.shader = RadialGradient(0f, -22f, 52f, (trim and 0x00FFFFFF) or 0x55000000, 0x00000000, Shader.TileMode.CLAMP)
        canvas.drawCircle(0f, -22f, 52f, p)
        p.shader = null

        p.color = 0x44FFD166
        canvas.drawOval(RectF(-38f, -5f, 38f, 9f), p)
        p.style = NativePaint.Style.STROKE
        p.strokeWidth = 2f
        p.color = trim
        canvas.drawOval(RectF(-34f, -3f, 34f, 7f), p)
        p.style = NativePaint.Style.FILL
    }

    // 1. 후방 특수 장비
    when (tankType) {
        TankType.TWIN_STRIKER -> {
            val bx = -dir * 21f
            p.color = outline
            canvas.drawRoundRect(RectF(bx - 7f, -26f, bx + 7f, -14f), 4f, 4f, p)
            p.color = 0xFF023E8A.toInt()
            canvas.drawRoundRect(RectF(bx - 5f, -24f, bx + 5f, -16f), 3f, 3f, p)
            p.color = 0xFF90E0EF.toInt()
            canvas.drawCircle(bx - dir * 8f, -20f, 5f, p)
        }
        TankType.TOXIC_CHEMIST -> {
            val tx = -dir * 18f
            p.color = outline
            canvas.drawRoundRect(RectF(tx - 8f, -38f, tx + 8f, -16f), 6f, 6f, p)
            p.color = 0xFF70E000.toInt()
            canvas.drawRoundRect(RectF(tx - 5.5f, -35f, tx + 5.5f, -19f), 4f, 4f, p)
            p.color = 0xFFCCFF33.toInt()
            canvas.drawCircle(tx, -29f, 2.5f, p)
        }
        TankType.JUNK_SCRAPPER -> {
            val ax = -dir * 17f
            p.color = outline
            p.strokeWidth = 3.5f
            p.strokeCap = NativePaint.Cap.ROUND
            canvas.drawLine(ax, -22f, ax - dir * 6f, -43f, p)
            canvas.drawCircle(ax - dir * 6f, -43f, 4.5f, p)
            p.color = 0xFFFFB703.toInt()
            canvas.drawCircle(ax - dir * 6f, -43f, 2.6f, p)
        }
        TankType.AQUA_HYDRO -> {
            val fx = -dir * 19f
            val fin = NativePath().apply {
                moveTo(fx, -22f)
                lineTo(fx - dir * 10f, -34f)
                lineTo(fx - dir * 3f, -15f)
                close()
            }
            p.color = 0xFF0077B6.toInt()
            canvas.drawPath(fin, p)
            p.style = NativePaint.Style.STROKE
            p.strokeWidth = 2.5f
            p.color = outline
            canvas.drawPath(fin, p)
            p.style = NativePaint.Style.FILL
        }
        else -> Unit
    }

    // 2. 포신
    val barrelRad = if (facingRight) -aimAngleDeg * (PI / 180.0) else (180.0 + aimAngleDeg) * (PI / 180.0)
    val cosB = cos(barrelRad).toFloat()
    val sinB = sin(barrelRad).toFloat()
    val perpX = -sinB
    val perpY = cosB
    val tcy = -24f

    p.strokeCap = NativePaint.Cap.ROUND
    when (tankType) {
        TankType.TWIN_STRIKER -> {
            listOf(-4.2f, 4.2f).forEach { off ->
                val sx = cosB * 6f + perpX * off
                val sy = tcy + sinB * 6f + perpY * off
                val ex = cosB * 36f + perpX * off
                val ey = tcy + sinB * 36f + perpY * off
                p.color = outline; p.strokeWidth = 9.5f; canvas.drawLine(sx, sy, ex, ey, p)
                p.color = accent; p.strokeWidth = 5.4f; canvas.drawLine(sx, sy, ex, ey, p)
                p.color = outline; canvas.drawCircle(ex, ey, 4.6f, p)
                p.color = 0xFF90E0EF.toInt(); canvas.drawCircle(ex, ey, 2.6f, p)
            }
        }
        TankType.MEGA_TITAN -> {
            val sx = cosB * 4f; val sy = tcy + sinB * 4f
            val ex = cosB * 38f; val ey = tcy + sinB * 38f
            p.color = outline; p.strokeWidth = 20f; canvas.drawLine(sx, sy, ex, ey, p)
            p.color = primary; p.strokeWidth = 13.5f; canvas.drawLine(sx, sy, ex, ey, p)
            p.color = outline; canvas.drawCircle(ex, ey, 10.5f, p)
            p.color = 0xFFFF7B00.toInt(); canvas.drawCircle(ex, ey, 7.0f, p)
            p.color = 0xFF3A0CA3.toInt(); canvas.drawCircle(ex, ey, 3.4f, p)
        }
        TankType.JUNK_SCRAPPER -> {
            val sx = cosB * 5f; val sy = tcy + sinB * 5f
            val ex = cosB * 35f; val ey = tcy + sinB * 35f
            val funnel = NativePath().apply {
                moveTo(sx + perpX * 5f, sy + perpY * 5f)
                lineTo(ex + perpX * 10.5f, ey + perpY * 10.5f)
                lineTo(ex - perpX * 10.5f, ey - perpY * 10.5f)
                lineTo(sx - perpX * 5f, sy - perpY * 5f)
                close()
            }
            p.color = accent; canvas.drawPath(funnel, p)
            p.style = NativePaint.Style.STROKE; p.strokeWidth = 3.5f; p.color = outline; canvas.drawPath(funnel, p)
            p.style = NativePaint.Style.FILL
            p.color = outline; canvas.drawCircle(ex, ey, 7.5f, p)
            p.color = trim; canvas.drawCircle(ex, ey, 4.6f, p)
        }
        else -> {
            val sx = cosB * 6f; val sy = tcy + sinB * 6f
            val ex = cosB * 36f; val ey = tcy + sinB * 36f
            p.color = outline; p.strokeWidth = 15f; canvas.drawLine(sx, sy, ex, ey, p)
            p.color = if (tankType == TankType.TOXIC_CHEMIST) 0xFF2B2D42.toInt() else accent
            p.strokeWidth = 9.5f; canvas.drawLine(sx, sy, ex, ey, p)
            if (tankType == TankType.AQUA_HYDRO || tankType == TankType.TOXIC_CHEMIST) {
                p.color = if (tankType == TankType.AQUA_HYDRO) 0xFFCAF0F8.toInt() else 0xFF70E000.toInt()
                p.strokeWidth = 4f; canvas.drawLine(sx, sy, ex, ey, p)
            }
            val tipR = if (tankType == TankType.POOP_KING) 10f else 8f
            p.color = outline; canvas.drawCircle(ex, ey, tipR, p)
            p.color = if (tankType == TankType.POOP_KING) 0xFFD97706.toInt() else trim
            canvas.drawCircle(ex, ey, tipR * 0.68f, p)
        }
    }

    // 3. 터렛
    when (tankType) {
        TankType.AQUA_HYDRO -> {
            p.color = outline; canvas.drawCircle(0f, tcy, 18.5f, p)
            p.color = 0xFF0077B6.toInt(); canvas.drawCircle(0f, tcy, 15.5f, p)
            p.color = 0xFF90E0EF.toInt(); canvas.drawCircle(0f, tcy - 2f, 12f, p)
            p.color = 0xAAFFFFFF.toInt(); canvas.drawCircle(-5f, tcy - 7f, 4.5f, p)
        }
        TankType.MEGA_TITAN -> {
            p.color = outline; canvas.drawRoundRect(RectF(-20f, -42f, 20f, -18f), 9f, 9f, p)
            p.color = primary; canvas.drawRoundRect(RectF(-17f, -39f, 17f, -21f), 7f, 7f, p)
            p.color = trim; canvas.drawRect(RectF(-16f, -36f, 16f, -32f), p)
        }
        TankType.JUNK_SCRAPPER -> {
            p.color = outline; canvas.drawRoundRect(RectF(-18f, -40f, 18f, -18f), 7f, 7f, p)
            p.color = primary; canvas.drawRoundRect(RectF(-15f, -37f, 15f, -21f), 5f, 5f, p)
            p.color = trim; canvas.drawRoundRect(RectF(-12f, -35f, -2f, -28f), 2f, 2f, p)
        }
        else -> {
            p.color = outline; canvas.drawCircle(0f, tcy, 17.5f, p)
            p.color = primary; canvas.drawCircle(0f, tcy, 14.5f, p)
            p.color = 0x55FFFFFF; canvas.drawCircle(-4f, tcy - 6f, 5f, p)
        }
    }

    // 4. 터렛 상단 크레스트
    if (tankType == TankType.POOP_KING) {
        p.color = outline; canvas.drawOval(RectF(-12f, -44f, 12f, -34f), p)
        p.color = 0xFF7F4F24.toInt(); canvas.drawOval(RectF(-10f, -42.5f, 10f, -35.5f), p)
        p.color = outline; canvas.drawOval(RectF(-8.5f, -50f, 8.5f, -41f), p)
        p.color = 0xFFA98467.toInt(); canvas.drawOval(RectF(-6.8f, -48.5f, 6.8f, -42.3f), p)
        val crown = NativePath().apply {
            moveTo(-5.5f, -50f); lineTo(-7f, -57f); lineTo(-2.5f, -53.5f)
            lineTo(0f, -59f); lineTo(2.5f, -53.5f); lineTo(7f, -57f); lineTo(5.5f, -50f); close()
        }
        p.shader = LinearGradient(0f, -59f, 0f, -50f, 0xFFFFF59D.toInt(), 0xFFFFA000.toInt(), Shader.TileMode.CLAMP)
        canvas.drawPath(crown, p)
        p.shader = null
        p.style = NativePaint.Style.STROKE; p.strokeWidth = 1.8f; p.color = outline; canvas.drawPath(crown, p)
        p.style = NativePaint.Style.FILL
    } else if (tankType == TankType.CLASSIC) {
        p.color = outline; canvas.drawRoundRect(RectF(-9f, -43.5f, 9f, -37f), 3f, 3f, p)
        p.color = trim; canvas.drawRoundRect(RectF(-7f, -42f, 7f, -38.5f), 2f, 2f, p)
    } else if (tankType == TankType.TOXIC_CHEMIST) {
        p.color = outline; canvas.drawCircle(0f, -42f, 6.5f, p)
        p.color = 0xFFCCFF33.toInt(); canvas.drawCircle(0f, -42f, 4.4f, p)
        p.color = outline; canvas.drawCircle(0f, -42f, 1.8f, p)
    }

    // 5. 차체 & 무한궤도
    val hullW = if (tankType == TankType.MEGA_TITAN) 56f else 50f
    p.color = outline; canvas.drawRoundRect(RectF(-hullW * 0.5f, -23f, hullW * 0.5f, -7f), 8f, 8f, p)
    p.color = primary; canvas.drawRoundRect(RectF(-hullW * 0.5f + 3f, -20f, hullW * 0.5f - 3f, -9f), 6f, 6f, p)
    p.color = trim; canvas.drawRect(RectF(-16f, -15.5f, 16f, -12.7f), p)

    val trackW = if (tankType == TankType.MEGA_TITAN) 60f else 54f
    p.color = outline; canvas.drawRoundRect(RectF(-trackW * 0.5f, -11f, trackW * 0.5f, 1f), 6f, 6f, p)
    p.color = 0xFF4A5759.toInt(); canvas.drawRoundRect(RectF(-trackW * 0.5f + 3f, -8.5f, trackW * 0.5f - 3f, -1.5f), 4f, 4f, p)
    val wheelXs = if (tankType == TankType.MEGA_TITAN) listOf(-19f, -6.5f, 6.5f, 19f) else listOf(-15f, 0f, 15f)
    p.color = trim
    wheelXs.forEach { wx -> canvas.drawCircle(wx, -5f, 2.9f, p) }

    // 6. 눈
    val eyeX = dir * 6f
    val eyeY = tcy - 2f
    p.color = outline; canvas.drawCircle(eyeX, eyeY, 7.2f, p)
    p.color = 0xFFFFFFFF.toInt(); canvas.drawCircle(eyeX, eyeY, 5.8f, p)
    p.color = outline; canvas.drawCircle(eyeX + dir * 2.2f, eyeY, 2.9f, p)
    p.color = 0xFFFFFFFF.toInt(); canvas.drawCircle(eyeX + dir * 2.2f - 0.8f, eyeY - 1f, 1.1f, p)

    canvas.restore()
}
