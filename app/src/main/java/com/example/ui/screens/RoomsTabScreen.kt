package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PartyRoom
import com.example.data.RoomType
import com.example.data.UiState
import com.example.ui.components.AnimatedMusicEqualizer
import com.example.ui.components.MehfilAvatar
import com.example.ui.components.MehfilGlassCard
import com.example.ui.components.MehfilLogoBadge
import com.example.ui.theme.MehfilChampagne
import com.example.ui.theme.MehfilDeepPurple
import com.example.ui.theme.MehfilGlassBgStrong
import com.example.ui.theme.MehfilGold
import com.example.ui.theme.MehfilMidnightPlum
import com.example.ui.theme.MehfilMint
import com.example.ui.theme.MehfilPurple
import com.example.ui.theme.MehfilPurpleLight
import com.example.ui.theme.MehfilRichGold
import com.example.ui.theme.MehfilRoyalPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomsTabScreen(
    roomsState: UiState<List<PartyRoom>>,
    onJoinRoom: (roomId: String) -> Unit,
    onCreateRoom: (name: String, roomType: RoomType, topic: String) -> Unit
) {
    var selectedType by remember { mutableStateOf<RoomType?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val filteredRooms = remember(roomsState, selectedType) {
        val list = (roomsState as? UiState.Success)?.data.orEmpty()
        if (selectedType == null) list
        else list.filter { RoomType.fromRaw(it.roomType) == selectedType }
    }

    val livePulseTransition = rememberInfiniteTransition(label = "live_dot")
    val livePulse by livePulseTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MehfilLogoBadge(size = 38.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Mehfil Rooms",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MehfilChampagne
                            )
                            Text(
                                text = "Yahan har baat ek Mehfil hai",
                                style = MaterialTheme.typography.labelSmall,
                                color = MehfilGold
                            )
                        }
                    }
                },
                actions = {
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MehfilGold,
                            contentColor = MehfilMidnightPlum
                        ),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .testTag("create_room_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Create Room",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create Room", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "rooms_hero_stage") {
                    MehfilGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        highlightGold = true,
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Image(
                                painter = painterResource(id = R.drawable.img_party_stage),
                                contentDescription = "Mehfil VIP Party Room stage",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .matchParentSize()
                                    .alpha(0.55f)
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                MehfilMidnightPlum.copy(alpha = 0.88f),
                                                MehfilDeepPurple.copy(alpha = 0.55f)
                                            )
                                        )
                                    )
                            )
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = MehfilGold.copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, MehfilGold.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.AutoAwesome,
                                                contentDescription = null,
                                                tint = MehfilGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "VIP PARTY STAGE",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MehfilGold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "8-Mic Royal Lounge & DJ Stage",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Hop onto a mic seat, drop luxury gifts, request DJ tracks & sing together.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MehfilChampagne.copy(alpha = 0.85f)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { showCreateDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MehfilGold,
                                            contentColor = MehfilMidnightPlum
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Mic,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Host Party", fontWeight = FontWeight.Bold)
                                    }
                                    val musicRoomId = (roomsState as? UiState.Success)?.data
                                        ?.firstOrNull { it.roomType == RoomType.MUSIC.name }?.id
                                    if (musicRoomId != null) {
                                        Button(
                                            onClick = { onJoinRoom(musicRoomId) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MehfilPurple.copy(alpha = 0.78f),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.MusicNote,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("DJ Room")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "room_type_filters") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "filter_all") {
                            FilterChip(
                                selected = selectedType == null,
                                onClick = { selectedType = null },
                                label = { Text("All Rooms") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MehfilGold,
                                    selectedLabelColor = MehfilMidnightPlum,
                                    containerColor = MehfilGlassBgStrong,
                                    labelColor = MehfilChampagne
                                ),
                                modifier = Modifier.testTag("room_filter_all")
                            )
                        }
                        items(RoomType.entries, key = { it.name }) { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = {
                                    selectedType = if (selectedType == type) null else type
                                },
                                label = { Text("${type.emoji} ${type.displayTitle}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MehfilGold,
                                    selectedLabelColor = MehfilMidnightPlum,
                                    containerColor = MehfilGlassBgStrong,
                                    labelColor = MehfilChampagne
                                ),
                                modifier = Modifier.testTag("room_filter_${type.name.lowercase()}")
                            )
                        }
                    }
                }

                when (roomsState) {
                    is UiState.Loading -> {
                        item(key = "rooms_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MehfilGold)
                            }
                        }
                    }
                    is UiState.Error -> {
                        item(key = "rooms_error") {
                            Text(
                                text = roomsState.message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                    is UiState.Success -> {
                        if (filteredRooms.isEmpty()) {
                            item(key = "rooms_empty") {
                                MehfilGlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No rooms in this category yet",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MehfilChampagne
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { showCreateDialog = true },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MehfilGold,
                                                contentColor = MehfilMidnightPlum
                                            )
                                        ) {
                                            Text("Create the First Room", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            items(filteredRooms, key = { it.id }) { room ->
                                PartyRoomLobbyCard(
                                    room = room,
                                    livePulse = livePulse,
                                    onJoin = { onJoinRoom(room.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePartyRoomDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, type, topic ->
                showCreateDialog = false
                onCreateRoom(name, type, topic)
            }
        )
    }
}

@Composable
private fun PartyRoomLobbyCard(
    room: PartyRoom,
    livePulse: Float,
    onJoin: () -> Unit
) {
    val type = RoomType.fromRaw(room.roomType)
    val typeColor = when (type) {
        RoomType.MUSIC -> MehfilPurpleLight
        RoomType.PARTY -> MehfilGold
        RoomType.CHAT -> MehfilMint
        RoomType.LIVE -> Color(0xFFFF6B9D)
    }
    val typeIcon = when (type) {
        RoomType.MUSIC -> Icons.Rounded.MusicNote
        RoomType.PARTY -> Icons.Rounded.People
        RoomType.CHAT -> Icons.Rounded.RecordVoiceOver
        RoomType.LIVE -> Icons.Rounded.LiveTv
    }

    MehfilGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("room_card_${room.id}"),
        highlightGold = type == RoomType.PARTY || type == RoomType.MUSIC,
        onClick = onJoin
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = typeColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, typeColor.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${type.emoji} ${type.displayTitle}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MehfilDeepPurple.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Diamond,
                                contentDescription = null,
                                tint = MehfilGold,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${room.totalGiftCoins}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MehfilGold
                            )
                        }
                    }

                    Surface(
                        color = MehfilMint.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .scale(livePulse)
                                    .clip(CircleShape)
                                    .background(MehfilMint)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = MehfilMint,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${room.viewerCount}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MehfilMint
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                MehfilAvatar(
                    name = room.hostName,
                    accentHex = "#6E2CF2",
                    size = 52.dp,
                    online = true,
                    hasStoryRing = true
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = room.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Host: ${room.hostName} • ${room.topic}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (room.musicPlaying && room.currentSongTitle.isNotBlank()) {
                    Surface(
                        color = MehfilRoyalPurple.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedMusicEqualizer(isPlaying = true)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = room.currentSongTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MehfilChampagne,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = MehfilGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "8 Mic Seats • Tap to Join",
                            style = MaterialTheme.typography.labelMedium,
                            color = MehfilChampagne.copy(alpha = 0.8f)
                        )
                    }
                }

                Button(
                    onClick = onJoin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MehfilGold,
                        contentColor = MehfilMidnightPlum
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Join", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CreatePartyRoomDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, roomType: RoomType, topic: String) -> Unit
) {
    var roomName by remember { mutableStateOf("") }
    var roomTopic by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(RoomType.PARTY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MehfilDeepPurple,
        titleContentColor = MehfilChampagne,
        textContentColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MehfilLogoBadge(size = 34.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Create Mehfil Room", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { if (it.length <= 60) roomName = it },
                    label = { Text("Room Name") },
                    placeholder = { Text("Royal Mehfil Night") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_room_name_input")
                )

                Text(
                    text = "Select Room Mode",
                    style = MaterialTheme.typography.labelMedium,
                    color = MehfilGold
                )

                // Prominent Music / Party selector row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoomModeOptionCard(
                        type = RoomType.MUSIC,
                        selected = selectedType == RoomType.MUSIC,
                        onClick = { selectedType = RoomType.MUSIC },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_room_type_music")
                    )
                    RoomModeOptionCard(
                        type = RoomType.PARTY,
                        selected = selectedType == RoomType.PARTY,
                        onClick = { selectedType = RoomType.PARTY },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_room_type_party")
                    )
                }

                // Additional Voice Chat / Live options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoomModeOptionCard(
                        type = RoomType.CHAT,
                        selected = selectedType == RoomType.CHAT,
                        onClick = { selectedType = RoomType.CHAT },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_room_type_chat")
                    )
                    RoomModeOptionCard(
                        type = RoomType.LIVE,
                        selected = selectedType == RoomType.LIVE,
                        onClick = { selectedType = RoomType.LIVE },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_room_type_live")
                    )
                }

                OutlinedTextField(
                    value = roomTopic,
                    onValueChange = { if (it.length <= 120) roomTopic = it },
                    label = { Text("Topic / Welcome Note (Optional)") },
                    placeholder = { Text(selectedType.description) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val resolvedName = roomName.trim().ifBlank { "Mehfil ${selectedType.displayTitle}" }
                    onCreate(resolvedName, selectedType, roomTopic)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MehfilGold,
                    contentColor = MehfilMidnightPlum
                ),
                modifier = Modifier.testTag("confirm_create_room_button")
            ) {
                Text("Go Live", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MehfilChampagne)
            }
        }
    )
}

@Composable
private fun RoomModeOptionCard(
    type: RoomType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MehfilGold.copy(alpha = 0.22f) else MehfilRoyalPurple.copy(alpha = 0.65f),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MehfilGold else MehfilPurpleLight.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = type.emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = type.shortBadge,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) MehfilGold else Color.White
            )
        }
    }
}
