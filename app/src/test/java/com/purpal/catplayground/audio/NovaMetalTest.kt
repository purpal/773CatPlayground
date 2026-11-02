package com.purpal.catplayground.audio

import com.purpal.catplayground.shooter.model.Phase
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class NovaMetalTest {
    @Test fun scoreSwitchesForBossWarningAndRetries() {
        assertEquals(Music.NOVA_STAGE,NovaMetal.track(Phase.STAGE,false))
        for(p in listOf(Phase.WARNING,Phase.BOSS,Phase.BOSS_DEFEATED,Phase.CONTINUE))assertEquals(Music.NOVA_BOSS,NovaMetal.track(p,true))
        assertEquals(Music.NOVA_STAGE,NovaMetal.track(Phase.CONTINUE,false))
    }
    @Test fun metalLoopsHaveHeadroomAndSilentEdgesWithFasterBossTempo() {
        val stage=MusicBank.render(Music.NOVA_STAGE);val boss=MusicBank.render(Music.NOVA_BOSS)
        assertTrue(boss.size<stage.size)
        for(pcm in listOf(stage,boss)) {
            assertTrue(pcm.size>Synth.SAMPLE_RATE*10)
            assertEquals(0,pcm.first().toInt());assertEquals(0,pcm.last().toInt())
            assertTrue(pcm.maxOf{abs(it.toInt())} in 20000..27000)
            assertTrue(abs(pcm.map{it.toDouble()}.average())<100)
            val rms=kotlin.math.sqrt(pcm.sumOf{it.toDouble()*it}/pcm.size)
            assertTrue("audible mix RMS: $rms",rms>1500)
        }
    }
}
