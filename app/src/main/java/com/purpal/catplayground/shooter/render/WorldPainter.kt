package com.purpal.catplayground.shooter.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import com.purpal.catplayground.shooter.model.BulletStyle
import com.purpal.catplayground.shooter.model.CatType
import com.purpal.catplayground.shooter.model.EnemyBullet
import com.purpal.catplayground.shooter.model.Laser
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
class Viewport(val scale: Float, val offsetX: Float, val offsetY: Float) {
    fun toScreen(x: Float, y: Float) = Offset(offsetX + x * scale, offsetY + y * scale)
}

fun viewportFor(size: Size, worldHeight: Float): Viewport {
    val scale = minOf(size.width / WORLD_WIDTH, size.height / worldHeight)
    return Viewport(scale, (size.width - WORLD_WIDTH * scale) / 2f, (size.height - worldHeight * scale) / 2f)
}

fun DrawScope.drawWorld(w: World, view: Viewport) {
    drawRect(themeEdge(w.stage.theme))
    val shake = if (w.shakeTime > 0f) sin(w.time * 90f) * 10f * minOf(1f, w.shakeTime * 3f) else 0f
    withTransform({
        translate(view.offsetX + shake, view.offsetY + shake * 0.6f)
        scale(view.scale, view.scale, Offset.Zero)
    }) {
        clipRect(0f, 0f, WORLD_WIDTH, w.height) {
            drawBackground(w.stage.theme, w.scroll, w.height, w.time)
            for (wall in w.walls) drawSonicWall(wall)
            for (item in w.items) at(item.x, item.y, 1.05f) { drawItemBadge(item.type, item.age) }
            for (e in w.enemies) drawEnemy(e, w.time)
            for (s in w.playerShots) drawShot(s)
            drawPlayer(w)
            for (l in w.lasers) drawLaser(l)
            for (b in w.bullets) drawBullet(b)
            for (p in w.particles) drawParticle(p)
            if (w.blastRadius >= 0f) drawBlast(w)
        }
    }
}

