package com.example.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MehfilRepository(private val dao: MehfilDao) {

    fun observeActiveUser(): Flow<UserAccount?> = dao.observeActiveUser()

    fun observeUserAccount(userId: String): Flow<UserAccount?> = dao.observeUserById(userId)

    fun observeContacts(userId: String): Flow<List<Contact>> = dao.observeContacts(userId)

    fun observeChats(userId: String): Flow<List<Chat>> = dao.observeChats(userId)

    fun observeMessages(chatId: String): Flow<List<ChatMessage>> = dao.observeMessages(chatId)

    fun observeCalls(userId: String): Flow<List<CallRecord>> = dao.observeCalls(userId)

    fun observeStories(): Flow<List<Story>> = dao.observeStories()

    // Party Rooms
    fun observePartyRooms(): Flow<List<PartyRoom>> = dao.observePartyRooms()

    fun observePartyRoom(roomId: String): Flow<PartyRoom?> = dao.observePartyRoom(roomId)

    fun observeRoomSeats(roomId: String): Flow<List<RoomSeat>> = dao.observeRoomSeats(roomId)

    fun observeRoomMessages(roomId: String): Flow<List<RoomMessage>> = dao.observeRoomMessages(roomId)

    fun observeRoomSongs(roomId: String): Flow<List<RoomSongRequest>> = dao.observeRoomSongs(roomId)

    suspend fun loginAsGuest(customName: String): Result<UserAccount> = try {
        val cleanName = customName.trim().ifBlank { "Mehfil Guest" }.take(60)
        val userId = "guest_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val mehfilId = "mehfil_${userId.takeLast(6)}"
        val now = System.currentTimeMillis()
        dao.clearActiveSessions()
        val account = UserAccount(
            userId = userId,
            displayName = cleanName,
            phoneNumber = null,
            loginMode = LoginMode.GUEST.name,
            mehfilId = mehfilId,
            bio = "Yahan har baat ek Mehfil hai",
            online = true,
            lowDataMode = false,
            isActiveSession = true,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertUser(account)
        seedStarterCircleIfEmpty(account)
        seedStarterRoomsIfEmpty()
        Result.success(account)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun loginWithPhoneOtp(phoneNumber: String, customName: String): Result<UserAccount> = try {
        val digits = phoneNumber.filter { it.isDigit() || it == '+' }.take(20)
        require(digits.count { it.isDigit() } >= 6) { "Enter a valid phone number." }
        val userId = "phone_${digits.filter { it.isDigit() }}"
        dao.clearActiveSessions()
        val existing = dao.getUserById(userId)
        val now = System.currentTimeMillis()
        val account = if (existing != null) {
            existing.copy(
                displayName = customName.trim().takeIf { it.isNotBlank() }?.take(60) ?: existing.displayName,
                online = true,
                isActiveSession = true,
                updatedAt = now
            )
        } else {
            val cleanName = customName.trim().ifBlank { "Mehfil Member" }.take(60)
            UserAccount(
                userId = userId,
                displayName = cleanName,
                phoneNumber = digits,
                loginMode = LoginMode.PHONE.name,
                mehfilId = "mehfil_${digits.takeLast(6)}",
                bio = "Yahan har baat ek Mehfil hai",
                online = true,
                lowDataMode = false,
                isActiveSession = true,
                createdAt = now,
                updatedAt = now
            )
        }
        dao.upsertUser(account)
        seedStarterCircleIfEmpty(account)
        seedStarterRoomsIfEmpty()
        Result.success(account)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signOut(): Result<Unit> = try {
        dao.clearActiveSessions()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun seedStarterCircleIfEmpty(user: UserAccount) {
        if (dao.getContactCount(user.userId) > 0) return
        val now = System.currentTimeMillis()
        val studioPeerId = "mehfil_studio"
        dao.upsertContact(
            Contact(
                id = "${user.userId}_$studioPeerId",
                userId = user.userId,
                peerId = studioPeerId,
                displayName = "Mehfil Studio",
                mehfilId = "mehfil_studio",
                statusText = "HD Video, Party Rooms & Audio Studio",
                avatarColorHex = "#6E2CF2",
                online = true,
                favorite = true,
                createdAt = now,
                updatedAt = now
            )
        )

        val chatId = "chat_${user.userId}_$studioPeerId"
        dao.upsertChat(
            Chat(
                id = chatId,
                ownerUserId = user.userId,
                peerId = studioPeerId,
                peerName = "Mehfil Studio",
                chatKind = ChatKind.DIRECT.name,
                lastMessageText = "Yahan har baat ek Mehfil hai — join a Party Room!",
                lastMessageType = MessageType.TEXT.name,
                lastMessageSenderId = studioPeerId,
                unreadCount = 0L,
                pinned = true,
                muted = false,
                createdAt = now,
                updatedAt = now
            )
        )

        dao.insertMessage(
            ChatMessage(
                id = "msg_welcome_${user.userId}",
                chatId = chatId,
                senderId = studioPeerId,
                senderName = "Mehfil Studio",
                messageType = MessageType.TEXT.name,
                text = "Welcome to Mehfil! Yahan har baat ek Mehfil hai. Explore the new Rooms tab for Music, Party, Voice Chat, and Live stages.",
                mediaDurationSec = 0L,
                createdAt = now,
                updatedAt = now
            )
        )

        dao.upsertStory(
            Story(
                id = "story_welcome_${user.userId}",
                ownerId = user.userId,
                ownerName = user.displayName,
                caption = "Yahan har baat ek Mehfil hai ✨",
                moodEmoji = "🎤",
                accentHex = "#6E2CF2",
                viewCount = 0L,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun seedStarterRoomsIfEmpty() {
        if (dao.getPartyRoomCount() > 0) return
        val now = System.currentTimeMillis()

        val starterRooms = listOf(
            PartyRoom(
                id = "room_music_mehfil",
                name = "Sufi & Bollywood DJ Night",
                roomType = RoomType.MUSIC.name,
                hostId = "host_dj_aarav",
                hostName = "DJ Aarav",
                topic = "Request tracks & sing together on stage",
                viewerCount = 342L,
                currentSongTitle = "Kesariya Royal Lo-Fi • DJ Aarav",
                isMusicPlaying = true,
                hostMicOn = true,
                totalGiftCoins = 4850L,
                createdAt = now - 4000L,
                updatedAt = now
            ),
            PartyRoom(
                id = "room_party_royal",
                name = "Royal Gold Mehfil Club",
                roomType = RoomType.PARTY.name,
                hostId = "host_zoya_vip",
                hostName = "Zoya Khan",
                topic = "8 mics open • VIP gifts & nonstop fun",
                viewerCount = 518L,
                currentSongTitle = "Mehfil Ambient Beat",
                isMusicPlaying = false,
                hostMicOn = true,
                totalGiftCoins = 9240L,
                createdAt = now - 3000L,
                updatedAt = now - 100L
            ),
            PartyRoom(
                id = "room_chat_shayari",
                name = "Late Night Chai & Shayari",
                roomType = RoomType.CHAT.name,
                hostId = "host_kabir",
                hostName = "Kabir Sharma",
                topic = "Voice chat only • Share stories & poetry",
                viewerCount = 164L,
                currentSongTitle = "",
                isMusicPlaying = false,
                hostMicOn = true,
                totalGiftCoins = 1520L,
                createdAt = now - 2000L,
                updatedAt = now - 200L
            ),
            PartyRoom(
                id = "room_live_mehfil",
                name = "Mehfil Live Spotlight Show",
                roomType = RoomType.LIVE.name,
                hostId = "host_ananya",
                hostName = "Ananya Verma",
                topic = "Host live broadcast • Ask anything!",
                viewerCount = 790L,
                currentSongTitle = "Spotlight Intro Theme",
                isMusicPlaying = true,
                hostMicOn = true,
                totalGiftCoins = 12400L,
                createdAt = now - 1000L,
                updatedAt = now - 300L
            )
        )

        val coHostSamples = listOf(
            Triple("peer_rohan", "Rohan", "#6E2CF2"),
            Triple("peer_meera", "Meera", "#9D4EDD"),
            Triple("peer_sana", "Sana", "#D4AF37"),
            Triple("peer_aryan", "Aryan", "#5B1FA6")
        )

        starterRooms.forEach { room ->
            dao.upsertPartyRoom(room)
            val seats = (0 until 8).map { idx ->
                val sample = if (idx < 3) coHostSamples[idx] else null
                RoomSeat(
                    id = "${room.id}_seat_$idx",
                    roomId = room.id,
                    seatIndex = idx,
                    occupantId = sample?.first,
                    occupantName = sample?.second,
                    avatarColorHex = sample?.third ?: "#6E2CF2",
                    isMuted = idx == 2,
                    giftCoins = if (sample != null) (idx + 1) * 120L else 0L,
                    lastReaction = if (idx == 0) "🎤" else null,
                    updatedAt = now
                )
            }
            dao.upsertRoomSeats(seats)

            dao.insertRoomMessage(
                RoomMessage(
                    id = "rmsg_welcome_${room.id}",
                    roomId = room.id,
                    senderId = room.hostId,
                    senderName = room.hostName,
                    kind = RoomMessageKind.SYSTEM.name,
                    text = "Welcome to ${room.name}! Tap an empty mic seat to join the circle.",
                    createdAt = now - 5000L
                )
            )
            dao.insertRoomMessage(
                RoomMessage(
                    id = "rmsg_chat1_${room.id}",
                    roomId = room.id,
                    senderId = "peer_rohan",
                    senderName = "Rohan",
                    kind = RoomMessageKind.TEXT.name,
                    text = "Yahan har baat ek Mehfil hai! Loving the vibe tonight.",
                    createdAt = now - 2500L
                )
            )

            if (room.roomType == RoomType.MUSIC.name || room.roomType == RoomType.LIVE.name) {
                dao.upsertRoomSong(
                    RoomSongRequest(
                        id = "song_1_${room.id}",
                        roomId = room.id,
                        title = "Kesariya Royal Lo-Fi",
                        artist = "DJ Aarav",
                        requestedBy = room.hostName,
                        isPlaying = true,
                        createdAt = now - 4000L
                    )
                )
                dao.upsertRoomSong(
                    RoomSongRequest(
                        id = "song_2_${room.id}",
                        roomId = room.id,
                        title = "Tere Bina Mehfil",
                        artist = "Mehfil Acoustic",
                        requestedBy = "Meera",
                        isPlaying = false,
                        createdAt = now - 3000L
                    )
                )
                dao.upsertRoomSong(
                    RoomSongRequest(
                        id = "song_3_${room.id}",
                        roomId = room.id,
                        title = "Raataan Lambiyan Remix",
                        artist = "Club Mehfil",
                        requestedBy = "Rohan",
                        isPlaying = false,
                        createdAt = now - 2000L
                    )
                )
            }
        }
    }

    suspend fun createPartyRoom(
        name: String,
        roomType: RoomType,
        hostId: String,
        hostName: String,
        topic: String
    ): Result<String> = try {
        val cleanName = name.trim().take(60)
        require(cleanName.isNotEmpty()) { "Room name cannot be empty." }
        val now = System.currentTimeMillis()
        val roomId = "room_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val defaultSong = if (roomType == RoomType.MUSIC) "Mehfil DJ Opening Mix • $hostName" else ""
        val room = PartyRoom(
            id = roomId,
            name = cleanName,
            roomType = roomType.name,
            hostId = hostId,
            hostName = hostName.ifBlank { "Mehfil Host" },
            topic = topic.trim().ifBlank { roomType.description }.take(120),
            viewerCount = 1L,
            currentSongTitle = defaultSong,
            isMusicPlaying = roomType == RoomType.MUSIC,
            hostMicOn = true,
            totalGiftCoins = 0L,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertPartyRoom(room)

        val seats = (0 until 8).map { idx ->
            RoomSeat(
                id = "${roomId}_seat_$idx",
                roomId = roomId,
                seatIndex = idx,
                occupantId = null,
                occupantName = null,
                avatarColorHex = "#6E2CF2",
                isMuted = false,
                giftCoins = 0L,
                updatedAt = now
            )
        }
        dao.upsertRoomSeats(seats)

        dao.insertRoomMessage(
            RoomMessage(
                id = "rmsg_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                roomId = roomId,
                senderId = hostId,
                senderName = hostName,
                kind = RoomMessageKind.SYSTEM.name,
                text = "$hostName started a ${roomType.displayTitle}! Tap any seat to take the mic.",
                createdAt = now
            )
        )

        if (roomType == RoomType.MUSIC) {
            dao.upsertRoomSong(
                RoomSongRequest(
                    id = "song_init_$roomId",
                    roomId = roomId,
                    title = "Mehfil DJ Opening Mix",
                    artist = hostName,
                    requestedBy = hostName,
                    isPlaying = true,
                    createdAt = now
                )
            )
        }
        Result.success(roomId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun joinOrTapSeat(
        roomId: String,
        seatIndex: Int,
        userId: String,
        userName: String
    ): Result<String> = try {
        val now = System.currentTimeMillis()
        val seats = dao.getRoomSeats(roomId)
        val target = seats.firstOrNull { it.seatIndex == seatIndex }
            ?: error("Seat not found")

        return when {
            target.occupantId == userId -> {
                dao.upsertRoomSeat(
                    target.copy(
                        occupantId = null,
                        occupantName = null,
                        isMuted = false,
                        lastReaction = null,
                        updatedAt = now
                    )
                )
                Result.success("Stepped down from Mic ${seatIndex + 1}")
            }
            target.occupantId == null -> {
                dao.vacateUserSeats(roomId, userId, now)
                dao.upsertRoomSeat(
                    target.copy(
                        occupantId = userId,
                        occupantName = userName.ifBlank { "Mehfil Guest" },
                        avatarColorHex = "#6E2CF2",
                        isMuted = false,
                        updatedAt = now
                    )
                )
                dao.insertRoomMessage(
                    RoomMessage(
                        id = "rmsg_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                        roomId = roomId,
                        senderId = userId,
                        senderName = userName,
                        kind = RoomMessageKind.SYSTEM.name,
                        text = "$userName took Mic Seat #${seatIndex + 1}!",
                        createdAt = now
                    )
                )
                Result.success("On Mic Seat #${seatIndex + 1}")
            }
            else -> Result.success("Seat #${seatIndex + 1} is occupied by ${target.occupantName}")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun toggleUserMicInRoom(
        roomId: String,
        userId: String,
        userName: String
    ): Result<Boolean> = try {
        val now = System.currentTimeMillis()
        val room = dao.getPartyRoom(roomId) ?: error("Room not found")
        if (room.hostId == userId) {
            val next = !room.hostMicOn
            dao.updateRoomHostMic(roomId, next, now)
            return Result.success(next)
        }
        val seats = dao.getRoomSeats(roomId)
        val mySeat = seats.firstOrNull { it.occupantId == userId }
        if (mySeat != null) {
            val nextMicOn = mySeat.isMuted
            dao.upsertRoomSeat(mySeat.copy(isMuted = !nextMicOn, updatedAt = now))
            Result.success(nextMicOn)
        } else {
            val freeSeat = seats.firstOrNull { it.occupantId == null }
            if (freeSeat != null) {
                dao.upsertRoomSeat(
                    freeSeat.copy(
                        occupantId = userId,
                        occupantName = userName,
                        isMuted = false,
                        updatedAt = now
                    )
                )
                dao.insertRoomMessage(
                    RoomMessage(
                        id = "rmsg_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                        roomId = roomId,
                        senderId = userId,
                        senderName = userName,
                        kind = RoomMessageKind.SYSTEM.name,
                        text = "$userName hopped onto Mic #${freeSeat.seatIndex + 1}!",
                        createdAt = now
                    )
                )
                Result.success(true)
            } else {
                error("All 8 mic seats are currently full")
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun toggleRoomMusic(roomId: String): Result<Boolean> = try {
        val room = dao.getPartyRoom(roomId) ?: error("Room not found")
        val nextPlaying = !room.isMusicPlaying
        val song = room.currentSongTitle.ifBlank { "Mehfil DJ Lounge Beat" }
        dao.updateRoomMusic(roomId, nextPlaying, song, System.currentTimeMillis())
        Result.success(nextPlaying)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun requestSongInRoom(
        roomId: String,
        title: String,
        artist: String,
        requesterName: String,
        requesterId: String,
        playImmediately: Boolean = false
    ): Result<Unit> = try {
        val cleanTitle = title.trim().take(80)
        require(cleanTitle.isNotEmpty()) { "Song title cannot be empty." }
        val cleanArtist = artist.trim().ifBlank { "Mehfil Request" }.take(60)
        val now = System.currentTimeMillis()
        val songId = "song_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        dao.upsertRoomSong(
            RoomSongRequest(
                id = songId,
                roomId = roomId,
                title = cleanTitle,
                artist = cleanArtist,
                requestedBy = requesterName,
                isPlaying = playImmediately,
                createdAt = now
            )
        )
        if (playImmediately) {
            dao.setRoomPlayingSong(roomId, songId)
            dao.updateRoomMusic(roomId, true, "$cleanTitle • $cleanArtist", now)
        }
        dao.insertRoomMessage(
            RoomMessage(
                id = "rmsg_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                roomId = roomId,
                senderId = requesterId,
                senderName = requesterName,
                kind = RoomMessageKind.SONG_REQUEST.name,
                text = "🎵 Requested \"$cleanTitle\" by $cleanArtist",
                createdAt = now
            )
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun playQueuedSong(roomId: String, song: RoomSongRequest): Result<Unit> = try {
        val now = System.currentTimeMillis()
        dao.setRoomPlayingSong(roomId, song.id)
        dao.updateRoomMusic(roomId, true, "${song.title} • ${song.artist}", now)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
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
    ): Result<Unit> = try {
        val now = System.currentTimeMillis()
        dao.addRoomGiftCoins(roomId, coins, now)
        if (recipientSeatId != null) {
            dao.addSeatGiftCoins(recipientSeatId, coins, now)
        }
        dao.insertRoomMessage(
            RoomMessage(
                id = "rmsg_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                roomId = roomId,
                senderId = senderId,
                senderName = senderName,
                kind = RoomMessageKind.GIFT.name,
                text = "$giftEmoji Sent $giftName ($coins 💎) to $recipientLabel!",
                createdAt = now
            )
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun sendRoomReaction(
        roomId: String,
        senderId: String,
        senderName: String,
        emoji: String
    ): Result<Unit> = try {
        val now = System.currentTimeMillis()
        dao.updateUserSeatReaction(roomId, senderId, emoji, now)
        dao.insertRoomMessage(
            RoomMessage(
                id = "rmsg_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                roomId = roomId,
                senderId = senderId,
                senderName = senderName,
                kind = RoomMessageKind.REACTION.name,
                text = "$emoji reacted",
                createdAt = now
            )
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun sendRoomChatMessage(
        roomId: String,
        senderId: String,
        senderName: String,
        text: String
    ): Result<Unit> = try {
        val clean = text.trim().take(300)
        require(clean.isNotEmpty()) { "Message cannot be empty." }
        dao.insertRoomMessage(
            RoomMessage(
                id = "rmsg_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                roomId = roomId,
                senderId = senderId,
                senderName = senderName,
                kind = RoomMessageKind.TEXT.name,
                text = clean,
                createdAt = System.currentTimeMillis()
            )
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateUserSettings(
        userId: String,
        displayName: String,
        bio: String,
        online: Boolean,
        lowDataMode: Boolean
    ): Result<Unit> = try {
        val current = dao.getUserById(userId) ?: error("User session not found")
        val cleanName = displayName.trim().take(60)
        require(cleanName.isNotEmpty()) { "Display name cannot be empty." }
        dao.upsertUser(
            current.copy(
                displayName = cleanName,
                bio = bio.trim().take(160),
                online = online,
                lowDataMode = lowDataMode,
                updatedAt = System.currentTimeMillis()
            )
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun addContact(
        userId: String,
        peerId: String,
        displayName: String,
        mehfilId: String,
        statusText: String,
        avatarColorHex: String = "#6E2CF2"
    ): Result<Unit> = try {
        val cleanName = displayName.trim().take(60)
        require(cleanName.isNotEmpty()) { "Contact name cannot be empty." }
        val now = System.currentTimeMillis()
        dao.upsertContact(
            Contact(
                id = "${userId}_$peerId",
                userId = userId,
                peerId = peerId,
                displayName = cleanName,
                mehfilId = mehfilId.trim().ifBlank { "mehfil_friend" }.take(40),
                statusText = statusText.trim().ifBlank { "On Mehfil" }.take(120),
                avatarColorHex = avatarColorHex,
                online = true,
                favorite = false,
                createdAt = now,
                updatedAt = now
            )
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun toggleContactFavorite(contactId: String, favorite: Boolean): Result<Unit> = try {
        dao.updateContactFavorite(contactId, favorite, System.currentTimeMillis())
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteContact(contactId: String): Result<Unit> = try {
        dao.deleteContact(contactId)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getOrCreateDirectChat(
        currentUserId: String,
        peerId: String,
        peerName: String
    ): Result<String> = try {
        val chatId = "chat_${currentUserId}_$peerId"
        if (dao.getChatById(chatId) == null) {
            val now = System.currentTimeMillis()
            dao.upsertChat(
                Chat(
                    id = chatId,
                    ownerUserId = currentUserId,
                    peerId = peerId,
                    peerName = peerName.take(60),
                    chatKind = ChatKind.DIRECT.name,
                    lastMessageText = "Say salam on Mehfil!",
                    lastMessageType = MessageType.TEXT.name,
                    lastMessageSenderId = currentUserId,
                    unreadCount = 0L,
                    pinned = false,
                    muted = false,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
        Result.success(chatId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        messageType: MessageType,
        text: String,
        mediaDurationSec: Long = 0L,
        imageDataBase64: String? = null
    ): Result<Unit> = try {
        val now = System.currentTimeMillis()
        val cleanText = text.trim().take(1000)
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        dao.insertMessage(
            ChatMessage(
                id = msgId,
                chatId = chatId,
                senderId = senderId,
                senderName = senderName.ifBlank { "Mehfil User" }.take(60),
                messageType = messageType.name,
                text = cleanText,
                mediaDurationSec = mediaDurationSec.coerceIn(0L, 600L),
                imageDataBase64 = imageDataBase64,
                createdAt = now,
                updatedAt = now
            )
        )
        val preview = when (messageType) {
            MessageType.VOICE -> "🎤 Voice note (${mediaDurationSec}s)"
            MessageType.IMAGE -> "📷 Photo${if (cleanText.isNotBlank()) ": $cleanText" else ""}"
            MessageType.CALL -> cleanText
            MessageType.TEXT -> cleanText
        }.take(500)
        dao.updateChatPreview(chatId, preview, messageType.name, senderId, now)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun reactToMessage(messageId: String, reaction: String): Result<Unit> = try {
        dao.updateMessageReaction(messageId, reaction, System.currentTimeMillis())
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun startCall(
        callerId: String,
        callerName: String,
        calleeId: String,
        calleeName: String,
        callType: CallType,
        qualityMode: CallQualityMode
    ): Result<String> = try {
        val now = System.currentTimeMillis()
        val callId = "call_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        dao.upsertCall(
            CallRecord(
                id = callId,
                ownerUserId = callerId,
                callerId = callerId,
                callerName = callerName.ifBlank { "Mehfil User" },
                calleeId = calleeId,
                calleeName = calleeName.ifBlank { "Mehfil Contact" },
                callType = callType.name,
                status = CallStatus.ACTIVE.name,
                qualityMode = qualityMode.name,
                durationSeconds = 0L,
                createdAt = now,
                updatedAt = now
            )
        )
        Result.success(callId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateCall(
        callId: String,
        status: CallStatus,
        durationSeconds: Long,
        qualityMode: CallQualityMode
    ): Result<Unit> = try {
        dao.updateCall(callId, status.name, durationSeconds.coerceIn(0L, 86400L), qualityMode.name, System.currentTimeMillis())
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun postStory(
        ownerId: String,
        ownerName: String,
        caption: String,
        moodEmoji: String,
        accentHex: String
    ): Result<Unit> = try {
        val cleanCaption = caption.trim().take(200)
        require(cleanCaption.isNotEmpty()) { "Story caption cannot be empty." }
        val now = System.currentTimeMillis()
        val storyId = "story_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        dao.upsertStory(
            Story(
                id = storyId,
                ownerId = ownerId,
                ownerName = ownerName.ifBlank { "Mehfil User" },
                caption = cleanCaption,
                moodEmoji = moodEmoji.ifBlank { "✨" },
                accentHex = accentHex,
                viewCount = 0L,
                createdAt = now,
                updatedAt = now
            )
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun incrementStoryView(storyId: String): Result<Unit> = try {
        dao.incrementStoryView(storyId, System.currentTimeMillis())
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
