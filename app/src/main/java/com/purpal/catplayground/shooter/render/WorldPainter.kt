package com.purpal.catplayground.shooter.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import com.purpal.catplayground.DebugFlags
import com.purpal.catplayground.shooter.model.BallTurret
import com.purpal.catplayground.shooter.model.Balloon
import com.purpal.catplayground.shooter.model.Boss
import com.purpal.catplayground.shooter.model.BulletStyle
import com.purpal.catplayground.shooter.model.CatType
import com.purpal.catplayground.shooter.model.CheeseDrone
import com.purpal.catplayground.shooter.model.Enemy
import com.purpal.catplayground.shooter.model.EnemyBullet
import com.purpal.catplayground.shooter.model.EnemyKind
import com.purpal.catplayground.shooter.model.Frisbee
import com.purpal.catplayground.shooter.model.HazardStyle
import com.purpal.catplayground.shooter.model.Item
import com.purpal.catplayground.shooter.model.ItemType
import com.purpal.catplayground.shooter.model.Laser
import com.purpal.catplayground.shooter.model.MainWeapon
import com.purpal.catplayground.shooter.model.MousePlane
import com.purpal.catplayground.shooter.model.Particle
import com.purpal.catplayground.shooter.model.ParticleKind
import com.purpal.catplayground.shooter.model.Phase
import com.purpal.catplayground.shooter.model.PlayerShot
import com.purpal.catplayground.shooter.model.ShotStyle
import com.purpal.catplayground.shooter.model.SonicWall
import com.purpal.catplayground.shooter.model.StageTheme
import com.purpal.catplayground.shooter.model.ThrownBone
import com.purpal.catplayground.shooter.model.WORLD_WIDTH
import com.purpal.catplayground.shooter.model.World
import com.purpal.catplayground.shooter.model.YuzuDrone
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/** 把世界座標（寬 1000）縮放到畫面並置中。 */
class Viewport(val scale: Float, val offsetX: Float, val offsetY: Float)

fun viewportFor(size: Size, worldHeight: Float): Viewport {
    val scale = minOf(size.width / WORLD_WIDTH, size.height / worldHeight)
    return Viewport(scale, (size.width - WORLD_WIDTH * scale) / 2f, (size.height - worldHeight * scale) / 2f)
}

private val WhiteFlash = ColorFilter.tint(Color.White, BlendMode.SrcIn)
private const val RAD = 57.29578f

/*
 * v3 特效：「外表萌萌，出手是硬派科幻」。玩家火力一律冷色（青／藍／白），
 * 敵人與 Boss 道具一律暖色（紅／洋紅／琥珀／珊瑚）；每種玩家武器各留一個貓咪萌點。
 */
private val Cyan = Color(0xFF00E5FF)
private val Navy = Color(0xFF0A1630)
private val Steel = Color(0xFF4A5370)
private val Violet = Color(0xFFB388FF)
private val Amber = Color(0xFFFFB300)

fun DrawScope.drawWorld(w: World, view: Viewport, sprites: SpriteCache) {
    drawRect(themeEdge(w.stage.theme))
    val shake = if (w.shakeTime > 0f) sin(w.time * 90f) * 10f * minOf(1f, w.shakeTime * 3f) else 0f
    withTransform({
        translate(view.offsetX + shake, view.offsetY + shake * 0.6f)
        scale(view.scale, view.scale, Offset.Zero)
    }) {
        clipRect(0f, 0f, WORLD_WIDTH, w.height) {
            drawBackground(w.stage.theme, w.scroll, w.height, w.time)
            // 背景壓暗一些，讓霓虹般的火力更突出。
            drawRect(Color(0xFF0A0E24).copy(alpha = 0.3f), size = Size(WORLD_WIDTH, w.height))
            for (wall in w.walls) drawSonicWall(wall)
            for (item in w.items) drawItem(item, sprites)
            for (e in w.enemies) drawEnemy(e, w, sprites)
            drawLockOns(w, sprites)
            for (s in w.playerShots) drawShot(s, w, sprites)
            drawBeam(w, sprites)
            drawPlayer(w, sprites)
            for (l in w.lasers) drawHazard(l, w, sprites)
            for (b in w.bullets) drawBullet(b, sprites)
            for (p in w.particles) drawParticle(p, sprites)
            if (w.blastRadius >= 0f) drawBlast(w, sprites)
            drawCombo(w, sprites)
        }
    }
    if (w.screenFlash > 0f) drawRect(Color.White.copy(alpha = w.screenFlash * 0.65f))
}

private fun DrawScope.drawPlayer(w: World, sprites: SpriteCache) {
    val p = w.player
    if (!p.alive || w.phase == Phase.PLAYER_DOWN || w.phase == Phase.CONTINUE) return
    val beast = p.transformed > 0f
    if (beast) {
        val aura = if (p.cat == CatType.ORANGE) 0xFFFFB300L else 0xFFB388FFL
        drawSprite(sprites.glow(aura), p.x, p.y, 2.4f + sin(w.time * 12f) * 0.2f, alpha = 0.7f, blendMode = BlendMode.Plus)
    }
    val blink = p.invulnerable > 0f && !beast && (w.time * 14f).toInt() % 2 == 0
    // 依左右移動的速度換成往左／往右跑的圖。
    val bank = (p.vx / p.cat.baseSpeed).coerceIn(-1.5f, 1.5f)
    val pose = when {
        bank < -0.2f -> 1
        bank > 0.2f -> 2
        else -> 0
    }
    val frame = (w.time * 12f).toInt() and 3
    drawSprite(sprites.cat(p.cat, pose, frame, beast), p.x, p.y, if (beast) 1.2f else 1f, bank * 5f, alpha = if (blink) 0.35f else 1f)
    val shooting = w.firing && (w.phase == Phase.STAGE || w.phase == Phase.BOSS || w.phase == Phase.WARNING)
    if (shooting && p.mainWeapon != MainWeapon.FOCUS) {
        val flicker = 0.4f + ((w.time * 30f).toInt() % 3) * 0.1f
        drawSprite(sprites.muzzle(), p.x, p.y - 64f, flicker)
    }
    // 判定點
    if (w.firing) {
        circle(p.x, p.y + 4f, 8f, Color.White)
        shape(Shapes.heart, p.x, p.y + 4f, 6.5f, Color(0xFFFF2D6F))
    }
}