private fun DrawScope.drawPlayer(w: World) {
    val p = w.player
    if (!p.alive || w.phase == Phase.PLAYER_DOWN || w.phase == Phase.CONTINUE) return
    val blinking = p.invulnerable > 0f && p.transformed <= 0f && (w.time * 14f).toInt() % 2 == 0
    if (blinking) return
    val bank = (p.vx / p.cat.baseSpeed) * 14f
    at(p.x, p.y, 1.3f) { drawCatShip(p.cat, w.time, bank, transformed = p.transformed > 0f) }
    // 判定點：小小的愛心
    if (w.firing) shape(Shapes.heart, p.x, p.y + 4f, 9f, Color(0xFFFF4081))
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
    // 前景飄落的小東西（灰塵、花瓣、葉子），比地面捲得快一點。
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
    // 蜿蜒的步道
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

// ───────────── 子彈與特效 ─────────────

private fun DrawScope.drawBullet(b: EnemyBullet) {
    val color = b.color.toColor()
    // 畫得比判定大一點，比較好看清楚；判定仍以 b.radius 計算。
    val r = b.radius * 1.3f
    circle(b.x, b.y, r * 1.7f, color.copy(alpha = 0.28f))
    when (b.style) {
        BulletStyle.ORB, BulletStyle.BIG_ORB, BulletStyle.SEED -> {
            circle(b.x, b.y, r, color)
            circle(b.x, b.y, r * 0.5f, Color.White)
        }
        BulletStyle.RICE -> {
            val angle = atan2(b.vy, b.vx) * 57.3f
            at(b.x, b.y, rotationDegrees = angle) {
                oval(0f, 0f, r * 2.6f, r * 1.5f, color)
                oval(0f, 0f, r * 1.6f, r * 0.7f, Color.White)
            }
        }
        BulletStyle.CHEESE -> {
            shape(Shapes.wedge, b.x, b.y, r * 1.2f, color, rotationDegrees = b.age * 300f)
            circle(b.x, b.y, r * 0.3f, Color(0xFFFFA000))
        }
        BulletStyle.BONE -> at(b.x, b.y, rotationDegrees = b.age * 420f) {
            drawRoundRect(color, Offset(-r, -r * 0.3f), Size(r * 2f, r * 0.6f), CornerRadius(r * 0.3f))
            for (sx in listOf(-1f, 1f)) for (sy in listOf(-1f, 1f)) circle(sx * r, sy * r * 0.32f, r * 0.38f, color)
        }
        BulletStyle.NOTE -> {
            circle(b.x - r * 0.2f, b.y + r * 0.35f, r * 0.65f, color)
            line(b.x + r * 0.35f, b.y + r * 0.35f, b.x + r * 0.35f, b.y - r, color, r * 0.3f)
            line(b.x + r * 0.35f, b.y - r, b.x + r, b.y - r * 0.6f, color, r * 0.3f)
            circle(b.x - r * 0.35f, b.y + r * 0.2f, r * 0.2f, Color.White)
        }
        BulletStyle.HONEY -> {
            shape(Shapes.drop, b.x, b.y, r, color)
            circle(b.x - r * 0.3f, b.y, r * 0.25f, Color.White.copy(alpha = 0.8f))
        }
        BulletStyle.STAR -> {
            shape(Shapes.star, b.x, b.y, r * 1.25f, color, rotationDegrees = b.age * 360f)
            circle(b.x, b.y, r * 0.3f, Color.White)
        }
    }
}

private fun DrawScope.drawShot(s: PlayerShot) {
    when (s.style) {
        ShotStyle.PELLET -> {
            oval(s.x, s.y, 18f, 30f, Color(0xFF40C4FF).copy(alpha = 0.35f))
            at(s.x, s.y, rotationDegrees = atan2(s.vy, s.vx) * 57.3f + 90f) { oval(0f, 0f, 9f, 22f, Color(0xFFB3E5FC)) }
        }
        ShotStyle.FOCUS -> {
            drawRoundRect(Color(0xFFFF6E40).copy(alpha = 0.4f), Offset(s.x - 9f, s.y - 22f), Size(18f, 44f), CornerRadius(9f))
            drawRoundRect(Color(0xFFFFE0B2), Offset(s.x - 4f, s.y - 18f), Size(8f, 36f), CornerRadius(4f))
        }
        ShotStyle.PAW -> {
            circle(s.x, s.y, s.radius * 1.4f, Color(0xFFFFB74D).copy(alpha = 0.35f))
            paw(s.x, s.y, s.radius, Color(0xFFFFE0B2))
            paw(s.x, s.y, s.radius * 0.7f, Color(0xFFFF8A65))
        }
        ShotStyle.NEEDLE -> {
            line(s.x, s.y - 20f, s.x, s.y + 16f, Color(0xFFE040FB).copy(alpha = 0.4f), 10f)
            line(s.x, s.y - 18f, s.x, s.y + 14f, Color(0xFFF3E5F5), 4f)
        }
        ShotStyle.MISSILE -> at(s.x, s.y, rotationDegrees = atan2(s.vy, s.vx) * 57.3f + 180f) {
            circle(18f, 0f, 7f, Color(0xFFB9F6CA).copy(alpha = 0.5f))
            fish(0f, 0f, 14f, Color(0xFF00E676))
            circle(-6f, -2f, 2f, Color(0xFF1B5E20))
        }
        ShotStyle.SONIC -> for (k in 0..2) {
            val r = 14f + k * 9f + (s.age * 60f) % 9f
            drawArc(Color(0xFFD1A6FF).copy(alpha = 0.9f - k * 0.25f), 200f, 140f, false, Offset(s.x - r, s.y - r), Size(r * 2, r * 2), style = Stroke(5f))
        }
    }
}

private fun DrawScope.drawLaser(l: Laser) {
    val color = l.color.toColor()
    val start = Offset(l.x, l.y)
    val end = Offset(l.endX, l.endY)
    if (!l.active) {
        if (l.age < l.warnTime) {
            val blink = if ((l.age * 12f).toInt() % 2 == 0) 0.8f else 0.35f
            drawLine(color.copy(alpha = blink), start, end, 5f)
            circle(l.x, l.y, 14f + l.age * 20f, color.copy(alpha = 0.4f))
        }
        return
    }
    val fade = minOf(1f, (l.warnTime + l.activeTime - l.age) * 6f)
    drawLine(color.copy(alpha = 0.3f * fade), start, end, l.width * 2.2f)
    drawLine(color.copy(alpha = fade), start, end, l.width)
    drawLine(Color.White.copy(alpha = fade), start, end, l.width * 0.4f)
    circle(l.x, l.y, l.width * 1.3f, color.copy(alpha = 0.6f * fade))
    circle(l.x, l.y, l.width * 0.7f, Color.White.copy(alpha = fade))
}

private fun DrawScope.drawSonicWall(w: SonicWall) {
    val fade = minOf(1f, (w.life - w.age) * 3f) * minOf(1f, w.age * 8f)
    val degrees = w.halfAngle * 57.3f
    val topLeft = Offset(w.x - w.radius, w.centerY - w.radius)
    val size = Size(w.radius * 2f, w.radius * 2f)
    val pulse = 1f + sin(w.age * 20f) * 0.1f
    drawArc(Color(0xFFB388FF).copy(alpha = 0.45f * fade), -90f - degrees, degrees * 2f, false, topLeft, size, style = Stroke(w.thickness * 2f * pulse))
    drawArc(Color(0xFFEDE7F6).copy(alpha = 0.9f * fade), -90f - degrees, degrees * 2f, false, topLeft, size, style = Stroke(6f))
}

private fun DrawScope.drawParticle(p: Particle) {
    val t = p.progress
    val color = p.color.toColor()
    when (p.kind) {
        ParticleKind.SPARK -> circle(p.x, p.y, p.size * (1f - t), color.copy(alpha = 1f - t))
        ParticleKind.STAR -> shape(Shapes.star, p.x, p.y, p.size * (1f - t * 0.5f), color.copy(alpha = 1f - t), rotationDegrees = t * 360f)
        ParticleKind.HEART -> shape(Shapes.heart, p.x, p.y, p.size * (1f - t * 0.3f), color.copy(alpha = 1f - t))
        ParticleKind.RING -> drawCircle(color.copy(alpha = 1f - t), p.size * (0.2f + t), Offset(p.x, p.y), style = Stroke(10f * (1f - t) + 1f))
        ParticleKind.SMOKE -> circle(p.x, p.y, p.size * (0.5f + t), color.copy(alpha = 0.5f * (1f - t)))
        ParticleKind.FLASH -> circle(p.x, p.y, p.size * (0.6f + t * 0.4f), color.copy(alpha = 1f - t))
    }
}

private fun DrawScope.drawBlast(w: World) {
    val r = w.blastRadius
    val p = w.player
    val color = if (p.cat == CatType.ORANGE) Color(0xFFFFC107) else Color(0xFFB388FF)
    drawCircle(
        Brush.radialGradient(
            0.7f to Color.Transparent,
            0.95f to color.copy(alpha = 0.55f),
            1f to Color.White.copy(alpha = 0.9f),
            center = Offset(p.x, p.y),
            radius = r.coerceAtLeast(1f),
        ),
        r,
        Offset(p.x, p.y),
    )
}
