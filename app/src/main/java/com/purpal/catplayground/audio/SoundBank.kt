package com.purpal.catplayground.audio

import com.purpal.catplayground.audio.Synth.Wave
import com.purpal.catplayground.audio.Synth.addTone
import com.purpal.catplayground.audio.Synth.midiToFreq
import kotlin.random.Random

enum class Sfx {
    CLICK, SELECT, POUR, COMPLETE, WIN, RANK_UP, INVALID,

    // 射擊遊戲
    SHOT, SHOT_HEAVY, ENEMY_HIT, EXPLODE_SMALL, EXPLODE_BIG, ITEM, POWER_UP, PLAYER_HIT,
    ROAR_TIGER, ROAR_PANTHER, BLAST, WARNING, BOSS_DOWN, SONIC, MISSILE, LASER, BEAM, DIALOG,
}

/** 勝利音效中「轟」的時間點，要和勝利橫幅落地的動畫對齊。 */
const val VICTORY_IMPACT_SEC = 0.33

/** 所有音效與背景音樂都在這裡即時合成，不需要任何音檔。 */
object SoundBank {

    fun render(sfx: Sfx): ShortArray = when (sfx) {
        Sfx.CLICK -> click()
        Sfx.SELECT -> select()
        Sfx.POUR -> pour()
        Sfx.COMPLETE -> complete()
        Sfx.WIN -> win()
        Sfx.RANK_UP -> rankUp()
        Sfx.INVALID -> invalid()
        Sfx.SHOT -> ShooterSounds.shot()
        Sfx.SHOT_HEAVY -> ShooterSounds.shotHeavy()
        Sfx.ENEMY_HIT -> ShooterSounds.enemyHit()
        Sfx.EXPLODE_SMALL -> ShooterSounds.explode(big = false)
        Sfx.EXPLODE_BIG -> ShooterSounds.explode(big = true)
        Sfx.ITEM -> ShooterSounds.item()
        Sfx.POWER_UP -> ShooterSounds.powerUp()
        Sfx.PLAYER_HIT -> ShooterSounds.playerHit()
        Sfx.ROAR_TIGER -> ShooterSounds.roar(tiger = true)
        Sfx.ROAR_PANTHER -> ShooterSounds.roar(tiger = false)
        Sfx.BLAST -> ShooterSounds.blast()
        Sfx.WARNING -> ShooterSounds.warning()
        Sfx.BOSS_DOWN -> ShooterSounds.bossDown()
        Sfx.SONIC -> ShooterSounds.sonic()
        Sfx.MISSILE -> ShooterSounds.missile()
        Sfx.LASER -> ShooterSounds.laser()
        Sfx.BEAM -> ShooterSounds.beam()
        Sfx.DIALOG -> ShooterSounds.dialog()
    }

    private fun click(): ShortArray {
        val b = Synth.buffer(0.06)
        addTone(b, 0.0, 0.05, 900.0, 700.0, 0.6)
        return Synth.toPcm(b, 0.5f)
    }

    private fun select(): ShortArray {
        val b = Synth.buffer(0.12)
        addTone(b, 0.0, 0.1, 700.0, 1200.0, 0.6)
        addTone(b, 0.0, 0.1, 1400.0, 2400.0, 0.15)
        return Synth.toPcm(b, 0.55f)
    }

    /** 咕嚕咕嚕的水泡聲加上低通濾波的水流聲。 */
    private fun pour(): ShortArray {
        val seconds = 0.3
        val b = Synth.buffer(seconds)
        val random = Random(7)
        var t = 0.0
        while (t < seconds - 0.06) {
            val f = 260.0 + random.nextDouble() * 220.0
            addTone(b, t, 0.05, f, f * 2.3, 0.55)
            t += 0.045 + random.nextDouble() * 0.03
        }
        var low = 0.0
        for (i in b.indices) {
            low += 0.08 * (random.nextDouble() * 2 - 1 - low)
            val env = (1.0 - i.toDouble() / b.size) * minOf(1.0, i / 600.0)
            b[i] += (low * 0.9 * env).toFloat()
        }
        return Synth.toPcm(b, 0.6f)
    }

    private fun complete(): ShortArray {
        val b = Synth.buffer(0.7)
        addTone(b, 0.0, 0.5, midiToFreq(84), volume = 0.5)
        addTone(b, 0.08, 0.6, midiToFreq(88), volume = 0.45)
        addTone(b, 0.0, 0.6, midiToFreq(96), volume = 0.12)
        return Synth.toPcm(b, 0.6f)
    }

    /** 勝利橫幅落下時的「轟」一聲：低頻下滑的正弦波加一段噪音爆裂。 */
    private fun impact(b: FloatArray, at: Double) {
        addTone(b, at, 0.7, 110.0, 38.0, 1.0, attackSec = 0.002)
        addTone(b, at, 0.25, 220.0, 60.0, 0.4, Wave.TRIANGLE, attackSec = 0.002)
        addTone(b, at, 0.18, 0.0, volume = 0.35, wave = Wave.NOISE, attackSec = 0.001)
    }

