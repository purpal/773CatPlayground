package com.purpal.catplayground.divine.ui

import androidx.compose.ui.graphics.Color
import com.purpal.catplayground.divine.model.Battle
import com.purpal.catplayground.divine.model.Cards
import com.purpal.catplayground.divine.model.Faction
import com.purpal.catplayground.divine.model.Fx
import com.purpal.catplayground.divine.model.FxType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

internal enum class PKind { SPARK, RING, GLOW, TEXT, BOLT, LINE, PILLAR, SLASH, PUFF, WAVE, FLAME, PLUS, STAR, LANE }

/** 一個特效粒子。座標是戰場的世界座標。 */
internal class Particle(
    val kind: PKind,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val life: Float,
    val size: Float,
    val color: Color,
    val text: String = "",
    val x2: Float = 0f,
    val y2: Float = 0f,
    val seed: Int = 0,
    val gravity: Float = 0f,
    val delay: Float = 0f,
) {
    var age = -delay
    val t: Float get() = (age / life).coerceIn(0f, 1f)
}

/**
 * 戰鬥特效：把模擬丟出來的事件（[Fx]）變成粒子，每幀更新。只管畫面，不影響戰鬥結果。
 */
internal class DivineFx {
    val particles = ArrayList<Particle>()
    private val random = Random(11)
    var shake = 0f
        private set
    /** 全螢幕閃光的顏色與強度（0～1）。 */
    var flashColor = Color.White
        private set
    var flash = 0f
        private set
    /** 節拍的脈動（剛敲下去是 1，之後衰減）。 */
    var beatPulse = 0f
        private set

    private fun add(p: Particle) {
        if (particles.size < MAX) particles += p
    }

    private fun rnd(a: Float, b: Float) = a + random.nextFloat() * (b - a)

    private fun sparks(x: Float, y: Float, n: Int, color: Color, speed: Float = 420f, life: Float = 0.4f, up: Boolean = false) {
        repeat(n) {
            val a = if (up) rnd(-2.6f, -0.5f) else rnd(0f, 6.283f)
            val s = rnd(speed * 0.4f, speed)
            add(Particle(PKind.SPARK, x, y, cos(a) * s, sin(a) * s, rnd(life * 0.6f, life), rnd(5f, 9f), color, gravity = 500f))
        }
    }

    private fun puffs(x: Float, y: Float, n: Int, color: Color, spread: Float = 160f, size: Float = 60f) {
        repeat(n) {
            val a = rnd(0f, 6.283f)
            val s = rnd(spread * 0.3f, spread)
            add(Particle(PKind.PUFF, x, y, cos(a) * s, sin(a) * s - 40f, rnd(0.4f, 0.7f), rnd(size * 0.6f, size), color))
        }
    }

    private fun flashScreen(color: Color, strength: Float) {
        if (strength >= flash) {
            flash = strength
            flashColor = color
        }
    }

    private fun bolt(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float, life: Float = 0.28f, delay: Float = 0f) {
        add(Particle(PKind.BOLT, x1, y1, life = life, size = width, color = color, x2 = x2, y2 = y2, seed = random.nextInt(), delay = delay))
        add(Particle(PKind.GLOW, x2, y2, life = life + 0.1f, size = width * 14f, color = color, delay = delay))
    }

    fun consume(events: List<Fx>) {
        for (e in events) handle(e)
    }

