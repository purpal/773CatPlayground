package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*

/** Private StageDef copies isolate the new factories and lessons from the original shooter. */
object NovaEncounters {
    fun stage(original: StageDef): StageDef {
        val factory: () -> Boss = when (original.number) {
            5 -> ::NovaRabbitBoss
            6 -> ::NovaPenguinBoss
            else -> return original
        }
        val script = StageScript().apply {
            original.script.events.forEach { at(it.time, it.action) }
            // Brief, isolated demonstrations before the first elite; no extra enemy wave.
            if (original.number == 6) at(18f) { w ->
                NovaIcePylon.row(w, null, w.height * .5f, 500f, lifetime = 3.5f)
            }
        }
        return StageDef(original.number, original.animal,
            if (original.number == 5) "月躍雷達試驗場" else "霜脊破冰航道",
            original.theme, original.country, original.bossName, original.bossArt,
            factory, original.outro, script)
    }
}
