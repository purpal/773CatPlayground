package com.purpal.catplayground.shooter.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sign

class WorldTest {

    /** 自動駕駛：跟著最近的敵人左右移動並持續射擊，對話直接跳過。 */
    private fun autoplay(w: World, maxSeconds: Float): Float {
        w.firing = true
        var t = 0f
        while (t < maxSeconds && w.phase != Phase.CLEAR) {
            if (w.phase == Phase.OUTRO_DIALOG) w.dialogFinished()
            val target = w.boss?.takeIf { !it.entering } ?: w.nearestEnemy(w.player.x, w.player.y)
            w.moveX = if (target != null && abs(target.x - w.player.x) > 20f) sign(target.x - w.player.x) else 0f
            // 肉球閃電是近身武器：要靠近目標下方才打得到
            val melee = w.player.cat == CatType.BLACK && w.player.mainWeapon == MainWeapon.SPECIAL
            val wantY = if (melee && target != null) target.y + target.radius + 70f else w.height * 0.72f
            w.moveY = if (abs(wantY - w.player.y) > 20f) sign(wantY - w.player.y) else 0f
            w.update(STEP)
            w.cues.clear()
            t += STEP
        }
        return t
    }

    @Test
    fun everyStageCanBeClearedWithBaseWeapons() {
        for (entry in Stages.all.filter { it.stage != null }) {
            for (cat in CatType.entries) {
                val w = World(entry.stage!!, cat, height = 2000f)
                w.godMode = true
                val time = autoplay(w, 600f)
                assertEquals("stage ${entry.number} with $cat", Phase.CLEAR, w.phase)
                assertTrue("stage ${entry.number} with $cat took $time s", time < 400f)
            }
        }
    }

    @Test
    fun bossFightLengthIsReasonableAtMaxPower() {
        for (entry in Stages.all.filter { it.stage != null }) {
            val w = World(entry.stage!!, CatType.ORANGE, height = 2000f)
            w.godMode = true
            w.player.mainWeapon = MainWeapon.FOCUS
            w.player.mainLevel = Player.MAX_MAIN_LEVEL
            var t = 0f
            autoplayUntil(w) { it.phase == Phase.BOSS }
            w.firing = true
            while (w.phase == Phase.BOSS && t < 400f) {
                val b = w.boss!!
                w.moveX = if (abs(b.x - w.player.x) > 20f) sign(b.x - w.player.x) else 0f
                w.update(STEP)
                w.cues.clear()
                t += STEP
            }
            // Boss 血量加倍後，滿火力時 boss 戰大約 40～120 秒。
            assertTrue("stage ${entry.number} boss took $t s", t in 30f..140f)
        }
    }

    private fun autoplayUntil(w: World, done: (World) -> Boolean) {
        var guard = 0
        w.firing = true
        while (!done(w) && guard++ < 60 * 400) {
            val target = w.nearestEnemy(w.player.x, w.player.y)
            w.moveX = if (target != null && abs(target.x - w.player.x) > 20f) sign(target.x - w.player.x) else 0f
            w.update(STEP)
            w.cues.clear()
        }
    }

    @Test
    fun sameWeaponItemStacksAndOtherTypeSwitchesKeepingLevel() {
        val w = World(Stages.mouse, CatType.BLACK, 2000f)
        w.collect(ItemType.SPREAD)
        w.collect(ItemType.SPREAD)
        assertEquals(MainWeapon.SPREAD, w.player.mainWeapon)
        assertEquals(3, w.player.mainLevel)
        w.collect(ItemType.FOCUS)
        assertEquals(MainWeapon.FOCUS, w.player.mainWeapon)
        assertEquals(3, w.player.mainLevel)
        repeat(5) { w.collect(ItemType.FOCUS) }
        assertEquals(Player.MAX_MAIN_LEVEL, w.player.mainLevel)

        w.collect(ItemType.SONIC)
        assertEquals(SubWeapon.SONIC, w.player.subWeapon)
        assertEquals(1, w.player.subLevel)
        repeat(4) { w.collect(ItemType.SONIC) }
        assertEquals(Player.MAX_SUB_LEVEL, w.player.subLevel)

        repeat(5) { w.collect(ItemType.BOMB) }
        assertEquals(Player.MAX_BOMBS, w.player.bombs)
    }

