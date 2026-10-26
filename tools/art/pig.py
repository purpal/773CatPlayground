"""法國：薰衣草農場的豬豬。

Boss「松露獵人 佩奇」：粉紅麻糬身體、大大的豬鼻子、垂耳、捲尾巴，戴藏青色貝雷帽、穿藍白條紋衫、圍紅領巾，
拿著電漿法棍（道具見 baguette_head）。招牌是泥巴坑，還會用鼻子嗅出地底的松露。
45%「卸甲」：法棍飛走。心情：m0 得意（瞇眼）、m1 生氣、m2 被打哭（貝雷帽飛走）。
小怪：小雞（自毀俯衝型）、乳牛砲台（地面固定型）；菁英怪：大野狼。
"""
from common import *
from bosses import crying_eyes, squash
from scifi import GLOW, NAVY, AMBER, lit

PINK = "#FFB8C9"
PINK_D = "#F28FA8"
SNOUT = "#FF9DB5"
NOSTRIL = "#C2577A"
STRIPE = "#3F5FA8"
SHIRT = "#FFFFFF"
BERET = "#2E3A66"
SCARF = "#E5484D"
BREAD = "#E8A85A"
BREAD_D = "#C27A35"
CHICK = "#FFE36B"
COW = "#FFFFFF"
COW_SPOT = "#3A3442"
WOLF = "#8E8EA6"
WOLF_D = "#6E6E86"

VIEW = "-180 -175 360 350"
SIZE = (540, 525)
BODY = mochi_path(0, 36, 260, 232)


def beret(cx, cy, rot=0, s=1.0):
    out = ellipse(0, 0, 70 * s, 26 * s, BERET, INK, 4.5)
    out += path(f"M{-60 * s},{4 * s} Q0,{20 * s} {60 * s},{4 * s}", "none", "#1F2850", 4)
    out += path(f"M{-4 * s},{-24 * s} Q0,{-38 * s} {8 * s},{-36 * s}", "none", INK, 5)
    return f'<g transform="translate({cx} {cy}) rotate({rot})">{out}</g>'


def snout(cx, cy, s=1.0):
    out = ellipse(cx, cy, 34 * s, 24 * s, SNOUT, INK, 3.5 * s)
    out += ellipse(cx - 12 * s, cy, 6 * s, 9 * s, NOSTRIL, "none") + ellipse(cx + 12 * s, cy, 6 * s, 9 * s, NOSTRIL, "none")
    return out


def ears():
    b = ""
    for side in (-1, 1):
        b += f'<g transform="rotate({side * 28} {side * 70} -70)">' + path(f"M{side * 50},-60 L{side * 96},-98 L{side * 92},-40 Z", PINK, INK, 4) + "</g>"
    return b


def face(m, ey):
    b = ""
    if m == 0:
        for side in (-1, 1):
            b += path(f"M{side * 34},{ey} Q{side * 48},{ey - 12} {side * 62},{ey}", "none", None, 5)
    elif m == 1:
        for side in (-1, 1):
            b += ellipse(side * 48, ey, 11, 12, "#FFFFFF", "none") + dot_eye(side * 48, ey + 2, 7.5)
            b += path(f"M{side * 30},{ey - 2} L{side * 64},{ey - 12} L{side * 64},{ey - 16} L{side * 30},{ey - 16} Z", PINK, "none")
            b += path(f"M{side * 24},{ey - 14} L{side * 70},{ey - 28}", "none", None, 6)
    else:
        for side in (-1, 1):
            b += path(f"M{side * 28},{ey - 28} Q{side * 46},{ey - 26} {side * 64},{ey - 16}", "none", None, 5)
        b += crying_eyes(-48, 48, ey, 10)
        b += tears(-62, ey + 16, 46, -1) + tears(62, ey + 16, 46, 1)
    b += snout(0, ey + 34)
    if m == 1:
        b += open_mouth(0, ey + 64, 14, 14)
    elif m == 2:
        b += wavy_mouth(0, ey + 70, 12)
    else:
        b += omega_mouth(0, ey + 66, 10)
    b += blush(-86, ey + 30, 20) + blush(86, ey + 30, 20)
    return b


