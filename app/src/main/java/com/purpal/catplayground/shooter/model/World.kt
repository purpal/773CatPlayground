package com.purpal.catplayground.shooter.model

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 射擊遊戲的整個世界狀態與規則，不依賴 Android，可以在單元測試裡直接模擬。
 * 畫面每一幀呼叫 [update]（固定 [STEP]），並讀取欄位來繪製。
 */
class World(
    val stage: StageDef,
    cat: CatType,
    /** 世界高度（寬度固定 1000），依螢幕比例決定。 */
    val height: Float,
    seed: Int = stage.number,
) {
    val random = Random(seed)
    val player = Player(cat)

    val enemies = ArrayList<Enemy>()
    val playerShots = ArrayList<PlayerShot>()
    val bullets = ArrayList<EnemyBullet>()
    val lasers = ArrayList<Laser>()
    val walls = ArrayList<SonicWall>()
    val items = ArrayList<Item>()
    val particles = ArrayList<Particle>()

    /** 這一幀產生的音效，畫面播放後清空。 */
    val cues = ArrayList<SoundCue>()

    // ── 給畫面的輸入 ──
    var moveX = 0f
    var moveY = 0f
    var firing = false

    var phase = Phase.INTRO
        private set
    var phaseTime = 0f
        private set
    var time = 0f
        private set
    var scroll = 0f
        private set
    var score = 0
        private set
    var continuesLeft = MAX_CONTINUES
        private set
    var boss: Boss? = null
        private set
    var shakeTime = 0f
        private set

    /** 大絕動畫剩餘時間；動畫期間世界靜止。 */
    var ultimateCinematic = 0f
        private set
    /** 大絕衝擊波目前的半徑；小於 0 表示沒有衝擊波。 */
    var blastRadius = -1f
        private set
    private val blastHit = HashSet<Int>()

    /** 測試用：無敵。 */
    var godMode = false

    private val script = stage.script.sorted()
    private var scriptIndex = 0
    private var stageTime = 0f
    private var lastShotCue = 0f
    private var lastHitCue = 0f

    val playerMinY: Float get() = 120f
    val playerMaxY: Float get() = height * 0.8f

    /** 畫面在對話、接關、過關時不需要更新世界。 */
    val running: Boolean
        get() = phase != Phase.BOSS_DIALOG && phase != Phase.OUTRO_DIALOG &&
            phase != Phase.CONTINUE && phase != Phase.CLEAR

    init {
        player.y = height + 120f
    }

    // ───────────────────────── 主迴圈 ─────────────────────────

    fun update(dt: Float) {
        if (!running) return
        updateParticles(dt)
        if (ultimateCinematic > 0f) {
            ultimateCinematic -= dt
            if (ultimateCinematic <= 0f) startBlast()
            return
        }
        time += dt
        phaseTime += dt
        scroll += SCROLL_SPEED * dt
        if (shakeTime > 0f) shakeTime -= dt

        when (phase) {
            Phase.INTRO -> {
                player.y = approach(player.y, height * 0.72f, 2.5f, dt)
                if (phaseTime > INTRO_TIME) setPhase(Phase.STAGE)
            }
            Phase.STAGE -> runScript(dt)
            Phase.WARNING -> if (phaseTime > WARNING_TIME && boss == null) spawnBoss()
            Phase.BOSS_DEFEATED -> bossDefeatedSequence()
            Phase.PLAYER_DOWN -> if (phaseTime > 1.6f) setPhase(Phase.CONTINUE)
            else -> Unit
        }

        updatePlayer(dt)
        updatePlayerShots(dt)
        updateEnemies(dt)
        updateBullets(dt)
        updateLasers(dt)
        updateWalls(dt)
        updateItems(dt)
        updateBlast(dt)
        collideShots()
        cleanup()
    }

    private fun setPhase(next: Phase) {
        phase = next
        phaseTime = 0f
        if (next == Phase.WARNING) cue(SoundCue.WARNING)
    }

    private fun runScript(dt: Float) {
        stageTime += dt
        while (scriptIndex < script.size && script[scriptIndex].time <= stageTime) {
            script[scriptIndex].action(this)
            scriptIndex++
        }
        val scriptDone = scriptIndex >= script.size
        val fieldClear = enemies.none { it !is Balloon }
        if (scriptDone && (fieldClear || stageTime > stage.script.length + 8f)) setPhase(Phase.WARNING)
    }

    private fun spawnBoss() {
        val b = stage.createBoss()
        boss = b
        enemies += b
    }

    fun onBossArrived() {
        setPhase(Phase.BOSS_DIALOG)
    }

    /** 對話結束後由畫面呼叫。 */
    fun dialogFinished() {
        when (phase) {
            Phase.BOSS_DIALOG -> setPhase(Phase.BOSS)
            Phase.OUTRO_DIALOG -> setPhase(Phase.CLEAR)
            else -> Unit
        }
    }

    fun onBossPhaseChanged(b: Boss) {
        // 換階段時送一個修理道具，也讓畫面閃一下。
        items += Item(b.x, b.y + 80f, ItemType.REPAIR)
        clearBullets(toScore = true)
        shake(0.4f)
        burst(b.x, b.y, 0xFFFFFFFFL, 24, 400f)
        cue(SoundCue.EXPLODE_BIG)
    }

    private fun bossDefeatedSequence() {
        val b = boss ?: return
        if (phaseTime < 2.4f && every(phaseTime, STEP, 0.18f)) {
            val ex = b.x + (random.nextFloat() - 0.5f) * b.radius * 2f
            val ey = b.y + (random.nextFloat() - 0.5f) * b.radius * 1.6f
            explode(ex, ey, big = random.nextFloat() < 0.4f)
        }
        if (phaseTime >= 2.6f) {
            b.alive = false
            setPhase(Phase.OUTRO_DIALOG)
        }
    }

    // ───────────────────────── 玩家 ─────────────────────────

    private fun updatePlayer(dt: Float) {
        val p = player
        if (p.invulnerable > 0f) p.invulnerable -= dt
        if (p.transformed > 0f) p.transformed -= dt
        if (!p.alive || phase == Phase.INTRO) return
        val len = hypot(moveX, moveY)
        val mx = if (len > 1f) moveX / len else moveX
        val my = if (len > 1f) moveY / len else moveY
        p.vx = mx * p.speed
        p.x = (p.x + p.vx * dt).coerceIn(40f, WORLD_WIDTH - 40f)
        p.y = (p.y + my * p.speed * dt).coerceIn(playerMinY, playerMaxY)

        val canShoot = phase == Phase.STAGE || phase == Phase.BOSS || phase == Phase.WARNING
        if (p.fireTimer > 0f) p.fireTimer -= dt
        if (p.subTimer > 0f) p.subTimer -= dt
        if (firing && canShoot) {
            if (p.fireTimer <= 0f) {
                Weapons.fireMain(this, p)
                if (time - lastShotCue > 0.09f) {
                    cue(if (p.mainWeapon == MainWeapon.SPECIAL && p.cat == CatType.ORANGE) SoundCue.SHOT_HEAVY else SoundCue.SHOT)
                    lastShotCue = time
                }
            }
            if (p.subWeapon != SubWeapon.NONE && p.subTimer <= 0f) Weapons.fireSub(this, p)
        }
    }

    fun damagePlayer(amount: Float) {
        val p = player
        if (!p.alive || p.invulnerable > 0f || godMode || amount <= 0f) return
        p.armor -= amount
        p.invulnerable = 1.2f
        shake(0.25f)
        burst(p.x, p.y, 0xFFFFFFFFL, 10, 260f)
        cue(SoundCue.PLAYER_HIT)
        if (p.armor <= 0f) {
            p.armor = 0f
            p.alive = false
            explode(p.x, p.y, big = true)
            setPhase(Phase.PLAYER_DOWN)
        }
    }

    /** 使用接關：原地復活，護甲全滿，主武器降一級，分數歸零。 */
    fun useContinue(): Boolean {
        if (phase != Phase.CONTINUE || continuesLeft <= 0) return false
        continuesLeft--
        val p = player
        p.alive = true
        p.armor = p.cat.maxArmor
        p.mainLevel = (p.mainLevel - 1).coerceAtLeast(1)
        p.bombs = Player.START_BOMBS
        p.invulnerable = 3f
        score = 0
        clearBullets(toScore = false)
        setPhase(if (boss != null) Phase.BOSS else Phase.STAGE)
        return true
    }

    /** 大絕：最多 3 次，播完變身動畫後放出消除全螢幕子彈的衝擊波。 */
    fun requestUltimate(): Boolean {
        val p = player
        val allowed = phase == Phase.STAGE || phase == Phase.BOSS || phase == Phase.WARNING
        if (!allowed || !p.alive || p.bombs <= 0 || ultimateCinematic > 0f || blastRadius >= 0f) return false
        p.bombs--
        p.transformed = ULTIMATE_CINEMATIC + 3.5f
        p.invulnerable = ULTIMATE_CINEMATIC + 2.5f
        ultimateCinematic = ULTIMATE_CINEMATIC
        cue(SoundCue.ROAR)
        return true
    }

    private fun startBlast() {
        blastRadius = 0f
        blastHit.clear()
        shake(0.8f)
        cue(SoundCue.BOMB_BLAST)
    }

    private fun updateBlast(dt: Float) {
        if (blastRadius < 0f) return
        blastRadius += 2600f * dt
        val r2 = blastRadius * blastRadius
        val px = player.x
        val py = player.y
        for (b in bullets) {
            if (b.alive && dist2(b.x, b.y, px, py) < r2) {
                b.alive = false
                score += 10
                particles += Particle(b.x, b.y, 0f, -60f, 0.6f, 10f, 0xFFFFF59DL, ParticleKind.STAR)
            }
        }
        lasers.forEach { it.alive = false }
        for (e in enemies) {
            if (e.alive && !e.invulnerable && e.id !in blastHit && dist2(e.x, e.y, px, py) < (blastRadius + e.radius).let { it * it }) {
                blastHit += e.id
                hitEnemy(e, if (e.isBoss) 700f else 400f)
            }
        }
        if (blastRadius > height * 1.6f) blastRadius = -1f
    }

    // ───────────────────────── 子彈與敵人 ─────────────────────────

    private fun updatePlayerShots(dt: Float) {
        for (s in playerShots) {
            s.age += dt
            if (s.style == ShotStyle.MISSILE) steerMissile(s, dt)
            s.x += s.vx * dt
            s.y += s.vy * dt
            if (s.style == ShotStyle.SONIC && s.age >= s.burstAt) {
                s.alive = false
                val level = s.wallLevel
                walls += SonicWall(s.x, s.y, radius = 120f + level * 40f, halfAngle = (36f + level * 10f) * DEG)
                cue(SoundCue.SONIC)
            }
            if (s.y < -80f || s.y > height + 80f || s.x < -80f || s.x > WORLD_WIDTH + 80f) s.alive = false
        }
    }

    private fun steerMissile(s: PlayerShot, dt: Float) {
        val target = s.target?.takeIf { it.alive && !it.invulnerable } ?: nearestEnemy(s.x, s.y).also { s.target = it }
        if (target != null && s.age > 0.12f) {
            val want = angleTo(s.x, s.y, target.x, target.y)
            val current = kotlin.math.atan2(s.vy, s.vx)
            var diff = want - current
            while (diff > HALF_PI * 2f) diff -= TWO_PI
            while (diff < -HALF_PI * 2f) diff += TWO_PI
            val turned = current + diff.coerceIn(-7f * dt, 7f * dt)
            val speed = minOf(hypot(s.vx, s.vy) + 900f * dt, 950f)
            s.vx = cos(turned) * speed
            s.vy = sin(turned) * speed
        }
        if (s.age > 3f) s.alive = false
    }

    fun nearestEnemy(x: Float, y: Float): Enemy? =
        enemies.filter { it.alive && !it.invulnerable && it.y > -40f && it !is Balloon }
            .minByOrNull { dist2(x, y, it.x, it.y) }

    private fun updateEnemies(dt: Float) {
        // 用索引走訪：敵人在 update 中可能生出新的敵人（例如 Boss 召喚手下）。
        var i = 0
        while (i < enemies.size) {
            val e = enemies[i]
            if (e.hitFlash > 0f) e.hitFlash -= dt
            if (e.alive && !(e.isBoss && phase == Phase.BOSS_DEFEATED)) e.update(this, dt)
            if (e.alive && e.contactDamage > 0f && player.alive && !e.invulnerable &&
                dist2(e.x, e.y, player.x, player.y) < (e.radius * 0.8f + player.radius).let { it * it }
            ) {
                damagePlayer(e.contactDamage)
            }
            i++
        }
    }

    private fun updateBullets(dt: Float) {
        val p = player
        for (b in bullets) {
            b.age += dt
            if (b.turn != 0f) {
                val c = cos(b.turn * dt)
                val s = sin(b.turn * dt)
                val vx = b.vx * c - b.vy * s
                b.vy = b.vx * s + b.vy * c
                b.vx = vx
            }
            b.vx += b.ax * dt
            b.vy += b.ay * dt
            b.x += b.vx * dt
            b.y += b.vy * dt
            if (b.y > height + 60f || b.y < -300f || b.x < -60f || b.x > WORLD_WIDTH + 60f) {
                b.alive = false
                continue
            }
            if (b.erasable && walls.any { it.alive && it.blocks(b.x, b.y, b.radius) }) {
                b.alive = false
                particles += Particle(b.x, b.y, 0f, 0f, 0.3f, b.radius * 1.6f, 0xFFB388FFL, ParticleKind.RING)
                continue
            }
            if (p.alive && dist2(b.x, b.y, p.x, p.y) < (b.radius * 0.75f + p.radius).let { it * it }) {
                if (p.invulnerable <= 0f && !godMode) b.alive = false
                damagePlayer(b.damage)
            }
        }
    }

    private fun updateLasers(dt: Float) {
        val p = player
        for (l in lasers) {
            l.age += dt
            val owner = l.owner
            if (owner != null) {
                if (!owner.alive) l.alive = false
                l.x = owner.x + l.offsetX
                l.y = owner.y + l.offsetY
            }
            if (l.age >= l.warnTime) l.angle += l.sweep * dt
            if (l.age > l.warnTime + l.activeTime) l.alive = false
            if (l.active && p.alive && l.distance2To(p.x, p.y) < (l.width * 0.4f + p.radius).let { it * it }) {
                damagePlayer(l.damage)
            }
        }
    }

    private fun updateWalls(dt: Float) {
        for (w in walls) {
            w.age += dt
            if (w.age > w.life) w.alive = false
        }
    }

    private fun updateItems(dt: Float) {
        val p = player
        for (item in items) {
            item.age += dt
            val d2 = dist2(item.x, item.y, p.x, p.y)
            if (p.alive && d2 < 200f * 200f) {
                // 靠近時被吸過去。
                val d = sqrt(d2).coerceAtLeast(1f)
                item.x += (p.x - item.x) / d * 520f * dt
                item.y += (p.y - item.y) / d * 520f * dt
            } else {
                item.y += 110f * dt
                item.x += sin(item.age * 2.5f) * 40f * dt
            }
            if (p.alive && d2 < (item.radius + p.radius + 16f).let { it * it }) {
                item.alive = false
                collect(item.type)
            }
            if (item.y > height + 60f) item.alive = false
        }
    }

    /** 吃到道具：同類型升級，不同類型則換過去但保留等級。 */
    fun collect(type: ItemType) {
        val p = player
        when (type) {
            ItemType.SPREAD, ItemType.FOCUS, ItemType.SPECIAL -> {
                val weapon = when (type) {
                    ItemType.SPREAD -> MainWeapon.SPREAD
                    ItemType.FOCUS -> MainWeapon.FOCUS
                    else -> MainWeapon.SPECIAL
                }
                if (p.mainWeapon == weapon) p.mainLevel = (p.mainLevel + 1).coerceAtMost(Player.MAX_MAIN_LEVEL)
                p.mainWeapon = weapon
                score += 500
                cue(SoundCue.POWER_UP)
            }
            ItemType.HOMING, ItemType.SONIC -> {
                val sub = if (type == ItemType.HOMING) SubWeapon.HOMING else SubWeapon.SONIC
                p.subLevel = if (p.subWeapon == sub) (p.subLevel + 1).coerceAtMost(Player.MAX_SUB_LEVEL) else p.subLevel.coerceAtLeast(1)
                p.subWeapon = sub
                score += 500
                cue(SoundCue.POWER_UP)
            }
            ItemType.REPAIR -> {
                p.armor = (p.armor + p.cat.maxArmor * 0.4f).coerceAtMost(p.cat.maxArmor)
                cue(SoundCue.POWER_UP)
                burst(p.x, p.y, 0xFFFF80ABL, 10, 200f, ParticleKind.HEART)
            }
            ItemType.BOMB -> {
                p.bombs = (p.bombs + 1).coerceAtMost(Player.MAX_BOMBS)
                cue(SoundCue.POWER_UP)
            }
            ItemType.COIN -> {
                score += 100
                cue(SoundCue.ITEM)
            }
        }
    }

    private fun collideShots() {
        for (s in playerShots) {
            if (!s.alive || s.style == ShotStyle.SONIC) continue
            for (e in enemies) {
                if (!e.alive || e.invulnerable || e.id in s.hits) continue
                val r = e.radius + s.radius
                if (dist2(s.x, s.y, e.x, e.y) > r * r) continue
                hitEnemy(e, s.damage)
                particles += Particle(s.x, s.y, 0f, -40f, 0.18f, s.radius * 1.4f, 0xFFFFFFFFL, ParticleKind.FLASH)
                if (s.pierce > 0 && !e.isBoss) {
                    s.pierce--
                    s.hits += e.id
                } else {
                    s.alive = false
                    break
                }
            }
        }
    }

    private fun hitEnemy(e: Enemy, damage: Float) {
        if (phase == Phase.BOSS_DEFEATED && e.isBoss) return
        e.hp -= damage
        e.hitFlash = 0.08f
        if (time - lastHitCue > 0.07f) {
            cue(SoundCue.ENEMY_HIT)
            lastHitCue = time
        }
        if (e.hp > 0f) return
        if (e.isBoss) {
            e.hp = 0f
            onBossDefeated()
            return
        }
        e.alive = false
        score += e.score
        explode(e.x, e.y, big = e.radius > 40f)
        if (e is HoneyPot) e.burst(this)
        val drop = e.drop ?: if (random.nextFloat() < 0.3f) ItemType.COIN else null
        if (drop != null) items += Item(e.x, e.y, drop)
    }

    private fun onBossDefeated() {
        val b = boss ?: return
        score += b.score
        clearBullets(toScore = true)
        enemies.filter { !it.isBoss }.forEach { it.alive = false }
        repeat(14) { items += Item(b.x + (random.nextFloat() - 0.5f) * 300f, b.y + random.nextFloat() * 100f, ItemType.COIN) }
        cue(SoundCue.BOSS_DOWN)
        shake(1.2f)
        setPhase(Phase.BOSS_DEFEATED)
    }

    private fun clearBullets(toScore: Boolean) {
        for (b in bullets) {
            if (toScore) score += 10
            particles += Particle(b.x, b.y, 0f, -60f, 0.5f, 9f, 0xFFFFF59DL, ParticleKind.STAR)
        }
        bullets.clear()
        lasers.clear()
    }

    private fun updateParticles(dt: Float) {
        for (p in particles) {
            p.age += dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vx *= 0.96f
            p.vy *= 0.96f
        }
        particles.removeAll { it.age >= it.life }
        if (particles.size > MAX_PARTICLES) particles.subList(0, particles.size - MAX_PARTICLES).clear()
    }

    private fun cleanup() {
        playerShots.removeAll { !it.alive }
        bullets.removeAll { !it.alive }
        lasers.removeAll { !it.alive }
        walls.removeAll { !it.alive }
        items.removeAll { !it.alive }
        enemies.removeAll { !it.alive }
    }

    // ───────────────────────── 給敵人用的工具 ─────────────────────────

    fun spawn(e: Enemy) {
        enemies += e
    }

    fun cue(c: SoundCue) {
        cues += c
    }

    fun shake(seconds: Float) {
        shakeTime = maxOf(shakeTime, seconds)
    }

    fun aimAt(x: Float, y: Float): Float = angleTo(x, y, player.x, player.y)

    fun jitter(amount: Float): Float = (random.nextFloat() * 2f - 1f) * amount

    fun bullet(x: Float, y: Float, vx: Float, vy: Float, style: BulletStyle, color: Long, radius: Float = defaultRadius(style)): EnemyBullet {
        val b = EnemyBullet(x, y, vx, vy, style, color, radius, defaultDamage(style))
        bullets += b
        return b
    }

    fun shoot(x: Float, y: Float, angle: Float, speed: Float, style: BulletStyle, color: Long, radius: Float = defaultRadius(style)): EnemyBullet =
        bullet(x, y, cos(angle) * speed, sin(angle) * speed, style, color, radius)

    fun aimed(x: Float, y: Float, speed: Float, style: BulletStyle, color: Long, radius: Float = defaultRadius(style)) =
        shoot(x, y, aimAt(x, y), speed, style, color, radius)

    fun fan(x: Float, y: Float, center: Float, count: Int, spread: Float, speed: Float, style: BulletStyle, color: Long) {
        for (i in 0 until count) {
            val a = if (count == 1) center else center - spread / 2f + spread * i / (count - 1)
            shoot(x, y, a, speed, style, color)
        }
    }

    fun ring(x: Float, y: Float, count: Int, speed: Float, offset: Float, style: BulletStyle, color: Long) {
        for (i in 0 until count) shoot(x, y, offset + i * TWO_PI / count, speed, style, color)
    }

    fun explode(x: Float, y: Float, big: Boolean) {
        cue(if (big) SoundCue.EXPLODE_BIG else SoundCue.EXPLODE_SMALL)
        particles += Particle(x, y, 0f, 0f, if (big) 0.6f else 0.35f, if (big) 120f else 60f, 0xFFFFF176L, ParticleKind.RING)
        particles += Particle(x, y, 0f, 0f, 0.25f, if (big) 90f else 45f, 0xFFFFFFFFL, ParticleKind.FLASH)
        burst(x, y, 0xFFFFB74DL, if (big) 18 else 9, if (big) 420f else 300f)
        burst(x, y, 0xFFFFF59DL, if (big) 8 else 4, if (big) 260f else 180f, ParticleKind.STAR)
        if (big) shake(0.3f)
    }

    fun puff(x: Float, y: Float, color: Long) = burst(x, y, color, 8, 180f, ParticleKind.SMOKE)

    fun burst(x: Float, y: Float, color: Long, count: Int, speed: Float, kind: ParticleKind = ParticleKind.SPARK) {
        repeat(count) {
            val a = random.nextFloat() * TWO_PI
            val v = speed * (0.4f + random.nextFloat() * 0.6f)
            val size = when (kind) {
                ParticleKind.STAR, ParticleKind.HEART -> 14f
                ParticleKind.SMOKE -> 26f
                else -> 6f
            }
            particles += Particle(x, y, cos(a) * v, sin(a) * v, 0.4f + random.nextFloat() * 0.4f, size, color, kind)
        }
    }

    companion object {
        const val SCROLL_SPEED = 120f
        const val INTRO_TIME = 2.8f
        const val WARNING_TIME = 2.8f
        const val ULTIMATE_CINEMATIC = 1.6f
        const val MAX_CONTINUES = 3
        private const val MAX_PARTICLES = 700

        fun defaultRadius(style: BulletStyle): Float = when (style) {
            BulletStyle.ORB -> 11f
            BulletStyle.BIG_ORB -> 22f
            BulletStyle.RICE -> 9f
            BulletStyle.CHEESE -> 13f
            BulletStyle.BONE -> 14f
            BulletStyle.NOTE -> 12f
            BulletStyle.HONEY -> 12f
            BulletStyle.STAR -> 12f
            BulletStyle.SEED -> 8f
        }

        fun defaultDamage(style: BulletStyle): Float = when (style) {
            BulletStyle.BIG_ORB -> 16f
            BulletStyle.BONE -> 12f
            BulletStyle.ORB, BulletStyle.CHEESE -> 10f
            BulletStyle.HONEY -> 9f
            else -> 8f
        }
    }
}
