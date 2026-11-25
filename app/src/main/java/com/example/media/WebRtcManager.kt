package com.example.media

import android.content.Context
import android.util.Log
import com.example.data.CallCandidate
import com.example.data.CallQualityMode
import com.example.data.CallStatus
import com.example.data.CallType
import com.example.data.MehfilRepository
import com.example.data.RoomSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

enum class WebRtcConnectionStatus {
    INITIALIZING,
    RINGING,
    CONNECTING,
    CONNECTED,
    STUDIO_READY,
    ENDED
}

private object SharedWebRtcRuntime {
    @Volatile
    private var initialized = false
    val eglBase: EglBase by lazy { EglBase.create() }

    @Synchronized
    fun createFactory(context: Context): PeerConnectionFactory {
        if (!initialized) {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions()
            )
            initialized = true
        }
        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    fun iceServers(): List<PeerConnection.IceServer> = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
    )
}

/**
 * Full real-time WebRTC 1-on-1 audio/video call engine using Firestore signaling
 * (/calls/{callId} + /calls/{callId}/candidates) and Google STUN servers.
 */
class WebRtcCallSession(
    private val context: Context,
    private val repository: MehfilRepository,
    private val scope: CoroutineScope,
    private val callId: String,
    private val currentUserId: String,
    private val peerId: String,
    private val initialCallType: CallType,
    private val initialQualityMode: CallQualityMode
) {
    val eglContext: EglBase.Context get() = SharedWebRtcRuntime.eglBase.eglBaseContext
    val isStudioSelfCheck: Boolean = peerId == "mehfil_studio" || peerId == currentUserId

    private val _connectionState = MutableStateFlow(
        if (isStudioSelfCheck) WebRtcConnectionStatus.STUDIO_READY else WebRtcConnectionStatus.INITIALIZING
    )
    val connectionState: StateFlow<WebRtcConnectionStatus> = _connectionState.asStateFlow()

    private val _hasRemoteVideo = MutableStateFlow(false)
    val hasRemoteVideo: StateFlow<Boolean> = _hasRemoteVideo.asStateFlow()

    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null

    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null

    private var callJob: Job? = null
    private var candidatesJob: Job? = null
    private var started = false
    private var offerHandled = false
    private var answerHandled = false
    private val processedCandidateIds = mutableSetOf<String>()
    private val pendingIceCandidates = mutableListOf<IceCandidate>()
    private var remoteDescriptionSet = false
    private var participantUids: List<String> = listOf(currentUserId, peerId).distinct()

    fun attachRenderers(local: SurfaceViewRenderer?, remote: SurfaceViewRenderer?) {
        if (local != null && localRenderer !== local) {
            localRenderer?.let { localVideoTrack?.removeSink(it) }
            localRenderer = local
            localVideoTrack?.addSink(local)
        }
        if (remote != null && remoteRenderer !== remote) {
            remoteRenderer?.let { remoteVideoTrack?.removeSink(it) }
            remoteRenderer = remote
            remoteVideoTrack?.addSink(remote)
        }
    }

    fun start(hasCameraPermission: Boolean, hasMicPermission: Boolean) {
        if (started) return
        started = true
        try {
            val f = SharedWebRtcRuntime.createFactory(context)
            factory = f

            if (hasMicPermission) {
                val aSource = f.createAudioSource(MediaConstraints())
                val aTrack = f.createAudioTrack("MEHFIL_AUDIO_${currentUserId.take(6)}", aSource)
                aTrack.setEnabled(true)
                audioSource = aSource
                localAudioTrack = aTrack
            }

            if (initialCallType == CallType.VIDEO && hasCameraPermission) {
                setupCameraCapture(f, initialQualityMode)
            }

            if (isStudioSelfCheck) {
                _connectionState.value = WebRtcConnectionStatus.STUDIO_READY
                return
            }

            val rtcConfig = PeerConnection.RTCConfiguration(SharedWebRtcRuntime.iceServers()).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            }

            val pc = f.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    when (state) {
                        PeerConnection.IceConnectionState.CONNECTED,
                        PeerConnection.IceConnectionState.COMPLETED ->
                            _connectionState.value = WebRtcConnectionStatus.CONNECTED
                        PeerConnection.IceConnectionState.DISCONNECTED,
                        PeerConnection.IceConnectionState.FAILED,
                        PeerConnection.IceConnectionState.CLOSED ->
                            if (_connectionState.value == WebRtcConnectionStatus.CONNECTED) {
                                _connectionState.value = WebRtcConnectionStatus.CONNECTING
                            }
                        else -> {}
                    }
                }

                override fun onIceConnectionReceivingChange(p0: Boolean) {}
                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}

                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate ?: return
                    scope.launch {
                        repository.addCallIceCandidate(
                            callId = callId,
                            participantUids = participantUids,
                            sdpMid = candidate.sdpMid ?: "0",
                            sdpMLineIndex = candidate.sdpMLineIndex,
                            sdpCandidate = candidate.sdp
                        )
                    }
                }

                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
                override fun onAddStream(p0: MediaStream?) {}
                override fun onRemoveStream(p0: MediaStream?) {}
                override fun onDataChannel(p0: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                    val track = receiver?.track()
                    if (track is VideoTrack) {
                        remoteVideoTrack = track
                        remoteRenderer?.let { track.addSink(it) }
                        _hasRemoteVideo.value = true
                    }
                }
            })

            peerConnection = pc
            val streamIds = listOf("MEHFIL_STREAM")
            localAudioTrack?.let { pc?.addTrack(it, streamIds) }
            localVideoTrack?.let { pc?.addTrack(it, streamIds) }
            applyQualityMode(initialQualityMode)
            observeSignaling()
        } catch (e: Exception) {
            Log.e("WebRtcCallSession", "Failed to initialize WebRTC: ${e.message}", e)
        }
    }

    private fun setupCameraCapture(f: PeerConnectionFactory, quality: CallQualityMode) {
        try {
            val enumerator = Camera2Enumerator(context)
            val deviceName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
                ?: enumerator.deviceNames.firstOrNull()
                ?: return
            val capturer = enumerator.createCapturer(deviceName, null) ?: return
            val helper = SurfaceTextureHelper.create("MehfilCapture", SharedWebRtcRuntime.eglBase.eglBaseContext)
            val vSource = f.createVideoSource(capturer.isScreencast)
            capturer.initialize(helper, context.applicationContext, vSource.capturerObserver)
            val width = if (quality == CallQualityMode.HD) 720 else 480
            val height = if (quality == CallQualityMode.HD) 1280 else 640
            val fps = if (quality == CallQualityMode.HD) 24 else 15
            capturer.startCapture(width, height, fps)

            val vTrack = f.createVideoTrack("MEHFIL_VIDEO_${currentUserId.take(6)}", vSource)
            vTrack.setEnabled(true)
            localRenderer?.let { vTrack.addSink(it) }

            videoCapturer = capturer
            surfaceHelper = helper
            videoSource = vSource
            localVideoTrack = vTrack
        } catch (e: Exception) {
            Log.w("WebRtcCallSession", "Camera2 capture unavailable: ${e.message}")
        }
    }

    private fun observeSignaling() {
        callJob = scope.launch {
            repository.observeCall(callId, currentUserId).collect { record ->
                if (record == null) return@collect
                participantUids = record.participantUids.ifEmpty { participantUids }

                when (CallStatus.fromRaw(record.status)) {
                    CallStatus.ENDED, CallStatus.DECLINED, CallStatus.MISSED -> {
                        _connectionState.value = WebRtcConnectionStatus.ENDED
                        return@collect
                    }
                    CallStatus.RINGING -> {
                        if (_connectionState.value == WebRtcConnectionStatus.INITIALIZING) {
                            _connectionState.value = WebRtcConnectionStatus.RINGING
                        }
                    }
                    CallStatus.ACTIVE -> {
                        if (_connectionState.value == WebRtcConnectionStatus.RINGING ||
                            _connectionState.value == WebRtcConnectionStatus.INITIALIZING
                        ) {
                            _connectionState.value = WebRtcConnectionStatus.CONNECTING
                        }
                    }
                }

                val isCaller = record.callerId == currentUserId
                val pc = peerConnection ?: return@collect

                if (isCaller) {
                    if (record.offerSdp.isNullOrBlank() && !offerHandled) {
                        offerHandled = true
                        createAndPublishOffer(pc)
                    } else if (!record.answerSdp.isNullOrBlank() && !answerHandled) {
                        answerHandled = true
                        setRemoteSdp(pc, SessionDescription.Type.ANSWER, record.answerSdp)
                    }
                } else {
                    if (!record.offerSdp.isNullOrBlank() && !offerHandled) {
                        offerHandled = true
                        setRemoteSdp(pc, SessionDescription.Type.OFFER, record.offerSdp) {
                            createAndPublishAnswer(pc)
                        }
                    }
                }
            }
        }

        candidatesJob = scope.launch {
            repository.observeCallCandidates(callId, currentUserId).collect { list ->
                for (cand in list) {
                    if (cand.senderId == currentUserId) continue
                    if (!processedCandidateIds.add(cand.id)) continue
                    val ice = IceCandidate(cand.sdpMid, cand.sdpMLineIndex, cand.sdpCandidate)
                    if (remoteDescriptionSet) {
                        peerConnection?.addIceCandidate(ice)
                    } else {
                        pendingIceCandidates.add(ice)
                    }
                }
            }
        }
    }

    private fun createAndPublishOffer(pc: PeerConnection) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "OfferToReceiveVideo",
                    if (initialCallType == CallType.VIDEO) "true" else "false"
                )
            )
        }
        pc.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp ?: return
                pc.setLocalDescription(SimpleSdpObserver(), sdp)
                scope.launch {
                    repository.publishCallOffer(callId, sdp.description)
                }
            }
        }, constraints)
    }

    private fun createAndPublishAnswer(pc: PeerConnection) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "OfferToReceiveVideo",
                    if (initialCallType == CallType.VIDEO) "true" else "false"
                )
            )
        }
        pc.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp ?: return
                pc.setLocalDescription(SimpleSdpObserver(), sdp)
                scope.launch {
                    repository.publishCallAnswer(callId, sdp.description)
                }
            }
        }, constraints)
    }

    private fun setRemoteSdp(
        pc: PeerConnection,
        type: SessionDescription.Type,
        sdpText: String,
        onApplied: () -> Unit = {}
    ) {
        pc.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                remoteDescriptionSet = true
                pendingIceCandidates.forEach { pc.addIceCandidate(it) }
                pendingIceCandidates.clear()
                onApplied()
            }
        }, SessionDescription(type, sdpText))
    }

    fun setMicMuted(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
    }

    fun setVideoEnabled(enabled: Boolean, hasCameraPermission: Boolean) {
        if (enabled && localVideoTrack == null && hasCameraPermission) {
            factory?.let { f ->
                setupCameraCapture(f, initialQualityMode)
                localVideoTrack?.let { peerConnection?.addTrack(it, listOf("MEHFIL_STREAM")) }
            }
        }
        localVideoTrack?.setEnabled(enabled)
    }

    fun switchCamera() {
        try {
            videoCapturer?.switchCamera(null)
        } catch (e: Exception) {
            Log.w("WebRtcCallSession", "Switch camera failed: ${e.message}")
        }
    }

    fun applyQualityMode(qualityMode: CallQualityMode) {
        try {
            val maxBitrate = if (qualityMode == CallQualityMode.LOW_DATA) 180_000 else 1_200_000
            peerConnection?.senders?.forEach { sender ->
                if (sender.track()?.kind() == "video") {
                    val params = sender.parameters
                    params.encodings.forEach { it.maxBitrateBps = maxBitrate }
                    sender.parameters = params
                }
            }
        } catch (_: Exception) {
        }
    }

    fun release() {
        callJob?.cancel()
        candidatesJob?.cancel()
        try {
            localRenderer?.let { localVideoTrack?.removeSink(it) }
            remoteRenderer?.let { remoteVideoTrack?.removeSink(it) }
        } catch (_: Exception) {
        }
        try {
            videoCapturer?.stopCapture()
        } catch (_: Exception) {
        }
        try {
            videoCapturer?.dispose()
        } catch (_: Exception) {
        }
        try {
            surfaceHelper?.dispose()
        } catch (_: Exception) {
        }
        try {
            localVideoTrack?.dispose()
            videoSource?.dispose()
            localAudioTrack?.dispose()
            audioSource?.dispose()
            peerConnection?.close()
            peerConnection?.dispose()
            factory?.dispose()
        } catch (_: Exception) {
        }
        videoCapturer = null
        surfaceHelper = null
        localVideoTrack = null
        videoSource = null
        localAudioTrack = null
        audioSource = null
        peerConnection = null
        factory = null
    }
}

