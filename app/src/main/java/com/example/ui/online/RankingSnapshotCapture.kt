package com.example.ui.online

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.firebase.PlayerProfile
import com.example.game.model.AppLanguage
import com.example.game.model.TankType
import com.example.ui.tank.drawThemedTankOnNativeCanvas
import com.example.ui.theme.DeepNavyOutline
import com.example.ui.theme.HudSurfaceDark
import com.example.ui.theme.PlayerBlue
import com.example.ui.theme.PrimaryGold
import com.example.ui.theme.SuccessGreen
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 랭킹 순위(특히 영광의 TOP 1 달성 순간!)를 기념하는 1280x720 고해상도 포스터 PNG Bitmap을 렌더링합니다.
 */
fun renderRankingSnapshotBitmap(
    context: Context,
    language: AppLanguage,
    myProfile: PlayerProfile?,
    myRank: Int?,
    topPlayers: List<PlayerProfile>,
    capturedAtMillis: Long = System.currentTimeMillis()
): Bitmap {
    val width = 1280
    val height = 720
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val isKo = language == AppLanguage.KO
    val isTopOne = myRank == 1

    val displayTypeface = runCatching { ResourcesCompat.getFont(context, R.font.jua) }
        .getOrNull() ?: Typeface.DEFAULT_BOLD
    val numberTypeface = runCatching { ResourcesCompat.getFont(context, R.font.fredoka) }
        .getOrNull() ?: Typeface.DEFAULT_BOLD

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 1. 배경 그라디언트 (TOP 1이면 찬란한 로열 골드-네이비, 일반 순위면 깊은 아케이드 네이비)
    paint.shader = LinearGradient(
        0f, 0f, width.toFloat(), height.toFloat(),
        if (isTopOne) {
            intArrayOf(0xFF1E1338.toInt(), 0xFF15294B.toInt(), 0xFF3E2908.toInt())
        } else {
            intArrayOf(0xFF0A1629.toInt(), 0xFF142A4A.toInt(), 0xFF0D213B.toInt())
        },
        floatArrayOf(0f, 0.55f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.shader = null

    // 2. 선버스트 광선 + 상단 라디얼 후광
    val glowX = if (isTopOne) 360f else 640f
    val glowY = 170f
    paint.shader = RadialGradient(
        glowX, glowY, 540f,
        if (isTopOne) 0x55FFD166 else 0x334CC9F0,
        0x00000000,
        Shader.TileMode.CLAMP
    )
    canvas.drawCircle(glowX, glowY, 540f, paint)
    paint.shader = null

    val rayCount = if (isTopOne) 18 else 12
    paint.color = if (isTopOne) 0x1AFFD166 else 0x0E90E0EF
    for (i in 0 until rayCount) {
        val angle = (i * (360.0 / rayCount)) * PI / 180.0
        val half = 6.5 * PI / 180.0
        val rayPath = Path().apply {
            moveTo(glowX, glowY)
            lineTo((glowX + cos(angle - half) * 1400).toFloat(), (glowY + sin(angle - half) * 1400).toFloat())
            lineTo((glowX + cos(angle + half) * 1400).toFloat(), (glowY + sin(angle + half) * 1400).toFloat())
            close()
        }
        canvas.drawPath(rayPath, paint)
    }

    // 3. 축하 콘페티 & 반짝이 별 (TOP 1일 때 더욱 화려하게!)
    val rnd = Random(if (isTopOne) 202601 else 202602)
    val confettiColors = intArrayOf(
        0xFFFFD166.toInt(),
        0xFFFF7A1A.toInt(),
        0xFF3BF58A.toInt(),
        0xFF4CC9F0.toInt(),
        0xFFFF5C8A.toInt(),
        0xFFFFFFFF.toInt()
    )
    val confettiCount = if (isTopOne) 75 else 30
    for (i in 0 until confettiCount) {
        val cx = rnd.nextFloat() * width
        val cy = rnd.nextFloat() * height
        val cw = 7f + rnd.nextFloat() * 11f
        val ch = 4f + rnd.nextFloat() * 6f
        val rot = rnd.nextFloat() * 180f
        paint.color = confettiColors[i % confettiColors.size]
        paint.alpha = if (isTopOne) 165 else 95
        canvas.save()
        canvas.rotate(rot, cx, cy)
        canvas.drawRoundRect(RectF(cx - cw / 2f, cy - ch / 2f, cx + cw / 2f, cy + ch / 2f), 2f, 2f, paint)
        canvas.restore()
    }
    paint.alpha = 255

    // 4. 외곽 골든 프레임
    val outerRect = RectF(22f, 22f, width - 22f, height - 22f)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = if (isTopOne) 6f else 4f
    paint.color = if (isTopOne) 0xFFFFD166.toInt() else 0xFF3B6699.toInt()
    canvas.drawRoundRect(outerRect, 28f, 28f, paint)

    val innerFrame = RectF(32f, 32f, width - 32f, height - 32f)
    paint.strokeWidth = 2f
    paint.color = if (isTopOne) 0x66FFF3B0 else 0x448ECAE6
    canvas.drawRoundRect(innerFrame, 22f, 22f, paint)
    paint.style = Paint.Style.FILL

    // 5. 상단 헤더 배너 (앱 아이콘 + 타이틀 + 기념 배지)
    val appIconBitmap = runCatching {
        BitmapFactory.decodeResource(context.resources, R.drawable.cannon_app_icon)
    }.getOrNull()

    val iconCenterX = 88f
    val iconCenterY = 82f
    val iconRadius = 32f
    paint.color = 0xFFFFD166.toInt()
    canvas.drawCircle(iconCenterX, iconCenterY, iconRadius + 4f, paint)
    if (appIconBitmap != null) {
        canvas.save()
        val clipPath = Path().apply { addCircle(iconCenterX, iconCenterY, iconRadius, Path.Direction.CW) }
        canvas.clipPath(clipPath)
        canvas.drawBitmap(
            appIconBitmap,
            null,
            RectF(
                iconCenterX - iconRadius,
                iconCenterY - iconRadius,
                iconCenterX + iconRadius,
                iconCenterY + iconRadius
            ),
            paint
        )
        canvas.restore()
        appIconBitmap.recycle()
    }

    // 게임 타이틀 텍스트
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = displayTypeface
        textSize = 42f
        color = 0xFFFFD166.toInt()
    }
    canvas.drawText(if (isKo) "캐논 대전 V2" else "CANNON BATTLE V2", 136f, 84f, titlePaint)

    val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = numberTypeface
        textSize = 18f
        color = 0xFF90E0EF.toInt()
    }
    canvas.drawText(
        if (isKo) "GLOBAL LEADERBOARD · RANKING CERTIFICATE" else "OFFICIAL GLOBAL LEADERBOARD SNAPSHOT",
        138f,
        110f,
        subTitlePaint
    )

    // 우상단 기념 리본 배지
    val bannerText = when {
        isTopOne && isKo -> "★ 영광의 랭킹 TOP 1 달성! ★"
        isTopOne -> "★ RANK #1 CHAMPION! ★"
        isKo -> "★ 명예의 전당 랭킹 기록 ★"
        else -> "★ HALL OF FAME SNAPSHOT ★"
    }
    val bannerRect = RectF(740f, 50f, 1226f, 114f)
    paint.shader = LinearGradient(
        bannerRect.left, bannerRect.top, bannerRect.right, bannerRect.bottom,
        if (isTopOne) 0xFFFFE066.toInt() else 0xFF28558A.toInt(),
        if (isTopOne) 0xFFFF8C00.toInt() else 0xFF1A3A63.toInt(),
        Shader.TileMode.CLAMP
    )
    canvas.drawRoundRect(bannerRect, 32f, 32f, paint)
    paint.shader = null
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 3f
    paint.color = if (isTopOne) 0xFFFFFFFF.toInt() else 0xFFFFD166.toInt()
    canvas.drawRoundRect(bannerRect, 32f, 32f, paint)
    paint.style = Paint.Style.FILL

    val bannerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = displayTypeface
        textSize = 28f
        textAlign = Paint.Align.CENTER
        color = if (isTopOne) 0xFF132238.toInt() else 0xFFFFD166.toInt()
    }
    canvas.drawText(bannerText, bannerRect.centerX(), bannerRect.centerY() + 10f, bannerTextPaint)

    // 6. 좌측 스포트라이트 카드 (내 순위 / TOP 1 챔피언 카드)
    val leftCard = RectF(54f, 140f, 580f, 646f)
    paint.color = if (isTopOne) 0xE6172947.toInt() else 0xE6102036.toInt()
    canvas.drawRoundRect(leftCard, 24f, 24f, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = if (isTopOne) 5f else 3f
    paint.color = if (isTopOne) 0xFFFFD166.toInt() else 0xFF4CC9F0.toInt()
    canvas.drawRoundRect(leftCard, 24f, 24f, paint)
    paint.style = Paint.Style.FILL

    val spotlightProfile = myProfile ?: topPlayers.firstOrNull()
    val spotlightTank = spotlightProfile?.parsedTankType() ?: TankType.CLASSIC
    val displayRank = myRank ?: if (spotlightProfile != null && topPlayers.firstOrNull()?.userId == spotlightProfile.userId) 1 else null

    // 좌측 상단: 왼쪽엔 순위 엠블럼(+TOP 1 황금 왕관), 오른쪽엔 내 전용 탱크 스포트라이트 쇼케이스!
    val badgeCenterX = leftCard.left + 126f
    val badgeCenterY = 238f
    if (displayRank == 1) {
        drawGoldenCrown(canvas, badgeCenterX, badgeCenterY - 44f, 60f)
    }

    // 원형 순위 엠블럼
    paint.shader = RadialGradient(
        badgeCenterX, badgeCenterY, 50f,
        when (displayRank) {
            1 -> 0xFFFFF275.toInt()
            2 -> 0xFFF0F4F8.toInt()
            3 -> 0xFFE5A46B.toInt()
            else -> 0xFF3A7BD5.toInt()
        },
        when (displayRank) {
            1 -> 0xFFE08E00.toInt()
            2 -> 0xFF9FB3C8.toInt()
            3 -> 0xFF9B5A1F.toInt()
            else -> 0xFF1B3A63.toInt()
        },
        Shader.TileMode.CLAMP
    )
    canvas.drawCircle(badgeCenterX, badgeCenterY, 48f, paint)
    paint.shader = null
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 4f
    paint.color = 0xFFFFFFFF.toInt()
    canvas.drawCircle(badgeCenterX, badgeCenterY, 48f, paint)
    paint.style = Paint.Style.FILL

    val rankNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = numberTypeface
        textSize = if (displayRank != null && displayRank < 10) 40f else 30f
        textAlign = Paint.Align.CENTER
        color = if (displayRank != null && displayRank <= 3) 0xFF102036.toInt() else 0xFFFFFFFF.toInt()
    }
    canvas.drawText(
        displayRank?.let { "#$it" } ?: "--",
        badgeCenterX,
        badgeCenterY + 13f,
        rankNumPaint
    )

    // 내 전용 탱크 쇼케이스 프레임 (스포트라이트 + 탱크 이름 배지)
    val tankStageRect = RectF(leftCard.left + 216f, 162f, leftCard.right - 26f, 304f)
    paint.color = 0xCC0D1B2E.toInt()
    canvas.drawRoundRect(tankStageRect, 20f, 20f, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2.5f
    paint.color = spotlightTank.trimColor.toArgb()
    canvas.drawRoundRect(tankStageRect, 20f, 20f, paint)
    paint.style = Paint.Style.FILL

    drawThemedTankOnNativeCanvas(
        canvas = canvas,
        tankType = spotlightTank,
        cx = tankStageRect.centerX() - 6f,
        baseY = tankStageRect.bottom - 38f,
        scale = 1.62f,
        facingRight = true,
        aimAngleDeg = 30f,
        showSpotlight = true
    )

    val tankBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = displayTypeface
        textSize = 18f
        textAlign = Paint.Align.CENTER
        color = spotlightTank.trimColor.toArgb()
    }
    canvas.drawText(
        "${spotlightTank.label(language)} · ${spotlightTank.signatureWeapon.label(language)}",
        tankStageRect.centerX(),
        tankStageRect.bottom - 10f,
        tankBadgePaint
    )

    // 지휘관 닉네임 + 레벨/티어 배지
    val nickText = spotlightProfile?.nickname ?: if (isKo) "전차병" else "Commander"
    val nickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = displayTypeface
        textSize = 38f
        textAlign = Paint.Align.CENTER
        color = 0xFFFFFFFF.toInt()
    }
    canvas.drawText(ellipsize(nickText, 14), leftCard.centerX(), 354f, nickPaint)

    if (spotlightProfile != null) {
        val tierColor = spotlightProfile.tier.badgeColor.toArgb()
        val pillRect = RectF(leftCard.centerX() - 175f, 374f, leftCard.centerX() + 175f, 416f)
        paint.color = 0xFF1D3557.toInt()
        canvas.drawRoundRect(pillRect, 21f, 21f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = tierColor
        canvas.drawRoundRect(pillRect, 21f, 21f, paint)
        paint.style = Paint.Style.FILL

        val tierPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = displayTypeface
            textSize = 23f
            textAlign = Paint.Align.CENTER
            color = tierColor
        }
        canvas.drawText(
            "LV.${spotlightProfile.level}  ·  ${spotlightProfile.tier.label(language)}  ·  ${spotlightProfile.rankPoints} RP",
            pillRect.centerX(),
            pillRect.centerY() + 8f,
            tierPaint
        )

        // 2x2 전적 스탯 그리드
        val statTop = 440f
        val statW = 230f
        val statH = 82f
        val gap = 16f
        val startX = leftCard.left + 25f

        drawStatBox(
            canvas = canvas,
            rect = RectF(startX, statTop, startX + statW, statTop + statH),
            label = if (isKo) "전체 승/패" else "TOTAL W / L",
            value = if (isKo) "${spotlightProfile.wins}승 ${spotlightProfile.losses}패" else "${spotlightProfile.wins}W ${spotlightProfile.losses}L",
            valueColor = 0xFFFFFFFF.toInt(),
            displayTypeface = displayTypeface,
            numberTypeface = numberTypeface
        )
        drawStatBox(
            canvas = canvas,
            rect = RectF(startX + statW + gap, statTop, startX + statW * 2 + gap, statTop + statH),
            label = if (isKo) "승률" else "WIN RATE",
            value = "${spotlightProfile.winRatePercent}%",
            valueColor = 0xFF4ADE80.toInt(),
            displayTypeface = displayTypeface,
            numberTypeface = numberTypeface
        )
        drawStatBox(
            canvas = canvas,
            rect = RectF(startX, statTop + statH + gap, startX + statW, statTop + statH * 2 + gap),
            label = if (isKo) "온라인 전적" else "ONLINE PvP",
            value = if (isKo) "${spotlightProfile.onlineWins}승 ${spotlightProfile.onlineLosses}패" else "${spotlightProfile.onlineWins}W ${spotlightProfile.onlineLosses}L",
            valueColor = 0xFF90E0EF.toInt(),
            displayTypeface = displayTypeface,
            numberTypeface = numberTypeface
        )
        drawStatBox(
            canvas = canvas,
            rect = RectF(startX + statW + gap, statTop + statH + gap, startX + statW * 2 + gap, statTop + statH * 2 + gap),
            label = if (isKo) "최고 연승" else "BEST STREAK",
            value = if (isKo) "${spotlightProfile.bestWinStreak}연승" else "${spotlightProfile.bestWinStreak} WIN",
            valueColor = 0xFFFFD166.toInt(),
            displayTypeface = displayTypeface,
            numberTypeface = numberTypeface
        )
    }

    // 7. 우측 패널: 글로벌 랭킹 TOP 5 명예의 전당
    val rightCard = RectF(604f, 140f, 1226f, 646f)
    paint.color = 0xE6102036.toInt()
    canvas.drawRoundRect(rightCard, 24f, 24f, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 3f
    paint.color = 0xFF2B4C78.toInt()
    canvas.drawRoundRect(rightCard, 24f, 24f, paint)
    paint.style = Paint.Style.FILL

    val boardHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = displayTypeface
        textSize = 27f
        color = 0xFFFFD166.toInt()
    }
    canvas.drawText(
        if (isKo) "글로벌 랭킹 TOP 5" else "GLOBAL LEADERBOARD TOP 5",
        rightCard.left + 26f,
        rightCard.top + 44f,
        boardHeaderPaint
    )

    val displayedTop = topPlayers.take(5).ifEmpty { listOfNotNull(myProfile) }
    val rowStartTop = rightCard.top + 66f
    val rowHeight = 76f
    val rowGap = 10f

    displayedTop.forEachIndexed { idx, player ->
        val rank = idx + 1
        val isMe = myProfile != null && player.userId == myProfile.userId
        val rowTop = rowStartTop + idx * (rowHeight + rowGap)
        val rowRect = RectF(rightCard.left + 22f, rowTop, rightCard.right - 22f, rowTop + rowHeight)

        paint.color = when {
            rank == 1 -> 0xFF2D2518.toInt()
            isMe -> 0xFF1F3D66.toInt()
            else -> 0xFF162B47.toInt()
        }
        canvas.drawRoundRect(rowRect, 16f, 16f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = if (rank == 1 || isMe) 3f else 1.5f
        paint.color = when {
            rank == 1 -> 0xFFFFD166.toInt()
            isMe -> 0xFF4CC9F0.toInt()
            else -> 0xFF2C4A70.toInt()
        }
        canvas.drawRoundRect(rowRect, 16f, 16f, paint)
        paint.style = Paint.Style.FILL

        // 순위 원형 배지
        val medalCx = rowRect.left + 32f
        val medalCy = rowRect.centerY()
        paint.color = when (rank) {
            1 -> 0xFFFFD166.toInt()
            2 -> 0xFFD9E2EC.toInt()
            3 -> 0xFFCD7F32.toInt()
            else -> 0xFF33527A.toInt()
        }
        canvas.drawCircle(medalCx, medalCy, 20f, paint)
        if (rank == 1) {
            drawGoldenCrown(canvas, medalCx, medalCy - 23f, 18f)
        }

        val medalNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = numberTypeface
            textSize = 21f
            textAlign = Paint.Align.CENTER
            color = if (rank <= 3) 0xFF102036.toInt() else 0xFFFFFFFF.toInt()
        }
        canvas.drawText("$rank", medalCx, medalCy + 7f, medalNumPaint)

        // 명예의 전당 플레이어 전용 탱크 배지
        val playerTank = player.parsedTankType()
        val miniTankBox = RectF(rowRect.left + 62f, rowRect.top + 10f, rowRect.left + 122f, rowRect.bottom - 10f)
        paint.color = 0xFF0E1D33.toInt()
        canvas.drawRoundRect(miniTankBox, 12f, 12f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = playerTank.trimColor.toArgb()
        canvas.drawRoundRect(miniTankBox, 12f, 12f, paint)
        paint.style = Paint.Style.FILL

        drawThemedTankOnNativeCanvas(
            canvas = canvas,
            tankType = playerTank,
            cx = miniTankBox.centerX() - 2f,
            baseY = miniTankBox.bottom - 10f,
            scale = 0.68f,
            facingRight = true,
            aimAngleDeg = 30f,
            showSpotlight = false
        )

        // 닉네임 + 레벨 + (나) 태그
        val rowNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = displayTypeface
            textSize = 23f
            color = if (rank == 1) 0xFFFFF3B0.toInt() else 0xFFFFFFFF.toInt()
        }
        val meTag = if (isMe) (if (isKo) " [나]" else " [ME]") else ""
        canvas.drawText(
            "${ellipsize(player.nickname, 10)} (Lv.${player.level})$meTag",
            rowRect.left + 134f,
            rowRect.top + 32f,
            rowNamePaint
        )

        // 티어 · 탱크명 · 승패 서브텍스트
        val rowSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = displayTypeface
            textSize = 16f
            color = player.tier.badgeColor.toArgb()
        }
        val recordText = if (isKo) {
            "${playerTank.label(language)} · ${player.wins}승 ${player.losses}패 (${player.winRatePercent}%)"
        } else {
            "${playerTank.label(language)} · ${player.wins}W ${player.losses}L (${player.winRatePercent}%)"
        }
        canvas.drawText(recordText, rowRect.left + 134f, rowRect.top + 59f, rowSubPaint)

        // 우측 RP 점수
        val rpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = numberTypeface
            textSize = 23f
            textAlign = Paint.Align.RIGHT
            color = 0xFFFFD166.toInt()
        }
        canvas.drawText("${player.rankPoints} RP", rowRect.right - 16f, rowRect.centerY() + 8f, rpPaint)
    }

    // 8. 하단 푸터 (날짜/시간 + 제작자 구지초도형캠프 워터마크)
    val dateStr = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault()).format(Date(capturedAtMillis))
    val footerLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = numberTypeface
        textSize = 18f
        color = 0xFFB8CCE6.toInt()
    }
    canvas.drawText(
        if (isKo) "기록 일시 · $dateStr" else "Captured · $dateStr",
        56f,
        686f,
        footerLeftPaint
    )

    val footerRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = displayTypeface
        textSize = 22f
        textAlign = Paint.Align.RIGHT
        color = 0xFFFFD166.toInt()
    }
    canvas.drawText("제작 · 구지초도형캠프", width - 56f, 686f, footerRightPaint)

    return bitmap
}

