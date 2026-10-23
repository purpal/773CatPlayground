package com.purpal.catplayground.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** 簡單的軟體合成器：把音符疊加到浮點緩衝區，最後轉成 16-bit PCM。 */
object Synth {
    const val SAMPLE_RATE = 22050

    enum class Wave { SINE, TRIANGLE, SQUARE, NOISE }

    fun buffer(seconds: Double) = FloatArray((seconds * SAMPLE_RATE).toInt())

    fun midiToFreq(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    /**
     * 疊加一個音。`pluck` 為 true 時音量會一路衰減（像木琴、音樂盒），
     * 否則維持音量到結尾前才淡出。
     */
    fun addTone(
        out: FloatArray,
        startSec: Double,
        durationSec: Double,
        freqStart: Double,
        freqEnd: Double = freqStart,
        volume: Double,
        wave: Wave = Wave.SINE,
        attackSec: Double = 0.005,
        pluck: Boolean = true,
    ) {
        val start = (startSec * SAMPLE_RATE).toInt()
        val length = (durationSec * SAMPLE_RATE).toInt()
        val fadeOut = 0.02 * SAMPLE_RATE
        val noise = Random(start)
        var phase = 0.0
        for (i in 0 until length) {
            val index = start + i
            if (index >= out.size) break
            val progress = i.toDouble() / length
            phase += (freqStart + (freqEnd - freqStart) * progress) / SAMPLE_RATE
            val p = phase - floor(phase)
            val sample = when (wave) {
                Wave.SINE -> sin(2 * PI * p)
                Wave.TRIANGLE -> 4 * abs(p - 0.5) - 1
                Wave.SQUARE -> if (p < 0.5) 1.0 else -1.0
                Wave.NOISE -> noise.nextDouble() * 2 - 1
            }
            val attack = min(1.0, i / (attackSec * SAMPLE_RATE))
            val release = if (pluck) (1 - progress).pow(2) else min(1.0, (length - i) / fadeOut)
            out[index] += (sample * attack * release * volume).toFloat()
        }
    }

    fun toPcm(buffer: FloatArray, peak: Float = 0.9f): ShortArray {
        val max = buffer.maxOfOrNull { abs(it) }?.takeIf { it > 0f } ?: 1f
        val gain = peak / max
        return ShortArray(buffer.size) { i ->
            (buffer[i] * gain * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
        }
    }
}
