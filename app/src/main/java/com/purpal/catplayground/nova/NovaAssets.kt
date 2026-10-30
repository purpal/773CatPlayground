package com.purpal.catplayground.nova

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.purpal.catplayground.shooter.model.CatType
import com.purpal.catplayground.shooter.render.Sprite
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/** Only the third game uses this loader. Original art files are never replaced. */
class NovaAssets private constructor(private val assets: AssetManager) {
    private data class Region(val file: String, val x: Int, val y: Int, val w: Int, val h: Int)
    private val regions = HashMap<String, Region>()
    private val anchors = HashMap<String, Pair<Float,Float>>()
    private val turnSheets = HashMap<String, Bitmap>()
    private val images = ConcurrentHashMap<String, ImageBitmap>()
    private val sprites = ConcurrentHashMap<String, Sprite>()
    init {
        for (manifest in listOf("manifest.json", "boss-actions/manifest.json", "sectors/manifest.json", "frontiers/manifest.json", "harbor/manifest.json", "forest/manifest.json")) {
            val root = JSONObject(assets.open("nova/$manifest").bufferedReader().use { it.readText() })
            val list = root.getJSONArray("atlases")
            for (i in 0 until list.length()) {
                val sheet = list.getJSONObject(i)
                val file = manifest.substringBeforeLast('/', "").let { if(it.isEmpty()) "" else "$it/" } + sheet.getString("file")
                val frames = sheet.getJSONArray("frames")
                for (j in 0 until frames.length()) {
                    val f = frames.getJSONObject(j)
                    val r = f.optJSONObject("source_rect_px") ?: f.getJSONObject("source_window_px")
                    regions[f.getString("id")] = Region(file, r.getInt("x"), r.getInt("y"), r.getInt("width"), r.getInt("height"))
                }
            }
        }
        val turns = JSONObject(assets.open("nova/turntable.json").bufferedReader().use { it.readText() }).getJSONArray("sheets")
        for (i in 0 until turns.length()) {
            val sheet = turns.getJSONObject(i)
            val frames = sheet.getJSONArray("frames")
            for (j in 0 until frames.length()) {
                val frame=frames.getJSONObject(j)
                val r = frame.getJSONObject("rect_px")
                val a=frame.getJSONArray("preview_anchor_px")
                anchors["turn_${sheet.getString("name")}_$j"] = a.getDouble(0).toFloat() to a.getDouble(1).toFloat()
                regions["turn_${sheet.getString("name")}_$j"] = Region(frame.optString("file", sheet.getString("file")), r.getInt("x"), r.getInt("y"), r.getInt("width"), r.getInt("height"))
            }
        }
    }
    fun anchor(id: String) = anchors[id]
    fun contains(id: String) = id in regions
    @Synchronized fun image(id: String): ImageBitmap {
        images[id]?.let { return it }
        val region = regions[id]
        val file = region?.file ?: when (id) {
            "ship_orange", "ship_black" -> "approved-top-view.png"
            "pilot_orange", "pilot_black" -> "pilots.png"
            "background" -> "sakura-park-background.png"
            "background_bear" -> "sectors/bear-background.png"
            "background_capy" -> "sectors/capy-background.png"
            "background_rabbit" -> "frontiers/rabbit-background.png"
            "background_penguin" -> "frontiers/penguin-background.png"
            "background_koala" -> "harbor/koala-background.png"
            "background_otter" -> "harbor/otter-background.png"
            "background_panda" -> "forest/panda-background.png"
            "background_sloth" -> "forest/sloth-background.png"
            "carrier" -> "supply-carrier.png"
            "boss" -> "boss-reference.png"
            "pilot_dog" -> "pilot-dog.png"
            else -> error("Unknown Nova asset: $id")
        }
        fun decode() = assets.open("nova/$file").use { BitmapFactory.decodeStream(it) } ?: error("Invalid image $file")
        val raw = if(id.startsWith("turn_")) turnSheets.getOrPut(file) { decode() } else decode()
        val section = when {
            region != null -> Bitmap.createBitmap(raw, region.x, region.y, region.w, region.h)
            id.startsWith("ship_") -> {
                val window = NovaShipRegion.window(id == "ship_orange", raw.width)
                Bitmap.createBitmap(raw, window.x, 0, window.width, raw.height)
            }
            id == "pilot_orange" || id == "pilot_black" -> {
                val left = id.endsWith("orange")
                Bitmap.createBitmap(raw, if (left) 0 else raw.width / 2, 0, raw.width / 2, raw.height)
            }
            else -> raw
        }
        val fixedBody = id in listOf("PANDA_BASE", "PANDA_TRANSITION", "PANDA_DEPLOYED", "SLOTH_BASE", "SLOTH_TRANSITION", "SLOTH_DEPLOYED", "KOALA_BASE", "KOALA_TRANSITION", "KOALA_DEPLOYED", "OTTER_BASE", "OTTER_TRANSITION", "OTTER_DEPLOYED", "OTTER_PORT", "OTTER_STARBOARD") || id.startsWith("BEAR_") || (id.startsWith("CAPY_") && id != "CAPY_BUOY_DROP") ||
            id in listOf("RABBIT_BASE", "RABBIT_TRANSITION", "RABBIT_DEPLOYED", "PENGUIN_BASE", "PENGUIN_TRANSITION", "PENGUIN_DEPLOYED")
        val bitmap = if (id.startsWith("background") || id.startsWith("turn_") || fixedBody) section else trim(section)
        val limit = if (id.startsWith("background")) 1024 else 640
        val scaled = if (bitmap.width > limit) Bitmap.createScaledBitmap(bitmap, limit, (bitmap.height.toLong() * limit / bitmap.width).toInt().coerceAtLeast(1), true) else bitmap
        if (id.startsWith("background")) {
            // Blend matching edge rows once to avoid a hard scrolling wrap.
            val editable = scaled.copy(Bitmap.Config.ARGB_8888, true)
            val n = minOf(80, editable.height / 8)
            for (y in 0 until n) for (x in 0 until editable.width) {
                val a = scaled.getPixel(x, y)
                val b = scaled.getPixel(x, scaled.height - 1 - y)
                val t = 0.5f * (1f - y / n.toFloat())
                fun channel(v: Int, u: Int) = (v * (1 - t) + u * t).toInt()
                fun mix(v: Int, u: Int): Int = (255 shl 24) or (channel((v shr 16) and 255, (u shr 16) and 255) shl 16) or (channel((v shr 8) and 255, (u shr 8) and 255) shl 8) or channel(v and 255, u and 255)
                editable.setPixel(x, y, mix(a, b))
                editable.setPixel(x, editable.height - 1 - y, mix(b, a))
            }
            return editable.asImageBitmap().also { images[id] = it }
        }
        return scaled.asImageBitmap().also { images[id] = it }
    }
    fun sprite(id: String, worldWidth: Float, pxPerUnit: Float): Sprite =
        sprites.getOrPut("$id/$worldWidth/$pxPerUnit") {
            val source = image(id)
            val target = (worldWidth * pxPerUnit).toInt().coerceAtLeast(1)
            val bitmap = source.asAndroidBitmap()
            val resized = if (bitmap.width > target) Bitmap.createScaledBitmap(bitmap,target,(bitmap.height.toLong()*target/bitmap.width).toInt().coerceAtLeast(1),true) else bitmap
            resized.prepareToDraw()
            Sprite(resized.asImageBitmap(), resized.width / worldWidth)
        }
    fun ship(cat: CatType, width: Float, ppu: Float) = sprite(if (cat == CatType.ORANGE) "ship_orange" else "ship_black", width, ppu)
    fun prewarmHangar() {
        for (cat in listOf("orange", "black")) {
            image("turn_${cat}_2").asAndroidBitmap().prepareToDraw()
            image("pilot_$cat").asAndroidBitmap().prepareToDraw()
        }
        turnSheets.clear()
    }
    fun prewarm(stage: Int = 2) {
        NovaSector.background(stage)?.let { image(it) }
        for(id in NovaSector.assets(stage)) image(id)
        for (id in regions.keys) if (id.startsWith("EnemyKind.") || id.startsWith("BulletStyle.") || id.startsWith("ItemType.") || id.startsWith("BODY_") || id.startsWith("BONE_") || id == "COMMAND_CAP_DROP") image(id)
        for(id in listOf("SPREAD.plasma_bolt","ORANGE.SPECIAL.energy_bolt","HOMING.missile")) image(id)
        for (id in listOf("ship_orange", "ship_black", "background", "carrier", "boss")) image(id)
    }
    private fun trim(source: Bitmap): Bitmap {
        val pixels = IntArray(source.width * source.height)
        source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        var minX = source.width; var maxX = -1; var minY = source.height; var maxY = -1
        for (y in 0 until source.height) for (x in 0 until source.width) {
            if ((pixels[y * source.width + x] ushr 24) > 40) {
                minX = minOf(minX, x); maxX = maxOf(maxX, x); minY = minOf(minY, y); maxY = maxOf(maxY, y)
            }
        }
        return if (maxX < 0) source else Bitmap.createBitmap(source, minX, minY, maxX - minX + 1, maxY - minY + 1)
    }
    companion object {
        @Volatile private var shared: NovaAssets? = null
        fun get(assets: AssetManager): NovaAssets = shared ?: synchronized(this) {
            shared ?: NovaAssets(assets).also { shared = it }
        }
    }
}