    private fun worldInStage(): World {
        val w = World(Stages.mouse, CatType.ORANGE, 2000f)
        while (w.phase != Phase.STAGE) w.update(STEP)
        return w
    }

    @Test
    fun ultimateClearsEveryBulletAndLaser() {
        val w = worldInStage()
        repeat(50) { w.shoot(100f + it * 15f, 200f, HALF_PI, 50f, BulletStyle.ORB, Tint.RED) }
        w.lasers += Laser(500f, 0f, HALF_PI, 2000f, 30f, 0.5f, 5f, Tint.RED, 20f)
        val bombs = w.player.bombs
        assertTrue(w.requestUltimate())
        assertEquals(bombs - 1, w.player.bombs)
        repeat(60 * 3) { w.update(STEP) }
        assertTrue(w.bullets.none { it.y < 400f })
        assertTrue(w.lasers.isEmpty())
    }

    @Test
    fun ultimateNeedsStock() {
        val w = worldInStage()
        w.player.bombs = 0
        assertFalse(w.requestUltimate())
    }

    @Test
    fun sonicWallBlocksBulletsButNotLasers() {
        val wall = SonicWall(500f, 800f, radius = 160f, halfAngle = 50f * DEG)
        assertTrue(wall.blocks(500f, 800f, 10f))
        assertFalse(wall.blocks(500f, 500f, 10f))

        val w = worldInStage()
        w.player.x = 500f
        w.player.y = 1400f
        w.walls += SonicWall(500f, 1000f, 160f, 50f * DEG)
        val bullet = w.bullet(500f, 900f, 0f, 300f, BulletStyle.ORB, Tint.RED)
        val laser = Laser(500f, 0f, HALF_PI, 3000f, 30f, 0f, 2f, Tint.RED, 20f)
        w.lasers += laser
        val armor = w.player.armor
        repeat(30) { w.update(STEP) }
        assertFalse(bullet.alive)
        assertTrue(w.player.armor < armor)
    }

    @Test
    fun damageSlowsThePlayerAndRepairRestores() {
        val w = worldInStage()
        val fullSpeed = w.player.speed
        w.damagePlayer(50f)
        assertTrue(w.player.speed < fullSpeed)
        w.collect(ItemType.REPAIR)
        w.collect(ItemType.REPAIR)
        assertEquals(fullSpeed, w.player.speed, 0.01f)
    }

    @Test
    fun continuesRunOutAfterThree() {
        val w = worldInStage()
        repeat(World.MAX_CONTINUES) {
            w.player.invulnerable = 0f
            w.damagePlayer(9999f)
            while (w.phase != Phase.CONTINUE) w.update(STEP)
            w.player.mainLevel = 3
            assertTrue(w.useContinue())
            assertEquals(2, w.player.mainLevel)
            assertEquals(0, w.score)
        }
        w.player.invulnerable = 0f
        w.damagePlayer(9999f)
        while (w.phase != Phase.CONTINUE) w.update(STEP)
        assertEquals(0, w.continuesLeft)
        assertFalse(w.useContinue())
    }

