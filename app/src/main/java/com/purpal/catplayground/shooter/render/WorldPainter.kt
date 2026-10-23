package com.purpal.catplayground.shooter.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
            drawRect(Color(0xFF140A26).copy(alpha = 0.28f), size = Size(WORLD_WIDTH, w.height))
            for (wall in w.walls) drawSonicWall(wall)
            for (item in w.items) {
                val bob = sin(item.age * 5f) * 4f
                if (item.type != ItemType.COIN) drawSprite(sprites.glow(0xFFFFFFFFL), item.x, item.y, 0.9f + sin(item.age * 6f) * 0.15f, alpha = 0.35f, blendMode = BlendMode.Plus)
                drawSprite(sprites.item(item.type), item.x, item.y + bob, 0.95f)
            }
            for (e in w.enemies) drawEnemy(e, w, sprites)
            for (s in w.playerShots) drawShot(s, sprites)
            drawBeam(w, sprites)
            drawPlayer(w, sprites)
            for (l in w.lasers) drawLaser(l, sprites)
            for (b in w.bullets) drawBullet(b, sprites)
            for (p in w.particles) drawParticle(p, sprites)
            if (w.blastRadius >= 0f) drawBlast(w)
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
        drawSprite(sprites.glow(aura), p.x, p.y, 2.2f + sin(w.time * 12f) * 0.2f, alpha = 0.7f, blendMode = BlendMode.Plus)
    }
    val blink = p.invulnerable > 0f && !beast && (w.time * 14f).toInt() % 2 == 0
    val bank = (p.vx / p.cat.baseSpeed).coerceIn(-1.5f, 1.5f) * 12f
    val frame = (w.time * 16f).toInt() % 4
    drawSprite(sprites.cat(p.cat, frame, beast), p.x, p.y, if (beast) 1.4f else 1.15f, bank, alpha = if (blink) 0.35f else 1f)
    val shooting = w.firing && (w.phase == Phase.STAGE || w.phase == Phase.BOSS || w.phase == Phase.WARNING)
    if (shooting && p.mainWeapon != MainWeapon.FOCUS) {
        val flicker = 0.45f + ((w.time * 30f).toInt() % 3) * 0.12f
        drawSprite(sprites.muzzle(), p.x, p.y - 58f, flicker, w.time * 600f, blendMode = BlendMode.Plus)
    }
    // 判定點
    if (w.firing) {
        circle(p.x, p.y + 4f, 8f, Color.White)
        shape(Shapes.heart, p.x, p.y + 4f, 6.5f, Color(0xFFFF2D6F))
    }
}

