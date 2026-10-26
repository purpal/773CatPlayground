"""第 13 關（最終關）：泥巴農場的豬豬。

Boss「現任萌王 佩奇」：衛冕的喵星萌王。粉紅麻糬身體、大大的豬鼻子、垂耳、捲尾巴，戴真的金皇冠、
紅披風、斜掛「萌王」綵帶。一開始坐在粉紅撲滿機甲裡（m0），55% 機甲爆開「卸甲」（金幣砲飛走，道具見 coin_cannon），
30% 皇冠發光「萌王覺醒」（畫面另外加金光）。
心情：m0 坐在撲滿機甲裡得意、m1 本人生氣、m2 被打哭（綵帶飛走，皇冠歪掉）。
小怪：小雞（自毀俯衝型）、乳牛砲台（地面固定型）；菁英怪：大野狼。
"""
from common import *
from bosses import crying_eyes, squash
from scifi import GLOW, NAVY, AMBER, lit

PINK = "#FFB8C9"
PINK_D = "#F28FA8"
SNOUT = "#FF9DB5"
NOSTRIL = "#C2577A"
MECH = "#FFA6BF"
MECH_D = "#E57D9C"
STEEL = "#5B6378"
STEEL_L = "#8A96B0"
GOLD = "#FFD15C"
GOLD_D = "#E0A92E"
CAPE = "#E5484D"
CAPE_D = "#B8323A"
SASH = "#FFF3C4"
CHICK = "#FFE36B"
COW = "#FFFFFF"
COW_SPOT = "#3A3442"
WOLF = "#8E8EA6"
WOLF_D = "#6E6E86"

VIEW = "-180 -175 360 350"
SIZE = (540, 525)


def crown(cx, cy, rot=0, s=1.0):
    d = (f"M{-40 * s},{14 * s} L{-46 * s},{-22 * s} L{-24 * s},{-4 * s} L{-8 * s},{-32 * s} L{8 * s},{-32 * s} "
         f"L{24 * s},{-4 * s} L{46 * s},{-22 * s} L{40 * s},{14 * s} Z")
    out = path(d, GOLD, INK, 4)
    out += path(f"M{-40 * s},{6 * s} L{40 * s},{6 * s}", "none", GOLD_D, 4)
    out += ellipse(0, -8 * s, 6 * s, 6 * s, "#E5484D", INK, 2.5)
    out += ellipse(-24 * s, 0, 4 * s, 4 * s, "#4FC3F7", INK, 2) + ellipse(24 * s, 0, 4 * s, 4 * s, "#9B5DE5", INK, 2)
    for x in (-46, -8, 8, 46):
        out += ellipse(x * s, (-22 if abs(x) == 46 else -32) * s, 4 * s, 4 * s, "#FFFFFF", INK, 2)
    return f'<g transform="translate({cx} {cy}) rotate({rot})">{out}</g>'


def snout(cx, cy, s=1.0):
    out = ellipse(cx, cy, 34 * s, 24 * s, SNOUT, INK, 3.5 * s)
    out += ellipse(cx - 12 * s, cy, 6 * s, 9 * s, NOSTRIL, "none") + ellipse(cx + 12 * s, cy, 6 * s, 9 * s, NOSTRIL, "none")
    return out


def pig_head(m, s=1.0):
    """佩奇本人的頭（身體一體）：m 決定表情。座標以臉中心為原點。"""
    b = ""
    for side in (-1, 1):
        b += f'<g transform="rotate({side * 28} {side * 70 * s} {-70 * s})">' + path(
            f"M{side * 50 * s},{-60 * s} L{side * 96 * s},{-98 * s} L{side * 92 * s},{-40 * s} Z", PINK, INK, 4) + "</g>"
    return b


