package com.example.ui.battle

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.game.engine.BattleEngine
import com.example.game.engine.DestructibleTerrain
import com.example.game.model.BattlePropState
import com.example.game.model.BattleRenderSnapshot
import com.example.game.model.MapTheme
import com.example.game.model.ProjectileState
import com.example.game.model.PropType
import com.example.game.model.SceneryKind
import com.example.game.model.SceneryProp
import com.example.game.model.TankExpression
import com.example.game.model.TankState
import com.example.game.model.TurnPhase
import com.example.game.model.WeaponType
import com.example.ui.tank.drawThemedTankBody
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepNavyOutline
import com.example.ui.theme.PrimaryGold
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun BattleCanvas(
    snapshot: BattleRenderSnapshot,
    terrain: DestructibleTerrain,
    onPanCamera: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val juaTypeface = remember(context) {
        ResourcesCompat.getFont(context, R.font.jua) ?: Typeface.DEFAULT_BOLD
    }

    val popupFillPaint = remember(juaTypeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 32f
            typeface = juaTypeface
        }
    }
    val popupStrokePaint = remember(juaTypeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 32f
            typeface = juaTypeface
            style = Paint.Style.STROKE
            strokeWidth = 6.5f
            color = DeepNavyOutline.toArgb()
        }
    }
    val namePaint = remember(juaTypeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 20f
            typeface = juaTypeface
            color = Color.White.toArgb()
        }
    }
    val nameStrokePaint = remember(juaTypeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 20f
            typeface = juaTypeface
            style = Paint.Style.STROKE
            strokeWidth = 4.5f
            color = DeepNavyOutline.toArgb()
        }
    }
    val propLabelPaint = remember(juaTypeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 14f
            typeface = juaTypeface
            color = DeepNavyOutline.toArgb()
        }
    }
    val fuelTextPaint = remember(juaTypeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 14f
            typeface = juaTypeface
            color = Color.White.toArgb()
        }
    }
    val fuelStrokePaint = remember(juaTypeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 14f
            typeface = juaTypeface
            style = Paint.Style.STROKE
            strokeWidth = 3.8f
            strokeJoin = Paint.Join.ROUND
            color = DeepNavyOutline.toArgb()
        }
    }

    val srcRect = remember(terrain.maskWidth, terrain.maskHeight) {
        Rect(0, 0, terrain.maskWidth, terrain.maskHeight)
    }
    val dstRect = remember(terrain.worldWidth, terrain.worldHeight) {
        Rect(0, 0, terrain.worldWidth.toInt(), terrain.worldHeight.toInt())
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    onPanCamera(-dragAmount * 1.4f)
                }
            }
    ) {
        val canvasW = size.width
        val canvasH = size.height
        if (canvasW <= 0f || canvasH <= 0f) return@Canvas

        val theme = snapshot.mapTheme

        // 1) 테마별 3단 하늘 그라데이션
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(theme.skyTop, theme.skyMid, theme.skyBottom)
            ),
            size = size
        )

        val scale = canvasH / 660f
        val viewWorldW = canvasW / scale
        val viewWorldH = canvasH / scale

        val maxCamX = (snapshot.worldWidth - viewWorldW * 0.5f).coerceAtLeast(viewWorldW * 0.5f)
        val minCamX = (viewWorldW * 0.5f).coerceAtMost(snapshot.worldWidth * 0.5f)
        val clampedCamX = snapshot.cameraX.coerceIn(minCamX, maxCamX)

        val maxCamY = (snapshot.worldHeight - viewWorldH * 0.5f).coerceAtLeast(viewWorldH * 0.5f)
        val minCamY = viewWorldH * 0.42f
        val clampedCamY = snapshot.cameraY.coerceIn(minCamY, maxCamY)

        val camLeft = clampedCamX - viewWorldW * 0.5f + snapshot.shakeOffsetX
        val camTop = clampedCamY - viewWorldH * 0.5f + snapshot.shakeOffsetY

        // 2) 3겹 이상 패럴랙스 배경 (태양/행성/성탑/피라미드/화산/언덕)
        drawThemedParallax(theme, camLeft, snapshot.animTime, canvasW, canvasH)

        // 3) 월드 좌표계 변환 적용
        withTransform({
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            translate(left = -camLeft, top = -camTop)
        }) {
            @Suppress("UNUSED_VARIABLE")
            val rev = snapshot.terrainRevision
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawBitmap(terrain.bitmap, srcRect, dstRect, null)
            }

            // 테마별 장식 지형지물 (나무, 선인장, 수정, 야자수, 우주 비콘, 깃발)
            snapshot.sceneryProps.forEach { scenery ->
                if (scenery.isAlive) {
                    drawSceneryProp(scenery, snapshot.animTime)
                }
            }

            // 상승 기류 간헐천 바람 기둥 (뒤쪽 레이어)
            snapshot.props.forEach { prop ->
                if (prop.isActive && prop.type == PropType.UPDRAFT_VENT) {
                    drawUpdraftVent(prop, snapshot.animTime)
                }
            }

            // 화약통, 보급 상자, 공중 도탄 범퍼
            snapshot.props.forEach { prop ->
                if (prop.isActive && prop.type != PropType.UPDRAFT_VENT) {
                    drawInteractiveProp(prop, snapshot.animTime, propLabelPaint)
                }
            }

            // 활성 플레이어 탱크의 지형 위 이동 가용 범위 표시 (연료 기반 실시간 추적)
            if (snapshot.turnPhase == TurnPhase.PLAYER_AIMING) {
                val activeTank = snapshot.tanks.getOrNull(snapshot.activeTankIndex)
                if (activeTank != null && activeTank.isAlive && !activeTank.isAi && !activeTank.isFalling) {
                    drawTankMovementRange(
                        tank = activeTank,
                        terrain = terrain,
                        animTime = snapshot.animTime
                    )
                }
            }

            // 탱크 렌더링 (표정·실드·반동·연료 게이지 포함)
            snapshot.tanks.forEachIndexed { idx, tank ->
                if (tank.isAlive) {
                    val isActive = idx == snapshot.activeTankIndex &&
                        (snapshot.turnPhase == TurnPhase.PLAYER_AIMING ||
                            snapshot.turnPhase == TurnPhase.CHARGING ||
                            snapshot.turnPhase == TurnPhase.AI_THINKING)
                    drawCartoonTank(
                        tank = tank,
                        isActive = isActive,
                        isCharging = isActive && snapshot.turnPhase == TurnPhase.CHARGING,
                        chargingPower = snapshot.chargingPower,
                        animTime = snapshot.animTime,
                        namePaint = namePaint,
                        nameStrokePaint = nameStrokePaint,
                        fuelTextPaint = fuelTextPaint,
                        fuelStrokePaint = fuelStrokePaint
                    )
                }
            }

            // 포구 화염 스타버스트
            if (snapshot.muzzleFlashTimer > 0f) {
                val alpha = (snapshot.muzzleFlashTimer / 0.16f).coerceIn(0f, 1f)
                drawStarburst(
                    center = snapshot.muzzleFlashPos,
                    outerRadius = 28f * alpha + 10f,
                    innerRadius = 11f * alpha + 4f,
                    points = 7,
                    color = Color(0xFFFFD166).copy(alpha = alpha)
                )
            }

            // 독가스 구름 장판 (가스포탄)
            snapshot.gasClouds.forEach { cloud ->
                val pulse = 1f + sin(snapshot.animTime * 4f + cloud.id) * 0.06f
                val r = cloud.radius * pulse
                drawCircle(
                    color = Color(0x4470E000),
                    radius = r,
                    center = Offset(cloud.x, cloud.y)
                )
                drawCircle(
                    color = Color(0x5538B000),
                    radius = r * 0.65f,
                    center = Offset(cloud.x - r * 0.25f, cloud.y - 10f)
                )
                drawCircle(
                    color = Color(0x559EF01A),
                    radius = r * 0.55f,
                    center = Offset(cloud.x + r * 0.28f, cloud.y + 6f)
                )
                drawCircle(
                    color = Color(0xAA70E000),
                    radius = r,
                    center = Offset(cloud.x, cloud.y),
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )
                )
            }

            // 포탄 및 화려한 흰 연기 + 무기별 개성 있는 포탄 모양
            snapshot.projectiles.forEach { proj ->
                if (proj.trail.size >= 2) {
                    val outerTrailColor = when (proj.weapon) {
                        WeaponType.WATER -> Color(0xFFCAF0F8)
                        WeaponType.POOP -> Color(0xFFA98467)
                        WeaponType.GAS -> Color(0xFF9EF01A)
                        WeaponType.TRASH -> Color(0xFFDEE2E6)
                        else -> Color.White
                    }
                    for (i in 1 until proj.trail.size) {
                        val frac = i.toFloat() / proj.trail.size.toFloat()
                        drawLine(
                            color = outerTrailColor.copy(alpha = frac * 0.52f),
                            start = proj.trail[i - 1],
                            end = proj.trail[i],
                            strokeWidth = proj.radius * 2.1f * frac,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = proj.weapon.shellColor.copy(alpha = frac * 0.9f),
                            start = proj.trail[i - 1],
                            end = proj.trail[i],
                            strokeWidth = proj.radius * 1.1f * frac,
                            cap = StrokeCap.Round
                        )
                    }
                }
                drawCustomWeaponProjectile(proj)
            }

            // 카툰 폭발 스타버스트 + 충격파 링
            snapshot.explosions.forEach { exp ->
                val p = (exp.age / exp.duration).coerceIn(0f, 1f)
                val r = exp.maxRadius * (0.35f + p * 0.8f)
                val alpha = 1f - p
                drawStarburst(
                    center = Offset(exp.x, exp.y),
                    outerRadius = r,
                    innerRadius = r * 0.52f,
                    points = 10,
                    color = exp.coreColor.copy(alpha = alpha * 0.72f)
                )
                drawStarburst(
                    center = Offset(exp.x, exp.y),
                    outerRadius = r * 0.65f,
                    innerRadius = r * 0.34f,
                    points = 8,
                    color = Color(0xFFFFF3B0).copy(alpha = alpha * 0.9f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.8f),
                    radius = r * 1.05f,
                    center = Offset(exp.x, exp.y),
                    style = Stroke(width = 4.5f * (1f - p) + 1f)
                )
            }

            // 파편/연기 파티클
            snapshot.particles.forEach { part ->
                val alpha = (1f - part.age / part.lifetime).coerceIn(0f, 1f)
                val drawRadius = if (part.isSmoke) {
                    part.radius * (0.7f + 0.8f * (part.age / part.lifetime))
                } else {
                    part.radius * (0.4f + 0.6f * alpha)
                }
                drawCircle(
                    color = part.color.copy(alpha = alpha * 0.9f),
                    radius = drawRadius,
                    center = Offset(part.x, part.y)
                )
            }

            // 테마별 날씨 효과 (눈, 모래바람, 불티, 별빛, 바다 안개)
            val weatherColor = when (theme) {
                MapTheme.SNOW -> Color.White
                MapTheme.DESERT -> Color(0xFFFFE599)
                MapTheme.VOLCANO -> Color(0xFFFF6D00)
                MapTheme.SPACE -> Color(0xFFE0FBFC)
                MapTheme.ISLAND -> Color(0xFFE0F7FA)
                MapTheme.GRASSLAND -> Color(0xFFFFFFE0)
            }
            snapshot.weatherParticles.forEach { wp ->
                drawCircle(
                    color = weatherColor.copy(alpha = wp.alpha),
                    radius = wp.size,
                    center = Offset(wp.x, wp.y)
                )
            }

            // 데미지 팝업 텍스트
            snapshot.popups.forEach { pop ->
                val alpha = ((1f - pop.age / pop.duration) * 1.3f).coerceIn(0f, 1f)
                val alphaInt = (alpha * 255).toInt()
                popupStrokePaint.alpha = alphaInt
                popupFillPaint.color = pop.color.toArgb()
                popupFillPaint.alpha = alphaInt
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(pop.text, pop.x, pop.y, popupStrokePaint)
                    canvas.nativeCanvas.drawText(pop.text, pop.x, pop.y, popupFillPaint)
                }
            }
        }
    }
}

