package com.purpal.catplayground.audio

import com.purpal.catplayground.audio.Synth.Wave
import com.purpal.catplayground.audio.Synth.addNoise
import com.purpal.catplayground.audio.Synth.addTone
import com.purpal.catplayground.audio.Synth.midiToFreq
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** 射擊遊戲的音效，全部即時合成。 */
internal object ShooterSounds {

    fun shot(): ShortArray {
        val b = Synth.buffer(0.07)
        addTone(b, 0.0, 0.06, 1500.0, 700.0, 0.5, Wave.SQUARE)
        return Synth.toPcm(b, 0.25f)
    }

    fun shotHeavy(): ShortArray {
        val b = Synth.buffer(0.14)
        addTone(b, 0.0, 0.12, 520.0, 200.0, 0.8, Wave.TRIANGLE)
        addNoise(b, 0.0, 0.05, 0.3, 0.3)
        return Synth.toPcm(b, 0.35f)
    }

    fun enemyHit(): ShortArray {
        val b = Synth.buffer(0.05)
        addTone(b, 0.0, 0.04, 2200.0, 1500.0, 0.4, Wave.SQUARE)
        addNoise(b, 0.0, 0.03, 0.4, 0.6)
        return Synth.toPcm(b, 0.22f)
    }

    fun explode(big: Boolean): ShortArray {
        val length = if (big) 0.9 else 0.4
        val b = Synth.buffer(length)
        addNoise(b, 0.0, length, 1.0, if (big) 0.05 else 0.12, seed = if (big) 3 else 2)
        addTone(b, 0.0, length * 0.8, if (big) 140.0 else 220.0, 40.0, 0.9, attackSec = 0.002)
        if (big) addNoise(b, 0.05, 0.3, 0.4, 0.4, seed = 5)
        return Synth.toPcm(b, if (big) 0.8f else 0.45f)
    }

    fun item(): ShortArray {
        val b = Synth.buffer(0.22)
        addTone(b, 0.0, 0.08, midiToFreq(88), volume = 0.5)
        addTone(b, 0.07, 0.14, midiToFreq(93), volume = 0.5)
        return Synth.toPcm(b, 0.4f)
    }

    fun powerUp(): ShortArray {
        val b = Synth.buffer(0.45)
        intArrayOf(72, 76, 79, 84, 88).forEachIndexed { i, n ->
            addTone(b, i * 0.06, 0.18, midiToFreq(n), volume = 0.45, wave = Wave.SQUARE)
        }
        return Synth.toPcm(b, 0.4f)
    }

    fun playerHit(): ShortArray {
        val b = Synth.buffer(0.3)
        addTone(b, 0.0, 0.25, 260.0, 110.0, 0.7, Wave.SQUARE)
        addNoise(b, 0.0, 0.2, 0.5, 0.25)
        return Synth.toPcm(b, 0.55f)
    }

    /**
     * 大吼：鋸齒波的低吼聲加上快速顫動（喉音），再疊一段呼吸般的噪音。
     * 老虎低沉粗獷，黑豹比較高、尾巴帶著嘶聲。
     */
    fun roar(tiger: Boolean): ShortArray {
        val seconds = 1.5
        val b = Synth.buffer(seconds)
        val from = if (tiger) 170.0 else 240.0
        val to = if (tiger) 65.0 else 95.0
        val random = Random(if (tiger) 11 else 12)
        var phase = 0.0
        var low = 0.0
        val n = b.size
        for (i in 0 until n) {
            val t = i.toDouble() / Synth.SAMPLE_RATE
            val progress = i.toDouble() / n
            // 先快速衝上去再慢慢下降的音高
            val bend = if (progress < 0.15) progress / 0.15 else 1 - (progress - 0.15) / 0.85
            val freq = to + (from - to) * bend.coerceIn(0.0, 1.0)
            phase += freq / Synth.SAMPLE_RATE
            val p = phase - floor(phase)
            val growl = 0.6 + 0.4 * sin(2 * PI * 32 * t)
            val saw = (2 * p - 1) * 0.7 + sin(4 * PI * phase) * 0.3
            low += 0.1 * (random.nextDouble() * 2 - 1 - low)
            val envelope = min(1.0, t / 0.05) * (1 - progress).pow(1.4)
            b[i] += ((saw * growl * 0.8 + low * 1.4) * envelope).toFloat()
        }
        if (!tiger) addNoise(b, 0.6, 0.8, 0.35, 0.7, seed = 13)
        return Synth.toPcm(b, 0.9f)
    }

