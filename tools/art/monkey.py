"""第 12 關：叢林遺跡的猴子。

Boss「遺跡猴王 吱吱」：棕色麻糬身體、心形的蜜桃色臉、大圓耳、捲尾巴，戴探險帽、圍綠色領巾。
招牌「有樣學樣」：模仿玩家現在的主武器。打到 30% 才「裝甲」（猴王覺醒）：額頭戴上金色頭箍，
拿出伸縮電漿棍（道具見 ruyi_head）。
心情：m0 調皮（露牙笑）、m1 覺醒生氣（金色頭箍）、m2 被打哭（探險帽飛走）。
小怪：盪藤小猴（自毀俯衝型）、遺跡石像砲台（地面固定型）；菁英怪：大猩猩守衛。
"""
from common import *
from bosses import crying_eyes, squash
from scifi import GLOW, NAVY, AMBER, CORAL, lit

FUR = "#A8744F"
FUR_D = "#8A5A3A"
FACE = "#F2C9A8"
FACE_D = "#E3AE88"
HAT = "#D9C38F"
HAT_D = "#B9A26E"
SCARF = "#4FA86A"
GOLD = "#FFD15C"
GOLD_D = "#E0A92E"
BANANA = "#FFE07A"
STONE = "#B8B2A6"
STONE_D = "#8F897D"
GORILLA = "#4A4452"
GORILLA_F = "#8E8496"

VIEW = "-180 -175 360 350"
SIZE = (540, 525)


def explorer_hat(cx, cy, rot=0, s=1.0):
    out = ellipse(0, 6 * s, 84 * s, 16 * s, HAT_D, INK, 4)
    out += path(f"M{-56 * s},{6 * s} C{-54 * s},{-56 * s} {54 * s},{-56 * s} {56 * s},{6 * s} Z", HAT, INK, 4.5)
    out += path(f"M{-56 * s},{-6 * s} Q0,{-14 * s} {56 * s},{-6 * s} L{56 * s},{4 * s} Q0,{-4 * s} {-56 * s},{4 * s} Z", "#7A5A3A", INK, 3)
    out += ellipse(0, -38 * s, 7 * s, 5 * s, HAT_D, INK, 2.5)
    return f'<g transform="translate({cx} {cy}) rotate({rot})">{out}</g>'


def banana(cx, cy, s=1.0, rot=0):
    out = path(f"M{-22 * s},{-6 * s} Q0,{22 * s} {24 * s},{-8 * s} Q4,{8 * s} {-22 * s},{-6 * s} Z", BANANA, INK, 3)
    out += path(f"M{24 * s},{-8 * s} L{28 * s},{-14 * s}", "none", "#8D6E63", 4)
    return f'<g transform="translate({cx} {cy}) rotate({rot})">{out}</g>'


