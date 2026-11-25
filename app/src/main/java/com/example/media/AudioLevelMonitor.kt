package com.example.media

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.math.sqrt

/**
 * Streams real microphone RMS audio amplitude [0f..1f] using Android's AudioRecord API
 * when RECORD_AUDIO permission is granted and mic is unmuted.
 */
object AudioLevelMonitor {
    private const val SAMPLE_RATE = 8000

    @SuppressLint("MissingPermission")
    fun observeMicLevel(hasPermission: Boolean, isMuted: Boolean): Flow<Float> = flow {
        if (!hasPermission || isMuted) {
            emit(0f)
            return@flow
        }

        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) {
            emit(0f)
            return@flow
        }

        var recorder: AudioRecord? = null
        try {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                emit(0f)
                return@flow
            }
            recorder.startRecording()
            val buffer = ShortArray(minBuf / 2)
            while (currentCoroutineContext().isActive) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    var sum = 0.0
                    for (i in 0 until read) {
                        val s = buffer[i].toDouble()
                        sum += s * s
                    }
                    val rms = sqrt(sum / read)
                    val normalized = (rms / 6500.0).toFloat().coerceIn(0.05f, 1f)
                    emit(normalized)
                }
                delay(90L)
            }
        } catch (e: SecurityException) {
            Log.w("AudioLevelMonitor", "RECORD_AUDIO permission missing: ${e.message}")
            emit(0f)
        } catch (e: Exception) {
            Log.w("AudioLevelMonitor", "AudioRecord unavailable on device: ${e.message}")
            emit(0f)
        } finally {
            try {
                recorder?.stop()
            } catch (_: Exception) {
            }
            try {
                recorder?.release()
            } catch (_: Exception) {
            }
        }
    }.flowOn(Dispatchers.IO)
}
