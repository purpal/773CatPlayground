package com.purpal.catplayground.shooter.render

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.purpal.catplayground.shooter.model.BulletStyle
import com.purpal.catplayground.shooter.model.CatType
import com.purpal.catplayground.shooter.model.EnemyKind
import com.purpal.catplayground.shooter.model.HazardStyle
import com.purpal.catplayground.shooter.model.ItemType
import com.purpal.catplayground.shooter.model.LostProp
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReferenceArray
import kotlin.math.ceil
import kotlin.math.roundToInt

class Sprite(val image: ImageBitmap, val pxPerUnit: Float)

/**
 * 遊戲用的圖：角色、道具與特效是 tools/art 產生的 PNG（assets/art/），
 * 載入時依螢幕解析度縮放好；子彈與光暈這類簡單的圖則用程式先畫成圖片。
 * 每幀只要貼圖，名稱都事先組好，不會每幀產生字串。
 */
class SpriteCache(private val pxPerUnit: Float, private val assets: AssetManager) {
    private val drawn = AtomicReferenceArray<Sprite?>(512)
    private val art = ConcurrentHashMap<String, Sprite>()

    private fun get(id: Int, width: Float, height: Float, draw: DrawScope.() -> Unit): Sprite {
        drawn.get(id)?.let { return it }
        val sprite = render(width, height, draw)
        drawn.set(id, sprite)
        return sprite
    }