def face(m, ey, s=1.0):
    b = ""
    if m == 0:
        for side in (-1, 1):
            b += path(f"M{side * 34 * s},{ey} Q{side * 48 * s},{ey - 12 * s} {side * 62 * s},{ey}", "none", None, 5 * s)
    elif m == 1:
        for side in (-1, 1):
            b += ellipse(side * 48 * s, ey, 11 * s, 12 * s, "#FFFFFF", "none") + dot_eye(side * 48 * s, ey + 2 * s, 7.5 * s)
            b += path(f"M{side * 30 * s},{ey - 2 * s} L{side * 64 * s},{ey - 12 * s} L{side * 64 * s},{ey - 16 * s} L{side * 30 * s},{ey - 16 * s} Z", PINK, "none")
            b += path(f"M{side * 24 * s},{ey - 14 * s} L{side * 70 * s},{ey - 28 * s}", "none", None, 6 * s)
    else:
        for side in (-1, 1):
            b += path(f"M{side * 28 * s},{ey - 28 * s} Q{side * 46 * s},{ey - 26 * s} {side * 64 * s},{ey - 16 * s}", "none", None, 5 * s)
        b += crying_eyes(-48 * s, 48 * s, ey, 10 * s)
        b += tears(-62 * s, ey + 16 * s, 46 * s, -1) + tears(62 * s, ey + 16 * s, 46 * s, 1)
    b += snout(0, ey + 34 * s, s)
    if m == 1:
        b += open_mouth(0, ey + 64 * s, 14 * s, 14 * s)
    elif m == 2:
        b += wavy_mouth(0, ey + 70 * s, 12 * s)
    else:
        b += omega_mouth(0, ey + 66 * s, 10 * s)
    b += blush(-86 * s, ey + 30 * s, 20 * s) + blush(86 * s, ey + 30 * s, 20 * s)
    return b


def pig_boss(m, frame):
    b = ""
    if m == 0:
        # 粉紅撲滿機甲：圓滾滾的撲滿身體、頂上投幣口、前面畫著可愛的撲滿臉、鋼製短腿；佩奇坐在頂上的玻璃座艙
        for x in (-110, -40, 40, 110):
            b += path(f"M{x - 18},120 L{x + 18},120 L{x + 22},166 L{x - 22},166 Z", STEEL, INK, 4)
            b += path(f"M{x - 22},158 L{x + 22},158", "none", STEEL_L, 4)
        b += path(mochi_path(0, 58, 330, 216, 1.05), MECH, INK, 5)
        b += path("M-150,90 C-110,150 110,150 150,90 C100,130 -100,130 -150,90 Z", MECH_D, "none", None, 'opacity="0.7"')
        b += path("M-24,-46 L24,-46 L24,-38 L-24,-38 Z", "#5A2E44", INK, 3)
        # 撲滿臉
        for side in (-1, 1):
            b += ellipse(side * 70, 40, 12, 14, "#3A2C3A", "none") + ellipse(side * 74, 35, 4, 4, "#FFFFFF", "none")
            b += path(f"M{side * 120},-6 L{side * 160},-40 L{side * 150},14 Z", MECH, INK, 4)
        b += ellipse(0, 78, 46, 32, SNOUT, INK, 4)
        b += ellipse(-15, 78, 8, 12, NOSTRIL, "none") + ellipse(15, 78, 8, 12, NOSTRIL, "none")
        b += path("M-120,104 L-96,96 M120,104 L96,96", "none", MECH_D, 4)
        for x in (-130, 130):
            b += ellipse(x, 60, 10, 10, STEEL_L, INK, 3)
        # 座艙
        b += path("M-70,-40 Q-70,-140 0,-140 Q70,-140 70,-40 Z", "#DDF3FF", INK, 4, 'fill-opacity="0.75"')
        b += f'<g transform="translate(0 -84) scale(0.42)">'
        b += pig_head(0) + path(mochi_path(0, 10, 220, 190), PINK, INK, 8) + face(0, -6)
        b += crown(0, -110, -6, 1.2) + "</g>"
        b += path("M-50,-120 Q-40,-134 -20,-136", "none", "#FFFFFF", 6, 'opacity="0.8"')
        b += path("M-74,-40 L74,-40 L70,-28 L-70,-28 Z", STEEL, INK, 4)
    else:
        # 佩奇本人
        b += path("M-118,-6 Q-168,110 -150,170 L150,170 Q168,110 118,-6 Z", CAPE, INK, 4.5)
        b += path("M-140,162 Q0,140 140,162 L150,170 L-150,170 Z", CAPE_D, "none")
        b += path("M118,118 Q160,118 150,92 Q142,72 128,86 Q122,100 140,100", "none", PINK_D, 7)
        b += pig_head(m)
        b += path(mochi_path(0, 36, 260, 232), PINK, INK, 5)
        b += ellipse(0, 116, 82, 38, PINK_D, "none", None, 'opacity="0.45"')
        b += face(m, -2)
        if m == 1:
            b += anger_mark(118, -40, 18)
            # 「萌王」綵帶
            b += path("M-118,30 L96,150 L78,166 L-128,48 Z", SASH, INK, 3.5)
            b += text(-10, 108, "萌王", 26, "#E5484D", stroke="none", sw=0, rot=30)
        b += ellipse(-110, 104, 22, 20, PINK, INK, 4) + ellipse(108, 96, 22, 20, PINK, INK, 4)
        for side in (-1, 1):
            b += ellipse(side * 58, 156, 34, 16, PINK, INK, 4)
            b += path(f"M{side * 58},146 L{side * 58},166", "none", PINK_D, 4)
        b += crown(0, -94, 0 if m == 1 else 14, 1.3)
    body = g(wob(b))
    if m == 2 and frame == 1:
        body += g(tears(-62, 30, 34, -1) + tears(62, 30, 34, 1))
    return Sprite(f"boss_pig_m{m}_{frame}", *SIZE, VIEW, squash(body, frame))


