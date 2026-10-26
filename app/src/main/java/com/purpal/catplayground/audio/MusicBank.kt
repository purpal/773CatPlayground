package com.purpal.catplayground.audio

import com.purpal.catplayground.audio.Synth.Wave
import com.purpal.catplayground.audio.Synth.addNoise
import com.purpal.catplayground.audio.Synth.addTone
import com.purpal.catplayground.audio.Synth.midiToFreq

enum class Music { LOBBY, STAGE_MOUSE, STAGE_DOG, STAGE_BEAR, STAGE_CAPY, STAGE_RABBIT, STAGE_PENGUIN, STAGE_KOALA, STAGE_OTTER, STAGE_PANDA,
    STAGE_SLOTH, STAGE_RACCOON, STAGE_MONKEY, STAGE_PIG, BOSS,
}

/** 背景音樂：8 小節循環，用簡單的音序器即時合成。 */
internal object MusicBank {

    fun render(track: Music): ShortArray = when (track) {
        Music.LOBBY -> SoundBank.music()
        Music.STAGE_MOUSE -> mouseStage()
        Music.STAGE_DOG -> dogStage()
        Music.STAGE_BEAR -> bearStage()
        Music.STAGE_CAPY -> capyStage()
        Music.STAGE_RABBIT -> rabbitStage()
        Music.STAGE_PENGUIN -> penguinStage()
        Music.STAGE_KOALA -> koalaStage()
        Music.STAGE_OTTER -> otterStage()
        Music.STAGE_PANDA -> pandaStage()
        Music.STAGE_SLOTH -> slothStage()
        Music.STAGE_RACCOON -> raccoonStage()
        Music.STAGE_MONKEY -> monkeyStage()
        Music.STAGE_PIG -> pigStage()
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

    /** 第 6 關：冰雪南極。叮叮噹噹像冰晶的三角波旋律（E 小調），輕快地滑來滑去。 */
    private fun penguinStage(): ShortArray = Song(bpm = 140.0, bars = 8).run {
        melody(
            "88:1 86:1 83:2 79:2 83:2 " +
                "81:2 79:1 76:1 79:4 " +
                "84:1 83:1 81:2 76:2 81:2 " +
                "79:2 78:2 76:4 " +
                "88:1 86:1 83:2 91:2 88:2 " +
                "86:2 84:1 83:1 81:4 " +
                "79:1 81:1 83:1 84:1 86:2 83:2 " +
                "88:6 r:2",
            volume = 0.2,
            wave = Wave.TRIANGLE,
            octaveUp = 0.08,
        )
        chords(
            listOf(
                intArrayOf(64, 67, 71), intArrayOf(67, 71, 74), intArrayOf(69, 72, 76), intArrayOf(62, 66, 69),
                intArrayOf(64, 67, 71), intArrayOf(60, 64, 67), intArrayOf(57, 60, 64), intArrayOf(59, 63, 66),
            ),
            volume = 0.03,
        )
        bass(intArrayOf(40, 43, 45, 38, 40, 36, 45, 47), 0.28)
        drums(8, kickEveryBeat = false, hatSixteenths = true)
        pcm()
    }

    /** 第 7 關：尤加利樹林。慵懶、慢半拍的旋律（F 大調），像在樹上打瞌睡。 */
    private fun koalaStage(): ShortArray = Song(bpm = 112.0, bars = 8).run {
        melody(
            "77:3 76:1 74:2 72:2 " +
                "69:2 72:2 74:4 " +
                "77:2 79:2 81:2 79:1 77:1 " +
                "76:6 r:2 " +
                "81:3 79:1 77:2 74:2 " +
                "72:2 74:2 77:4 " +
                "79:2 77:1 76:1 74:2 76:2 " +
                "77:6 r:2",
            volume = 0.2,
            wave = Wave.TRIANGLE,
            octaveUp = 0.05,
        )
        chords(
            listOf(
                intArrayOf(65, 69, 72), intArrayOf(62, 65, 69), intArrayOf(58, 62, 65), intArrayOf(60, 64, 67),
                intArrayOf(65, 69, 72), intArrayOf(62, 65, 69), intArrayOf(55, 58, 62), intArrayOf(60, 64, 67),
            ),
            volume = 0.035,
        )
        bass(intArrayOf(41, 38, 34, 36, 41, 38, 43, 36), 0.28)
        drums(8, kickEveryBeat = false, hatSixteenths = false)
        pcm()
    }

    /** 第 8 關：清澈溪流。像水花一樣上下跳動的琶音旋律（A 大調）。 */
    private fun otterStage(): ShortArray = Song(bpm = 150.0, bars = 8).run {
        melody(
            "81:1 85:1 88:1 85:1 83:2 81:2 " +
                "80:1 81:1 83:2 76:4 " +
                "78:1 81:1 85:1 81:1 83:2 85:2 " +
                "88:4 r:2 85:2 " +
                "86:1 85:1 83:1 81:1 83:2 85:2 " +
                "81:2 78:2 76:4 " +
                "78:1 80:1 81:1 83:1 85:2 83:2 " +
                "81:6 r:2",
            volume = 0.2,
            wave = Wave.SQUARE,
            octaveUp = 0.06,
        )
        chords(
            listOf(
                intArrayOf(57, 61, 64), intArrayOf(56, 59, 64), intArrayOf(54, 57, 61), intArrayOf(50, 54, 57),
                intArrayOf(57, 61, 64), intArrayOf(54, 57, 61), intArrayOf(50, 54, 57), intArrayOf(52, 56, 59),
            ),
            volume = 0.035,
        )
        bass(intArrayOf(45, 40, 42, 38, 45, 42, 38, 40), 0.28)
        drums(8, kickEveryBeat = true, hatSixteenths = false)
        pcm()
    }

    /**
     * 第 9 關：翠綠竹林。古箏般的撥弦（三角波＋高八度泛音），A 小調五聲音階（A C D E G），
     * 長音用十六分音符反覆撥奏模仿古箏的輪指。
     */
    private fun pandaStage(): ShortArray = Song(bpm = 120.0, bars = 8).run {
        melody(
            "81:1 84:1 86:2 88:1 86:1 84:2 " +
                "81:1 79:1 76:2 79:0.5 79:0.5 79:0.5 79:0.5 r:2 " +
                "76:1 79:1 81:2 84:1 81:1 79:2 " +
                "76:1 74:1 72:2 76:0.5 76:0.5 76:0.5 76:0.5 r:2 " +
                "88:1 86:1 84:2 86:1 88:1 91:2 " +
                "88:1 86:1 84:1 81:1 84:0.5 84:0.5 84:0.5 84:0.5 r:2 " +
                "81:1 84:1 79:1 76:1 74:2 76:2 " +
                "81:0.5 81:0.5 81:0.5 81:0.5 81:0.5 81:0.5 81:0.5 81:0.5 r:4",
            volume = 0.2,
            wave = Wave.TRIANGLE,
            octaveUp = 0.07,
        )
        chords(
            listOf(
                intArrayOf(57, 64, 69), intArrayOf(55, 62, 67), intArrayOf(57, 64, 69), intArrayOf(52, 59, 64),
                intArrayOf(53, 60, 65), intArrayOf(55, 62, 67), intArrayOf(52, 59, 64), intArrayOf(57, 64, 69),
            ),
            volume = 0.03,
        )
        bass(intArrayOf(45, 43, 45, 40, 41, 43, 40, 45), 0.26)
        drums(8, kickEveryBeat = false, hatSixteenths = false)
        pcm()
    }

    /** 第 10 關：雨林樹冠。慢吞吞、懶洋洋的長音旋律（F 大調）。 */
    private fun slothStage(): ShortArray = Song(bpm = 96.0, bars = 8).run {
        melody(
            "77:4 76:2 74:2 " +
                "72:6 r:2 " +
                "74:2 76:2 77:2 79:2 " +
                "81:6 r:2 " +
                "79:4 77:2 76:2 " +
                "74:4 72:4 " +
                "70:2 72:2 74:2 76:2 " +
                "77:6 r:2",
            volume = 0.2,
            wave = Wave.TRIANGLE,
            octaveUp = 0.05,
        )
        chords(
            listOf(
                intArrayOf(65, 69, 72), intArrayOf(62, 65, 69), intArrayOf(58, 62, 65), intArrayOf(60, 64, 67),
                intArrayOf(65, 69, 72), intArrayOf(62, 65, 69), intArrayOf(58, 62, 65), intArrayOf(60, 64, 67),
            ),
            volume = 0.03,
        )
        bass(intArrayOf(41, 38, 34, 36, 41, 38, 34, 36), 0.26)
        drums(8, kickEveryBeat = false, hatSixteenths = false)
        pcm()
    }

    /** 第 11 關：月夜小鎮。躡手躡腳的斷奏（D 小調），像怪盜在屋頂上偷偷移動。 */
    private fun raccoonStage(): ShortArray = Song(bpm = 128.0, bars = 8).run {
        melody(
            "74:1 r:1 77:1 r:1 81:1 r:1 80:1 81:1 " +
                "77:1 r:1 74:1 r:1 73:2 r:2 " +
                "74:1 r:1 77:1 r:1 82:1 r:1 81:1 79:1 " +
                "77:2 76:2 74:2 r:2 " +
                "70:1 r:1 74:1 r:1 77:1 r:1 76:1 77:1 " +
                "79:1 r:1 77:1 r:1 76:2 r:2 " +
                "74:1 76:1 77:1 79:1 81:1 r:1 80:1 r:1 " +
                "81:2 r:2 74:2 r:2",
            volume = 0.2,
            wave = Wave.SQUARE,
            octaveUp = 0.04,
        )
        chords(
            listOf(
                intArrayOf(62, 65, 69), intArrayOf(62, 65, 69), intArrayOf(58, 62, 65), intArrayOf(57, 61, 64),
                intArrayOf(55, 58, 62), intArrayOf(55, 58, 62), intArrayOf(58, 62, 65), intArrayOf(57, 61, 64),
            ),
            volume = 0.03,
        )
        bass(intArrayOf(38, 38, 34, 33, 43, 43, 34, 33), 0.28)
        drums(8, kickEveryBeat = false, hatSixteenths = true)
        pcm()
    }

    /** 第 12 關：叢林遺跡。蹦蹦跳跳的叢林鼓點（G 大調）。 */
    private fun monkeyStage(): ShortArray = Song(bpm = 150.0, bars = 8).run {
        melody(
            "79:1 83:1 86:1 83:1 84:2 83:2 " +
                "81:1 79:1 76:2 79:4 " +
                "79:1 83:1 86:1 88:1 86:2 83:2 " +
                "81:2 83:2 79:4 " +
                "84:1 84:1 83:1 81:1 83:2 79:2 " +
                "76:1 79:1 81:2 79:4 " +
                "79:1 81:1 83:1 86:1 88:2 86:2 " +
                "91:6 r:2",
            volume = 0.2,
            wave = Wave.SQUARE,
            octaveUp = 0.06,
        )
        chords(
            listOf(
                intArrayOf(55, 59, 62), intArrayOf(52, 55, 59), intArrayOf(48, 52, 55), intArrayOf(50, 54, 57),
                intArrayOf(48, 52, 55), intArrayOf(52, 55, 59), intArrayOf(50, 54, 57), intArrayOf(55, 59, 62),
            ),
            volume = 0.035,
        )
        bass(intArrayOf(43, 40, 36, 38, 36, 40, 38, 43), 0.28)
        drums(8, kickEveryBeat = true, hatSixteenths = true)
        pcm()
    }

    /** 第 13 關（最終關）：泥巴農場。萌王決賽的雄壯進行曲（C 大調）。 */
    private fun pigStage(): ShortArray = Song(bpm = 140.0, bars = 8).run {
        melody(
            "72:2 76:1 79:1 84:3 79:1 " +
                "81:2 79:2 77:2 76:2 " +
                "74:2 77:1 81:1 86:3 84:1 " +
                "83:2 81:2 79:4 " +
                "72:2 76:1 79:1 84:3 86:1 " +
                "88:2 86:2 84:2 81:2 " +
                "79:1 81:1 83:1 84:1 86:2 83:2 " +
                "84:6 r:2",
            volume = 0.2,
            wave = Wave.SAW,
            octaveUp = 0.06,
        )
        chords(
            listOf(
                intArrayOf(60, 64, 67), intArrayOf(57, 60, 64), intArrayOf(50, 53, 57), intArrayOf(55, 59, 62),
                intArrayOf(60, 64, 67), intArrayOf(53, 57, 60), intArrayOf(55, 59, 62), intArrayOf(60, 64, 67),
            ),
            volume = 0.04,
        )
        bass(intArrayOf(36, 45, 38, 43, 36, 41, 43, 36), 0.28)
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
