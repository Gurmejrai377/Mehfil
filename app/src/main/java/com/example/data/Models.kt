package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class LoginMode {
    GUEST,
    PHONE;

    companion object {
        fun fromRaw(raw: String?): LoginMode =
            entries.firstOrNull { it.name == raw } ?: GUEST
    }
}

enum class ChatKind {
    DIRECT,
    GROUP;

    companion object {
        fun fromRaw(raw: String?): ChatKind =
            entries.firstOrNull { it.name == raw } ?: DIRECT
    }
}

enum class MessageType {
    TEXT,
    VOICE,
    IMAGE,
    CALL;

    companion object {
        fun fromRaw(raw: String?): MessageType =
            entries.firstOrNull { it.name == raw } ?: TEXT
    }
}

enum class CallType {
    VIDEO,
    VOICE;

    companion object {
        fun fromRaw(raw: String?): CallType =
            entries.firstOrNull { it.name == raw } ?: VIDEO
    }
}

enum class CallStatus {
    RINGING,
    ACTIVE,
    ENDED,
    MISSED,
    DECLINED;

    companion object {
        fun fromRaw(raw: String?): CallStatus =
            entries.firstOrNull { it.name == raw } ?: ENDED
    }
}

enum class CallQualityMode {
    HD,
    LOW_DATA;

    companion object {
        fun fromRaw(raw: String?): CallQualityMode =
            entries.firstOrNull { it.name == raw } ?: HD
    }
}

enum class RoomType(
    val displayTitle: String,
    val shortBadge: String,
    val description: String,
    val emoji: String
) {
    MUSIC(
        displayTitle = "Music Room",
        shortBadge = "Music",
        description = "DJ music play, request songs & sing together",
        emoji = "🎵"
    ),
    PARTY(
        displayTitle = "Party Room",
        shortBadge = "Party",
        description = "8 mic seats on stage, send gifts & reactions",
        emoji = "👑"
    ),
    CHAT(
        displayTitle = "Chat Room",
        shortBadge = "Voice Chat",
        description = "Voice chat only lounge with friends",
        emoji = "🎙️"
    ),
    LIVE(
        displayTitle = "Live Room",
        shortBadge = "Live",
        description = "Host goes live while viewers join & cheer",
        emoji = "✨"
    );

    companion object {
        fun fromRaw(raw: String?): RoomType =
            entries.firstOrNull { it.name == raw } ?: PARTY
    }
}

enum class RoomMessageKind {
    TEXT,
    GIFT,
    SONG_REQUEST,
    REACTION,
    SYSTEM;

    companion object {
        fun fromRaw(raw: String?): RoomMessageKind =
            entries.firstOrNull { it.name == raw } ?: TEXT
    }
}

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val userId: String,
    val displayName: String,
    val phoneNumber: String? = null,
    val loginMode: String = LoginMode.GUEST.name,
    val mehfilId: String,
    val bio: String,
    val online: Boolean = true,
    val lowDataMode: Boolean = false,
    val isActiveSession: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey val id: String,
    val userId: String,
    val peerId: String,
    val displayName: String,
    val mehfilId: String,
    val statusText: String,
    val avatarColorHex: String = "#6E2CF2",
    val online: Boolean = true,
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey val id: String,
    val ownerUserId: String,
    val peerId: String,
    val peerName: String,
    val chatKind: String = ChatKind.DIRECT.name,
    val lastMessageText: String = "",
    val lastMessageType: String = MessageType.TEXT.name,
    val lastMessageSenderId: String = "",
    val unreadCount: Long = 0L,
    val pinned: Boolean = false,
    val muted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val messageType: String = MessageType.TEXT.name,
    val text: String = "",
    val mediaDurationSec: Long = 0L,
    val imageDataBase64: String? = null,
    val reaction: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "call_records")
data class CallRecord(
    @PrimaryKey val id: String,
    val ownerUserId: String,
    val callerId: String,
    val callerName: String,
    val calleeId: String,
    val calleeName: String,
    val callType: String = CallType.VIDEO.name,
    val status: String = CallStatus.ENDED.name,
    val qualityMode: String = CallQualityMode.HD.name,
    val durationSeconds: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stories")
data class Story(
    @PrimaryKey val id: String,
    val ownerId: String,
    val ownerName: String,
    val caption: String,
    val moodEmoji: String = "✨",
    val accentHex: String = "#6E2CF2",
    val viewCount: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "party_rooms")
data class PartyRoom(
    @PrimaryKey val id: String,
    val name: String,
    val roomType: String = RoomType.PARTY.name,
    val hostId: String,
    val hostName: String,
    val topic: String,
    val viewerCount: Long = 1L,
    val currentSongTitle: String = "",
    val isMusicPlaying: Boolean = false,
    val hostMicOn: Boolean = true,
    val totalGiftCoins: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "room_seats")
data class RoomSeat(
    @PrimaryKey val id: String,
    val roomId: String,
    val seatIndex: Int,
    val occupantId: String? = null,
    val occupantName: String? = null,
    val avatarColorHex: String = "#6E2CF2",
    val isMuted: Boolean = false,
    val giftCoins: Long = 0L,
    val lastReaction: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "room_messages")
data class RoomMessage(
    @PrimaryKey val id: String,
    val roomId: String,
    val senderId: String,
    val senderName: String,
    val kind: String = RoomMessageKind.TEXT.name,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "room_songs")
data class RoomSongRequest(
    @PrimaryKey val id: String,
    val roomId: String,
    val title: String,
    val artist: String,
    val requestedBy: String,
    val isPlaying: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
