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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
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
import com.purpal.catplayground.shooter.model.WORLD_WIDTH
import com.purpal.catplayground.shooter.model.World
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

fun DrawScope.drawWorld(w: World, view: Viewport, sprites: SpriteCache) {
    drawRect(themeEdge(w.stage.theme))
    val shake = if (w.shakeTime > 0f) sin(w.time * 90f) * 10f * minOf(1f, w.shakeTime * 3f) else 0f
    withTransform({
        translate(view.offsetX + shake, view.offsetY + shake * 0.6f)
        scale(view.scale, view.scale, Offset.Zero)
    }) {
        clipRect(0f, 0f, WORLD_WIDTH, w.height) {
            drawBackground(w.stage.theme, w.scroll, w.height, w.time)
            // 背景稍微壓暗，讓角色、子彈與特效更突出。
            drawRect(Color(0xFF140A26).copy(alpha = 0.24f), size = Size(WORLD_WIDTH, w.height))
            for (wall in w.walls) drawSonicWall(wall)
            for (item in w.items) drawItem(item, sprites)
            for (e in w.enemies) drawEnemy(e, w, sprites)
            for (s in w.playerShots) drawShot(s, w, sprites)
            drawBeam(w, sprites)
            drawPlayer(w, sprites)
            for (l in w.lasers) drawHazard(l, w, sprites)
            for (b in w.bullets) drawBullet(b, sprites)
            for (p in w.particles) drawParticle(p, sprites)
            if (w.blastRadius >= 0f) drawBlast(w)
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
        val aura = if (p.cat == CatType.ORANGE) 0xFFFFC107L else 0xFFB388FFL
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
        drawSprite(sprites.muzzle(), p.x, p.y - 64f, flicker, w.time * 600f, blendMode = BlendMode.Plus)
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
    // 被打中時果凍般壓扁一下並閃白；持續被光束照射時改成閃爍，避免整隻一直泛白。
    val hit = e.hitFlash > 0f
    val flash = hit && ((w.time * 20f).toInt() % 2 == 0)
    val squash = if (hit) 1.07f else 1f
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
        EnemyKind.BALL_TURRET -> {
            sprite(sprites.barrel(), rotation = (e as BallTurret).aim * RAD)
            sprite(sprites.enemy(e.kind, 0))
        }
        EnemyKind.BEEHIVE -> sprite(sprites.enemy(e.kind, 0), rotation = e.age * 200f)
        EnemyKind.MOUSE_PLANE -> sprite(sprites.enemy(e.kind, fast), flipX = (e as MousePlane).facing < 0f)
        EnemyKind.MINI_MOUSE -> sprite(sprites.enemy(e.kind, fast), 0.8f)
        EnemyKind.MOUSE_RUNNER, EnemyKind.BEE, EnemyKind.PUPPY -> sprite(sprites.enemy(e.kind, fast))
        EnemyKind.BALLOON -> {
            // 氣球本體在判定圓上，道具掛在線的下端。
            sprite(sprites.enemy(e.kind, 0), dy = 30f)
            val drop = (e as Balloon).drop
            if (drop != null) drawSprite(sprites.item(drop), e.x, e.y + 104f, 0.85f)
        }
        EnemyKind.ELITE_MOUSE, EnemyKind.ELITE_DOG, EnemyKind.ELITE_BEE -> {
            val pulse = 3.4f + sin(w.time * 6f + e.id) * 0.3f
            drawSprite(sprites.glow(0xFFFFE07AL), e.x, e.y, pulse, alpha = 0.55f, blendMode = BlendMode.Plus)
            sprite(sprites.enemy(e.kind, if (e.kind == EnemyKind.ELITE_BEE) fast else slow))
        }
        EnemyKind.BOSS_MOUSE, EnemyKind.BOSS_DOG, EnemyKind.BOSS_BEAR -> drawBoss(e as Boss, w, sprites, flash, squash)
        else -> sprite(sprites.enemy(e.kind, slow))
    }
}

/** Boss：心情隨血量變化（逞強 → 生氣 → 被打哭），手上的道具沒在攻擊時拿在手上晃。 */
private fun DrawScope.drawBoss(boss: Boss, w: World, sprites: SpriteCache, flash: Boolean, squash: Float) {
    val defeated = w.phase == Phase.BOSS_DEFEATED
    val mood = if (defeated) 2 else boss.phase
    val frame = ((w.time * (if (mood == 2) 6f else 3f)).toInt()) and 1
    val wobble = if (defeated) sin(w.time * 50f) * 6f else 0f
    val x = boss.x + wobble
    val held = boss.heldProp(boss.phase)
    val busy = held != null && w.lasers.any { it.owner === boss && it.style == held }
    val sprite = sprites.boss(boss.kind, mood, frame)
    drawSprite(sprite, x, boss.y, rotationDegrees = wobble * 0.5f, stretch = squash)
    if (flash || defeated && (w.time * 10f).toInt() % 2 == 0) {
        drawSprite(sprite, x, boss.y, alpha = 0.3f, colorFilter = WhiteFlash, rotationDegrees = wobble * 0.5f, stretch = squash)
    }
    if (held != null && !busy && !defeated) {
        val sway = sin(w.time * 2.4f) * 8f
        drawProp(held, x + boss.handX, boss.y + boss.handY, (-68f + sway) / RAD, idleLength(held), propWidth(held), w.time, sprites)
    }
}

private fun idleLength(style: HazardStyle) = when (style) {
    HazardStyle.FORK -> 230f
    HazardStyle.BONE -> 190f
    HazardStyle.DIPPER -> 210f
    else -> 120f
}

private fun propWidth(style: HazardStyle) = when (style) {
    HazardStyle.FORK -> 16f
    HazardStyle.BONE -> 28f
    HazardStyle.DIPPER -> 18f
    else -> 24f
}

private fun DrawScope.drawItem(item: Item, sprites: SpriteCache) {
    if (item.type == ItemType.CANDY) {
        drawSprite(sprites.candy(item.variant), item.x, item.y, 1f, item.age * 420f)
        return
    }
    val bob = sin(item.age * 5f) * 4f
    if (item.type != ItemType.COIN) {
        drawSprite(sprites.glow(0xFFFFFFFFL), item.x, item.y, 0.9f + sin(item.age * 6f) * 0.15f, alpha = 0.35f, blendMode = BlendMode.Plus)
    }
    drawSprite(sprites.item(item.type), item.x, item.y + bob, 0.95f)
    if (item.cycle != null) {
        // 倒數圈：繞一圈就換成下一種武器。
        val r = 44f
        drawArc(
            Color.White.copy(alpha = 0.9f), -90f, 360f * item.cycleProgress, false,
            Offset(item.x - r, item.y + bob - r), Size(r * 2f, r * 2f), style = Stroke(5f, cap = StrokeCap.Round),
        )
    }
}

// ───────────── 背景 ─────────────

private fun themeEdge(theme: StageTheme): Color = when (theme) {
    StageTheme.CHEESE_BARN -> Color(0xFF6D4C2F)
    StageTheme.PARK -> Color(0xFF3E7D3A)
    StageTheme.HONEY_FOREST -> Color(0xFF1B3A22)
}

private const val TILE = 300f

private fun DrawScope.drawBackground(theme: StageTheme, scroll: Float, height: Float, time: Float) {
    when (theme) {
        StageTheme.CHEESE_BARN -> drawRect(Color(0xFFE6C08A), size = Size(WORLD_WIDTH, height))
        StageTheme.PARK -> drawRect(Color(0xFF8ED47F), size = Size(WORLD_WIDTH, height))
        StageTheme.HONEY_FOREST -> drawRect(Color(0xFF2E5E3A), size = Size(WORLD_WIDTH, height))
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

// ───────────── 子彈、武器與特效 ─────────────

private fun DrawScope.drawBullet(b: EnemyBullet, sprites: SpriteCache) {
    val sprite = sprites.bullet(b.style, b.radius)
    // 果凍般微微脈動
    val scale = b.radius / SpriteCache.bulletReference(b.radius) * (1f + sin(b.age * 16f) * 0.07f)
    val rotation = when {
        b.style == BulletStyle.RICE -> atan2(b.vy, b.vx) * RAD
        b.spin -> b.age * 300f
        else -> 0f
    }
    drawSprite(sprite, b.x, b.y, scale, rotation)
}

private val OrangeTrail = Color(0xFFFFB45C)
private val VioletTrail = Color(0xFFC9A8FF)

/** 玩家子彈：白色貼紙描邊＋光暈＋長拖尾。 */
private fun DrawScope.drawShot(s: PlayerShot, w: World, sprites: SpriteCache) {
    when (s.style) {
        ShotStyle.PELLET -> {
            val trail = if (w.player.cat == CatType.ORANGE) OrangeTrail else VioletTrail
            trail(s, trail, 16f, 0.05f)
            drawSprite(sprites.pawShot(w.player.cat), s.x, s.y, 1f, atan2(s.vy, s.vx) * RAD + 90f)
        }
        ShotStyle.PAW -> {
            val scale = s.radius / 30f
            trail(s, OrangeTrail, s.radius * 1.4f, 0.09f)
            drawSprite(sprites.bigPaw(), s.x, s.y, scale)
        }
        ShotStyle.CLAW -> {
            trail(s, VioletTrail, 10f, 0.03f)
            drawSprite(sprites.claw(), s.x, s.y, 1f)
        }
        ShotStyle.MISSILE -> {
            drawSprite(sprites.glow(0xFFFFFFFFL), s.x - s.vx * 0.02f, s.y - s.vy * 0.02f, 0.3f, alpha = 0.7f, blendMode = BlendMode.Plus)
            drawSprite(sprites.fish(), s.x, s.y, 1f, atan2(s.vy, s.vx) * RAD)
        }
        ShotStyle.SONIC -> for (k in 0..2) {
            val r = 14f + k * 10f + (s.age * 70f) % 10f
            drawArc(Color(0xFFD9CCFF).copy(alpha = 0.95f - k * 0.25f), 200f, 140f, false, Offset(s.x - r, s.y - r), Size(r * 2, r * 2), style = Stroke(6f, cap = StrokeCap.Round))
        }
    }
}

/** 子彈後方的拖尾：外層半透明粗線＋內層白線。 */
private fun DrawScope.trail(s: PlayerShot, color: Color, width: Float, seconds: Float) {
    val start = Offset(s.x, s.y)
    val end = Offset(s.x - s.vx * seconds, s.y - s.vy * seconds)
    drawLine(color.copy(alpha = 0.45f), start, end, width, StrokeCap.Round)
    drawLine(Color.White.copy(alpha = 0.7f), start, Offset((s.x + end.x) / 2f, (s.y + end.y) / 2f), width * 0.35f, StrokeCap.Round)
}

private val Rainbow = listOf(
    Color(0x00B5E3FF), Color(0xFFB5E3FF), Color(0xFFC9B8FF), Color(0xFFFFC2E1), Color.White,
    Color(0xFFFFE7A8), Color(0xFFC2F5D9), Color(0xFFB5E3FF), Color(0x00B5E3FF),
)

/** 集中型：彩虹棉花糖光束，白色核心、往上流動的能量環與泡泡，命中處開花。 */
private fun DrawScope.drawBeam(w: World, sprites: SpriteCache) {
    val b = w.beam
    if (!b.active) return
    val top = b.top
    val height = b.bottom - top
    if (height <= 0f) return
    val pulse = 1f + sin(w.time * 40f) * 0.08f
    val width = b.width * pulse
    drawRect(
        Brush.horizontalGradient(listOf(Color.Transparent, Color(0x99FFC2E1), Color.Transparent), b.x - width * 1.5f, b.x + width * 1.5f),
        Offset(b.x - width * 1.5f, top),
        Size(width * 3f, height),
    )
    drawRect(Brush.horizontalGradient(Rainbow, b.x - width * 0.6f, b.x + width * 0.6f), Offset(b.x - width * 0.6f, top), Size(width * 1.2f, height))
    drawRect(Color.White, Offset(b.x - width * 0.13f, top), Size(width * 0.26f, height))
    var k = 0
    while (k < 30) {
        val y = b.bottom - ((w.time * 1300f + k * 120f) % height)
        if (y < top) break
        drawOval(Color.White.copy(alpha = 0.75f), Offset(b.x - width * 0.75f, y - width * 0.14f), Size(width * 1.5f, width * 0.28f), style = Stroke(3f))
        k++
    }
    for (i in 0 until 6) {
        val y = b.bottom - ((w.time * 700f + i * 170f) % height)
        if (y < top) continue
        val side = if (i % 2 == 0) -1f else 1f
        val bx = b.x + side * (width * 0.8f + sin(w.time * 6f + i) * 8f)
        circle(bx, y, 7f + (i % 3) * 2f, Color.White)
        drawCircle(Color(0xFFC9B8FF), 7f + (i % 3) * 2f, Offset(bx, y), style = Stroke(2.5f))
    }
    drawSprite(sprites.glow(0xFFC9B8FFL), b.x, b.bottom, width / 50f * 1.2f, alpha = 0.8f, blendMode = BlendMode.Plus)
    if (top > 0f) {
        drawSprite(sprites.glow(0xFFFFFFFFL), b.x, top, width / 50f * 2.2f * pulse, alpha = 0.8f, blendMode = BlendMode.Plus)
        drawSprite(sprites.flower(), b.x, top, width / 60f * pulse, w.time * 240f)
    }
}

/** 雷射與 Boss 的近身攻擊。預告時畫虛線（掃擊另外畫出掃過的範圍），發動時道具伸出去。 */
private fun DrawScope.drawHazard(l: Laser, w: World, sprites: SpriteCache) {
    if (l.age < l.warnTime) {
        val blink = if ((l.age * 12f).toInt() % 2 == 0) 0.95f else 0.45f
        val warn = Color(0xFFFF3D8B)
        val dash = PathEffect.dashPathEffect(floatArrayOf(26f, 18f), -l.age * 200f)
        drawLine(warn.copy(alpha = blink), Offset(l.x, l.y), Offset(l.endX, l.endY), 7f, StrokeCap.Round, pathEffect = dash)
        if (l.sweep != 0f) {
            val endAngle = l.angle + l.sweep * l.activeTime
            for (k in 1..4) {
                val a = l.angle + (endAngle - l.angle) * k / 4f
                drawLine(warn.copy(alpha = 0.25f * blink), Offset(l.x, l.y), Offset(l.x + cos(a) * l.length, l.y + sin(a) * l.length), 4f, pathEffect = dash)
            }
        }
        if (l.style == HazardStyle.BEAM) {
            drawSprite(sprites.glow(0xFFFF8FB1L), l.x, l.y, 0.3f + l.age * 0.8f, blendMode = BlendMode.Plus)
        } else if (l.style != HazardStyle.CLAW && l.style != HazardStyle.TAIL) {
            // 道具往後拉、蓄力發抖
            val shake = sin(l.age * 60f) * 4f
            val back = 0.75f + 0.25f * (1f - l.age / l.warnTime)
            drawProp(l.style, l.x + shake, l.y, l.angle, idleLength(l.style) * back, propWidth(l.style), w.time, sprites)
        }
        return
    }
    if (!l.active) return
    when (l.style) {
        HazardStyle.BEAM -> drawBeamLaser(l, sprites)
        else -> drawProp(l.style, l.x, l.y, l.angle, maxOf(l.reach, idleLength(l.style) * 0.6f), propWidth(l.style), w.time, sprites, l.width)
    }
}

private val ForkSilver = Color(0xFFF2F0F8)
private val Ink = Color(0xFF6B4C52)
private val BoneCream = Color(0xFFFFF8EC)
private val Wood = Color(0xFFC98B55)
private val TailPink = Color(0xFFFFCADA)
private val tailPath = Path()

/** 從 (x0,y0) 朝 angle 伸出 length 長的道具。 */
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
) {
    val dx = cos(angle)
    val dy = sin(angle)
    val tipX = x0 + dx * length
    val tipY = y0 + dy * length
    fun shaft(fromBase: Float, fromTip: Float, color: Color) {
        val sx = x0 + dx * fromBase
        val sy = y0 + dy * fromBase
        val ex = tipX - dx * fromTip
        val ey = tipY - dy * fromTip
        if ((ex - sx) * dx + (ey - sy) * dy <= 0f) return
        drawLine(Ink, Offset(sx, sy), Offset(ex, ey), width + 8f, StrokeCap.Round)
        drawLine(color, Offset(sx, sy), Offset(ex, ey), width, StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.7f), Offset(sx - dy * width * 0.2f, sy + dx * width * 0.2f), Offset(ex - dy * width * 0.2f, ey + dx * width * 0.2f), width * 0.25f, StrokeCap.Round)
    }
    when (style) {
        HazardStyle.FORK -> {
            shaft(0f, 120f, ForkSilver)
            sprites.propHead(style)?.let { drawSprite(it, tipX - dx * 68f, tipY - dy * 68f, 1f, angle * RAD) }
        }
        HazardStyle.BONE -> {
            shaft(55f, 55f, BoneCream)
            sprites.propHead(style)?.let {
                drawSprite(it, tipX - dx * 30f, tipY - dy * 30f, 1f, angle * RAD)
                drawSprite(it, x0 + dx * 30f, y0 + dy * 30f, 1f, angle * RAD + 180f)
            }
        }
        HazardStyle.DIPPER -> {
            shaft(0f, 96f, Wood)
            sprites.propHead(style)?.let { drawSprite(it, tipX - dx * 50f, tipY - dy * 50f, 1f, angle * RAD) }
        }
        HazardStyle.TAIL -> {
            // 甩動的粉紅尾巴：沿著線段畫一條波浪
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
            drawPath(tailPath, Ink, style = Stroke(hitWidth + 8f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            drawPath(tailPath, TailPink, style = Stroke(hitWidth, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            drawPath(tailPath, Color.White.copy(alpha = 0.6f), style = Stroke(hitWidth * 0.25f, cap = StrokeCap.Round))
        }
        HazardStyle.CLAW -> {
            // 三道爪痕
            val nx = -dy
            val ny = dx
            for (k in -1..1) {
                val off = k * hitWidth * 0.42f
                val s = Offset(x0 + nx * off, y0 + ny * off)
                val e = Offset(tipX + nx * off, tipY + ny * off)
                drawLine(Color(0x66FF8FB1), s, e, 30f, StrokeCap.Round)
                drawLine(Color(0xFFFF4F7B), s, e, 14f, StrokeCap.Round)
                drawLine(Color.White, s, e, 5f, StrokeCap.Round)
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
    drawSprite(sprites.glow(0xFF80F0FFL), l.x, l.y, l.width / 50f * 2.2f * pulse, alpha = fade, blendMode = BlendMode.Plus)
}

private fun DrawScope.drawSonicWall(w: SonicWall) {
    val fade = minOf(1f, (w.life - w.age) * 3f) * minOf(1f, w.age * 8f)
    val degrees = w.halfAngle * RAD
    val topLeft = Offset(w.x - w.radius, w.centerY - w.radius)
    val size = Size(w.radius * 2f, w.radius * 2f)
    val pulse = 1f + sin(w.age * 20f) * 0.1f
    drawArc(Color(0xFFB9A7FF).copy(alpha = 0.45f * fade), -90f - degrees, degrees * 2f, false, topLeft, size, style = Stroke(w.thickness * 2f * pulse), blendMode = BlendMode.Plus)
    drawArc(Color(0xFFFFFFFF).copy(alpha = 0.95f * fade), -90f - degrees, degrees * 2f, false, topLeft, size, style = Stroke(5f))
    val inner = w.radius - w.thickness * 0.6f
    drawArc(Color(0xFFFFC2E1).copy(alpha = 0.7f * fade), -90f - degrees, degrees * 2f, false, Offset(w.x - inner, w.centerY - inner), Size(inner * 2f, inner * 2f), style = Stroke(3f))
}

private fun DrawScope.drawParticle(p: Particle, sprites: SpriteCache) {
    val t = p.progress
    when (p.kind) {
        ParticleKind.SPARK -> drawLine(
            p.color.toColor().copy(alpha = 1f - t),
            Offset(p.x, p.y),
            Offset(p.x - p.vx * 0.035f, p.y - p.vy * 0.035f),
            p.size * (1f - t * 0.5f),
            StrokeCap.Round,
        )
        ParticleKind.FIRE -> {
            val s = p.size * (0.6f + t * 0.9f) / 50f
            drawSprite(sprites.puff(), p.x, p.y, s, alpha = (1f - t) * (1f - t * 0.3f))
        }
        ParticleKind.SMOKE -> drawSprite(sprites.smoke(), p.x, p.y, p.size * (0.6f + t) / 50f, alpha = 0.6f * (1f - t))
        ParticleKind.FLASH -> drawSprite(sprites.glow(0xFFFFFFFFL), p.x, p.y, p.size / 50f * (0.7f + t * 0.5f), alpha = 1f - t, blendMode = BlendMode.Plus)
        ParticleKind.STAR -> drawSprite(sprites.star(), p.x, p.y, p.size / 22f * (1f - t * 0.4f), t * 360f, alpha = 1f - t * t)
        ParticleKind.HEART -> drawSprite(sprites.heart(), p.x, p.y, p.size / 18f * (1f - t * 0.3f), alpha = 1f - t)
        ParticleKind.RING -> drawCircle(p.color.toColor().copy(alpha = 1f - t), p.size * (0.2f + t), Offset(p.x, p.y), style = Stroke(14f * (1f - t) + 1f))
        ParticleKind.CONFETTI -> withTransform({
            translate(p.x, p.y)
            rotate(p.rotation, Offset.Zero)
            // 紙片翻面：寬度隨轉動忽寬忽窄
            scale(0.3f + 0.7f * kotlin.math.abs(cos(p.rotation / 40f)), 1f, Offset.Zero)
        }) {
            drawRect(p.color.toColor().copy(alpha = (1f - t * t)), Offset(-p.size / 2f, -p.size * 0.3f), Size(p.size, p.size * 0.6f))
        }
        ParticleKind.PROP -> drawSprite(sprites.lostProp(p.tag), p.x, p.y, 1f, p.rotation, alpha = minOf(1f, (1f - t) * 4f))
        ParticleKind.SFX -> {
            val pop = if (t < 0.15f) 0.5f + t / 0.15f * 0.7f else 1.2f - minOf(0.2f, (t - 0.15f) * 2f)
            drawSprite(sprites.sfx(p.tag), p.x, p.y, pop, alpha = minOf(1f, (1f - t) * 3.5f))
        }
        ParticleKind.SCORE -> {
            val pop = if (t < 0.12f) 0.6f + t / 0.12f * 0.6f else 1.2f - minOf(0.2f, (t - 0.12f) * 2f)
            drawNumber(p.tag, p.x, p.y, p.size * pop, sprites, plus = true, alpha = minOf(1f, (1f - t) * 3f))
        }
        ParticleKind.FLOWER -> drawSprite(sprites.flower(), p.x, p.y, p.size / 60f * (0.6f + t * 0.6f), t * 120f, alpha = 1f - t)
    }
}

private val digitBuffer = IntArray(10)

/** 用數字圖畫出一個數字（置中）。 */
private fun DrawScope.drawNumber(value: Int, cx: Float, cy: Float, scale: Float, sprites: SpriteCache, plus: Boolean = false, alpha: Float = 1f, alignRight: Boolean = false) {
    var n = value.coerceAtLeast(0)
    var count = 0
    do {
        digitBuffer[count++] = n % 10
        n /= 10
    } while (n > 0 && count < digitBuffer.size)
    val step = SpriteCache.DIGIT_WIDTH * 0.78f * scale
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
    drawSprite(sprites.comboLabel(), right - 75f, 130f, 1f, -4f, alpha = alpha)
    drawNumber(w.combo, right - 20f, 70f, scale, sprites, alpha = alpha, alignRight = true)
    drawSprite(sprites.times(), right - 30f - (digitCount(w.combo) + 0.6f) * SpriteCache.DIGIT_WIDTH * 0.78f * scale, 74f, scale * 0.7f, alpha = alpha)
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

private fun DrawScope.drawBlast(w: World) {
    val r = w.blastRadius
    val p = w.player
    val color = if (p.cat == CatType.ORANGE) Color(0xFFFFC107) else Color(0xFFB388FF)
    drawCircle(
        Brush.radialGradient(
            0.6f to Color.Transparent,
            0.9f to color.copy(alpha = 0.6f),
            1f to Color.White,
            center = Offset(p.x, p.y),
            radius = r.coerceAtLeast(1f),
        ),
        r,
        Offset(p.x, p.y),
        blendMode = BlendMode.Plus,
    )
}
