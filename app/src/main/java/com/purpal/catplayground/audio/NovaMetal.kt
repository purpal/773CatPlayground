package com.purpal.catplayground.audio

import com.purpal.catplayground.shooter.model.Phase
import kotlin.math.*

/** Separate Nova score: palm-muted power chords and bass, with denser double-kick boss phrasing. */
object NovaMetal {
    const val STAGE_BPM=116.0
    const val BOSS_BPM=172.0
    fun track(phase:Phase,hasBoss:Boolean):Music = if(phase in listOf(Phase.WARNING,Phase.BOSS,Phase.BOSS_DEFEATED)||
        (phase in listOf(Phase.PLAYER_DOWN,Phase.CONTINUE)&&hasBoss)) Music.NOVA_BOSS else Music.NOVA_STAGE

    fun render(boss:Boolean):ShortArray {
        val beat=60.0/if(boss)BOSS_BPM else STAGE_BPM
        val out=Synth.buffer(32*beat)
        val roots=intArrayOf(38,38,34,36,38,41,36,37)
        val riff=if(boss)intArrayOf(0,0,7,0,0,0,10,7,0,0,3,0,7,6,3,1)else intArrayOf(0,-1,0,-1,7,-1,-1,-1,0,-1,0,0,3,-1,-1,-1)
        for(bar in 0..7) {
            val root=roots[bar]
            for(step in 0..15) {
                val t=(bar*4+step*.25)*beat
                if(riff[step]>=0) {
                    val note=root+riff[step]
                    guitar(out,t,beat*(if(step%4==0).42 else .2),note,.24)
                    guitar(out,t+.006,beat*.22,note+7,.14)
                    Synth.addTone(out,t,beat*.22,Synth.midiToFreq(root-12),volume=.3,wave=Synth.Wave.TRIANGLE)
                }
                if(step%4==0 || (boss&&step%4==2) || (boss&&bar%2==1&&step>=12)) {
                    Synth.addTone(out,t,.12,140.0,42.0,.7,attackSec=.001)
                    Synth.addNoise(out,t,.018,.16,.85,bar*17+step)
                }
                if(step==4||step==12) {
                    Synth.addNoise(out,t,.14,.48,.72,bar*19+step)
                    Synth.addTone(out,t,.1,185.0,110.0,.18)
                }
                if(step%(if(boss)1 else 2)==0)Synth.addNoise(out,t,.035,.08,.95,bar*31+step)
            }
            // Sparse sustained stage lead; the boss answers with a descending minor-key phrase.
            val lead=if(boss)intArrayOf(12,15,14,12,10,7,6,7)else intArrayOf(12,7)
            for((i,n) in lead.withIndex()) guitar(out,(bar*4+i*4.0/lead.size)*beat,beat*(if(boss).36 else 1.4),root+n,.065)
            if(bar==0||bar==4||bar==7)Synth.addNoise(out,bar*4*beat,.65,.2,.85,bar+91)
        }
        // Remove the distortion DC component before short loop/transition edge ramps.
        for(i in out.indices)out[i]=tanh(out[i]*1.15).toFloat()
        val dc=out.sumOf{it.toDouble()}/out.size
        for(i in out.indices) {
            val edge=minOf(1.0,i/(Synth.SAMPLE_RATE*.008),(out.lastIndex-i)/(Synth.SAMPLE_RATE*.012))
            out[i]=((out[i]-dc)*edge).toFloat()
        }
        return Synth.toPcm(out,.8f)
    }
    private fun guitar(out:FloatArray,start:Double,duration:Double,note:Int,level:Double) {
        val offset=(start*Synth.SAMPLE_RATE).toInt()
        val length=(duration*Synth.SAMPLE_RATE).toInt()
        val hz=Synth.midiToFreq(note)
        var cabinet=0.0
        for(i in 0 until minOf(length,out.size-offset)) {
            val time=i.toDouble()/Synth.SAMPLE_RATE
            val phase=2*PI*hz*time
            val string=sin(phase)+.48*sin(phase*2)+.27*sin(phase*3)+.13*sin(phase*5)
            val distorted=tanh(string*3.8)
            cabinet+=.36*(distorted-cabinet)
            val envelope=minOf(1.0,time/.003)*(1.0-i.toDouble()/length).pow(1.6)
            out[offset+i]+=(cabinet*envelope*level).toFloat()
        }
    }
}