private fun DrawScope.drawThemedParallax(
    theme: MapTheme,
    camLeft: Float,
    animTime: Float,
    canvasW: Float,
    canvasH: Float
) {
    // 1겹: 태양 / 달 / 고리 행성
    val celestialX = canvasW * 0.82f - camLeft * 0.03f
    val celestialY = canvasH * 0.19f
    when {
        theme.isNightSpace -> {
            drawCircle(color = Color(0x339BF6FF), radius = canvasH * 0.14f, center = Offset(celestialX, celestialY))
            drawCircle(color = Color(0xFFCDB4DB), radius = canvasH * 0.08f, center = Offset(celestialX, celestialY))
            rotate(degrees = -18f, pivot = Offset(celestialX, celestialY)) {
                drawOval(
                    color = Color(0xAA9BF6FF),
                    topLeft = Offset(celestialX - canvasH * 0.14f, celestialY - canvasH * 0.025f),
                    size = Size(canvasH * 0.28f, canvasH * 0.05f),
                    style = Stroke(width = 5f)
                )
            }
        }
        theme == MapTheme.VOLCANO -> {
            drawCircle(color = Color(0x44FF5400), radius = canvasH * 0.16f, center = Offset(celestialX, celestialY))
            drawCircle(color = Color(0xFFFF9E00), radius = canvasH * 0.09f, center = Offset(celestialX, celestialY))
        }
        else -> {
            drawCircle(color = Color(0x44FFD166), radius = canvasH * 0.14f, center = Offset(celestialX, celestialY))
            drawCircle(color = Color(0xFFFFE66D), radius = canvasH * 0.082f, center = Offset(celestialX, celestialY))
        }
    }

    // 2겹: 둥실둥실 구름 (우주 맵 제외)
    if (!theme.isNightSpace) {
        val cloudColor = if (theme == MapTheme.VOLCANO) Color(0xFF4A2525).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.88f)
        val cloudOffsets = listOf(140f, 540f, 960f, 1380f)
        cloudOffsets.forEachIndexed { idx, baseX ->
            val drift = animTime * (12f + (idx % 2) * 5f)
            val cx = ((baseX + drift - camLeft * 0.14f) % (canvasW + 320f) + canvasW + 320f) % (canvasW + 320f) - 160f
            val cy = canvasH * (0.14f + (idx % 2) * 0.1f)
            drawCircle(color = cloudColor, radius = 26f, center = Offset(cx, cy))
            drawCircle(color = cloudColor, radius = 34f, center = Offset(cx + 28f, cy - 9f))
            drawCircle(color = cloudColor, radius = 24f, center = Offset(cx + 58f, cy + 2f))
        }
    }

    // 3겹: 원경 실루엣 (초원 요새 성탑 / 사막 피라미드 / 설산 봉우리 / 화산 / 바다)
    val farPath = Path().apply {
        moveTo(0f, canvasH)
        var sx = -80f
        while (sx <= canvasW + 80f) {
            val wx = sx + camLeft * 0.18f
            val hy = when (theme) {
                MapTheme.VOLCANO -> canvasH * 0.58f - kotlin.math.abs(sin(wx * 0.004f)) * canvasH * 0.16f
                MapTheme.DESERT -> canvasH * 0.64f - kotlin.math.abs(sin(wx * 0.0035f)) * canvasH * 0.12f
                else -> canvasH * 0.62f + sin(wx * 0.004f) * (canvasH * 0.07f)
            }
            lineTo(sx, hy)
            sx += 50f
        }
        lineTo(canvasW, canvasH)
        close()
    }
    drawPath(path = farPath, color = theme.farHillColor.copy(alpha = 0.62f))

    // 초원 요새 맵에서는 레퍼런스 이미지 스타일의 성곽 망루 실루엣 추가
    if (theme.hasCastleSilhouettes) {
        val towerX = canvasW * 0.85f - camLeft * 0.22f
        val towerBaseY = canvasH * 0.72f
        val towerW = 74f
        val towerH = 140f
        drawRect(
            color = Color(0xFF7D8597).copy(alpha = 0.55f),
            topLeft = Offset(towerX, towerBaseY - towerH),
            size = Size(towerW, towerH)
        )
        // 성벽 흉벽(요철)
        for (b in 0..2) {
            drawRect(
                color = Color(0xFF7D8597).copy(alpha = 0.55f),
                topLeft = Offset(towerX + b * 28f, towerBaseY - towerH - 18f),
                size = Size(18f, 18f)
            )
        }
    }

    // 4겹: 중경 언덕 / 파도
    val midPath = Path().apply {
        moveTo(0f, canvasH)
        var sx = -60f
        while (sx <= canvasW + 60f) {
            val wx = sx + camLeft * 0.3f
            val hy = canvasH * 0.71f + sin(wx * 0.006f) * (canvasH * 0.055f) + cos(wx * 0.011f) * (canvasH * 0.025f)
            lineTo(sx, hy)
            sx += 45f
        }
        lineTo(canvasW, canvasH)
        close()
    }
    drawPath(path = midPath, color = theme.midHillColor.copy(alpha = 0.72f))
}