    private fun handle(e: Fx) {
        val lx = if (e.lane >= 0) Battle.laneX(e.lane.coerceIn(0, Battle.LANES - 1)) else 500f
        when (e.type) {
            FxType.SUMMON -> {
                val gold = Cards.get(e.id)?.faction != Faction.GREEK
                val c = if (gold) Gold else Sky
                add(Particle(PKind.PILLAR, e.x, e.y, life = 0.6f, size = 150f, color = c))
                add(Particle(PKind.RING, e.x, e.y + 46f, life = 0.5f, size = 150f, color = c))
                sparks(e.x, e.y + 30f, 14, c, 520f, 0.6f, up = true)
                shake = maxOf(shake, 5f)
            }
            FxType.SPELL -> spell(e, lx)
            FxType.ENCHANT -> {
                val c = accent(e.id)
                add(Particle(PKind.LANE, lx, 0f, life = 0.7f, size = 1f, color = c))
                for (k in 0 until 3) add(Particle(PKind.RING, lx, 300f + k * 280f, life = 0.6f, size = 190f, color = c, delay = k * 0.08f))
            }
            FxType.EQUIP -> {
                add(Particle(PKind.RING, e.x, e.y, life = 0.45f, size = 130f, color = Gold))
                repeat(8) { add(Particle(PKind.STAR, e.x + rnd(-60f, 60f), e.y + rnd(-60f, 40f), 0f, rnd(-160f, -60f), rnd(0.5f, 0.9f), rnd(14f, 26f), Gold)) }
            }
            FxType.SLASH -> {
                add(Particle(PKind.SLASH, e.x, e.y, life = 0.2f, size = 120f, color = Color.White, seed = random.nextInt(360)))
                sparks(e.x, e.y, 5, Color(0xFFFFE9A0), 380f, 0.3f)
            }
            FxType.SHOT_HIT -> {
                val c = shotColor(e.id)
                add(Particle(PKind.RING, e.x, e.y, life = 0.25f, size = 70f, color = c))
                sparks(e.x, e.y, 5, c, 360f, 0.3f)
            }
            FxType.BOLT -> when (e.id) {
                "thunderbolt" -> {
                    bolt(e.x2 + rnd(-30f, 30f), -80f, e.x2, e.y2, Color(0xFFBFE6FF), 16f, 0.4f)
                    bolt(e.x2 + rnd(-60f, 60f), -80f, e.x2, e.y2, Color.White, 8f, 0.4f, 0.05f)
                    add(Particle(PKind.RING, e.x2, e.y2, life = 0.5f, size = 220f, color = Sky))
                    sparks(e.x2, e.y2, 18, Color(0xFFBFE6FF), 640f, 0.5f)
                    flashScreen(Color(0xFFBFE6FF), 0.5f)
                    shake = maxOf(shake, 14f)
                }
                "chain" -> bolt(e.x, e.y, e.x2, e.y2, Color(0xFF9FE8FF), 6f, 0.2f)
                else -> {
                    add(Particle(PKind.LINE, e.x, e.y, life = 0.25f, size = 12f, color = Color(0xFFFF9A3A), x2 = e.x2, y2 = e.y2))
                    sparks(e.x2, e.y2, 12, Color(0xFFFFB86A), 520f, 0.45f)
                }
            }
            FxType.ENEMY_DIE -> {
                val boss = e.id == "ninetail"
                puffs(e.x, e.y, if (boss) 26 else 6, Color(0xFFB08AF2), if (boss) 420f else 150f, if (boss) 130f else 56f)
                add(Particle(PKind.RING, e.x, e.y, life = 0.35f, size = if (boss) 420f else 110f, color = Color(0xFFE0C8FF)))
                sparks(e.x, e.y, if (boss) 40 else 6, Color(0xFFE0C8FF), if (boss) 900f else 380f, if (boss) 0.9f else 0.35f)
                if (boss) {
                    flashScreen(Color.White, 0.9f)
                    shake = 30f
                }
            }
            FxType.CREATURE_DIE -> {
                puffs(e.x, e.y, 8, Color(0xFFFFF3D6), 170f, 60f)
                sparks(e.x, e.y, 10, Gold, 420f, 0.5f, up = true)
                shake = maxOf(shake, 8f)
            }
            FxType.BREACH -> {
                add(Particle(PKind.RING, e.x, Battle.TEMPLE_Y, life = 0.5f, size = 240f, color = Color(0xFFFF5A5A)))
                add(Particle(PKind.TEXT, e.x, Battle.TEMPLE_Y - 40f, 0f, -90f, 1f, 64f, Color(0xFFFF5A5A), "-${e.value.toInt()}"))
                sparks(e.x, Battle.TEMPLE_Y, 16, Color(0xFFFF8A6A), 600f, 0.5f, up = true)
                flashScreen(Color(0xFFFF2A2A), 0.45f)
                shake = maxOf(shake, 18f)
            }
            FxType.BEAT -> {
                beatPulse = 1f
                add(Particle(PKind.RING, 500f, Battle.TEMPLE_Y + 20f, life = 0.7f, size = 760f, color = Gold.copy(alpha = 0.6f)))
            }
            FxType.HEAL -> {
                repeat(3) { add(Particle(PKind.PLUS, e.x + rnd(-50f, 50f), e.y + rnd(-30f, 40f), 0f, rnd(-150f, -80f), rnd(0.5f, 0.8f), rnd(16f, 24f), Color(0xFF7FFFA8))) }
                add(Particle(PKind.TEXT, e.x + rnd(-20f, 20f), e.y - 70f, 0f, -70f, 0.7f, 40f, Color(0xFF7FFFA8), "+${statText(e.value)}"))
            }
            FxType.NUMBER -> add(Particle(PKind.TEXT, e.x + rnd(-26f, 26f), e.y - 10f, rnd(-30f, 30f), -120f, 0.6f, if (e.value >= 5f) 54f else 42f, if (e.value >= 5f) Color(0xFFFFE45C) else Color.White, statText(e.value)))
            FxType.ENEMY_HIT_CREATURE -> {
                add(Particle(PKind.TEXT, e.x + rnd(-24f, 24f), e.y - 60f, rnd(-30f, 30f), -110f, 0.6f, 40f, Color(0xFFFF6A6A), "-${statText(e.value)}"))
                sparks(e.x, e.y - 20f, 4, Color(0xFFFF8A8A), 320f, 0.3f)
            }
            FxType.KNOCKBACK -> sparks(e.x, e.y, 6, Color(0xFFBFF3FF), 420f, 0.4f, up = true)
            FxType.FOXFIRE -> {
                for (k in 0 until 9) add(Particle(PKind.FLAME, lx + rnd(-70f, 70f), 820f + k * 60f, 0f, rnd(-260f, -120f), rnd(0.5f, 0.8f), rnd(70f, 110f), Color(0xFFD08AFF), delay = k * 0.02f))
                add(Particle(PKind.LANE, lx, 0f, life = 0.5f, size = 1f, color = Color(0xFFD08AFF)))
                flashScreen(Color(0xFFB06BFF), 0.3f)
                shake = maxOf(shake, 10f)
            }
            FxType.BOSS_VANISH, FxType.BOSS_APPEAR -> {
                puffs(e.x, e.y, 22, Color(0xFFD08AFF), 420f, 130f)
                add(Particle(PKind.RING, e.x, e.y, life = 0.6f, size = 460f, color = Color(0xFFFF7ACD)))
                shake = maxOf(shake, 12f)
            }
            FxType.REVIVE -> {
                add(Particle(PKind.PILLAR, e.x, e.y, life = 0.9f, size = 190f, color = Gold))
                add(Particle(PKind.TEXT, e.x, e.y - 110f, 0f, -60f, 1.1f, 56f, Gold, "復活！"))
                sparks(e.x, e.y, 24, Gold, 620f, 0.7f, up = true)
                flashScreen(Gold, 0.35f)
            }
            FxType.BUFF -> {
                add(Particle(PKind.TEXT, e.x, e.y - 90f, 0f, -70f, 0.8f, 40f, Color(0xFFFFB86A), "攻擊↑"))
                add(Particle(PKind.RING, e.x, e.y, life = 0.4f, size = 110f, color = Color(0xFFFFB86A)))
            }
            FxType.ARROW -> add(Particle(PKind.LINE, e.x, e.y, life = 0.18f, size = 6f, color = Color(0xFFBFFFF0), x2 = e.x2, y2 = e.y2))
            FxType.STUN -> if (e.value >= 1f) add(Particle(PKind.RING, e.x, e.y, life = 0.35f, size = 90f, color = Color(0xFF9FD8FF)))
            FxType.TELEGRAPH, FxType.INCENSE, FxType.DRAW -> Unit
        }
    }

