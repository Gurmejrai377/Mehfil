package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.PartyRoom
import com.example.data.RoomMessage
import com.example.data.RoomMessageKind
import com.example.data.RoomSeat
import com.example.data.RoomSongRequest
import com.example.data.RoomType
import com.example.data.UiState
import com.example.media.MehfilDjPlayer
import com.example.media.RoomVoiceMesh
import com.example.ui.components.AnimatedMusicEqualizer
import com.example.ui.components.MehfilAvatar
import com.example.ui.components.MehfilGlassCard
import com.example.ui.components.MehfilLogoBadge
import com.example.ui.theme.MehfilChampagne
import com.example.ui.theme.MehfilCoral
import com.example.ui.theme.MehfilDeepPurple
import com.example.ui.theme.MehfilGlassBg
import com.example.ui.theme.MehfilGlassBgStrong
import com.example.ui.theme.MehfilGold
import com.example.ui.theme.MehfilMidnightPlum
import com.example.ui.theme.MehfilMint
import com.example.ui.theme.MehfilPurple
import com.example.ui.theme.MehfilPurpleLight
import com.example.ui.theme.MehfilRichGold
import com.example.ui.theme.MehfilRoyalPurple
import com.example.viewmodel.GiftBurst
import com.example.viewmodel.PartyRoomViewModel
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class LuxuryGift(
    val emoji: String,
    val name: String,
    val coins: Long
)

private val LUXURY_GIFTS = listOf(
    LuxuryGift("🌹", "Royal Rose", 10L),
    LuxuryGift("💖", "Golden Heart", 50L),
    LuxuryGift("🎤", "Gold Mic", 199L),
    LuxuryGift("👑", "Mehfil Crown", 599L),
    LuxuryGift("🏎️", "VIP Roadster", 1500L),
    LuxuryGift("🏰", "Royal Palace", 5000L)
)