private fun drawGoldenCrown(canvas: Canvas, cx: Float, bottomY: Float, width: Float) {
    val halfW = width * 0.5f
    val crownH = width * 0.62f
    val path = Path().apply {
        moveTo(cx - halfW * 0.78f, bottomY)
        lineTo(cx - halfW, bottomY - crownH * 0.85f)
        lineTo(cx - halfW * 0.42f, bottomY - crownH * 0.42f)
        lineTo(cx, bottomY - crownH)
        lineTo(cx + halfW * 0.42f, bottomY - crownH * 0.42f)
        lineTo(cx + halfW, bottomY - crownH * 0.85f)
        lineTo(cx + halfW * 0.78f, bottomY)
        close()
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            cx, bottomY - crownH, cx, bottomY,
            0xFFFFF59D.toInt(),
            0xFFFFA000.toInt(),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawPath(path, fillPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (width * 0.06f).coerceAtLeast(2f)
        color = 0xFF7A4400.toInt()
        strokeJoin = Paint.Join.ROUND
    }
    canvas.drawPath(path, strokePaint)

    // 왕관 꼭대기 보석 포인트
    val gemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF4D6D.toInt() }
    val gemR = (width * 0.08f).coerceAtLeast(2.2f)
    canvas.drawCircle(cx, bottomY - crownH, gemR, gemPaint)
    canvas.drawCircle(cx - halfW, bottomY - crownH * 0.85f, gemR * 0.8f, gemPaint)
    canvas.drawCircle(cx + halfW, bottomY - crownH * 0.85f, gemR * 0.8f, gemPaint)
}