    @Test
    fun dragMovesThePlayerAndFasterCatsMoveFurther() {
        fun moved(cat: CatType): Float {
            val w = World(Stages.mouse, cat, 2000f)
            while (w.phase != Phase.STAGE) w.update(STEP)
            val start = w.player.x
            w.player.x = 300f
            w.drag(100f, 0f)
            repeat(15) { w.update(STEP) }
            return w.player.x - 300f + 0f * start
        }
        val orange = moved(CatType.ORANGE)
        val black = moved(CatType.BLACK)
        // 手指移動 100：橘貓約 110、黑貓約 154，甩太快時會在幾步內補完。
        assertEquals(100f * World.DRAG_GAIN * CatType.ORANGE.baseSpeed / World.REFERENCE_SPEED, orange, 1f)
        assertTrue("black ($black) should move further than orange ($orange)", black > orange)
    }

    @Test
    fun beamHitsTheClosestEnemyAbove() {
        val w = worldInStage()
        w.player.mainWeapon = MainWeapon.FOCUS
        w.player.x = 500f
        w.player.y = 1500f
        val near = Balloon(500f, ItemType.COIN).also { it.y = 900f }
        val far = Balloon(500f, ItemType.COIN).also { it.y = 400f }
        w.spawn(near)
        w.spawn(far)
        w.firing = true
        w.update(STEP)
        assertTrue(w.beam.active)
        assertTrue(near.hp < 26f)
        assertEquals(26f, far.hp, 0.001f)
        assertTrue(w.beam.top > 850f)
    }

    @Test
    fun weaponCapsulesCycleThroughTheirGroup() {
        val main = Item(0f, 0f, ItemType.SPREAD)
        val seen = mutableListOf(main.type)
        repeat(3) {
            repeat(((Item.CYCLE_TIME + 0.01f) / STEP).toInt() + 1) { main.updateCycle(STEP) }
            seen += main.type
        }
        assertEquals(listOf(ItemType.SPREAD, ItemType.FOCUS, ItemType.SPECIAL, ItemType.SPREAD), seen)

        val sub = Item(0f, 0f, ItemType.HOMING)
        repeat(((Item.CYCLE_TIME + 0.01f) / STEP).toInt() + 1) { sub.updateCycle(STEP) }
        assertEquals(ItemType.SONIC, sub.type)

        val repair = Item(0f, 0f, ItemType.REPAIR)
        repeat(300) { repair.updateCycle(STEP) }
        assertEquals(ItemType.REPAIR, repair.type)
    }

    @Test
    fun weaponCapsulesHaveHalfTheMagnetRange() {
        val w = worldInStage()
        w.player.x = 500f
        w.player.y = 1400f
        val weapon = Item(500f, 1250f, ItemType.FOCUS)
        val repair = Item(500f, 1250f, ItemType.REPAIR)
        w.items += weapon
        w.items += repair
        w.update(STEP)
        // 150 單位外：修理道具會被吸過來，武器膠囊不會（往下飄的速度比較慢）。
        assertTrue("repair should be pulled in", repair.y - 1250f > 5f)
        assertTrue("weapon should only drift", weapon.y - 1250f < 3f)
    }

    @Test
    fun meleeOnlyHitsTheExtendedPart() {
        val w = worldInStage()
        w.player.x = 500f
        w.player.y = 900f
        val fork = Laser(500f, 100f, HALF_PI, 1000f, 40f, warnTime = 0.2f, activeTime = 1f, color = Tint.PINK, damage = 30f, style = HazardStyle.FORK, extendTime = 0.4f)
        w.lasers += fork
        val armor = w.player.armor
        repeat((0.3f / STEP).toInt()) { w.update(STEP) }
        assertTrue("still extending: reach ${fork.reach}", fork.reach < 800f)
        assertEquals(armor, w.player.armor, 0.001f)
        repeat((0.4f / STEP).toInt()) { w.update(STEP) }
        assertTrue(w.player.armor < armor)
    }

    private fun bossFight(stage: StageDef): World {
        val w = World(stage, CatType.ORANGE, 2000f)
        w.godMode = true
        autoplayUntil(w) { it.phase == Phase.BOSS }
        return w
    }

