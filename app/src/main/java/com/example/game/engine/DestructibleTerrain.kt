package com.example.game.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.example.game.model.MapTheme
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * 비트맵 마스크 기반 픽셀 단위 파괴 지형 엔진.
 * 기본 언덕/계곡 지형에 더해 중앙 파괴 가능 성벽(요새/아치)과 공중 부유 발판을
 * 테마별 석재·얼음·사암·합금 블록 질감으로 함께 생성한다.
 */
class DestructibleTerrain(
    val worldWidth: Float = WORLD_WIDTH,
    val worldHeight: Float = WORLD_HEIGHT,
    val maskScale: Float = MASK_SCALE
) {
    val maskWidth: Int = (worldWidth / maskScale).toInt()
    val maskHeight: Int = (worldHeight / maskScale).toInt()

    private val solidMask = BooleanArray(maskWidth * maskHeight)
    val bitmap: Bitmap = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ARGB_8888)
    private val bitmapCanvas = Canvas(bitmap)

    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val scorchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4.5f
        color = 0xFF3E2414.toInt()
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }

    var revision: Int = 0
        private set

    fun generate(theme: MapTheme = MapTheme.GRASSLAND, seed: Long = System.currentTimeMillis()) {
        scorchPaint.color = theme.scorchHex
        val rng = Random(seed)
        val phase1 = rng.nextFloat() * 6.28f
        val phase2 = rng.nextFloat() * 6.28f
        val phase3 = rng.nextFloat() * 6.28f
        val centerRidge = when (theme) {
            MapTheme.VOLCANO -> rng.nextFloat() * 80f + 50f
            MapTheme.DESERT -> rng.nextFloat() * 38f + 22f
            else -> rng.nextFloat() * 60f + 40f
        }

        val pixels = IntArray(maskWidth * maskHeight)
        val surfaceYs = IntArray(maskWidth)

        for (mx in 0 until maskWidth) {
            val nx = mx.toFloat() / maskWidth.toFloat()
            val wave1 = sin(nx * 5.2f + phase1) * (if (theme == MapTheme.DESERT) 34f else 48f)
            val wave2 = sin(nx * 11.5f + phase2) * 22f
            val wave3 = cos(nx * 23.0f + phase3) * (if (theme == MapTheme.VOLCANO) 14f else 8f)
            val centerBump = sin(nx * Math.PI.toFloat()) * centerRidge
            val baseHeight = maskHeight * 0.64f - centerBump + wave1 + wave2 + wave3
            val sy = baseHeight.roundToInt().coerceIn((maskHeight * 0.30f).toInt(), (maskHeight * 0.84f).toInt())
            surfaceYs[mx] = sy
        }

        for (mx in 0 until maskWidth) {
            val sy = surfaceYs[mx]
            for (my in 0 until maskHeight) {
                val idx = my * maskWidth + mx
                if (my >= sy) {
                    solidMask[idx] = true
                    val depth = my - sy
                    val color = when {
                        depth < 2 -> theme.grassTopHex
                        depth < 7 -> theme.grassBodyHex
                        depth < 10 -> theme.grassShadowHex
                        else -> {
                            val strata = ((my + (sin(mx * 0.08f) * 6f).toInt()) / 18) % 3
                            val noise = ((mx * 73856093 xor my * 19349663) and 0xFF)
                            when {
                                noise < 14 -> theme.rockHex
                                strata == 0 -> theme.soilA
                                strata == 1 -> theme.soilB
                                else -> theme.soilC
                            }
                        }
                    }
                    pixels[idx] = color
                } else {
                    solidMask[idx] = false
                    pixels[idx] = 0x00000000
                }
            }
        }

        // 파괴 가능한 전장 구조물 1: 중앙 성벽 망루 (아치 통로 포함)
        buildCenterFortressWall(pixels, surfaceYs, theme, rng)

        // 파괴 가능한 전장 구조물 2: 양측 공중 부유 발판/돌다리 2개
        buildFloatingPlatform(
            pixels = pixels,
            centerMx = (maskWidth * 0.35f).toInt() + rng.nextInt(-25, 25),
            topMy = (surfaceYs[(maskWidth * 0.35f).toInt()] - 92).coerceAtLeast(75),
            halfWidth = 34,
            thickness = 14,
            theme = theme
        )
        buildFloatingPlatform(
            pixels = pixels,
            centerMx = (maskWidth * 0.65f).toInt() + rng.nextInt(-25, 25),
            topMy = (surfaceYs[(maskWidth * 0.65f).toInt()] - 92).coerceAtLeast(75),
            halfWidth = 34,
            thickness = 14,
            theme = theme
        )

        bitmap.setPixels(pixels, 0, maskWidth, 0, 0, maskWidth, maskHeight)
        revision++
    }

    private fun buildCenterFortressWall(
        pixels: IntArray,
        surfaceYs: IntArray,
        theme: MapTheme,
        rng: Random
    ) {
        val centerMx = maskWidth / 2 + rng.nextInt(-30, 31)
        val halfW = 19
        val wallHeight = 76
        val baseSurface = (centerMx - halfW..centerMx + halfW).maxOf { surfaceYs[it.coerceIn(0, maskWidth - 1)] }
        val topMy = (baseSurface - wallHeight).coerceAtLeast(65)

        val (blockMain, blockAlt, mortar, topCap) = structurePalette(theme)

        for (mx in (centerMx - halfW)..(centerMx + halfW)) {
            if (mx !in 0 until maskWidth) continue
            val dx = mx - centerMx
            val localGround = surfaceYs[mx]
            // 상단 성가퀴(요철 흉벽) 패턴
            val isBattlementGap = (mx - (centerMx - halfW)) % 10 in 6..8
            val effectiveTop = if (isBattlementGap) topMy + 7 else topMy

            for (my in effectiveTop..localGround) {
                if (my !in 0 until maskHeight) continue
                // 중앙 하단 아치형 구멍 (낮은 직사탄 통과 가능!)
                val archBottom = localGround - 2
                val archHeight = 20
                val inArch = (my >= archBottom - archHeight) &&
                    (dx * dx) * 0.9f + (archBottom - my) * (archBottom - my) * 0.45f < 110f
                if (inArch) continue

                val idx = my * maskWidth + mx
                solidMask[idx] = true

                val row = (my - effectiveTop) / 9
                val brickOffset = if (row % 2 == 0) 0 else 6
                val isMortar = ((my - effectiveTop) % 9 == 0) || ((mx + brickOffset) % 12 == 0)
                pixels[idx] = when {
                    my - effectiveTop < 2 -> topCap
                    isMortar -> mortar
                    ((mx + row) % 5 == 0) -> blockAlt
                    else -> blockMain
                }
            }
        }
    }

    private fun buildFloatingPlatform(
        pixels: IntArray,
        centerMx: Int,
        topMy: Int,
        halfWidth: Int,
        thickness: Int,
        theme: MapTheme
    ) {
        val (blockMain, blockAlt, mortar, topCap) = structurePalette(theme)
        for (mx in (centerMx - halfWidth)..(centerMx + halfWidth)) {
            if (mx !in 0 until maskWidth) continue
            val edgeDist = kotlin.math.abs(mx - centerMx)
            val taper = if (edgeDist > halfWidth - 8) (edgeDist - (halfWidth - 8)) / 2 else 0
            val bottomMy = topMy + thickness - taper
            for (my in topMy..bottomMy) {
                if (my !in 0 until maskHeight) continue
                val idx = my * maskWidth + mx
                solidMask[idx] = true
                val isMortar = ((my - topMy) % 7 == 0) || (mx % 11 == 0)
                pixels[idx] = when {
                    my - topMy < 3 -> theme.grassBodyHex
                    my - topMy == 3 -> topCap
                    isMortar -> mortar
                    (mx % 4 == 0) -> blockAlt
                    else -> blockMain
                }
            }
        }
    }

    private fun structurePalette(theme: MapTheme): Array<Int> {
        return when (theme) {
            MapTheme.GRASSLAND -> arrayOf(0xFF8D99AE.toInt(), 0xFF7B879D.toInt(), 0xFF495057.toInt(), 0xFFB8C0CC.toInt())
            MapTheme.DESERT -> arrayOf(0xFFE09F3E.toInt(), 0xFFCA8A2E.toInt(), 0xFF8C5319.toInt(), 0xFFFFD166.toInt())
            MapTheme.SNOW -> arrayOf(0xFFA2D2FF.toInt(), 0xFF84BCEE.toInt(), 0xFF457B9D.toInt(), 0xFFFFFFFF.toInt())
            MapTheme.VOLCANO -> arrayOf(0xFF4A2C2A.toInt(), 0xFF332020.toInt(), 0xFFDC2F02.toInt(), 0xFFFF7B00.toInt())
            MapTheme.ISLAND -> arrayOf(0xFFFFB5A7.toInt(), 0xFFFCD5CE.toInt(), 0xFFB56576.toInt(), 0xFFFFF1E6.toInt())
            MapTheme.SPACE -> arrayOf(0xFF5C677D.toInt(), 0xFF495057.toInt(), 0xFF212529.toInt(), 0xFF9BF6FF.toInt())
        }
    }

    fun isSolidWorld(worldX: Float, worldY: Float): Boolean {
        val mx = (worldX / maskScale).toInt()
        val my = (worldY / maskScale).toInt()
        if (mx < 0 || mx >= maskWidth || my < 0 || my >= maskHeight) return false
        return solidMask[my * maskWidth + mx]
    }

    fun carveCrater(worldX: Float, worldY: Float, worldRadius: Float) {
        val cx = worldX / maskScale
        val cy = worldY / maskScale
        val r = worldRadius / maskScale
        val rSq = r * r

        val minX = max(0, (cx - r - 2f).toInt())
        val maxX = min(maskWidth - 1, (cx + r + 2f).toInt())
        val minY = max(0, (cy - r - 2f).toInt())
        val maxY = min(maskHeight - 1, (cy + r + 2f).toInt())

        for (my in minY..maxY) {
            val dy = my - cy
            val dySq = dy * dy
            val rowOffset = my * maskWidth
            for (mx in minX..maxX) {
                val dx = mx - cx
                if (dx * dx + dySq <= rSq) {
                    solidMask[rowOffset + mx] = false
                }
            }
        }

        bitmapCanvas.drawCircle(cx, cy, r + 1.2f, scorchPaint)
        bitmapCanvas.drawCircle(cx, cy, r, clearPaint)
        revision++
    }

    fun findGroundY(worldX: Float, currentY: Float, maxStepUp: Float = 20f): Float? {
        val mx = (worldX / maskScale).toInt().coerceIn(0, maskWidth - 1)
        val currentMy = (currentY / maskScale).toInt()
        val minSearchMy = max(0, currentMy - (maxStepUp / maskScale).toInt())

        if (currentMy in 0 until maskHeight && solidMask[currentMy * maskWidth + mx]) {
            var scanY = currentMy
            while (scanY > minSearchMy && solidMask[(scanY - 1) * maskWidth + mx]) {
                scanY--
            }
            if (scanY == 0 || !solidMask[(scanY - 1) * maskWidth + mx]) {
                return scanY * maskScale
            }
            return null
        }

        val startDown = max(0, currentMy)
        for (my in startDown until maskHeight) {
            if (solidMask[my * maskWidth + mx]) {
                return my * maskScale
            }
        }
        return null
    }

    fun findTopSurfaceY(worldX: Float): Float {
        val mx = (worldX / maskScale).toInt().coerceIn(0, maskWidth - 1)
        for (my in 0 until maskHeight) {
            if (solidMask[my * maskWidth + mx]) {
                return my * maskScale
            }
        }
        return worldHeight * 0.65f
    }

    fun computeSlopeAngleDeg(worldX: Float, surfaceY: Float): Float {
        val sampleSpan = 18f
        val leftY = findGroundY((worldX - sampleSpan).coerceIn(0f, worldWidth - 1f), surfaceY, 26f)
            ?: surfaceY
        val rightY = findGroundY((worldX + sampleSpan).coerceIn(0f, worldWidth - 1f), surfaceY, 26f)
            ?: surfaceY
        val angleRad = atan2(rightY - leftY, sampleSpan * 2f)
        val deg = Math.toDegrees(angleRad.toDouble()).toFloat()
        return deg.coerceIn(-40f, 40f)
    }

    companion object {
        const val WORLD_WIDTH = 2200f
        const val WORLD_HEIGHT = 1000f
        const val MASK_SCALE = 2f
    }
}