    private fun render(width: Float, height: Float, draw: DrawScope.() -> Unit): Sprite {
        val w = ceil(width * pxPerUnit).toInt().coerceAtLeast(1)
        val h = ceil(height * pxPerUnit).toInt().coerceAtLeast(1)
        val image = ImageBitmap(w, h)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(w.toFloat(), h.toFloat())) {
            withTransform({
                translate(w / 2f, h / 2f)
                scale(pxPerUnit, pxPerUnit, Offset.Zero)
            }) { draw() }
        }
        return Sprite(image, pxPerUnit)
    }

    /** 讀取 PNG 並縮放成畫面上寬 [worldWidth] 的大小（逐次減半縮小，邊緣比較不會鋸齒）。 */
    private fun art(name: String, worldWidth: Float): Sprite = art[name] ?: loadArt(name, worldWidth).also { art[name] = it }

    private fun loadArt(name: String, worldWidth: Float): Sprite {
        val source = assets.open("art/$name.png").use { BitmapFactory.decodeStream(it) }
        val target = (worldWidth * pxPerUnit).roundToInt().coerceAtLeast(1)
        var bitmap: Bitmap = source
        while (bitmap.width / 2 >= target) {
            bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width / 2, (bitmap.height / 2).coerceAtLeast(1), true)
        }
        if (bitmap.width != target) {
            val h = (bitmap.height * target / bitmap.width.toFloat()).roundToInt().coerceAtLeast(1)
            bitmap = Bitmap.createScaledBitmap(bitmap, target, h, true)
        }
        if (!com.purpal.catplayground.DebugFlags.noPrepare) bitmap.prepareToDraw()
        return Sprite(bitmap.asImageBitmap(), bitmap.width / worldWidth)
    }

    // ── 貓咪 ──

    /** pose：0 直走、1 往左、2 往右；frame 0～3 奔跑循環。 */
    fun cat(cat: CatType, pose: Int, frame: Int, beast: Boolean): Sprite =
        art(RUN[(if (beast) 2 else 0) + cat.ordinal][pose][frame and 3], CAT_WIDTH)

    /** 大絕特寫（寫實漫畫風）。 */
    /** 大絕道具：和大絕按鈕同一個圖示。 */
    fun ultButton(cat: CatType): Sprite = art(if (cat == CatType.ORANGE) "ult_btn_orange" else "ult_btn_black", ITEM_WIDTH)

    fun ultimate(cat: CatType): Sprite = art(if (cat == CatType.ORANGE) "ult_tiger" else "ult_panther", ULT_WIDTH)

    // ── 敵人 ──

    fun boss(kind: EnemyKind, mood: Int, frame: Int): Sprite {
        val index = when (kind) {
            EnemyKind.BOSS_DOG -> 1
            EnemyKind.BOSS_BEAR -> 2
            EnemyKind.BOSS_CAPY -> 3
            EnemyKind.BOSS_RABBIT -> 4
            else -> 0
        }
        return art(BOSS[index][mood.coerceIn(0, 2)][frame and 1], BOSS_WIDTH)
    }

    fun enemy(kind: EnemyKind, frame: Int): Sprite {
        val f = frame and 1
        return when (kind) {
            EnemyKind.MOUSE_RUNNER, EnemyKind.MINI_MOUSE -> art(MOUSE[f], 105f)
            EnemyKind.MOUSE_PLANE -> art(PLANE[f], 150f)
            EnemyKind.CHEESE_DRONE -> art("min_cheese_pilot", 70f)
            EnemyKind.PUPPY -> art(PUPPY[f], 115f)
            EnemyKind.FRISBEE -> art("min_frisbee", 104f)
            EnemyKind.BALL_TURRET -> art("min_turret", 120f)
            EnemyKind.BEE -> art(BEE[f], 74f)
            EnemyKind.HONEY_POT -> art(HONEYPOT[f], 115f)
            EnemyKind.BEAR_CUB -> art(BEARCUB[f], 125f)
            EnemyKind.BEEHIVE -> art("min_beehive", 90f)
            EnemyKind.BALLOON -> art("min_balloon", 100f)
            EnemyKind.ELITE_MOUSE -> art(ELITE_MOUSE[f], ELITE_WIDTH)
            EnemyKind.ELITE_DOG -> art(ELITE_DOG[f], ELITE_WIDTH)
            EnemyKind.ELITE_BEE -> art(ELITE_BEE[f], ELITE_WIDTH)
            EnemyKind.ELITE_MONKEY -> art(ELITE_MONKEY[f], ELITE_WIDTH)
            EnemyKind.DUCKLING -> art(DUCK[f], 76f)
            EnemyKind.YUZU -> art("min_yuzu", 84f)
            EnemyKind.BUCKET -> art("min_bucket", 96f)
            EnemyKind.BABY_CAPY -> art(BABY_CAPY[f], 118f)
            EnemyKind.BUNNY -> art(BUNNY[f], 92f)
            EnemyKind.CARROT_TURRET -> art("min_carrot", 112f)
            EnemyKind.ELITE_HARE -> art(ELITE_HARE[f], ELITE_WIDTH)
            EnemyKind.THROWN_BONE -> art("lost_bone", 200f)
            EnemyKind.BOSS_MOUSE, EnemyKind.BOSS_DOG, EnemyKind.BOSS_BEAR, EnemyKind.BOSS_CAPY, EnemyKind.BOSS_RABBIT -> boss(kind, 0, f)
        }
    }

    fun cheeseWheel() = art("min_cheese_wheel", 120f)
    fun barrel() = art("min_barrel", 140f)

    /** 科技道具的頭（電漿叉頭、硬光骨頭的一端、熱熔鑽頭、橘子砲），朝 +x，寬度即世界長度。 */
    fun propHead(style: HazardStyle): Sprite? = when (style) {
        HazardStyle.FORK -> art("prop_fork_head", 140f)
        HazardStyle.BONE -> art("prop_bone_end", 75f)
        HazardStyle.DIPPER -> art("prop_dipper_head", 125f)
        HazardStyle.WATER -> art("prop_yuzu_head", 135f)
        HazardStyle.CARROT -> art("prop_carrot_head", 135f)
        else -> null
    }

    fun lostProp(id: Int): Sprite = when (id) {
        LostProp.CROWN -> art("lost_crown", 100f)
        LostProp.RIBBON -> art("lost_ribbon", 110f)
        LostProp.HAT -> art("lost_hat", 170f)
        LostProp.POT -> art("lost_pot", 170f)
        LostProp.BONE -> art("lost_bone", 200f)
        LostProp.DIPPER -> art("lost_dipper", 200f)
        LostProp.YUZU -> art("lost_yuzu", 70f)
        else -> art("lost_fork", 200f)
    }

    // ── 道具 ──

    fun item(type: ItemType): Sprite = when (type) {
        ItemType.COIN -> art("item_coin", 75f)
        ItemType.DATA -> art("item_data", 34f)
        else -> art(ITEMS[type.ordinal], ITEM_WIDTH)
    }

    // ── 武器與特效（v3 科技風，每種武器各留一個貓咪萌點） ──

    fun plasma() = art("shot_plasma", 24f)
    fun casing() = art("fx_casing", 24f)
    fun catHolo() = art("fx_catholo", 110f)
    fun graviton() = art("shot_graviton", 120f)
    fun gravitonPaw() = art("shot_graviton_paw", 50f)
    fun fishMissile() = art("shot_fish2", 96f)
    fun reticle() = art("fx_reticle", 110f)
    fun hexPaw() = art("fx_hexpaw", 52f)
    fun hitMark() = art("fx_hitmark", 34f)
    fun pawStamp() = art("fx_pawstamp", 260f)
    fun star() = art("fx_star", 52f)
    fun heart() = art("fx_heart", 52f)
    fun sfx(id: Int) = art(SFX[id.coerceIn(0, SFX.size - 1)], SFX_WIDTH[id.coerceIn(0, SFX.size - 1)])
    fun comboLabel() = art("sfx_chain", 130f)
    fun roar() = art("sfx_roar", 420f)
    fun overdrive() = art("sfx_overdrive", 560f)
    fun digit(d: Int) = art(DIGITS[d], DIGIT_WIDTH)
    fun plus() = art("fx_plus", DIGIT_WIDTH)
    fun times() = art("fx_x", DIGIT_WIDTH)

    /** 敵人子彈：統一的糖果粉色系（深梅色描邊＋白色核心），在任何背景上都一眼看得出來。 */
    fun bullet(style: BulletStyle, radius: Float): Sprite {
        val reference = bulletReference(radius)
        val id = style.ordinal * 2 + if (reference > 11f) 1 else 0
        val r = reference * 1.3f
        return get(id, r * 4f, r * 4f) { drawBulletArt(style, r) }
    }

    /** 爆炸時的能量閃光團（白→青）。 */
    fun puff() = get(100, 100f, 100f) {
        drawCircle(
            Brush.radialGradient(
                0f to Color.White, 0.35f to Color(0xFFBFF8FF), 0.65f to Color(0xFF00E5FF).copy(alpha = 0.55f), 1f to Color.Transparent,
                radius = 50f, center = Offset.Zero,
            ),
            50f,
            Offset.Zero,
        )
    }

    fun smoke() = get(101, 100f, 100f) {
        drawCircle(Brush.radialGradient(listOf(Color(0xCCFFFFFF), Color(0x00F3E8FF)), Offset.Zero, 50f), 50f, Offset.Zero)
    }

    fun glow(color: Long) = get(110 + glowIndex(color), 100f, 100f) {
        drawCircle(Brush.radialGradient(listOf(Color.White, Color(color), Color(color).copy(alpha = 0f)), Offset.Zero, 50f), 50f, Offset.Zero)
    }

    /** 槍口：V 形噴焰＋青色光暈。 */
    fun muzzle() = get(102, 90f, 90f) {
        drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFF80F0FF), Color(0x0000E5FF)), Offset.Zero, 45f), 45f, Offset.Zero)
        val v = androidx.compose.ui.graphics.Path().apply {
            moveTo(-16f, 14f); lineTo(0f, -26f); lineTo(16f, 14f); lineTo(0f, 2f); close()
        }
        drawPath(v, Color.White)
    }

    /** 先在背景執行緒把這一關會用到的圖都載入好，避免開打時卡頓。 */
    fun prewarm(cat: CatType, kinds: Collection<EnemyKind>) {
        for (beast in listOf(false, true)) for (pose in 0..2) for (f in 0..3) cat(cat, pose, f, beast)
        for (kind in kinds) {
            for (f in 0..1) {
                if (kind == EnemyKind.BOSS_MOUSE || kind == EnemyKind.BOSS_DOG || kind == EnemyKind.BOSS_BEAR || kind == EnemyKind.BOSS_CAPY || kind == EnemyKind.BOSS_RABBIT) {
                    for (mood in 0..2) boss(kind, mood, f)
                } else {
                    enemy(kind, f)
                }
            }
        }
        cheeseWheel(); barrel()
        HazardStyle.entries.forEach { propHead(it) }
        listOf(LostProp.CROWN, LostProp.HAT, LostProp.POT, LostProp.FORK, LostProp.BONE, LostProp.DIPPER, LostProp.YUZU, LostProp.RIBBON).forEach { lostProp(it) }
        ItemType.entries.forEach { item(it) }
        plasma(); casing(); catHolo(); graviton(); gravitonPaw(); fishMissile(); reticle(); hexPaw(); hitMark(); pawStamp()
        star(); heart(); comboLabel(); roar(); overdrive()
        SFX.indices.forEach { sfx(it) }
        for (d in 0..9) digit(d)
        plus(); times()
        ultimate(cat)
        ultButton(cat)
        puff(); smoke(); muzzle()
        GLOWS.forEach { glow(it) }
        for (style in BulletStyle.entries) {
            bullet(style, 11f)
            bullet(style, 22f)
        }
    }

    companion object {
        /** 子彈圖以這個半徑畫，畫面上再依實際半徑縮放。 */
        fun bulletReference(radius: Float) = if (radius > 16f) 22f else 11f

        const val CAT_WIDTH = 130f
        const val BOSS_WIDTH = 540f
        const val ELITE_WIDTH = 175f
        const val ITEM_WIDTH = 123f
        const val ULT_WIDTH = 760f
        const val DIGIT_WIDTH = 30f

        val GLOWS = longArrayOf(
            0xFFFFFFFFL, 0xFF80F0FFL, 0xFFFFC107L, 0xFFB388FFL, 0xFFFF8FB1L, 0xFFFFE07AL, 0xFFC9B8FFL,
            0xFF00E5FFL, 0xFFFF7A45L, 0xFFFF2BD6L, 0xFFFF3D5AL, 0xFFFFB300L,
        )
        private fun glowIndex(color: Long): Int {
            for (i in GLOWS.indices) if (GLOWS[i] == color) return i
            return GLOWS.size
        }

        private val CAT_KINDS = listOf("orange", "black", "tiger", "panther")
        private val POSES = listOf("c", "l", "r")
        private val RUN = Array(4) { k -> Array(3) { p -> Array(4) { f -> "run_${CAT_KINDS[k]}_${POSES[p]}_$f" } } }
        private val BOSS = Array(5) { b -> Array(3) { m -> Array(2) { f -> "boss_${listOf("mouse", "dog", "bear", "capy", "rabbit")[b]}_m${m}_$f" } } }
        private fun frames(base: String) = Array(2) { "${base}_$it" }
        private val MOUSE = frames("min_mouse")
        private val PLANE = frames("min_plane")
        private val PUPPY = frames("min_puppy")
        private val BEE = frames("min_bee")
        private val HONEYPOT = frames("min_honeypot")
        private val BEARCUB = frames("min_bearcub")
        private val ELITE_MOUSE = frames("elite_mouse")
        private val ELITE_DOG = frames("elite_dog")
        private val ELITE_BEE = frames("elite_bee")
        private val ELITE_MONKEY = frames("elite_monkey")
        private val DUCK = frames("min_duck")
        private val BABY_CAPY = frames("min_babycapy")
        private val BUNNY = frames("min_bunny")
        private val ELITE_HARE = frames("elite_hare")
        private val ITEMS = ItemType.entries.map { "item_${it.name.lowercase()}" }.toTypedArray()
        private val SFX = arrayOf("sfx_hit", "sfx_term", "sfx_crit", "sfx_data", "sfx_elite", "sfx_break", "sfx_armor", "sfx_chain")
        private val SFX_WIDTH = floatArrayOf(110f, 260f, 240f, 170f, 300f, 330f, 380f, 220f)
        private val DIGITS = Array(10) { "fx_d$it" }
    }
}