private fun DrawScope.drawEnemy(e: Enemy, w: World, sprites: SpriteCache) {
    val slow = ((w.time * 4f).toInt() + e.id) and 1
    val fast = ((w.time * 14f).toInt() + e.id) and 1
    // 被打中時壓扁一下並閃白；持續被光束照射時改成閃爍，避免整隻一直泛白。
    val hit = e.hitFlash > 0f
    val flash = hit && ((w.time * 20f).toInt() % 2 == 0)
    val squash = if (hit && !DebugFlags.noStretch) 1.07f else 1f
    fun sprite(s: Sprite, scale: Float = 1f, rotation: Float = 0f, flipX: Boolean = false, dy: Float = 0f) {
        drawSprite(s, e.x, e.y + dy, scale, rotation, flipX = flipX, stretch = squash)
        if (flash) drawSprite(s, e.x, e.y + dy, scale, rotation, alpha = 0.45f, flipX = flipX, colorFilter = WhiteFlash, stretch = squash)
    }
    when (e.kind) {
        EnemyKind.CHEESE_DRONE -> {
            sprite(sprites.cheeseWheel(), rotation = (e as CheeseDrone).spin * RAD)
            sprite(sprites.enemy(e.kind, 0))
        }
        EnemyKind.FRISBEE -> sprite(sprites.enemy(e.kind, 0), rotation = (e as Frisbee).spin * RAD)
        EnemyKind.YUZU -> sprite(sprites.enemy(e.kind, 0), rotation = (e as YuzuDrone).spin * RAD * 0.4f)
        EnemyKind.BALL_TURRET -> {
            sprite(sprites.barrel(), rotation = (e as BallTurret).aim * RAD)
            sprite(sprites.enemy(e.kind, 0))
        }
        EnemyKind.BEEHIVE -> sprite(sprites.enemy(e.kind, 0), rotation = e.age * 200f)
        EnemyKind.MOUSE_PLANE -> sprite(sprites.enemy(e.kind, fast), flipX = (e as MousePlane).facing < 0f)
        EnemyKind.MINI_MOUSE -> sprite(sprites.enemy(e.kind, fast), 0.8f)
        EnemyKind.MOUSE_RUNNER, EnemyKind.BEE, EnemyKind.PUPPY, EnemyKind.DUCKLING -> sprite(sprites.enemy(e.kind, fast))
        EnemyKind.BALLOON -> {
            // 氣球本體在判定圓上，道具掛在線的下端。
            sprite(sprites.enemy(e.kind, 0), dy = 30f)
            val drop = (e as Balloon).drop
            if (drop != null) drawSprite(sprites.item(drop), e.x, e.y + 104f, 0.85f)
        }
        EnemyKind.ELITE_MOUSE, EnemyKind.ELITE_DOG, EnemyKind.ELITE_BEE, EnemyKind.ELITE_MONKEY -> {
            val pulse = 3.4f + sin(w.time * 6f + e.id) * 0.3f
            drawSprite(sprites.glow(0xFFFF2BD6L), e.x, e.y, pulse, alpha = 0.45f, blendMode = BlendMode.Plus)
            sprite(sprites.enemy(e.kind, if (e.kind == EnemyKind.ELITE_BEE) fast else slow))
        }
        EnemyKind.THROWN_BONE -> {
            // 丟出來的硬光骨頭：旋轉＋紅色殘光
            drawSprite(sprites.glow(0xFFFF3D5AL), e.x, e.y, 2.6f, alpha = 0.5f, blendMode = BlendMode.Plus)
            drawSprite(sprites.enemy(e.kind, 0), e.x, e.y, 0.9f, (e as ThrownBone).spin)
        }
        EnemyKind.BOSS_MOUSE, EnemyKind.BOSS_DOG, EnemyKind.BOSS_BEAR, EnemyKind.BOSS_CAPY -> drawBoss(e as Boss, w, sprites, flash, squash)
        else -> sprite(sprites.enemy(e.kind, slow))
    }
}

/**
 * Boss：本體維持萌感，表情隨血量變化；手上的科技道具沒在攻擊時拿在手上晃。
 * 裝甲中（拿出新武器的那 1.6 秒）會發出珊瑚色的光。
 */
private fun DrawScope.drawBoss(boss: Boss, w: World, sprites: SpriteCache, flash: Boolean, squash: Float) {
    val defeated = w.phase == Phase.BOSS_DEFEATED
    val mood = if (defeated) 2 else boss.mood()
    val frame = ((w.time * (if (mood == 2) 6f else 3f)).toInt()) and 1
    val wobble = if (defeated) sin(w.time * 50f) * 6f else 0f
    val x = boss.x + wobble
    if (boss.armorTime > 0f) {
        val k = 1f - boss.armorTime / Boss.ARMOR_TIME
        drawSprite(sprites.glow(0xFFFF7A45L), x, boss.y, 5f + k * 3f, alpha = 0.7f * (1f - k * 0.5f), blendMode = BlendMode.Plus)
    }
    val held = boss.heldProp()
    val busy = held != null && w.lasers.any { it.owner === boss && it.style == held }
    val sprite = sprites.boss(boss.kind, mood, frame)
    drawSprite(sprite, x, boss.y, rotationDegrees = wobble * 0.5f, stretch = squash)
    if (flash || defeated && (w.time * 10f).toInt() % 2 == 0) {
        drawSprite(sprite, x, boss.y, alpha = 0.3f, colorFilter = WhiteFlash, rotationDegrees = wobble * 0.5f, stretch = squash)
    }
    if (held != null && !busy && !defeated) {
        val sway = sin(w.time * 2.4f) * 8f
        drawProp(held, x + boss.handX, boss.y + boss.handY, (-68f + sway) / RAD, idleLength(held), propWidth(held), w.time, sprites, active = false)
    }
}