private fun drawStatBox(
    canvas: Canvas,
    rect: RectF,
    label: String,
    value: String,
    valueColor: Int,
    displayTypeface: Typeface,
    numberTypeface: Typeface
) {
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1A3150.toInt() }
    canvas.drawRoundRect(rect, 14f, 14f, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = 0xFF2D4F7A.toInt()
    }
    canvas.drawRoundRect(rect, 14f, 14f, borderPaint)

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = displayTypeface
        textSize = 17f
        textAlign = Paint.Align.CENTER
        color = 0xFF9BB4D3.toInt()
    }
    canvas.drawText(label, rect.centerX(), rect.top + 30f, labelPaint)

    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = numberTypeface
        textSize = 26f
        textAlign = Paint.Align.CENTER
        color = valueColor
    }
    canvas.drawText(value, rect.centerX(), rect.bottom - 18f, valuePaint)
}

private fun ellipsize(text: String, maxChars: Int): String {
    return if (text.length <= maxChars) text else text.take(maxChars - 1) + "…"
}

/**
 * 권한 요청 없이(Zero-Permission MediaStore API 29+ / 앱 전용 Pictures 폴백) 랭킹 PNG 이미지를 저장합니다.
 */
fun saveRankingBitmapToGallery(
    context: Context,
    bitmap: Bitmap,
    isTopOne: Boolean,
    language: AppLanguage
): Result<String> {
    return runCatching {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val prefix = if (isTopOne) "CannonBattle_TOP1_" else "CannonBattle_Rank_"
        val fileName = "$prefix$stamp.png"
        val isKo = language == AppLanguage.KO

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/CannonBattle")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: error("MediaStore URI 생성에 실패했습니다.")
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        error("PNG 인코딩에 실패했습니다.")
                    }
                } ?: error("출력 스트림을 열 수 없습니다.")
                val readyValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(uri, readyValues, null, null)
            } catch (t: Throwable) {
                resolver.delete(uri, null, null)
                throw t
            }
            if (isKo) "갤러리(사진/CannonBattle)에 저장되었습니다!" else "Saved to Gallery (Pictures/CannonBattle)!"
        } else {
            val dir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.cacheDir,
                "CannonBattle"
            ).apply { mkdirs() }
            val outFile = File(dir, fileName)
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            if (isKo) "이미지가 저장되었습니다: $fileName" else "Saved image: $fileName"
        }
    }
}

