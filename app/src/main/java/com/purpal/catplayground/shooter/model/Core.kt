package com.purpal.catplayground.shooter.model

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.floor

/** 世界寬度固定，高度依螢幕比例決定（見 [World.height]）。 */
const val WORLD_WIDTH = 1000f

/** 固定的物理更新間隔。 */
const val STEP = 1f / 60f

const val DEG = (PI / 180.0).toFloat()
const val HALF_PI = (PI / 2.0).toFloat()
const val TWO_PI = (PI * 2.0).toFloat()

enum class CatType(
    val displayName: String,
    val shortName: String,
    val maxArmor: Float,
    val baseSpeed: Float,
    val specialName: String,
    val ultimateName: String,
) {
    ORANGE("橘斑貓 小橘", "小橘", 112f, 560f, "重力肉球砲", "猛虎咆哮"),
    /** 移動特化：速度快、判定點小，快速甩動手指會「影閃」（短暫無敵＋殘影）。 */
    BLACK("黑貓 小黑", "小黑", 70f, 780f, "單分子爪刃", "黑豹咆哮"),
}

enum class MainWeapon(val label: String) { SPREAD("散射"), FOCUS("集中"), SPECIAL("專武") }

enum class SubWeapon(val label: String) { NONE("無"), HOMING("追蹤飛彈"), SONIC("力場盾") }

/** DATA：消彈時子彈分解成的資料方塊，飛向貓咪加分。 */
enum class ItemType { SPREAD, FOCUS, SPECIAL, HOMING, SONIC, REPAIR, BOMB, COIN, DATA }

/** 給畫面播放音效用的事件。 */
enum class SoundCue {
    SHOT, SHOT_HEAVY, ENEMY_HIT, EXPLODE_SMALL, EXPLODE_BIG, ITEM, POWER_UP, PLAYER_HIT,
    ROAR, BOMB_BLAST, WARNING, BOSS_DOWN, SONIC, MISSILE, LASER, BEAM, DATA, SWING, BREAK, ARMOR_UP,
}

enum class Phase {
    /** 關卡標題，機體從下方飛入。 */
    INTRO,
    STAGE,
    /** 警告畫面；Boss 在這段時間進場，到定位後直接開打。 */
    WARNING,
    BOSS,
    BOSS_DEFEATED,
    OUTRO_DIALOG,
    CLEAR,
    /** 護甲歸零，爆炸中。 */
    PLAYER_DOWN,
    /** 等玩家選擇接關；世界暫停。 */
    CONTINUE,
}

fun angleTo(fromX: Float, fromY: Float, toX: Float, toY: Float): Float = atan2(toY - fromY, toX - fromX)

fun dist2(ax: Float, ay: Float, bx: Float, by: Float): Float {
    val dx = ax - bx
    val dy = ay - by
    return dx * dx + dy * dy
}

/** 時間從 t-dt 走到 t 的這一步是否跨過 interval 的倍數（含 0）。 */
fun every(t: Float, dt: Float, interval: Float, offset: Float = 0f): Boolean {
    val now = t - offset
    if (now < 0f) return false
    val before = now - dt
    return before < 0f || floor(now / interval) != floor(before / interval)
}

/** 時間是否剛好在這一步跨過某個時間點。 */
fun crossed(t: Float, dt: Float, at: Float): Boolean = t >= at && t - dt < at