private fun DrawScope.drawEnemy(e: Enemy, w: World, sprites: SpriteCache) {
    val slow = ((w.time * 4f).toInt() + e.id) % 2
    val fast = ((w.time * 18f).toInt() + e.id) % 2
    // 被打中時白色閃一下；持續被光束照射時改成閃爍，避免整隻一直泛白。
    val flash = e.hitFlash > 0f && ((w.time * 20f).toInt() % 2 == 0)
    fun sprite(s: Sprite, scale: Float = 1f, rotation: Float = 0f, flipX: Boolean = false) {
        drawSprite(s, e.x, e.y, scale, rotation, flipX = flipX)
        if (flash) drawSprite(s, e.x, e.y, scale, rotation, alpha = 0.5f, flipX = flipX, colorFilter = WhiteFlash)
    }
    when (e.kind) {
        EnemyKind.CHEESE_DRONE -> {
            sprite(sprites.cheeseWheel(), rotation = (e as CheeseDrone).spin * 57.3f)
            sprite(sprites.enemy(e.kind, 0, 0))
        }
        EnemyKind.FRISBEE -> sprite(sprites.frisbee(), rotation = (e as Frisbee).spin * 57.3f)
        EnemyKind.BALL_TURRET -> {
            sprite(sprites.enemy(e.kind, 0, 0))
            sprite(sprites.barrel(), rotation = (e as BallTurret).aim * 57.3f)
        }
        EnemyKind.BEEHIVE -> sprite(sprites.beehive(), rotation = e.age * 200f)
        EnemyKind.MOUSE_PLANE -> sprite(sprites.enemy(e.kind, fast, 0), flipX = (e as MousePlane).facing < 0f)
        EnemyKind.MINI_MOUSE -> sprite(sprites.enemy(e.kind, fast, 0), 0.8f)
        EnemyKind.BEE -> sprite(sprites.enemy(e.kind, fast, 0))
        EnemyKind.BALLOON -> sprite(sprites.enemy(e.kind, 0, 0, (e as Balloon).drop))
        EnemyKind.BOSS_MOUSE, EnemyKind.BOSS_DOG, EnemyKind.BOSS_BEAR -> {
            val boss = e as Boss
            val defeated = w.phase == Phase.BOSS_DEFEATED
            val wobble = if (defeated) sin(w.time * 50f) * 6f else 0f
            drawSprite(sprites.enemy(e.kind, slow, boss.phase), e.x + wobble, e.y, rotationDegrees = wobble * 0.5f)
            if (flash || defeated && (w.time * 10f).toInt() % 2 == 0) {
                drawSprite(sprites.enemy(e.kind, slow, boss.phase), e.x + wobble, e.y, alpha = 0.3f, colorFilter = WhiteFlash)
            }
        }
        else -> sprite(sprites.enemy(e.kind, slow, 0))
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
    val sprite = sprites.bullet(b.style, b.color, b.radius)
    val scale = b.radius / SpriteCache.bulletReference(b.radius)
    val rotation = when {
        b.style == BulletStyle.RICE -> atan2(b.vy, b.vx) * 57.3f
        b.spin -> b.age * 300f
        else -> 0f
    }
    drawSprite(sprite, b.x, b.y, scale, rotation)
}

private fun DrawScope.drawShot(s: PlayerShot, sprites: SpriteCache) {
    when (s.style) {
        ShotStyle.PELLET -> drawSprite(sprites.pellet(), s.x, s.y, 1f, atan2(s.vy, s.vx) * 57.3f + 90f)
        ShotStyle.PAW -> {
            val scale = s.radius / 26f
            drawSprite(sprites.fire(), s.x, s.y + 40f * scale, scale * 0.9f, alpha = 0.35f, blendMode = BlendMode.Plus)
            drawSprite(sprites.paw(), s.x, s.y + 22f * scale, scale * 0.85f, alpha = 0.4f, blendMode = BlendMode.Plus)
            drawSprite(sprites.paw(), s.x, s.y, scale, blendMode = BlendMode.Plus)
        }
        ShotStyle.CLAW -> drawSprite(sprites.claw(), s.x, s.y, 1f)
        ShotStyle.MISSILE -> {
            val angle = atan2(s.vy, s.vx) * 57.3f
            drawSprite(sprites.glow(0xFF69F0AEL), s.x - s.vx * 0.02f, s.y - s.vy * 0.02f, 0.35f, blendMode = BlendMode.Plus)
            drawSprite(sprites.missile(), s.x, s.y, 1f, angle + 180f)
        }
        ShotStyle.SONIC -> for (k in 0..2) {
            val r = 14f + k * 10f + (s.age * 70f) % 10f
            drawArc(Color(0xFFD1A6FF).copy(alpha = 0.9f - k * 0.25f), 200f, 140f, false, Offset(s.x - r, s.y - r), Size(r * 2, r * 2), style = Stroke(6f), blendMode = BlendMode.Plus)
        }
    }
}

/** 集中型光束：外層光暈、青色本體、白色核心、往上流動的能量環，命中點有光球。 */
private fun DrawScope.drawBeam(w: World, sprites: SpriteCache) {
    val b = w.beam
    if (!b.active) return
    val top = b.top
    val height = b.bottom - top
    if (height <= 0f) return
    val pulse = 1f + sin(w.time * 40f) * 0.08f
    val width = b.width * pulse
    val cyan = Color(0xFF40E0FF)
    drawRect(
        Brush.horizontalGradient(listOf(Color.Transparent, cyan.copy(alpha = 0.45f), Color.Transparent), b.x - width * 1.6f, b.x + width * 1.6f),
        Offset(b.x - width * 1.6f, top),
        Size(width * 3.2f, height),
        blendMode = BlendMode.Plus,
    )
    drawRect(
        Brush.horizontalGradient(listOf(cyan.copy(alpha = 0.2f), cyan, Color.White, cyan, cyan.copy(alpha = 0.2f)), b.x - width / 2f, b.x + width / 2f),
        Offset(b.x - width / 2f, top),
        Size(width, height),
    )
    drawRect(Color.White, Offset(b.x - width * 0.14f, top), Size(width * 0.28f, height))
    var k = 0
    while (k < 30) {
        val y = b.bottom - ((w.time * 1500f + k * 110f) % height)
        if (y < top) break
        drawOval(Color.White.copy(alpha = 0.5f), Offset(b.x - width * 0.7f, y - width * 0.15f), Size(width * 1.4f, width * 0.3f), blendMode = BlendMode.Plus)
        k++
    }
    drawSprite(sprites.glow(0xFF80F0FFL), b.x, b.bottom, width / 50f * 1.4f, blendMode = BlendMode.Plus)
    if (top > 0f) {
        drawSprite(sprites.glow(0xFF80F0FFL), b.x, top, width / 50f * 2.4f * pulse, alpha = 0.9f, blendMode = BlendMode.Plus)
        drawSprite(sprites.muzzle(), b.x, top, width / 90f * 1.8f, w.time * 900f, alpha = 0.8f, blendMode = BlendMode.Plus)
    }
}

private fun DrawScope.drawLaser(l: Laser, sprites: SpriteCache) {
    val color = l.color.toColor()
    val start = Offset(l.x, l.y)
    val end = Offset(l.endX, l.endY)
    if (!l.active) {
        if (l.age < l.warnTime) {
            val blink = if ((l.age * 12f).toInt() % 2 == 0) 0.9f else 0.4f
            drawLine(color.copy(alpha = blink), start, end, 6f)
            drawLine(Color.White.copy(alpha = blink), start, end, 2f)
            drawSprite(sprites.glow(l.color), l.x, l.y, 0.3f + l.age * 0.8f, blendMode = BlendMode.Plus)
        }
        return
    }
    val fade = minOf(1f, (l.warnTime + l.activeTime - l.age) * 6f)
    val pulse = 1f + sin(l.age * 50f) * 0.1f
    drawLine(color.copy(alpha = 0.35f * fade), start, end, l.width * 2.6f * pulse, StrokeCap.Round, blendMode = BlendMode.Plus)
    drawLine(color.copy(alpha = fade), start, end, l.width * pulse, StrokeCap.Round)
    drawLine(Color.White.copy(alpha = fade), start, end, l.width * 0.4f, StrokeCap.Round)
    drawSprite(sprites.glow(l.color), l.x, l.y, l.width / 50f * 2.2f * pulse, alpha = fade, blendMode = BlendMode.Plus)
}

private fun DrawScope.drawSonicWall(w: SonicWall) {
    val fade = minOf(1f, (w.life - w.age) * 3f) * minOf(1f, w.age * 8f)
    val degrees = w.halfAngle * 57.3f
    val topLeft = Offset(w.x - w.radius, w.centerY - w.radius)
    val size = Size(w.radius * 2f, w.radius * 2f)
    val pulse = 1f + sin(w.age * 20f) * 0.1f
    drawArc(Color(0xFF9C6BFF).copy(alpha = 0.45f * fade), -90f - degrees, degrees * 2f, false, topLeft, size, style = Stroke(w.thickness * 2f * pulse), blendMode = BlendMode.Plus)
    drawArc(Color(0xFFEDE7F6).copy(alpha = 0.95f * fade), -90f - degrees, degrees * 2f, false, topLeft, size, style = Stroke(5f))
    val inner = w.radius - w.thickness * 0.6f
    drawArc(Color(0xFFD1C4E9).copy(alpha = 0.6f * fade), -90f - degrees, degrees * 2f, false, Offset(w.x - inner, w.centerY - inner), Size(inner * 2f, inner * 2f), style = Stroke(3f))
}

private fun DrawScope.drawParticle(p: Particle, sprites: SpriteCache) {
    val t = p.progress
    val color = p.color.toColor()
    when (p.kind) {
        ParticleKind.SPARK -> drawLine(
            color.copy(alpha = 1f - t),
            Offset(p.x, p.y),
            Offset(p.x - p.vx * 0.035f, p.y - p.vy * 0.035f),
            p.size * (1f - t * 0.5f),
            StrokeCap.Round,
            blendMode = BlendMode.Plus,
        )
        ParticleKind.FIRE -> {
            val s = p.size * (0.6f + t * 0.9f) / 50f
            drawSprite(sprites.fire(), p.x, p.y, s, alpha = (1f - t) * (1f - t * 0.3f))
            if (t < 0.35f) drawSprite(sprites.fire(), p.x, p.y, s * 0.7f, alpha = 1f - t / 0.35f, blendMode = BlendMode.Plus)
        }
        ParticleKind.SMOKE -> drawSprite(sprites.smoke(), p.x, p.y, p.size * (0.6f + t) / 50f, alpha = 0.6f * (1f - t))
        ParticleKind.FLASH -> drawSprite(sprites.glow(0xFFFFFFFFL), p.x, p.y, p.size / 50f * (0.7f + t * 0.5f), alpha = 1f - t, blendMode = BlendMode.Plus)
        ParticleKind.STAR -> shape(Shapes.star, p.x, p.y, p.size * (1f - t * 0.5f), color.copy(alpha = 1f - t), rotationDegrees = t * 360f)
        ParticleKind.HEART -> shape(Shapes.heart, p.x, p.y, p.size * (1f - t * 0.3f), color.copy(alpha = 1f - t))
        ParticleKind.RING -> drawCircle(color.copy(alpha = 1f - t), p.size * (0.2f + t), Offset(p.x, p.y), style = Stroke(14f * (1f - t) + 1f), blendMode = BlendMode.Plus)
    }
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
