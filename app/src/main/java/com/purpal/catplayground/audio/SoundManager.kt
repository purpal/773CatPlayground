package com.purpal.catplayground.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.purpal.catplayground.data.AppPrefs
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.concurrent.thread

val LocalSound = staticCompositionLocalOf<SoundManager> { error("SoundManager not provided") }

/**
 * 音效用 SoundPool 播放（可以同時重疊很多聲，射擊遊戲需要），
 * 合成好的 PCM 先寫成 WAV 檔放在 cache 再載入。
 * 背景音樂用串流 AudioTrack 循環播放，可隨時切換曲目。
 */
class SoundManager(context: Context, private val prefs: AppPrefs) {

    var musicOn by mutableStateOf(prefs.musicOn)
        private set
    var sfxOn by mutableStateOf(prefs.sfxOn)
        private set

    private val cacheDir = File(context.cacheDir, "sfx")
    private val soundIds = ConcurrentHashMap<Sfx, Int>()
    private val musicTracks = ConcurrentHashMap<Music, ShortArray>()
    private val renderer = Executors.newSingleThreadExecutor()

    @Volatile private var wantedMusic = Music.LOBBY
    @Volatile private var foreground = false
    @Volatile private var musicGeneration = 0
    @Volatile private var musicPlaying = false
    @Volatile private var released = false

    private val attributes by lazy {
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
    private val format by lazy {
        AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(Synth.SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
    }
    private val pool by lazy {
        SoundPool.Builder().setMaxStreams(16).setAudioAttributes(attributes).build()
    }

    init {
        thread(name = "sound-init") {
            cacheDir.mkdirs()
            for (sfx in Sfx.entries) {
                if (released) return@thread
                val file = File(cacheDir, "${sfx.name.lowercase()}.wav")
                writeWav(file, SoundBank.render(sfx))
                soundIds[sfx] = pool.load(file.path, 1)
            }
            renderMusic(Music.LOBBY)
        }
    }

    /** rate 0.5～2 可以微調音高，讓連續的同一個音效聽起來不單調。 */
    fun play(sfx: Sfx, volume: Float = 1f, rate: Float = 1f) {
        if (!sfxOn || released) return
        val id = soundIds[sfx] ?: return
        pool.play(id, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    /** 切換背景音樂；還沒合成好的曲目會在背景合成，好了就接上。 */
    fun playMusic(track: Music) {
        if (wantedMusic == track) return
        wantedMusic = track
        if (!musicTracks.containsKey(track)) renderer.execute { renderMusic(track) }
    }

    fun toggleMusic() {
        musicOn = !musicOn
        prefs.musicOn = musicOn
        updateMusic()
    }

    fun toggleSfx() {
        sfxOn = !sfxOn
        prefs.sfxOn = sfxOn
    }

    fun onForeground() {
        foreground = true
        updateMusic()
    }

    fun onBackground() {
        foreground = false
        updateMusic()
    }

    fun release() {
        released = true
        foreground = false
        updateMusic()
        renderer.shutdownNow()
        pool.release()
    }

    private fun renderMusic(track: Music) {
        if (musicTracks.containsKey(track)) return
        musicTracks[track] = MusicBank.render(track)
        updateMusic()
    }

    @Synchronized
    private fun updateMusic() {
        val shouldPlay = musicOn && foreground && !released && musicTracks.isNotEmpty()
        if (shouldPlay == musicPlaying) return
        musicPlaying = shouldPlay
        val generation = ++musicGeneration
        if (shouldPlay) startMusicLoop(generation)
    }

    private fun startMusicLoop(generation: Int) = thread(name = "music") {
        val minBuffer = AudioTrack.getMinBufferSize(
            Synth.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(maxOf(minBuffer, 8192))
            .build()
        track.setVolume(0.45f)
        track.play()
        val silence = ShortArray(1024)
        var playing: ShortArray? = null
        var position = 0
        while (musicGeneration == generation) {
            val pcm = musicTracks[wantedMusic]
            if (pcm == null) {
                // 新曲目還在合成：先安靜等一下。
                track.write(silence, 0, silence.size)
                continue
            }
            if (pcm !== playing) {
                playing = pcm
                position = 0
            }
            val count = minOf(2048, pcm.size - position)
            track.write(pcm, position, count)
            position += count
            if (position >= pcm.size) position = 0
        }
        track.pause()
        track.flush()
        track.release()
    }

    private fun writeWav(file: File, pcm: ShortArray) {
        val dataBytes = pcm.size * 2
        val buffer = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + dataBytes).put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(Synth.SAMPLE_RATE).putInt(Synth.SAMPLE_RATE * 2).putShort(2).putShort(16)
        buffer.put("data".toByteArray()).putInt(dataBytes)
        for (sample in pcm) buffer.putShort(sample)
        file.writeBytes(buffer.array())
    }
}
