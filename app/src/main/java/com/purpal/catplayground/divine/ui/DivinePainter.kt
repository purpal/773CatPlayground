package com.purpal.catplayground.divine.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.purpal.catplayground.divine.model.Battle
import com.purpal.catplayground.divine.model.CardDef
import com.purpal.catplayground.divine.model.CardType
import com.purpal.catplayground.divine.model.Creature
import com.purpal.catplayground.divine.model.Enemy
import com.purpal.catplayground.divine.model.EnemyKind
import com.purpal.catplayground.divine.model.Target
import com.purpal.catplayground.ui.ArtImages
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** 拖牌時的提示：牌要落在哪裡、合不合法。 */
internal class DropHint(val card: CardDef, val lane: Int, val row: Int, val valid: Boolean)

internal class DivineSprites(private val context: Context) {
    fun get(name: String): ImageBitmap = ArtImages.get(context, name)
    fun unit(card: CardDef): ImageBitmap = get("dv_unit_${card.id}")
    fun foe(e: Enemy, time: Float): ImageBitmap = when (e.kind) {
        EnemyKind.BAT -> get("dv_foe_bat_${((time * 7f).toInt() + e.lane) and 1}")
        else -> get("dv_foe_${e.kind.id}")
    }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        color = 0xFF1A1030.toInt()
    }

    fun text(scope: DrawScope, s: String, x: Float, y: Float, size: Float, color: Color, alpha: Float = 1f) {
        fill.textSize = size
        fill.color = color.copy(alpha = alpha).toArgb()
        outline.textSize = size
        outline.strokeWidth = size * 0.2f
        outline.alpha = (alpha * 255).toInt()
        scope.drawIntoCanvas {
            it.nativeCanvas.drawText(s, x, y, outline)
            it.nativeCanvas.drawText(s, x, y, fill)
        }
    }
}

private val WhiteFlash = ColorFilter.tint(Color.White.copy(alpha = 0.75f), BlendMode.SrcAtop)
private val StunTint = ColorFilter.tint(Color(0xFF9FD8FF).copy(alpha = 0.45f), BlendMode.SrcAtop)
private val boltPath = Path()

/** 圖的中心對在 (x, y)（角色的身體中心在圖高的 57% 處）。 */
private fun DrawScope.sprite(
    image: ImageBitmap, x: Float, y: Float, size: Float, scaleX: Float = 1f, scaleY: Float = 1f, rotation: Float = 0f, alpha: Float = 1f, filter: ColorFilter? = null,
) {
    val w = size
    val h = size * image.height / image.width
    withTransform({
        translate(x, y)
        rotate(rotation, Offset.Zero)
        scale(scaleX, scaleY, Offset(0f, h * 0.36f))
    }) {
        drawImage(image, dstOffset = IntOffset((-w / 2f).toInt(), (-h * 0.571f).toInt()), dstSize = IntSize(w.toInt(), h.toInt()), alpha = alpha, colorFilter = filter)
    }
}

private fun DrawScope.glow(x: Float, y: Float, r: Float, color: Color, alpha: Float) {
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)), Offset(x, y), r), r, Offset(x, y), blendMode = BlendMode.Plus)
}

private fun DrawScope.bar(x: Float, y: Float, w: Float, ratio: Float, color: Color, h: Float = 13f) {
    drawRoundRect(Color(0xCC140C28), Offset(x - w / 2f - 3f, y - 3f), Size(w + 6f, h + 6f), androidx.compose.ui.geometry.CornerRadius(h))
    drawRoundRect(color, Offset(x - w / 2f, y), Size(w * ratio.coerceIn(0f, 1f), h), androidx.compose.ui.geometry.CornerRadius(h))
    drawRoundRect(Color.White.copy(alpha = 0.35f), Offset(x - w / 2f, y), Size(w * ratio.coerceIn(0f, 1f), h * 0.4f), androidx.compose.ui.geometry.CornerRadius(h))
}