    fun blast(): ShortArray {
        val b = Synth.buffer(1.4)
        addTone(b, 0.0, 1.3, 110.0, 28.0, 1.0, attackSec = 0.002)
        addNoise(b, 0.0, 1.2, 1.0, 0.04, seed = 21)
        addNoise(b, 0.0, 0.35, 0.6, 0.5, seed = 22)
        for (k in 0 until 6) addTone(b, 0.1 + k * 0.07, 0.25, midiToFreq(84 + k * 2), volume = 0.08)
        return Synth.toPcm(b, 0.95f)
    }

    fun warning(): ShortArray {
        val b = Synth.buffer(2.4)
        for (k in 0 until 3) {
            addTone(b, k * 0.8, 0.38, 880.0, 880.0, 0.4, Wave.SQUARE, pluck = false)
            addTone(b, k * 0.8 + 0.4, 0.38, 660.0, 660.0, 0.4, Wave.SQUARE, pluck = false)
        }
        return Synth.toPcm(b, 0.4f)
    }

    fun bossDown(): ShortArray {
        val b = Synth.buffer(2.2)
        for (k in 0 until 5) {
            addNoise(b, k * 0.32, 0.6, 0.8, 0.08, seed = 30 + k)
            addTone(b, k * 0.32, 0.5, 180.0 - k * 15, 40.0, 0.6)
        }
        addTone(b, 1.5, 0.7, 120.0, 25.0, 1.0)
        return Synth.toPcm(b, 0.9f)
    }

    fun sonic(): ShortArray {
        val b = Synth.buffer(0.3)
        addTone(b, 0.0, 0.28, 300.0, 1000.0, 0.6)
        addTone(b, 0.0, 0.28, 600.0, 2000.0, 0.15, Wave.TRIANGLE)
        return Synth.toPcm(b, 0.35f)
    }

    fun missile(): ShortArray {
        val b = Synth.buffer(0.15)
        addNoise(b, 0.0, 0.14, 0.6, 0.45)
        addTone(b, 0.0, 0.12, 500.0, 1100.0, 0.3, Wave.TRIANGLE)
        return Synth.toPcm(b, 0.25f)
    }

    fun laser(): ShortArray {
        val b = Synth.buffer(0.8)
        addTone(b, 0.0, 0.75, 250.0, 1300.0, 0.5, Wave.SAW, pluck = false)
        addTone(b, 0.0, 0.75, 500.0, 2600.0, 0.15, pluck = false)
        return Synth.toPcm(b, 0.35f)
    }

    /** 光束的嗡嗡聲（持續射擊時每隔一小段重播）。 */
    fun beam(): ShortArray {
        val b = Synth.buffer(0.2)
        addTone(b, 0.0, 0.2, 180.0, 190.0, 0.4, Wave.SAW, pluck = false)
        addTone(b, 0.0, 0.2, 720.0, 760.0, 0.15, Wave.SQUARE, pluck = false)
        addNoise(b, 0.0, 0.2, 0.15, 0.6)
        return Synth.toPcm(b, 0.3f)
    }

    fun dialog(): ShortArray {
        val b = Synth.buffer(0.05)
        addTone(b, 0.0, 0.04, 1000.0, 1200.0, 0.4, Wave.SQUARE)
        return Synth.toPcm(b, 0.2f)
    }
}
