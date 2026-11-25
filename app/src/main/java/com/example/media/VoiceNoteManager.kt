package com.example.media

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class VoiceNoteManager(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var recordFile: File? = null
    private var recordStartMs: Long = 0L

    private var player: MediaPlayer? = null
    private var playbackTempFile: File? = null
    private var meterJob: Job? = null

    private val _playingMessageId = MutableStateFlow<String?>(null)
    val playingMessageId: StateFlow<String?> = _playingMessageId.asStateFlow()

    private val _micAmplitude = MutableStateFlow(0f)
    val micAmplitude: StateFlow<Float> = _micAmplitude.asStateFlow()

    fun startRecording(scope: CoroutineScope): Boolean {
        stopPlayback()
        cancelRecording()
        return try {
            val file = File(context.cacheDir, "mehfil_vn_${System.currentTimeMillis()}.m4a")
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(16000)
            r.setAudioEncodingBitRate(24000)
            r.setMaxDuration(30_000)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()

            recorder = r
            recordFile = file
            recordStartMs = System.currentTimeMillis()
            meterJob = scope.launch(Dispatchers.Default) {
                while (isActive && recorder != null) {
                    val amp = try {
                        ((recorder?.maxAmplitude ?: 0) / 14000f).coerceIn(0.05f, 1f)
                    } catch (_: Exception) {
                        0f
                    }
                    _micAmplitude.value = amp
                    delay(90L)
                }
            }
            true
        } catch (e: Exception) {
            Log.w("VoiceNoteManager", "Failed to start voice recording: ${e.message}")
            cancelRecording()
            false
        }
    }

    fun stopRecordingAndEncode(): Pair<String?, Long> {
        meterJob?.cancel()
        meterJob = null
        _micAmplitude.value = 0f

        val file = recordFile
        val elapsedSec = ((System.currentTimeMillis() - recordStartMs) / 1000L).coerceIn(1L, 600L)
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        recordFile = null

        val encoded = try {
            if (file != null && file.exists()) {
                val bytes = file.readBytes()
                if (bytes.isNotEmpty() && bytes.size <= 175_000) {
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                } else if (bytes.size > 175_000) {
                    Base64.encodeToString(bytes.copyOf(175_000), Base64.NO_WRAP)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("VoiceNoteManager", "Encode error: ${e.message}")
            null
        } finally {
            try {
                file?.delete()
            } catch (_: Exception) {
            }
        }
        return encoded to elapsedSec
    }

    fun cancelRecording() {
        meterJob?.cancel()
        meterJob = null
        _micAmplitude.value = 0f
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        try {
            recordFile?.delete()
        } catch (_: Exception) {
        }
        recordFile = null
    }

    fun togglePlayback(messageId: String, audioBase64: String?): Boolean {
        if (audioBase64.isNullOrBlank()) return false
        if (_playingMessageId.value == messageId) {
            stopPlayback()
            return true
        }
        stopPlayback()
        return try {
            val bytes = Base64.decode(audioBase64, Base64.DEFAULT)
            val temp = File(context.cacheDir, "mehfil_play_${System.currentTimeMillis()}.m4a")
            temp.writeBytes(bytes)
            playbackTempFile = temp

            val mp = MediaPlayer().apply {
                setDataSource(temp.absolutePath)
                prepare()
                setOnCompletionListener {
                    stopPlayback()
                }
                setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    true
                }
                start()
            }
            player = mp
            _playingMessageId.value = messageId
            true
        } catch (e: Exception) {
            Log.w("VoiceNoteManager", "Playback failed: ${e.message}")
            stopPlayback()
            false
        }
    }

    fun stopPlayback() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        try {
            playbackTempFile?.delete()
        } catch (_: Exception) {
        }
        playbackTempFile = null
        _playingMessageId.value = null
    }

    fun release() {
        cancelRecording()
        stopPlayback()
    }
}
