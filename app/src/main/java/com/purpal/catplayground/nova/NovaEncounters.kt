package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*

/** Private StageDef copies isolate the new factories and lessons from the original shooter. */
object NovaEncounters {
    fun stage(original: StageDef): StageDef {
        val factory: () -> Boss = when (original.number) {
            5 -> ::NovaRabbitBoss
            6 -> ::NovaPenguinBoss
            7 -> ::NovaKoalaBoss
            8 -> ::NovaOtterBoss
            9 -> ::NovaPandaBoss
            10 -> ::NovaSlothBoss
            else -> return original
        }
        val script = StageScript().apply {
            original.script.events.forEach { at(it.time, it.action) }
            // Brief, isolated demonstrations before the first elite; no extra enemy wave.
            if (original.number == 6) at(18f) { w ->
                NovaIcePylon.row(w, null, w.height * .5f, 500f, lifetime = 3.5f)
            }
            if (original.number == 7) at(18f) { w -> w.spawn(NovaCapacitor(null, -1f, 250f, 500f)) }
            if (original.number == 8) at(18f) { w -> w.spawn(NovaSonarBuoy(null, 750f, 500f)) }
            if (original.number == 9) at(18f) { w -> w.spawn(NovaSupplyCell(null, 280f, 520f)) }
            if (original.number == 10) at(18f) { w -> w.spawn(NovaTimeBeacon(500f, 500f)) }
        }
        return StageDef(original.number, original.animal,
            when(original.number) { 5 -> "月躍雷達試驗場"; 6 -> "霜脊破冰航道"; 7 -> "眠巢能源樞紐"; 8 -> "雙潮聲納港灣"; 9 -> "玄竹反彈試驗場"; else -> "時差雨林中繼站" },
            original.theme, original.country, original.bossName, original.bossArt,
            factory, original.outro, script)
    }
}