private fun DrawScope.drawStarburst(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    points: Int,
    color: Color
) {
    val path = Path()
    val total = points * 2
    for (i in 0 until total) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val angle = (i.toFloat() / total) * (2f * PI.toFloat())
        val x = center.x + cos(angle) * r
        val y = center.y + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = color)
}

private fun DrawScope.drawTankMovementRange(
    tank: TankState,
    terrain: DestructibleTerrain,
    animTime: Float
) {
    val maxReachPx = ((tank.fuel / BattleEngine.FUEL_BURN_PER_SEC) * BattleEngine.TANK_MOVE_SPEED)
        .coerceAtLeast(0f)
    if (maxReachPx < 6f) return

    val fuelFrac = (tank.fuel / tank.maxFuel.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val rangeColor = when {
        fuelFrac > 0.35f -> SuccessGreen
        fuelFrac > 0.15f -> WarningAmber
        else -> DangerRed
    }
    val stepPx = 8f
    val pulse = 0.72f + 0.28f * sin(animTime * 5.5f)

    listOf(-1f, 1f).forEach { dir ->
        val points = mutableListOf(Offset(tank.x, tank.y + 1f))
        var curX = tank.x
        var curY = tank.y
        var traveled = 0f
        while (traveled + stepPx <= maxReachPx) {
            val nextX = curX + dir * stepPx
            if (nextX < 36f || nextX > terrain.worldWidth - 36f) break
            val groundY = terrain.findGroundY(nextX, curY, maxStepUp = 18f) ?: break
            curX = nextX
            curY = groundY
            traveled += stepPx
            points.add(Offset(curX, curY + 1f))
        }
        if (points.size >= 2) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            // 지형 표면 위 진한 외곽선 + 밝은 이동 가능 점선 리본
            drawPath(
                path = path,
                color = DeepNavyOutline.copy(alpha = 0.75f),
                style = Stroke(width = 8.5f, cap = StrokeCap.Round)
            )
            drawPath(
                path = path,
                color = rangeColor.copy(alpha = 0.92f),
                style = Stroke(
                    width = 4.5f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 7f), -animTime * 22f * dir)
                )
            )

            // 이동 가용 한계 지점 말뚝 & 빛나는 경계 핀
            val endPt = points.last()
            val poleTop = Offset(endPt.x, endPt.y - 22f)
            drawLine(
                color = DeepNavyOutline,
                start = endPt,
                end = poleTop,
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = rangeColor,
                start = endPt,
                end = poleTop,
                strokeWidth = 2.6f,
                cap = StrokeCap.Round
            )
            // 방향 깃발 삼각형
            val flagPath = Path().apply {
                moveTo(poleTop.x, poleTop.y)
                lineTo(poleTop.x - dir * 10f, poleTop.y + 4.5f)
                lineTo(poleTop.x, poleTop.y + 9f)
                close()
            }
            drawPath(path = flagPath, color = rangeColor.copy(alpha = pulse))
            drawPath(path = flagPath, color = DeepNavyOutline, style = Stroke(width = 1.6f))
            drawCircle(
                color = rangeColor.copy(alpha = 0.35f * pulse),
                radius = 9f,
                center = endPt
            )
            drawCircle(
                color = Color.White,
                radius = 3.2f,
                center = endPt
            )
        }
    }
}

