package com.example.ui.online

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebase.MatchProgressionResult
import com.example.firebase.MatchRoom
import com.example.firebase.OnlineGameRepository
import com.example.firebase.OnlineUiState
import com.example.firebase.PlayerProfile
import com.example.firebase.RoomInvite
import com.example.game.engine.OnlineTurnSubmission
import com.example.game.model.Difficulty
import com.example.game.model.MapTheme
import com.example.game.model.TankType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class OnlineViewModel(
    private val repository: OnlineGameRepository,
    val currentUserId: String
) : ViewModel() {

    val myProfileState: StateFlow<OnlineUiState<PlayerProfile?>> = repository.observeMyProfile(currentUserId)
        .map<PlayerProfile?, OnlineUiState<PlayerProfile?>> { OnlineUiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing profile", root)
            emit(OnlineUiState.Error(root.message ?: "프로필을 불러오지 못했습니다."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = OnlineUiState.Loading
        )

    val leaderboardState: StateFlow<OnlineUiState<List<PlayerProfile>>> = repository.observeLeaderboard(currentUserId)
        .map<List<PlayerProfile>, OnlineUiState<List<PlayerProfile>>> { OnlineUiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing leaderboard", root)
            emit(OnlineUiState.Error(root.message ?: "랭킹을 불러오지 못했습니다."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = OnlineUiState.Loading
        )

    val waitingRoomsState: StateFlow<OnlineUiState<List<MatchRoom>>> = repository.observeWaitingRooms(currentUserId)
        .map<List<MatchRoom>, OnlineUiState<List<MatchRoom>>> { OnlineUiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing rooms", root)
            emit(OnlineUiState.Error(root.message ?: "대기실 목록을 불러오지 못했습니다."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = OnlineUiState.Loading
        )

    val incomingInvitesState: StateFlow<OnlineUiState<List<RoomInvite>>> = repository.observeIncomingInvites(currentUserId)
        .map<List<RoomInvite>, OnlineUiState<List<RoomInvite>>> { OnlineUiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing invites", root)
            emit(OnlineUiState.Error(root.message ?: "초대 목록을 불러오지 못했습니다."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = OnlineUiState.Loading
        )

    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomState: StateFlow<MatchRoom?> = _activeRoomId
        .flatMapLatest { roomId ->
            if (roomId.isNullOrBlank()) {
                flowOf(null)
            } else {
                repository.observeRoom(roomId, currentUserId).catch { error ->
                    val firestoreError = unwrapFirestoreError(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing active room", firestoreError ?: error)
                    emit(null)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = null
        )

    private val _lastProgression = MutableStateFlow<MatchProgressionResult?>(null)
    val lastProgression: StateFlow<MatchProgressionResult?> = _lastProgression.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _latestPushInvite = MutableStateFlow<RoomInvite?>(null)
    val latestPushInvite: StateFlow<RoomInvite?> = _latestPushInvite.asStateFlow()

    private val seenInviteIds = mutableSetOf<String>()
    private var initialInvitesSeeded = false

    init {
        viewModelScope.launch {
            incomingInvitesState.collect { state ->
                val invites = (state as? OnlineUiState.Success)?.data ?: return@collect
                if (!initialInvitesSeeded) {
                    invites.forEach { seenInviteIds.add(it.id) }
                    initialInvitesSeeded = true
                } else {
                    val fresh = invites.firstOrNull { it.id.isNotBlank() && it.id !in seenInviteIds }
                    invites.forEach { seenInviteIds.add(it.id) }
                    if (fresh != null) {
                        _latestPushInvite.value = fresh
                    }
                    val activeBanner = _latestPushInvite.value
                    if (activeBanner != null && invites.none { it.id == activeBanner.id }) {
                        _latestPushInvite.value = null
                    }
                }
            }
        }
    }

    fun dismissPushInvite() {
        _latestPushInvite.value = null
    }

    fun currentProfileOrDefault(fallbackName: String = "전차병"): PlayerProfile {
        val state = myProfileState.value
        return (state as? OnlineUiState.Success)?.data
            ?: PlayerProfile(id = currentUserId, userId = currentUserId, nickname = fallbackName)
    }

    fun ensureProfile(defaultNickname: String, initialPushEnabled: Boolean = false) {
        viewModelScope.launch {
            repository.ensurePlayerProfile(defaultNickname, initialPushEnabled)
        }
    }

    fun updateNickname(newNickname: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateNickname(newNickname)
            _isBusy.value = false
            _statusMessage.value = if (res.isSuccess) {
                "닉네임이 변경되었습니다!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "닉네임 변경 실패"
            }
        }
    }

    fun updateSelectedTank(tankType: TankType) {
        viewModelScope.launch {
            val res = repository.updateSelectedTank(tankType)
            if (res.isFailure) {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "탱크 변경 저장 실패"
            }
            val room = activeRoomState.value
            if (room != null && room.status == "WAITING") {
                repository.updateRoomTank(room, tankType)
            }
        }
    }

    fun updateInvitePushEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateInvitePushEnabled(enabled)
        }
    }

    fun createRoom(roomTitle: String, mapTheme: MapTheme, fallbackName: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val profile = currentProfileOrDefault(fallbackName)
            val res = repository.createRoom(profile, roomTitle, mapTheme)
            _isBusy.value = false
            res.onSuccess { room ->
                _activeRoomId.value = room.id
                _statusMessage.value = "대기실 생성 완료! 초대 코드: ${room.roomCode}"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "대기실 생성에 실패했습니다."
            }
        }
    }

    fun joinRoomById(roomId: String, fallbackName: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val profile = currentProfileOrDefault(fallbackName)
            val res = repository.joinRoomById(roomId, profile)
            _isBusy.value = false
            res.onSuccess { room ->
                _activeRoomId.value = room.id
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "대기실 입장에 실패했습니다."
            }
        }
    }

    fun joinRoomByCode(code: String, fallbackName: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val profile = currentProfileOrDefault(fallbackName)
            val res = repository.joinRoomByCode(code, profile)
            _isBusy.value = false
            res.onSuccess { room ->
                _activeRoomId.value = room.id
                _statusMessage.value = "${room.hostName}님의 방에 입장했습니다!"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "초대 코드로 입장에 실패했습니다."
            }
        }
    }

    fun toggleGuestReady() {
        val room = activeRoomState.value ?: return
        viewModelScope.launch {
            repository.toggleGuestReady(room.id, !room.guestReady)
        }
    }

    fun changeRoomMap(mapTheme: MapTheme) {
        val room = activeRoomState.value ?: return
        if (room.hostUid != currentUserId || room.status != "WAITING") return
        viewModelScope.launch {
            repository.updateRoomMap(room.id, mapTheme)
        }
    }

    fun startOnlineBattle() {
        val room = activeRoomState.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.startOnlineMatch(room)
            _isBusy.value = false
            if (res.isFailure) {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "게임 시작 실패"
            }
        }
    }

    fun leaveOrCloseActiveRoom() {
        val room = activeRoomState.value
        _activeRoomId.value = null
        if (room == null) return
        viewModelScope.launch {
            if (room.status == "WAITING") {
                repository.leaveOrCloseWaitingRoom(room)
            } else if (room.status == "PLAYING") {
                val opponentUid = room.participantUids.firstOrNull { it != currentUserId }
                if (!opponentUid.isNullOrBlank()) {
                    repository.finishOnlineMatch(room, winnerUid = opponentUid, actionType = "FORFEIT")
                }
            }
        }
    }

    fun clearActiveRoomLocal() {
        _activeRoomId.value = null
    }

    fun invitePlayerToActiveOrNewRoom(
        targetProfile: PlayerProfile,
        fallbackName: String,
        preferredMap: MapTheme
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val myProfile = currentProfileOrDefault(fallbackName)
            var room = activeRoomState.value
            if (room == null || room.hostUid != currentUserId || room.status != "WAITING") {
                val created = repository.createRoom(
                    hostProfile = myProfile,
                    roomTitle = "${myProfile.nickname} vs ${targetProfile.nickname}",
                    mapTheme = preferredMap
                )
                room = created.getOrNull()
                if (room != null) {
                    _activeRoomId.value = room.id
                }
            }
            if (room == null) {
                _isBusy.value = false
                _statusMessage.value = "초대용 대기실을 만들지 못했습니다."
                return@launch
            }
            val inviteRes = repository.sendRoomInvite(room, myProfile, targetProfile)
            _isBusy.value = false
            _statusMessage.value = if (inviteRes.isSuccess) {
                if (targetProfile.invitePushEnabled) {
                    "${targetProfile.nickname}님께 대전 초대 푸시 알림을 보냈습니다!"
                } else {
                    "${targetProfile.nickname}님께 초대장을 보냈습니다 (상대가 푸시 알림 꺼둠 · 초대함으로 전달됨)"
                }
            } else {
                inviteRes.exceptionOrNull()?.localizedMessage ?: "초대 전송 실패"
            }
        }
    }

    fun acceptInvite(invite: RoomInvite, fallbackName: String) {
        viewModelScope.launch {
            if (_latestPushInvite.value?.id == invite.id) {
                _latestPushInvite.value = null
            }
            _isBusy.value = true
            repository.respondToInvite(invite.id, accept = true)
            val profile = currentProfileOrDefault(fallbackName)
            val joinRes = repository.joinRoomById(invite.roomId, profile)
            _isBusy.value = false
            joinRes.onSuccess { room ->
                _activeRoomId.value = room.id
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "방이 이미 닫혔거나 가득 찼습니다."
            }
        }
    }

    fun declineInvite(invite: RoomInvite) {
        viewModelScope.launch {
            if (_latestPushInvite.value?.id == invite.id) {
                _latestPushInvite.value = null
            }
            repository.respondToInvite(invite.id, accept = false)
        }
    }

    fun submitOnlineTurn(submission: OnlineTurnSubmission) {
        val room = activeRoomState.value ?: return
        if (room.status != "PLAYING" || room.activeTurnUid != currentUserId) return
        viewModelScope.launch {
            repository.submitTurnAction(
                room = room,
                actionType = submission.actionType,
                tankX = submission.tankX,
                tankY = submission.tankY,
                facingRight = submission.facingRight,
                aimAngleDeg = submission.aimAngleDeg,
                power = submission.power,
                weapon = submission.weapon,
                usedItem = submission.usedItem
            )
        }
    }

    fun finishOnlineMatchIfNeeded(winnerTankIndex: Int?) {
        val room = activeRoomState.value ?: return
        if (room.status != "PLAYING" || winnerTankIndex == null) return
        val winnerUid = if (winnerTankIndex == 0) room.hostUid else room.guestUid
        if (winnerUid.isBlank()) return
        viewModelScope.launch {
            repository.finishOnlineMatch(room, winnerUid = winnerUid)
        }
    }

    fun recordMatchProgression(
        won: Boolean,
        isOnline: Boolean,
        difficulty: Difficulty,
        damageDealt: Int
    ) {
        viewModelScope.launch {
            val res = repository.recordMatchResult(
                won = won,
                isOnline = isOnline,
                difficulty = difficulty,
                damageDealt = damageDealt
            )
            res.onSuccess { progression ->
                _lastProgression.value = progression
            }
        }
    }

    fun clearProgression() {
        _lastProgression.value = null
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }

    private companion object {
        const val TAG = "OnlineViewModel"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