private fun idleLength(style: HazardStyle) = when (style) {
    HazardStyle.FORK -> 230f
    HazardStyle.BONE -> 190f
    HazardStyle.DIPPER -> 210f
    HazardStyle.WATER -> 135f
    else -> 120f
}

private fun propWidth(style: HazardStyle) = when (style) {
    HazardStyle.FORK -> 16f
    HazardStyle.BONE -> 28f
    HazardStyle.DIPPER -> 18f
    else -> 24f
}

private fun DrawScope.drawItem(item: Item, sprites: SpriteCache) {
    if (item.type == ItemType.DATA) {
        drawSprite(sprites.item(ItemType.DATA), item.x, item.y, if (item.variant % 3 == 0) 1.3f else 1f, item.age * 300f)
        return
    }
    val bob = sin(item.age * 5f) * 4f
    if (item.type != ItemType.COIN) {
        drawSprite(sprites.glow(0xFFFFFFFFL), item.x, item.y, 0.9f + sin(item.age * 6f) * 0.15f, alpha = 0.35f, blendMode = BlendMode.Plus)
    }
    drawSprite(sprites.item(item.type), item.x, item.y + bob, 0.95f)
    if (item.cycle != null && !DebugFlags.noArc) {
        // 倒數圈：繞一圈就換成下一種武器。
        val r = 44f
        drawArc(
            Color.White.copy(alpha = 0.9f), -90f, 360f * item.cycleProgress, false,
            Offset(item.x - r, item.y + bob - r), Size(r * 2f, r * 2f), style = Stroke(5f, cap = StrokeCap.Round),
        )
    }
}

private val lockedIds = IntArray(32)

/** 追蹤飛彈鎖定的目標畫上洋紅色鎖定框（同一隻只畫一次）。 */
private fun DrawScope.drawLockOns(w: World, sprites: SpriteCache) {
    var n = 0
    for (s in w.playerShots) {
        if (s.style != ShotStyle.MISSILE) continue
        val t = s.target ?: continue
        if (!t.alive) continue
        var seen = false
        for (i in 0 until n) if (lockedIds[i] == t.id) seen = true
        if (seen || n >= lockedIds.size) continue
        lockedIds[n++] = t.id
        val pulse = 1f + sin(w.time * 12f) * 0.06f
        drawSprite(sprites.reticle(), t.x, t.y, (t.radius / 40f).coerceIn(0.8f, 2.2f) * pulse)
    }
}

// ───────────── 背景 ─────────────

private fun themeEdge(theme: StageTheme): Color = when (theme) {
    StageTheme.CHEESE_BARN -> Color(0xFF6D4C2F)
    StageTheme.PARK -> Color(0xFF3E7D3A)
    StageTheme.HONEY_FOREST -> Color(0xFF1B3A22)
    StageTheme.ONSEN -> Color(0xFF3B6E66)
}

private const val TILE = 300f

private fun DrawScope.drawBackground(theme: StageTheme, scroll: Float, height: Float, time: Float) {
    when (theme) {
        StageTheme.CHEESE_BARN -> drawRect(Color(0xFFE6C08A), size = Size(WORLD_WIDTH, height))
        StageTheme.PARK -> drawRect(Color(0xFF8ED47F), size = Size(WORLD_WIDTH, height))
        StageTheme.HONEY_FOREST -> drawRect(Color(0xFF2E5E3A), size = Size(WORLD_WIDTH, height))
        StageTheme.ONSEN -> drawRect(Color(0xFF7CCFC4), size = Size(WORLD_WIDTH, height))
    }
    val first = floor(scroll / TILE).toInt() - 1
    val last = floor((scroll + height) / TILE).toInt() + 1
    for (row in first..last) {
        val y = height - (row * TILE - scroll) - TILE
        val random = Random(row * 7919 + theme.ordinal)
        when (theme) {
            StageTheme.CHEESE_BARN -> barnRow(y, random, row)
            StageTheme.PARK -> parkRow(y, random, row)
            StageTheme.HONEY_FOREST -> forestRow(y, random)
            StageTheme.ONSEN -> onsenRow(y, random, row, time)
        }
    }
    val drift = Random(theme.ordinal)
    repeat(14) {
        val x = drift.nextFloat() * WORLD_WIDTH + sin(time * 0.8f + it) * 30f
        val y = (drift.nextFloat() * height + scroll * 1.8f) % (height + 40f) - 20f
        when (theme) {
            StageTheme.CHEESE_BARN -> circle(x, y, 4f, Color.White.copy(alpha = 0.35f))
            StageTheme.PARK -> oval(x, y, 10f, 6f, Color(0xFFFFC1E3).copy(alpha = 0.8f))
            StageTheme.HONEY_FOREST -> oval(x, y, 14f, 8f, Color(0xFF9CCC65).copy(alpha = 0.7f))
            // 溫泉的熱氣往上飄
            StageTheme.ONSEN -> oval(x, height - y, 26f, 14f, Color.White.copy(alpha = 0.35f))
        }
    }
}

private fun DrawScope.barnRow(y: Float, random: Random, row: Int) {
    val plank = TILE / 3f
    for (k in 0 until 3) {
        val py = y + k * plank
        drawRect(if ((row * 3 + k) % 2 == 0) Color(0xFFE6C08A) else Color(0xFFDDB47C), Offset(0f, py), Size(WORLD_WIDTH, plank))
        drawRect(Color(0xFFB98A55), Offset(0f, py), Size(WORLD_WIDTH, 4f))
        val seam = 120f + random.nextFloat() * 760f
        drawRect(Color(0xFFB98A55), Offset(seam, py), Size(4f, plank))
        circle(seam - 14f, py + 14f, 3f, Color(0xFF8D6E63))
        circle(seam + 18f, py + plank - 14f, 3f, Color(0xFF8D6E63))
    }
    repeat(2) {
        val x = 60f + random.nextFloat() * (WORLD_WIDTH - 120f)
        val cy = y + random.nextFloat() * TILE
        when (random.nextInt(3)) {
            0 -> {
                shape(Shapes.wedge, x, cy, 34f, Color(0xFFFFC94A), rotationDegrees = random.nextFloat() * 360f)
                circle(x - 6f, cy + 2f, 5f, Color(0xFFF2A93B))
            }
            1 -> for (k in 0 until 5) line(x + k * 6f, cy, x + k * 8f - 10f, cy - 24f, Color(0xFFF7D774), 3f)
            else -> {
                oval(x, cy, 46f, 30f, Color(0xFF5D4037))
                oval(x, cy + 4f, 40f, 22f, Color(0xFF3E2723))
            }
        }
    }
}

