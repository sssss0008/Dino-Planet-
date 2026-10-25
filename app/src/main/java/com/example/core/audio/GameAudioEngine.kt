package com.example.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

object GameAudioEngine {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var ambientJob: Job? = null

    var sfxVolume: Float = 0.8f
    var musicVolume: Float = 0.5f
    var isMuted: Boolean = false

    private const val SAMPLE_RATE = 22050

    fun playRoar() {
        if (isMuted || sfxVolume <= 0.01f) return
        scope.launch {
            val durationMs = 600
            val numSamples = (durationMs * SAMPLE_RATE) / 1000
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                // Modulated frequency descending from 160Hz to 80Hz with heavy harmonic growl
                val baseFreq = 160.0 - 80.0 * (i.toDouble() / numSamples)
                val tremolo = 1.0 + 0.3 * sin(2.0 * PI * 18.0 * t)
                val noise = (Math.random() * 2.0 - 1.0) * 0.4
                val wave = (sin(2.0 * PI * baseFreq * t) + 0.5 * sin(4.0 * PI * baseFreq * t) + noise) * tremolo
                val envelope = when {
                    i < numSamples * 0.1 -> i / (numSamples * 0.1)
                    else -> (1.0 - (i - numSamples * 0.1) / (numSamples * 0.9))
                }
                val sample = (wave * envelope * Short.MAX_VALUE * 0.7 * sfxVolume).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        }
    }

    fun playAttack() {
        if (isMuted || sfxVolume <= 0.01f) return
        scope.launch {
            val durationMs = 250
            val numSamples = (durationMs * SAMPLE_RATE) / 1000
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                val freq = 400.0 - 320.0 * (i.toDouble() / numSamples)
                val noise = (Math.random() * 2.0 - 1.0) * 0.6
                val wave = (sin(2.0 * PI * freq * t) * 0.5 + noise * 0.5)
                val env = 1.0 - (i.toDouble() / numSamples)
                val sample = (wave * env * Short.MAX_VALUE * 0.6 * sfxVolume).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        }
    }

    fun playCollect() {
        if (isMuted || sfxVolume <= 0.01f) return
        scope.launch {
            val durationMs = 200
            val numSamples = (durationMs * SAMPLE_RATE) / 1000
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                val freq = if (i < numSamples / 2) 587.33 else 880.0 // D5 to A5 chime
                val wave = sin(2.0 * PI * freq * t)
                val env = 1.0 - (i.toDouble() / numSamples)
                val sample = (wave * env * Short.MAX_VALUE * 0.5 * sfxVolume).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        }
    }

    fun playDrink() {
        if (isMuted || sfxVolume <= 0.01f) return
        scope.launch {
            val durationMs = 300
            val numSamples = (durationMs * SAMPLE_RATE) / 1000
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                val freq = 300.0 + 150.0 * sin(2.0 * PI * 12.0 * t)
                val wave = sin(2.0 * PI * freq * t)
                val env = sin(PI * (i.toDouble() / numSamples))
                val sample = (wave * env * Short.MAX_VALUE * 0.45 * sfxVolume).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        }
    }

    fun playLevelUp() {
        if (isMuted || sfxVolume <= 0.01f) return
        scope.launch {
            val notes = doubleArrayOf(440.0, 554.37, 659.25, 880.0) // A major arpeggio
            val noteDurationMs = 150
            val totalSamples = ((notes.size * noteDurationMs) * SAMPLE_RATE) / 1000
            val buffer = ShortArray(totalSamples)

            for (n in notes.indices) {
                val freq = notes[n]
                val startIdx = (n * noteDurationMs * SAMPLE_RATE) / 1000
                val endIdx = ((n + 1) * noteDurationMs * SAMPLE_RATE) / 1000
                val len = endIdx - startIdx
                for (i in 0 until len) {
                    val t = i.toFloat() / SAMPLE_RATE
                    val wave = sin(2.0 * PI * freq * t)
                    val env = 1.0 - (i.toDouble() / len)
                    val sample = (wave * env * Short.MAX_VALUE * 0.6 * sfxVolume).toInt()
                    buffer[startIdx + i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            playPcm(buffer)
        }
    }

    fun playHatch() {
        if (isMuted || sfxVolume <= 0.01f) return
        scope.launch {
            // Egg crack snap followed by high joyous chirp
            val durationMs = 500
            val numSamples = (durationMs * SAMPLE_RATE) / 1000
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                val wave = if (i < numSamples * 0.3) {
                    (Math.random() * 2.0 - 1.0) // Crackle
                } else {
                    val chirpFreq = 900.0 + 400.0 * sin(2.0 * PI * 10.0 * t)
                    sin(2.0 * PI * chirpFreq * t)
                }
                val env = 1.0 - (i.toDouble() / numSamples)
                val sample = (wave * env * Short.MAX_VALUE * 0.6 * sfxVolume).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        }
    }

    fun playVolcanoWarning() {
        if (isMuted || sfxVolume <= 0.01f) return
        scope.launch {
            val durationMs = 800
            val numSamples = (durationMs * SAMPLE_RATE) / 1000
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                // Deep 45-65Hz sub-bass earth rumble
                val rumble = sin(2.0 * PI * 55.0 * t) + 0.4 * sin(2.0 * PI * 35.0 * t) + (Math.random() * 0.3)
                val env = sin(PI * (i.toDouble() / numSamples))
                val sample = (rumble * env * Short.MAX_VALUE * 0.75 * sfxVolume).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        }
    }

    fun playButtonClick() {
        if (isMuted || sfxVolume <= 0.01f) return
        scope.launch {
            val durationMs = 60
            val numSamples = (durationMs * SAMPLE_RATE) / 1000
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                val wave = sin(2.0 * PI * 750.0 * t)
                val env = 1.0 - (i.toDouble() / numSamples)
                val sample = (wave * env * Short.MAX_VALUE * 0.35 * sfxVolume).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        }
    }

    fun startAmbientLoop() {
        if (ambientJob?.isActive == true) return
        ambientJob = scope.launch {
            while (isActive) {
                if (!isMuted && musicVolume > 0.01f) {
                    // Prehistoric wind & distant cricket ambient pulse
                    val chordDurationMs = 2500
                    val numSamples = (chordDurationMs * SAMPLE_RATE) / 1000
                    val buffer = ShortArray(numSamples)
                    val base = 110.0 // A2 soothing deep chord
                    for (i in 0 until numSamples) {
                        val t = i.toFloat() / SAMPLE_RATE
                        val chord = 0.5 * sin(2.0 * PI * base * t) +
                                0.3 * sin(2.0 * PI * (base * 1.5) * t) +
                                0.2 * sin(2.0 * PI * (base * 2.0) * t)
                        val env = sin(PI * (i.toDouble() / numSamples))
                        val sample = (chord * env * Short.MAX_VALUE * 0.25 * musicVolume).toInt()
                        buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    playPcm(buffer)
                }
                delay(3000)
            }
        }
    }

    fun stopAmbientLoop() {
        ambientJob?.cancel()
        ambientJob = null
    }

    private fun playPcm(buffer: ShortArray) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            // Release after playing
            scope.launch {
                delay((buffer.size.toLong() * 1000L / SAMPLE_RATE) + 100L)
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }
}