/**
 * Multi-peer WebRTC voice stage mesh for live Party/Music/Chat/Live rooms.
 * Connects speakers in the room over WebRTC audio via /rooms/{roomId}/signals.
 */
class RoomVoiceMesh(
    private val context: Context,
    private val repository: MehfilRepository,
    private val scope: CoroutineScope,
    private val roomId: String,
    private val currentUserId: String
) {
    private var factory: PeerConnectionFactory? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private val peers = mutableMapOf<String, PeerConnection>()
    private val processedSignalIds = mutableSetOf<String>()
    private val remoteSetForPeer = mutableSetOf<String>()
    private val queuedIce = mutableMapOf<String, MutableList<IceCandidate>>()
    private var signalJob: Job? = null

    fun startListening() {
        if (signalJob != null) return
        signalJob = scope.launch {
            repository.observeRoomSignals(roomId, currentUserId).collect { signals ->
                for (sig in signals) {
                    if (sig.fromUid == currentUserId) continue
                    if (!processedSignalIds.add(sig.id)) continue
                    handleIncomingSignal(sig)
                }
            }
        }
    }

    fun syncStageState(speakerUids: Set<String>, myMicLive: Boolean) {
        try {
            if (myMicLive && localAudioTrack == null) {
                ensureAudioTrack()
            }
            localAudioTrack?.setEnabled(myMicLive)

            val activePeers = speakerUids.filter { it != currentUserId }.toSet()
            // Close connections for peers no longer on stage
            val toRemove = peers.keys.filter { it !in activePeers }
            toRemove.forEach { uid ->
                peers.remove(uid)?.close()
                remoteSetForPeer.remove(uid)
                queuedIce.remove(uid)
            }

            // Lexicographically smaller UID initiates OFFER when we have live stage participants
            if (myMicLive || activePeers.isNotEmpty()) {
                activePeers.forEach { peerUid ->
                    if (!peers.containsKey(peerUid) && currentUserId < peerUid) {
                        val pc = getOrCreatePeer(peerUid) ?: return@forEach
                        val constraints = MediaConstraints().apply {
                            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                        }
                        pc.createOffer(object : SimpleSdpObserver() {
                            override fun onCreateSuccess(sdp: SessionDescription?) {
                                sdp ?: return
                                pc.setLocalDescription(SimpleSdpObserver(), sdp)
                                scope.launch {
                                    repository.sendRoomSignal(
                                        roomId = roomId,
                                        fromUid = currentUserId,
                                        toUid = peerUid,
                                        signalType = "OFFER",
                                        payload = sdp.description
                                    )
                                }
                            }
                        }, constraints)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("RoomVoiceMesh", "Stage sync skipped: ${e.message}")
        }
    }

    private fun ensureAudioTrack() {
        val f = factory ?: SharedWebRtcRuntime.createFactory(context).also { factory = it }
        if (localAudioTrack == null) {
            val aSource = f.createAudioSource(MediaConstraints())
            val aTrack = f.createAudioTrack("MEHFIL_STAGE_AUDIO", aSource)
            audioSource = aSource
            localAudioTrack = aTrack
        }
    }

    private fun getOrCreatePeer(peerUid: String): PeerConnection? {
        peers[peerUid]?.let { return it }
        val f = factory ?: SharedWebRtcRuntime.createFactory(context).also { factory = it }
        val rtcConfig = PeerConnection.RTCConfiguration(SharedWebRtcRuntime.iceServers()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        val pc = f.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(cand: IceCandidate?) {
                cand ?: return
                val payload = "${cand.sdpMid ?: "0"}|${cand.sdpMLineIndex}|${cand.sdp}"
                scope.launch {
                    repository.sendRoomSignal(roomId, currentUserId, peerUid, "ICE", payload)
                }
            }
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
        }) ?: return null
        localAudioTrack?.let { pc.addTrack(it, listOf("MEHFIL_STAGE")) }
        peers[peerUid] = pc
        return pc
    }

    private fun handleIncomingSignal(sig: RoomSignal) {
        val pc = getOrCreatePeer(sig.fromUid) ?: return
        when (sig.signalType) {
            "OFFER" -> {
                pc.setRemoteDescription(object : SimpleSdpObserver() {
                    override fun onSetSuccess() {
                        remoteSetForPeer.add(sig.fromUid)
                        queuedIce.remove(sig.fromUid)?.forEach { pc.addIceCandidate(it) }
                        val constraints = MediaConstraints().apply {
                            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                        }
                        pc.createAnswer(object : SimpleSdpObserver() {
                            override fun onCreateSuccess(sdp: SessionDescription?) {
                                sdp ?: return
                                pc.setLocalDescription(SimpleSdpObserver(), sdp)
                                scope.launch {
                                    repository.sendRoomSignal(
                                        roomId = roomId,
                                        fromUid = currentUserId,
                                        toUid = sig.fromUid,
                                        signalType = "ANSWER",
                                        payload = sdp.description
                                    )
                                }
                            }
                        }, constraints)
                    }
                }, SessionDescription(SessionDescription.Type.OFFER, sig.payload))
            }
            "ANSWER" -> {
                pc.setRemoteDescription(object : SimpleSdpObserver() {
                    override fun onSetSuccess() {
                        remoteSetForPeer.add(sig.fromUid)
                        queuedIce.remove(sig.fromUid)?.forEach { pc.addIceCandidate(it) }
                    }
                }, SessionDescription(SessionDescription.Type.ANSWER, sig.payload))
            }
            "ICE" -> {
                val parts = sig.payload.split("|", limit = 3)
                if (parts.size == 3) {
                    val mLine = parts[1].toIntOrNull() ?: 0
                    val ice = IceCandidate(parts[0], mLine, parts[2])
                    if (sig.fromUid in remoteSetForPeer) {
                        pc.addIceCandidate(ice)
                    } else {
                        queuedIce.getOrPut(sig.fromUid) { mutableListOf() }.add(ice)
                    }
                }
            }
        }
    }

    fun release() {
        signalJob?.cancel()
        signalJob = null
        peers.values.forEach {
            try {
                it.close()
                it.dispose()
            } catch (_: Exception) {
            }
        }
        peers.clear()
        try {
            localAudioTrack?.dispose()
            audioSource?.dispose()
            factory?.dispose()
        } catch (_: Exception) {
        }
        localAudioTrack = null
        audioSource = null
        factory = null
    }
}

private open class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {
        Log.w("WebRtcSdp", "SDP create failed: $error")
    }
    override fun onSetFailure(error: String?) {
        Log.w("WebRtcSdp", "SDP set failed: $error")
    }
}
