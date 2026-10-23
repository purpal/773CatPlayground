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
    /** 搖桿式輸入（-1～1），測試與自動駕駛用。 */
    var moveX = 0f
    var moveY = 0f
    var firing = false
    /** 觸控板式輸入：手指移動的距離（世界座標），下一步套用後歸零。 */
    private var dragX = 0f
    private var dragY = 0f

    /** 集中型光束。 */
    val beam = BeamState()

    /** 大爆炸時整個畫面閃白的強度（0～1）。 */
    var screenFlash = 0f
        private set

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
        get() = phase != Phase.OUTRO_DIALOG && phase != Phase.CONTINUE && phase != Phase.CLEAR

    init {
        player.y = height + 120f
    }

    // ───────────────────────── 主迴圈 ─────────────────────────

    fun update(dt: Float) {
        if (!running) return
        updateParticles(dt)
        if (screenFlash > 0f) screenFlash = (screenFlash - dt * 3f).coerceAtLeast(0f)
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
        setPhase(Phase.BOSS)
    }

    /** 打倒 Boss 後的對話結束時由畫面呼叫。 */
    fun dialogFinished() {
        if (phase == Phase.OUTRO_DIALOG) setPhase(Phase.CLEAR)
    }

    /** 手指拖曳的距離（世界座標）。 */
    fun drag(dx: Float, dy: Float) {
        dragX += dx
        dragY += dy
    }

    fun onBossPhaseChanged(b: Boss) {
        // 換階段時送一個修理道具，也讓畫面閃一下。
        items += Item(b.x, b.y + 80f, ItemType.REPAIR)
        clearBullets(toScore = true)
        shake(0.4f)
        flash(0.6f)
        explode(b.x, b.y, big = true)
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
        beam.active = false
        val dragDx = dragX
        val dragDy = dragY
        dragX = 0f
        dragY = 0f
        if (!p.alive || phase == Phase.INTRO) return
        val len = hypot(moveX, moveY)
        val mx = if (len > 1f) moveX / len else moveX
        val my = if (len > 1f) moveY / len else moveY
        // 觸控板式：跟著手指走；速度越快（黑貓、護甲完整）同樣的手指距離走得越遠，
        // 每步的最大位移也受速度限制，所以中彈變慢時會感覺得到。
        val gain = DRAG_GAIN * p.speed / REFERENCE_SPEED
        var dx = mx * p.speed * dt + dragDx * gain
        var dy = my * p.speed * dt + dragDy * gain
        val step = hypot(dx, dy)
        val maxStep = p.speed * MAX_DRAG_SPEED_FACTOR * dt
        if (step > maxStep) {
            // 太快的甩動：這一步只走最大距離，剩下的留到後面幾步補上，最後仍會停在手指指到的位置。
            val keep = 1f - maxStep / step
            dragX += dragDx * keep
            dragY += dragDy * keep
            dx = dx / step * maxStep
            dy = dy / step * maxStep
        }
        p.vx = p.vx * 0.6f + dx / dt * 0.4f
        p.x = (p.x + dx).coerceIn(40f, WORLD_WIDTH - 40f)
        p.y = (p.y + dy).coerceIn(playerMinY, playerMaxY)

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
            if (p.mainWeapon == MainWeapon.FOCUS) {
                val hit = Weapons.updateBeam(this, p, dt)
                if (hit != null && every(time, dt, 0.05f)) {
                    sparks(beam.x + jitter(beam.width * 0.3f), beam.top, 2, 0xFF80F0FFL)
                }
                if (every(time, dt, 0.16f)) cue(SoundCue.BEAM)
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
        flash(0.25f)
        sparks(p.x, p.y, 10, 0xFFFFFFFFL)
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
        flash(1f)
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
                damageEnemy(e, if (e.isBoss) 700f else 400f)
            }
        }
        if (blastRadius > height * 1.6f) blastRadius = -1f
    }

    // ───────────────────────── 子彈與敵人 ─────────────────────────

    private fun updatePlayerShots(dt: Float) {
        for (s in playerShots) {
            s.age += dt
            if (s.style == ShotStyle.MISSILE) {
                steerMissile(s, dt)
                if (every(s.age, dt, 0.035f)) particles += Particle(s.x, s.y, 0f, 0f, 0.4f, 16f, 0xFFDDE6EEL, ParticleKind.SMOKE)
            }
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
                damageEnemy(e, s.damage)
                particles += Particle(s.x, s.y, 0f, -40f, 0.15f, s.radius * 2.2f, 0xFFFFF3C4L, ParticleKind.FLASH)
                if (random.nextFloat() < 0.5f) sparks(s.x, s.y, 2, 0xFFFFE082L)
                if (s.style == ShotStyle.MISSILE) explode(s.x, s.y, big = false, quiet = true)
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

    fun damageEnemy(e: Enemy, damage: Float) {
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
        flash(1f)
        setPhase(Phase.BOSS_DEFEATED)
    }

    private fun clearBullets(toScore: Boolean) {
        for (b in bullets) {
            if (toScore) score += 10
            if (particles.size < MAX_PARTICLES) particles += Particle(b.x, b.y, 0f, -60f, 0.5f, 9f, 0xFFFFF59DL, ParticleKind.STAR)
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

    /**
     * 爆炸：閃光 → 數團火球 → 火花四射 → 煙 → 衝擊波圈。大爆炸還會震動、閃白。
     */
    fun explode(x: Float, y: Float, big: Boolean, quiet: Boolean = false) {
        if (!quiet) cue(if (big) SoundCue.EXPLODE_BIG else SoundCue.EXPLODE_SMALL)
        val scale = if (big) 1.8f else 1f
        particles += Particle(x, y, 0f, 0f, 0.18f, 70f * scale, 0xFFFFFFFFL, ParticleKind.FLASH)
        val fireballs = if (big) 7 else if (quiet) 2 else 4
        repeat(fireballs) {
            val a = random.nextFloat() * TWO_PI
            val d = random.nextFloat() * 22f * scale
            val v = 40f + random.nextFloat() * 80f * scale
            particles += Particle(
                x + cos(a) * d, y + sin(a) * d, cos(a) * v, sin(a) * v - 30f,
                0.35f + random.nextFloat() * 0.25f * scale, (34f + random.nextFloat() * 26f) * scale, 0xFFFFA726L, ParticleKind.FIRE,
            )
        }
        if (quiet) return
        sparks(x, y, if (big) 16 else 8, 0xFFFFE082L, speed = if (big) 900f else 600f)
        repeat(if (big) 5 else 2) {
            val a = random.nextFloat() * TWO_PI
            particles += Particle(x, y, cos(a) * 60f, sin(a) * 60f - 40f, 0.9f, 30f * scale, 0xFF8D8098L, ParticleKind.SMOKE)
        }
        particles += Particle(x, y, 0f, 0f, if (big) 0.55f else 0.35f, (if (big) 150f else 70f), 0xFFFFE0A0L, ParticleKind.RING)
        if (big) {
            shake(0.3f)
            flash(0.35f)
            burst(x, y, 0xFFFFF59DL, 6, 260f, ParticleKind.STAR)
        }
    }

    /** 細長的火花（畫的時候沿著速度方向拉成線）。 */
    fun sparks(x: Float, y: Float, count: Int, color: Long, speed: Float = 500f) {
        repeat(count) {
            if (particles.size >= MAX_PARTICLES) return
            val a = random.nextFloat() * TWO_PI
            val v = speed * (0.4f + random.nextFloat() * 0.6f)
            particles += Particle(x, y, cos(a) * v, sin(a) * v, 0.2f + random.nextFloat() * 0.25f, 3f + random.nextFloat() * 3f, color, ParticleKind.SPARK)
        }
    }

    fun flash(amount: Float) {
        screenFlash = maxOf(screenFlash, amount)
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
        const val WARNING_TIME = 2.2f
        const val ULTIMATE_CINEMATIC = 1.6f

        /** 手指移動 1 單位，速度為 REFERENCE_SPEED 的機體移動 DRAG_GAIN 單位。 */
        const val DRAG_GAIN = 1.3f
        const val REFERENCE_SPEED = 660f
        /** 每秒最多移動 speed × 這個倍數。 */
        const val MAX_DRAG_SPEED_FACTOR = 5f
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
            BulletStyle.SEED -> 10f
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
