package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.DataSaverOn
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.CallQualityMode
import com.example.data.CallType
import com.example.media.AudioLevelMonitor
import com.example.ui.components.MehfilAvatar
import com.example.ui.theme.MehfilCoral
import com.example.ui.theme.MehfilDeepPurple
import com.example.ui.theme.MehfilGold
import com.example.ui.theme.MehfilMidnightPlum
import com.example.ui.theme.MehfilMint
import com.example.ui.theme.MehfilPurple
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun ActiveCallScreen(
    peerId: String,
    peerName: String,
    initialCallType: CallType,
    initialQualityMode: CallQualityMode,
    onQualityChanged: (CallQualityMode) -> Unit,
    onEndCall: (durationSeconds: Long, qualityMode: CallQualityMode) -> Unit
) {
    val context = LocalContext.current
    var isVideoEnabled by remember { mutableStateOf(initialCallType == CallType.VIDEO) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(initialCallType == CallType.VIDEO) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    var qualityMode by remember { mutableStateOf(initialQualityMode) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasCameraPermission = grants[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasMicPermission = grants[Manifest.permission.RECORD_AUDIO] ?: hasMicPermission
    }

    LaunchedEffect(Unit) {
        val needed = buildList {
            if (initialCallType == CallType.VIDEO && !hasCameraPermission) add(Manifest.permission.CAMERA)
            if (!hasMicPermission) add(Manifest.permission.RECORD_AUDIO)
        }
        if (needed.isNotEmpty()) {
            permissionsLauncher.launch(needed.toTypedArray())
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            elapsedSeconds += 1L
        }
    }

    DisposableEffect(isSpeakerOn) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        try {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = isSpeakerOn
        } catch (_: Exception) {
        }
        onDispose {
            try {
                audioManager?.isSpeakerphoneOn = false
                audioManager?.mode = AudioManager.MODE_NORMAL
            } catch (_: Exception) {
            }
        }
    }

    val micLevel by AudioLevelMonitor
        .observeMicLevel(hasPermission = hasMicPermission, isMuted = isMuted)
        .collectAsStateWithLifecycle(initialValue = 0f)
    val animatedScale by animateFloatAsState(
        targetValue = 1f + (micLevel * 0.42f),
        label = "mic_pulse"
    )

    BackHandler {
        onEndCall(elapsedSeconds, qualityMode)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MehfilMidnightPlum)
    ) {
        if (isVideoEnabled && hasCameraPermission) {
            CameraXLivePreview(
                lensFacing = lensFacing,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_mehfil_backdrop),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.88f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.46f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = peerName,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val mins = elapsedSeconds / 60
                        val secs = elapsedSeconds % 60
                        val timer = String.format(Locale.US, "%02d:%02d", mins, secs)
                        Text(
                            text = if (peerId == "mehfil_studio") "Mehfil Studio • $timer" else "Connected • $timer",
                            color = MehfilGold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Surface(
                    onClick = {
                        val next = if (qualityMode == CallQualityMode.HD) CallQualityMode.LOW_DATA else CallQualityMode.HD
                        qualityMode = next
                        onQualityChanged(next)
                    },
                    color = Color.Black.copy(alpha = 0.48f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("toggle_call_quality_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (qualityMode == CallQualityMode.HD) Icons.Rounded.Hd else Icons.Rounded.DataSaverOn,
                            contentDescription = "Toggle call bandwidth mode",
                            tint = if (qualityMode == CallQualityMode.HD) MehfilGold else MehfilMint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (qualityMode == CallQualityMode.HD) "HD Mode" else "Eco Data",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            if (!isVideoEnabled || !hasCameraPermission) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(144.dp)
                                .scale(animatedScale)
                                .clip(CircleShape)
                                .background(MehfilGold.copy(alpha = 0.28f))
                        )
                        MehfilAvatar(
                            name = peerName,
                            accentHex = "#5B1FA6",
                            size = 118.dp,
                            online = true
                        )
                    }
                    if (isVideoEnabled && !hasCameraPermission) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                permissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MehfilGold,
                                contentColor = MehfilDeepPurple
                            ),
                            modifier = Modifier.testTag("grant_camera_button")
                        ) {
                            Text("Enable Camera for HD Video", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }

            Surface(
                color = Color.Black.copy(alpha = 0.58f),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 18.dp, horizontal = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CallControlButton(
                            icon = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                            label = if (isMuted) "Unmute" else "Mute",
                            active = isMuted,
                            onClick = { isMuted = !isMuted },
                            tag = "call_mute_button"
                        )
                        CallControlButton(
                            icon = if (isVideoEnabled) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
                            label = if (isVideoEnabled) "Video On" else "Video Off",
                            active = !isVideoEnabled,
                            onClick = {
                                if (!isVideoEnabled && !hasCameraPermission) {
                                    permissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                                }
                                isVideoEnabled = !isVideoEnabled
                            },
                            tag = "call_video_toggle_button"
                        )
                        CallControlButton(
                            icon = Icons.Rounded.Cameraswitch,
                            label = "Flip",
                            active = false,
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                    CameraSelector.LENS_FACING_BACK
                                } else {
                                    CameraSelector.LENS_FACING_FRONT
                                }
                            },
                            tag = "call_flip_camera_button"
                        )
                        CallControlButton(
                            icon = Icons.Rounded.VolumeUp,
                            label = if (isSpeakerOn) "Speaker" else "Earpiece",
                            active = isSpeakerOn,
                            onClick = { isSpeakerOn = !isSpeakerOn },
                            tag = "call_speaker_button"
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    IconButton(
                        onClick = { onEndCall(elapsedSeconds, qualityMode) },
                        modifier = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .background(MehfilCoral)
                            .testTag("end_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CallEnd,
                            contentDescription = "End call",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (active) MehfilPurple else Color.White.copy(alpha = 0.16f))
                .testTag(tag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun CameraXLivePreview(
    lensFacing: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    DisposableEffect(lensFacing, lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        var boundProvider: ProcessCameraProvider? = null

        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                boundProvider = provider
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val selector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview)
            } catch (e: Exception) {
                Log.w("CameraXPreview", "Camera unavailable on this device/emulator: ${e.message}")
            }
        }, executor)

        onDispose {
            try {
                boundProvider?.unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}