/**
 * FileProvider를 통해 캡쳐한 랭킹 이미지를 즉시 공유합니다.
 */
fun shareRankingBitmap(
    context: Context,
    bitmap: Bitmap,
    isTopOne: Boolean,
    language: AppLanguage
): Result<Unit> {
    return runCatching {
        val capturesDir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(capturesDir, "cannon_rank_snapshot.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val isKo = language == AppLanguage.KO
        val shareText = when {
            isTopOne && isKo -> "캐논 대전 V2 랭킹 TOP 1 달성 기념! (제작 · 구지초도형캠프)"
            isTopOne -> "Rank #1 Champion in Cannon Battle V2!"
            isKo -> "캐논 대전 V2 나의 랭킹 순위 기록! (제작 · 구지초도형캠프)"
            else -> "My Cannon Battle V2 Leaderboard Ranking!"
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(
            shareIntent,
            if (isKo) "랭킹 기념 이미지 공유" else "Share Ranking Snapshot"
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

@Composable
fun RankingCapturePreviewDialog(
    bitmap: Bitmap,
    isTopOne: Boolean,
    language: AppLanguage,
    statusMessage: String?,
    onSaveToGallery: () -> Unit,
    onShareImage: () -> Unit,
    onDismiss: () -> Unit
) {
    val isKo = language == AppLanguage.KO

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6050B14))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(14.dp)
            .testTag("ranking_capture_dialog"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(22.dp))
                .background(HudSurfaceDark)
                .border(
                    width = 3.dp,
                    color = if (isTopOne) PrimaryGold else Color(0xFF4CC9F0),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 바
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = PrimaryGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = when {
                            isTopOne && isKo -> "🏆 TOP 1 달성 기념 이미지 미리보기"
                            isTopOne -> "🏆 RANK #1 CHAMPION SNAPSHOT"
                            isKo -> "랭킹 순위 기념 이미지 미리보기"
                            else -> "Ranking Snapshot Preview"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_capture_dialog_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = if (isKo) "닫기" else "Close",
                        tint = Color.White
                    )
                }
            }

            // 16:9 고해상도 미리보기 이미지
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = if (isKo) "랭킹 기념 캡쳐 이미지" else "Captured ranking card image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(2.dp, PrimaryGold.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                )
            }

            // 저장 완료 알림 + 저장 / 공유 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!statusMessage.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF17382A))
                            .border(1.dp, SuccessGreen, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = if (isKo) "고화질 PNG로 저장하거나 친구에게 자랑해 보세요!" else "Save as PNG or share your achievement!",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFB7CBE5),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onSaveToGallery,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("save_ranking_image_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = DeepNavyOutline,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isKo) "갤러리에 저장" else "Save PNG",
                            style = MaterialTheme.typography.labelLarge,
                            color = DeepNavyOutline,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onShareImage,
                        colors = ButtonDefaults.buttonColors(containerColor = PlayerBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("share_ranking_image_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isKo) "이미지 공유" else "Share",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
