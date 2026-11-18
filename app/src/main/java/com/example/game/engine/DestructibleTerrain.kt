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
 * 6종 맵 테마(초원 요새, 황금 사막, 만년 설원, 화산 협곡, 바다 섬, 우주 기지)의
 * 고유한 지층 색상·질감을 생성하고 원형 파괴 및 경사각을 계산한다.
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
            MapTheme.VOLCANO -> rng.nextFloat() * 85f + 55f
            MapTheme.DESERT -> rng.nextFloat() * 40f + 25f
            else -> rng.nextFloat() * 65f + 45f
        }

        val pixels = IntArray(maskWidth * maskHeight)
        val surfaceYs = IntArray(maskWidth)

        for (mx in 0 until maskWidth) {
            val nx = mx.toFloat() / maskWidth.toFloat()
            val wave1 = sin(nx * 5.2f + phase1) * (if (theme == MapTheme.DESERT) 34f else 48f)
            val wave2 = sin(nx * 11.5f + phase2) * 22f
            val wave3 = cos(nx * 23.0f + phase3) * (if (theme == MapTheme.VOLCANO) 14f else 8f)
            val centerBump = sin(nx * Math.PI.toFloat()) * centerRidge
            val baseHeight = maskHeight * 0.62f - centerBump + wave1 + wave2 + wave3
            val sy = baseHeight.roundToInt().coerceIn((maskHeight * 0.28f).toInt(), (maskHeight * 0.84f).toInt())
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

        bitmap.setPixels(pixels, 0, maskWidth, 0, 0, maskWidth, maskHeight)
        revision++
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