def lost_sash():
    b = path("M-90,-14 L90,-14 L90,14 L-90,14 Z", SASH, INK, 3.5)
    b += text(0, 9, "萌王", 24, "#E5484D", stroke="none", sw=0)
    return Sprite("lost_sash", 220, 60, "-100 -24 200 48", g(wob(b)))


def coin_cannon():
    """撲滿機甲的「金幣砲」：鋼製機械臂接上粉紅色的砲口，砲口裡一枚發光的金幣。朝 +x。"""
    b = path("M-4,-14 L56,-14 L56,14 L-4,14 Z", "url(#steel)", NAVY, 3)
    b += path("M14,-14 L14,14 M40,-14 L40,14", "none", STEEL_L, 3)
    b += path("M56,-22 L104,-18 L104,18 L56,22 Z", MECH, NAVY, 3)
    b += path("M64,-20 L64,20", "none", MECH_D, 3)
    b += lit(f'<ellipse cx="104" cy="0" rx="6" ry="15" fill="{GOLD}" stroke="{AMBER}" stroke-width="3"/>', 3)
    return Sprite("prop_coin_head", 240, 100, "-8 -25 120 50", b, GLOW)


def lost_coingun():
    h = path("M-90,-7 L-36,-7 L-36,7 L-90,7 Z", "url(#steel)", NAVY, 3)
    h += f'<g transform="translate(-40 0)">{coin_cannon().body}</g>'
    return Sprite("lost_coingun", 300, 140, "-95 -35 190 70", h, GLOW)


# ───────────── 小怪 ─────────────

def mini(name, body, size=180, view="-60 -60 120 120"):
    return Sprite(name, size, size, view, g(wob(body)))


def chick(frame):
    """小雞：黃色圓球、小翅膀拍拍，衝向玩家。自毀俯衝型。frame 1 翅膀舉起。"""
    up = -30 if frame else 10
    b = ""
    for s in (-1, 1):
        b += f'<g transform="rotate({s * up} {s * 22} 4)">' + ellipse(s * 30, 6, 12, 8, "#FFD23F", INK, 3) + "</g>"
    b += ellipse(0, 4, 28, 26, CHICK, INK, 3.5)
    b += path("M-4,-22 Q0,-34 6,-24 Q10,-32 12,-22", "none", "#FFD23F", 4)
    b += dot_eye(-10, 0, 3.5) + dot_eye(10, 0, 3.5)
    b += path("M-6,8 L6,8 L0,16 Z", "#FF9A3C", INK, 2)
    b += blush(-18, 10, 6, lines=False) + blush(18, 10, 6, lines=False)
    b += path("M-8,30 L-8,38 M8,30 L8,38", "none", "#FF9A3C", 3)
    return mini(f"min_chick_{frame}", b, 120, "-44 -40 88 84")


