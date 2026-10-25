package com.purpal.catplayground.audio

import com.purpal.catplayground.audio.Synth.Wave
import com.purpal.catplayground.audio.Synth.addNoise
import com.purpal.catplayground.audio.Synth.addTone
import com.purpal.catplayground.audio.Synth.midiToFreq

enum class Music { LOBBY, STAGE_MOUSE, STAGE_DOG, STAGE_BEAR, STAGE_CAPY, STAGE_RABBIT, BOSS }

/** 背景音樂：8 小節循環，用簡單的音序器即時合成。 */
internal object MusicBank {

    fun render(track: Music): ShortArray = when (track) {
        Music.LOBBY -> SoundBank.music()
        Music.STAGE_MOUSE -> mouseStage()
        Music.STAGE_DOG -> dogStage()
        Music.STAGE_BEAR -> bearStage()
        Music.STAGE_CAPY -> capyStage()
        Music.STAGE_RABBIT -> rabbitStage()
        Music.BOSS -> boss()
    }

    /** 以八分音符為單位的簡單音序器。 */
    private class Song(bpm: Double, bars: Int) {
        val beat = 60.0 / bpm
        val buffer = Synth.buffer(bars * 4 * beat)

        /** "音高:長度" 以空白分隔，長度以八分音符計，r 表示休止。 */
        fun melody(text: String, volume: Double, wave: Wave = Wave.SQUARE, octaveUp: Double = 0.0) {
            var t = 0.0
            for (token in text.trim().split(Regex("\\s+"))) {
                val (note, length) = token.split(":")
                val duration = length.toDouble() * beat / 2
                if (note != "r") {
                    val midi = note.toInt()
                    addTone(buffer, t * 1.0, duration * 0.9, midiToFreq(midi), volume = volume, wave = wave, attackSec = 0.008)
                    if (octaveUp > 0) addTone(buffer, t, duration * 0.6, midiToFreq(midi + 12), volume = octaveUp, attackSec = 0.008)
                }
                t += duration
            }
        }

        /** 每小節一個根音，八分音符跳動的貝斯。 */
        fun bass(roots: IntArray, volume: Double, sixteenths: Boolean = false) {
            val steps = if (sixteenths) 16 else 8
            roots.forEachIndexed { bar, root ->
                for (k in 0 until steps) {
                    val note = if (!sixteenths && k % 2 == 1) root + 12 else root
                    val start = (bar * 4 + k * 4.0 / steps) * beat
                    addTone(buffer, start, 4.0 / steps * beat * 0.85, midiToFreq(note), volume = volume, wave = Wave.TRIANGLE)
                }
            }
        }

        fun chords(chords: List<IntArray>, volume: Double) {
            chords.forEachIndexed { bar, notes ->
                for (beatIndex in 0 until 4) {
                    for (n in notes) {
                        addTone(buffer, (bar * 4 + beatIndex + 0.5) * beat, beat * 0.4, midiToFreq(n), volume = volume, wave = Wave.SQUARE)
                    }
                }
            }
        }

        fun drums(bars: Int, kickEveryBeat: Boolean, hatSixteenths: Boolean) {
            for (bar in 0 until bars) {
                for (b in 0 until 4) {
                    val t = (bar * 4 + b) * beat
                    if (kickEveryBeat || b % 2 == 0) addTone(buffer, t, 0.16, 150.0, 45.0, 0.8, attackSec = 0.002)
                    if (b % 2 == 1) {
                        addNoise(buffer, t, 0.13, 0.45, 0.45, seed = bar * 4 + b)
                        addTone(buffer, t, 0.06, 220.0, 180.0, 0.2, Wave.TRIANGLE)
                    }
                    val hats = if (hatSixteenths) 4 else 2
                    for (h in 0 until hats) addNoise(buffer, t + h * beat / hats, 0.03, 0.12, 0.9, seed = 100 + h)
                }
            }
        }

        fun pcm(): ShortArray = Synth.toPcm(buffer, 0.75f)
    }

    private fun mouseStage(): ShortArray = Song(bpm = 144.0, bars = 8).run {
        melody(
            "72:1 r:1 77:1 r:1 81:1 79:1 77:2 " +
                "79:1 r:1 76:1 r:1 72:2 r:2 " +
                "74:1 77:1 81:1 77:1 74:1 77:1 81:2 " +
                "82:2 81:1 79:1 77:2 r:2 " +
                "72:1 r:1 77:1 r:1 81:1 84:1 81:2 " +
                "79:1 81:1 79:1 76:1 72:2 r:2 " +
                "74:1 77:1 82:2 81:1 79:1 77:2 " +
                "76:2 79:2 77:4",
            volume = 0.22,
            octaveUp = 0.06,
        )
        bass(intArrayOf(41, 36, 38, 46, 41, 36, 46, 36), 0.3)
        drums(8, kickEveryBeat = false, hatSixteenths = false)
        pcm()
    }

    private fun dogStage(): ShortArray = Song(bpm = 152.0, bars = 8).run {
        melody(
            "79:2 83:2 86:3 83:1 " +
                "81:2 78:2 74:4 " +
                "76:1 79:1 83:1 79:1 76:1 79:1 83:2 " +
                "84:2 83:1 81:1 79:2 76:2 " +
                "79:1 81:1 83:1 86:1 88:2 86:2 " +
                "86:1 84:1 83:1 81:1 78:4 " +
                "76:2 79:2 84:2 83:2 " +
                "81:2 83:2 86:4",
            volume = 0.2,
            wave = Wave.TRIANGLE,
            octaveUp = 0.08,
        )
        chords(
            listOf(
                intArrayOf(67, 71, 74), intArrayOf(66, 69, 74), intArrayOf(64, 67, 71), intArrayOf(64, 67, 72),
                intArrayOf(67, 71, 74), intArrayOf(66, 69, 74), intArrayOf(64, 67, 72), intArrayOf(66, 69, 74),
            ),
            volume = 0.035,
        )
        bass(intArrayOf(43, 38, 40, 36, 43, 38, 36, 38), 0.3)
        drums(8, kickEveryBeat = false, hatSixteenths = true)
        pcm()
    }

