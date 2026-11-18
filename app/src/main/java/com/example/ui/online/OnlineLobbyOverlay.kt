package com.example.ui.online

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firebase.MatchRoom
import com.example.firebase.OnlineUiState
import com.example.firebase.PlayerProfile
import com.example.firebase.RoomInvite
import com.example.game.model.AppLanguage
import com.example.game.model.MapTheme
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepNavyOutline
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.HudSurfaceDark
import com.example.ui.theme.PlayerBlue
import com.example.ui.theme.PrimaryGold
import com.example.ui.theme.PrimaryGoldDark
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted

enum class LobbyTab {
    ROOMS,
    INVITES,
    RANKING
}

@Composable
fun OnlineLobbyOverlay(
    initialTab: LobbyTab,
    language: AppLanguage,
    currentUserId: String,
    myProfileState: OnlineUiState<PlayerProfile?>,
    leaderboardState: OnlineUiState<List<PlayerProfile>>,
    waitingRoomsState: OnlineUiState<List<MatchRoom>>,
    incomingInvitesState: OnlineUiState<List<RoomInvite>>,
    isBusy: Boolean,
    statusMessage: String?,
    onCreateRoom: (title: String, map: MapTheme) -> Unit,
    onJoinRoom: (roomId: String) -> Unit,
    onJoinByCode: (code: String) -> Unit,
    onAcceptInvite: (RoomInvite) -> Unit,
    onDeclineInvite: (RoomInvite) -> Unit,
    onInvitePlayer: (PlayerProfile) -> Unit,
    onUpdateNickname: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isKo = language == AppLanguage.KO
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val pendingInviteCount = (incomingInvitesState as? OnlineUiState.Success)?.data?.size ?: 0

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xD908101C))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(HudSurfaceDark)
                .border(3.dp, PrimaryGold, RoundedCornerShape(24.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 상단 탭 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LobbyTabButton(
                        selected = selectedTab == LobbyTab.ROOMS,
                        icon = Icons.Rounded.Groups,
                        label = if (isKo) "대전 대기실" else "Rooms",
                        badgeCount = 0,
                        onClick = { selectedTab = LobbyTab.ROOMS },
                        tag = "tab_lobby_rooms"
                    )
                    LobbyTabButton(
                        selected = selectedTab == LobbyTab.INVITES,
                        icon = Icons.Rounded.Mail,
                        label = if (isKo) "초대함" else "Invites",
                        badgeCount = pendingInviteCount,
                        onClick = { selectedTab = LobbyTab.INVITES },
                        tag = "tab_lobby_invites"
                    )
                    LobbyTabButton(
                        selected = selectedTab == LobbyTab.RANKING,
                        icon = Icons.Rounded.EmojiEvents,
                        label = if (isKo) "랭킹 · 레벨" else "Rankings",
                        badgeCount = 0,
                        onClick = { selectedTab = LobbyTab.RANKING },
                        tag = "tab_lobby_ranking"
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = PrimaryGold,
                            strokeWidth = 2.5.dp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF23395B))
                            .testTag("close_lobby_button")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "닫기", tint = Color.White)
                    }
                }
            }

            if (!statusMessage.isNullOrBlank()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryGold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33FFD166))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // 탭 본문
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    LobbyTab.ROOMS -> RoomsTabContent(
                        isKo = isKo,
                        language = language,
                        currentUserId = currentUserId,
                        waitingRoomsState = waitingRoomsState,
                        isBusy = isBusy,
                        onCreateRoom = onCreateRoom,
                        onJoinRoom = onJoinRoom,
                        onJoinByCode = onJoinByCode
                    )
                    LobbyTab.INVITES -> InvitesTabContent(
                        isKo = isKo,
                        language = language,
                        incomingInvitesState = incomingInvitesState,
                        isBusy = isBusy,
                        onAcceptInvite = onAcceptInvite,
                        onDeclineInvite = onDeclineInvite
                    )
                    LobbyTab.RANKING -> RankingTabContent(
                        isKo = isKo,
                        language = language,
                        currentUserId = currentUserId,
                        myProfileState = myProfileState,
                        leaderboardState = leaderboardState,
                        isBusy = isBusy,
                        onInvitePlayer = onInvitePlayer,
                        onUpdateNickname = onUpdateNickname
                    )
                }
            }
        }
    }
}