    @Test
    fun timidMouseDisarmsEarlyAndLosesItsFork() {
        val w = bossFight(Stages.mouse)
        val boss = w.boss!!
        assertEquals(HazardStyle.FORK, boss.heldProp())
        boss.hp = boss.maxHp * 0.7f
        w.update(STEP)
        assertFalse("70% is above the mouse's break point", boss.broken)
        boss.hp = boss.maxHp * 0.62f
        w.update(STEP)
        assertTrue(boss.broken)
        assertEquals(null, boss.heldProp())
        assertTrue(w.particles.any { it.kind == ParticleKind.PROP && it.tag == LostProp.FORK })
        assertTrue(w.particles.any { it.kind == ParticleKind.SFX && it.tag == SfxText.BREAK })
    }

    @Test
    fun eachBossBreaksAtItsOwnThreshold() {
        assertEquals(0.65f, MouseBoss().breakAt, 0f)
        assertEquals(0.35f, DogBoss().breakAt, 0f)
        assertEquals(0.25f, BearBoss().breakAt, 0f)
        assertEquals(BreakKind.ARMOR, CapyBoss().breakKind)
        val bear = bossFight(Stages.bear).boss!!
        bear.hp = bear.maxHp * 0.3f
        repeat(3) { bear.update(World(Stages.bear, CatType.ORANGE, 2000f), STEP) }
        assertFalse("the tough bear keeps its dipper at 30%", bear.broken)
    }

    @Test
    fun capybaraArmorsUpWithANewWeapon() {
        val w = bossFight(Stages.capybara)
        val boss = w.boss!!
        assertEquals("no weapon before armoring", null, boss.heldProp())
        boss.hp = boss.maxHp * 0.48f
        w.update(STEP)
        assertTrue(boss.broken)
        assertTrue("invulnerable while armoring", boss.invulnerable)
        assertEquals(HazardStyle.WATER, boss.heldProp())
        assertEquals(1, boss.mood())
        // 卸甲／裝甲時有短暫停格，所以多等一點
        repeat(((Boss.ARMOR_TIME + 0.4f) / STEP).toInt()) { w.update(STEP) }
        assertFalse(boss.invulnerable)
        repeat(60 * 6) { w.update(STEP) }
        assertTrue("armored capybara uses its yuzu cannon", w.lasers.any { it.style == HazardStyle.WATER } || w.bullets.isNotEmpty())
    }

    @Test
    fun thrownBoneFliesOutAndComesBack() {
        val w = bossFight(Stages.dog)
        val dog = w.boss as DogBoss
        w.player.x = 300f
        w.player.y = 1500f
        val bone = ThrownBone(dog, w.player.x, w.player.y)
        w.spawn(bone)
        assertTrue(dog.boneOut)
        assertEquals(null, dog.heldProp())
        assertTrue(bone.invulnerable && bone.hurtsOnContact)
        var maxY = 0f
        repeat(((ThrownBone.FLIGHT + 0.2f) / STEP).toInt()) {
            w.update(STEP)
            maxY = maxOf(maxY, bone.y)
        }
        assertTrue("bone should fly toward the player (maxY=$maxY)", maxY > 1000f)
        assertFalse(bone.alive)
        assertFalse(dog.boneOut)
    }

    @Test
    fun cancelledBulletsTurnIntoDataThatFliesToThePlayer() {
        val w = worldInStage()
        repeat(20) { w.shoot(100f + it * 40f, 300f, HALF_PI, 10f, BulletStyle.ORB, Tint.RED) }
        val score = w.score
        assertTrue(w.requestUltimate())
        repeat(60 * 4) { w.update(STEP) }
        assertTrue(w.bullets.isEmpty())
        assertTrue(w.items.none { it.type == ItemType.DATA })
        assertTrue("data bits add score", w.score >= score + 20 * World.DATA_SCORE)
    }