    private fun win(): ShortArray {
        val b = Synth.buffer(2.4)
        impact(b, VICTORY_IMPACT_SEC)
        val start = VICTORY_IMPACT_SEC + 0.15
        val notes = intArrayOf(67, 72, 76, 79)
        notes.forEachIndexed { i, n ->
            addTone(b, start + i * 0.1, 0.35, midiToFreq(n), volume = 0.45, wave = Wave.TRIANGLE)
        }
        for (n in intArrayOf(60, 72, 76, 79, 84, 88)) {
            addTone(b, start + 0.42, 1.5, midiToFreq(n), volume = 0.24, wave = Wave.TRIANGLE)
        }
        addTone(b, start + 0.42, 1.5, midiToFreq(96), volume = 0.08)
        return Synth.toPcm(b, 0.85f)
    }

    private fun rankUp(): ShortArray {
        val b = Synth.buffer(3.0)
        impact(b, VICTORY_IMPACT_SEC)
        val start = VICTORY_IMPACT_SEC + 0.15
        val melody = intArrayOf(67, 72, 76, 79, 84, 79, 84, 88)
        melody.forEachIndexed { i, n ->
            addTone(b, start + i * 0.1, 0.3, midiToFreq(n), volume = 0.42, wave = Wave.TRIANGLE)
        }
        for (n in intArrayOf(48, 60, 72, 76, 79, 84, 91)) {
            addTone(b, start + 0.85, 1.6, midiToFreq(n), volume = 0.2, wave = Wave.TRIANGLE)
        }
        for (i in 0 until 12) {
            addTone(b, start + 0.9 + i * 0.09, 0.15, midiToFreq(96 + (i % 3) * 4), volume = 0.07)
        }
        return Synth.toPcm(b, 0.85f)
    }

    private fun invalid(): ShortArray {
        val b = Synth.buffer(0.2)
        addTone(b, 0.0, 0.08, 220.0, 180.0, 0.4, Wave.SQUARE)
        addTone(b, 0.09, 0.1, 180.0, 150.0, 0.4, Wave.SQUARE)
        return Synth.toPcm(b, 0.35f)
    }

    /** 輕快的 8 小節循環背景音樂（C 大調，C-G-Am-F-C-G-F-C）。 */
    fun music(): ShortArray {
        val bpm = 108.0
        val beat = 60.0 / bpm
        val bars = 8
        val b = Synth.buffer(bars * 4 * beat)

        val melody = listOf(
            76 to 1.0, 79 to 1.0, 84 to 1.0, 79 to 1.0,
            74 to 1.0, 79 to 1.0, 83 to 1.5, 81 to 0.5,
            81 to 1.0, 76 to 1.0, 72 to 1.0, 76 to 1.0,
            77 to 1.5, 76 to 0.5, 74 to 1.0, 72 to 1.0,
            76 to 0.5, 77 to 0.5, 79 to 1.0, 84 to 1.0, 79 to 1.0,
            83 to 1.0, 81 to 0.5, 79 to 0.5, 74 to 2.0,
            77 to 1.0, 81 to 1.0, 79 to 1.0, 77 to 1.0,
            76 to 1.0, 74 to 1.0, 72 to 2.0,
        )
        var t = 0.0
        for ((note, beats) in melody) {
            val duration = beats * beat
            addTone(b, t, duration * 0.95, midiToFreq(note), volume = 0.32, attackSec = 0.01)
            addTone(b, t, duration * 0.6, midiToFreq(note + 12), volume = 0.05, wave = Wave.TRIANGLE)
            t += duration
        }

        val chords = listOf(
            intArrayOf(48, 55, 64), intArrayOf(43, 50, 59), intArrayOf(45, 52, 60), intArrayOf(41, 48, 57),
            intArrayOf(48, 55, 64), intArrayOf(43, 50, 59), intArrayOf(41, 48, 57), intArrayOf(48, 55, 64),
        )
        chords.forEachIndexed { bar, chord ->
            val barStart = bar * 4 * beat
            for (eighth in 0 until 8) {
                val note = if (eighth % 4 == 2) chord[1] else chord[0]
                addTone(b, barStart + eighth * beat / 2, beat / 2 * 0.9, midiToFreq(note), volume = 0.2, wave = Wave.TRIANGLE)
            }
            for (beatIndex in 0 until 4) {
                val arp = chord[(beatIndex % 2) + 1] + 12
                addTone(b, barStart + beatIndex * beat + beat / 2, beat / 2, midiToFreq(arp), volume = 0.06)
                if (beatIndex % 2 == 1) {
                    addTone(b, barStart + beatIndex * beat, 0.04, 0.0, volume = 0.05, wave = Wave.NOISE)
                }
            }
        }
        return Synth.toPcm(b, 0.8f)
    }
}