def striped_shirt():
    """藍白條紋衫：用身體輪廓裁切，從胸口以下開始。"""
    out = path("M-160,78 Q0,108 160,78 L160,200 L-160,200 Z", SHIRT, "none")
    for k in range(5):
        y = 96 + k * 18
        out += path(f"M-160,{y} Q0,{y + 26} 160,{y} L160,{y + 9} Q0,{y + 35} -160,{y + 9} Z", STRIPE, "none")
    return f'<clipPath id="pigBody"><path d="{BODY}"/></clipPath><g clip-path="url(#pigBody)">{out}</g>'


def pig_boss(m, frame):
    b = ""
    b += path("M118,118 Q160,118 150,92 Q142,72 128,86 Q122,100 140,100", "none", PINK_D, 7)
    b += ears()
    b += path(BODY, PINK, INK, 5)
    b += striped_shirt()
    b += path(BODY, "none", INK, 5)
    b += face(m, -2)
    if m == 1:
        b += anger_mark(118, -40, 18)
    # 紅領巾
    b += path("M-70,72 Q0,96 70,72 L64,90 Q0,112 -64,90 Z", SCARF, INK, 3.5)
    b += path("M30,92 L58,128 L26,124 Z", SCARF, INK, 3)
    # 手：右手拿法棍
    b += ellipse(-110, 104, 22, 20, PINK, INK, 4) + ellipse(108, 96, 22, 20, PINK, INK, 4)
    for side in (-1, 1):
        b += ellipse(side * 58, 156, 34, 16, PINK, INK, 4)
        b += path(f"M{side * 58},146 L{side * 58},166", "none", PINK_D, 4)
    if m < 2:
        b += beret(-10, -88, -10 if m == 0 else 6)
    body = g(wob(b))
    if m == 2 and frame == 1:
        body += g(tears(-62, 30, 34, -1) + tears(62, 30, 34, 1))
    return Sprite(f"boss_pig_m{m}_{frame}", *SIZE, VIEW, squash(body, frame))


def lost_beret():
    return Sprite("lost_beret", 180, 110, "-80 -46 160 92", g(wob(beret(0, 4))))


def baguette_head():
    """松露獵人的「電漿法棍」：一條烤得金黃的法棍，斜斜的割紋發出琥珀色的光。朝 +x，握柄由遊戲畫。"""
    b = path("M-4,-12 Q60,-20 128,-10 Q142,0 128,10 Q60,20 -4,12 Q-10,0 -4,-12 Z", BREAD, NAVY, 3)
    b += path("M0,-6 Q60,-12 120,-6", "none", "#F6CE8C", 4)
    cuts = ""
    for k in range(4):
        x = 18 + k * 28
        cuts += f'<path d="M{x},8 L{x + 16},-8" stroke="{AMBER}" stroke-width="4" fill="none"/>'
    b += lit(cuts, 2)
    b += path("M14,8 L30,-8 M42,8 L58,-8 M70,8 L86,-8 M98,8 L114,-8", "none", BREAD_D, 2)
    return Sprite("prop_baguette_head", 300, 70, "-10 -17 150 34", b, GLOW)


def lost_baguette():
    h = path("M-90,-7 L-30,-7 L-30,7 L-90,7 Z", "url(#steel)", NAVY, 3)
    h += f'<g transform="translate(-34 0)">{baguette_head().body}</g>'
    return Sprite("lost_baguette", 340, 90, "-95 -22 210 44", h, GLOW)


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
    out += [lost_beret(), baguette_head(), lost_baguette()]
    for f in range(2):
        out += [chick(f), cow(f), elite_wolf(f)]
    return out