@Composable
private fun LobbyTabButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    badgeCount: Int,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) PrimaryGold else Color(0xFF1A2D4A))
            .border(2.dp, if (selected) Color.White else DeepNavyOutline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) DeepNavyOutline else Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) DeepNavyOutline else Color.White
            )
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(DangerRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$badgeCount",
                        fontFamily = FredokaFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomsTabContent(
    isKo: Boolean,
    language: AppLanguage,
    currentUserId: String,
    waitingRoomsState: OnlineUiState<List<MatchRoom>>,
    isBusy: Boolean,
    onCreateRoom: (String, MapTheme) -> Unit,
    onJoinRoom: (String) -> Unit,
    onJoinByCode: (String) -> Unit
) {
    var roomTitle by remember { mutableStateOf("") }
    var selectedMap by remember { mutableStateOf(MapTheme.GRASSLAND) }
    var inviteCode by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 좌측: 방 만들기 + 초대 코드 참가
        Column(
            modifier = Modifier
                .weight(0.95f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF102036))
                .border(2.dp, Color(0xFF284369), RoundedCornerShape(18.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isKo) "새 대기실 만들기" else "Create Match Room",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryGold
            )

            OutlinedTextField(
                value = roomTitle,
                onValueChange = { roomTitle = it.take(32) },
                placeholder = { Text(if (isKo) "방 제목 (예: 초원 1:1 한판!)" else "Room title", color = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = PrimaryGold,
                    unfocusedBorderColor = Color(0xFF3E5C82)
                ),
                modifier = Modifier.fillMaxWidth().testTag("room_title_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MapTheme.entries.forEach { map ->
                    val chosen = map == selectedMap
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (chosen) PrimaryGold else Color(0xFF1F3554))
                            .clickable { selectedMap = map }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = map.label(language),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (chosen) DeepNavyOutline else Color.White
                        )
                    }
                }
            }

            Button(
                onClick = { onCreateRoom(roomTitle, selectedMap) },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGoldDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("create_room_button")
            ) {
                Icon(Icons.Rounded.AddCircle, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isKo) "방 만들기 & 초대 코드 받기" else "Create Room", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 초대 코드로 바로 참가
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(6) },
                    placeholder = { Text(if (isKo) "초대 코드 6자리" else "6-char code", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Rounded.VpnKey, contentDescription = null, tint = Color(0xFF4CC9F0)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4CC9F0),
                        unfocusedBorderColor = Color(0xFF3E5C82)
                    ),
                    modifier = Modifier.weight(1f).testTag("invite_code_input")
                )
                Button(
                    onClick = { onJoinByCode(inviteCode) },
                    enabled = !isBusy && inviteCode.length == 6,
                    colors = ButtonDefaults.buttonColors(containerColor = PlayerBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(52.dp).testTag("join_by_code_button")
                ) {
                    Text(if (isKo) "참가" else "Join", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
        }

        // 우측: 실시간 공개 대기실 목록
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF102036))
                .border(2.dp, Color(0xFF284369), RoundedCornerShape(18.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isKo) "실시간 공개 대기실" else "Open Waiting Rooms",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF90E0EF)
            )
            when (waitingRoomsState) {
                is OnlineUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryGold)
                }
                is OnlineUiState.Error -> Text(waitingRoomsState.message, color = DangerRed)
                is OnlineUiState.Success -> {
                    val rooms = waitingRoomsState.data
                    if (rooms.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isKo) "열린 대기실이 없습니다.\n왼쪽에서 방을 만들어 친구를 초대해 보세요!" else "No open rooms yet.\nCreate one on the left!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(rooms, key = { it.id }) { room ->
                                val isFull = room.guestUid.isNotBlank() && room.hostUid != currentUserId && room.guestUid != currentUserId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF1A3150))
                                        .border(1.5.dp, Color(0xFF33527A), RoundedCornerShape(14.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = room.roomTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "방장: ${room.hostName} (Lv.${room.hostLevel}) · ${room.parsedMapTheme().label(language)} · #${room.roomCode}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF90E0EF),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onJoinRoom(room.id) },
                                        enabled = !isBusy && !isFull,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFull) Color(0xFF495057) else SuccessGreen
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(44.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                room.hostUid == currentUserId || room.guestUid == currentUserId -> if (isKo) "재입장" else "Enter"
                                                isFull -> if (isKo) "만석" else "Full"
                                                else -> if (isKo) "입장" else "Join"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            color = DeepNavyOutline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvitesTabContent(
    isKo: Boolean,
    language: AppLanguage,
    incomingInvitesState: OnlineUiState<List<RoomInvite>>,
    isBusy: Boolean,
    onAcceptInvite: (RoomInvite) -> Unit,
    onDeclineInvite: (RoomInvite) -> Unit
) {
    when (incomingInvitesState) {
        is OnlineUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryGold)
        }
        is OnlineUiState.Error -> Text(incomingInvitesState.message, color = DangerRed)
        is OnlineUiState.Success -> {
            val invites = incomingInvitesState.data
            if (invites.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isKo) "받은 대전 초대가 없습니다.\n랭킹 탭에서 다른 플레이어를 초대하거나 초대 코드를 공유해 보세요!" else "No pending invites.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(invites, key = { it.id }) { invite ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF162C49))
                                .border(2.dp, PrimaryGold, RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${invite.fromName}님의 1:1 대전 초대!",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PrimaryGold
                                )
                                Text(
                                    text = "${invite.roomTitle} · ${invite.parsedMapTheme().label(language)} · 코드 #${invite.roomCode}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onAcceptInvite(invite) },
                                    enabled = !isBusy,
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = DeepNavyOutline)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isKo) "수락" else "Accept", color = DeepNavyOutline, style = MaterialTheme.typography.titleMedium)
                                }
                                Button(
                                    onClick = { onDeclineInvite(invite) },
                                    enabled = !isBusy,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D4B63)),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text(if (isKo) "거절" else "Decline", color = Color.White, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingTabContent(
    isKo: Boolean,
    language: AppLanguage,
    currentUserId: String,
    myProfileState: OnlineUiState<PlayerProfile?>,
    leaderboardState: OnlineUiState<List<PlayerProfile>>,
    isBusy: Boolean,
    onInvitePlayer: (PlayerProfile) -> Unit,
    onUpdateNickname: (String) -> Unit
) {
    val myProfile = (myProfileState as? OnlineUiState.Success)?.data
    var editingNick by remember { mutableStateOf(false) }
    var nickDraft by remember(myProfile?.nickname) { mutableStateOf(myProfile?.nickname ?: "") }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 좌측: 내 지휘관 프로필 & 레벨/경험치 카드
        Column(
            modifier = Modifier
                .weight(0.85f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF102036))
                .border(2.dp, PrimaryGold, RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isKo) "내 지휘관 정보" else "My Commander",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryGold
            )

            if (myProfile == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryGold)
                }
            } else {
                if (editingNick) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = nickDraft,
                            onValueChange = { nickDraft = it.take(20) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PrimaryGold
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (nickDraft.isNotBlank()) onUpdateNickname(nickDraft)
                                editingNick = false
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = "저장", tint = SuccessGreen)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(myProfile.tier.badgeColor)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "LV.${myProfile.level}",
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = DeepNavyOutline
                                )
                            }
                            Text(
                                text = myProfile.nickname,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { editingNick = true }, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Rounded.Edit, contentDescription = "닉네임 수정", tint = Color(0xFF90E0EF))
                        }
                    }
                }

                // 레벨업 XP 게이지
                val xpFraction = (myProfile.xp.toFloat() / myProfile.xpNeededForNextLevel.toFloat()).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "EXP ${myProfile.xp} / ${myProfile.xpNeededForNextLevel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF90E0EF)
                        )
                        Text(
                            text = if (isKo) "AI전 HP +${myProfile.levelHpBonus}" else "AI HP +${myProfile.levelHpBonus}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SuccessGreen
                        )
                    }
                    LinearProgressIndicator(
                        progress = { xpFraction },
                        color = SuccessGreen,
                        trackColor = Color(0xFF23395B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )
                }

                // 티어 & 승패 요약
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A3150))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ProfileMiniStat(if (isKo) "티어" else "Tier", myProfile.tier.label(language), myProfile.tier.badgeColor)
                    ProfileMiniStat("RP", "${myProfile.rankPoints}", PrimaryGold)
                    ProfileMiniStat(if (isKo) "승/패" else "W/L", "${myProfile.wins}승 ${myProfile.losses}패", Color.White)
                    ProfileMiniStat(if (isKo) "승률" else "Win%", "${myProfile.winRatePercent}%", SuccessGreen)
                }

                Text(
                    text = if (isKo) "온라인 전적: ${myProfile.onlineWins}승 ${myProfile.onlineLosses}패 · 최고 ${myProfile.bestWinStreak}연승"
                    else "Online: ${myProfile.onlineWins}W ${myProfile.onlineLosses}L · Best Streak ${myProfile.bestWinStreak}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }
        }

        // 우측: 실시간 글로벌 랭킹 보드 + 바로 초대
        Column(
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF102036))
                .border(2.dp, Color(0xFF284369), RoundedCornerShape(18.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isKo) "글로벌 랭킹 TOP 50 (유저를 눌러 바로 초대!)" else "Global Leaderboard (Invite Players)",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryGold
            )

            when (leaderboardState) {
                is OnlineUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryGold)
                }
                is OnlineUiState.Error -> Text(leaderboardState.message, color = DangerRed)
                is OnlineUiState.Success -> {
                    val players = leaderboardState.data.take(50)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(players, key = { _, p -> p.userId }) { index, player ->
                            val rank = index + 1
                            val isMe = player.userId == currentUserId
                            val rankColor = when (rank) {
                                1 -> Color(0xFFFFD166)
                                2 -> Color(0xFFD9E2EC)
                                3 -> Color(0xFFCD7F32)
                                else -> Color(0xFF33527A)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isMe) Color(0xFF23436E) else Color(0xFF182C47))
                                    .border(
                                        width = if (isMe) 2.dp else 1.dp,
                                        color = if (isMe) PrimaryGold else Color(0xFF2A446B),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(rankColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$rank",
                                            fontFamily = FredokaFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (rank <= 3) DeepNavyOutline else Color.White
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${player.nickname} (Lv.${player.level})",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${player.tier.label(language)} · ${player.rankPoints} RP · ${player.wins}승 ${player.losses}패 (${player.winRatePercent}%)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = player.tier.badgeColor
                                        )
                                    }
                                }

                                if (!isMe) {
                                    Button(
                                        onClick = { onInvitePlayer(player) },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.buttonColors(containerColor = PlayerBlue),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isKo) "초대" else "Invite", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                    }
                                } else {
                                    Text(
                                        text = if (isKo) "나" else "ME",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = PrimaryGold,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMiniStat(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = valueColor, fontWeight = FontWeight.Bold)
    }
}
