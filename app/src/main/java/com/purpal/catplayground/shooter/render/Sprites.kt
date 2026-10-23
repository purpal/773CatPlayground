package com.purpal.catplayground.shooter.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.purpal.catplayground.shooter.model.BulletStyle
import com.purpal.catplayground.shooter.model.CatType
import com.purpal.catplayground.shooter.model.EnemyKind
import com.purpal.catplayground.shooter.model.ItemType
import com.purpal.catplayground.shooter.model.Tint
import java.util.concurrent.atomic.AtomicReferenceArray
import kotlin.math.ceil

class Sprite(val image: ImageBitmap, val pxPerUnit: Float)

/**
 * 把角色、子彈、特效先畫成圖片（以實際螢幕解析度），每幀只要貼圖，
 * 這樣卡通描邊、漸層等比較費工的畫法也不會拖慢遊戲。
 * 以整數 id 存取，避免每幀產生字串或物件。
 */
class SpriteCache(private val pxPerUnit: Float) {
    private val sprites = AtomicReferenceArray<Sprite?>(4096)

    private fun get(id: Int, width: Float, height: Float, scale: Float = pxPerUnit, draw: DrawScope.() -> Unit): Sprite {
        sprites.get(id)?.let { return it }
        val sprite = render(width, height, scale, draw)
        sprites.set(id, sprite)
        return sprite
    }

