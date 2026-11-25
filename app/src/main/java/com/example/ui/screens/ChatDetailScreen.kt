package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CallType
import com.example.data.ChatMessage
import com.example.data.MessageType
import com.example.data.UiState
import com.example.media.AudioLevelMonitor
import com.example.media.ImageUtils
import com.example.ui.components.MehfilAvatar
import com.example.ui.theme.MehfilCoral
import com.example.ui.theme.MehfilDeepPurple
import com.example.ui.theme.MehfilGold
import com.example.ui.theme.MehfilMint
import com.example.ui.theme.MehfilPurple
import com.example.viewmodel.ChatDetailViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    viewModel: ChatDetailViewModel,
    currentUserId: String,
    currentUserName: String,
    peerId: String,
    peerName: String,
    onBack: () -> Unit,
    onStartCall: (CallType) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    var draftText by remember { mutableStateOf("") }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableLongStateOf(0L) }
    var selectedMessageForReaction by remember { mutableStateOf<ChatMessage?>(null) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            isRecordingVoice = true
            recordingSeconds = 0L
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Microphone permission is required for voice notes.")
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val base64 = ImageUtils.encodeUriToCompactBase64(context, uri)
                if (base64 != null) {
                    viewModel.sendImage(currentUserName, base64)
                } else {
                    snackbarHostState.showSnackbar("Unable to attach selected photo.")
                }
            }
        }
    }

    val liveMicLevel by AudioLevelMonitor
        .observeMicLevel(hasPermission = hasMicPermission && isRecordingVoice, isMuted = !isRecordingVoice)
        .collectAsStateWithLifecycle(initialValue = 0f)

    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            while (true) {
                delay(1000L)
                recordingSeconds += 1L
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val messageCount = (messagesState as? UiState.Success)?.data?.size ?: 0
    LaunchedEffect(messageCount) {
        if (messageCount > 0) {
            listState.animateScrollToItem(messageCount - 1)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to chats"
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MehfilAvatar(
                            name = peerName,
                            accentHex = if (peerId == "mehfil_studio") "#5B1FA6" else "#7B3FE4",
                            size = 40.dp,
                            online = true
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = peerName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Online • Mehfil HD",
                                style = MaterialTheme.typography.labelSmall,
                                color = MehfilMint
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onStartCall(CallType.VOICE) },
                        modifier = Modifier.testTag("detail_voice_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = "Voice call $peerName",
                            tint = MehfilMint
                        )
                    }
                    IconButton(
                        onClick = { onStartCall(CallType.VIDEO) },
                        modifier = Modifier.testTag("detail_video_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Videocam,
                            contentDescription = "Video call $peerName",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Column {
                    if (selectedMessageForReaction != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                listOf("❤️", "👍", "😂", "😮", "🔥").forEach { emoji ->
                                    Text(
                                        text = emoji,
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.clickable {
                                            selectedMessageForReaction?.let {
                                                viewModel.reactToMessage(it.id, emoji)
                                            }
                                            selectedMessageForReaction = null
                                        }
                                    )
                                }
                            }
                            IconButton(onClick = { selectedMessageForReaction = null }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close reactions")
                            }
                        }
                    }

                    if (isRecordingVoice) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    isRecordingVoice = false
                                    recordingSeconds = 0L
                                },
                                modifier = Modifier.testTag("cancel_voice_note_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Cancel voice note",
                                    tint = MehfilCoral
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MehfilCoral.copy(alpha = 0.12f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size((10 + (liveMicLevel * 8)).dp)
                                            .clip(CircleShape)
                                            .background(MehfilCoral)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Recording voice note • ${recordingSeconds}s",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MehfilCoral
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    val duration = recordingSeconds.coerceAtLeast(1L)
                                    isRecordingVoice = false
                                    recordingSeconds = 0L
                                    viewModel.sendVoiceNote(currentUserName, duration)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MehfilPurple)
                                    .testTag("send_voice_note_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Stop,
                                    contentDescription = "Send voice note",
                                    tint = Color.White
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("pick_photo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoLibrary,
                                    contentDescription = "Send photo",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            OutlinedTextField(
                                value = draftText,
                                onValueChange = { if (it.length <= 1000) draftText = it },
                                placeholder = { Text("Message $peerName...") },
                                maxLines = 4,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("message_input")
                            )

                            Spacer(modifier = Modifier.width(6.dp))
                            if (draftText.isBlank()) {
                                IconButton(
                                    onClick = {
                                        if (hasMicPermission) {
                                            isRecordingVoice = true
                                            recordingSeconds = 0L
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .testTag("record_voice_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Mic,
                                        contentDescription = "Record voice note",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        val msg = draftText.trim()
                                        draftText = ""
                                        viewModel.sendText(currentUserName, msg)
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MehfilPurple)
                                        .testTag("send_message_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = "Send message",
                                        tint = MehfilGold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when (val state = messagesState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.data, key = { it.id }) { msg ->
                        MessageBubbleItem(
                            message = msg,
                            isMine = msg.senderId == currentUserId && msg.senderName != "Mehfil Studio",
                            onSelectForReaction = { selectedMessageForReaction = msg }
                        )
                    }
                }
            }
        }
    }
}

private fun formatMessageTime(epochMillis: Long): String {
    return if (epochMillis <= 0L) "Now"
    else SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMillis))
}

@Composable
private fun MessageBubbleItem(
    message: ChatMessage,
    isMine: Boolean,
    onSelectForReaction: () -> Unit
) {
    val msgType = MessageType.fromRaw(message.messageType)
    val timeText = formatMessageTime(message.createdAt)

    if (msgType == MessageType.CALL) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            val bubbleShape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isMine) 20.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 20.dp
            )

            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .then(
                        if (isMine) {
                            Modifier.background(Brush.linearGradient(listOf(MehfilDeepPurple, MehfilPurple)))
                        } else {
                            Modifier.background(MaterialTheme.colorScheme.surface)
                        }
                    )
                    .clickable(onClick = onSelectForReaction)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                val contentColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface

                Column {
                    if (msgType == MessageType.IMAGE && !message.imageDataBase64.isNullOrBlank()) {
                        val bitmap = remember(message.imageDataBase64) {
                            ImageUtils.decodeBase64ToBitmap(message.imageDataBase64)
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Shared photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            if (message.text.isNotBlank()) Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    if (msgType == MessageType.VOICE) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = "Voice note",
                                tint = if (isMine) MehfilGold else contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Voice message • ${message.mediaDurationSec}s",
                                color = contentColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            color = contentColor,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = timeText,
                        color = if (isMine) MehfilGold.copy(alpha = 0.86f) else contentColor.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            if (!message.reaction.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.padding(top = 2.dp, start = 6.dp, end = 6.dp)
                ) {
                    Text(
                        text = message.reaction,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
