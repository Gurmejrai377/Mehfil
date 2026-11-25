package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MehfilRepository
import com.example.data.PartyRoom
import com.example.data.RoomMessage
import com.example.data.RoomSeat
import com.example.data.RoomSongRequest
import com.example.data.UiState
import com.example.data.unwrapFirestoreError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GiftBurst(
    val id: Long = System.currentTimeMillis(),
    val emoji: String,
    val title: String,
    val senderName: String
)

class PartyRoomViewModel(
    val repository: MehfilRepository,
    val roomId: String,
    val currentUserId: String,
    val currentUserName: String
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.joinRoomPresence(roomId, currentUserId)
        }
    }

    val roomState: StateFlow<UiState<PartyRoom?>> = repository.observePartyRoom(roomId)
        .map<PartyRoom?, UiState<PartyRoom?>> { UiState.Success(it) }
        .catch { error ->
            val unwrapped = unwrapFirestoreError(error)
            if (unwrapped is CancellationException) throw unwrapped
            Log.w("PartyRoomVM", "Error observing room", unwrapped)
            emit(UiState.Error(unwrapped.message ?: "Failed to load room"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val seatsState: StateFlow<List<RoomSeat>> = repository.observeRoomSeats(roomId)
        .catch { error ->
            val unwrapped = unwrapFirestoreError(error)
            if (unwrapped is CancellationException) throw unwrapped
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val messagesState: StateFlow<List<RoomMessage>> = repository.observeRoomMessages(roomId)
        .catch { error ->
            val unwrapped = unwrapFirestoreError(error)
            if (unwrapped is CancellationException) throw unwrapped
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val songsState: StateFlow<List<RoomSongRequest>> = repository.observeRoomSongs(roomId)
        .catch { error ->
            val unwrapped = unwrapFirestoreError(error)
            if (unwrapped is CancellationException) throw unwrapped
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _giftBursts = MutableSharedFlow<GiftBurst>(extraBufferCapacity = 8)
    val giftBursts = _giftBursts.asSharedFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    fun leaveRoom(onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                repository.leaveRoomPresence(roomId, currentUserId)
            }
            onDone()
        }
    }

    fun tapSeat(seatIndex: Int) {
        viewModelScope.launch {
            repository.joinOrTapSeat(roomId, seatIndex, currentUserId, currentUserName)
                .onSuccess { msg -> _events.emit(msg) }
                .onFailure { _events.emit(it.localizedMessage ?: "Could not update mic seat") }
        }
    }

    fun toggleMic() {
        viewModelScope.launch {
            repository.toggleUserMicInRoom(roomId, currentUserId, currentUserName)
                .onSuccess { micOn ->
                    _events.emit(if (micOn) "Mic is LIVE" else "Mic muted")
                }
                .onFailure { _events.emit(it.localizedMessage ?: "Could not toggle mic") }
        }
    }

    fun toggleMusic() {
        viewModelScope.launch {
            repository.toggleRoomMusic(roomId)
                .onSuccess { playing ->
                    _events.emit(if (playing) "DJ Music playing" else "Music paused")
                }
                .onFailure { _events.emit(it.localizedMessage ?: "Could not toggle music") }
        }
    }

    fun requestSong(title: String, artist: String, playNow: Boolean = false) {
        viewModelScope.launch {
            repository.requestSongInRoom(
                roomId = roomId,
                title = title,
                artist = artist,
                requesterName = currentUserName,
                requesterId = currentUserId,
                playImmediately = playNow
            ).onSuccess {
                _events.emit("Added \"$title\" to Music Queue")
            }.onFailure {
                _events.emit(it.localizedMessage ?: "Could not request song")
            }
        }
    }

    fun playSong(song: RoomSongRequest) {
        viewModelScope.launch {
            repository.playQueuedSong(roomId, song)
        }
    }

    fun sendGift(
        emoji: String,
        giftName: String,
        coins: Long,
        recipientLabel: String,
        recipientSeatId: String? = null
    ) {
        viewModelScope.launch {
            repository.sendRoomGift(
                roomId = roomId,
                senderId = currentUserId,
                senderName = currentUserName,
                giftEmoji = emoji,
                giftName = giftName,
                coins = coins,
                recipientLabel = recipientLabel,
                recipientSeatId = recipientSeatId
            ).onSuccess {
                _giftBursts.emit(
                    GiftBurst(
                        emoji = emoji,
                        title = "$giftName ($coins 💎)",
                        senderName = currentUserName
                    )
                )
            }.onFailure {
                _events.emit(it.localizedMessage ?: "Gift failed")
            }
        }
    }

    fun sendReaction(emoji: String) {
        viewModelScope.launch {
            repository.sendRoomReaction(roomId, currentUserId, currentUserName, emoji)
            _giftBursts.emit(GiftBurst(emoji = emoji, title = "Reaction", senderName = currentUserName))
        }
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendRoomChatMessage(roomId, currentUserId, currentUserName, text)
        }
    }
}
