package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CallType
import com.example.data.Chat
import com.example.data.Contact
import com.example.data.Story
import com.example.data.UiState
import com.example.data.UserAccount
import com.example.ui.components.MehfilAvatar
import com.example.ui.components.MehfilGlassCard
import com.example.ui.components.MehfilLogoBadge
import com.example.ui.components.parseHexColor
import com.example.ui.theme.MehfilChampagne
import com.example.ui.theme.MehfilDeepPurple
import com.example.ui.theme.MehfilGlassBg
import com.example.ui.theme.MehfilGold
import com.example.ui.theme.MehfilMidnightPlum
import com.example.ui.theme.MehfilMint
import com.example.ui.theme.MehfilPurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsTabScreen(
    accountState: UiState<UserAccount?>,
    chatsState: UiState<List<Chat>>,
    storiesState: UiState<List<Story>>,
    contactsState: UiState<List<Contact>>,
    onOpenChat: (chatId: String, peerId: String, peerName: String) -> Unit,
    onStartCall: (peerId: String, peerName: String, callType: CallType) -> Unit,
    onOpenProfile: () -> Unit,
    onPostStory: (caption: String, moodEmoji: String, accentHex: String) -> Unit,
    onOpenStory: (Story) -> Unit,
    onGoToContacts: () -> Unit
) {
    val account = (accountState as? UiState.Success)?.data
    val contacts = (contactsState as? UiState.Success)?.data.orEmpty()
    val stories = (storiesState as? UiState.Success)?.data.orEmpty()

    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddStoryDialog by remember { mutableStateOf(false) }

    val filteredChats = remember(chatsState, searchQuery) {
        val list = (chatsState as? UiState.Success)?.data.orEmpty()
        if (searchQuery.isBlank()) list
        else list.filter { chat ->
            chat.peerName.contains(searchQuery, ignoreCase = true) ||
                chat.lastMessageText.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MehfilMidnightPlum, MehfilDeepPurple, MehfilMidnightPlum)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    if (searchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search Mehfil chats...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_search_input")
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MehfilLogoBadge(size = 38.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Mehfil",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MehfilChampagne
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = MehfilGold.copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = if (account?.lowDataMode == true) "Eco" else "HD",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MehfilGold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Yahan har baat ek Mehfil hai",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MehfilGold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            searchActive = !searchActive
                            if (!searchActive) searchQuery = ""
                        },
                        modifier = Modifier.testTag("toggle_chat_search_button")
                    ) {
                        Icon(
                            imageVector = if (searchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = if (searchActive) "Close search" else "Search chats",
                            tint = MehfilChampagne
                        )
                    }
                    IconButton(
                        onClick = onOpenProfile,
                        modifier = Modifier.testTag("open_profile_button")
                    ) {
                        MehfilAvatar(
                            name = account?.displayName ?: "Mehfil",
                            size = 36.dp,
                            online = account?.online ?: true
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            when (chatsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MehfilGold)
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chatsState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is UiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "stories_strip") {
                            StoriesAndOnlineStrip(
                                myName = account?.displayName ?: "My Story",
                                stories = stories,
                                contacts = contacts,
                                onAddStoryClick = { showAddStoryDialog = true },
                                onStoryClick = onOpenStory,
                                onQuickVideoCall = { contact ->
                                    onStartCall(contact.peerId, contact.displayName, CallType.VIDEO)
                                }
                            )
                        }

                        if (filteredChats.isEmpty()) {
                            item(key = "empty_chats") {
                                EmptyChatsCard(
                                    isSearching = searchQuery.isNotBlank(),
                                    onFindContacts = onGoToContacts
                                )
                            }
                        } else {
                            items(filteredChats, key = { it.id }) { chat ->
                                val contact = contacts.firstOrNull { it.peerId == chat.peerId }
                                val isOnline = contact?.online ?: true
                                val accentHex = contact?.avatarColorHex ?: "#6E2CF2"
                                val timeText = formatChatTimestamp(chat.updatedAt)

                                ChatRowItem(
                                    chat = chat,
                                    accentHex = accentHex,
                                    timeText = timeText,
                                    isOnline = isOnline,
                                    onClick = { onOpenChat(chat.id, chat.peerId, chat.peerName) },
                                    onVoiceCall = { onStartCall(chat.peerId, chat.peerName, CallType.VOICE) },
                                    onVideoCall = { onStartCall(chat.peerId, chat.peerName, CallType.VIDEO) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddStoryDialog) {
        AddStoryDialog(
            onDismiss = { showAddStoryDialog = false },
            onSubmit = { caption, emoji, hex ->
                onPostStory(caption, emoji, hex)
                showAddStoryDialog = false
            }
        )
    }
}

private fun formatChatTimestamp(epochMillis: Long): String {
    return if (epochMillis <= 0L) "Now"
    else SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMillis))
}

@Composable
private fun StoriesAndOnlineStrip(
    myName: String,
    stories: List<Story>,
    contacts: List<Contact>,
    onAddStoryClick: () -> Unit,
    onStoryClick: (Story) -> Unit,
    onQuickVideoCall: (Contact) -> Unit
) {
    MehfilGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        containerColor = MehfilGlassBg
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "add_story") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(68.dp)
                            .clickable(onClick = onAddStoryClick)
                            .testTag("add_story_button")
                    ) {
                        Box(modifier = Modifier.size(58.dp)) {
                            MehfilAvatar(name = myName, size = 58.dp)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(MehfilGold)
                                    .border(1.5.dp, MehfilDeepPurple, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Post story",
                                    tint = MehfilDeepPurple,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "My Story",
                            style = MaterialTheme.typography.labelSmall,
                            color = MehfilChampagne,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                items(stories, key = { it.id }) { story ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(68.dp)
                            .clickable { onStoryClick(story) }
                            .testTag("story_item_${story.id}")
                    ) {
                        MehfilAvatar(
                            name = story.ownerName,
                            accentHex = story.accentHex,
                            size = 58.dp,
                            hasStoryRing = true
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${story.moodEmoji} ${story.ownerName.substringBefore(" ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MehfilChampagne,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                items(contacts.take(6), key = { "online_${it.id}" }) { contact ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(68.dp)
                            .clickable { onQuickVideoCall(contact) }
                            .testTag("quick_call_${contact.peerId}")
                    ) {
                        MehfilAvatar(
                            name = contact.displayName,
                            accentHex = contact.avatarColorHex,
                            size = 58.dp,
                            online = contact.online
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = contact.displayName.substringBefore(" "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MehfilChampagne,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatRowItem(
    chat: Chat,
    accentHex: String,
    timeText: String,
    isOnline: Boolean,
    onClick: () -> Unit,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    MehfilGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("chat_row_${chat.id}"),
        shape = RoundedCornerShape(22.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MehfilAvatar(
                name = chat.peerName,
                accentHex = accentHex,
                size = 54.dp,
                online = isOnline,
                hasStoryRing = chat.pinned
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = chat.peerName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (chat.pinned) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = "Pinned chat",
                            tint = MehfilGold,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (chat.muted) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeOff,
                            contentDescription = "Muted chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = chat.lastMessageText.ifBlank { "Tap to start chatting on Mehfil" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MehfilGold.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onVoiceCall,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_voice_call_${chat.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = "Voice call ${chat.peerName}",
                            tint = MehfilMint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onVideoCall,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_video_call_${chat.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Videocam,
                            contentDescription = "Video call ${chat.peerName}",
                            tint = MehfilGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChatsCard(
    isSearching: Boolean,
    onFindContacts: () -> Unit
) {
    MehfilGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        highlightGold = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MehfilLogoBadge(size = 86.dp)
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = if (isSearching) "No matching Mehfils" else "Start your first Mehfil",
                style = MaterialTheme.typography.titleLarge,
                color = MehfilChampagne,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isSearching) {
                    "Try another name or keyword."
                } else {
                    "Yahan har baat ek Mehfil hai — add friends or hop into a Party Room!"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!isSearching) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onFindContacts,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MehfilGold,
                        contentColor = MehfilMidnightPlum
                    )
                ) {
                    Text("Explore Contacts", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AddStoryDialog(
    onDismiss: () -> Unit,
    onSubmit: (caption: String, emoji: String, accentHex: String) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🎤") }
    var selectedHex by remember { mutableStateOf("#6E2CF2") }
    val emojis = listOf("🎤", "👑", "✨", "🎵", "☕", "🔥")
    val colors = listOf("#6E2CF2", "#281054", "#D4AF37", "#00897B")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MehfilDeepPurple,
        titleContentColor = MehfilChampagne,
        textContentColor = Color.White,
        title = { Text("Share a Mehfil Story") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { if (it.length <= 200) caption = it },
                    label = { Text("What's on your mind?") },
                    placeholder = { Text("Yahan har baat ek Mehfil hai!") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("story_caption_input")
                )
                Text("Mood", style = MaterialTheme.typography.labelMedium, color = MehfilGold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    emojis.forEach { emoji ->
                        FilterChip(
                            selected = selectedEmoji == emoji,
                            onClick = { selectedEmoji = emoji },
                            label = { Text(emoji) }
                        )
                    }
                }
                Text("Theme", style = MaterialTheme.typography.labelMedium, color = MehfilGold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colors.forEach { hex ->
                        val color = parseHexColor(hex)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedHex = hex }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedHex == hex) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(caption, selectedEmoji, selectedHex) },
                enabled = caption.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MehfilGold,
                    contentColor = MehfilMidnightPlum
                ),
                modifier = Modifier.testTag("publish_story_button")
            ) {
                Text("Share Story", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MehfilChampagne) }
        }
    )
}