def monkey_boss(m, frame):
    b = ""
    # 捲尾巴
    b += path("M100,120 Q178,120 170,60 Q164,22 132,30 Q120,44 140,50 Q154,56 150,74 Q140,100 96,96", "none", FUR, 16)
    b += path("M100,120 Q178,120 170,60 Q164,22 132,30", "none", INK, 2)
    # 大圓耳
    for s in (-1, 1):
        b += ellipse(s * 122, -10, 38, 40, FUR, INK, 4.5) + ellipse(s * 120, -8, 24, 26, FACE, "none")
    b += path(mochi_path(0, 36, 250, 232), FUR, INK, 5)
    b += ellipse(0, 118, 72, 34, FUR_D, "none", None, 'opacity="0.5"')
    # 心形的臉
    b += path("M0,-48 C30,-80 92,-62 92,-6 C92,48 44,82 0,86 C-44,82 -92,48 -92,-6 C-92,-62 -30,-80 0,-48 Z", FACE, INK, 3.5)
    ey = 0
    if m == 0:
        for s in (-1, 1):
            b += ellipse(s * 38, ey, 11, 12, "#FFFFFF", "none") + dot_eye(s * 38, ey + 1, 8)
            b += path(f"M{s * 24},{ey - 22} Q{s * 38},{ey - 30} {s * 54},{ey - 22}", "none", None, 4)
    elif m == 1:
        # 金色頭箍
        b += path("M-90,-30 Q0,-56 90,-30 L88,-16 Q0,-42 -88,-16 Z", GOLD, INK, 4)
        b += ellipse(0, -40, 12, 10, "#FF5C6E", INK, 3)
        for s in (-1, 1):
            b += ellipse(s * 38, ey, 12, 13, "#FFFFFF", "none") + dot_eye(s * 38, ey + 2, 8)
            b += path(f"M{s * 24},{ey - 2} L{s * 54},{ey - 12} L{s * 54},{ey - 16} L{s * 24},{ey - 16} Z", FACE, "none")
            b += path(f"M{s * 18},{ey - 12} L{s * 60},{ey - 26}", "none", None, 6)
        b += anger_mark(112, -60, 18)
    else:
        for s in (-1, 1):
            b += path(f"M{s * 20},{ey - 26} Q{s * 38},{ey - 24} {s * 56},{ey - 14}", "none", None, 5)
            b += ellipse(s * 38, ey, 14, 15, "#FFFFFF", "none")
        b += crying_eyes(-38, 38, ey + 1, 10)
        b += tears(-52, ey + 18, 50, -1) + tears(52, ey + 18, 50, 1)
    # 鼻孔與嘴
    b += ellipse(-6, ey + 26, 3.5, 2.5, FUR_D, "none") + ellipse(6, ey + 26, 3.5, 2.5, FUR_D, "none")
    if m == 0:
        # 露牙調皮笑
        b += path(f"M-30,{ey + 40} Q0,{ey + 72} 30,{ey + 40} Z", "#7A2340", INK, 3.5)
        b += path(f"M-26,{ey + 42} L26,{ey + 42} L22,{ey + 50} L-22,{ey + 50} Z", "#FFFFFF", "none")
    elif m == 1:
        b += open_mouth(0, ey + 42, 18, 18, fangs=True)
    else:
        b += wavy_mouth(0, ey + 50, 14)
    b += blush(-66, ey + 30, 18) + blush(66, ey + 30, 18)
    # 綠色領巾
    b += path("M-92,92 Q0,124 92,92 L88,110 Q0,140 -88,110 Z", SCARF, INK, 3.5)
    b += path("M30,112 L54,148 L28,144 Z", SCARF, INK, 3)
    # 手：右手拿伸縮棍；左手拿香蕉
    b += ellipse(-110, 108, 22, 20, FUR, INK, 4)
    if m == 0:
        b += banana(-118, 84, 1.1, -30)
    b += ellipse(108, 100, 22, 20, FUR, INK, 4)
    for s in (-1, 1):
        b += ellipse(s * 58, 156, 36, 16, FUR, INK, 4) + ellipse(s * 58, 158, 22, 8, FACE_D, "none")
    if m < 2:
        b += explorer_hat(0, -92, -6 if m == 0 else 0)
    body = g(wob(b))
    if m == 2 and frame == 1:
        body += g(tears(-52, ey + 36, 34, -1) + tears(52, ey + 36, 34, 1))
    return Sprite(f"boss_monkey_m{m}_{frame}", *SIZE, VIEW, squash(body, frame))


def lost_helmet():
    return Sprite("lost_helmet", 200, 110, "-90 -52 180 76", g(wob(explorer_hat(0, 0))))


def ruyi_head():
    """猴王覺醒後的「伸縮電漿棍」：金色棍頭（兩圈金箍）＋紅色能量核心。朝 +x，棍身由遊戲畫。"""
    b = path("M-4,-12 L60,-12 L60,12 L-4,12 Z", "#E0402F", NAVY, 3)
    b += path("M8,-12 L8,12 M50,-12 L50,12", "none", "#B02A20", 2.5)
    b += path("M60,-16 L100,-16 L100,16 L60,16 Z", GOLD, NAVY, 3)
    b += path("M70,-16 L70,16 M90,-16 L90,16", "none", GOLD_D, 4)
    b += lit(f'<path d="M100,-10 L108,0 L100,10" fill="none" stroke="{AMBER}" stroke-width="4"/>', 3)
    return Sprite("prop_ruyi_head", 230, 80, "-8 -20 115 40", b, GLOW)


# ───────────── 小怪 ─────────────

def mini(name, body, size=180, view="-60 -60 120 120"):
    return Sprite(name, size, size, view, g(wob(body)))


