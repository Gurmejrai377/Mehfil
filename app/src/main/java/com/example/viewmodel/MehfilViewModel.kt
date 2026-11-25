package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CallQualityMode
import com.example.data.CallRecord
import com.example.data.CallStatus
import com.example.data.CallType
import com.example.data.Chat
import com.example.data.Contact
import com.example.data.MehfilRepository
import com.example.data.MessageType
import com.example.data.PartyRoom
import com.example.data.RoomType
import com.example.data.Story
import com.example.data.UiState
import com.example.data.UserAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionViewModel(
    private val repository: MehfilRepository
) : ViewModel() {

    val activeUser: StateFlow<UiState<UserAccount?>> = repository.observeActiveUser()
        .map<UserAccount?, UiState<UserAccount?>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.localizedMessage ?: "Failed to load session")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    fun loginAsGuest(displayName: String) {
        viewModelScope.launch {
            repository.loginAsGuest(displayName).onFailure {
                _events.emit(it.localizedMessage ?: "Could not start guest session")
            }
        }
    }

    fun loginWithPhoneOtp(phoneNumber: String, displayName: String) {
        viewModelScope.launch {
            repository.loginWithPhoneOtp(phoneNumber, displayName).onFailure {
                _events.emit(it.localizedMessage ?: "Could not sign in with phone")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
        }
    }
}

class MehfilViewModel(
    private val repository: MehfilRepository,
    val currentUserId: String
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.seedStarterRoomsIfEmpty()
        }
    }

    private fun <T> Flow<T>.toUiStateFlow(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                emit(UiState.Error(error.localizedMessage ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val userAccount: StateFlow<UiState<UserAccount?>> =
        repository.observeUserAccount(currentUserId).toUiStateFlow("account")

    val chatsState: StateFlow<UiState<List<Chat>>> =
        repository.observeChats(currentUserId).toUiStateFlow("chats")

    val contactsState: StateFlow<UiState<List<Contact>>> =
        repository.observeContacts(currentUserId).toUiStateFlow("contacts")

    val callsState: StateFlow<UiState<List<CallRecord>>> =
        repository.observeCalls(currentUserId).toUiStateFlow("calls")

    val storiesState: StateFlow<UiState<List<Story>>> =
        repository.observeStories().toUiStateFlow("stories")

    val partyRoomsState: StateFlow<UiState<List<PartyRoom>>> =
        repository.observePartyRooms().toUiStateFlow("rooms")

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    fun createPartyRoom(
        name: String,
        roomType: RoomType,
        topic: String,
        onRoomCreated: (roomId: String) -> Unit
    ) {
        val myName = (userAccount.value as? UiState.Success)?.data?.displayName ?: "Mehfil Host"
        viewModelScope.launch {
            repository.createPartyRoom(
                name = name,
                roomType = roomType,
                hostId = currentUserId,
                hostName = myName,
                topic = topic
            ).onSuccess { roomId ->
                onRoomCreated(roomId)
            }.onFailure {
                _events.emit(it.localizedMessage ?: "Could not create room")
            }
        }
    }

    fun updateProfileSettings(
        displayName: String,
        bio: String,
        online: Boolean,
        lowDataMode: Boolean
    ) {
        viewModelScope.launch {
            repository.updateUserSettings(currentUserId, displayName, bio, online, lowDataMode)
                .onSuccess { _events.emit("Profile updated") }
                .onFailure { _events.emit(it.localizedMessage ?: "Could not update profile") }
        }
    }

    fun addContact(
        peerId: String,
        displayName: String,
        mehfilId: String,
        statusText: String,
        avatarColorHex: String = "#6E2CF2"
    ) {
        viewModelScope.launch {
            repository.addContact(
                userId = currentUserId,
                peerId = peerId,
                displayName = displayName,
                mehfilId = mehfilId,
                statusText = statusText,
                avatarColorHex = avatarColorHex
            ).onSuccess {
                _events.emit("$displayName added to Mehfil contacts")
            }.onFailure {
                _events.emit(it.localizedMessage ?: "Could not add contact")
            }
        }
    }

    fun toggleFavorite(contact: Contact) {
        viewModelScope.launch {
            repository.toggleContactFavorite(contact.id, !contact.favorite)
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            repository.deleteContact(contactId)
                .onSuccess { _events.emit("Contact removed") }
        }
    }

    fun openDirectChat(
        peerId: String,
        peerName: String,
        onChatReady: (chatId: String) -> Unit
    ) {
        viewModelScope.launch {
            repository.getOrCreateDirectChat(currentUserId, peerId, peerName)
                .onSuccess { chatId -> onChatReady(chatId) }
                .onFailure { _events.emit(it.localizedMessage ?: "Could not open chat") }
        }
    }

    fun startOutgoingCall(
        peerId: String,
        peerName: String,
        callType: CallType,
        onCallStarted: (callId: String) -> Unit
    ) {
        val account = (userAccount.value as? UiState.Success)?.data
        val myName = account?.displayName ?: "Mehfil User"
        val quality = if (account?.lowDataMode == true) CallQualityMode.LOW_DATA else CallQualityMode.HD
        viewModelScope.launch {
            repository.startCall(
                callerId = currentUserId,
                callerName = myName,
                calleeId = peerId,
                calleeName = peerName,
                callType = callType,
                qualityMode = quality
            ).onSuccess { callId ->
                onCallStarted(callId)
            }.onFailure {
                _events.emit(it.localizedMessage ?: "Unable to place call")
            }
        }
    }

    fun finishCall(
        callId: String,
        peerId: String,
        peerName: String,
        callType: CallType,
        durationSeconds: Long,
        qualityMode: CallQualityMode
    ) {
        val myName = (userAccount.value as? UiState.Success)?.data?.displayName ?: "Mehfil User"
        viewModelScope.launch {
            repository.updateCall(callId, CallStatus.ENDED, durationSeconds, qualityMode)
            repository.getOrCreateDirectChat(currentUserId, peerId, peerName)
                .onSuccess { chatId ->
                    val label = if (callType == CallType.VIDEO) "HD Video call" else "Voice call"
                    val formatted = formatDurationShort(durationSeconds)
                    repository.sendMessage(
                        chatId = chatId,
                        senderId = currentUserId,
                        senderName = myName,
                        messageType = MessageType.CALL,
                        text = "$label • $formatted",
                        mediaDurationSec = durationSeconds
                    )
                }
        }
    }

    fun updateActiveCallQuality(callId: String, qualityMode: CallQualityMode) {
        viewModelScope.launch {
            repository.updateCall(callId, CallStatus.ACTIVE, 0L, qualityMode)
        }
    }

    fun postStory(caption: String, moodEmoji: String, accentHex: String) {
        val myName = (userAccount.value as? UiState.Success)?.data?.displayName ?: "Mehfil User"
        viewModelScope.launch {
            repository.postStory(currentUserId, myName, caption, moodEmoji, accentHex)
                .onSuccess { _events.emit("Story shared on Mehfil") }
                .onFailure { _events.emit(it.localizedMessage ?: "Could not post story") }
        }
    }

    fun markStoryViewed(story: Story) {
        viewModelScope.launch {
            repository.incrementStoryView(story.id)
        }
    }

    companion object {
        fun formatDurationShort(seconds: Long): String {
            val mins = seconds / 60
            val secs = seconds % 60
            return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
        }
    }
}