private fun DrawScope.parkRow(y: Float, random: Random, row: Int) {
    if (row % 2 == 0) drawRect(Color(0xFF98DB8A), Offset(0f, y), Size(WORLD_WIDTH, TILE / 2f))
    for (k in 0..6) {
        val fy = y + k * TILE / 6f
        val worldY = row * TILE + (TILE - k * TILE / 6f)
        val px = WORLD_WIDTH / 2f + sin(worldY / 420f) * 260f
        circle(px, fy, 70f, Color(0xFFE9D8A6))
    }
    repeat(4) {
        val x = random.nextFloat() * WORLD_WIDTH
        val fy = y + random.nextFloat() * TILE
        when (random.nextInt(4)) {
            0 -> {
                for (k in 0 until 5) {
                    val a = k * 1.2566f
                    circle(x + kotlin.math.cos(a) * 7f, fy + kotlin.math.sin(a) * 7f, 6f, Color(0xFFFFF59D))
                }
                circle(x, fy, 5f, Color(0xFFFF8A65))
            }
            1 -> {
                circle(x, fy, 34f, Color(0xFF4CAF50))
                circle(x - 26f, fy + 10f, 26f, Color(0xFF43A047))
                circle(x + 24f, fy + 8f, 28f, Color(0xFF66BB6A))
            }
            2 -> {
                drawRoundRect(Color(0xFFE53935), Offset(x - 12f, fy - 26f), Size(24f, 44f), CornerRadius(8f))
                circle(x, fy - 26f, 12f, Color(0xFFC62828))
            }
            else -> for (k in 0 until 3) circle(x + k * 9f, fy, 4f, Color(0xFFFFFFFF).copy(alpha = 0.8f))
        }
    }
}

private fun DrawScope.forestRow(y: Float, random: Random) {
    repeat(3) {
        val x = random.nextFloat() * WORLD_WIDTH
        val fy = y + random.nextFloat() * TILE
        oval(x, fy + 10f, 120f, 50f, Color(0xFF244D2E))
    }
    repeat(3) {
        val x = random.nextFloat() * WORLD_WIDTH
        val fy = y + random.nextFloat() * TILE
        when (random.nextInt(4)) {
            0, 1 -> {
                val r = 60f + random.nextFloat() * 40f
                circle(x + 12f, fy + 14f, r, Color(0xFF1E4428))
                circle(x, fy, r, Color(0xFF3F7F4A))
                circle(x - r * 0.3f, fy - r * 0.3f, r * 0.55f, Color(0xFF5FA060))
                if (random.nextInt(3) == 0) drawHoneycomb(x + r * 0.3f, fy + r * 0.3f)
            }
            2 -> {
                oval(x, fy, 34f, 22f, Color(0xFFE53935))
                drawRect(Color(0xFFFFF3E0), Offset(x - 6f, fy + 6f), Size(12f, 16f))
                circle(x - 7f, fy - 3f, 3f, Color.White)
                circle(x + 6f, fy - 1f, 3f, Color.White)
            }
            else -> for (k in 0 until 4) oval(x + k * 14f, fy + (k % 2) * 8f, 16f, 9f, Color(0xFFC0CA33).copy(alpha = 0.8f))
        }
    }
}

private fun DrawScope.drawHoneycomb(x: Float, y: Float) {
    for (k in 0 until 3) {
        val cx = x + (k % 2) * 16f
        val cy = y + k * 14f
        circle(cx, cy, 9f, Color(0xFFFFB300))
        circle(cx, cy, 5f, Color(0xFFFFE082))
    }
}


/** 溫泉河：兩岸的石頭、水面波紋、漂浮的柚子。 */
private fun DrawScope.onsenRow(y: Float, random: Random, row: Int, time: Float) {
    if (row % 2 == 0) drawRect(Color(0xFF86D6CB), Offset(0f, y), Size(WORLD_WIDTH, TILE / 2f))
    // 兩岸
    for (side in 0..1) {
        val bx = if (side == 0) 0f else WORLD_WIDTH - 110f
        drawRect(Color(0xFF9C8F84), Offset(bx, y), Size(110f, TILE))
        repeat(3) {
            val sx = bx + 20f + random.nextFloat() * 70f
            val sy = y + random.nextFloat() * TILE
            oval(sx, sy, 60f + random.nextFloat() * 30f, 40f, Color(0xFFB8ADA2))
            oval(sx - 6f, sy - 6f, 34f, 18f, Color(0xFFD3CABF))
        }
    }
    // 波紋
    repeat(4) {
        val wx = 150f + random.nextFloat() * (WORLD_WIDTH - 300f)
        val wy = y + random.nextFloat() * TILE
        val r = 30f + (sin(time * 1.5f + it + row) + 1f) * 10f
        drawOval(Color.White.copy(alpha = 0.35f), Offset(wx - r, wy - r * 0.35f), Size(r * 2f, r * 0.7f), style = Stroke(3f))
    }
    // 漂浮的柚子
    if (random.nextInt(3) == 0) {
        val fx = 180f + random.nextFloat() * (WORLD_WIDTH - 360f)
        val fy = y + random.nextFloat() * TILE
        circle(fx, fy, 16f, Color(0xFFFFC233))
        circle(fx - 5f, fy - 5f, 5f, Color(0xFFFFE89A))
        oval(fx + 8f, fy - 16f, 12f, 6f, Color(0xFF7ED957))
    }
}
// ───────────── 子彈、武器與特效 ─────────────

