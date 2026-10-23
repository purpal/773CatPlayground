package com.purpal.catplayground.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.purpal.catplayground.data.AppPrefs
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

val LocalSound = staticCompositionLocalOf<SoundManager> { error("SoundManager not provided") }

class SoundManager(private val prefs: AppPrefs) {

    var musicOn by mutableStateOf(prefs.musicOn)
        private set
    var sfxOn by mutableStateOf(prefs.sfxOn)
        private set

    private val tracks = ConcurrentHashMap<Sfx, AudioTrack>()
    @Volatile private var musicPcm: ShortArray? = null
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

    init {
        thread(name = "sound-init") {
            for (sfx in Sfx.entries) {
                val pcm = SoundBank.render(sfx)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(format)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.size * 2)
                    .build()
                track.write(pcm, 0, pcm.size)
                if (released) track.release() else tracks[sfx] = track
            }
            musicPcm = SoundBank.music()
            updateMusic()
        }
    }

    fun play(sfx: Sfx) {
        if (!sfxOn) return
        val track = tracks[sfx] ?: return
        try {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.reloadStaticData()
            track.play()
        } catch (_: IllegalStateException) {
        }
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
        tracks.values.forEach { it.release() }
        tracks.clear()
    }

    @Synchronized
    private fun updateMusic() {
        val shouldPlay = musicOn && foreground && !released && musicPcm != null
        if (shouldPlay == musicPlaying) return
        musicPlaying = shouldPlay
        val generation = ++musicGeneration
        if (shouldPlay) startMusicLoop(generation, musicPcm!!)
    }

    private fun startMusicLoop(generation: Int, pcm: ShortArray) = thread(name = "music") {
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
        var position = 0
        while (musicGeneration == generation) {
            val count = minOf(2048, pcm.size - position)
            track.write(pcm, position, count)
            position += count
            if (position >= pcm.size) position = 0
        }
        track.pause()
        track.flush()
        track.release()
    }
}