    @Test
    fun loadoutCarriesWeaponsBombsAndArmor() {
        val w = worldInStage()
        w.player.mainWeapon = MainWeapon.SPECIAL
        w.player.mainLevel = 4
        w.player.subWeapon = SubWeapon.SONIC
        w.player.subLevel = 2
        w.player.bombs = 1
        w.damagePlayer(40f)
        val saved = Loadout.decode(w.loadout().encode())!!
        assertEquals(w.loadout(), saved)

        val next = World(Stages.dog, CatType.ORANGE, 2000f, loadout = saved)
        assertEquals(MainWeapon.SPECIAL, next.player.mainWeapon)
        assertEquals(4, next.player.mainLevel)
        assertEquals(SubWeapon.SONIC, next.player.subWeapon)
        assertEquals(2, next.player.subLevel)
        assertEquals(1, next.player.bombs)
        assertEquals(w.player.armorRatio, next.player.armorRatio, 0.001f)

        assertEquals(null, Loadout.decode("garbage"))
        // 護甲至少留 20%
        val low = World(Stages.dog, CatType.BLACK, 2000f, loadout = saved.copy(armorRatio = 0.01f))
        assertEquals(0.2f, low.player.armorRatio, 0.001f)
    }

    @Test
    fun elitesAlwaysDropAWeaponCapsule() {
        val w = worldInStage()
        val elite = EliteMouse(500f)
        w.spawn(elite)
        repeat(60) { w.update(STEP) }
        w.damageEnemy(elite, 9999f)
        assertFalse(elite.alive)
        assertTrue(w.items.any { it.cycle == Item.MAIN_CYCLE })
    }

    @Test
    fun diversRamThePlayerAndSelfDestruct() {
        val w = World(Stages.mouse, CatType.ORANGE, 2000f)
        repeat((World.INTRO_TIME / STEP).toInt() + 5) { w.update(STEP) }
        w.enemies.clear()
        val diver = MouseRunner(w.player.x)
        diver.y = w.player.y - 400f
        w.spawn(diver)
        val armor = w.player.armor
        var guard = 0
        while (diver.alive && guard++ < 600) {
            w.update(STEP)
            w.bullets.clear()
        }
        assertFalse("diver blew up", diver.alive)
        assertTrue("diver never shoots and hurts on contact", w.player.armor < armor)
    }

    @Test
    fun onlyGroundShootersFire() {
        val w = World(Stages.mouse, CatType.ORANGE, 2000f)
        repeat((World.INTRO_TIME / STEP).toInt() + 5) { w.update(STEP) }
        w.enemies.clear()
        w.godMode = true
        w.spawn(MousePlane(fromLeft = true, startY = 300f))
        w.spawn(Bee(500f, 0f))
        repeat(60 * 3) { w.update(STEP) }
        assertTrue("divers fire nothing", w.bullets.isEmpty())
        w.spawn(BallTurret(500f).also { it.y = 300f })
        repeat(60 * 3) { w.update(STEP) }
        assertTrue("ground turret fires", w.bullets.isNotEmpty())
        assertEquals(World.defaultRadius(BulletStyle.ORB) * World.BULLET_SCALE, w.bullets.first().radius, 0.01f)
    }

    @Test
    fun bossBodyIsSolidAndHurts() {
        val w = World(Stages.mouse, CatType.ORANGE, 2000f)
        val boss = MouseBoss()
        boss.y = 600f
        w.enemies += boss
        repeat((World.INTRO_TIME / STEP).toInt() + 5) { w.update(STEP) }
        w.player.x = boss.x
        w.player.y = boss.y + 20f
        w.player.invulnerable = 0f
        val armor = w.player.armor
        w.update(STEP)
        assertTrue("touching the boss hurts", w.player.armor < armor)
        val d = kotlin.math.sqrt(dist2(boss.x, boss.y, w.player.x, w.player.y))
        assertTrue("player is pushed out of the boss body ($d)", d >= boss.radius * World.BOSS_BODY)
    }