def cow(frame):
    """乳牛砲台：白底黑斑的乳牛從柵欄後探頭，張嘴時（frame 1）噴牛奶彈。地面固定型。"""
    b = ""
    for s in (-1, 1):
        b += path(f"M{s * 26},-30 Q{s * 40},-46 {s * 34},-52", "none", "#E8D9B0", 6)
        b += ellipse(s * 40, -20, 14, 8, COW, INK, 3)
    b += ellipse(0, -6, 36, 32, COW, INK, 4)
    b += path("M-30,-24 Q-14,-30 -10,-14 Q-24,-6 -30,-24 Z", COW_SPOT, "none")
    b += ellipse(20, -26, 9, 7, COW_SPOT, "none")
    b += dot_eye(-13, -10, 4) + dot_eye(13, -10, 4)
    b += ellipse(0, 12, 22, 14, "#FFC7D6", INK, 3)
    if frame:
        b += ellipse(0, 18, 9, 6, "#7A2340", INK, 2)
    else:
        b += ellipse(-7, 10, 3, 4, NOSTRIL, "none") + ellipse(7, 10, 3, 4, NOSTRIL, "none")
    # 柵欄
    for x in (-40, 0, 40):
        b += path(f"M{x - 6},14 L{x + 6},14 L{x + 6},44 L{x - 6},44 Z", "#C98B55", INK, 3)
    b += path("M-50,24 L50,24 L50,34 L-50,34 Z", "#D9A066", INK, 3)
    return mini(f"min_cow_{frame}", b, 140, "-56 -60 112 110")


def elite_wolf(frame):
    """菁英怪：大野狼。灰色、尖耳朵、白色口鼻，圍著三隻小豬的拼布圍巾；frame 1 鼓起臉頰用力吹氣。"""
    b = ""
    for s in (-1, 1):
        b += path(f"M{s * 24},-40 L{s * 52},-84 L{s * 60},-30 Z", WOLF, INK, 4)
        b += path(f"M{s * 32},-42 L{s * 50},-70 L{s * 54},-38 Z", "#F2C9D6", "none")
    b += path(mochi_path(0, 14, 128, 124), WOLF, INK, 4.5)
    b += ellipse(0, 40, 36, 26, "#EDEDF5", "none")
    b += dot_eye(-22, -10, 5.5) + dot_eye(22, -10, 5.5)
    b += path("M-34,-24 L-12,-18 M34,-24 L12,-18", "none", WOLF_D, 5)
    if frame:
        b += ellipse(0, 14, 34, 24, "#EDEDF5", INK, 3.5)
        b += ellipse(0, 20, 8, 8, "#7A2340", INK, 2)
        for k in range(3):
            b += path(f"M{-14 + k * 14},40 Q{-20 + k * 14},64 {-12 + k * 14},84", "none", "#FFFFFF", 5, 'opacity="0.9"')
    else:
        b += ellipse(0, 12, 26, 18, "#EDEDF5", INK, 3)
        b += ellipse(0, 4, 8, 6, "#3A3442", "none")
        b += path("M-10,18 Q0,24 10,18", "none", None, 2.5)
    b += blush(-36, 6, 9, lines=False) + blush(36, 6, 9, lines=False)
    b += path("M-50,46 Q0,64 50,46 L48,58 Q0,76 -48,58 Z", "#FF9DB5", INK, 3)
    for x in (-26, 0, 26):
        b += ellipse(x, 58, 6, 4, NOSTRIL, "none")
    b += ellipse(-48, 66, 13, 10, WOLF, INK, 3.5) + ellipse(48, 66, 13, 10, WOLF, INK, 3.5)
    return mini(f"elite_wolf_{frame}", b, 240, "-90 -100 180 200")


def all_sprites():
    out = []
    for m in range(3):
        for f in range(2):
            out.append(pig_boss(m, f))
    out += [lost_sash(), coin_cannon(), lost_coingun()]
    for f in range(2):
        out += [chick(f), cow(f), elite_wolf(f)]
    return out
