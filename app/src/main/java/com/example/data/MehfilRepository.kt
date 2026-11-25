package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

private fun resolveMehfilFirestore(context: Context): FirebaseFirestore {
    val app = FirebaseApp.getInstance()
    val projectId = app.options.projectId
    return if (projectId == "gen-lang-client-0466626391") {
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    } else {
        FirebaseFirestore.getInstance(app)
    }
}

class MehfilRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        resolveMehfilFirestore(context.applicationContext)
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Mehfil.")

    private fun sanitizeId(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "id" }

    fun directChatId(uid1: String, uid2: String): String {
        val a = sanitizeId(uid1)
        val b = sanitizeId(uid2)
        val combined = if (a <= b) "dm_${a}_$b" else "dm_${b}_$a"
        return combined.take(128)
    }

    private fun <T> guardedSnapshotFlow(
        path: String,
        op: OperationType,
        expectedUid: String? = null,
        flowFactory: () -> Flow<T>
    ): Flow<T> = flow {
        val activeUid = expectedUid ?: requireUserId()
        emitAll(
            flowFactory().catch { error ->
                val unwrapped = unwrapFirestoreError(error)
                if (unwrapped is CancellationException) throw unwrapped
                if (auth.currentUser?.uid == activeUid) {
                    handleFirestoreError(unwrapped, op, path)
                }
                throw unwrapped
            }
        )
    }

    fun observeUserAccount(userId: String): Flow<UserAccount?> =
        guardedSnapshotFlow("users/$userId", OperationType.GET, userId) {
            db.collection("users").document(userId)
                .snapshots()
                .map { it.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    fun observeDiscoverableProfiles(): Flow<List<PublicProfile>> =
        guardedSnapshotFlow("profiles", OperationType.LIST) {
            db.collection("profiles")
                .whereEqualTo("discoverable", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareByDescending<PublicProfile> { it.online }.thenBy { it.displayName.lowercase() })
                }
        }

    fun observeContacts(userId: String): Flow<List<Contact>> =
        guardedSnapshotFlow("users/$userId/contacts", OperationType.LIST, userId) {
            db.collection("users").document(userId).collection("contacts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Contact::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareByDescending<Contact> { it.favorite }.thenBy { it.displayName.lowercase() })
                }
        }

    fun observeChats(userId: String): Flow<List<Chat>> =
        guardedSnapshotFlow("chats", OperationType.LIST, userId) {
            db.collection("chats")
                .whereArrayContains("participantUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Chat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<Chat> { it.pinned }
                                .thenByDescending { it.updatedAt ?: Timestamp(0, 0) }
                        )
                }
        }

    fun observeMessages(chatId: String, userId: String): Flow<List<ChatMessage>> =
        guardedSnapshotFlow("chats/$chatId/messages", OperationType.LIST, userId) {
            db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("participantUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
        }

    fun observeCalls(userId: String): Flow<List<CallRecord>> =
        guardedSnapshotFlow("calls", OperationType.LIST, userId) {
            db.collection("calls")
                .whereArrayContains("participantUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CallRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        }

    fun observeCall(callId: String, userId: String): Flow<CallRecord?> =
        guardedSnapshotFlow("calls/$callId", OperationType.GET, userId) {
            db.collection("calls").document(callId)
                .snapshots()
                .map { it.toObject(CallRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    fun observeCallCandidates(callId: String, userId: String): Flow<List<CallCandidate>> =
        guardedSnapshotFlow("calls/$callId/candidates", OperationType.LIST, userId) {
            db.collection("calls").document(callId).collection("candidates")
                .whereArrayContains("participantUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CallCandidate::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
        }

    fun observeStories(): Flow<List<Story>> =
        guardedSnapshotFlow("stories", OperationType.LIST) {
            db.collection("stories")
                .whereEqualTo("discoverable", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Story::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
                }
        }

    fun observePartyRooms(): Flow<List<PartyRoom>> =
        guardedSnapshotFlow("rooms", OperationType.LIST) {
            db.collection("rooms")
                .whereEqualTo("publicRoom", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<PartyRoom> { it.viewerCount }
                                .thenByDescending { it.updatedAt ?: Timestamp(0, 0) }
                        )
                }
        }

    fun observePartyRoom(roomId: String): Flow<PartyRoom?> =
        guardedSnapshotFlow("rooms/$roomId", OperationType.GET) {
            db.collection("rooms").document(roomId)
                .snapshots()
                .map { it.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    fun observeRoomSeats(roomId: String): Flow<List<RoomSeat>> =
        guardedSnapshotFlow("rooms/$roomId/seats", OperationType.LIST) {
            db.collection("rooms").document(roomId).collection("seats")
                .whereEqualTo("publicRoom", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(RoomSeat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.seatIndex }
                }
        }

    fun observeRoomMessages(roomId: String): Flow<List<RoomMessage>> =
        guardedSnapshotFlow("rooms/$roomId/messages", OperationType.LIST) {
            db.collection("rooms").document(roomId).collection("messages")
                .whereEqualTo("publicRoom", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(RoomMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
        }

    fun observeRoomSongs(roomId: String): Flow<List<RoomSongRequest>> =
        guardedSnapshotFlow("rooms/$roomId/songs", OperationType.LIST) {
            db.collection("rooms").document(roomId).collection("songs")
                .whereEqualTo("publicRoom", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(RoomSongRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<RoomSongRequest> { it.playing }
                                .thenBy { it.createdAt ?: Timestamp(0, 0) }
                        )
                }
        }

    fun observeRoomSignals(roomId: String, userId: String): Flow<List<RoomSignal>> =
        guardedSnapshotFlow("rooms/$roomId/signals", OperationType.LIST, userId) {
            db.collection("rooms").document(roomId).collection("signals")
                .whereArrayContains("participantUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(RoomSignal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
        }

    suspend fun ensureUserProfileAndCircle(
        displayNameHint: String? = null,
        emailHint: String? = null
    ): Result<UserAccount> {
        var path = "users"
        return try {
            val user = auth.currentUser
                ?: throw IllegalStateException("User must be signed in with Google.")
            val uid = user.uid
            val userRef = db.collection("users").document(uid)
            val profileRef = db.collection("profiles").document(uid)
            path = userRef.path

            val existingSnap = userRef.get().await()
            val resolvedName = (displayNameHint ?: user.displayName ?: user.email?.substringBefore("@"))
                ?.trim()?.take(60)?.ifBlank { "Mehfil Member" } ?: "Mehfil Member"
            val resolvedEmail = (emailHint ?: user.email)?.trim()?.take(120)?.takeIf { it.isNotEmpty() }
            val defaultMehfilId = "mehfil_${uid.lowercase().replace(Regex("[^a-z0-9]"), "").takeLast(6).padStart(4, 'm')}"

            val account: UserAccount
            if (!existingSnap.exists()) {
                val payload = mapOf<String, Any?>(
                    "userId" to uid,
                    "displayName" to resolvedName,
                    "email" to resolvedEmail,
                    "mehfilId" to defaultMehfilId,
                    "bio" to "Yahan har baat ek Mehfil hai",
                    "online" to true,
                    "lowDataMode" to false,
                    "coinBalance" to 5000L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                userRef.set(payload).await()

                val profilePayload = mapOf(
                    "userId" to uid,
                    "displayName" to resolvedName,
                    "mehfilId" to defaultMehfilId,
                    "statusText" to "Available on Mehfil",
                    "avatarColorHex" to "#6E2CF2",
                    "online" to true,
                    "discoverable" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                profileRef.set(profilePayload).await()

                seedPersonalWelcome(uid, resolvedName, defaultMehfilId)
                account = UserAccount(
                    id = uid,
                    userId = uid,
                    displayName = resolvedName,
                    email = resolvedEmail,
                    mehfilId = defaultMehfilId,
                    bio = "Yahan har baat ek Mehfil hai",
                    online = true,
                    lowDataMode = false,
                    coinBalance = 5000L
                )
            } else {
                val parsed = existingSnap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                val finalName = parsed?.displayName?.ifBlank { resolvedName } ?: resolvedName
                val finalMehfilId = parsed?.mehfilId?.ifBlank { defaultMehfilId } ?: defaultMehfilId
                val finalBio = parsed?.bio ?: "Yahan har baat ek Mehfil hai"

                val profileSnap = profileRef.get().await()
                if (!profileSnap.exists()) {
                    profileRef.set(
                        mapOf(
                            "userId" to uid,
                            "displayName" to finalName,
                            "mehfilId" to finalMehfilId,
                            "statusText" to finalBio.take(120),
                            "avatarColorHex" to "#6E2CF2",
                            "online" to true,
                            "discoverable" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    profileRef.update(
                        mapOf(
                            "online" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                userRef.update(
                    mapOf(
                        "online" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                account = parsed ?: UserAccount(
                    id = uid,
                    userId = uid,
                    displayName = finalName,
                    email = resolvedEmail,
                    mehfilId = finalMehfilId,
                    bio = finalBio
                )
            }

            seedStarterRoomsIfEmpty(uid, account.displayName)
            Result.success(account)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun seedPersonalWelcome(uid: String, displayName: String, mehfilId: String) {
        try {
            val studioContactRef = db.collection("users").document(uid)
                .collection("contacts").document("mehfil_studio")
            if (!studioContactRef.get().await().exists()) {
                studioContactRef.set(
                    mapOf(
                        "userId" to uid,
                        "peerId" to "mehfil_studio",
                        "displayName" to "Mehfil Studio",
                        "mehfilId" to "mehfil_studio",
                        "statusText" to "Live Camera, Mic & Audio Self-Check",
                        "avatarColorHex" to "#6E2CF2",
                        "online" to true,
                        "favorite" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val studioChatId = directChatId(uid, "mehfil_studio")
            val chatRef = db.collection("chats").document(studioChatId)
            val batch = db.batch()
            if (!chatRef.get().await().exists()) {
                batch.set(
                    chatRef,
                    mapOf(
                        "participantUids" to listOf(uid, "mehfil_studio"),
                        "participantNames" to mapOf(uid to displayName, "mehfil_studio" to "Mehfil Studio"),
                        "chatKind" to ChatKind.DIRECT.name,
                        "title" to "Mehfil Studio",
                        "lastMessageText" to "Welcome to Mehfil! Add friends from Mehfil Circle or join a Party Room.",
                        "lastMessageType" to MessageType.TEXT.name,
                        "lastMessageSenderId" to uid,
                        "unreadCount" to 0L,
                        "pinned" to true,
                        "muted" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val welcomeMsgRef = chatRef.collection("messages").document("msg_welcome")
                batch.set(
                    welcomeMsgRef,
                    mapOf(
                        "chatId" to studioChatId,
                        "senderId" to uid,
                        "senderName" to displayName,
                        "participantUids" to listOf(uid, "mehfil_studio"),
                        "messageType" to MessageType.TEXT.name,
                        "text" to "Mehfil activated for $displayName ($mehfilId)! Send voice notes, photos, or start an HD WebRTC call.",
                        "mediaDurationSec" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("MehfilRepo", "Personal welcome seed skipped: ${e.message}")
        }
    }

    suspend fun seedStarterRoomsIfEmpty(hostUid: String? = null, hostName: String? = null) {
        try {
            val uid = hostUid ?: requireUserId()
            val resolvedHost = hostName?.takeIf { it.isNotBlank() } ?: "Mehfil Host"
            val existing = db.collection("rooms")
                .whereEqualTo("publicRoom", true)
                .limit(1)
                .get()
                .await()
            if (!existing.isEmpty) return

            createPartyRoom(
                name = "Sufi & Bollywood DJ Night",
                roomType = RoomType.MUSIC,
                hostId = uid,
                hostName = resolvedHost,
                topic = "Live DJ beats, song requests & 8-mic stage"
            )
            createPartyRoom(
                name = "Royal Gold Mehfil Club",
                roomType = RoomType.PARTY,
                hostId = uid,
                hostName = resolvedHost,
                topic = "8 live mics open • Gifts, reactions & voice chat"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("MehfilRepo", "Starter rooms check skipped: ${e.message}")
        }
    }

    suspend fun createPartyRoom(
        name: String,
        roomType: RoomType,
        hostId: String,
        hostName: String,
        topic: String
    ): Result<String> {
        var docPath = "rooms"
        return try {
            val uid = requireUserId()
            check(uid == hostId) { "Authenticated user mismatch." }
            val cleanName = name.trim().take(60)
            require(cleanName.isNotEmpty()) { "Room name cannot be empty." }
            val cleanHostName = hostName.trim().ifBlank { "Mehfil Host" }.take(60)
            val cleanTopic = topic.trim().ifBlank { roomType.description }.take(120)
            val roomId = "room_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val roomRef = db.collection("rooms").document(roomId)
            docPath = roomRef.path

            val defaultSong = if (roomType == RoomType.MUSIC) "Mehfil Royal Lo-Fi • $cleanHostName" else ""
            val roomPayload = mapOf(
                "name" to cleanName,
                "roomType" to roomType.name,
                "hostId" to uid,
                "hostName" to cleanHostName,
                "topic" to cleanTopic,
                "publicRoom" to true,
                "activeMemberUids" to listOf(uid),
                "viewerCount" to 1L,
                "currentSongTitle" to defaultSong,
                "musicPlaying" to (roomType == RoomType.MUSIC),
                "hostMicOn" to true,
                "totalGiftCoins" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            roomRef.set(roomPayload).await()

            val batch = db.batch()
            for (idx in 0 until 8) {
                val seatRef = roomRef.collection("seats").document("seat_$idx")
                batch.set(
                    seatRef,
                    mapOf(
                        "roomId" to roomId,
                        "seatIndex" to idx,
                        "publicRoom" to true,
                        "avatarColorHex" to "#6E2CF2",
                        "muted" to false,
                        "giftCoins" to 0L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            val msgRef = roomRef.collection("messages").document("rmsg_${UUID.randomUUID().toString().replace("-", "").take(12)}")
            batch.set(
                msgRef,
                mapOf(
                    "roomId" to roomId,
                    "senderId" to uid,
                    "senderName" to cleanHostName,
                    "kind" to RoomMessageKind.SYSTEM.name,
                    "text" to "$cleanHostName started ${roomType.displayTitle}! Tap any seat to join live.",
                    "publicRoom" to true,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            if (roomType == RoomType.MUSIC) {
                val songRef = roomRef.collection("songs").document("song_init_${sanitizeId(roomId)}")
                batch.set(
                    songRef,
                    mapOf(
                        "roomId" to roomId,
                        "title" to "Mehfil Royal Lo-Fi",
                        "artist" to cleanHostName,
                        "requestedBy" to cleanHostName,
                        "requestedByUid" to uid,
                        "playing" to true,
                        "publicRoom" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(roomId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinRoomPresence(roomId: String, userId: String): Result<Unit> {
        val roomRef = db.collection("rooms").document(roomId)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.runTransaction { tx ->
                val snap = tx.get(roomRef)
                if (!snap.exists()) return@runTransaction
                @Suppress("UNCHECKED_CAST")
                val members = (snap.get("activeMemberUids") as? List<String>).orEmpty()
                if (!members.contains(uid)) {
                    val updatedMembers = (members + uid).take(50)
                    tx.update(
                        roomRef,
                        mapOf(
                            "activeMemberUids" to updatedMembers,
                            "viewerCount" to updatedMembers.size.toLong().coerceAtLeast(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun leaveRoomPresence(roomId: String, userId: String): Result<Unit> {
        val roomRef = db.collection("rooms").document(roomId)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            // Vacate any occupied seat cleanly
            val seats = roomRef.collection("seats")
                .whereEqualTo("publicRoom", true)
                .get()
                .await()
                .toObjects(RoomSeat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            seats.filter { it.occupantId == uid }.forEach { seat ->
                roomRef.collection("seats").document(seat.id).update(
                    mapOf(
                        "occupantId" to FieldValue.delete(),
                        "occupantName" to FieldValue.delete(),
                        "lastReaction" to FieldValue.delete(),
                        "muted" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            db.runTransaction { tx ->
                val snap = tx.get(roomRef)
                if (!snap.exists()) return@runTransaction
                @Suppress("UNCHECKED_CAST")
                val members = (snap.get("activeMemberUids") as? List<String>).orEmpty()
                if (members.contains(uid)) {
                    val updatedMembers = members.filterNot { it == uid }
                    tx.update(
                        roomRef,
                        mapOf(
                            "activeMemberUids" to updatedMembers,
                            "viewerCount" to updatedMembers.size.toLong().coerceAtLeast(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun joinOrTapSeat(
        roomId: String,
        seatIndex: Int,
        userId: String,
        userName: String
    ): Result<String> {
        val roomRef = db.collection("rooms").document(roomId)
        val seatDocId = "seat_$seatIndex"
        val seatRef = roomRef.collection("seats").document(seatDocId)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanName = userName.trim().ifBlank { "Mehfil Guest" }.take(60)
            val seatsSnap = roomRef.collection("seats")
                .whereEqualTo("publicRoom", true)
                .get()
                .await()
            val allSeats = seatsSnap.toObjects(RoomSeat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val target = allSeats.firstOrNull { it.seatIndex == seatIndex }
                ?: error("Seat not found")

            return when {
                target.occupantId == uid -> {
                    roomRef.collection("seats").document(target.id).update(
                        mapOf(
                            "occupantId" to FieldValue.delete(),
                            "occupantName" to FieldValue.delete(),
                            "lastReaction" to FieldValue.delete(),
                            "muted" to false,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    Result.success("Stepped down from Mic #${seatIndex + 1}")
                }
                target.occupantId == null -> {
                    // Vacate any other seat held by this user first
                    allSeats.filter { it.occupantId == uid }.forEach { prev ->
                        roomRef.collection("seats").document(prev.id).update(
                            mapOf(
                                "occupantId" to FieldValue.delete(),
                                "occupantName" to FieldValue.delete(),
                                "lastReaction" to FieldValue.delete(),
                                "muted" to false,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                    roomRef.collection("seats").document(target.id).update(
                        mapOf(
                            "occupantId" to uid,
                            "occupantName" to cleanName,
                            "avatarColorHex" to "#6E2CF2",
                            "muted" to false,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    sendRoomSystemMessage(roomId, uid, cleanName, "$cleanName took Mic Seat #${seatIndex + 1}!")
                    Result.success("On Mic Seat #${seatIndex + 1}")
                }
                else -> Result.success("Seat #${seatIndex + 1} is occupied by ${target.occupantName}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, seatRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleUserMicInRoom(
        roomId: String,
        userId: String,
        userName: String
    ): Result<Boolean> {
        val roomRef = db.collection("rooms").document(roomId)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanName = userName.trim().ifBlank { "Mehfil User" }.take(60)
            val room = roomRef.get().await()
                .toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: error("Room not found")

            if (room.hostId == uid) {
                val next = !room.hostMicOn
                roomRef.update(
                    mapOf(
                        "hostMicOn" to next,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                return Result.success(next)
            }

            val allSeats = roomRef.collection("seats")
                .whereEqualTo("publicRoom", true)
                .get()
                .await()
                .toObjects(RoomSeat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedBy { it.seatIndex }

            val mySeat = allSeats.firstOrNull { it.occupantId == uid }
            if (mySeat != null) {
                val nextMicOn = mySeat.muted
                roomRef.collection("seats").document(mySeat.id).update(
                    mapOf(
                        "muted" to !nextMicOn,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(nextMicOn)
            } else {
                val freeSeat = allSeats.firstOrNull { it.occupantId == null }
                    ?: error("All 8 mic seats are currently full")
                roomRef.collection("seats").document(freeSeat.id).update(
                    mapOf(
                        "occupantId" to uid,
                        "occupantName" to cleanName,
                        "avatarColorHex" to "#6E2CF2",
                        "muted" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                sendRoomSystemMessage(roomId, uid, cleanName, "$cleanName hopped onto Mic #${freeSeat.seatIndex + 1}!")
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleRoomMusic(roomId: String): Result<Boolean> {
        val roomRef = db.collection("rooms").document(roomId)
        return try {
            requireUserId()
            val room = roomRef.get().await()
                .toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: error("Room not found")
            val nextPlaying = !room.musicPlaying
            val song = room.currentSongTitle.ifBlank { "Mehfil Royal Lo-Fi" }
            roomRef.update(
                mapOf(
                    "musicPlaying" to nextPlaying,
                    "currentSongTitle" to song,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(nextPlaying)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun requestSongInRoom(
        roomId: String,
        title: String,
        artist: String,
        requesterName: String,
        requesterId: String,
        playImmediately: Boolean = false
    ): Result<Unit> {
        val roomRef = db.collection("rooms").document(roomId)
        var docPath = "${roomRef.path}/songs"
        return try {
            val uid = requireUserId()
            check(uid == requesterId)
            val cleanTitle = title.trim().take(80)
            require(cleanTitle.isNotEmpty()) { "Song title cannot be empty." }
            val cleanArtist = artist.trim().ifBlank { "Mehfil DJ" }.take(60)
            val cleanRequester = requesterName.trim().ifBlank { "Mehfil Guest" }.take(60)
            val songId = "song_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val songRef = roomRef.collection("songs").document(songId)
            docPath = songRef.path

            if (playImmediately) {
                val existingSongs = roomRef.collection("songs")
                    .whereEqualTo("publicRoom", true)
                    .get()
                    .await()
                    .toObjects(RoomSongRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                existingSongs.filter { it.playing }.forEach { oldSong ->
                    roomRef.collection("songs").document(oldSong.id).update(
                        mapOf("playing" to false, "updatedAt" to FieldValue.serverTimestamp())
                    ).await()
                }
            }

            songRef.set(
                mapOf(
                    "roomId" to roomId,
                    "title" to cleanTitle,
                    "artist" to cleanArtist,
                    "requestedBy" to cleanRequester,
                    "requestedByUid" to uid,
                    "playing" to playImmediately,
                    "publicRoom" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (playImmediately) {
                roomRef.update(
                    mapOf(
                        "musicPlaying" to true,
                        "currentSongTitle" to "$cleanTitle • $cleanArtist".take(120),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            roomRef.collection("messages").document("rmsg_${UUID.randomUUID().toString().replace("-", "").take(12)}")
                .set(
                    mapOf(
                        "roomId" to roomId,
                        "senderId" to uid,
                        "senderName" to cleanRequester,
                        "kind" to RoomMessageKind.SONG_REQUEST.name,
                        "text" to "🎵 Requested \"$cleanTitle\" by $cleanArtist",
                        "publicRoom" to true,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun playQueuedSong(roomId: String, song: RoomSongRequest): Result<Unit> {
        val roomRef = db.collection("rooms").document(roomId)
        return try {
            requireUserId()
            val allSongs = roomRef.collection("songs")
                .whereEqualTo("publicRoom", true)
                .get()
                .await()
                .toObjects(RoomSongRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            val batch = db.batch()
            allSongs.forEach { item ->
                val ref = roomRef.collection("songs").document(item.id)
                batch.update(
                    ref,
                    mapOf(
                        "playing" to (item.id == song.id),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.update(
                roomRef,
                mapOf(
                    "musicPlaying" to true,
                    "currentSongTitle" to "${song.title} • ${song.artist}".take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendRoomGift(
        roomId: String,
        senderId: String,
        senderName: String,
        giftEmoji: String,
        giftName: String,
        coins: Long,
        recipientLabel: String,
        recipientSeatId: String? = null
    ): Result<Unit> {
        val roomRef = db.collection("rooms").document(roomId)
        return try {
            val uid = requireUserId()
            check(uid == senderId)
            val cleanSender = senderName.trim().ifBlank { "Mehfil Guest" }.take(60)
            val safeCoins = coins.coerceAtLeast(1L)

            roomRef.update(
                mapOf(
                    "totalGiftCoins" to FieldValue.increment(safeCoins),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (!recipientSeatId.isNullOrBlank()) {
                val seatRef = roomRef.collection("seats").document(recipientSeatId)
                val seat = seatRef.get().await()
                    .toObject(RoomSeat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (seat != null && !seat.occupantId.isNullOrBlank()) {
                    seatRef.update(
                        mapOf(
                            "giftCoins" to FieldValue.increment(safeCoins),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            roomRef.collection("messages").document("rmsg_${UUID.randomUUID().toString().replace("-", "").take(12)}")
                .set(
                    mapOf(
                        "roomId" to roomId,
                        "senderId" to uid,
                        "senderName" to cleanSender,
                        "kind" to RoomMessageKind.GIFT.name,
                        "text" to "$giftEmoji Sent $giftName ($safeCoins 💎) to $recipientLabel!",
                        "publicRoom" to true,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendRoomReaction(
        roomId: String,
        senderId: String,
        senderName: String,
        emoji: String
    ): Result<Unit> {
        val roomRef = db.collection("rooms").document(roomId)
        return try {
            val uid = requireUserId()
            check(uid == senderId)
            val cleanSender = senderName.trim().ifBlank { "Mehfil Guest" }.take(60)
            val cleanEmoji = emoji.trim().take(16).ifBlank { "🔥" }

            val mySeat = roomRef.collection("seats")
                .whereEqualTo("publicRoom", true)
                .get()
                .await()
                .toObjects(RoomSeat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .firstOrNull { it.occupantId == uid }

            if (mySeat != null) {
                roomRef.collection("seats").document(mySeat.id).update(
                    mapOf(
                        "lastReaction" to cleanEmoji,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            roomRef.collection("messages").document("rmsg_${UUID.randomUUID().toString().replace("-", "").take(12)}")
                .set(
                    mapOf(
                        "roomId" to roomId,
                        "senderId" to uid,
                        "senderName" to cleanSender,
                        "kind" to RoomMessageKind.REACTION.name,
                        "text" to "$cleanEmoji reacted",
                        "publicRoom" to true,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "${roomRef.path}/messages")
            Result.failure(e)
        }
    }

    suspend fun sendRoomChatMessage(
        roomId: String,
        senderId: String,
        senderName: String,
        text: String
    ): Result<Unit> {
        val roomRef = db.collection("rooms").document(roomId)
        var docPath = "${roomRef.path}/messages"
        return try {
            val uid = requireUserId()
            check(uid == senderId)
            val clean = text.trim().take(400)
            require(clean.isNotEmpty()) { "Message cannot be empty." }
            val cleanSender = senderName.trim().ifBlank { "Mehfil Guest" }.take(60)
            val msgRef = roomRef.collection("messages")
                .document("rmsg_${UUID.randomUUID().toString().replace("-", "").take(12)}")
            docPath = msgRef.path
            msgRef.set(
                mapOf(
                    "roomId" to roomId,
                    "senderId" to uid,
                    "senderName" to cleanSender,
                    "kind" to RoomMessageKind.TEXT.name,
                    "text" to clean,
                    "publicRoom" to true,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun sendRoomSystemMessage(
        roomId: String,
        senderId: String,
        senderName: String,
        text: String
    ) {
        db.collection("rooms").document(roomId).collection("messages")
            .document("rmsg_${UUID.randomUUID().toString().replace("-", "").take(12)}")
            .set(
                mapOf(
                    "roomId" to roomId,
                    "senderId" to senderId,
                    "senderName" to senderName,
                    "kind" to RoomMessageKind.SYSTEM.name,
                    "text" to text.take(400),
                    "publicRoom" to true,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    suspend fun sendRoomSignal(
        roomId: String,
        fromUid: String,
        toUid: String,
        signalType: String,
        payload: String
    ): Result<Unit> {
        val roomRef = db.collection("rooms").document(roomId)
        var docPath = "${roomRef.path}/signals"
        return try {
            val uid = requireUserId()
            check(uid == fromUid)
            val sigRef = roomRef.collection("signals")
                .document("sig_${UUID.randomUUID().toString().replace("-", "").take(14)}")
            docPath = sigRef.path
            sigRef.set(
                mapOf(
                    "roomId" to roomId,
                    "fromUid" to uid,
                    "toUid" to toUid,
                    "participantUids" to listOf(uid, toUid).distinct(),
                    "signalType" to signalType,
                    "payload" to payload.take(30000),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserSettings(
        userId: String,
        displayName: String,
        bio: String,
        online: Boolean,
        lowDataMode: Boolean
    ): Result<Unit> {
        val userRef = db.collection("users").document(userId)
        val profileRef = db.collection("profiles").document(userId)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanName = displayName.trim().take(60)
            require(cleanName.isNotEmpty()) { "Display name cannot be empty." }
            val cleanBio = bio.trim().take(160)
            userRef.update(
                mapOf(
                    "displayName" to cleanName,
                    "bio" to cleanBio,
                    "online" to online,
                    "lowDataMode" to lowDataMode,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            profileRef.update(
                mapOf(
                    "displayName" to cleanName,
                    "statusText" to cleanBio.take(120).ifBlank { "On Mehfil" },
                    "online" to online,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            Result.failure(e)
        }
    }

    suspend fun addContact(
        userId: String,
        peerId: String,
        displayName: String,
        mehfilId: String,
        statusText: String,
        avatarColorHex: String = "#6E2CF2"
    ): Result<Unit> {
        val cleanPeerId = sanitizeId(peerId).take(128)
        val contactRef = db.collection("users").document(userId).collection("contacts").document(cleanPeerId)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanName = displayName.trim().take(60)
            require(cleanName.isNotEmpty()) { "Contact name cannot be empty." }
            contactRef.set(
                mapOf(
                    "userId" to uid,
                    "peerId" to peerId.trim().take(128),
                    "displayName" to cleanName,
                    "mehfilId" to mehfilId.trim().ifBlank { "mehfil_friend" }.take(40),
                    "statusText" to statusText.trim().ifBlank { "On Mehfil" }.take(120),
                    "avatarColorHex" to avatarColorHex,
                    "online" to true,
                    "favorite" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, contactRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleContactFavorite(userId: String, contactId: String, favorite: Boolean): Result<Unit> {
        val contactRef = db.collection("users").document(userId).collection("contacts").document(contactId)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            contactRef.update(
                mapOf(
                    "favorite" to favorite,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, contactRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteContact(userId: String, contactId: String): Result<Unit> {
        val contactRef = db.collection("users").document(userId).collection("contacts").document(contactId)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            contactRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, contactRef.path)
            Result.failure(e)
        }
    }

    suspend fun getOrCreateDirectChat(
        currentUserId: String,
        currentUserName: String,
        peerId: String,
        peerName: String
    ): Result<String> {
        val chatId = directChatId(currentUserId, peerId)
        val chatRef = db.collection("chats").document(chatId)
        return try {
            val uid = requireUserId()
            check(uid == currentUserId)
            val snap = chatRef.get().await()
            if (!snap.exists()) {
                val cleanMyName = currentUserName.trim().ifBlank { "Mehfil User" }.take(60)
                val cleanPeerName = peerName.trim().ifBlank { "Mehfil Friend" }.take(60)
                val participants = listOf(uid, peerId).distinct()
                val namesMap = if (peerId == uid) {
                    mapOf(uid to cleanMyName)
                } else {
                    mapOf(uid to cleanMyName, peerId to cleanPeerName)
                }
                chatRef.set(
                    mapOf(
                        "participantUids" to participants,
                        "participantNames" to namesMap,
                        "chatKind" to ChatKind.DIRECT.name,
                        "title" to cleanPeerName.take(80),
                        "lastMessageText" to "Say salam on Mehfil!",
                        "lastMessageType" to MessageType.TEXT.name,
                        "lastMessageSenderId" to uid,
                        "unreadCount" to 0L,
                        "pinned" to false,
                        "muted" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, chatRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        messageType: MessageType,
        text: String,
        mediaDurationSec: Long = 0L,
        imageDataBase64: String? = null,
        audioDataBase64: String? = null
    ): Result<Unit> {
        val chatRef = db.collection("chats").document(chatId)
        var msgPath = "${chatRef.path}/messages"
        return try {
            val uid = requireUserId()
            check(uid == senderId)
            val chat = chatRef.get().await()
                .toObject(Chat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: error("Chat does not exist")
            val participants = chat.participantUids.ifEmpty { listOf(uid) }
            val cleanText = text.trim().take(1000)
            val cleanSender = senderName.trim().ifBlank { "Mehfil User" }.take(60)
            val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val msgRef = chatRef.collection("messages").document(msgId)
            msgPath = msgRef.path

            val msgPayload = mapOf<String, Any?>(
                "chatId" to chatId,
                "senderId" to uid,
                "senderName" to cleanSender,
                "participantUids" to participants,
                "messageType" to messageType.name,
                "text" to cleanText,
                "mediaDurationSec" to mediaDurationSec.coerceIn(0L, 600L),
                "imageDataBase64" to imageDataBase64?.take(150000),
                "audioDataBase64" to audioDataBase64?.take(250000),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            val preview = when (messageType) {
                MessageType.VOICE -> "🎤 Voice note (${mediaDurationSec}s)"
                MessageType.IMAGE -> "📷 Photo${if (cleanText.isNotBlank()) ": $cleanText" else ""}"
                MessageType.CALL -> cleanText
                MessageType.TEXT -> cleanText
            }.take(500)

            val batch = db.batch()
            batch.set(msgRef, msgPayload)
            batch.update(
                chatRef,
                mapOf(
                    "lastMessageText" to preview,
                    "lastMessageType" to messageType.name,
                    "lastMessageSenderId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgPath)
            Result.failure(e)
        }
    }

    suspend fun reactToMessage(chatId: String, messageId: String, reaction: String): Result<Unit> {
        val msgRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
        return try {
            requireUserId()
            msgRef.update(
                mapOf(
                    "reaction" to reaction.trim().take(16),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path)
            Result.failure(e)
        }
    }

    suspend fun startCall(
        callerId: String,
        callerName: String,
        calleeId: String,
        calleeName: String,
        callType: CallType,
        qualityMode: CallQualityMode
    ): Result<String> {
        val callId = "call_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val callRef = db.collection("calls").document(callId)
        return try {
            val uid = requireUserId()
            check(uid == callerId)
            val isStudioSelfCheck = calleeId == "mehfil_studio" || calleeId == uid
            val participants = if (calleeId == uid) listOf(uid) else listOf(uid, calleeId)
            callRef.set(
                mapOf(
                    "callerId" to uid,
                    "callerName" to callerName.trim().ifBlank { "Mehfil User" }.take(60),
                    "calleeId" to calleeId.trim().take(128),
                    "calleeName" to calleeName.trim().ifBlank { "Mehfil Contact" }.take(60),
                    "participantUids" to participants,
                    "callType" to callType.name,
                    "status" to if (isStudioSelfCheck) CallStatus.ACTIVE.name else CallStatus.RINGING.name,
                    "qualityMode" to qualityMode.name,
                    "durationSeconds" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(callId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, callRef.path)
            Result.failure(e)
        }
    }

    suspend fun publishCallOffer(callId: String, offerSdp: String): Result<Unit> {
        val callRef = db.collection("calls").document(callId)
        return try {
            requireUserId()
            val current = callRef.get().await().getString("status") ?: return Result.success(Unit)
            if (current != CallStatus.RINGING.name && current != CallStatus.ACTIVE.name) return Result.success(Unit)
            callRef.update(
                mapOf(
                    "offerSdp" to offerSdp.take(30000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, callRef.path)
            Result.failure(e)
        }
    }

    suspend fun publishCallAnswer(callId: String, answerSdp: String): Result<Unit> {
        val callRef = db.collection("calls").document(callId)
        return try {
            requireUserId()
            val current = callRef.get().await().getString("status") ?: return Result.success(Unit)
            if (current != CallStatus.RINGING.name && current != CallStatus.ACTIVE.name) return Result.success(Unit)
            callRef.update(
                mapOf(
                    "status" to CallStatus.ACTIVE.name,
                    "answerSdp" to answerSdp.take(30000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, callRef.path)
            Result.failure(e)
        }
    }

    suspend fun addCallIceCandidate(
        callId: String,
        participantUids: List<String>,
        sdpMid: String,
        sdpMLineIndex: Int,
        sdpCandidate: String
    ): Result<Unit> {
        val callRef = db.collection("calls").document(callId)
        val candRef = callRef.collection("candidates")
            .document("cand_${UUID.randomUUID().toString().replace("-", "").take(14)}")
        return try {
            val uid = requireUserId()
            candRef.set(
                mapOf(
                    "callId" to callId,
                    "senderId" to uid,
                    "participantUids" to participantUids,
                    "sdpMid" to sdpMid.take(64),
                    "sdpMLineIndex" to sdpMLineIndex.coerceIn(0, 32),
                    "sdpCandidate" to sdpCandidate.take(4096),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, candRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCallQuality(callId: String, qualityMode: CallQualityMode): Result<Unit> {
        val callRef = db.collection("calls").document(callId)
        return try {
            requireUserId()
            val current = callRef.get().await().getString("status") ?: return Result.success(Unit)
            if (current != CallStatus.RINGING.name && current != CallStatus.ACTIVE.name) return Result.success(Unit)
            callRef.update(
                mapOf(
                    "qualityMode" to qualityMode.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, callRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCall(
        callId: String,
        status: CallStatus,
        durationSeconds: Long,
        qualityMode: CallQualityMode
    ): Result<Unit> {
        val callRef = db.collection("calls").document(callId)
        return try {
            requireUserId()
            val current = callRef.get().await().getString("status") ?: return Result.success(Unit)
            // Respect terminal state locking in firestore.rules
            if (current != CallStatus.RINGING.name && current != CallStatus.ACTIVE.name) {
                return Result.success(Unit)
            }
            callRef.update(
                mapOf(
                    "status" to status.name,
                    "durationSeconds" to durationSeconds.coerceIn(0L, 86400L),
                    "qualityMode" to qualityMode.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, callRef.path)
            Result.failure(e)
        }
    }

    suspend fun postStory(
        ownerId: String,
        ownerName: String,
        caption: String,
        moodEmoji: String,
        accentHex: String
    ): Result<Unit> {
        val storyId = "story_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val storyRef = db.collection("stories").document(storyId)
        return try {
            val uid = requireUserId()
            check(uid == ownerId)
            val cleanCaption = caption.trim().take(200)
            require(cleanCaption.isNotEmpty()) { "Story caption cannot be empty." }
            storyRef.set(
                mapOf(
                    "ownerId" to uid,
                    "ownerName" to ownerName.trim().ifBlank { "Mehfil User" }.take(60),
                    "caption" to cleanCaption,
                    "moodEmoji" to moodEmoji.trim().ifBlank { "✨" }.take(16),
                    "accentHex" to accentHex.trim().take(10).ifBlank { "#6E2CF2" },
                    "discoverable" to true,
                    "viewCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, storyRef.path)
            Result.failure(e)
        }
    }

    suspend fun incrementStoryView(storyId: String): Result<Unit> {
        val storyRef = db.collection("stories").document(storyId)
        return try {
            requireUserId()
            storyRef.update(
                mapOf(
                    "viewCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, storyRef.path)
            Result.failure(e)
        }
    }
}