    @Test
    fun blackCatDashesOnAFastFlickAndOrangeDoesNot() {
        for (cat in CatType.entries) {
            val w = World(Stages.mouse, cat, 2000f)
            repeat((World.INTRO_TIME / STEP).toInt() + 5) { w.update(STEP) }
            w.player.invulnerable = 0f
            w.drag(300f, 0f)
            w.update(STEP)
            if (cat == CatType.BLACK) {
                assertTrue("black cat dashes", w.player.dashTime > 0f && w.player.invulnerable > 0f)
            } else {
                assertEquals(0f, w.player.dashTime, 0f)
            }
        }
    }

    @Test
    fun blackCatVibroPawOnlyHurtsWhatTouchesItsOutline() {
        val w = World(Stages.mouse, CatType.BLACK, 2000f)
        repeat((World.INTRO_TIME / STEP).toInt() + 5) { w.update(STEP) }
        w.enemies.clear()
        w.godMode = true
        w.player.mainWeapon = MainWeapon.SPECIAL
        w.player.mainLevel = 3
        w.firing = true
        w.update(STEP)
        val size = w.pawBolt.size
        // 一隻壓在輪廓上（掌心正下方的邊），一隻在掌心裡面
        val onEdge = BallTurret(w.player.x).also { it.hp = 10000f }
        val inside = BallTurret(w.player.x).also { it.hp = 10000f }
        w.enemies += onEdge
        w.enemies += inside
        repeat(60 * 2) {
            onEdge.x = w.player.x
            onEdge.y = w.player.y + size
            inside.x = w.player.x
            inside.y = w.player.y + size * 0.15f
            w.update(STEP)
            assertTrue(w.pawBolt.active)
        }
        assertTrue("enemy on the outline is hurt", onEdge.hp < 10000f)
        assertEquals("enemy inside the paw is untouched", 10000f, inside.hp, 0.01f)
        assertTrue("no projectiles", w.playerShots.isEmpty())
    }

    /** 掃擊型的雷射／道具一定要留安全區：柴犬的雙眼光束與熊的旋轉蜂蜜鑽，正下方那一塊永遠打不到。 */
    @Test
    fun sweepingAttacksLeaveASafeZoneBelowTheBoss() {
        for (stage in listOf(Stages.dog, Stages.bear)) {
            val w = World(stage, CatType.ORANGE, height = 2000f)
            w.godMode = true
            autoplayUntil(w) { it.phase == Phase.BOSS }
            val boss = w.boss!!
            // 打到卸甲／後半段，讓雙眼光束與旋轉鑽都會出現
            boss.hp = boss.maxHp * 0.3f
            var sweeps = 0
            var t = 0f
            while (t < 90f && w.phase == Phase.BOSS) {
                w.firing = false
                w.update(STEP)
                w.cues.clear()
                t += STEP
                val sx = boss.x
                val sy = boss.y + boss.radius + 260f
                for (l in w.lasers) {
                    if (l.sweep == 0f || !l.active) continue
                    if (l.style != HazardStyle.BEAM && l.style != HazardStyle.DIPPER) continue
                    sweeps++
                    assertTrue("${stage.title}: ${l.style} hits the safe zone", l.distance2To(sx, sy) > (l.width * 0.4f + 12f).let { it * it })
                }
            }
            assertTrue("${stage.title}: saw a sweeping attack", sweeps > 0)
        }
    }

