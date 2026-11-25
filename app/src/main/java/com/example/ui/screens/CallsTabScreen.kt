package com.example.ui.screens

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CallRecord
import com.example.data.CallStatus
import com.example.data.CallType
import com.example.data.UiState
import com.example.ui.components.MehfilAvatar
import com.example.ui.components.MehfilGlassCard
import com.example.ui.theme.MehfilChampagne
import com.example.ui.theme.MehfilCoral
import com.example.ui.theme.MehfilDeepPurple
import com.example.ui.theme.MehfilGlassBgStrong
import com.example.ui.theme.MehfilGold
import com.example.ui.theme.MehfilMidnightPlum
import com.example.ui.theme.MehfilMint
import com.example.ui.theme.MehfilPurple
import com.example.ui.theme.MehfilRoyalPurple
import com.example.viewmodel.MehfilViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

private enum class CallFilter(val label: String) {
    ALL("All"),
    VIDEO("Video"),
    VOICE("Voice"),
    MISSED("Missed")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsTabScreen(
    currentUserId: String,
    callsState: UiState<List<CallRecord>>,
    onStartCall: (peerId: String, peerName: String, callType: CallType) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(CallFilter.ALL) }
    val filteredCalls = remember(callsState, selectedFilter) {
        val list = (callsState as? UiState.Success)?.data.orEmpty()
        list.filter { call ->
            when (selectedFilter) {
                CallFilter.ALL -> true
                CallFilter.VIDEO -> CallType.fromRaw(call.callType) == CallType.VIDEO
                CallFilter.VOICE -> CallType.fromRaw(call.callType) == CallType.VOICE
                CallFilter.MISSED -> {
                    val st = CallStatus.fromRaw(call.status)
                    st == CallStatus.MISSED || st == CallStatus.DECLINED
                }
            }
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
                            text = "Mehfil Calls",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "mehfil_studio_banner") {
                    MehfilGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        highlightGold = true,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.linearGradient(listOf(MehfilRoyalPurple, MehfilPurple.copy(alpha = 0.55f))))
                                .padding(18.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Mehfil HD Video Studio",
                                    color = MehfilGold,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Test live HD WebRTC camera, front/rear lens switch, and real-time mic waveform.",
                                    color = MehfilChampagne.copy(alpha = 0.88f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = {
                                            onStartCall("mehfil_studio", "Mehfil Studio", CallType.VIDEO)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MehfilGold,
                                            contentColor = MehfilMidnightPlum
                                        ),
                                        modifier = Modifier.testTag("test_video_call_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Videocam,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("HD Video", fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            onStartCall("mehfil_studio", "Mehfil Studio", CallType.VOICE)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.16f),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("test_voice_call_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Call,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Voice")
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "call_filters") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(CallFilter.entries) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MehfilGold,
                                    selectedLabelColor = MehfilMidnightPlum,
                                    containerColor = MehfilGlassBgStrong,
                                    labelColor = MehfilChampagne
                                ),
                                modifier = Modifier.testTag("call_filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }

                when (callsState) {
                    is UiState.Loading -> {
                        item(key = "calls_loading") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MehfilGold)
                            }
                        }
                    }
                    is UiState.Error -> {
                        item(key = "calls_error") {
                            Text(
                                text = callsState.message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                    is UiState.Success -> {
                        if (filteredCalls.isEmpty()) {
                            item(key = "calls_empty") {
                                MehfilGlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Call,
                                            contentDescription = null,
                                            tint = MehfilGold.copy(alpha = 0.7f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "No call history yet",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Place a video or voice call from Chats, Contacts, or Mehfil HD Studio above.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredCalls, key = { it.id }) { call ->
                                val isOutgoing = call.callerId == currentUserId
                                val peerId = if (isOutgoing) call.calleeId else call.callerId
                                val peerName = if (isOutgoing) call.calleeName else call.callerName
                                val status = CallStatus.fromRaw(call.status)
                                val callType = CallType.fromRaw(call.callType)
                                val isMissed = status == CallStatus.MISSED || status == CallStatus.DECLINED
                                val timestampLabel = formatCallTimestamp(call.createdAt)

                                CallLogRow(
                                    peerName = peerName,
                                    isOutgoing = isOutgoing,
                                    isMissed = isMissed,
                                    callType = callType,
                                    qualityMode = call.qualityMode,
                                    durationSeconds = call.durationSeconds,
                                    timestampLabel = timestampLabel,
                                    onCallbackVoice = { onStartCall(peerId, peerName, CallType.VOICE) },
                                    onCallbackVideo = { onStartCall(peerId, peerName, CallType.VIDEO) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCallTimestamp(ts: Timestamp?): String {
    return if (ts == null) "Just now"
    else SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(ts.toDate())
}

@Composable
private fun CallLogRow(
    peerName: String,
    isOutgoing: Boolean,
    isMissed: Boolean,
    callType: CallType,
    qualityMode: String,
    durationSeconds: Long,
    timestampLabel: String,
    onCallbackVoice: () -> Unit,
    onCallbackVideo: () -> Unit
) {
    MehfilGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MehfilAvatar(name = peerName, size = 48.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isMissed) MehfilCoral else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when {
                            isMissed -> Icons.AutoMirrored.Rounded.CallMissed
                            isOutgoing -> Icons.AutoMirrored.Rounded.CallMade
                            else -> Icons.AutoMirrored.Rounded.CallReceived
                        },
                        contentDescription = null,
                        tint = if (isMissed) MehfilCoral else MehfilMint,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    val durationText = if (durationSeconds > 0L) {
                        " • ${MehfilViewModel.formatDurationShort(durationSeconds)}"
                    } else {
                        ""
                    }
                    Text(
                        text = "$timestampLabel$durationText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                color = MehfilGold.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (qualityMode == "LOW_DATA") "Eco" else "HD",
                    style = MaterialTheme.typography.labelSmall,
                    color = MehfilGold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = if (callType == CallType.VIDEO) onCallbackVideo else onCallbackVoice,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (callType == CallType.VIDEO) Icons.Rounded.Videocam else Icons.Rounded.Call,
                    contentDescription = "Call back $peerName",
                    tint = if (callType == CallType.VIDEO) MehfilGold else MehfilMint
                )
            }
        }
    }
}
