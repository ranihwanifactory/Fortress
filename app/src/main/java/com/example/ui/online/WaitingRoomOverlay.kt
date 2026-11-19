package com.example.ui.online

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firebase.MatchRoom
import com.example.firebase.OnlineUiState
import com.example.firebase.PlayerProfile
import com.example.game.model.AppLanguage
import com.example.game.model.MapTheme
import com.example.game.model.TankType
import com.example.ui.tank.TankAvatar
import com.example.ui.theme.DeepNavyOutline
import com.example.ui.theme.EnemyCoral
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.HudSurfaceDark
import com.example.ui.theme.PlayerBlue
import com.example.ui.theme.PrimaryGold
import com.example.ui.theme.PrimaryGoldDark
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted

@Composable
fun WaitingRoomOverlay(
    room: MatchRoom,
    currentUserId: String,
    language: AppLanguage,
    leaderboardState: OnlineUiState<List<PlayerProfile>>,
    isBusy: Boolean,
    statusMessage: String?,
    onChangeMap: (MapTheme) -> Unit,
    onToggleReady: () -> Unit,
    onStartMatch: () -> Unit,
    onInvitePlayer: (PlayerProfile) -> Unit,
    onOpenTankSelector: () -> Unit,
    onLeaveRoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isKo = language == AppLanguage.KO
    val isHost = room.hostUid == currentUserId
    val hasGuest = room.guestUid.isNotBlank()
    var copiedFeedback by remember { mutableStateOf(false) }

    val invitablePlayers = ((leaderboardState as? OnlineUiState.Success)?.data ?: emptyList())
        .filter { it.userId != currentUserId && it.userId != room.guestUid }
        .take(15)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xDD08101C))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(HudSurfaceDark)
                .border(3.dp, PrimaryGold, RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 상단 바: 방 제목 + 초대 코드 공유/복사 + 나가기
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = room.roomTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = PrimaryGold
                    )
                    Text(
                        text = if (isKo) "맵: ${room.parsedMapTheme().label(language)} · 친구에게 아래 초대 코드를 공유하세요!"
                        else "Map: ${room.parsedMapTheme().label(language)} · Share the invite code!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF90E0EF)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // 6자리 초대 코드 배지
                    Row(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A3150))
                            .border(2.dp, PrimaryGold, RoundedCornerShape(12.dp))
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("RoomCode", room.roomCode))
                                copiedFeedback = true
                            }
                            .padding(horizontal = 12.dp)
                            .testTag("copy_room_code_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = room.roomCode,
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = PrimaryGold
                        )
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "복사",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 시스템 공유 시트
                    Button(
                        onClick = {
                            val shareText = if (isKo) {
                                "[캐논 대전] 1:1 온라인 대포 대전에 초대합니다! 대기실 초대 코드: ${room.roomCode}"
                            } else {
                                "[Cannon Battle] Join my 1v1 match! Room Invite Code: ${room.roomCode}"
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, if (isKo) "친구 초대 공유" else "Share Invite"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PlayerBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp).testTag("share_room_code_button")
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isKo) "공유" else "Share", style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedButton(
                        onClick = onLeaveRoom,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp).testTag("leave_room_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isKo) "나가기" else "Leave", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            if (copiedFeedback || !statusMessage.isNullOrBlank()) {
                Text(
                    text = statusMessage ?: if (isKo) "초대 코드 ${room.roomCode}가 클립보드에 복사되었습니다!" else "Copied code ${room.roomCode}!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SuccessGreen
                )
            }

            // 중앙: 1P 방장 vs 2P 도전자 카드 (각자 선택한 탱크 일러스트 대면 표시!)
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerSlotCard(
                    roleLabel = if (isKo) "1P 방장" else "1P HOST",
                    name = room.hostName,
                    level = room.hostLevel,
                    tankType = room.parsedHostTank(),
                    facingRight = true,
                    language = language,
                    accentColor = PlayerBlue,
                    statusBadge = if (isKo) "방장 준비 완료" else "HOST READY",
                    badgeColor = PrimaryGold,
                    onChangeTank = if (isHost) onOpenTankSelector else null,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "VS",
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = PrimaryGold
                )

                if (hasGuest) {
                    PlayerSlotCard(
                        roleLabel = if (isKo) "2P 도전자" else "2P GUEST",
                        name = room.guestName,
                        level = room.guestLevel,
                        tankType = room.parsedGuestTank(),
                        facingRight = false,
                        language = language,
                        accentColor = EnemyCoral,
                        statusBadge = if (room.guestReady) {
                            if (isKo) "준비 완료!" else "READY!"
                        } else {
                            if (isKo) "준비 대기 중…" else "NOT READY"
                        },
                        badgeColor = if (room.guestReady) SuccessGreen else Color(0xFF495057),
                        onChangeTank = if (!isHost) onOpenTankSelector else null,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(126.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF102036))
                            .border(2.dp, Color(0xFF33527A), RoundedCornerShape(18.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isKo) "도전자 입장 대기 중…\n아래에서 유저를 초대하거나 코드를 공유하세요!" else "Waiting for opponent…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextMuted
                        )
                    }
                }
            }

            // 방장일 때: 빠른 유저 초대 줄 + 맵 변경
            if (isHost && !hasGuest && invitablePlayers.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isKo) "빠른 초대:" else "Quick Invite:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF90E0EF)
                    )
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(invitablePlayers, key = { it.userId }) { target ->
                            Row(
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1A3150))
                                    .border(1.5.dp, PlayerBlue, RoundedCornerShape(10.dp))
                                    .clickable(enabled = !isBusy) { onInvitePlayer(target) }
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${target.nickname} (Lv.${target.level})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                                Icon(Icons.Rounded.Send, contentDescription = "초대", tint = PrimaryGold, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // 하단 컨트롤: 맵 선택(방장) + 시작/준비 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isHost) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MapTheme.entries.forEach { map ->
                            val selected = room.parsedMapTheme() == map
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) PrimaryGold else Color(0xFF1F3554))
                                    .clickable { onChangeMap(map) }
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = map.label(language),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) DeepNavyOutline else Color.White
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = onStartMatch,
                        enabled = hasGuest && !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGoldDark),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(50.dp).testTag("start_online_match_button")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isKo) "온라인 대전 시작!" else "Start Battle!",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                } else {
                    Text(
                        text = if (isKo) "방장이 대전을 시작할 때까지 기다려 주세요." else "Waiting for host to start…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF90E0EF),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onToggleReady,
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (room.guestReady) Color(0xFF495057) else SuccessGreen
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(50.dp).testTag("toggle_ready_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = if (room.guestReady) Color.White else DeepNavyOutline
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (room.guestReady) {
                                if (isKo) "준비 해제" else "Unready"
                            } else {
                                if (isKo) "준비 완료!" else "Ready!"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = if (room.guestReady) Color.White else DeepNavyOutline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerSlotCard(
    roleLabel: String,
    name: String,
    level: Long,
    tankType: TankType,
    facingRight: Boolean,
    language: AppLanguage,
    accentColor: Color,
    statusBadge: String,
    badgeColor: Color,
    onChangeTank: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val isKo = language == AppLanguage.KO
    Row(
        modifier = modifier
            .height(126.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF142842))
            .border(3.dp, accentColor, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TankAvatar(
            tankType = tankType,
            size = 76.dp,
            facingRight = facingRight,
            showPedestal = true,
            highlighted = true,
            modifier = if (onChangeTank != null) Modifier.clickable { onChangeTank() } else Modifier
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = roleLabel, style = MaterialTheme.typography.labelMedium, color = accentColor)
                if (onChangeTank != null) {
                    Text(
                        text = if (isKo) "탱크 교체" else "Swap",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF23395B))
                            .clickable { onChangeTank() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(PrimaryGold)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LV.$level",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = DeepNavyOutline
                    )
                }
                Text(text = name, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
            }
            Text(
                text = "${tankType.label(language)} (${tankType.signatureWeapon.label(language)})",
                style = MaterialTheme.typography.labelSmall,
                color = tankType.trimColor,
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = statusBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (badgeColor == Color(0xFF495057)) Color.White else DeepNavyOutline
                )
            }
        }
    }
}