    /** Boss 不會瞬間移動：掃擊暫停後、換階段或裝甲時都用滑的過去。 */
    @Test
    fun bossesNeverTeleport() {
        for (entry in Stages.all.filter { it.stage != null }) {
            val w = World(entry.stage!!, CatType.ORANGE, height = 2000f)
            w.godMode = true
            autoplayUntil(w) { it.phase == Phase.BOSS }
            val boss = w.boss!!
            w.player.mainWeapon = MainWeapon.FOCUS
            w.player.mainLevel = Player.MAX_MAIN_LEVEL
            w.firing = true
            var lastX = boss.x
            var lastY = boss.y
            var t = 0f
            while (t < 120f && w.phase == Phase.BOSS) {
                w.moveX = if (abs(boss.x - w.player.x) > 20f) sign(boss.x - w.player.x) else 0f
                w.update(STEP)
                w.cues.clear()
                t += STEP
                val step = boss.maxMoveSpeed * STEP + 0.5f
                assertTrue("${entry.title}: boss jumped ${abs(boss.x - lastX)} at $t s", abs(boss.x - lastX) <= step)
                assertTrue("${entry.title}: boss jumped vertically at $t s", abs(boss.y - lastY) <= step)
                lastX = boss.x
                lastY = boss.y
            }
        }
    }

    /** 企鵝的急凍子彈：停在空中一下，再照原本的速度繼續飛。 */
    @Test
    fun frozenBulletsStopThenResume() {
        val w = worldInStage()
        val b = w.bullet(500f, 300f, 0f, 200f, BulletStyle.ICE, Tint.CYAN)
        b.freezeAt = 0.2f
        b.freezeTime = 0.5f
        repeat((0.3f / STEP).toInt()) { w.update(STEP) }
        val frozenY = b.y
        assertTrue(b.frozen)
        repeat((0.3f / STEP).toInt()) { w.update(STEP) }
        assertEquals("stays put while frozen", frozenY, b.y, 0.01f)
        repeat((0.3f / STEP).toInt()) { w.update(STEP) }
        assertFalse(b.frozen)
        assertTrue("moves again after thawing", b.y > frozenY + 20f)
    }

    /** 無尾熊打瞌睡時受到兩倍傷害，被吵醒（裝甲）後就不會再睡。 */
    @Test
    fun koalaTakesDoubleDamageWhileNapping() {
        val w = bossFight(Stages.koala)
        val koala = w.boss as KoalaBoss
        w.firing = false
        var t = 0f
        while (!koala.napping && t < 30f) {
            w.update(STEP)
            t += STEP
        }
        assertTrue("the koala dozes off", koala.napping)
        repeat((0.5f / STEP).toInt()) { w.update(STEP) }
        val before = koala.hp
        w.damageEnemy(koala, 100f)
        assertEquals(200f, before - koala.hp, 0.01f)
        assertTrue("blows a snot bubble", w.enemies.any { it is SnotBubble })
        koala.hp = koala.maxHp * 0.28f
        repeat(10) { w.update(STEP) }
        assertTrue(koala.broken)
        assertFalse(koala.napping)
        assertEquals(HazardStyle.LEAF, koala.heldProp())
    }

    /** 水獺姊妹共用一條血量：打妹妹也會扣姊姊的血。 */
    @Test
    fun otterSistersShareOneHealthBar() {
        val w = bossFight(Stages.otter)
        val otter = w.boss as OtterBoss
        val twin = w.enemies.filterIsInstance<OtterTwin>().single()
        val before = otter.hp
        w.damageEnemy(twin, 100f)
        assertEquals(100f, before - otter.hp, 0.01f)
        assertTrue("the twin stands apart from her sister", kotlin.math.abs(twin.x - otter.x) > 200f)
        otter.hp = otter.maxHp * 0.4f
        repeat(10) { w.update(STEP) }
        assertTrue(otter.broken)
        assertEquals(null, otter.heldProp())
    }

    /** 跑到熊貓師父種下竹筍為止（不開火）。 */
    private fun untilShootsPlanted(w: World): List<BambooShoot> {
        w.firing = false
        var t = 0f
        while (w.enemies.none { it is BambooShoot } && t < 30f) {
            w.update(STEP)
            w.cues.clear()
            t += STEP
        }
        return w.enemies.filterIsInstance<BambooShoot>()
    }

