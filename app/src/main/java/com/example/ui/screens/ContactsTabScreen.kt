package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import com.example.R
import com.example.data.CallType
import com.example.data.Contact
import com.example.data.PublicProfile
import com.example.data.UiState
import com.example.data.UserAccount
import com.example.ui.components.MehfilAvatar
import com.example.ui.components.MehfilGlassCard
import com.example.ui.theme.MehfilChampagne
import com.example.ui.theme.MehfilDeepPurple
import com.example.ui.theme.MehfilGold
import com.example.ui.theme.MehfilMidnightPlum
import com.example.ui.theme.MehfilMint
import com.example.ui.theme.MehfilRoyalPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsTabScreen(
    accountState: UiState<UserAccount?>,
    contactsState: UiState<List<Contact>>,
    discoverableProfilesState: UiState<List<PublicProfile>>,
    onAddContact: (peerId: String, name: String, mehfilId: String, status: String, colorHex: String) -> Unit,
    onToggleFavorite: (Contact) -> Unit,
    onDeleteContact: (String) -> Unit,
    onOpenChat: (peerId: String, peerName: String) -> Unit,
    onStartCall: (peerId: String, peerName: String, callType: CallType) -> Unit
) {
    val account = (accountState as? UiState.Success)?.data
    val clipboard = LocalClipboardManager.current
    var copiedId by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredContacts = remember(contactsState, searchQuery) {
        val list = (contactsState as? UiState.Success)?.data.orEmpty()
        if (searchQuery.isBlank()) list
        else list.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
                it.mehfilId.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredDirectory = remember(discoverableProfilesState, contactsState, searchQuery) {
        val existingPeerIds = (contactsState as? UiState.Success)?.data.orEmpty().map { it.peerId }.toSet()
        val profiles = (discoverableProfilesState as? UiState.Success)?.data.orEmpty()
        val base = profiles.filter { it.userId !in existingPeerIds }
        if (searchQuery.isBlank()) base
        else base.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
                it.mehfilId.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MehfilMidnightPlum)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_party_stage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.24f)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MehfilMidnightPlum.copy(alpha = 0.72f),
                            MehfilDeepPurple.copy(alpha = 0.78f),
                            MehfilMidnightPlum.copy(alpha = 0.92f)
                        )
                    )
                )
        )
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mehfil Circle",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MehfilChampagne
                        )
                        Text(
                            text = "Yahan har baat ek Mehfil hai",
                            style = MaterialTheme.typography.labelSmall,
                            color = MehfilGold
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_contact_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PersonAdd,
                            contentDescription = "Add Mehfil contact",
                            tint = MehfilGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "my_mehfil_id_card") {
                    val myMehfilId = account?.mehfilId ?: "mehfil_user"
                    MehfilGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        highlightGold = true,
                        shape = RoundedCornerShape(22.dp),
                        onClick = {
                            clipboard.setText(AnnotatedString(myMehfilId))
                            copiedId = true
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MehfilAvatar(
                                name = account?.displayName ?: "Mehfil",
                                size = 48.dp,
                                online = account?.online ?: true,
                                hasStoryRing = true
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Your Mehfil ID",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MehfilChampagne
                                )
                                Text(
                                    text = myMehfilId,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MehfilGold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MehfilRoyalPurple
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "Copy Mehfil ID",
                                        tint = MehfilGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (copiedId) "Copied" else "Copy",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MehfilGold
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "contact_search") {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter contacts or Mehfil ID...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Rounded.Search, contentDescription = null, tint = MehfilGold)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("contact_search_input")
                    )
                }

                when (contactsState) {
                    is UiState.Loading -> {
                        item(key = "contacts_loading") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MehfilGold)
                            }
                        }
                    }
                    is UiState.Error -> {
                        item(key = "contacts_error") {
                            Text(
                                text = contactsState.message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    is UiState.Success -> {
                        item(key = "contacts_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MY CONTACTS (${filteredContacts.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MehfilGold
                                )
                                OutlinedButton(
                                    onClick = { showAddDialog = true },
                                    border = BorderStroke(1.dp, MehfilGold.copy(alpha = 0.55f)),
                                    modifier = Modifier.testTag("quick_add_contact_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PersonAdd,
                                        contentDescription = null,
                                        tint = MehfilGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Friend", color = MehfilGold)
                                }
                            }
                        }

                        items(filteredContacts, key = { it.id }) { contact ->
                            ContactRowItem(
                                contact = contact,
                                onToggleFavorite = { onToggleFavorite(contact) },
                                onDelete = { onDeleteContact(contact.id) },
                                onChat = { onOpenChat(contact.peerId, contact.displayName) },
                                onVoiceCall = { onStartCall(contact.peerId, contact.displayName, CallType.VOICE) },
                                onVideoCall = { onStartCall(contact.peerId, contact.displayName, CallType.VIDEO) }
                            )
                        }
                    }
                }

                if (filteredDirectory.isNotEmpty()) {
                    item(key = "discover_header") {
                        Text(
                            text = "LIVE ON MEHFIL (${filteredDirectory.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MehfilMint,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp)
                        )
                    }

                    items(filteredDirectory, key = { "dir_${it.userId}" }) { profile ->
                        DirectoryProfileRowItem(
                            profile = profile,
                            onAddFriend = {
                                onAddContact(
                                    profile.userId,
                                    profile.displayName,
                                    profile.mehfilId,
                                    profile.statusText,
                                    profile.avatarColorHex
                                )
                            },
                            onChat = { onOpenChat(profile.userId, profile.displayName) },
                            onVoiceCall = { onStartCall(profile.userId, profile.displayName, CallType.VOICE) },
                            onVideoCall = { onStartCall(profile.userId, profile.displayName, CallType.VIDEO) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddContactDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, mehfilId, status ->
                val cleanPeerId = "local_${mehfilId.lowercase().replace(Regex("[^a-z0-9_]"), "_")}"
                onAddContact(cleanPeerId, name, mehfilId, status, "#6E2CF2")
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ContactRowItem(
    contact: Contact,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onChat: () -> Unit,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    MehfilGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("contact_row_${contact.peerId}"),
        shape = RoundedCornerShape(20.dp),
        onClick = onChat
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MehfilAvatar(
                name = contact.displayName,
                accentHex = contact.avatarColorHex,
                size = 50.dp,
                online = contact.online
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${contact.mehfilId} • ${contact.statusText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = if (contact.favorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                    contentDescription = "Toggle favorite",
                    tint = if (contact.favorite) MehfilGold else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onVoiceCall, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = "Voice call ${contact.displayName}",
                    tint = MehfilMint,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onVideoCall, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Videocam,
                    contentDescription = "Video call ${contact.displayName}",
                    tint = MehfilGold,
                    modifier = Modifier.size(22.dp)
                )
            }
            if (contact.peerId != "mehfil_studio") {
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Remove contact",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectoryProfileRowItem(
    profile: PublicProfile,
    onAddFriend: () -> Unit,
    onChat: () -> Unit,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    MehfilGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("directory_row_${profile.userId}"),
        shape = RoundedCornerShape(20.dp),
        onClick = onChat
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MehfilAvatar(
                name = profile.displayName,
                accentHex = profile.avatarColorHex,
                size = 50.dp,
                online = profile.online
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${profile.mehfilId} • ${profile.statusText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onAddFriend, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Rounded.PersonAdd,
                    contentDescription = "Add ${profile.displayName}",
                    tint = MehfilGold,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onChat, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Rounded.ChatBubbleOutline,
                    contentDescription = "Message ${profile.displayName}",
                    tint = MehfilChampagne,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onVoiceCall, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = "Voice call ${profile.displayName}",
                    tint = MehfilMint,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onVideoCall, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Videocam,
                    contentDescription = "Video call ${profile.displayName}",
                    tint = MehfilGold,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, mehfilId: String, status: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mehfilId by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Available on Mehfil") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MehfilDeepPurple,
        titleContentColor = MehfilChampagne,
        textContentColor = Color.White,
        title = { Text("Add Mehfil Contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 60) name = it },
                    label = { Text("Contact Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_contact_name_input")
                )
                OutlinedTextField(
                    value = mehfilId,
                    onValueChange = { if (it.length <= 40) mehfilId = it },
                    label = { Text("Mehfil ID (e.g. mehfil_arjun)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_contact_id_input")
                )
                OutlinedTextField(
                    value = status,
                    onValueChange = { if (it.length <= 120) status = it },
                    label = { Text("Status Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val resolvedId = mehfilId.trim().ifBlank {
                        "mehfil_${name.trim().lowercase().replace(Regex("[^a-z0-9]"), "").take(12).ifBlank { "friend" }}"
                    }
                    onAdd(name.trim(), resolvedId, status.trim())
                },
                enabled = name.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MehfilGold,
                    contentColor = MehfilMidnightPlum
                ),
                modifier = Modifier.testTag("confirm_add_contact_button")
            ) {
                Text("Save Contact", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MehfilChampagne) }
        }
    )
}