@Composable
fun PartyRoomScreen(
    viewModel: PartyRoomViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val roomState by viewModel.roomState.collectAsStateWithLifecycle()
    val seats by viewModel.seatsState.collectAsStateWithLifecycle()
    val messages by viewModel.messagesState.collectAsStateWithLifecycle()
    val songs by viewModel.songsState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val chatListState = rememberLazyListState()

    var chatDraft by remember { mutableStateOf("") }
    var showGiftSheet by remember { mutableStateOf(false) }
    var showMusicSheet by remember { mutableStateOf(false) }
    var activeBurst by remember { mutableStateOf<GiftBurst?>(null) }

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
    }

    val room = (roomState as? UiState.Success)?.data
    val mySeat = seats.firstOrNull { it.occupantId == viewModel.currentUserId }
    val isUserHost = room?.hostId == viewModel.currentUserId
    val isMyMicLive = when {
        isUserHost -> room?.hostMicOn == true
        mySeat != null -> !mySeat.muted
        else -> false
    }

    val scope = rememberCoroutineScope()
    val djPlayer = remember(viewModel.roomId) { MehfilDjPlayer(scope) }
    val voiceMesh = remember(viewModel.roomId, viewModel.currentUserId) {
        RoomVoiceMesh(
            context = context.applicationContext,
            repository = viewModel.repository,
            scope = scope,
            roomId = viewModel.roomId,
            currentUserId = viewModel.currentUserId
        )
    }

    DisposableEffect(viewModel.roomId) {
        voiceMesh.startListening()
        onDispose {
            djPlayer.release()
            voiceMesh.release()
        }
    }

    LaunchedEffect(room?.currentSongTitle, room?.musicPlaying) {
        djPlayer.updateState(
            trackTitle = room?.currentSongTitle.orEmpty(),
            shouldPlay = room?.musicPlaying == true
        )
    }

    val speakerUids = remember(seats, room?.hostId, room?.hostMicOn) {
        buildSet {
            val hostId = room?.hostId
            if (!hostId.isNullOrBlank() && room?.hostMicOn == true) {
                add(hostId)
            }
            seats.forEach { seat ->
                val uid = seat.occupantId
                if (!uid.isNullOrBlank() && !seat.muted) {
                    add(uid)
                }
            }
        }
    }

    LaunchedEffect(speakerUids, isMyMicLive, hasMicPermission) {
        voiceMesh.syncStageState(
            speakerUids = speakerUids,
            myMicLive = isMyMicLive && hasMicPermission
        )
    }

    val micPulseTransition = rememberInfiniteTransition(label = "mic_pulse")
    val animatedMicPulse by micPulseTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_val"
    )
    val micLevel = if (isMyMicLive && hasMicPermission) animatedMicPulse else 0f

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.giftBursts.collect { burst ->
            activeBurst = burst
            delay(1800L)
            if (activeBurst?.id == burst.id) {
                activeBurst = null
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            chatListState.animateScrollToItem(messages.size - 1)
        }
    }

    var isLeavingRoom by remember { mutableStateOf(false) }
    val handleLeaveRoom: () -> Unit = {
        if (!isLeavingRoom) {
            isLeavingRoom = true
            viewModel.leaveRoom(onDone = onBack)
        }
    }

    BackHandler(onBack = handleLeaveRoom)

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
                .alpha(0.45f)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MehfilMidnightPlum.copy(alpha = 0.78f),
                            MehfilDeepPurple.copy(alpha = 0.72f),
                            MehfilMidnightPlum.copy(alpha = 0.94f)
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                PartyRoomBottomDock(
                    chatDraft = chatDraft,
                    onDraftChange = { if (it.length <= 250) chatDraft = it },
                    onSendChat = {
                        val txt = chatDraft.trim()
                        if (txt.isNotEmpty()) {
                            chatDraft = ""
                            viewModel.sendChatMessage(txt)
                        }
                    },
                    isMyMicLive = isMyMicLive,
                    isMusicPlaying = room?.musicPlaying == true,
                    onToggleMic = {
                        if (!hasMicPermission) {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                        viewModel.toggleMic()
                    },
                    onOpenMusic = { showMusicSheet = true },
                    onOpenGift = { showGiftSheet = true },
                    onQuickReaction = viewModel::sendReaction
                )
            }
        ) { innerPadding ->
            when (roomState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MehfilGold)
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (roomState as UiState.Error).message,
                            color = MehfilCoral
                        )
                    }
                }
                is UiState.Success -> {
                    if (room == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Room ended", color = MehfilChampagne)
                        }
                    } else {
                        val roomType = RoomType.fromRaw(room.roomType)

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(bottom = innerPadding.calculateBottomPadding())
                        ) {
                            // 1. Top Header Bar
                            PartyRoomTopHeader(
                                room = room,
                                roomType = roomType,
                                onBack = handleLeaveRoom,
                                onOpenMusic = { showMusicSheet = true }
                            )

                            // 2. Host on Top
                            HostStagePedestal(
                                room = room,
                                roomType = roomType,
                                micLevel = if (isUserHost && isMyMicLive) micLevel else 0.18f,
                                onToggleMusic = { viewModel.toggleMusic() }
                            )

                            // 3. 8 Mic Seats in Circle around Stage Medallion
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(255.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularMicStage(
                                    room = room,
                                    roomType = roomType,
                                    seats = seats,
                                    currentUserId = viewModel.currentUserId,
                                    myMicLevel = micLevel,
                                    onSeatClick = { seat -> viewModel.tapSeat(seat.seatIndex) },
                                    onCenterAction = {
                                        if (roomType == RoomType.MUSIC) showMusicSheet = true
                                        else showGiftSheet = true
                                    }
                                )

                                // Floating Luxury Gift / Reaction Overlay
                                this@Column.AnimatedVisibility(
                                    visible = activeBurst != null,
                                    enter = fadeIn() + scaleIn(),
                                    exit = fadeOut() + scaleOut()
                                ) {
                                    activeBurst?.let { burst ->
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MehfilMidnightPlum.copy(alpha = 0.88f),
                                            border = BorderStroke(1.5.dp, MehfilGold),
                                            shadowElevation = 12.dp
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = burst.emoji, fontSize = 24.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = burst.senderName,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MehfilGold
                                                    )
                                                    Text(
                                                        text = burst.title,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Live Chat Box Below
                            MehfilGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(22.dp),
                                containerColor = MehfilGlassBg.copy(alpha = 0.55f)
                            ) {
                                LazyColumn(
                                    state = chatListState,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(messages, key = { it.id }) { msg ->
                                        RoomChatBubble(message = msg)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGiftSheet && room != null) {
        SendGiftBottomSheet(
            room = room,
            seats = seats.filter { !it.occupantId.isNullOrBlank() },
            onSendGift = { gift, recipientLabel, seatId ->
                showGiftSheet = false
                viewModel.sendGift(
                    emoji = gift.emoji,
                    giftName = gift.name,
                    coins = gift.coins,
                    recipientLabel = recipientLabel,
                    recipientSeatId = seatId
                )
            },
            onDismiss = { showGiftSheet = false }
        )
    }

    if (showMusicSheet && room != null) {
        MusicDjBottomSheet(
            room = room,
            songs = songs,
            onTogglePlay = { viewModel.toggleMusic() },
            onRequestSong = { title, artist, playNow ->
                viewModel.requestSong(title, artist, playNow)
            },
            onPlaySong = { song -> viewModel.playSong(song) },
            onDismiss = { showMusicSheet = false }
        )
    }
}

@Composable
private fun PartyRoomTopHeader(
    room: PartyRoom,
    roomType: RoomType,
    onBack: () -> Unit,
    onOpenMusic: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("leave_room_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Leave room",
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${roomType.emoji} ${roomType.displayTitle} • ${room.viewerCount} Members",
                        style = MaterialTheme.typography.labelSmall,
                        color = MehfilGold
                    )
                    if (roomType == RoomType.LIVE) {
                        Surface(
                            color = MehfilCoral,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (roomType == RoomType.MUSIC) {
                Surface(
                    onClick = onOpenMusic,
                    color = MehfilPurple.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MehfilGold.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedMusicEqualizer(isPlaying = room.musicPlaying)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("DJ", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }
            Surface(
                color = MehfilGlassBgStrong,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, MehfilGold.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Diamond,
                        contentDescription = null,
                        tint = MehfilGold,
                        modifier = Modifier.size(13.dp)
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
                color = MehfilGlassBgStrong,
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = MehfilMint,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${room.viewerCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MehfilMint
                    )
                }
            }
        }
    }
}

@Composable
private fun HostStagePedestal(
    room: PartyRoom,
    roomType: RoomType,
    micLevel: Float,
    onToggleMusic: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (room.hostMicOn) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(1f + micLevel * 0.25f)
                        .clip(CircleShape)
                        .background(MehfilGold.copy(alpha = 0.24f))
                )
            }
            MehfilAvatar(
                name = room.hostName,
                accentHex = "#6E2CF2",
                size = 62.dp,
                online = true,
                hasStoryRing = true
            )
            Surface(
                color = MehfilGold,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 6.dp)
            ) {
                Text(
                    text = "👑 HOST",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MehfilMidnightPlum,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = room.hostName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        if (roomType == RoomType.MUSIC && room.currentSongTitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                onClick = onToggleMusic,
                color = MehfilRoyalPurple.copy(alpha = 0.8f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, MehfilGold.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (room.musicPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Toggle DJ song",
                        tint = MehfilGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = room.currentSongTitle,
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

@Composable
private fun CircularMicStage(
    room: PartyRoom,
    roomType: RoomType,
    seats: List<RoomSeat>,
    currentUserId: String,
    myMicLevel: Float,
    onSeatClick: (RoomSeat) -> Unit,
    onCenterAction: () -> Unit
) {
    val spinTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val spinAngle by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "spin"
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val radiusX = (maxWidth * 0.34f).coerceAtMost(142.dp)
        val radiusY = (maxHeight * 0.34f).coerceAtMost(84.dp)

        // Subtle golden orbital ring behind the 8 seats
        Box(
            modifier = Modifier
                .width(radiusX * 2)
                .height(radiusY * 2)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(MehfilGold.copy(alpha = 0.32f), MehfilPurpleLight.copy(alpha = 0.18f))
                    ),
                    shape = RoundedCornerShape(50)
                )
        )

        // Center Stage Medallion
        Surface(
            onClick = onCenterAction,
            shape = CircleShape,
            color = MehfilRoyalPurple.copy(alpha = 0.85f),
            border = BorderStroke(1.5.dp, MehfilGold.copy(alpha = 0.65f)),
            shadowElevation = 10.dp,
            modifier = Modifier.size(76.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (roomType == RoomType.MUSIC && room.musicPlaying) {
                            Modifier.rotate(spinAngle)
                        } else {
                            Modifier
                        }
                    )
            ) {
                MehfilLogoBadge(size = 66.dp)
            }
        }

        // 8 Mic Seats arranged in a circle
        seats.take(8).forEachIndexed { idx, seat ->
            val angleRad = (2.0 * PI * idx / 8.0) - (PI / 2.0)
            val offsetX: Dp = (radiusX.value * cos(angleRad).toFloat()).dp
            val offsetY: Dp = (radiusY.value * sin(angleRad).toFloat()).dp
            val isMine = seat.occupantId == currentUserId

            MicSeatCircleItem(
                seat = seat,
                isMine = isMine,
                micLevel = if (isMine) myMicLevel else 0f,
                onClick = { onSeatClick(seat) },
                modifier = Modifier.offset(x = offsetX, y = offsetY)
            )
        }
    }
}

@Composable
private fun MicSeatCircleItem(
    seat: RoomSeat,
    isMine: Boolean,
    micLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOccupied = !seat.occupantId.isNullOrBlank()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(64.dp)
            .clickable(onClick = onClick)
            .testTag("room_seat_${seat.seatIndex}")
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isOccupied && !seat.muted) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .scale(1f + micLevel * 0.25f)
                        .clip(CircleShape)
                        .background(
                            if (isMine) MehfilGold.copy(alpha = 0.28f)
                            else MehfilPurpleLight.copy(alpha = 0.18f)
                        )
                )
            }

            if (isOccupied) {
                MehfilAvatar(
                    name = seat.occupantName ?: "Guest",
                    accentHex = seat.avatarColorHex,
                    size = 46.dp,
                    online = false,
                    hasStoryRing = isMine
                )
                if (seat.muted) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(MehfilCoral),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MicOff,
                            contentDescription = "Muted",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
                if (!seat.lastReaction.isNullOrBlank()) {
                    Text(
                        text = seat.lastReaction,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                    )
                }
            } else {
                Surface(
                    shape = CircleShape,
                    color = MehfilGlassBgStrong,
                    border = BorderStroke(1.dp, MehfilGold.copy(alpha = 0.42f)),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "Take mic seat ${seat.seatIndex + 1}",
                            tint = MehfilGold.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (isOccupied) {
                if (isMine) "You" else seat.occupantName.orEmpty().substringBefore(" ")
            } else {
                "Mic ${seat.seatIndex + 1}"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (isMine) MehfilGold else MehfilChampagne.copy(alpha = 0.86f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RoomChatBubble(message: RoomMessage) {
    val kind = RoomMessageKind.fromRaw(message.kind)
    val bgColor = when (kind) {
        RoomMessageKind.GIFT -> MehfilGold.copy(alpha = 0.22f)
        RoomMessageKind.SONG_REQUEST -> MehfilPurple.copy(alpha = 0.35f)
        RoomMessageKind.SYSTEM -> MehfilRoyalPurple.copy(alpha = 0.5f)
        else -> Color.White.copy(alpha = 0.08f)
    }
    val senderColor = when (kind) {
        RoomMessageKind.GIFT -> MehfilGold
        RoomMessageKind.SONG_REQUEST -> MehfilPurpleLight
        RoomMessageKind.SYSTEM -> MehfilMint
        else -> MehfilChampagne
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${message.senderName}: ",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = senderColor
            )
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
    }
}

@Composable
private fun PartyRoomBottomDock(
    chatDraft: String,
    onDraftChange: (String) -> Unit,
    onSendChat: () -> Unit,
    isMyMicLive: Boolean,
    isMusicPlaying: Boolean,
    onToggleMic: () -> Unit,
    onOpenMusic: () -> Unit,
    onOpenGift: () -> Unit,
    onQuickReaction: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MehfilMidnightPlum.copy(alpha = 0.92f))
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Quick Emoji Reaction Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            items(listOf("👏", "🔥", "❤️", "😂", "🎵", "👑")) { emoji ->
                Surface(
                    onClick = { onQuickReaction(emoji) },
                    shape = RoundedCornerShape(50),
                    color = MehfilRoyalPurple.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, MehfilGold.copy(alpha = 0.28f))
                ) {
                    Text(
                        text = emoji,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Bottom Controls: Chat Input + Mic On/Off + Music + Gift
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = chatDraft,
                onValueChange = onDraftChange,
                placeholder = { Text("Say hi...", color = MehfilChampagne.copy(alpha = 0.6f)) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MehfilGold,
                    unfocusedBorderColor = MehfilPurpleLight.copy(alpha = 0.35f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                trailingIcon = {
                    if (chatDraft.isNotBlank()) {
                        IconButton(
                            onClick = onSendChat,
                            modifier = Modifier.testTag("room_send_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Send room chat",
                                tint = MehfilGold
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("room_chat_input")
            )

            // Mic On/Off Button
            IconButton(
                onClick = onToggleMic,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isMyMicLive) MehfilMint else MehfilCoral.copy(alpha = 0.85f))
                    .testTag("room_mic_toggle_button")
            ) {
                Icon(
                    imageVector = if (isMyMicLive) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                    contentDescription = if (isMyMicLive) "Mute mic" else "Turn mic on",
                    tint = if (isMyMicLive) MehfilMidnightPlum else Color.White
                )
            }

            // Music Button
            IconButton(
                onClick = onOpenMusic,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isMusicPlaying) MehfilPurple else MehfilRoyalPurple)
                    .border(1.dp, MehfilGold.copy(alpha = 0.45f), CircleShape)
                    .testTag("room_music_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = "Music & Song Requests",
                    tint = MehfilGold
                )
            }

            // Gift Button
            IconButton(
                onClick = onOpenGift,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(MehfilGold, MehfilPurple, MehfilRichGold)))
                    .testTag("room_gift_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.CardGiftcard,
                    contentDescription = "Send gift",
                    tint = MehfilMidnightPlum
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SendGiftBottomSheet(
    room: PartyRoom,
    seats: List<RoomSeat>,
    onSendGift: (gift: LuxuryGift, recipientLabel: String, seatId: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTargetSeatId by remember { mutableStateOf<String?>(null) }
    var selectedTargetLabel by remember { mutableStateOf(room.hostName) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MehfilDeepPurple,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Send Luxury Mehfil Gift",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MehfilGold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Recipient: $selectedTargetLabel",
                style = MaterialTheme.typography.labelMedium,
                color = MehfilChampagne
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item(key = "target_host") {
                    Surface(
                        onClick = {
                            selectedTargetSeatId = null
                            selectedTargetLabel = room.hostName
                        },
                        shape = RoundedCornerShape(50),
                        color = if (selectedTargetSeatId == null) MehfilGold else MehfilRoyalPurple,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "👑 ${room.hostName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selectedTargetSeatId == null) MehfilMidnightPlum else Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                items(seats, key = { it.id }) { seat ->
                    val selected = selectedTargetSeatId == seat.id
                    Surface(
                        onClick = {
                            selectedTargetSeatId = seat.id
                            selectedTargetLabel = seat.occupantName ?: "Seat ${seat.seatIndex + 1}"
                        },
                        shape = RoundedCornerShape(50),
                        color = if (selected) MehfilGold else MehfilRoyalPurple,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = seat.occupantName ?: "Seat ${seat.seatIndex + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) MehfilMidnightPlum else Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LUXURY_GIFTS.chunked(3).forEach { rowGifts ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowGifts.forEach { gift ->
                            MehfilGlassCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("gift_item_${gift.name.lowercase().replace(" ", "_")}"),
                                highlightGold = gift.coins >= 500L,
                                onClick = { onSendGift(gift, selectedTargetLabel, selectedTargetSeatId) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = gift.emoji, fontSize = 28.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = gift.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${gift.coins} 💎",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MehfilGold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicDjBottomSheet(
    room: PartyRoom,
    songs: List<RoomSongRequest>,
    onTogglePlay: () -> Unit,
    onRequestSong: (title: String, artist: String, playNow: Boolean) -> Unit,
    onPlaySong: (RoomSongRequest) -> Unit,
    onDismiss: () -> Unit
) {
    var songTitle by remember { mutableStateOf("") }
    var songArtist by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MehfilDeepPurple,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = MehfilGold,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "DJ Music & Sing Together",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MehfilGold
                        )
                        Text(
                            text = room.currentSongTitle.ifBlank { "Tap play to start DJ deck" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MehfilChampagne
                        )
                    }
                }
                Button(
                    onClick = onTogglePlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MehfilGold,
                        contentColor = MehfilMidnightPlum
                    )
                ) {
                    Icon(
                        imageVector = if (room.musicPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (room.musicPlaying) "Pause" else "Play", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = songTitle,
                    onValueChange = { if (it.length <= 60) songTitle = it },
                    label = { Text("Request Song") },
                    placeholder = { Text("Tum Hi Ho...") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("request_song_title_input")
                )
                OutlinedTextField(
                    value = songArtist,
                    onValueChange = { if (it.length <= 40) songArtist = it },
                    label = { Text("Artist") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (songTitle.isNotBlank()) {
                            onRequestSong(songTitle, songArtist, true)
                            songTitle = ""
                            songArtist = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MehfilGold)
                        .testTag("submit_song_request_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add & play song",
                        tint = MehfilMidnightPlum
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "SONG QUEUE (${songs.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MehfilGold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (songs.isEmpty()) {
                Text(
                    text = "No songs queued yet — request a track above to sing together!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MehfilChampagne.copy(alpha = 0.75f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                songs.take(5).forEach { song ->
                    MehfilGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        highlightGold = song.playing,
                        onClick = { onPlaySong(song) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                AnimatedMusicEqualizer(isPlaying = song.playing && room.musicPlaying)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (song.playing) MehfilGold else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${song.artist} • Req by ${song.requestedBy}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MehfilChampagne.copy(alpha = 0.72f)
                                    )
                                }
                            }
                            Text(
                                text = if (song.playing) "PLAYING" else "PLAY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MehfilGold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