    /** 熊貓師父：沒打掉的竹筍會被拔去吃，吃的時候受到 1.5 倍傷害，但會回血。 */
    @Test
    fun pandaEatsUneatenBambooShootsAndHeals() {
        val w = bossFight(Stages.panda)
        val panda = w.boss as PandaBoss
        panda.hp = panda.maxHp * 0.7f
        val shoots = untilShootsPlanted(w)
        assertEquals(3, shoots.size)
        val before = panda.hp
        var t = 0f
        while (!panda.eating && t < 10f) {
            w.update(STEP)
            w.cues.clear()
            t += STEP
        }
        assertTrue("the panda sits down to eat", panda.eating)
        assertEquals(1, panda.feasts)
        assertEquals(before + panda.maxHp * PandaBoss.FEAST_HEAL, panda.hp, 1f)
        assertTrue("the shoots are gone", w.enemies.none { it is BambooShoot })
        val hp = panda.hp
        w.damageEnemy(panda, 100f)
        assertEquals(150f, hp - panda.hp, 0.01f)
    }

    /** 竹筍全部打掉就吃不到：不回血、也不會坐下來。 */
    @Test
    fun clearingTheBambooShootsDeniesTheFeast() {
        val w = bossFight(Stages.panda)
        val panda = w.boss as PandaBoss
        panda.hp = panda.maxHp * 0.7f
        for (s in untilShootsPlanted(w)) w.damageEnemy(s, 1000f)
        val before = panda.hp
        repeat((6f / STEP).toInt()) {
            w.update(STEP)
            w.cues.clear()
            assertFalse(panda.eating)
        }
        assertEquals(0, panda.feasts)
        assertEquals(before, panda.hp, 0.01f)
    }

    /** 回血不會把師父拉回上一個階段。 */
    @Test
    fun pandaFeastNeverUndoesAPhase() {
        val w = bossFight(Stages.panda)
        val panda = w.boss as PandaBoss
        panda.hp = panda.maxHp * 0.59f
        w.update(STEP)
        assertEquals(1, panda.phase)
        untilShootsPlanted(w)
        var t = 0f
        while (panda.feasts == 0 && t < 10f) {
            w.update(STEP)
            w.cues.clear()
            t += STEP
        }
        assertEquals(1, panda.feasts)
        assertTrue(panda.hpRatio < 0.6f)
        assertEquals(1, panda.phase)
    }

    /** 熊貓師父 40% 卸甲（竹棍飛走），之後會縮成球滾動，而且先畫出預測路線、不會滾出範圍。 */
    @Test
    fun pandaDisarmsThenRollsInsideItsLane() {
        val w = bossFight(Stages.panda)
        val panda = w.boss as PandaBoss
        assertEquals(HazardStyle.STAFF, panda.heldProp())
        panda.hp = panda.maxHp * 0.42f
        w.update(STEP)
        assertFalse(panda.broken)
        panda.hp = panda.maxHp * 0.38f
        repeat(10) { w.update(STEP) }
        assertTrue(panda.broken)
        assertEquals(null, panda.heldProp())
        w.firing = false
        var sawWarning = false
        var rolled = 0f
        var t = 0f
        while (t < 40f && w.phase == Phase.BOSS) {
            w.update(STEP)
            w.cues.clear()
            t += STEP
            if (panda.rollWarn > 0f) {
                sawWarning = true
                assertTrue(panda.rollPath.size >= 4)
            }
            if (panda.rolling) {
                rolled += STEP
                assertTrue(panda.x >= PandaBoss.ROLL_MARGIN - 20f && panda.x <= WORLD_WIDTH - PandaBoss.ROLL_MARGIN + 20f)
                assertTrue("rolls away from the player's row", panda.y < w.player.y - panda.radius)
            }
        }
        assertTrue("warns before rolling", sawWarning)
        assertTrue("rolls for a while ($rolled s)", rolled > 2f)
    }
}