private val BulletInk = Color(0xFF3A0A1E)
private val WarnRed = Color(0xFFFF3D5A)
private val WarnMagenta = Color(0xFFFF2BD6)

/**
 * 子彈圖，中心 (0,0)，r 為視覺半徑。敵彈一律暖色（警示紅／洋紅）＋白核心＋深色硬邊，
 * 和玩家的冷色火力永遠分得開；形狀依種類保留動物特色。
 */
private fun DrawScope.drawBulletArt(style: BulletStyle, r: Float) {
    val color = when (style) {
        BulletStyle.RICE, BulletStyle.SEED, BulletStyle.ORB -> WarnRed
        else -> WarnMagenta
    }
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.5f), color.copy(alpha = 0f)), Offset.Zero, r * 2f), r * 2f, Offset.Zero)
    when (style) {
        BulletStyle.ORB, BulletStyle.BIG_ORB, BulletStyle.SEED -> {
            drawCircle(BulletInk, r + 2.5f, Offset.Zero)
            drawCircle(color, r, Offset.Zero)
            drawCircle(Color.White, r * 0.5f, Offset(-r * 0.15f, -r * 0.15f))
        }
        BulletStyle.RICE -> {
            drawOval(BulletInk, Offset(-r * 1.4f - 2.5f, -r * 0.8f - 2.5f), Size(r * 2.8f + 5f, r * 1.6f + 5f))
            drawOval(color, Offset(-r * 1.4f, -r * 0.8f), Size(r * 2.8f, r * 1.6f))
            drawOval(Color.White, Offset(-r * 0.9f, -r * 0.35f), Size(r * 1.8f, r * 0.7f))
        }
        BulletStyle.CHEESE -> {
            shape(Shapes.wedge, 0f, 0f, r * 1.25f + 3f, BulletInk)
            shape(Shapes.wedge, 0f, 0f, r * 1.25f, color)
            drawCircle(Color.White, r * 0.3f, Offset(-r * 0.2f, -r * 0.1f))
        }
        BulletStyle.BONE -> {
            for (pass in 0..1) {
                val c = if (pass == 0) BulletInk else color
                val g = if (pass == 0) 2.5f else 0f
                drawRoundRect(c, Offset(-r - g, -r * 0.32f - g), Size(r * 2f + g * 2, r * 0.64f + g * 2), CornerRadius(r * 0.3f))
                for (sx in listOf(-1f, 1f)) for (sy in listOf(-1f, 1f)) drawCircle(c, r * 0.4f + g, Offset(sx * r, sy * r * 0.34f))
            }
            drawLine(Color.White, Offset(-r * 0.7f, 0f), Offset(r * 0.7f, 0f), r * 0.22f, StrokeCap.Round)
        }
        BulletStyle.NOTE -> {
            drawCircle(BulletInk, r * 0.75f + 2.5f, Offset(-r * 0.2f, r * 0.35f))
            drawLine(BulletInk, Offset(r * 0.4f, r * 0.35f), Offset(r * 0.4f, -r), r * 0.35f + 5f, StrokeCap.Round)
            drawLine(BulletInk, Offset(r * 0.4f, -r), Offset(r, -r * 0.55f), r * 0.35f + 5f, StrokeCap.Round)
            drawCircle(color, r * 0.75f, Offset(-r * 0.2f, r * 0.35f))
            drawLine(color, Offset(r * 0.4f, r * 0.35f), Offset(r * 0.4f, -r), r * 0.35f, StrokeCap.Round)
            drawLine(color, Offset(r * 0.4f, -r), Offset(r, -r * 0.55f), r * 0.35f, StrokeCap.Round)
            drawCircle(Color.White, r * 0.28f, Offset(-r * 0.35f, r * 0.2f))
        }
        BulletStyle.HONEY -> {
            shape(Shapes.drop, 0f, 0f, r + 2.5f, BulletInk)
            shape(Shapes.drop, 0f, 0f, r, color)
            drawCircle(Color.White, r * 0.3f, Offset(-r * 0.25f, 0f))
        }
        BulletStyle.BUBBLE -> {
            drawCircle(BulletInk, r + 2.5f, Offset.Zero)
            drawCircle(color, r, Offset.Zero)
            drawCircle(color.copy(alpha = 0.35f).compositeOver(Color.White), r * 0.72f, Offset.Zero)
            drawCircle(Color.White, r * 0.28f, Offset(-r * 0.32f, -r * 0.32f))
        }
        BulletStyle.STAR -> {
            shape(Shapes.star, 0f, 0f, r * 1.3f + 3.5f, BulletInk)
            shape(Shapes.star, 0f, 0f, r * 1.3f, color)
            drawCircle(Color.White, r * 0.38f, Offset.Zero)
        }
    }
}

/** 以世界座標畫一張精靈圖（精靈圖本身已是螢幕解析度）。stretch > 1 時橫向拉寬、縱向壓扁（命中時的果凍感）。 */
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
    stretch: Float = 1f,
) {
    withTransform({
        translate(x, y)
        if (rotationDegrees != 0f) rotate(rotationDegrees, Offset.Zero)
        val k = scale / sprite.pxPerUnit
        scale(if (flipX) -k * stretch else k * stretch, k / stretch, Offset.Zero)
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