/** 畫整個戰場（世界座標 1000×1400；呼叫前已經套好縮放與位移）。 */
internal fun DrawScope.drawBattle(b: Battle, fx: DivineFx, sprites: DivineSprites, time: Float, hint: DropHint?) {
    drawImage(sprites.get("dv_field"), dstOffset = IntOffset.Zero, dstSize = IntSize(Battle.WIDTH.toInt(), Battle.HEIGHT.toInt()))

    // 結界
    for (en in b.enchants) drawEnchant(en.lane, en.def, en.remaining / en.spec.duration, en.age, time, sprites)

    // Boss 的狐火預告
    for (t in b.telegraphs) {
        val x0 = t.lane * Battle.LANE_WIDTH
        val blink = if ((time * 10f).toInt() % 2 == 0) 0.42f else 0.22f
        drawRect(Color(0xFFFF3D7A).copy(alpha = blink), Offset(x0 + 14f, 760f), Size(172f, 560f))
        val dash = PathEffect.dashPathEffect(floatArrayOf(26f, 18f), -time * 200f)
        for (side in listOf(16f, 184f)) drawLine(Color(0xFFFF3D7A), Offset(x0 + side, 760f), Offset(x0 + side, 1320f), 7f, pathEffect = dash)
        sprites.text(this, "狐火！", x0 + 100f, 800f, 44f, Color(0xFFFFB0D0))
    }

    // 拖牌的提示
    if (hint != null) drawHint(hint, time)

    // 神靈與妖魔：由上往下畫，下面的蓋住上面的
    val creatures = b.creatures()
    val drawn = ArrayList<Any>(creatures.size + b.enemies.size)
    drawn.addAll(creatures)
    drawn.addAll(b.enemies)
    drawn.sortBy { if (it is Creature) it.y else (it as Enemy).y }
    for (o in drawn) if (o is Creature) drawCreature(o, b, sprites, time) else drawEnemy(o as Enemy, sprites, time)

    // 飛行中的遠程攻擊
    for (s in b.shots) {
        val c = DivineFx.shotColor(s.owner.def.id)
        val dx = s.target.x - s.x
        val dy = s.target.y - s.y
        val d = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        glow(s.x, s.y, 46f, c, 0.9f)
        drawLine(c.copy(alpha = 0.7f), Offset(s.x, s.y), Offset(s.x - dx / d * 70f, s.y - dy / d * 70f), 12f, StrokeCap.Round)
        drawCircle(Color.White, 10f, Offset(s.x, s.y))
    }

    for (p in fx.particles) if (p.age >= 0f) drawParticle(p, sprites)
}

private fun DrawScope.drawEnchant(lane: Int, card: CardDef, left: Float, age: Float, time: Float, sprites: DivineSprites) {
    val c = DivineFx.accent(card.id)
    val x0 = lane * Battle.LANE_WIDTH
    val cx = x0 + 100f
    val fade = minOf(1f, age * 3f) * minOf(1f, left * 8f)
    drawRect(Brush.verticalGradient(listOf(c.copy(alpha = 0.06f * fade), c.copy(alpha = 0.3f * fade)), 0f, 1320f), Offset(x0 + 14f, 0f), Size(172f, 1320f))
    for (side in listOf(16f, 184f)) {
        drawLine(c.copy(alpha = 0.85f * fade), Offset(x0 + side, 0f), Offset(x0 + side, 1320f), 5f)
        drawLine(Color.White.copy(alpha = 0.5f * fade), Offset(x0 + side, 0f), Offset(x0 + side, 1320f), 1.5f)
    }
    // 沿著車道流動的符文：對敵人的往下壓、對我方的往上升
    val hostile = card.enchant!!.enemySpeed < 1f || card.enchant.burn > 0f
    val flow = (time * (if (hostile) 90f else -120f)) % 160f
    var y = -160f + flow
    while (y < 1320f) {
        if (y > 0f) {
            val p = Path().apply {
                moveTo(cx, y - 16f); lineTo(cx + 16f, y); lineTo(cx, y + 16f); lineTo(cx - 16f, y); close()
            }
            drawPath(p, c.copy(alpha = 0.55f * fade), style = Stroke(3.5f))
        }
        y += 160f
    }
    // 轉動的法陣
    val ry = 700f
    withTransform({ rotate(time * 40f, Offset(cx, ry)) }) {
        drawCircle(c.copy(alpha = 0.8f * fade), 84f, Offset(cx, ry), style = Stroke(5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 14f))))
        for (k in 0 until 8) {
            val a = k * 0.7853982f
            drawLine(c.copy(alpha = 0.7f * fade), Offset(cx + cos(a) * 60f, ry + sin(a) * 60f), Offset(cx + cos(a) * 78f, ry + sin(a) * 78f), 5f, StrokeCap.Round)
        }
    }
    withTransform({ rotate(-time * 60f, Offset(cx, ry)) }) {
        drawCircle(Color.White.copy(alpha = 0.6f * fade), 56f, Offset(cx, ry), style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 12f))))
    }
    glow(cx, ry, 130f, c, 0.35f * fade)
    // 名字與剩餘時間
    drawArc(c.copy(alpha = fade), -90f, 360f * left, false, Offset(cx - 30f, ry - 30f), Size(60f, 60f), style = Stroke(8f, cap = StrokeCap.Round))
    sprites.text(this, card.name, cx, ry + 128f, 30f, Color.White, fade)
}

