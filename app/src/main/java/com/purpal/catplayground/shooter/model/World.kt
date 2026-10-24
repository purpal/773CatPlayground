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
    /** 從上一關帶過來的機體狀態。 */
    loadout: Loadout? = null,
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
    private val blastCleared = ArrayList<EnemyBullet>()

    /** 測試用：無敵。 */
    var godMode = false

    /** 打倒菁英怪、Boss 換階段時短暫停格，打擊感更強。 */
    var hitStop = 0f
        private set

    /** 連續擊破數；一段時間沒擊破就歸零。 */
    var combo = 0
        private set
    var comboTime = 0f
        private set
    private var lastDataCue = 0f
    private var lastPokoTime = -1f
    private var lastHitMark = -1f
    private var lastCasing = 0f

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
        loadout?.applyTo(player)
    }

    // ───────────────────────── 主迴圈 ─────────────────────────

    fun update(dt: Float) {
        if (!running) return
        updateParticles(dt)
        if (screenFlash > 0f) screenFlash = (screenFlash - dt * 3f).coerceAtLeast(0f)
        if (hitStop > 0f) {
            hitStop -= dt
            return
        }
        if (ultimateCinematic > 0f) {
            ultimateCinematic -= dt
            if (ultimateCinematic <= 0f) startBlast()
            return
        }
        time += dt
        phaseTime += dt
        scroll += SCROLL_SPEED * dt
        if (shakeTime > 0f) shakeTime -= dt
        if (comboTime > 0f) {
            comboTime -= dt
            if (comboTime <= 0f) combo = 0
        }

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

    /** 目前機體的狀態，破關時存下來給下一關用。 */
    fun loadout(): Loadout = Loadout.of(player)

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
        hitStop = HIT_STOP * 2f
        if (b.phase == 2) {
            // 被打哭了：頭上的配件被打飛出去。
            for ((i, prop) in b.lostProps.withIndex()) {
                val side = if (i % 2 == 0) 1f else -1f
                val px = b.x + side * 30f
                val py = b.y - 110f
                particles += Particle(px, py, side * (260f + random.nextFloat() * 120f), -620f, 2.2f, 1f, 0, ParticleKind.PROP, prop).also {
                    it.spin = side * 540f
                }
            }
            sfx(b.x + 120f, b.y - 120f, SfxText.CRITICAL)
        }
    }

    /**
     * Boss 打到一定血量：「卸甲」把攻擊道具打飛（換成徒手的攻擊模式），
     * 或「裝甲」拿出新武器裝上（更強的攻擊模式）。
     */
    fun onBossBreak(b: Boss) {
        clearBullets(toScore = true)
        hitStop = HIT_STOP * 3f
        shake(0.6f)
        flash(0.7f)
        particles += Particle(b.x, b.y, 0f, 0f, 0.5f, b.radius * 2.6f, 0xFFFFFFFFL, ParticleKind.SHOCKWAVE)
        if (b.breakKind == BreakKind.DISARM) {
            // 丟出去還沒回來的武器也一起消失
            for (e in enemies) if (e is ThrownBone) e.alive = false
            if (b is DogBoss) b.boneOut = false
            explode(b.x + b.handX, b.y + b.handY, big = true)
            particles += Particle(b.x + b.handX, b.y + b.handY, 380f, -700f, 2.4f, 1f, 0, ParticleKind.PROP, b.lostWeapon).also { it.spin = 620f }
            sfx(b.x, b.y - 150f, SfxText.BREAK)
            cue(SoundCue.BREAK)
        } else {
            sparks(b.x, b.y, 24, 0xFFFF7A45L, speed = 700f)
            sfx(b.x, b.y - 150f, SfxText.ARMOR)
            cue(SoundCue.ARMOR_UP)
        }
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
                if (p.mainWeapon == MainWeapon.SPREAD) ejectCasing(p.x, p.y - 30f)
                if (time - lastShotCue > 0.09f) {
                    cue(if (p.mainWeapon == MainWeapon.SPECIAL && p.cat == CatType.ORANGE) SoundCue.SHOT_HEAVY else SoundCue.SHOT)
                    lastShotCue = time
                }
            }
            if (p.mainWeapon == MainWeapon.FOCUS) {
                val hit = Weapons.updateBeam(this, p, dt)
                if (hit != null && every(time, dt, 0.05f)) {
                    sparks(beam.x + jitter(beam.width * 0.3f), beam.top, 2, 0xFFBFF8FFL)
                }
                // 萌點：命中處偶爾浮現貓頭全息標誌
                if (hit != null && every(time, dt, 0.7f)) {
                    particles += Particle(beam.x, beam.top, 0f, -40f, 0.5f, 1f, 0, ParticleKind.CATHOLO)
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
        blastCleared.clear()
        for (b in bullets) {
            if (b.alive && dist2(b.x, b.y, px, py) < r2) blastCleared += b
        }
        bulletsToData(blastCleared)
        for (b in blastCleared) b.alive = false
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
            if (e.alive && e.contactDamage > 0f && player.alive && e.hurtsOnContact &&
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
                particles += Particle(b.x, b.y, 0f, 0f, 0.35f, 1f, 0, ParticleKind.HEXHIT)
                sparks(b.x, b.y, 3, 0xFFFF3D5AL, speed = 260f)
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
            if (l.style == HazardStyle.DIPPER && l.extendTime > 0f && crossed(l.age, dt, l.warnTime + l.extendTime) && l.sweep == 0f) {
                // 蜂蜜棒敲到地面：震動＋濺出一圈蜂蜜
                shake(0.35f)
                cue(SoundCue.EXPLODE_SMALL)
                val tx = l.tipX.coerceIn(30f, WORLD_WIDTH - 30f)
                val ty = l.tipY.coerceIn(30f, height - 30f)
                ring(tx, ty, 10, 170f, random.nextFloat(), BulletStyle.HONEY, Tint.HONEY)
                burst(tx, ty, 0xFFFFD15CL, 10, 260f, ParticleKind.STAR)
            }
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
            item.updateCycle(dt)
            val d2 = dist2(item.x, item.y, p.x, p.y)
            if (item.type == ItemType.DATA) {
                // 資料方塊先彈開，接著一路飛向貓咪。
                if (item.age < DATA_POP || !p.alive) {
                    item.x += item.vx * dt
                    item.y += item.vy * dt
                    item.vx *= 0.9f
                    item.vy *= 0.9f
                } else {
                    val d = sqrt(d2).coerceAtLeast(1f)
                    val speed = 700f + item.age * 900f
                    item.x += (p.x - item.x) / d * minOf(speed * dt, d)
                    item.y += (p.y - item.y) / d * minOf(speed * dt, d)
                }
            } else if (p.alive && d2 < magnetRadius(item).let { it * it }) {
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
            if (item.y > height + 60f || item.age > 12f) item.alive = false
        }
    }

    /** 武器膠囊的吸附距離只有一半，避免不小心換到不想要的武器。 */
    private fun magnetRadius(item: Item): Float = if (item.cycle != null) MAGNET_RADIUS / 2f else MAGNET_RADIUS

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
            ItemType.DATA -> {
                score += DATA_SCORE
                if (time - lastDataCue > 0.06f) {
                    cue(SoundCue.DATA)
                    lastDataCue = time
                }
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
                if (random.nextFloat() < 0.35f && particles.size < MAX_PARTICLES) {
                    particles += Particle(s.x, s.y, jitter(160f), -120f - random.nextFloat() * 120f, 0.45f, 13f, 0xFFFFE07AL, ParticleKind.STAR)
                }
                if (time - lastHitMark > 0.12f) {
                    lastHitMark = time
                    particles += Particle(s.x, s.y, 0f, 0f, 0.22f, 1f, 0, ParticleKind.HITMARK)
                }
                if (e.isBoss && time - lastPokoTime > 1.2f && e.alive) {
                    lastPokoTime = time
                    sfx(s.x + jitter(60f), s.y - 40f, SfxText.HIT)
                }
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
        combo++
        comboTime = COMBO_TIME
        val gained = (e.score * (1f + minOf(combo, 50) * 0.02f)).toInt()
        score += gained
        explode(e.x, e.y, big = e.radius > 40f)
        voxels(e.x, e.y, e.radius, if (e.isElite) 28 else if (e.radius > 40f) 14 else 8)
        particles += Particle(e.x, e.y - 20f, 0f, -90f, 0.9f, if (e.isElite || e.radius > 45f) 1.2f else 0.8f, 0, ParticleKind.SCORE, gained)
        if (e.isElite) {
            hitStop = HIT_STOP
            shake(0.4f)
            flash(0.4f)
            sfx(e.x, e.y - 60f, SfxText.CRITICAL)
        } else if (e.radius > 45f) {
            sfx(e.x + 30f, e.y - 40f, SfxText.TERMINATED)
        }
        if (e is HoneyPot) e.burst(this)
        val drop = e.drop ?: if (random.nextFloat() < 0.3f) ItemType.COIN else null
        if (drop != null) items += Item(e.x, e.y, drop)
    }

    private fun onBossDefeated() {
        val b = boss ?: return
        hitStop = HIT_STOP * 2f
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
        if (toScore) bulletsToData(bullets)
        for (b in bullets) {
            if (particles.size < MAX_PARTICLES) particles += Particle(b.x, b.y, 0f, -60f, 0.5f, 9f, 0xFFFFF59DL, ParticleKind.STAR)
        }
        bullets.clear()
        lasers.clear()
    }

    /** 消彈：子彈分解成資料方塊飛向貓咪（數量有上限，其餘直接加分）。 */
    private fun bulletsToData(list: List<EnemyBullet>) {
        var made = 0
        for (b in list) {
            if (!b.alive) continue
            if (made < MAX_DATA_BITS && items.size < 120) {
                items += Item(b.x, b.y, ItemType.DATA).also {
                    it.variant = random.nextInt(5)
                    val a = random.nextFloat() * TWO_PI
                    it.vx = cos(a) * 220f
                    it.vy = sin(a) * 220f - 80f
                }
                made++
            } else {
                score += DATA_SCORE
            }
        }
        if (made >= 8) sfx(player.x, player.y - 140f, SfxText.DATA)
    }

    private fun updateParticles(dt: Float) {
        for (p in particles) {
            p.age += dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rotation += p.spin * dt
            when (p.kind) {
                ParticleKind.VOXEL, ParticleKind.CASING -> {
                    p.vx *= 0.95f
                    p.vy = p.vy * 0.95f + 900f * dt
                }
                ParticleKind.PROP -> p.vy += 1100f * dt
                ParticleKind.SCORE, ParticleKind.SFX -> p.vy *= 0.9f
                else -> {
                    p.vx *= 0.96f
                    p.vy *= 0.96f
                }
            }
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
        if (e.isElite) sfx(e.x.coerceIn(150f, WORLD_WIDTH - 150f), 160f, SfxText.ELITE)
    }

    /** 漫畫音效字。 */
    fun sfx(x: Float, y: Float, which: Int) {
        // 字樣最寬約 380，靠邊時往內收，避免被畫面切掉
        particles += Particle(x.coerceIn(200f, WORLD_WIDTH - 200f), y.coerceAtLeast(60f), 0f, -50f, 0.8f, 1f, 0, ParticleKind.SFX, which)
    }

    /** 擊破時「數位分解」：衝擊環＋往外飛散的體素方塊（敵人的粉彩色混著青色資料色）。 */
    fun voxels(x: Float, y: Float, radius: Float, count: Int) {
        particles += Particle(x, y, 0f, 0f, 0.35f, radius * 2.2f, 0xFF00E5FFL, ParticleKind.SHOCKWAVE)
        repeat(count) {
            if (particles.size >= MAX_PARTICLES) return
            val a = random.nextFloat() * TWO_PI
            val v = 160f + random.nextFloat() * 460f
            val d = random.nextFloat() * radius * 0.6f
            particles += Particle(
                x + cos(a) * d, y + sin(a) * d, cos(a) * v, sin(a) * v - 220f, 0.6f + random.nextFloat() * 0.5f, 6f + random.nextFloat() * 9f,
                VOXEL_COLORS[random.nextInt(VOXEL_COLORS.size)], ParticleKind.VOXEL,
            ).also {
                it.rotation = random.nextFloat() * 90f
                it.spin = (random.nextFloat() - 0.5f) * 600f
            }
        }
    }

    /** 散彈射擊時退出的彈殼（萌點：印著肉球）。 */
    fun ejectCasing(x: Float, y: Float) {
        if (time - lastCasing < 0.18f || particles.size >= MAX_PARTICLES) return
        lastCasing = time
        val side = if (random.nextBoolean()) 1f else -1f
        particles += Particle(x + side * 18f, y, side * (140f + random.nextFloat() * 80f), -260f - random.nextFloat() * 80f, 0.7f, 1f, 0, ParticleKind.CASING).also {
            it.rotation = random.nextFloat() * 360f
            it.spin = side * 720f
        }
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
            particles += Particle(x, y, cos(a) * 60f, sin(a) * 60f - 40f, 0.7f, 30f * scale, 0xFFFFFFFFL, ParticleKind.SMOKE)
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
        const val HIT_STOP = 0.06f
        const val COMBO_TIME = 1.6f
        const val MAGNET_RADIUS = 200f
        const val DATA_SCORE = 10
        const val DATA_POP = 0.3f
        private const val MAX_DATA_BITS = 60
        val VOXEL_COLORS = longArrayOf(0xFF00E5FFL, 0xFFFFFFFFL, 0xFF2979FFL, 0xFFFFCADAL, 0xFFEDE6F5L, 0xFFFFE07AL)
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
            BulletStyle.BUBBLE -> 15f
        }

        fun defaultDamage(style: BulletStyle): Float = when (style) {
            BulletStyle.BIG_ORB -> 22f
            BulletStyle.BONE -> 17f
            BulletStyle.ORB, BulletStyle.CHEESE -> 14f
            BulletStyle.HONEY -> 13f
            else -> 12f
        }
    }
}