    private fun spell(e: Fx, lx: Float) {
        val c = accent(e.id)
        when (e.id) {
            "wuleifu" -> {
                for (k in 0 until 5) {
                    val x = lx + rnd(-70f, 70f)
                    bolt(x + rnd(-40f, 40f), -60f, x, rnd(120f, 1000f), Color(0xFFFFF3A0), 10f, 0.3f, k * 0.05f)
                }
                add(Particle(PKind.LANE, lx, 0f, life = 0.5f, size = 1f, color = Color(0xFFFFE45C)))
                flashScreen(Color(0xFFFFF3A0), 0.35f)
                shake = maxOf(shake, 10f)
            }
            "dingshen" -> {
                add(Particle(PKind.LANE, lx, 0f, life = 0.8f, size = 1f, color = Color(0xFF7FD0FF)))
                for (k in 0 until 5) add(Particle(PKind.RING, lx, 120f + k * 200f, life = 0.7f, size = 150f, color = Color(0xFFBFE8FF), delay = k * 0.05f))
            }
            "olympus" -> {
                repeat(46) { add(Particle(PKind.FLAME, rnd(20f, 980f), rnd(-200f, 900f), rnd(-40f, 40f), rnd(300f, 620f), rnd(0.5f, 0.9f), rnd(70f, 130f), Color(0xFFFFB02A), delay = rnd(0f, 0.3f))) }
                flashScreen(Color(0xFFFF8A2A), 0.6f)
                shake = maxOf(shake, 18f)
            }
            "feast" -> {
                add(Particle(PKind.LANE, lx, 0f, life = 0.6f, size = 1f, color = c))
                repeat(16) { add(Particle(PKind.STAR, lx + rnd(-80f, 80f), rnd(820f, 1260f), 0f, rnd(-220f, -100f), rnd(0.6f, 1f), rnd(16f, 30f), Color(0xFFE0A8FF))) }
            }
            "pantao" -> repeat(10) { add(Particle(PKind.STAR, lx + rnd(-70f, 70f), rnd(840f, 1240f), 0f, rnd(-200f, -90f), rnd(0.6f, 1f), rnd(14f, 26f), Color(0xFFFFB0D0))) }
            "tsunami" -> {
                add(Particle(PKind.WAVE, lx, e.y - 60f, 0f, -1500f, 0.7f, 190f, Color(0xFF7FD8FF)))
                shake = maxOf(shake, 10f)
            }
            "zeus" -> {
                repeat(9) { k -> bolt(rnd(60f, 940f), -60f, rnd(60f, 940f), rnd(100f, 900f), Color(0xFFBFE6FF), 11f, 0.32f, k * 0.04f) }
                flashScreen(Color.White, 0.7f)
                shake = maxOf(shake, 20f)
            }
            else -> add(Particle(PKind.LANE, lx, 0f, life = 0.5f, size = 1f, color = c))
        }
    }