private fun DrawScope.drawCartoonTank(
    tank: TankState,
    isActive: Boolean,
    isCharging: Boolean,
    chargingPower: Float,
    animTime: Float,
    namePaint: Paint,
    nameStrokePaint: Paint,
    fuelTextPaint: Paint,
    fuelStrokePaint: Paint
) {
    rotate(degrees = tank.slopeAngleDeg, pivot = Offset(tank.x, tank.y)) {
        val turretCenter = Offset(tank.x, tank.y - 22f)
        val barrelAngleDeg = if (tank.facingRight) -tank.aimAngleDeg else -(180f - tank.aimAngleDeg)
        val barrelRad = Math.toRadians(barrelAngleDeg.toDouble()).toFloat()
        val recoil = tank.recoilOffset

        if (isActive && !tank.isAi) {
            val arcRadius = 56f
            val startAngle = if (tank.facingRight) -90f else -180f
            drawArc(
                color = Color.White.copy(alpha = 0.38f),
                startAngle = startAngle,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(turretCenter.x - arcRadius, turretCenter.y - arcRadius),
                size = Size(arcRadius * 2f, arcRadius * 2f),
                style = Stroke(
                    width = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
            )
            val aimStart = Offset(
                turretCenter.x + cos(barrelRad) * 38f,
                turretCenter.y + sin(barrelRad) * 38f
            )
            val aimEnd = Offset(
                turretCenter.x + cos(barrelRad) * 102f,
                turretCenter.y + sin(barrelRad) * 102f
            )
            drawLine(
                color = PrimaryGold.copy(alpha = 0.9f),
                start = aimStart,
                end = aimEnd,
                strokeWidth = 3.2f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f), 0f)
            )
        }

        if (isCharging) {
            drawArc(
                color = PrimaryGold,
                startAngle = -90f,
                sweepAngle = 360f * chargingPower,
                useCenter = false,
                topLeft = Offset(tank.x - 44f, tank.y - 62f),
                size = Size(88f, 88f),
                style = Stroke(width = 5.5f, cap = StrokeCap.Round)
            )
        }

        // 7종 포탄별 전용 커스텀 탱크 렌더링 (포신 · 터렛 · 장식 · 차체 · 바퀴 · 표정)
        withTransform({
            translate(left = tank.x, top = tank.y)
        }) {
            drawThemedTankBody(
                tankType = tank.tankType,
                aimAngleDeg = tank.aimAngleDeg,
                facingRight = tank.facingRight,
                recoil = recoil,
                expression = tank.expression,
                animTime = animTime,
                hasShield = tank.hasShield,
                poisonTurns = tank.poisonTurns
            )
        }
    }

    // 이름 + HP 바 + 활성 화살표
    val barW = 56f
    val barH = 7.5f
    val barLeft = tank.x - barW * 0.5f
    val barTop = tank.y - 54f

    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(tank.name, tank.x, barTop - 8f, nameStrokePaint)
        canvas.nativeCanvas.drawText(tank.name, tank.x, barTop - 8f, namePaint)
    }

    drawRoundRect(
        color = DeepNavyOutline,
        topLeft = Offset(barLeft - 2f, barTop - 2f),
        size = Size(barW + 4f, barH + 4f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    val hpFrac = (tank.hp.toFloat() / tank.maxHp.toFloat()).coerceIn(0f, 1f)
    val hpColor = when {
        hpFrac > 0.5f -> SuccessGreen
        hpFrac > 0.25f -> PrimaryGold
        else -> DangerRed
    }
    drawRoundRect(
        color = hpColor,
        topLeft = Offset(barLeft, barTop),
        size = Size(barW * hpFrac, barH),
        cornerRadius = CornerRadius(3f, 3f)
    )

    if (isActive) {
        val bob = sin(animTime * 6f) * 3.5f
        val arrowY = barTop - 30f + bob
        val arrowPath = Path().apply {
            moveTo(tank.x, arrowY + 11f)
            lineTo(tank.x - 9f, arrowY)
            lineTo(tank.x + 9f, arrowY)
            close()
        }
        drawPath(path = arrowPath, color = PrimaryGold)
        drawPath(path = arrowPath, color = DeepNavyOutline, style = Stroke(width = 2.4f))

        // 활성 플레이어 탱크 바로 아래 실시간 연료 게이지 바 + 잔량 % 표시
        if (!tank.isAi) {
            val fuelFrac = (tank.fuel / tank.maxFuel.coerceAtLeast(1f)).coerceIn(0f, 1f)
            val fuelPct = (fuelFrac * 100f).roundToInt()
            val fuelBarW = 62f
            val fuelBarH = 7f
            val fuelLeft = tank.x - fuelBarW * 0.5f
            val fuelTop = tank.y + 9f
            val fuelColor = when {
                fuelFrac > 0.35f -> SuccessGreen
                fuelFrac > 0.15f -> WarningAmber
                else -> DangerRed
            }

            drawRoundRect(
                color = DeepNavyOutline,
                topLeft = Offset(fuelLeft - 2f, fuelTop - 2f),
                size = Size(fuelBarW + 4f, fuelBarH + 4f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = Color(0xFF0B1320),
                topLeft = Offset(fuelLeft, fuelTop),
                size = Size(fuelBarW, fuelBarH),
                cornerRadius = CornerRadius(3f, 3f)
            )
            if (fuelFrac > 0f) {
                drawRoundRect(
                    color = fuelColor,
                    topLeft = Offset(fuelLeft, fuelTop),
                    size = Size(fuelBarW * fuelFrac, fuelBarH),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }
            val fuelLabel = if (fuelPct > 0) "연료 $fuelPct%" else "연료 0%"
            fuelTextPaint.color = if (fuelPct > 0) android.graphics.Color.WHITE else DangerRed.toArgb()
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(fuelLabel, tank.x, fuelTop + fuelBarH + 13f, fuelStrokePaint)
                canvas.nativeCanvas.drawText(fuelLabel, tank.x, fuelTop + fuelBarH + 13f, fuelTextPaint)
            }
        }
    }
}

private fun DrawScope.drawCustomWeaponProjectile(proj: ProjectileState) {
    val center = Offset(proj.x, proj.y)
    val r = proj.radius
    when (proj.weapon) {
        WeaponType.WATER -> {
            // 반짝이는 하늘색 물방울 포탄
            drawCircle(color = DeepNavyOutline, radius = r + 3f, center = center)
            drawCircle(color = Color(0xFF00BBF9), radius = r, center = center)
            drawCircle(color = Color(0xFF90E0EF), radius = r * 0.65f, center = Offset(center.x - 1.5f, center.y - 1.5f))
            drawCircle(color = Color.White, radius = r * 0.32f, center = Offset(center.x - r * 0.35f, center.y - r * 0.35f))
        }
        WeaponType.POOP -> {
            // 3단 소용돌이 카툰 똥포탄
            val brownDark = Color(0xFF582F0E)
            val brownMain = Color(0xFF7F4F24)
            val brownLight = Color(0xFFA98467)
            drawOval(
                color = DeepNavyOutline,
                topLeft = Offset(center.x - r * 1.45f, center.y - r * 0.1f),
                size = Size(r * 2.9f, r * 1.3f)
            )
            drawOval(
                color = brownMain,
                topLeft = Offset(center.x - r * 1.25f, center.y + r * 0.05f),
                size = Size(r * 2.5f, r * 0.95f)
            )
            drawOval(
                color = DeepNavyOutline,
                topLeft = Offset(center.x - r * 1.05f, center.y - r * 0.7f),
                size = Size(r * 2.1f, r * 1.0f)
            )
            drawOval(
                color = brownMain,
                topLeft = Offset(center.x - r * 0.88f, center.y - r * 0.55f),
                size = Size(r * 1.76f, r * 0.78f)
            )
            drawCircle(color = DeepNavyOutline, radius = r * 0.58f, center = Offset(center.x, center.y - r * 0.72f))
            drawCircle(color = brownLight, radius = r * 0.42f, center = Offset(center.x, center.y - r * 0.72f))
            // 반짝 하이라이트
            drawCircle(color = brownDark, radius = 1.5f, center = Offset(center.x + 2f, center.y))
        }
        WeaponType.GAS -> {
            // 형광 초록 독가스 캐니스터
            rotate(degrees = proj.spinDeg * 0.5f, pivot = center) {
                drawRoundRect(
                    color = DeepNavyOutline,
                    topLeft = Offset(center.x - r * 0.95f, center.y - r * 1.25f),
                    size = Size(r * 1.9f, r * 2.5f),
                    cornerRadius = CornerRadius(5f, 5f)
                )
                drawRoundRect(
                    color = Color(0xFF70E000),
                    topLeft = Offset(center.x - r * 0.7f, center.y - r * 0.98f),
                    size = Size(r * 1.4f, r * 1.96f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRect(
                    color = DeepNavyOutline,
                    topLeft = Offset(center.x - r * 0.7f, center.y - 2f),
                    size = Size(r * 1.4f, 4f)
                )
            }
        }
        WeaponType.TRASH -> {
            // 회전하는 고철/쓰레기 덩어리
            rotate(degrees = proj.spinDeg, pivot = center) {
                drawStarburst(
                    center = center,
                    outerRadius = r * 1.45f,
                    innerRadius = r * 0.85f,
                    points = 6,
                    color = DeepNavyOutline
                )
                drawStarburst(
                    center = center,
                    outerRadius = r * 1.15f,
                    innerRadius = r * 0.65f,
                    points = 6,
                    color = Color(0xFFADB5BD)
                )
                drawCircle(color = Color(0xFFFFD166), radius = r * 0.38f, center = Offset(center.x - 2f, center.y))
                drawCircle(color = Color(0xFF6C757D), radius = r * 0.28f, center = Offset(center.x + 2.5f, center.y + 1.5f))
            }
        }
        else -> {
            drawCircle(color = DeepNavyOutline, radius = r + 3f, center = center)
            drawCircle(color = proj.weapon.shellColor, radius = r, center = center)
            drawCircle(
                color = Color.White,
                radius = r * 0.45f,
                center = Offset(center.x - 1.5f, center.y - 1.5f)
            )
        }
    }
}

private fun DrawScope.drawUpdraftVent(prop: BattlePropState, animTime: Float) {
    val left = prop.x - prop.width * 0.5f
    val top = prop.y - prop.height

    // 반투명 하늘색 바람 기둥
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x2890E0EF),
                Color(0x44CAF0F8)
            ),
            startY = top,
            endY = prop.y
        ),
        topLeft = Offset(left, top),
        size = Size(prop.width, prop.height)
    )

    // 상승하는 바람 줄무늬 애니메이션
    for (i in 0 until 4) {
        val cycle = ((animTime * 0.85f + i * 0.25f) % 1f)
        val ribbonY = prop.y - 18f - cycle * (prop.height - 32f)
        val laneX = prop.x + (-18f + i * 12f) + sin(animTime * 4f + i) * 5f
        val alpha = (1f - cycle).coerceIn(0f, 1f) * 0.78f
        drawLine(
            color = Color.White.copy(alpha = alpha),
            start = Offset(laneX, ribbonY + 26f),
            end = Offset(laneX, ribbonY),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
    }

    // 지면 간헐천 분출구 노즐
    drawRoundRect(
        color = DeepNavyOutline,
        topLeft = Offset(prop.x - 28f, prop.y - 14f),
        size = Size(56f, 16f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = Color(0xFF48CAE4),
        topLeft = Offset(prop.x - 24f, prop.y - 11f),
        size = Size(48f, 10f),
        cornerRadius = CornerRadius(4f, 4f)
    )
}

private fun DrawScope.drawInteractiveProp(
    prop: BattlePropState,
    animTime: Float,
    propLabelPaint: Paint
) {
    when (prop.type) {
        PropType.TNT_BARREL -> {
            val w = prop.width
            val h = prop.height
            val left = prop.x - w * 0.5f
            val top = prop.y - h

            // 붉은 TNT 화약통 외곽 & 본체
            drawRoundRect(
                color = DeepNavyOutline,
                topLeft = Offset(left - 2.5f, top - 2.5f),
                size = Size(w + 5f, h + 5f),
                cornerRadius = CornerRadius(9f, 9f)
            )
            drawRoundRect(
                color = Color(0xFFE63946),
                topLeft = Offset(left, top),
                size = Size(w, h),
                cornerRadius = CornerRadius(7f, 7f)
            )
            // 노란 중앙 경고 밴드 + 상하 철제 띠
            drawRect(
                color = Color(0xFF495057),
                topLeft = Offset(left, top + 5f),
                size = Size(w, 5f)
            )
            drawRect(
                color = Color(0xFF495057),
                topLeft = Offset(left, prop.y - 10f),
                size = Size(w, 5f)
            )
            drawRect(
                color = PrimaryGold,
                topLeft = Offset(left + 2f, top + h * 0.34f),
                size = Size(w - 4f, h * 0.32f)
            )
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText("TNT", prop.x, top + h * 0.58f, propLabelPaint)
            }
        }

        PropType.SUPPLY_CRATE -> {
            val w = prop.width
            val h = prop.height
            val left = prop.x - w * 0.5f
            val top = prop.y - h

            // 금색 나무 보급 상자 + 대각선 보강대 + 회복/연료 십자 마크
            drawRoundRect(
                color = DeepNavyOutline,
                topLeft = Offset(left - 2.5f, top - 2.5f),
                size = Size(w + 5f, h + 5f),
                cornerRadius = CornerRadius(7f, 7f)
            )
            drawRoundRect(
                color = Color(0xFFF4A261),
                topLeft = Offset(left, top),
                size = Size(w, h),
                cornerRadius = CornerRadius(5f, 5f)
            )
            drawRoundRect(
                color = Color(0xFFFFD166),
                topLeft = Offset(left + 4f, top + 4f),
                size = Size(w - 8f, h - 8f),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawLine(
                color = Color(0xFFBC6C25),
                start = Offset(left + 4f, top + 4f),
                end = Offset(left + w - 4f, top + h - 4f),
                strokeWidth = 3f
            )
            // 중앙 초록 십자 배지
            val cx = prop.x
            val cy = top + h * 0.5f
            drawCircle(color = DeepNavyOutline, radius = 10.5f, center = Offset(cx, cy))
            drawCircle(color = SuccessGreen, radius = 8.5f, center = Offset(cx, cy))
            drawLine(
                color = Color.White,
                start = Offset(cx - 4.5f, cy),
                end = Offset(cx + 4.5f, cy),
                strokeWidth = 3.2f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(cx, cy - 4.5f),
                end = Offset(cx, cy + 4.5f),
                strokeWidth = 3.2f,
                cap = StrokeCap.Round
            )
        }

        PropType.RICOCHET_BUMPER -> {
            val pulseScale = if (prop.bounceAnimTimer > 0f) {
                1f + sin((prop.bounceAnimTimer / 0.35f) * PI.toFloat()) * 0.34f
            } else {
                1f + sin(animTime * 3.5f + prop.id) * 0.06f
            }
            val r = prop.radius * pulseScale
            val center = Offset(prop.x, prop.y)

            // 외곽 에너지 글로우 + 핑볼 범퍼 링
            drawCircle(color = Color(0x444CC9F0), radius = r * 1.32f, center = center)
            drawCircle(color = DeepNavyOutline, radius = r + 3f, center = center)
            drawCircle(color = Color(0xFF4CC9F0), radius = r, center = center)
            drawCircle(color = Color(0xFFFF4D6D), radius = r * 0.72f, center = center)
            drawStarburst(
                center = center,
                outerRadius = r * 0.52f,
                innerRadius = r * 0.25f,
                points = 5,
                color = PrimaryGold
            )
        }

        PropType.UPDRAFT_VENT -> Unit
    }
}

private fun DrawScope.drawSceneryProp(scenery: SceneryProp, animTime: Float) {
    val s = scenery.scale
    val x = scenery.x
    val y = scenery.y
    val sway = sin(animTime * 1.8f + scenery.id) * 2.2f * s

    when (scenery.kind) {
        SceneryKind.OAK_TREE -> {
            // 둥근 잎의 카툰 참나무
            drawLine(
                color = DeepNavyOutline,
                start = Offset(x, y),
                end = Offset(x + sway * 0.3f, y - 34f * s),
                strokeWidth = 11f * s,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF8D5524),
                start = Offset(x, y),
                end = Offset(x + sway * 0.3f, y - 34f * s),
                strokeWidth = 6.5f * s,
                cap = StrokeCap.Round
            )
            val crown = Offset(x + sway, y - 44f * s)
            drawCircle(color = DeepNavyOutline, radius = 21f * s, center = crown)
            drawCircle(color = DeepNavyOutline, radius = 15f * s, center = Offset(crown.x - 13f * s, crown.y + 6f * s))
            drawCircle(color = DeepNavyOutline, radius = 15f * s, center = Offset(crown.x + 13f * s, crown.y + 6f * s))
            drawCircle(color = Color(0xFF40916C), radius = 13f * s, center = Offset(crown.x - 13f * s, crown.y + 6f * s))
            drawCircle(color = Color(0xFF40916C), radius = 13f * s, center = Offset(crown.x + 13f * s, crown.y + 6f * s))
            drawCircle(color = Color(0xFF52B788), radius = 18.5f * s, center = crown)
        }

        SceneryKind.PINE_TREE -> {
            // 설원 맵 눈 덮인 침엽수
            drawLine(
                color = DeepNavyOutline,
                start = Offset(x, y),
                end = Offset(x, y - 22f * s),
                strokeWidth = 9f * s,
                cap = StrokeCap.Round
            )
            listOf(0f to 28f, 16f to 22f, 30f to 16f).forEachIndexed { tier, (dy, halfW) ->
                val tipY = y - (36f + dy) * s
                val baseY = y - (14f + dy) * s
                val tri = Path().apply {
                    moveTo(x + sway * (0.4f + tier * 0.25f), tipY)
                    lineTo(x - halfW * s, baseY)
                    lineTo(x + halfW * s, baseY)
                    close()
                }
                drawPath(tri, color = if (tier == 2) Color(0xFFEAF6FF) else Color(0xFF2D6A4F))
                drawPath(tri, color = DeepNavyOutline, style = Stroke(width = 2.4f))
            }
        }

        SceneryKind.CACTUS -> {
            // 사막 선인장
            val topY = y - 46f * s
            drawLine(
                color = DeepNavyOutline,
                start = Offset(x, y),
                end = Offset(x, topY),
                strokeWidth = 14f * s,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF52B788),
                start = Offset(x, y),
                end = Offset(x, topY),
                strokeWidth = 9.5f * s,
                cap = StrokeCap.Round
            )
            // 좌/우 팔
            drawLine(
                color = DeepNavyOutline,
                start = Offset(x - 2f, y - 22f * s),
                end = Offset(x - 14f * s, y - 26f * s),
                strokeWidth = 10f * s,
                cap = StrokeCap.Round
            )
            drawLine(
                color = DeepNavyOutline,
                start = Offset(x - 14f * s, y - 26f * s),
                end = Offset(x - 14f * s, y - 38f * s),
                strokeWidth = 10f * s,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF52B788),
                start = Offset(x - 2f, y - 22f * s),
                end = Offset(x - 14f * s, y - 26f * s),
                strokeWidth = 6f * s,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF52B788),
                start = Offset(x - 14f * s, y - 26f * s),
                end = Offset(x - 14f * s, y - 38f * s),
                strokeWidth = 6f * s,
                cap = StrokeCap.Round
            )
        }

        SceneryKind.PALM_TREE -> {
            // 바다 섬 야자수
            val trunkTop = Offset(x + 9f * s + sway, y - 52f * s)
            drawLine(
                color = DeepNavyOutline,
                start = Offset(x, y),
                end = trunkTop,
                strokeWidth = 10.5f * s,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFB07D4F),
                start = Offset(x, y),
                end = trunkTop,
                strokeWidth = 6.5f * s,
                cap = StrokeCap.Round
            )
            listOf(-155f, -35f, 15f, 145f).forEach { deg ->
                val rad = Math.toRadians(deg.toDouble()).toFloat()
                val leafEnd = Offset(trunkTop.x + cos(rad) * 24f * s, trunkTop.y + sin(rad) * 18f * s + 6f)
                drawLine(
                    color = DeepNavyOutline,
                    start = trunkTop,
                    end = leafEnd,
                    strokeWidth = 9f * s,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF40916C),
                    start = trunkTop,
                    end = leafEnd,
                    strokeWidth = 5.5f * s,
                    cap = StrokeCap.Round
                )
            }
        }

        SceneryKind.CRYSTAL -> {
            // 화산/우주 빛나는 결정체
            val crystalPath = Path().apply {
                moveTo(x, y - 42f * s)
                lineTo(x - 12f * s, y - 16f * s)
                lineTo(x - 8f * s, y)
                lineTo(x + 8f * s, y)
                lineTo(x + 12f * s, y - 16f * s)
                close()
            }
            drawPath(crystalPath, color = Color(0xFFFF6D00))
            drawPath(crystalPath, color = DeepNavyOutline, style = Stroke(width = 2.6f))
            drawCircle(color = Color(0xFFFFE066), radius = 3.5f * s, center = Offset(x - 2f, y - 24f * s))
        }

        SceneryKind.SPACE_BEACON -> {
            // 우주 기지 레이더 비콘
            val top = Offset(x, y - 44f * s)
            drawLine(color = DeepNavyOutline, start = Offset(x, y), end = top, strokeWidth = 7.5f * s)
            drawLine(color = Color(0xFFADB5BD), start = Offset(x, y), end = top, strokeWidth = 4f * s)
            val blink = 0.55f + 0.45f * sin(animTime * 5f + scenery.id)
            drawCircle(color = Color(0xFF9BF6FF).copy(alpha = blink * 0.5f), radius = 11f * s, center = top)
            drawCircle(color = Color(0xFF00F5D4), radius = 5.5f * s, center = top)
        }

        SceneryKind.BATTLE_FLAG -> {
            // 바람 방향/세기를 반영해 펄럭이는 요새 깃발
            val poleTop = Offset(x, y - 50f * s)
            drawLine(color = DeepNavyOutline, start = Offset(x, y), end = poleTop, strokeWidth = 6.5f * s)
            drawLine(color = Color(0xFFE9ECEF), start = Offset(x, y), end = poleTop, strokeWidth = 3.2f * s)
            val wave = sin(animTime * 6f + scenery.id) * 4f
            val flagPath = Path().apply {
                moveTo(poleTop.x, poleTop.y + 2f)
                lineTo(poleTop.x + 26f * s, poleTop.y + 8f * s + wave)
                lineTo(poleTop.x, poleTop.y + 20f * s)
                close()
            }
            drawPath(flagPath, color = DangerRed)
            drawPath(flagPath, color = DeepNavyOutline, style = Stroke(width = 2.2f))
        }
    }
}