private fun DrawScope.drawHint(h: DropHint, time: Float) {
    val c = if (h.valid) Color(0xFF7FFFA8) else Color(0xFFFF5A5A)
    val pulse = 0.55f + sin(time * 9f) * 0.2f
    when (h.card.target) {
        Target.FIELD -> drawRect(c.copy(alpha = 0.16f * pulse), Offset.Zero, Size(Battle.WIDTH, Battle.TEMPLE_Y))
        Target.LANE -> {
            val x0 = h.lane * Battle.LANE_WIDTH
            drawRect(Brush.verticalGradient(listOf(c.copy(alpha = 0.1f), c.copy(alpha = 0.42f * pulse)), 0f, 1320f), Offset(x0 + 8f, 0f), Size(184f, 1320f))
            drawRect(c, Offset(x0 + 8f, 0f), Size(184f, 1320f), style = Stroke(6f))
        }
        Target.SLOT, Target.CREATURE -> {
            val cx = Battle.laneX(h.lane)
            val cy = Battle.ROW_Y[h.row] + 20f
            drawOval(c.copy(alpha = 0.4f * pulse), Offset(cx - 92f, cy - 62f), Size(184f, 150f))
            drawOval(c, Offset(cx - 92f, cy - 62f), Size(184f, 150f), style = Stroke(7f))
            // 遠程生物：畫出射程
            val unit = h.card.unit
            if (unit != null && h.valid) {
                val top = (Battle.ROW_Y[h.row] - unit.range).coerceAtLeast(0f)
                val x0 = h.lane * Battle.LANE_WIDTH
                drawRect(c.copy(alpha = 0.13f), Offset(x0 + 14f, top), Size(172f, Battle.ROW_Y[h.row] - top))
                drawLine(c.copy(alpha = 0.9f), Offset(x0 + 14f, top), Offset(x0 + 186f, top), 5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f)))
            }
        }
    }
}

private fun DrawScope.drawCreature(c: Creature, b: Battle, sprites: DivineSprites, time: Float) {
    // 入場：從光裡彈出來
    val pop = (c.age / 0.3f).coerceIn(0f, 1f)
    val grow = if (pop < 1f) 0.3f + pop * 0.85f + sin(pop * 3.14159f) * 0.15f else 1f
    val bob = sin(c.age * 3f + c.lane) * 3f
    var dy = bob
    var sx = 1f
    var sy = 1f
    val swing = (1f - c.attackAge / 0.2f).coerceIn(0f, 1f)
    if (c.unit.ranged) {
        // 遠程：出手時往後縮一下
        sy = 1f - swing * 0.1f
        sx = 1f + swing * 0.08f
    } else {
        dy -= swing * 34f
    }
    val hasted = c.haste > 0f
    if (hasted) glow(c.x, c.y, 120f, Color(0xFFD08AFF), 0.5f + sin(time * 12f) * 0.15f)
    if (c.atkBonus > 0f || b.laneAtkBonus(c.lane) > 0f) glow(c.x, c.y + 30f, 100f, Color(0xFFFFB86A), 0.35f)
    sprite(sprites.unit(c.def), c.x, c.y + dy, 196f, sx * grow, sy * grow, filter = if (c.hitFlash > 0f) WhiteFlash else null)
    // 血條
    val ratio = c.hp / c.maxHp
    bar(c.x, c.y + 74f, 120f, ratio, if (ratio > 0.5f) Color(0xFF5BE38A) else if (ratio > 0.25f) Color(0xFFFFC94A) else Color(0xFFFF5A5A))
    sprites.text(this, statText(kotlin.math.ceil(c.hp.toDouble()).toFloat()), c.x + 76f, c.y + 90f, 30f, Color.White)
    sprites.text(this, statText(b.attackOf(c)), c.x - 76f, c.y + 90f, 30f, Color(0xFFFFB86A))
    // 神器
    c.equips.forEachIndexed { i, e ->
        val ex = c.x - 62f + i * 44f
        val ey = c.y - 96f
        drawCircle(Color(0xFF1A1030), 21f, Offset(ex, ey))
        drawCircle(DivineFx.Gold, 21f, Offset(ex, ey), style = Stroke(4f))
        sprites.text(this, e.name.take(1), ex, ey + 11f, 28f, Color(0xFFFFE9A0))
    }
}