    fun update(dt: Float) {
        if (shake > 0f) shake = (shake - dt * 60f).coerceAtLeast(0f)
        if (flash > 0f) flash = (flash - dt * 2.4f).coerceAtLeast(0f)
        if (beatPulse > 0f) beatPulse = (beatPulse - dt * 2.5f).coerceAtLeast(0f)
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.age += dt
            if (p.age < 0f) continue
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += p.gravity * dt
            if (p.kind == PKind.TEXT || p.kind == PKind.PUFF) {
                p.vx *= 0.92f
                p.vy *= 0.92f
            }
            if (p.age >= p.life) it.remove()
        }
    }

    companion object {
        const val MAX = 420
        val Gold = Color(0xFFFFD24A)
        val Sky = Color(0xFF7FD0FF)

        /** 每張卡的代表色（結界的光、法術的閃光）。 */
        fun accent(id: String): Color = when (id) {
            "wuleifu" -> Color(0xFFFFE45C)
            "dingshen" -> Color(0xFF7FD0FF)
            "pantao" -> Color(0xFFFF9AC8)
            "bagua" -> Color(0xFF6FE8B8)
            "ziqi" -> Color(0xFFC88BFF)
            "labyrinth" -> Color(0xFFD8C8A0)
            "sanctuary" -> Color(0xFF8FD8FF)
            "feast" -> Color(0xFFD08AFF)
            "olympus" -> Color(0xFFFFB02A)
            else -> Gold
        }

        /** 遠程攻擊的顏色。 */
        fun shotColor(id: String): Color = when (id) {
            "leigong", "zeus" -> Color(0xFFBFE6FF)
            "nezha" -> Color(0xFFFF8A3A)
            "erlang" -> Color(0xFFDCE8FF)
            "longwang", "poseidon" -> Color(0xFF5CD8FF)
            "artemis" -> Color(0xFFE6ECFA)
            "apollo" -> Color(0xFFFFD24A)
            else -> Color.White
        }
    }
}
