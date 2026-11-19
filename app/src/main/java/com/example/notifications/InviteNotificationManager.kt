package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.firebase.RoomInvite
import com.example.game.model.AppLanguage

/**
 * 친구 대전 초대 푸시 알림(Android 시스템 상단 헤드업 알림) 관리자.
 * 사용자가 푸시 알림 수신을 허용한 경우에만 채널 생성 및 알림 발송을 수행합니다.
 */
object InviteNotificationManager {

    const val CHANNEL_ID_INVITES = "cannon_battle_match_invites"
    const val EXTRA_OPEN_INVITES = "extra_open_invites"
    const val EXTRA_INVITE_ID = "extra_invite_id"
    private const val TAG = "InviteNotifications"

    fun ensureChannel(context: Context, language: AppLanguage = AppLanguage.KO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val isKo = language == AppLanguage.KO
        val channel = NotificationChannel(
            CHANNEL_ID_INVITES,
            if (isKo) "친구 대전 초대 알림" else "Friend Match Invites",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = if (isKo) {
                "친구가 1:1 온라인 대전에 초대할 때 푸시 알림을 받습니다."
            } else {
                "Receive push alerts when a friend invites you to a 1v1 online battle."
            }
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 120L, 80L, 160L)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Android 13(API 33) 이상에서 POST_NOTIFICATIONS 런타임 권한 보유 여부를 반환합니다.
     */
    fun hasRuntimeNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * 시스템 설정 및 런타임 권한상 알림 표시가 가능한지 확인합니다.
     */
    fun canPostSystemNotification(context: Context): Boolean {
        return hasRuntimeNotificationPermission(context) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * 받은 친구 초대장을 고우선순위 시스템 푸시 알림으로 표시합니다.
     * 탭하면 앱이 전면으로 열리며 즉시 '초대함' 탭으로 이동합니다.
     */
    fun showInviteNotification(
        context: Context,
        invite: RoomInvite,
        language: AppLanguage
    ): Boolean {
        if (!canPostSystemNotification(context)) return false
        ensureChannel(context, language)

        val isKo = language == AppLanguage.KO
        val mapName = invite.parsedMapTheme().label(language)
        val title = if (isKo) {
            "⚔️ ${invite.fromName}님의 1:1 대전 초대!"
        } else {
            "⚔️ Match Invite from ${invite.fromName}!"
        }
        val shortText = if (isKo) {
            "${invite.roomTitle} · $mapName (#${invite.roomCode})"
        } else {
            "${invite.roomTitle} · $mapName (#${invite.roomCode})"
        }
        val bigText = if (isKo) {
            "${invite.fromName}님이 '${invite.roomTitle}' 대기실($mapName)로 초대했습니다.\n알림을 눌러 바로 참가하세요! (초대 코드 #${invite.roomCode})"
        } else {
            "${invite.fromName} invited you to '${invite.roomTitle}' ($mapName).\nTap to open Invites and join! (Code #${invite.roomCode})"
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_INVITES, true)
            putExtra(EXTRA_INVITE_ID, invite.id)
        }
        val requestCode = invite.id.hashCode()
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_INVITES)
            .setSmallIcon(R.drawable.ic_stat_cannon)
            .setContentTitle(title)
            .setContentText(shortText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(notificationIdFor(invite.id), notification)
            true
        } catch (se: SecurityException) {
            Log.w(TAG, "Notification permission missing when posting invite notification", se)
            false
        }
    }

    fun cancelInviteNotification(context: Context, inviteId: String) {
        if (inviteId.isBlank()) return
        runCatching {
            NotificationManagerCompat.from(context).cancel(notificationIdFor(inviteId))
        }
    }

    fun cancelAllInviteNotifications(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancelAll()
        }
    }

    private fun notificationIdFor(inviteId: String): Int =
        40_000 + (inviteId.hashCode() and 0x7FFF)
}