def swing_monkey(frame):
    """盪藤小猴：單手抓著藤蔓盪下來，再撲向玩家。自毀俯衝型。frame 1 腳踢出去。"""
    b = path("M0,-60 L6,-26", "none", "#5FA84A", 6)
    for k in range(3):
        b += ellipse(4 + (k % 2) * 6, -54 + k * 12, 6, 3.5, "#7CC46A", "none")
    b += path("M4,-28 Q14,-22 14,-12", "none", FUR, 9)
    b += ellipse(0, 4, 26, 24, FUR, INK, 3.5)
    for s in (-1, 1):
        b += ellipse(s * 24, -2, 9, 10, FUR, INK, 3) + ellipse(s * 24, -2, 5, 6, FACE, "none")
    b += ellipse(0, 6, 18, 15, FACE, "none")
    b += dot_eye(-7, 2, 3.5) + dot_eye(7, 2, 3.5)
    b += path("M-5,12 Q0,16 5,12", "none", None, 2)
    kick = 10 if frame else 0
    b += ellipse(-12, 28 + kick, 8, 6, FUR, INK, 2.5) + ellipse(12, 28 - kick // 2, 8, 6, FUR, INK, 2.5)
    return mini(f"min_swingmonkey_{frame}", b, 140, "-46 -64 92 108")


def totem(frame):
    """遺跡石像砲台：方頭石像，眼睛亮起時（frame 1）發射。地面固定型。"""
    b = path("M-36,-36 L36,-36 L40,40 L-40,40 Z", STONE, INK, 4)
    b += path("M-36,-36 L36,-36 L32,-26 L-32,-26 Z", STONE_D, INK, 3)
    b += path("M-24,12 L24,12", "none", STONE_D, 4)
    for x in (-18, 0, 18):
        b += path(f"M{x - 4},12 L{x - 4},22 M{x + 4},12 L{x + 4},22", "none", STONE_D, 3)
    for s in (-1, 1):
        if frame:
            b += ellipse(s * 15, -10, 9, 7, "#FFB300", INK, 2.5) + ellipse(s * 15, -10, 4, 3, "#FFFFFF", "none")
        else:
            b += path(f"M{s * 15 - 8},-10 L{s * 15 + 8},-10", "none", None, 3)
    b += path("M-6,-2 L0,4 L6,-2", "none", STONE_D, 3)
    b += ellipse(-30, 30, 8, 5, "#7CC46A", "none") + ellipse(28, -30, 7, 4, "#7CC46A", "none")
    return mini(f"min_totem_{frame}", b, 140, "-50 -50 100 100")


def elite_gorilla(frame):
    """菁英怪：大猩猩守衛。深灰色、灰色臉、戴頭盔拿圓盾；frame 1 捶胸。"""
    b = ""
    b += path(mochi_path(0, 14, 136, 126), GORILLA, INK, 4.5)
    b += ellipse(0, 40, 40, 28, GORILLA_F, "none", None, 'opacity="0.7"')
    b += ellipse(0, -2, 34, 30, GORILLA_F, INK, 3)
    b += path("M-30,-20 Q0,-30 30,-20", "none", GORILLA, 7)
    b += dot_eye(-14, -8, 5) + dot_eye(14, -8, 5)
    b += ellipse(-5, 8, 4, 3, GORILLA, "none") + ellipse(5, 8, 4, 3, GORILLA, "none")
    b += path("M-10,18 Q0,14 10,18", "none", None, 3)
    b += blush(-28, 6, 8, lines=False) + blush(28, 6, 8, lines=False)
    # 頭盔
    b += path("M-46,-36 Q-44,-80 0,-82 Q44,-80 46,-36 Z", "#C9A25E", INK, 4)
    b += path("M-50,-36 L50,-36 L48,-28 L-48,-28 Z", "#A8823E", INK, 3)
    b += path("M0,-82 L0,-96", "none", "#E5484D", 8)
    if frame:
        b += ellipse(-20, 34, 18, 16, GORILLA, INK, 3.5) + ellipse(20, 34, 18, 16, GORILLA, INK, 3.5)
        for x, y in ((-60, 20), (60, 20), (0, 70)):
            b += star(x, y, 8, "#FFE07A", INK, 2)
    else:
        b += ellipse(-62, 30, 18, 16, GORILLA, INK, 3.5)
        b += ellipse(66, 22, 28, 30, "#C9A25E", INK, 4) + ellipse(66, 22, 10, 10, "#E5484D", INK, 2.5)
    b += ellipse(-36, 82, 20, 10, GORILLA, INK, 3.5) + ellipse(36, 82, 20, 10, GORILLA, INK, 3.5)
    return mini(f"elite_gorilla_{frame}", b, 240, "-100 -110 200 210")


def all_sprites():
    out = []
    for m in range(3):
        for f in range(2):
            out.append(monkey_boss(m, f))
    out += [lost_helmet(), ruyi_head()]
    for f in range(2):
        out += [swing_monkey(f), totem(f), elite_gorilla(f)]
    return out
