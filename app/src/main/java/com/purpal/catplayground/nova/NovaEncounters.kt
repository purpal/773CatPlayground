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
            11 -> ::NovaRaccoonBoss
            12 -> ::NovaMonkeyBoss
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
            if (original.number == 11) at(18f) { w -> w.spawn(NovaCargoVault(null, 500f, 520f)) }
            if (original.number == 12) at(18f) { w -> w.spawn(NovaReplayNode(null, NovaMimicSnapshot(MainWeapon.SPREAD,w.player.cat,w.player.x,w.player.y),500f,520f)) }
        }
        return StageDef(original.number, original.animal,
            when(original.number) { 5 -> "月躍雷達試驗場"; 6 -> "霜脊破冰航道"; 7 -> "眠巢能源樞紐"; 8 -> "雙潮聲納港灣"; 9 -> "玄竹反彈試驗場"; 10 -> "時差雨林中繼站"; 11 -> "夜匣貨運軌道"; else -> "擬神回聲遺跡" },
            original.theme, original.country, when(original.number) { 11 -> "夜匣指揮官 基德"; 12 -> "擬神駕駛員 吱吱"; else -> original.bossName }, original.bossArt,
            factory, original.outro, script)
    }
}