private fun DrawScope.drawEnemy(e: Enemy, sprites: DivineSprites, time: Float) {
    if (e.vanish > 0f) return
    val size = e.kind.radius * 3.5f
    val moving = !e.engaged && e.stun <= 0f
    val hop = if (moving && !e.kind.flying) -kotlin.math.abs(sin(e.age * (if (e.kind == EnemyKind.ZOMBIE) 5f else 9f))) * (if (e.kind == EnemyKind.ZOMBIE) 22f else 10f) else 0f
    val float = if (e.kind.flying) sin(e.age * 5f) * 10f - 30f else 0f
    val lunge = (1f - e.attackAge / 0.22f).coerceIn(0f, 1f) * 30f
    val rot = if (moving && !e.isBoss) sin(e.age * 8f) * 4f else 0f
    val y = e.y + hop + float + lunge
    if (e.isBoss) glow(e.x, e.y, 320f, Color(0xFFD08AFF), 0.3f + sin(time * 3f) * 0.08f)
    if (e.kind.flying) drawOval(Color(0x55140C28), Offset(e.x - 40f, e.y + 66f), Size(80f, 22f))
    sprite(sprites.foe(e, time), e.x, y, size, rotation = rot, filter = if (e.hitFlash > 0f) WhiteFlash else if (e.stun > 0f) StunTint else null)
    if (e.stun > 0f) {
        for (k in 0 until 3) {
            val a = time * 6f + k * 2.094f
            drawCircle(Color(0xFFFFE45C), 9f, Offset(e.x + cos(a) * e.kind.radius * 0.9f, y - e.kind.radius * 1.3f + sin(a) * 10f))
        }
    }
    if (e.hp < e.maxHp && !e.isBoss) bar(e.x, y - e.kind.radius * 1.55f, e.kind.radius * 1.7f, e.hp / e.maxHp, Color(0xFFFF6A8A), 10f)
}

