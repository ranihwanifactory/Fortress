package com.example.firebase

import android.content.Context
import com.example.R
import com.example.game.model.Difficulty
import com.example.game.model.ItemType
import com.example.game.model.MapTheme
import com.example.game.model.TankType
import com.example.game.model.WeaponType
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/**
 * 명명된 Firestore 엔터프라이즈 DB(R.string.firestore_database_id)를 사용하는
 * 온라인 프로필·랭킹·멀티플레이 대기실·초대장 저장소.
 */
class OnlineGameRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("Google 로그인 후 이용할 수 있습니다.")
        }
        return uid
    }

    // -------------------------------------------------------------------------
    // 1. 플레이어 프로필, 레벨업 & 글로벌 랭킹
    // -------------------------------------------------------------------------

    suspend fun ensurePlayerProfile(
        defaultNickname: String,
        initialPushEnabled: Boolean = false
    ): Result<PlayerProfile> {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Not authenticated"))
        }
        val path = "$COL_PROFILES/$uid"
        return try {
            val docRef = db.collection(COL_PROFILES).document(uid)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(
                    PlayerProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existing != null) return Result.success(existing)
            }
            val safeNick = sanitizeNickname(defaultNickname)
            val payload = mapOf(
                "userId" to uid,
                "nickname" to safeNick,
                "selectedTank" to TankType.CLASSIC.name,
                "invitePushEnabled" to initialPushEnabled,
                "listedInRanking" to true,
                "level" to 1L,
                "xp" to 0L,
                "totalXp" to 0L,
                "wins" to 0L,
                "losses" to 0L,
                "onlineWins" to 0L,
                "onlineLosses" to 0L,
                "rankPoints" to 1000L,
                "winStreak" to 0L,
                "bestWinStreak" to 0L,
                "totalDamage" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(
                PlayerProfile(
                    id = uid,
                    userId = uid,
                    nickname = safeNick,
                    selectedTank = TankType.CLASSIC.name,
                    invitePushEnabled = initialPushEnabled
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeMyProfile(uid: String): Flow<PlayerProfile?> = flow {
        if (uid.isBlank()) return@flow
        val path = "$COL_PROFILES/$uid"
        emitAll(
            db.collection(COL_PROFILES).document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            PlayerProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, uid, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeLeaderboard(uid: String): Flow<List<PlayerProfile>> = flow {
        if (uid.isBlank()) return@flow
        val path = COL_PROFILES
        emitAll(
            db.collection(COL_PROFILES)
                .whereEqualTo("listedInRanking", true)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PlayerProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<PlayerProfile> { it.rankPoints }
                            .thenByDescending { it.level }
                            .thenByDescending { it.wins }
                    )
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun updateNickname(newNickname: String): Result<Unit> {
        var path = "$COL_PROFILES/unknown"
        return try {
            val uid = requireUserId()
            path = "$COL_PROFILES/$uid"
            val sanitized = sanitizeNickname(newNickname)
            db.collection(COL_PROFILES).document(uid)
                .update(
                    mapOf(
                        "nickname" to sanitized,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSelectedTank(tankType: TankType): Result<Unit> {
        var path = "$COL_PROFILES/unknown"
        return try {
            val uid = requireUserId()
            path = "$COL_PROFILES/$uid"
            db.collection(COL_PROFILES).document(uid)
                .update(
                    mapOf(
                        "selectedTank" to tankType.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateInvitePushEnabled(enabled: Boolean): Result<Unit> {
        var path = "$COL_PROFILES/unknown"
        return try {
            val uid = requireUserId()
            path = "$COL_PROFILES/$uid"
            db.collection(COL_PROFILES).document(uid)
                .update(
                    mapOf(
                        "invitePushEnabled" to enabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordMatchResult(
        won: Boolean,
        isOnline: Boolean,
        difficulty: Difficulty,
        damageDealt: Int
    ): Result<MatchProgressionResult> {
        var path = "$COL_PROFILES/unknown"
        return try {
            val uid = requireUserId()
            path = "$COL_PROFILES/$uid"
            val docRef = db.collection(COL_PROFILES).document(uid)
            val snap = docRef.get().await()
            val current = snap.toObject(
                PlayerProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: ensurePlayerProfile(auth.currentUser?.displayName ?: "전차병").getOrThrow()

            val progression = LevelProgressionCalculator.calculate(
                current = current,
                won = won,
                isOnline = isOnline,
                difficulty = difficulty,
                damageDealt = damageDealt
            )
            val u = progression.updatedProfile
            docRef.update(
                mapOf(
                    "level" to u.level,
                    "xp" to u.xp,
                    "totalXp" to u.totalXp,
                    "wins" to u.wins,
                    "losses" to u.losses,
                    "onlineWins" to u.onlineWins,
                    "onlineLosses" to u.onlineLosses,
                    "rankPoints" to u.rankPoints,
                    "winStreak" to u.winStreak,
                    "bestWinStreak" to u.bestWinStreak,
                    "totalDamage" to u.totalDamage,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(progression)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // 2. 멀티플레이어 대기실 & 실시간 턴제 대전 동기화
    // -------------------------------------------------------------------------

    fun observeWaitingRooms(uid: String): Flow<List<MatchRoom>> = flow {
        if (uid.isBlank()) return@flow
        emitAll(
            db.collection(COL_ROOMS)
                .whereEqualTo("status", "WAITING")
                .limit(40)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MatchRoom::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, uid, OperationType.LIST, COL_ROOMS)
                    throw error
                }
        )
    }

    fun observeRoom(roomId: String, uid: String): Flow<MatchRoom?> = flow {
        if (uid.isBlank() || roomId.isBlank()) return@flow
        val path = "$COL_ROOMS/$roomId"
        emitAll(
            db.collection(COL_ROOMS).document(roomId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            MatchRoom::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, uid, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun createRoom(
        hostProfile: PlayerProfile,
        roomTitle: String,
        mapTheme: MapTheme
    ): Result<MatchRoom> {
        var path = COL_ROOMS
        return try {
            val uid = requireUserId()
            val docRef = db.collection(COL_ROOMS).document()
            path = docRef.path
            val code = generateRoomCode()
            val safeTitle = roomTitle.trim().take(36).ifBlank { "${hostProfile.nickname}의 대전방" }
            val seed = Random.nextLong(1000L, 999_999_999L)
            val initWind = Random.nextInt(-6, 7).toLong()

            val hostTankName = hostProfile.parsedTankType().name
            val payload = mapOf(
                "roomCode" to code,
                "roomTitle" to safeTitle,
                "hostUid" to uid,
                "hostName" to sanitizeNickname(hostProfile.nickname),
                "hostLevel" to hostProfile.level.coerceIn(1L, 999L),
                "hostTank" to hostTankName,
                "guestUid" to "",
                "guestName" to "",
                "guestLevel" to 0L,
                "guestTank" to "",
                "guestReady" to false,
                "participantUids" to listOf(uid),
                "mapTheme" to mapTheme.name,
                "terrainSeed" to seed,
                "status" to "WAITING",
                "activeTurnUid" to uid,
                "turnSeq" to 0L,
                "windForce" to initWind,
                "lastActionType" to "NONE",
                "lastActionUid" to "",
                "lastTankX" to 320.0,
                "lastTankY" to 500.0,
                "lastFacingRight" to true,
                "lastAimAngle" to 45.0,
                "lastPower" to 0.5,
                "lastWeapon" to WeaponType.STANDARD.name,
                "lastUsedItem" to "",
                "winnerUid" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(
                MatchRoom(
                    id = docRef.id,
                    roomCode = code,
                    roomTitle = safeTitle,
                    hostUid = uid,
                    hostName = sanitizeNickname(hostProfile.nickname),
                    hostLevel = hostProfile.level,
                    hostTank = hostTankName,
                    participantUids = listOf(uid),
                    mapTheme = mapTheme.name,
                    terrainSeed = seed,
                    status = "WAITING",
                    activeTurnUid = uid,
                    windForce = initWind
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinRoomById(roomId: String, guestProfile: PlayerProfile): Result<MatchRoom> {
        val path = "$COL_ROOMS/$roomId"
        return try {
            val uid = requireUserId()
            require(roomId.isNotBlank()) { "Room ID required" }
            val docRef = db.collection(COL_ROOMS).document(roomId)
            val snap = docRef.get().await()
            val room = snap.toObject(
                MatchRoom::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(IllegalStateException("대기실을 찾을 수 없습니다."))

            if (room.hostUid == uid || room.guestUid == uid) {
                return Result.success(room)
            }
            if (room.status != "WAITING" || room.guestUid.isNotBlank()) {
                return Result.failure(IllegalStateException("이미 시작되었거나 가득 찬 방입니다."))
            }

            val guestTankName = guestProfile.parsedTankType().name
            docRef.update(
                mapOf(
                    "guestUid" to uid,
                    "guestName" to sanitizeNickname(guestProfile.nickname),
                    "guestLevel" to guestProfile.level.coerceIn(1L, 999L),
                    "guestTank" to guestTankName,
                    "guestReady" to false,
                    "participantUids" to listOf(room.hostUid, uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(
                room.copy(
                    guestUid = uid,
                    guestName = sanitizeNickname(guestProfile.nickname),
                    guestLevel = guestProfile.level,
                    guestTank = guestTankName,
                    guestReady = false,
                    participantUids = listOf(room.hostUid, uid)
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinRoomByCode(rawCode: String, guestProfile: PlayerProfile): Result<MatchRoom> {
        return try {
            requireUserId()
            val code = rawCode.trim().uppercase()
            if (code.length != 6) {
                return Result.failure(IllegalArgumentException("6자리 초대 코드를 입력해 주세요."))
            }
            val querySnap = db.collection(COL_ROOMS)
                .whereEqualTo("status", "WAITING")
                .limit(100)
                .get()
                .await()
            val roomDoc = querySnap.documents.firstOrNull { it.getString("roomCode") == code }
                ?: return Result.failure(IllegalStateException("해당 초대 코드의 대기실을 찾을 수 없습니다."))
            joinRoomById(roomDoc.id, guestProfile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, COL_ROOMS)
            Result.failure(e)
        }
    }

    suspend fun toggleGuestReady(roomId: String, ready: Boolean): Result<Unit> {
        val path = "$COL_ROOMS/$roomId"
        return try {
            requireUserId()
            db.collection(COL_ROOMS).document(roomId)
                .update(
                    mapOf(
                        "guestReady" to ready,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateRoomMap(roomId: String, mapTheme: MapTheme): Result<Unit> {
        val path = "$COL_ROOMS/$roomId"
        return try {
            requireUserId()
            db.collection(COL_ROOMS).document(roomId)
                .update(
                    mapOf(
                        "mapTheme" to mapTheme.name,
                        "terrainSeed" to Random.nextLong(1000L, 999_999_999L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun startOnlineMatch(room: MatchRoom): Result<Unit> {
        val path = "$COL_ROOMS/${room.id}"
        return try {
            val uid = requireUserId()
            check(room.hostUid == uid) { "방장만 게임을 시작할 수 있습니다." }
            check(room.guestUid.isNotBlank()) { "상대방이 입장할 때까지 기다려 주세요." }
            val seed = if (room.terrainSeed > 0L) room.terrainSeed else Random.nextLong(1000L, 999_999_999L)
            val initWind = Random.nextInt(-7, 8).toLong()
            db.collection(COL_ROOMS).document(room.id)
                .update(
                    mapOf(
                        "status" to "PLAYING",
                        "activeTurnUid" to room.hostUid,
                        "terrainSeed" to seed,
                        "windForce" to initWind,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateRoomTank(room: MatchRoom, tankType: TankType): Result<Unit> {
        val path = "$COL_ROOMS/${room.id}"
        return try {
            val uid = requireUserId()
            if (room.status != "WAITING") return Result.success(Unit)
            val field = when (uid) {
                room.hostUid -> "hostTank"
                room.guestUid -> "guestTank"
                else -> return Result.success(Unit)
            }
            db.collection(COL_ROOMS).document(room.id)
                .update(
                    mapOf(
                        field to tankType.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun leaveOrCloseWaitingRoom(room: MatchRoom): Result<Unit> {
        val path = "$COL_ROOMS/${room.id}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection(COL_ROOMS).document(room.id)
            if (room.hostUid == uid && room.status == "WAITING") {
                docRef.delete().await()
            } else if (room.guestUid == uid && room.status == "WAITING") {
                docRef.update(
                    mapOf(
                        "guestUid" to "",
                        "guestName" to "",
                        "guestLevel" to 0L,
                        "guestTank" to "",
                        "guestReady" to false,
                        "participantUids" to listOf(room.hostUid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitTurnAction(
        room: MatchRoom,
        actionType: String,
        tankX: Float,
        tankY: Float,
        facingRight: Boolean,
        aimAngleDeg: Float,
        power: Float,
        weapon: WeaponType,
        usedItem: ItemType?
    ): Result<Unit> {
        val path = "$COL_ROOMS/${room.id}"
        return try {
            val uid = requireUserId()
            val nextTurnUid = room.participantUids.firstOrNull { it != uid }
                ?: return Result.failure(IllegalStateException("Opponent UID not found"))
            val nextWind = Random.nextInt(-8, 9).toLong()
            db.collection(COL_ROOMS).document(room.id)
                .update(
                    mapOf(
                        "activeTurnUid" to nextTurnUid,
                        "turnSeq" to (room.turnSeq + 1L),
                        "windForce" to nextWind,
                        "lastActionType" to actionType,
                        "lastActionUid" to uid,
                        "lastTankX" to tankX.toDouble().coerceIn(0.0, 3000.0),
                        "lastTankY" to tankY.toDouble().coerceIn(0.0, 1500.0),
                        "lastFacingRight" to facingRight,
                        "lastAimAngle" to aimAngleDeg.toDouble().coerceIn(0.0, 90.0),
                        "lastPower" to power.toDouble().coerceIn(0.0, 1.0),
                        "lastWeapon" to weapon.name,
                        "lastUsedItem" to (usedItem?.name ?: ""),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun finishOnlineMatch(room: MatchRoom, winnerUid: String, actionType: String = "FIRE"): Result<Unit> {
        val path = "$COL_ROOMS/${room.id}"
        return try {
            val uid = requireUserId()
            if (room.status == "FINISHED") return Result.success(Unit)
            db.collection(COL_ROOMS).document(room.id)
                .update(
                    mapOf(
                        "status" to "FINISHED",
                        "winnerUid" to winnerUid,
                        "lastActionType" to actionType,
                        "lastActionUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // 3. 플레이어 직접 초대장 (Room Invites)
    // -------------------------------------------------------------------------

    fun observeIncomingInvites(uid: String): Flow<List<RoomInvite>> = flow {
        if (uid.isBlank()) return@flow
        emitAll(
            db.collection(COL_INVITES)
                .whereEqualTo("toUid", uid)
                .limit(30)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RoomInvite::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                        .filter { it.status == "PENDING" }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, uid, OperationType.LIST, COL_INVITES)
                    throw error
                }
        )
    }

    suspend fun sendRoomInvite(
        room: MatchRoom,
        fromProfile: PlayerProfile,
        targetProfile: PlayerProfile
    ): Result<Unit> {
        var path = COL_INVITES
        return try {
            val uid = requireUserId()
            if (targetProfile.userId == uid) {
                return Result.failure(IllegalArgumentException("자기 자신은 초대할 수 없습니다."))
            }
            val docRef = db.collection(COL_INVITES).document()
            path = docRef.path
            val payload = mapOf(
                "roomId" to room.id,
                "roomCode" to room.roomCode,
                "roomTitle" to room.roomTitle.take(36),
                "mapTheme" to room.mapTheme,
                "fromUid" to uid,
                "fromName" to sanitizeNickname(fromProfile.nickname),
                "toUid" to targetProfile.userId,
                "toName" to sanitizeNickname(targetProfile.nickname),
                "status" to "PENDING",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun respondToInvite(inviteId: String, accept: Boolean): Result<Unit> {
        val path = "$COL_INVITES/$inviteId"
        return try {
            requireUserId()
            db.collection(COL_INVITES).document(inviteId)
                .update(
                    mapOf(
                        "status" to if (accept) "ACCEPTED" else "DECLINED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private fun logStreamErrorIfSignedIn(
        error: Throwable,
        expectedUid: String,
        operationType: OperationType,
        path: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    private fun sanitizeNickname(raw: String): String {
        val trimmed = raw.trim().replace("\n", " ")
        return trimmed.take(24).ifBlank { "전차병" }
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    companion object {
        const val COL_PROFILES = "player_profiles"
        const val COL_ROOMS = "match_rooms"
        const val COL_INVITES = "room_invites"
    }
}