private fun DrawScope.drawBullet(b: EnemyBullet, sprites: SpriteCache) {
    val sprite = sprites.bullet(b.style, b.radius)
    val scale = b.radius / SpriteCache.bulletReference(b.radius) * (1f + sin(b.age * 16f) * 0.06f)
    val rotation = when {
        b.style == BulletStyle.RICE -> atan2(b.vy, b.vx) * RAD
        b.spin -> b.age * 300f
        else -> 0f
    }
    drawSprite(sprite, b.x, b.y, scale, rotation)
}

/** 玩家子彈：冷色硬光＋深藍描邊（明亮背景上也清楚）＋拖尾。 */
private fun DrawScope.drawShot(s: PlayerShot, w: World, sprites: SpriteCache) {
    when (s.style) {
        ShotStyle.PELLET -> {
            trail(s, Cyan, 10f, 0.05f)
            drawSprite(sprites.plasma(), s.x, s.y, 1f, atan2(s.vy, s.vx) * RAD + 90f)
        }
        ShotStyle.PAW -> {
            // 橘貓專武：重力肉球砲（黑洞＋全息肉球）
            val scale = s.radius / 26f
            trail(s, Violet, s.radius * 0.8f, 0.06f)
            drawSprite(sprites.glow(0xFFB388FFL), s.x, s.y, scale * 1.6f, alpha = 0.5f, blendMode = BlendMode.Plus)
            drawSprite(sprites.graviton(), s.x, s.y, scale, s.age * 220f)
        }
        ShotStyle.CLAW -> {
            trail(s, Cyan, 6f, 0.025f)
            drawSprite(sprites.monoClaw(), s.x, s.y, 1f)
        }
        ShotStyle.MISSILE -> drawSprite(sprites.fishMissile(), s.x, s.y, 1f, atan2(s.vy, s.vx) * RAD)
        ShotStyle.SONIC -> for (k in 0..2) {
            // 力場盾的發射體：往上飛的青色 V 形
            val y = s.y + k * 14f
            val r = 22f - k * 4f
            drawLine(Cyan.copy(alpha = 1f - k * 0.3f), Offset(s.x - r, y + r * 0.5f), Offset(s.x, y - r * 0.2f), 5f, StrokeCap.Square)
            drawLine(Cyan.copy(alpha = 1f - k * 0.3f), Offset(s.x + r, y + r * 0.5f), Offset(s.x, y - r * 0.2f), 5f, StrokeCap.Square)
        }
    }
}

/** 子彈後方的拖尾：深藍外框＋色光＋白色核心。 */
private fun DrawScope.trail(s: PlayerShot, color: Color, width: Float, seconds: Float) {
    if (DebugFlags.noTrail) return
    val start = Offset(s.x, s.y)
    val end = Offset(s.x - s.vx * seconds, s.y - s.vy * seconds)
    drawLine(Navy.copy(alpha = 0.5f), start, end, width + 4f, StrokeCap.Round)
    drawLine(color.copy(alpha = 0.7f), start, end, width, StrokeCap.Round)
    drawLine(Color.White.copy(alpha = 0.85f), start, Offset((s.x + end.x) / 2f, (s.y + end.y) / 2f), width * 0.3f, StrokeCap.Round)
}

private val arcPath = Path()

/** 集中型：磁軌光束。白色細核心＋青色外殼＋往上流動的螺旋線圈＋側邊電弧；命中處有六角衝擊。 */
private fun DrawScope.drawBeam(w: World, sprites: SpriteCache) {
    val b = w.beam
    if (!b.active) return
    val top = b.top
    val height = b.bottom - top
    if (height <= 0f) return
    val pulse = 1f + sin(w.time * 40f) * 0.08f
    val width = b.width * pulse
    drawRect(Navy.copy(alpha = 0.45f), Offset(b.x - width * 0.62f, top), Size(width * 1.24f, height))
    drawRect(
        Brush.horizontalGradient(listOf(Color(0x002979FF), Cyan, Color.White, Cyan, Color(0x002979FF)), b.x - width * 0.6f, b.x + width * 0.6f),
        Offset(b.x - width * 0.6f, top),
        Size(width * 1.2f, height),
    )
    drawRect(Color.White, Offset(b.x - width * 0.09f, top), Size(width * 0.18f, height))
    var k = 0
    while (k < 30) {
        val y = b.bottom - ((w.time * 1500f + k * 70f) % height)
        if (y < top) break
        drawOval(Cyan.copy(alpha = 0.8f), Offset(b.x - width * 0.85f, y - width * 0.12f), Size(width * 1.7f, width * 0.24f), style = Stroke(2.5f))
        k++
    }
    // 側邊電弧：每 0.06 秒換一個形狀
    val seed = (w.time / 0.06f).toInt()
    val rnd = Random(seed)
    repeat(3) {
        val y0 = top + rnd.nextFloat() * height
        val side = if (rnd.nextBoolean()) 1f else -1f
        arcPath.reset()
        var ax = b.x + side * width * 0.4f
        var ay = y0
        arcPath.moveTo(ax, ay)
        repeat(5) {
            ax += side * (10f + rnd.nextFloat() * 14f)
            ay += (rnd.nextFloat() - 0.3f) * 26f
            arcPath.lineTo(ax, ay)
        }
        drawPath(arcPath, Cyan, style = Stroke(3f, join = StrokeJoin.Bevel))
        drawPath(arcPath, Color.White, style = Stroke(1.2f))
    }
    drawSprite(sprites.glow(0xFF00E5FFL), b.x, b.bottom, width / 50f * 1.1f, alpha = 0.7f, blendMode = BlendMode.Plus)
    if (top > 0f) {
        drawSprite(sprites.glow(0xFFFFFFFFL), b.x, top, width / 50f * 2.2f * pulse, alpha = 0.8f, blendMode = BlendMode.Plus)
        val r = width * 1.3f
        drawPath(hexPath(b.x, top, r, w.time * 2f), Cyan, style = Stroke(4f))
        drawPath(hexPath(b.x, top, r * 0.6f, -w.time * 3f), Color.White, style = Stroke(2.5f))
    }
}

private val hexScratch = Path()

