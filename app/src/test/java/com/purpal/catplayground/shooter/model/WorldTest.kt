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

    /** 隨機池的 9 關都可能是第一站：用第一站的強度、初始武器也要打得完。 */
    @Test
    fun everyPoolStageCanBeClearedWithBaseWeaponsAsTheFirstBout() {
        for (number in Tour.POOL) {
            for (cat in CatType.entries) {
                val w = World(Stages.get(number)!!, cat, height = 2000f, difficulty = Difficulty.forBout(0), bout = 1)
                w.godMode = true
                val time = autoplay(w, 600f)
                assertEquals("stage $number with $cat", Phase.CLEAR, w.phase)
                assertTrue("stage $number with $cat took $time s", time < 400f)
            }
        }
    }

    /**
     * 三大天王與萌王一定排在 9 站之後，不會用初始武器去打。
     * 以「一路上只撿到一點點道具」的裝備（主武器 Lv2、副武器 Lv1）在它們的站數強度下也要打得完。
     */
    @Test
    fun kingsAndChampionCanBeClearedWithAModestLoadout() {
        val bouts = Tour.KINGS.mapIndexed { i, n -> n to Tour.RANDOM_BOUTS + i } + (Tour.CHAMPION to Tour.SIZE - 1)
        for ((number, bout) in bouts) {
            for (cat in CatType.entries) {
                val loadout = Loadout(MainWeapon.SPREAD, 2, SubWeapon.HOMING, 1, Player.START_BOMBS, 1f)
                val w = World(Stages.get(number)!!, cat, height = 2000f, loadout = loadout, difficulty = Difficulty.forBout(bout), bout = bout + 1)
                w.godMode = true
                val time = autoplay(w, 700f)
                assertEquals("stage $number with $cat", Phase.CLEAR, w.phase)
                assertTrue("stage $number (bout $bout) with $cat took $time s", time < 450f)
            }
        }
    }

    /** 這一關在巡迴裡大約第幾站出場：天王與萌王位置固定，隨機池取中間（第 5 站）。 */
    private fun typicalBout(number: Int): Int =
        if (number == Tour.CHAMPION) Tour.SIZE - 1
        else Tour.KINGS.indexOf(number).takeIf { it >= 0 }?.let { Tour.RANDOM_BOUTS + it }
            ?: (Tour.RANDOM_BOUTS / 2)

    @Test
    fun bossFightLengthIsReasonableAtMaxPower() {
        for (entry in Stages.all.filter { it.stage != null }) {
            val bout = typicalBout(entry.number)
            val w = World(entry.stage!!, CatType.ORANGE, height = 2000f, difficulty = Difficulty.forBout(bout))
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
            // 滿火力時，中段的 boss 戰大約 30～100 秒；天王與萌王血比較厚，可以再長一些。
            val limit = if (bout >= Tour.RANDOM_BOUTS) 170f else 140f
            assertTrue("stage ${entry.number} (bout $bout) boss took $t s", t in 25f..limit)
        }
    }

    /** 整趟巡迴：機體狀態一站一站帶下去，越後面越難，但每一站都要打得完。 */
    @Test
    fun wholeTourCanBeClearedWithEscalatingDifficulty() {
        for (cat in CatType.entries) {
            var tour = Tour.start(cat, kotlin.random.Random(7))
            val times = ArrayList<Float>()
            while (!tour.finished) {
                val bout = tour.nextBout
                val w = World(
                    tour.stageAt(bout)!!, cat, height = 2000f,
                    loadout = tour.loadout, difficulty = Difficulty.forBout(bout), bout = bout + 1,
                )
                w.godMode = true
                val time = autoplay(w, 600f)
                assertEquals("bout $bout (stage ${tour.nextStage}) with $cat", Phase.CLEAR, w.phase)
                assertTrue("bout $bout (stage ${tour.nextStage}) with $cat took $time s", time < 400f)
                times += time
                tour = tour.reveal().afterClear(w.score, w.loadout())
            }
            assertEquals(Tour.SIZE, times.size)
        }
    }

    @Test
    fun tourDrawsEveryAnimalOnceAndEndsWithKingsAndChampion() {
        val tour = Tour.start(CatType.BLACK, kotlin.random.Random(3))
        assertEquals(Tour.SIZE, tour.order.size)
        assertEquals(Tour.POOL.toSet(), tour.order.take(Tour.RANDOM_BOUTS).toSet())
        assertEquals(Tour.KINGS + Tour.CHAMPION, tour.order.drop(Tour.RANDOM_BOUTS))
        assertEquals(Countries.HOME, tour.departure())
        assertFalse(tour.nextRevealed)

        val next = tour.reveal()
        assertTrue(next.nextRevealed)
        val loadout = World(Stages.dog, CatType.BLACK, 2000f).loadout()
        val after = next.afterClear(1234, loadout)
        assertEquals(1, after.cleared)
        assertFalse(after.nextRevealed)
        assertEquals(tour.stageAt(0)!!.country, after.departure())

        val decoded = TourState.decode(after.encode())
        assertEquals(after, decoded)
        assertEquals(null, TourState.decode("garbage"))
        // 舊版（有 3×3 格子欄位）的存檔也讀得回來
        val old = after.encode().split(";").toMutableList().apply { add(2, "0.1.2.3.4.5.6.7.8") }.joinToString(";")
        assertEquals(after, TourState.decode(old))
        assertEquals(null, TourState.decode(null))
    }

    @Test
    fun laterBoutsAreHarder() {
        for (i in 1 until Tour.SIZE) {
            val a = Difficulty.forBout(i - 1)
            val b = Difficulty.forBout(i)
            assertTrue(b.bossHp!! > a.bossHp!!)
            assertTrue(b.bulletSpeed > a.bulletSpeed && b.tempo > a.tempo && b.density > a.density)
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
        val wall = SonicWall(500f, 800f, radius = 160f, halfAngle = 50f * DEG, maxHp = 100f)
        assertTrue(wall.blocks(500f, 800f, 10f))
        assertFalse(wall.blocks(500f, 500f, 10f))

        val w = worldInStage()
        w.player.x = 500f
        w.player.y = 1400f
        w.walls += SonicWall(500f, 1000f, 160f, 50f * DEG, maxHp = 100f)
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
        val fork = Laser(500f, 100f, HALF_PI, 1000f, 40f, warnTime = 0.2f, activeTime = 1f, color = Tint.PINK, damage = 30f, style = HazardStyle.SWISSKNIFE, extendTime = 0.4f)
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
    fun championMouseDisarmsAndLosesItsSwissKnife() {
        val w = bossFight(Stages.mouse)
        val boss = w.boss!!
        assertEquals(HazardStyle.SWISSKNIFE, boss.heldProp())
        boss.hp = boss.maxHp * 0.65f
        w.update(STEP)
        assertFalse("65% is above the mouse's break point", boss.broken)
        boss.hp = boss.maxHp * 0.58f
        w.update(STEP)
        assertTrue(boss.broken)
        assertEquals(null, boss.heldProp())
        assertTrue(w.particles.any { it.kind == ParticleKind.PROP && it.tag == LostProp.SWISSKNIFE })
        assertTrue(w.particles.any { it.kind == ParticleKind.SFX && it.tag == SfxText.BREAK })
    }

    @Test
    fun eachBossBreaksAtItsOwnThreshold() {
        assertEquals(0.6f, MouseBoss().breakAt, 0f)
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
                assertEquals(0f, w.player.tempArmor, 0f)
            } else {
                assertEquals(0f, w.player.dashTime, 0f)
                assertEquals(Difficulty.NEUTRAL.tempArmor, w.player.tempArmor, 0f)
            }
        }
    }

    /** 力場盾一次只有一道：上一道消失 0.3 秒後才放下一道。 */
    @Test
    fun onlyOneForceFieldAtATime() {
        val w = worldInStage()
        w.godMode = true
        w.player.subWeapon = SubWeapon.SONIC
        w.player.subLevel = 3
        w.firing = true
        var maxWalls = 0
        var gone = -1f
        var gap = Float.MAX_VALUE
        var t = 0f
        while (t < 12f) {
            val before = w.walls.count { it.alive }
            w.update(STEP)
            w.cues.clear()
            t += STEP
            val now = w.walls.count { it.alive }
            maxWalls = maxOf(maxWalls, now + w.playerShots.count { it.alive && it.style == ShotStyle.SONIC })
            if (before > 0 && now == 0) gone = t
            if (before == 0 && now > 0 && gone >= 0f) gap = minOf(gap, t - gone)
        }
        assertEquals(1, maxWalls)
        assertTrue("waits ${World.WALL_GAP}s after the last wall ($gap)", gap >= World.WALL_GAP)
    }

    /** 雷射擋不住，但穿過力場盾時一樣扣盾的耐久（同一道雷射只算一次）。 */
    @Test
    fun lasersPassThroughForceFieldsButWearThemDown() {
        val w = worldInStage()
        w.godMode = true
        w.enemies.clear()
        val wall = SonicWall(500f, 900f, 160f, 50f * DEG, maxHp = 100f)
        w.walls += wall
        w.lasers += Laser(500f, 0f, HALF_PI, 3000f, 30f, 0f, 1f, Tint.RED, 30f)
        runFor(w, 0.5f)
        assertTrue(wall.alive)
        assertEquals(70f, wall.hp, 0.01f)
        w.lasers += Laser(500f, 0f, HALF_PI, 3000f, 30f, 0f, 1f, Tint.RED, 80f)
        runFor(w, 0.2f)
        assertFalse("second laser breaks it", wall.alive)
    }

    /** 熊熊大王的蜂蜜鑽：來回橫掃身體下方，不會往上轉。 */
    @Test
    fun bearDipperSweepsTheLowerHalf() {
        val w = bossFight(Stages.bear)
        w.firing = false
        val sweeps = HashSet<Laser>()
        var t = 0f
        while (t < 60f && (sweeps.size < 2 || w.lasers.any { it in sweeps })) {
            w.update(STEP)
            w.cues.clear()
            t += STEP
            for (l in w.lasers) {
                if (l.style != HazardStyle.DIPPER || l.sweep == 0f) continue
                sweeps += l
                if (!l.active) continue
                val a = Math.floorMod((l.angle / DEG).toInt(), 360)
                assertTrue("dipper points down ($a°)", a in 0..180)
            }
        }
        assertTrue("sweeps there and back (${sweeps.size})", sweeps.size >= 2)
    }

    /** 小橘的肉球護甲：時間內的傷害先扣臨時護甲；沒扣完的時間到就消失，扣超過的才扣本體。 */
    @Test
    fun orangeFlickGivesTemporaryArmorThatAbsorbsDamageThenFades() {
        fun flicked(): World {
            val w = World(Stages.mouse, CatType.ORANGE, 2000f)
            repeat((World.INTRO_TIME / STEP).toInt() + 5) { w.update(STEP) }
            w.player.invulnerable = 0f
            w.drag(300f, 0f)
            w.update(STEP)
            return w
        }
        val small = flicked()
        val temp = small.player.tempArmor
        assertTrue(temp > 0f)
        small.damagePlayer(temp - 6f)
        assertEquals(CatType.ORANGE.maxArmor, small.player.armor, 0f)
        assertEquals(0f, small.player.invulnerable, 0f)
        assertEquals(6f, small.player.tempArmor, 0.01f)
        runFor(small, World.DASH_TIME + 0.05f)
        assertEquals(0f, small.player.tempArmor, 0f)
        assertEquals(CatType.ORANGE.maxArmor, small.player.armor, 0f)

        val big = flicked()
        big.damagePlayer(big.player.tempArmor + 10f)
        assertEquals(CatType.ORANGE.maxArmor - 10f, big.player.armor, 0.01f)
        assertTrue(big.player.invulnerable > 0f)

        assertTrue(Difficulty.forBout(Tour.SIZE - 1).tempArmor > Difficulty.forBout(0).tempArmor)
    }

    /** 力場盾有耐久：擋下的子彈傷害超過耐久就碎掉。 */
    @Test
    fun forceFieldShattersAfterBlockingTooMuch() {
        val w = worldInStage()
        w.godMode = true
        w.player.subWeapon = SubWeapon.SONIC
        w.player.subLevel = 1
        w.firing = true
        var t = 0f
        while (w.walls.isEmpty() && t < 3f) { w.update(STEP); w.cues.clear(); t += STEP }
        w.firing = false
        val wall = w.walls.single()
        w.enemies.clear()
        w.bullets.clear()
        val maxHp = World.WALL_HP + World.WALL_HP_PER_LEVEL
        assertEquals(maxHp, wall.maxHp, 0f)
        val top = wall.centerY - wall.radius
        val needed = kotlin.math.ceil(maxHp / World.defaultDamage(BulletStyle.ORB)).toInt()
        repeat(needed - 1) { w.bullet(wall.x, top - 60f, 0f, 400f, BulletStyle.ORB, Tint.RED) }
        runFor(w, 0.4f)
        assertTrue("still standing after ${needed - 1} bullets", wall.alive)
        w.bullet(wall.x, top - 60f, 0f, 400f, BulletStyle.ORB, Tint.RED)
        runFor(w, 0.4f)
        assertFalse("shattered", wall.alive)
        assertTrue(w.particles.any { it.kind == ParticleKind.SHARD })
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

    /**
     * 掃擊型的雷射／道具一定要留安全區：柴犬的雙眼光束正下方那一塊永遠打不到；
     * 熊的蜂蜜鑽會來回橫掃身體下方，但搆不到更下面的地方。
     */
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
                for (l in w.lasers) {
                    if (l.sweep == 0f || !l.active) continue
                    if (l.style != HazardStyle.BEAM && l.style != HazardStyle.DIPPER) continue
                    sweeps++
                    val sx = boss.x
                    val sy = if (l.style == HazardStyle.DIPPER) l.y + l.length + l.width + 40f else boss.y + boss.radius + 260f
                    assertTrue("safe zone is on screen", sy < w.height * 0.8f)
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

    private fun runFor(w: World, seconds: Float, each: () -> Unit = {}) {
        var t = 0f
        while (t < seconds) {
            w.update(STEP)
            w.cues.clear()
            each()
            t += STEP
        }
    }

    /** 樹懶郵差：慢動作力場讓圈內的子彈變慢，裝甲後反過來變快。 */
    @Test
    fun slothFieldSlowsBulletsThenSpeedsThemUpAfterArmoring() {
        val w = bossFight(Stages.sloth)
        val sloth = w.boss as SlothBoss
        w.firing = false
        assertEquals(SlothBoss.SLOW_FIELD, sloth.bulletTimeScale(sloth.x, sloth.y + 100f), 0.001f)
        assertEquals(1f, sloth.bulletTimeScale(sloth.x, sloth.y + SlothBoss.FIELD_RADIUS + 50f), 0.001f)
        w.bullets.clear()
        val inside = w.bullet(sloth.x, sloth.y + 60f, 0f, 200f, BulletStyle.ORB, Tint.RED)
        val outside = w.bullet(60f, sloth.y + 400f, 0f, 200f, BulletStyle.ORB, Tint.RED)
        val y0 = inside.y
        val y1 = outside.y
        repeat((0.3f / STEP).toInt()) { w.update(STEP) }
        assertTrue("slowed inside the field", inside.y - y0 < (outside.y - y1) * 0.5f)
        sloth.hp = sloth.maxHp * 0.33f
        runFor(w, Boss.ARMOR_TIME + 0.2f)
        assertTrue(sloth.broken)
        assertEquals(SlothBoss.FAST_FIELD, sloth.bulletTimeScale(sloth.x, sloth.y + 100f), 0.001f)
        assertEquals(HazardStyle.ROCKET, sloth.heldProp())
    }

    /** 月夜怪盜：抓鉤會偷走畫面上的道具，打破布袋就全部吐出來，怪盜也會慌張（1.5 倍傷害）。 */
    @Test
    fun raccoonStealsItemsAndTheBagGivesThemBack() {
        val w = bossFight(Stages.raccoon)
        val raccoon = w.boss as RaccoonBoss
        w.firing = false
        var t = 0f
        while (raccoon.loot.isEmpty() && t < 30f) {
            if (w.items.none { it.type == ItemType.REPAIR }) w.items += Item(raccoon.x, raccoon.y + 380f, ItemType.REPAIR)
            w.update(STEP)
            w.cues.clear()
            t += STEP
        }
        assertTrue("stole something", raccoon.loot.isNotEmpty())
        w.items.clear()
        val stolen = raccoon.loot.toList()
        val bag = raccoon.bag!!
        w.damageEnemy(bag, LootBag.HP + 1f)
        w.update(STEP)
        assertTrue(raccoon.loot.isEmpty())
        assertTrue("panics after losing the bag", raccoon.panic > 0f)
        for (type in stolen) assertTrue("gave back $type", w.items.any { it.type == type })
        assertTrue("and some coins", w.items.count { it.type == ItemType.COIN } >= 8)
        val hp = raccoon.hp
        w.damageEnemy(raccoon, 100f)
        assertEquals(150f, hp - raccoon.hp, 0.01f)
        runFor(w, RaccoonBoss.BAG_RESPAWN + 1f)
        assertTrue("brings a new bag", raccoon.bag?.alive == true)
    }

    /** 怪盜卸甲後會變出兩隻分身（沒有影子、不是 Boss），一段時間後消失。 */
    @Test
    fun raccoonMakesDecoysAfterDisarming() {
        val w = bossFight(Stages.raccoon)
        val raccoon = w.boss as RaccoonBoss
        w.firing = false
        raccoon.hp = raccoon.maxHp * 0.48f
        w.update(STEP)
        assertTrue(raccoon.broken)
        assertEquals(null, raccoon.heldProp())
        var t = 0f
        while (w.enemies.none { it is RaccoonDecoy } && t < 20f) {
            w.update(STEP)
            w.cues.clear()
            t += STEP
        }
        assertEquals(2, w.enemies.count { it is RaccoonDecoy })
        assertTrue(w.enemies.filterIsInstance<RaccoonDecoy>().none { it.isBoss })
        runFor(w, RaccoonDecoy.LIFE + 0.5f)
        assertTrue(w.enemies.none { it is RaccoonDecoy && it.age > RaccoonDecoy.LIFE })
    }

    /** 遺跡猴王：學玩家的主武器（集中 → 光束），學的時候站著不動。 */
    @Test
    fun monkeyMimicsThePlayersWeaponAndStandsStill() {
        val w = bossFight(Stages.monkey)
        val monkey = w.boss as MonkeyBoss
        w.firing = false
        w.player.mainWeapon = MainWeapon.FOCUS
        var t = 0f
        var sawBeam = false
        var lastX = Float.NaN
        var still = true
        while (t < 40f && !sawBeam) {
            w.update(STEP)
            w.cues.clear()
            t += STEP
            if (monkey.mimicking == MainWeapon.FOCUS) {
                if (!lastX.isNaN() && kotlin.math.abs(monkey.x - lastX) > 0.01f) still = false
                lastX = monkey.x
                if (w.lasers.any { it.owner === monkey && it.style == HazardStyle.BEAM }) sawBeam = true
            } else {
                lastX = Float.NaN
            }
        }
        assertTrue("copies the focus beam", sawBeam)
        assertTrue("stands still while copying", still)
    }

    /** 學專武：小橘 → 一整排巨大香蕉砲；小黑 → 排成貓掌形狀往外擴散的香蕉。 */
    @Test
    fun monkeyCopiesEachCatsSpecialWeaponDifferently() {
        for (cat in CatType.entries) {
            val w = World(Stages.monkey, cat, 2000f)
            w.godMode = true
            autoplayUntil(w) { it.phase == Phase.BOSS }
            val monkey = w.boss as MonkeyBoss
            w.firing = false
            w.player.mainWeapon = MainWeapon.SPECIAL
            var t = 0f
            var bigBananas = 0
            var biggestBurst = 0
            while (t < 40f && monkey.mimicking == null) { w.update(STEP); w.cues.clear(); t += STEP }
            assertEquals(MainWeapon.SPECIAL, monkey.mimicking)
            while (monkey.mimicking == MainWeapon.SPECIAL) {
                val before = w.bullets.toHashSet()
                w.update(STEP)
                w.cues.clear()
                val fresh = w.bullets.filter { it !in before && it.style == BulletStyle.BANANA }
                biggestBurst = maxOf(biggestBurst, fresh.size)
                bigBananas += fresh.count { it.radius >= 30f * World.BULLET_SCALE - 0.01f }
            }
            if (cat == CatType.ORANGE) {
                assertTrue("orange: giant banana cannon ($bigBananas)", bigBananas >= 6)
            } else {
                assertEquals("black: no giant bananas", 0, bigBananas)
                assertTrue("black: paw-shaped burst ($biggestBurst)", biggestBurst >= 20)
            }
        }
    }

    /** 松露獵人：45% 法棍被打飛（卸甲）；嗅松露時地上冒出記號，時間到才炸開。 */
    @Test
    fun truffleHunterSniffsTrufflesThatBurstLater() {
        val w = bossFight(Stages.pig)
        val pig = w.boss as PigBoss
        w.firing = false
        assertEquals(HazardStyle.BAGUETTE, pig.heldProp())
        pig.hp = pig.maxHp * 0.5f
        w.update(STEP)
        assertFalse(pig.broken)
        var t = 0f
        while (w.enemies.none { it is Truffle } && t < 30f) {
            w.update(STEP)
            w.cues.clear()
            t += STEP
        }
        val truffle = w.enemies.filterIsInstance<Truffle>().first()
        assertTrue("one truffle under the cat", w.enemies.filterIsInstance<Truffle>().any { kotlin.math.abs(it.x - w.player.x) < 80f })
        w.bullets.clear()
        runFor(w, Truffle.WARN - truffle.age - 0.1f)
        assertTrue("nothing bursts before the warning ends", w.bullets.none { dist2(it.x, it.y, truffle.x, truffle.y) < 30f * 30f })
        runFor(w, 0.2f)
        assertTrue("bursts after the warning", w.bullets.any { dist2(it.x, it.y, truffle.x, truffle.y) < 80f * 80f })
        pig.hp = pig.maxHp * 0.44f
        repeat(10) { w.update(STEP) }
        assertTrue(pig.broken)
        assertEquals(null, pig.heldProp())
    }

    /** 萌王鼠鼠：60% 卸甲後擺老鼠夾（靠近才會夾），30% 搬出起司火鍋（第二次變身）。 */
    @Test
    fun championMouseSetsTrapsThenAwakens() {
        val w = bossFight(Stages.mouse)
        val mouse = w.boss as MouseBoss
        w.firing = false
        mouse.hp = mouse.maxHp * 0.58f
        repeat(20) { w.update(STEP) }
        assertTrue(mouse.broken)
        var t = 0f
        while (w.enemies.none { it is MouseTrap } && t < 30f) {
            w.update(STEP)
            w.cues.clear()
            t += STEP
        }
        val traps = w.enemies.filterIsInstance<MouseTrap>()
        assertEquals(3, traps.size)
        val far = traps.maxBy { dist2(it.x, it.y, w.player.x, w.player.y) }
        if (dist2(far.x, far.y, w.player.x, w.player.y) > MouseTrap.TRIGGER * MouseTrap.TRIGGER) assertTrue(far.triggered < 0f)
        mouse.hp = mouse.maxHp * 0.28f
        repeat(20) { w.update(STEP) }
        assertTrue(mouse.awakened)
        assertTrue("invulnerable while transforming", mouse.invulnerable)
    }

    /** 起司洞洞牆：一整排起司裡一定留著鑽得過去的洞。 */
    @Test
    fun cheeseWallsAlwaysLeaveHoles() {
        val w = bossFight(Stages.mouse)
        w.firing = false
        var walls = 0
        var t = 0f
        while (walls < 3 && t < 40f) {
            w.bullets.clear()
            w.update(STEP)
            w.cues.clear()
            t += STEP
            val row = w.bullets.filter { it.style == BulletStyle.CHEESE && it.y < 0f }
            if (row.size > 8) {
                walls++
                val xs = row.map { it.x }.sorted()
                val widest = (listOf(0f) + xs + WORLD_WIDTH).zipWithNext { a, b -> b - a }.max()
                assertTrue("a hole wide enough to fly through ($widest)", widest > 140f)
            }
        }
        assertTrue(walls > 0)
    }

    /** 泥巴坑：落地後踩進去會變慢，離開就恢復。 */
    @Test
    fun mudPuddleSlowsThePlayer() {
        val w = worldInStage()
        w.enemies.clear()
        w.godMode = true
        val mud = MudPuddle(w.player.x, w.player.y)
        w.spawn(mud)
        w.update(STEP)
        assertEquals("not landed yet", 1f, w.player.slow, 0.001f)
        runFor(w, MudPuddle.WARN + 0.1f)
        assertEquals(MudPuddle.MUD_SLOW, w.player.slow, 0.001f)
        w.player.x = mud.x + mud.radius + 100f
        w.update(STEP)
        assertEquals(1f, w.player.slow, 0.001f)
    }

    /** 大野狼吹氣會把正下方的貓咪往後推。 */
    @Test
    fun wolfBlowsThePlayerBack() {
        val w = worldInStage()
        w.enemies.clear()
        w.godMode = true
        val wolf = EliteWolf(w.player.x)
        w.spawn(wolf)
        var pushed = false
        runFor(w, 5f) {
            wolf.x = w.player.x
            if (wolf.blowing) {
                val y = w.player.y
                w.player.y = w.playerMaxY - 200f
                w.update(STEP)
                if (w.player.y > w.playerMaxY - 200f + 1f) pushed = true
                w.player.y = y
            }
        }
        assertTrue(pushed)
    }
}