private fun DrawScope.drawParticle(p: Particle, sprites: DivineSprites) {
    val t = p.t
    when (p.kind) {
        PKind.SPARK -> drawLine(p.color.copy(alpha = 1f - t), Offset(p.x, p.y), Offset(p.x - p.vx * 0.04f, p.y - p.vy * 0.04f), p.size * (1f - t * 0.5f), StrokeCap.Round)
        PKind.RING -> {
            val r = p.size * (0.25f + t * 0.75f)
            drawCircle(p.color.copy(alpha = (1f - t) * 0.9f), r, Offset(p.x, p.y), style = Stroke(12f * (1f - t) + 2f))
            drawCircle(Color.White.copy(alpha = (1f - t) * 0.6f), r * 0.92f, Offset(p.x, p.y), style = Stroke(2.5f))
        }
        PKind.GLOW -> glow(p.x, p.y, p.size * (0.6f + t * 0.4f), p.color, (1f - t) * 0.9f)
        PKind.TEXT -> {
            val pop = if (t < 0.15f) 0.6f + t / 0.15f * 0.6f else 1.2f - minOf(0.2f, (t - 0.15f) * 2f)
            sprites.text(this, p.text, p.x, p.y, p.size * pop, p.color, minOf(1f, (1f - t) * 3f))
        }
        PKind.BOLT -> {
            // 每隔一小段時間重新抖一次，看起來像在閃
            val rnd = Random(p.seed + (p.age * 40f).toInt())
            boltPath.reset()
            boltPath.moveTo(p.x, p.y)
            val n = 9
            val dx = p.x2 - p.x
            val dy = p.y2 - p.y
            val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            for (i in 1 until n) {
                val f = i / n.toFloat()
                val j = (rnd.nextFloat() - 0.5f) * len * 0.16f
                boltPath.lineTo(p.x + dx * f - dy / len * j, p.y + dy * f + dx / len * j)
            }
            boltPath.lineTo(p.x2, p.y2)
            val a = 1f - t * t
            drawPath(boltPath, p.color.copy(alpha = 0.45f * a), style = Stroke(p.size * 3.2f, cap = StrokeCap.Round), blendMode = BlendMode.Plus)
            drawPath(boltPath, p.color.copy(alpha = a), style = Stroke(p.size, cap = StrokeCap.Round))
            drawPath(boltPath, Color.White.copy(alpha = a), style = Stroke(p.size * 0.4f, cap = StrokeCap.Round))
        }
        PKind.LINE -> {
            val a = 1f - t
            drawLine(p.color.copy(alpha = 0.4f * a), Offset(p.x, p.y), Offset(p.x2, p.y2), p.size * 3f, StrokeCap.Round, blendMode = BlendMode.Plus)
            drawLine(p.color.copy(alpha = a), Offset(p.x, p.y), Offset(p.x2, p.y2), p.size, StrokeCap.Round)
            drawLine(Color.White.copy(alpha = a), Offset(p.x, p.y), Offset(p.x2, p.y2), p.size * 0.35f, StrokeCap.Round)
        }
        PKind.PILLAR -> {
            val a = (1f - t) * (1f - t)
            val w = p.size * (1f - t * 0.5f)
            val top = p.y - 900f
            drawRect(
                Brush.horizontalGradient(listOf(p.color.copy(alpha = 0f), p.color.copy(alpha = 0.75f * a), Color.White.copy(alpha = 0.9f * a), p.color.copy(alpha = 0.75f * a), p.color.copy(alpha = 0f)), p.x - w / 2f, p.x + w / 2f),
                Offset(p.x - w / 2f, top), Size(w, p.y + 60f - top), blendMode = BlendMode.Plus,
            )
            glow(p.x, p.y + 40f, p.size * 1.2f, p.color, 0.8f * a)
        }
        PKind.SLASH -> {
            val a = 1f - t
            withTransform({ rotate(p.seed.toFloat(), Offset(p.x, p.y)) }) {
                drawArc(p.color.copy(alpha = a), -60f, 120f * (0.4f + t * 0.6f), false, Offset(p.x - p.size, p.y - p.size * 0.5f), Size(p.size * 2f, p.size), style = Stroke(16f * a + 3f, cap = StrokeCap.Round))
                drawArc(Color(0xFFFFE45C).copy(alpha = a * 0.7f), -60f, 120f * (0.4f + t * 0.6f), false, Offset(p.x - p.size * 0.9f, p.y - p.size * 0.4f), Size(p.size * 1.8f, p.size * 0.8f), style = Stroke(6f, cap = StrokeCap.Round))
            }
        }
        PKind.PUFF -> drawCircle(p.color.copy(alpha = 0.55f * (1f - t)), p.size * (0.5f + t * 0.8f), Offset(p.x, p.y))
        PKind.WAVE -> {
            val a = 1f - t * t
            drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.9f * a), p.color.copy(alpha = 0.7f * a), p.color.copy(alpha = 0f)), p.y - 60f, p.y + 260f),
                Offset(p.x - 88f, p.y - 60f), Size(176f, 320f), androidx.compose.ui.geometry.CornerRadius(80f))
            for (k in -1..1) drawCircle(Color.White.copy(alpha = 0.8f * a), 26f, Offset(p.x + k * 56f, p.y - 50f + kotlin.math.abs(k) * 14f))
        }
        PKind.FLAME -> {
            val a = 1f - t
            val s = p.size * (0.6f + t * 0.6f)
            glow(p.x, p.y, s * 1.3f, p.color, 0.6f * a)
            drawOval(p.color.copy(alpha = 0.85f * a), Offset(p.x - s * 0.32f, p.y - s * 0.6f), Size(s * 0.64f, s * 1.1f))
            drawOval(Color.White.copy(alpha = 0.9f * a), Offset(p.x - s * 0.14f, p.y - s * 0.25f), Size(s * 0.28f, s * 0.55f))
        }
        PKind.PLUS -> {
            val a = 1f - t
            drawLine(p.color.copy(alpha = a), Offset(p.x - p.size, p.y), Offset(p.x + p.size, p.y), p.size * 0.6f, StrokeCap.Round)
            drawLine(p.color.copy(alpha = a), Offset(p.x, p.y - p.size), Offset(p.x, p.y + p.size), p.size * 0.6f, StrokeCap.Round)
        }
        PKind.STAR -> {
            val a = 1f - t * t
            val s = p.size * (1f - t * 0.3f)
            val star = Path().apply {
                moveTo(p.x, p.y - s); quadraticTo(p.x + s * 0.18f, p.y - s * 0.18f, p.x + s, p.y)
                quadraticTo(p.x + s * 0.18f, p.y + s * 0.18f, p.x, p.y + s); quadraticTo(p.x - s * 0.18f, p.y + s * 0.18f, p.x - s, p.y)
                quadraticTo(p.x - s * 0.18f, p.y - s * 0.18f, p.x, p.y - s); close()
            }
            drawPath(star, p.color.copy(alpha = a))
            glow(p.x, p.y, s * 1.8f, p.color, 0.4f * a)
        }
        PKind.LANE -> {
            val a = (1f - t) * 0.55f
            drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = a), p.color.copy(alpha = a)), 0f, 1320f), Offset(p.x - 92f, 0f), Size(184f, 1320f), blendMode = BlendMode.Plus)
        }
    }
}

@Suppress("unused")
private val cardTypes = CardType.entries
