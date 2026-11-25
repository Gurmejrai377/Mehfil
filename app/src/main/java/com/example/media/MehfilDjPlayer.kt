package com.example.media

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Real audible AudioTrack DJ music synthesizer for Mehfil Music & Party Rooms.
 * Generates soft warm royal lo-fi / Sufi chord progressions and subtle percussion
 * mapped deterministically to the currently playing track title.
 */
class MehfilDjPlayer(private val scope: CoroutineScope) {
    private var playJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var currentTitle: String = ""
    private var currentlyPlaying: Boolean = false
    @Volatile
    private var mutedByUser: Boolean = false

    fun updateState(trackTitle: String, shouldPlay: Boolean) {
        val normalized = trackTitle.ifBlank { "Mehfil Royal Lo-Fi" }
        if (shouldPlay == currentlyPlaying && (!shouldPlay || normalized == currentTitle)) {
            return
        }
        currentTitle = normalized
        currentlyPlaying = shouldPlay
        if (shouldPlay) {
            startLoop(normalized)
        } else {
            stopLoop()
        }
    }

    fun setLocalMute(muted: Boolean) {
        mutedByUser = muted
    }

    private fun startLoop(title: String) {
        stopLoop()
        playJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 22050
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (e: Exception) {
                Log.w("MehfilDjPlayer", "AudioTrack unavailable: ${e.message}")
                return@launch
            }

            audioTrack = track
            try {
                track.play()
                val seed = abs(title.hashCode())
                val tempoBpm = 78 + (seed % 26)
                val samplesPerBeat = (sampleRate * 60) / tempoBpm
                val chords = listOf(
                    doubleArrayOf(220.0, 261.63, 329.63, 392.0), // Am7
                    doubleArrayOf(174.61, 220.0, 261.63, 329.63), // Fmaj7
                    doubleArrayOf(196.0, 246.94, 293.66, 392.0), // G6
                    doubleArrayOf(164.81, 196.0, 246.94, 329.63) // Em7
                )
                val melodyScale = doubleArrayOf(329.63, 392.0, 440.0, 523.25, 587.33, 659.25)

                val chunkSize = 1024
                val pcm = ShortArray(chunkSize)
                var sampleIndex = 0L

                while (isActive && currentlyPlaying) {
                    for (i in 0 until chunkSize) {
                        if (mutedByUser) {
                            pcm[i] = 0
                            sampleIndex++
                            continue
                        }
                        val t = sampleIndex.toDouble() / sampleRate
                        val beatPos = (sampleIndex % (samplesPerBeat * 16))
                        val chordIdx = ((beatPos / (samplesPerBeat * 4)).toInt() + seed) % chords.size
                        val chord = chords[chordIdx]

                        val stepIdx = ((sampleIndex / (samplesPerBeat / 2)) + seed) % melodyScale.size
                        val stepPhase = (sampleIndex % (samplesPerBeat / 2)).toDouble() / (samplesPerBeat / 2)
                        val melodyEnv = sin(PI * stepPhase).coerceAtLeast(0.0)
                        val melodyFreq = melodyScale[stepIdx.toInt()]

                        val beatFrac = (sampleIndex % samplesPerBeat).toDouble() / samplesPerBeat
                        val kickEnv = if (beatFrac < 0.14) (1.0 - beatFrac / 0.14) else 0.0
                        val kick = sin(2.0 * PI * (55.0 + 45.0 * kickEnv) * t) * kickEnv * 0.25

                        var pad = 0.0
                        for (freq in chord) {
                            pad += sin(2.0 * PI * freq * t)
                        }
                        pad = (pad / chord.size) * 0.22

                        val lead = sin(2.0 * PI * melodyFreq * t) * melodyEnv * 0.16
                        val mix = (pad + lead + kick).coerceIn(-0.85, 0.85)
                        pcm[i] = (mix * 11000.0).toInt().toShort()
                        sampleIndex++
                    }
                    track.write(pcm, 0, pcm.size)
                }
            } catch (e: Exception) {
                Log.w("MehfilDjPlayer", "Playback interrupted: ${e.message}")
            } finally {
                try {
                    track.stop()
                } catch (_: Exception) {
                }
                try {
                    track.release()
                } catch (_: Exception) {
                }
                if (audioTrack === track) audioTrack = null
            }
        }
    }

    private fun stopLoop() {
        playJob?.cancel()
        playJob = null
    }

    fun release() {
        currentlyPlaying = false
        stopLoop()
    }
}
