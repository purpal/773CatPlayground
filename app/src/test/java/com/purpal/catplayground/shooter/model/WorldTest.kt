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
            while (w.phase == Phase.BOSS && t < 300f) {
                val b = w.boss!!
                w.moveX = if (abs(b.x - w.player.x) > 20f) sign(b.x - w.player.x) else 0f
                w.update(STEP)
                w.cues.clear()
                t += STEP
            }
            // 滿火力時 boss 戰大約 20～60 秒。
            assertTrue("stage ${entry.number} boss took $t s", t in 15f..70f)
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
}
