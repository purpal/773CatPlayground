package com.purpal.catplayground.shooter.model

/**
 * 破關時機體的狀態（武器等級、大絕存量、護甲），直接進下一關時沿用；
 * 回到關卡列表時先存起來，下次挑戰下一關時用一次。
 */
data class Loadout(
    val mainWeapon: MainWeapon,
    val mainLevel: Int,
    val subWeapon: SubWeapon,
    val subLevel: Int,
    val bombs: Int,
    val armorRatio: Float,
) {
    fun applyTo(p: Player) {
        p.mainWeapon = mainWeapon
        p.mainLevel = mainLevel.coerceIn(1, Player.MAX_MAIN_LEVEL)
        p.subWeapon = subWeapon
        p.subLevel = if (subWeapon == SubWeapon.NONE) 0 else subLevel.coerceIn(1, Player.MAX_SUB_LEVEL)
        p.bombs = bombs.coerceIn(0, Player.MAX_BOMBS)
        // 至少留一點護甲，避免一開場就被打倒。
        p.armor = p.cat.maxArmor * armorRatio.coerceIn(0.2f, 1f)
    }

    fun encode(): String = listOf(mainWeapon.name, mainLevel, subWeapon.name, subLevel, bombs, armorRatio).joinToString(",")

    companion object {
        fun of(p: Player) = Loadout(p.mainWeapon, p.mainLevel, p.subWeapon, p.subLevel, p.bombs, p.armorRatio)

        fun decode(text: String?): Loadout? {
            val parts = text?.split(",") ?: return null
            if (parts.size != 6) return null
            return runCatching {
                Loadout(
                    MainWeapon.valueOf(parts[0]), parts[1].toInt(), SubWeapon.valueOf(parts[2]), parts[3].toInt(),
                    parts[4].toInt(), parts[5].toFloat(),
                )
            }.getOrNull()
        }
    }
}
