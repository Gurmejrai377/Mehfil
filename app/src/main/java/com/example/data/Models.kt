package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
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

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val mehfilId: String = "",
    val bio: String = "Yahan har baat ek Mehfil hai",
    val online: Boolean = true,
    val lowDataMode: Boolean = false,
    val coinBalance: Long = 5000L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val mehfilId: String = "",
    val statusText: String = "On Mehfil",
    val avatarColorHex: String = "#6E2CF2",
    val online: Boolean = true,
    val discoverable: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Contact(
    @DocumentId val id: String = "",
    val userId: String = "",
    val peerId: String = "",
    val displayName: String = "",
    val mehfilId: String = "",
    val statusText: String = "",
    val avatarColorHex: String = "#6E2CF2",
    val online: Boolean = true,
    val favorite: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Chat(
    @DocumentId val id: String = "",
    val participantUids: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val chatKind: String = ChatKind.DIRECT.name,
    val title: String = "",
    val lastMessageText: String = "",
    val lastMessageType: String = MessageType.TEXT.name,
    val lastMessageSenderId: String = "",
    val unreadCount: Long = 0L,
    val pinned: Boolean = false,
    val muted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedPeerId(currentUserId: String): String =
        participantUids.firstOrNull { it != currentUserId }
            ?: participantUids.firstOrNull().orEmpty()

    fun resolvedPeerName(currentUserId: String): String {
        val pid = resolvedPeerId(currentUserId)
        return participantNames[pid]?.takeIf { it.isNotBlank() }
            ?: title.ifBlank { "Mehfil Friend" }
    }
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val participantUids: List<String> = emptyList(),
    val messageType: String = MessageType.TEXT.name,
    val text: String = "",
    val mediaDurationSec: Long = 0L,
    val imageDataBase64: String? = null,
    val audioDataBase64: String? = null,
    val reaction: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CallRecord(
    @DocumentId val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val calleeId: String = "",
    val calleeName: String = "",
    val participantUids: List<String> = emptyList(),
    val callType: String = CallType.VIDEO.name,
    val status: String = CallStatus.RINGING.name,
    val qualityMode: String = CallQualityMode.HD.name,
    val durationSeconds: Long = 0L,
    val offerSdp: String? = null,
    val answerSdp: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CallCandidate(
    @DocumentId val id: String = "",
    val callId: String = "",
    val senderId: String = "",
    val participantUids: List<String> = emptyList(),
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val sdpCandidate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Story(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val caption: String = "",
    val moodEmoji: String = "✨",
    val accentHex: String = "#6E2CF2",
    val discoverable: Boolean = true,
    val viewCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PartyRoom(
    @DocumentId val id: String = "",
    val name: String = "",
    val roomType: String = RoomType.PARTY.name,
    val hostId: String = "",
    val hostName: String = "",
    val topic: String = "",
    val publicRoom: Boolean = true,
    val activeMemberUids: List<String> = emptyList(),
    val viewerCount: Long = 1L,
    val currentSongTitle: String = "",
    val musicPlaying: Boolean = false,
    val hostMicOn: Boolean = true,
    val totalGiftCoins: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomSeat(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val seatIndex: Int = 0,
    val publicRoom: Boolean = true,
    val occupantId: String? = null,
    val occupantName: String? = null,
    val avatarColorHex: String = "#6E2CF2",
    val muted: Boolean = false,
    val giftCoins: Long = 0L,
    val lastReaction: String? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomMessage(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val kind: String = RoomMessageKind.TEXT.name,
    val text: String = "",
    val publicRoom: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomSongRequest(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val title: String = "",
    val artist: String = "",
    val requestedBy: String = "",
    val requestedByUid: String = "",
    val playing: Boolean = false,
    val publicRoom: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomSignal(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val participantUids: List<String> = emptyList(),
    val signalType: String = "OFFER",
    val payload: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