    private fun render(width: Float, height: Float, scale: Float, draw: DrawScope.() -> Unit): Sprite {
        val w = ceil(width * scale).toInt().coerceAtLeast(1)
        val h = ceil(height * scale).toInt().coerceAtLeast(1)
        val image = ImageBitmap(w, h)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(w.toFloat(), h.toFloat())) {
            withTransform({
                translate(w / 2f, h / 2f)
                scale(scale, scale, Offset.Zero)
            }) { draw() }
        }
        return Sprite(image, scale)
    }

    fun cat(cat: CatType, frame: Int, beast: Boolean): Sprite {
        val id = cat.ordinal * 8 + (if (beast) 4 else 0) + frame % 4
        return get(id, 150f, 170f) { drawCatSprite(cat, frame % 4, beast) }
    }

    /** 大絕動畫用的高解析度大頭。 */
    fun bigCat(cat: CatType, frame: Int): Sprite =
        get(20 + cat.ordinal * 2 + frame % 2, 150f, 170f, scale = pxPerUnit * 4f) { drawCatSprite(cat, frame % 2, beast = true) }

    fun enemy(kind: EnemyKind, frame: Int, phase: Int, item: ItemType? = null): Sprite {
        val id = 100 + kind.ordinal * 64 + phase * 16 + (frame % 2) * 8 + (item?.ordinal ?: 0)
        val (w, h) = enemyBox(kind)
        return get(id, w, h) { drawEnemyArt(kind, frame % 2, phase, item) }
    }

    fun cheeseWheel() = get(1100, 120f, 120f) { cheeseWheel() }
    fun frisbee() = get(1101, 104f, 104f) { frisbee() }
    fun barrel() = get(1102, 140f, 34f) { turretBarrel() }
    fun beehive() = get(1103, 90f, 90f) { beehive() }

    fun item(type: ItemType) = get(1110 + type.ordinal, 76f, 76f) { drawItemBadge(type, 0f) }

    /** 敵人子彈：深色描邊＋鮮豔本體＋白色核心＋外圈光暈，在任何背景上都清楚。 */
    fun bullet(style: BulletStyle, color: Long, radius: Float): Sprite {
        val colorIndex = bulletColors.indexOf(color).let { if (it < 0) bulletColors.size else it }
        val reference = bulletReference(radius)
        val id = 1200 + style.ordinal * 40 + colorIndex * 2 + if (reference > 11f) 1 else 0
        val r = reference * 1.3f
        return get(id, r * 4f, r * 4f) { drawBulletArt(style, Color(color), r) }
    }

    // ── 特效 ──
    fun fire() = get(2000, 100f, 100f) {
        drawCircle(
            Brush.radialGradient(
                0f to Color.White, 0.25f to Color(0xFFFFF59D), 0.5f to Color(0xFFFFA726),
                0.75f to Color(0xFFE64A19).copy(alpha = 0.6f), 1f to Color.Transparent,
                radius = 50f, center = Offset.Zero,
            ),
            50f,
            Offset.Zero,
        )
    }

    fun smoke() = get(2001, 100f, 100f) {
        drawCircle(Brush.radialGradient(listOf(Color(0xCC9E94A8), Color(0x00807088)), Offset.Zero, 50f), 50f, Offset.Zero)
    }

    fun glow(color: Long) = get(2010 + (bulletColors.indexOf(color).takeIf { it >= 0 } ?: 15), 100f, 100f) {
        drawCircle(Brush.radialGradient(listOf(Color.White, Color(color), Color(color).copy(alpha = 0f)), Offset.Zero, 50f), 50f, Offset.Zero)
    }

    fun pellet() = get(2030, 34f, 70f) {
        // 玩家子彈用藍色系，和敵人的暖色子彈一眼就分得出來。
        drawOval(Brush.radialGradient(listOf(Color(0xFF2979FF), Color(0x002962FF)), Offset.Zero, 34f), Offset(-17f, -35f), Size(34f, 70f))
        drawOval(Color(0xFF2979FF), Offset(-8f, -26f), Size(16f, 52f))
        drawOval(Color(0xFF80D8FF), Offset(-5.5f, -22f), Size(11f, 44f))
        drawOval(Color(0xFFFFFDE7), Offset(-3.5f, -19f), Size(7f, 34f))
    }

    fun paw() = get(2031, 110f, 110f) {
        drawCircle(Brush.radialGradient(listOf(Color(0xCCFFB300), Color(0x00FF6D00)), Offset.Zero, 55f), 55f, Offset.Zero)
        paw(0f, 4f, 30f, Color(0xFFFF8F00))
        paw(0f, 4f, 24f, Color(0xFFFFE082))
        paw(0f, 4f, 15f, Color(0xFFFFFDE7))
    }

    fun claw() = get(2032, 50f, 64f) {
        for (k in -1..1) {
            val x = k * 11f
            drawArc(Color(0x99AA00FF), 200f, 140f, false, Offset(x - 16f, -24f), Size(32f, 60f), style = Stroke(11f, cap = StrokeCap.Round))
            drawArc(Color(0xFFE040FB), 200f, 140f, false, Offset(x - 16f, -24f), Size(32f, 60f), style = Stroke(6f, cap = StrokeCap.Round))
            drawArc(Color(0xFFFFF0FF), 200f, 140f, false, Offset(x - 16f, -24f), Size(32f, 60f), style = Stroke(2.5f, cap = StrokeCap.Round))
        }
    }

    fun missile() = get(2033, 50f, 30f) {
        blob(ovalPath(-2f, 0f, 30f, 18f), Color(0xFF00E676), Color(0xFF00A152), width = 2.2f)
        drawPath(polyPath(10f, 0f, 22f, -10f, 22f, 10f), Color(0xFF00C853))
        drawPath(polyPath(10f, 0f, 22f, -10f, 22f, 10f), Toon.Outline, style = Stroke(2.2f))
        drawCircle(Toon.Outline, 2.5f, Offset(-10f, -3f))
    }

    fun muzzle() = get(2034, 90f, 90f) {
        drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFFFE082), Color(0x00FF9100)), Offset.Zero, 45f), 45f, Offset.Zero)
        shape(Shapes.star, 0f, 0f, 40f, Color(0xAAFFFFFF))
    }

    /** 先在背景執行緒把這一關會用到的圖都畫好，避免開打時卡頓。 */
    fun prewarm(cat: CatType, kinds: Collection<EnemyKind>) {
        for (f in 0 until 4) {
            cat(cat, f, beast = false)
            cat(cat, f, beast = true)
        }
        bigCat(cat, 0)
        bigCat(cat, 1)
        for (kind in kinds) for (f in 0..1) for (phase in 0..2) {
            if (phase > 0 && kind !in BOSSES) continue
            if (kind == EnemyKind.BALLOON) ItemType.entries.forEach { enemy(kind, f, 0, it) } else enemy(kind, f, phase)
        }
        cheeseWheel(); frisbee(); barrel(); beehive()
        ItemType.entries.forEach { item(it) }
        fire(); smoke(); pellet(); paw(); claw(); missile(); muzzle()
        for (color in bulletColors) glow(color)
        for (style in BulletStyle.entries) for (color in bulletColors) bullet(style, color, 11f)
    }

    companion object {
        /** 子彈圖以這個半徑畫，畫面上再依實際半徑縮放。 */
        fun bulletReference(radius: Float) = if (radius > 16f) 22f else 11f

        val bulletColors = longArrayOf(
            Tint.CHEESE, Tint.SEED, Tint.PINK, Tint.BONE, Tint.NOTE, Tint.TENNIS, Tint.HONEY,
            Tint.STAR, Tint.RED, Tint.CYAN, Tint.ORANGE, Tint.PURPLE, Tint.GREEN,
            0xFFFFE082L, 0xFF80F0FFL, 0xFFFFFFFFL,
        )
        private val BOSSES = setOf(EnemyKind.BOSS_MOUSE, EnemyKind.BOSS_DOG, EnemyKind.BOSS_BEAR)

        private fun enemyBox(kind: EnemyKind): Pair<Float, Float> = when (kind) {
            EnemyKind.MOUSE_RUNNER, EnemyKind.MINI_MOUSE -> 110f to 150f
            EnemyKind.MOUSE_PLANE -> 150f to 130f
            EnemyKind.CHEESE_DRONE -> 90f to 70f
            EnemyKind.PUPPY -> 130f to 120f
            EnemyKind.FRISBEE -> 104f to 104f
            EnemyKind.BALL_TURRET -> 110f to 90f
            EnemyKind.BEE -> 80f to 96f
            EnemyKind.HONEY_POT -> 110f to 110f
            EnemyKind.BEAR_CUB -> 140f to 130f
            EnemyKind.BEEHIVE -> 90f to 90f
            EnemyKind.BALLOON -> 100f to 220f
            EnemyKind.BOSS_MOUSE -> 360f to 330f
            EnemyKind.BOSS_DOG -> 270f to 300f
            EnemyKind.BOSS_BEAR -> 300f to 330f
        }
    }
}

