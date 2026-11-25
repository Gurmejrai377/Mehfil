package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatMessage
import com.example.data.MehfilRepository
import com.example.data.MessageType
import com.example.data.UiState
import com.example.data.unwrapFirestoreError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatDetailViewModel(
    private val repository: MehfilRepository,
    private val chatId: String,
    private val currentUserId: String
) : ViewModel() {

    val messagesState: StateFlow<UiState<List<ChatMessage>>> =
        repository.observeMessages(chatId, currentUserId)
            .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
            .catch { error ->
                val unwrapped = unwrapFirestoreError(error)
                if (unwrapped is CancellationException) throw unwrapped
                Log.w("ChatDetailVM", "Error observing messages", unwrapped)
                emit(UiState.Error(unwrapped.message ?: "Failed to load messages"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    fun sendText(senderName: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                senderId = currentUserId,
                senderName = senderName,
                messageType = MessageType.TEXT,
                text = text
            ).onFailure {
                _events.emit(it.localizedMessage ?: "Message failed to send")
            }
        }
    }

    fun sendVoiceNote(senderName: String, audioBase64: String?, durationSec: Long) {
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                senderId = currentUserId,
                senderName = senderName,
                messageType = MessageType.VOICE,
                text = "Voice note",
                mediaDurationSec = durationSec.coerceAtLeast(1L),
                audioDataBase64 = audioBase64
            ).onFailure {
                _events.emit(it.localizedMessage ?: "Voice note failed to send")
            }
        }
    }

    fun sendImage(senderName: String, base64: String, caption: String = "") {
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                senderId = currentUserId,
                senderName = senderName,
                messageType = MessageType.IMAGE,
                text = caption,
                imageDataBase64 = base64
            ).onFailure {
                _events.emit(it.localizedMessage ?: "Photo failed to send")
            }
        }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        viewModelScope.launch {
            repository.reactToMessage(chatId, messageId, emoji)
                .onFailure { _events.emit(it.localizedMessage ?: "Could not add reaction") }
        }
    }
}
