package com.example.ui.online

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firebase.RoomInvite
import com.example.game.model.AppLanguage
import com.example.ui.theme.DeepNavyOutline
import com.example.ui.theme.HudSurfaceDark
import com.example.ui.theme.PlayerBlue
import com.example.ui.theme.PrimaryGold
import com.example.ui.theme.PrimaryGoldDark
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted

/**
 * 게임 실행 중(전체화면 가로 모드 포함) 친구 초대 푸시 알림이 도착했을 때
 * 화면 상단에서 부드럽게 내려오는 실시간 헤드업 초대 배너.
 */
@Composable
fun InvitePushBanner(
    invite: RoomInvite?,
    language: AppLanguage,
    isBusy: Boolean,
    onAccept: (RoomInvite) -> Unit,
    onDecline: (RoomInvite) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isKo = language == AppLanguage.KO
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = invite != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            if (invite != null) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xF5142847), Color(0xF51C365B))
                            )
                        )
                        .border(2.5.dp, PrimaryGold, RoundedCornerShape(18.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("invite_push_banner"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryGold.copy(alpha = 0.22f))
                            .border(1.5.dp, PrimaryGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = if (isKo) "대전 초대 알림" else "Match Invite Alert",
                            tint = PrimaryGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (isKo) {
                                "${invite.fromName}님의 1:1 대전 초대!"
                            } else {
                                "Match Invite from ${invite.fromName}!"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${invite.roomTitle} · ${invite.parsedMapTheme().label(language)} · #${invite.roomCode}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { onAccept(invite) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("banner_accept_invite_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = DeepNavyOutline,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isKo) "수락" else "Accept",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavyOutline
                        )
                    }

                    Button(
                        onClick = { onDecline(invite) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF394A63)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("banner_decline_invite_button")
                    ) {
                        Text(
                            text = if (isKo) "거절" else "Decline",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("banner_dismiss_invite_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = if (isKo) "배너 닫기" else "Dismiss",
                            tint = TextMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * 최초 로그인 시 사용자가 친구 초대 푸시 알림 수신 여부를 직접 선택할 수 있는 안내 다이얼로그.
 */
@Composable
fun PushOptInPromptDialog(
    language: AppLanguage,
    onAllowPush: () -> Unit,
    onDeclinePush: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isKo = language == AppLanguage.KO
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xBB08101C))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(HudSurfaceDark)
                .border(3.dp, PrimaryGold, RoundedCornerShape(24.dp))
                .padding(22.dp)
                .testTag("push_opt_in_dialog"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(PrimaryGold.copy(alpha = 0.18f))
                    .border(2.dp, PrimaryGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = if (isKo) "초대 푸시 알림" else "Invite Push Alerts",
                    tint = PrimaryGold,
                    modifier = Modifier.size(30.dp)
                )
            }

            Text(
                text = if (isKo) "친구 초대 푸시 알림을 받으시겠어요?" else "Enable Friend Invite Alerts?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryGold,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isKo) {
                    "친구가 1:1 온라인 대전에 초대하면 실시간 푸시 알림으로 바로 알려드려요.\n원하지 않으시면 꺼둘 수 있으며, 설정이나 초대함에서 언제든 변경할 수 있습니다."
                } else {
                    "Get instant push notifications when friends invite you to a 1v1 online battle.\nYou can turn this on or off anytime in Settings or the Invites tab."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE0ECFF),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDeclinePush,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("push_opt_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsOff,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isKo) "알림 끄기" else "Not Now",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                }

                Button(
                    onClick = onAllowPush,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGoldDark),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("push_opt_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isKo) "푸시 알림 받기" else "Allow Alerts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