private fun hexPath(cx: Float, cy: Float, r: Float, rot: Float): Path {
    hexScratch.reset()
    for (i in 0 until 6) {
        val a = rot + i * 1.0471976f
        val x = cx + cos(a) * r
        val y = cy + sin(a) * r
        if (i == 0) hexScratch.moveTo(x, y) else hexScratch.lineTo(x, y)
    }
    hexScratch.close()
    return hexScratch
}

/** Boss 科技道具的能量色（暖色系）。 */
private fun energy(style: HazardStyle): Color = when (style) {
    HazardStyle.FORK, HazardStyle.TAIL -> Color(0xFFFF2BD6)
    HazardStyle.BONE, HazardStyle.CLAW, HazardStyle.BEAM -> Color(0xFFFF3D5A)
    HazardStyle.DIPPER -> Amber
    HazardStyle.WATER -> Color(0xFFFF7A45)
}

/** 雷射與 Boss 的道具攻擊。預告時畫紅色虛線（掃擊另外畫出掃過的範圍），發動時道具伸出去。 */
private fun DrawScope.drawHazard(l: Laser, w: World, sprites: SpriteCache) {
    if (l.age < l.warnTime) {
        val blink = if ((l.age * 12f).toInt() % 2 == 0) 0.95f else 0.45f
        val warn = Color(0xFFFF3D5A)
        val dash = if (DebugFlags.plainFx) null else PathEffect.dashPathEffect(floatArrayOf(26f, 18f), -l.age * 200f)
        drawLine(warn.copy(alpha = blink), Offset(l.x, l.y), Offset(l.endX, l.endY), 7f, StrokeCap.Round, pathEffect = dash)
        if (l.sweep != 0f) {
            val endAngle = l.angle + l.sweep * l.activeTime
            for (k in 1..4) {
                val a = l.angle + (endAngle - l.angle) * k / 4f
                drawLine(warn.copy(alpha = 0.25f * blink), Offset(l.x, l.y), Offset(l.x + cos(a) * l.length, l.y + sin(a) * l.length), 4f, pathEffect = dash)
            }
        }
        if (l.style == HazardStyle.BEAM) {
            drawSprite(sprites.glow(0xFFFF3D5AL), l.x, l.y, 0.3f + l.age * 0.8f, blendMode = BlendMode.Plus)
        } else if (l.style != HazardStyle.CLAW && l.style != HazardStyle.TAIL) {
            // 道具往後拉、蓄力發抖
            val shake = sin(l.age * 60f) * 4f
            val back = 0.75f + 0.25f * (1f - l.age / l.warnTime)
            drawProp(l.style, l.x + shake, l.y, l.angle, idleLength(l.style) * back, propWidth(l.style), w.time, sprites, active = false)
        }
        return
    }
    if (!l.active) return
    when (l.style) {
        HazardStyle.BEAM -> drawBeamLaser(l, sprites)
        else -> drawProp(l.style, l.x, l.y, l.angle, maxOf(l.reach, idleLength(l.style) * 0.6f), propWidth(l.style), w.time, sprites, l.width, active = true)
    }
}

private val TailPink = Color(0xFFFFCADA)
private val tailPath = Path()