    private fun bearStage(): ShortArray = Song(bpm = 132.0, bars = 8).run {
        melody(
            "74:2 77:2 81:3 79:1 " +
                "76:2 72:2 79:4 " +
                "77:1 79:1 81:1 82:1 81:2 77:2 " +
                "79:2 76:2 72:4 " +
                "74:2 77:2 81:2 86:2 " +
                "84:2 81:2 77:4 " +
                "79:1 81:1 83:1 86:1 84:2 83:2 " +
                "81:4 r:2 73:2",
            volume = 0.22,
            octaveUp = 0.05,
        )
        chords(
            listOf(
                intArrayOf(62, 65, 69), intArrayOf(60, 64, 67), intArrayOf(58, 62, 65), intArrayOf(60, 64, 67),
                intArrayOf(62, 65, 69), intArrayOf(65, 69, 72), intArrayOf(67, 71, 74), intArrayOf(69, 73, 76),
            ),
            volume = 0.03,
        )
        bass(intArrayOf(38, 36, 34, 36, 38, 41, 43, 45), 0.3)
        drums(8, kickEveryBeat = false, hatSixteenths = false)
        pcm()
    }

    /** 第 4 關：溫泉河畔。日本「陽音階」的悠哉旋律，節奏保持輕快。 */
    private fun capyStage(): ShortArray = Song(bpm = 124.0, bars = 8).run {
        melody(
            "74:2 76:2 79:2 81:2 " +
                "83:3 81:1 79:4 " +
                "76:2 79:2 81:1 79:1 76:2 " +
                "74:6 r:2 " +
                "79:2 81:2 83:2 86:2 " +
                "88:3 86:1 83:4 " +
                "81:2 79:2 76:2 79:2 " +
                "74:6 r:2",
            volume = 0.2,
            wave = Wave.TRIANGLE,
            octaveUp = 0.07,
        )
        chords(
            listOf(
                intArrayOf(62, 67, 69), intArrayOf(67, 71, 74), intArrayOf(64, 67, 71), intArrayOf(62, 67, 69),
                intArrayOf(67, 71, 74), intArrayOf(71, 74, 79), intArrayOf(69, 74, 76), intArrayOf(62, 67, 69),
            ),
            volume = 0.03,
        )
        bass(intArrayOf(38, 43, 40, 38, 43, 47, 45, 38), 0.28)
        drums(8, kickEveryBeat = false, hatSixteenths = false)
        pcm()
    }

    /** 第 5 關：胡蘿蔔田。蹦蹦跳跳的切分音旋律（大調五聲音階），節奏輕快。 */
    private fun rabbitStage(): ShortArray = Song(bpm = 160.0, bars = 8).run {
        melody(
            "76:1 79:1 r:1 81:1 84:2 81:1 79:1 " +
                "76:1 74:1 76:2 72:4 " +
                "74:1 76:1 r:1 79:1 81:2 79:1 76:1 " +
                "79:6 r:2 " +
                "84:1 86:1 r:1 84:1 81:2 79:1 81:1 " +
                "84:1 81:1 79:2 76:4 " +
                "74:1 76:1 79:1 81:1 79:2 76:1 74:1 " +
                "72:6 r:2",
            volume = 0.2,
            wave = Wave.SQUARE,
            octaveUp = 0.06,
        )
        chords(
            listOf(
                intArrayOf(60, 64, 67), intArrayOf(57, 60, 64), intArrayOf(62, 65, 69), intArrayOf(55, 59, 62),
                intArrayOf(60, 64, 67), intArrayOf(57, 60, 64), intArrayOf(62, 65, 67), intArrayOf(60, 64, 67),
            ),
            volume = 0.035,
        )
        bass(intArrayOf(36, 33, 38, 31, 36, 33, 38, 36), 0.28)
        drums(8, kickEveryBeat = true, hatSixteenths = false)
        pcm()
    }

    private fun boss(): ShortArray = Song(bpm = 168.0, bars = 8).run {
        melody(
            "81:1 81:1 84:1 81:1 88:2 86:1 84:1 " +
                "83:2 81:1 79:1 81:4 " +
                "77:1 77:1 81:1 77:1 84:2 83:1 81:1 " +
                "79:2 77:1 76:1 77:4 " +
                "79:1 79:1 83:1 79:1 86:2 84:1 83:1 " +
                "84:2 83:1 81:1 79:4 " +
                "76:1 80:1 83:1 88:1 86:2 83:2 " +
                "80:2 83:2 88:4",
            volume = 0.2,
            wave = Wave.SAW,
            octaveUp = 0.05,
        )
        chords(
            listOf(
                intArrayOf(57, 64), intArrayOf(57, 64), intArrayOf(53, 60), intArrayOf(53, 60),
                intArrayOf(55, 62), intArrayOf(55, 62), intArrayOf(52, 59), intArrayOf(52, 59),
            ),
            volume = 0.05,
        )
        bass(intArrayOf(33, 33, 29, 29, 31, 31, 28, 28), 0.28, sixteenths = true)
        drums(8, kickEveryBeat = true, hatSixteenths = true)
        pcm()
    }
}