private fun LongArray.indexOf(value: Long): Int {
    for (i in indices) if (this[i] == value) return i
    return -1
}

private val BulletInk = Color(0xFF1A0F24)

/** 子彈圖，中心 (0,0)，r 為視覺半徑。 */
private fun DrawScope.drawBulletArt(style: BulletStyle, color: Color, r: Float) {
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0f)), Offset.Zero, r * 2f), r * 2f, Offset.Zero)
    when (style) {
        BulletStyle.ORB, BulletStyle.BIG_ORB, BulletStyle.SEED -> {
            drawCircle(BulletInk, r + 2.5f, Offset.Zero)
            drawCircle(color, r, Offset.Zero)
            drawCircle(Brush.radialGradient(listOf(Color.White, Color.White, color.light(0.4f)), Offset(-r * 0.15f, -r * 0.15f), r * 0.6f), r * 0.55f, Offset(-r * 0.1f, -r * 0.1f))
        }
        BulletStyle.RICE -> {
            drawOval(BulletInk, Offset(-r * 1.4f - 2.5f, -r * 0.8f - 2.5f), Size(r * 2.8f + 5f, r * 1.6f + 5f))
            drawOval(color, Offset(-r * 1.4f, -r * 0.8f), Size(r * 2.8f, r * 1.6f))
            drawOval(Color.White, Offset(-r * 0.9f, -r * 0.35f), Size(r * 1.8f, r * 0.7f))
        }
        BulletStyle.CHEESE -> {
            shape(Shapes.wedge, 0f, 0f, r * 1.25f + 3f, BulletInk)
            shape(Shapes.wedge, 0f, 0f, r * 1.25f, color)
            drawCircle(color.shade(0.25f), r * 0.28f, Offset(-r * 0.2f, r * 0.2f))
            drawCircle(Color.White, r * 0.2f, Offset(-r * 0.3f, -r * 0.25f))
        }
        BulletStyle.BONE -> {
            for (pass in 0..1) {
                val c = if (pass == 0) BulletInk else color
                val g = if (pass == 0) 2.5f else 0f
                drawRoundRect(c, Offset(-r - g, -r * 0.32f - g), Size(r * 2f + g * 2, r * 0.64f + g * 2), androidx.compose.ui.geometry.CornerRadius(r * 0.3f))
                for (sx in listOf(-1f, 1f)) for (sy in listOf(-1f, 1f)) drawCircle(c, r * 0.4f + g, Offset(sx * r, sy * r * 0.34f))
            }
        }
        BulletStyle.NOTE -> {
            drawCircle(BulletInk, r * 0.75f + 2.5f, Offset(-r * 0.2f, r * 0.35f))
            drawLine(BulletInk, Offset(r * 0.4f, r * 0.35f), Offset(r * 0.4f, -r), r * 0.35f + 5f, StrokeCap.Round)
            drawLine(BulletInk, Offset(r * 0.4f, -r), Offset(r, -r * 0.55f), r * 0.35f + 5f, StrokeCap.Round)
            drawCircle(color, r * 0.75f, Offset(-r * 0.2f, r * 0.35f))
            drawLine(color, Offset(r * 0.4f, r * 0.35f), Offset(r * 0.4f, -r), r * 0.35f, StrokeCap.Round)
            drawLine(color, Offset(r * 0.4f, -r), Offset(r, -r * 0.55f), r * 0.35f, StrokeCap.Round)
            drawCircle(Color.White, r * 0.25f, Offset(-r * 0.35f, r * 0.2f))
        }
        BulletStyle.HONEY -> {
            shape(Shapes.drop, 0f, 0f, r + 2.5f, BulletInk)
            shape(Shapes.drop, 0f, 0f, r, color)
            drawCircle(Color.White, r * 0.25f, Offset(-r * 0.3f, 0f))
        }
        BulletStyle.STAR -> {
            shape(Shapes.star, 0f, 0f, r * 1.3f + 3.5f, BulletInk)
            shape(Shapes.star, 0f, 0f, r * 1.3f, color)
            drawCircle(Color.White, r * 0.35f, Offset.Zero)
        }
    }
}

/** 以世界座標畫一張精靈圖（精靈圖本身已是螢幕解析度）。 */
internal fun DrawScope.drawSprite(
    sprite: Sprite,
    x: Float,
    y: Float,
    scale: Float = 1f,
    rotationDegrees: Float = 0f,
    alpha: Float = 1f,
    flipX: Boolean = false,
    blendMode: BlendMode = BlendMode.SrcOver,
    colorFilter: ColorFilter? = null,
) {
    withTransform({
        translate(x, y)
        if (rotationDegrees != 0f) rotate(rotationDegrees, Offset.Zero)
        val k = scale / sprite.pxPerUnit
        scale(if (flipX) -k else k, k, Offset.Zero)
    }) {
        drawImage(
            sprite.image,
            topLeft = Offset(-sprite.image.width / 2f, -sprite.image.height / 2f),
            alpha = alpha.coerceIn(0f, 1f),
            colorFilter = colorFilter,
            blendMode = blendMode,
        )
    }
}