/** 從 (x0,y0) 朝 angle 伸出 length 長的科技道具；active 時多一層能量光。 */
private fun DrawScope.drawProp(
    style: HazardStyle,
    x0: Float,
    y0: Float,
    angle: Float,
    length: Float,
    width: Float,
    time: Float,
    sprites: SpriteCache,
    hitWidth: Float = width,
    active: Boolean,
) {
    val dx = cos(angle)
    val dy = sin(angle)
    val tipX = x0 + dx * length
    val tipY = y0 + dy * length
    val glow = energy(style)
    fun shaft(fromBase: Float, fromTip: Float) {
        val sx = x0 + dx * fromBase
        val sy = y0 + dy * fromBase
        val ex = tipX - dx * fromTip
        val ey = tipY - dy * fromTip
        if ((ex - sx) * dx + (ey - sy) * dy <= 0f) return
        if (active) drawLine(glow.copy(alpha = 0.35f), Offset(sx, sy), Offset(ex, ey), width * 2.6f, StrokeCap.Round)
        drawLine(Navy, Offset(sx, sy), Offset(ex, ey), width + 8f, StrokeCap.Round)
        drawLine(Steel, Offset(sx, sy), Offset(ex, ey), width, StrokeCap.Round)
        drawLine(glow, Offset(sx, sy), Offset(ex, ey), width * 0.3f, StrokeCap.Round)
    }
    when (style) {
        HazardStyle.FORK -> {
            shaft(0f, 120f)
            sprites.propHead(style)?.let { drawSprite(it, tipX - dx * 68f, tipY - dy * 68f, 1f, angle * RAD) }
        }
        HazardStyle.BONE -> {
            shaft(58f, 58f)
            sprites.propHead(style)?.let {
                drawSprite(it, tipX - dx * 32f, tipY - dy * 32f, 1f, angle * RAD)
                drawSprite(it, x0 + dx * 32f, y0 + dy * 32f, 1f, angle * RAD + 180f)
            }
        }
        HazardStyle.DIPPER -> {
            shaft(0f, 100f)
            sprites.propHead(style)?.let { drawSprite(it, tipX - dx * 57f, tipY - dy * 57f, 1f, angle * RAD) }
        }
        HazardStyle.WATER -> {
            // 柚子砲在手上，伸出去的是高壓溫泉水柱
            if (active && length > 130f) {
                val mx = x0 + dx * 126f
                val my = y0 + dy * 126f
                val m = Offset(mx, my)
                val t = Offset(tipX, tipY)
                drawLine(glow.copy(alpha = 0.35f), m, t, hitWidth * 2.2f, StrokeCap.Round)
                drawLine(glow, m, t, hitWidth, StrokeCap.Round)
                drawLine(Color(0xFFFFE0CC), m, t, hitWidth * 0.55f, StrokeCap.Round)
                drawLine(Color.White, m, t, hitWidth * 0.18f, StrokeCap.Round)
                val reach = length - 126f
                var k = 0
                while (k < 10) {
                    val f = ((time * 2.5f + k * 0.1f) % 1f)
                    val px = mx + dx * reach * f - dy * sin(k * 2.3f + time * 9f) * hitWidth * 0.8f
                    val py = my + dy * reach * f + dx * sin(k * 2.3f + time * 9f) * hitWidth * 0.8f
                    circle(px, py, 5f, Color.White.copy(alpha = 0.8f))
                    k++
                }
            }
            sprites.propHead(style)?.let { drawSprite(it, x0 + dx * 62f, y0 + dy * 62f, 1f, angle * RAD) }
        }
        HazardStyle.TAIL -> {
            // 甩動的電漿尾巴：粉紅尾巴外包洋紅能量
            tailPath.reset()
            val nx = -dy
            val ny = dx
            val steps = 24
            for (i in 0..steps) {
                val t = i / steps.toFloat()
                val wave = sin(t * 9f - time * 14f) * 22f * t
                val px = x0 + dx * length * t + nx * wave
                val py = y0 + dy * length * t + ny * wave
                if (i == 0) tailPath.moveTo(px, py) else tailPath.lineTo(px, py)
            }
            drawPath(tailPath, glow.copy(alpha = 0.35f), style = Stroke(hitWidth * 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(tailPath, Navy, style = Stroke(hitWidth + 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(tailPath, TailPink, style = Stroke(hitWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(tailPath, glow, style = Stroke(hitWidth * 0.25f, cap = StrokeCap.Round))
        }
        HazardStyle.CLAW -> {
            // 三道紅色硬光爪痕
            val nx = -dy
            val ny = dx
            for (k in -1..1) {
                val off = k * hitWidth * 0.42f
                val s = Offset(x0 + nx * off, y0 + ny * off)
                val e = Offset(tipX + nx * off, tipY + ny * off)
                drawLine(glow.copy(alpha = 0.35f), s, e, 30f, StrokeCap.Square)
                drawLine(glow, s, e, 12f, StrokeCap.Square)
                drawLine(Color.White, s, e, 4f, StrokeCap.Square)
            }
        }
        HazardStyle.BEAM -> Unit
    }
}

private fun DrawScope.drawBeamLaser(l: Laser, sprites: SpriteCache) {
    val color = l.color.toColor()
    val start = Offset(l.x, l.y)
    val end = Offset(l.endX, l.endY)
    val fade = minOf(1f, (l.warnTime + l.activeTime - l.age) * 6f)
    val pulse = 1f + sin(l.age * 50f) * 0.1f
    drawLine(color.copy(alpha = 0.35f * fade), start, end, l.width * 2.6f * pulse, StrokeCap.Round, blendMode = BlendMode.Plus)
    drawLine(color.copy(alpha = fade), start, end, l.width * pulse, StrokeCap.Round)
    drawLine(Color.White.copy(alpha = fade), start, end, l.width * 0.4f, StrokeCap.Round)
    drawSprite(sprites.glow(0xFFFF3D5AL), l.x, l.y, l.width / 50f * 2.2f * pulse, alpha = fade, blendMode = BlendMode.Plus)
}

/** 力場盾：沿著弧線排列的六角格＋白色邊緣。 */
private fun DrawScope.drawSonicWall(w: SonicWall) {
    val fade = minOf(1f, (w.life - w.age) * 3f) * minOf(1f, w.age * 8f)
    val degrees = w.halfAngle * RAD
    val topLeft = Offset(w.x - w.radius, w.centerY - w.radius)
    val size = Size(w.radius * 2f, w.radius * 2f)
    drawArc(Cyan.copy(alpha = 0.18f * fade), -90f - degrees, degrees * 2f, false, topLeft, size, style = Stroke(w.thickness * 2f))
    val cell = 15f
    val steps = (w.radius * w.halfAngle * 2f / (cell * 1.7f)).toInt().coerceAtLeast(3)
    for (row in 0..1) {
        val r = w.radius + (row - 0.5f) * cell * 1.5f
        for (i in 0..steps) {
            val a = -1.5707964f - w.halfAngle + (w.halfAngle * 2f) * (i + row * 0.5f) / steps
            if (a > -1.5707964f + w.halfAngle) continue
            drawPath(hexPath(w.x + cos(a) * r, w.centerY + sin(a) * r, cell, 0.5235988f), Cyan.copy(alpha = 0.55f * fade), style = Stroke(2f))
        }
    }
    drawArc(Color.White.copy(alpha = 0.9f * fade), -90f - degrees, degrees * 2f, false, topLeft, size, style = Stroke(4f))
}

private fun DrawScope.drawParticle(p: Particle, sprites: SpriteCache) {
    val t = p.progress
    when (p.kind) {
        ParticleKind.SPARK -> drawLine(
            p.color.toColor().copy(alpha = 1f - t),
            Offset(p.x, p.y),
            Offset(p.x - p.vx * 0.045f, p.y - p.vy * 0.045f),
            p.size * (1f - t * 0.5f),
            StrokeCap.Round,
        )
        ParticleKind.FIRE -> {
            val s = p.size * (0.6f + t * 0.9f) / 50f
            drawSprite(sprites.puff(), p.x, p.y, s, alpha = (1f - t) * (1f - t * 0.3f))
        }
        ParticleKind.SMOKE -> drawSprite(sprites.smoke(), p.x, p.y, p.size * (0.6f + t) / 50f, alpha = 0.5f * (1f - t))
        ParticleKind.FLASH -> drawSprite(sprites.glow(0xFFFFFFFFL), p.x, p.y, p.size / 50f * (0.7f + t * 0.5f), alpha = 1f - t, blendMode = BlendMode.Plus)
        ParticleKind.STAR -> drawSprite(sprites.star(), p.x, p.y, p.size / 22f * (1f - t * 0.4f), t * 360f, alpha = 1f - t * t)
        ParticleKind.HEART -> drawSprite(sprites.heart(), p.x, p.y, p.size / 18f * (1f - t * 0.3f), alpha = 1f - t)
        ParticleKind.RING -> drawCircle(p.color.toColor().copy(alpha = 1f - t), p.size * (0.2f + t), Offset(p.x, p.y), style = Stroke(14f * (1f - t) + 1f))
        ParticleKind.SHOCKWAVE -> {
            val r = p.size * (0.25f + t * 0.75f)
            drawCircle(p.color.toColor().copy(alpha = (1f - t) * 0.9f), r, Offset(p.x, p.y), style = Stroke(10f * (1f - t) + 2f))
            drawCircle(Color.White.copy(alpha = (1f - t) * 0.7f), r * 1.15f, Offset(p.x, p.y), style = Stroke(2f))
        }
        ParticleKind.VOXEL -> if (!DebugFlags.noConfetti) withTransform({
            translate(p.x, p.y)
            rotate(p.rotation, Offset.Zero)
        }) {
            val s = p.size * (1f - t * 0.4f)
            drawRect(Navy.copy(alpha = 1f - t * t), Offset(-s / 2f - 1.5f, -s / 2f - 1.5f), Size(s + 3f, s + 3f))
            drawRect(p.color.toColor().copy(alpha = 1f - t * t), Offset(-s / 2f, -s / 2f), Size(s, s))
        }
        ParticleKind.HITMARK -> drawSprite(sprites.hitMark(), p.x, p.y, 0.8f + t * 0.5f, 45f * t, alpha = 1f - t)
        ParticleKind.HEXHIT -> drawSprite(sprites.hexPaw(), p.x, p.y, 0.8f + t * 0.4f, alpha = 1f - t * t)
        ParticleKind.CASING -> drawSprite(sprites.casing(), p.x, p.y, 1f, p.rotation, alpha = 1f - t * t)
        ParticleKind.CATHOLO -> drawSprite(sprites.catHolo(), p.x, p.y, 0.7f + t * 0.5f, alpha = 1f - t)
        ParticleKind.PROP -> drawSprite(sprites.lostProp(p.tag), p.x, p.y, 1f, p.rotation, alpha = minOf(1f, (1f - t) * 4f))
        ParticleKind.SFX -> {
            val pop = if (t < 0.15f) 0.5f + t / 0.15f * 0.7f else 1.2f - minOf(0.2f, (t - 0.15f) * 2f)
            // 色差抖動：前段左右錯位幾下
            val jx = if (t < 0.3f) sin(t * 120f) * 6f else 0f
            drawSprite(sprites.sfx(p.tag), p.x + jx, p.y, pop, alpha = minOf(1f, (1f - t) * 3.5f))
        }
        ParticleKind.SCORE -> {
            val pop = if (t < 0.12f) 0.6f + t / 0.12f * 0.6f else 1.2f - minOf(0.2f, (t - 0.12f) * 2f)
            drawNumber(p.tag, p.x, p.y, p.size * pop, sprites, plus = true, alpha = minOf(1f, (1f - t) * 3f))
        }
    }
}

private val digitBuffer = IntArray(10)

/** 用數字圖畫出一個數字（置中或靠右）。 */
private fun DrawScope.drawNumber(value: Int, cx: Float, cy: Float, scale: Float, sprites: SpriteCache, plus: Boolean = false, alpha: Float = 1f, alignRight: Boolean = false) {
    var n = value.coerceAtLeast(0)
    var count = 0
    do {
        digitBuffer[count++] = n % 10
        n /= 10
    } while (n > 0 && count < digitBuffer.size)
    val step = SpriteCache.DIGIT_WIDTH * 0.72f * scale
    val total = (count + if (plus) 1 else 0) * step
    var x = (if (alignRight) cx - total else cx - total / 2f) + step / 2f
    if (plus) {
        drawSprite(sprites.plus(), x, cy, scale * 0.8f, alpha = alpha)
        x += step
    }
    for (i in count - 1 downTo 0) {
        drawSprite(sprites.digit(digitBuffer[i]), x, cy, scale, alpha = alpha)
        x += step
    }
}

/** 連續擊破數（3 連以上才顯示），右上角，每次增加時彈一下。 */
private fun DrawScope.drawCombo(w: World, sprites: SpriteCache) {
    if (w.combo < 3 || w.comboTime <= 0f) return
    val pop = ((w.comboTime - (World.COMBO_TIME - 0.12f)) / 0.12f).coerceIn(0f, 1f)
    val alpha = minOf(1f, w.comboTime * 3f)
    val scale = 1.3f + pop * 0.45f
    val right = WORLD_WIDTH - 24f
    drawSprite(sprites.comboLabel(), right - 65f, 130f, 1f, alpha = alpha)
    drawNumber(w.combo, right - 20f, 70f, scale, sprites, alpha = alpha, alignRight = true)
    drawSprite(sprites.times(), right - 30f - (digitCount(w.combo) + 0.6f) * SpriteCache.DIGIT_WIDTH * 0.72f * scale, 74f, scale * 0.7f, alpha = alpha)
}

private fun digitCount(value: Int): Int {
    var n = value
    var c = 1
    while (n >= 10) {
        n /= 10
        c++
    }
    return c
}

/** 大絕 OVERDRIVE：天降琥珀軌道砲光柱＋六角衝擊波＋巨大肉球印（萌點）。 */
private fun DrawScope.drawBlast(w: World, sprites: SpriteCache) {
    val r = w.blastRadius
    val p = w.player
    val k = (r / (w.height * 1.6f)).coerceIn(0f, 1f)
    val beamW = 120f * (1f - k)
    if (beamW > 2f) {
        drawRect(
            Brush.horizontalGradient(listOf(Color(0x00FFB300), Amber, Color.White, Amber, Color(0x00FFB300)), p.x - beamW, p.x + beamW),
            Offset(p.x - beamW, 0f),
            Size(beamW * 2f, p.y),
            alpha = 0.9f,
        )
    }
    for (ring in 0..2) {
        val rr = r * (1f - ring * 0.18f)
        if (rr > 0f) drawPath(hexPath(p.x, p.y, rr, 0.5235988f), Amber.copy(alpha = (1f - k) * (0.9f - ring * 0.25f)), style = Stroke(10f - ring * 3f))
    }
    drawSprite(sprites.pawStamp(), p.x, p.y, 0.6f + k * 2.2f, alpha = (1f - k) * 0.9f)
}
