"""第 6 關：冰雪南極的企鵝。

Boss「企鵝艦長 冰冰」：圓滾滾的皇帝企鵝寶寶身形、白色心形臉、橘色小嘴、戴藍色毛線帽（頂上一顆毛球）、
繫著紅色小圍巾。企鵝個性認真、有點愛面子，55% 就被打到「卸甲」：急凍光線槍飛走，
之後肚子貼地滑行衝撞、丟會轉彎的小魚（道具由遊戲另外畫，見 scifi.cryo_gun）。
心情：m0 認真（挺胸）、m1 生氣、m2 被打哭（毛線帽飛走）。
小怪：滑行小企鵝（自毀俯衝型）、雪人砲台（地面固定型）；菁英怪：海豹教官。
"""
from common import *
from bosses import brows, crying_eyes, squash, sweat

NAVY_F = "#34456B"
NAVY_D = "#25324F"
BELLY = "#FFFFFF"
BELLY_D = "#E3EEF8"
BEAK = "#FFA43A"
BEAK_D = "#E5822A"
HAT = "#5FA8FF"
HAT_D = "#3C86E0"
SCARF = "#FF5C6E"
SCARF_D = "#D9404F"
SNOW = "#F6FBFF"
SNOW_D = "#D5E6F5"
SEAL = "#B9C4D2"
SEAL_D = "#94A1B3"
WHISTLE = "#FFD15C"

VIEW = "-180 -175 360 350"
SIZE = (540, 525)


def beanie(cx, cy, s=1.0, rot=0):
    out = path(f"M{-74 * s},{18 * s} C{-74 * s},{-56 * s} {74 * s},{-56 * s} {74 * s},{18 * s} Z", HAT, INK, 4.5)
    for k in range(-3, 4):
        out += path(f"M{k * 18 * s},{12 * s} L{k * 16 * s},{-30 * s}", "none", HAT_D, 4)
    out += path(f"M{-80 * s},{8 * s} Q0,{-4 * s} {80 * s},{8 * s} L{80 * s},{28 * s} Q0,{16 * s} {-80 * s},{28 * s} Z", "#FFFFFF", INK, 4)
    out += ellipse(0, -44 * s, 20 * s, 20 * s, "#FFFFFF", INK, 4)
    return f'<g transform="translate({cx} {cy}) rotate({rot})">{out}</g>'


def penguin_boss(m, frame):
    b = ""
    # 身體：深藍色麻糬，白色心形臉＋肚子
    b += path(mochi_path(0, 22, 286, 300), NAVY_F, INK, 5)
    b += path("M0,-70 C40,-118 118,-80 104,-6 C118,40 96,150 0,154 C-96,150 -118,40 -104,-6 C-118,-80 -40,-118 0,-70 Z", BELLY, INK, 4)
    b += ellipse(0, 112, 72, 30, BELLY_D, "none", None, 'opacity="0.8"')
    # 翅膀：左邊放下、右邊抬起（拿急凍光線槍的位置）
    b += f'<g transform="rotate(18 -140 30)">' + ellipse(-140, 66, 26, 66, NAVY_F, INK, 4.5) + "</g>"
    b += f'<g transform="rotate(-50 112 70)">' + ellipse(122, 80, 24, 56, NAVY_F, INK, 4.5) + "</g>"
    # 圍巾
    b += path("M-96,54 Q0,84 96,54 L92,76 Q0,104 -92,76 Z", SCARF, INK, 4)
    b += path("M-60,72 L-86,128 L-60,132 L-44,80 Z", SCARF, INK, 3.5)
    b += path("M-70,92 L-62,96 M-76,108 L-66,112", "none", SCARF_D, 3)
    ey = 4
    if m == 0:
        b += brows("brave", -42, 42, ey - 28)
        b += dot_eye(-42, ey, 11) + dot_eye(42, ey, 11)
        b += sweat(118, -40)
    elif m == 1:
        b += brows("angry", -42, 42, ey - 30, sw=6)
        b += dot_eye(-42, ey + 2, 10) + dot_eye(42, ey + 2, 10)
        b += anger_mark(108, -62, 18)
    else:
        b += brows("worried", -42, 42, ey - 26)
        b += crying_eyes(-42, 42, ey)
        b += tears(-56, ey + 16, 52, -1) + tears(56, ey + 16, 52, 1)
    # 橘色小嘴（m1 張嘴大叫）
    if m == 1:
        b += path(f"M-20,{ey + 26} L20,{ey + 26} L0,{ey + 40} Z", BEAK, INK, 3.5)
        b += path(f"M-16,{ey + 44} L16,{ey + 44} L0,{ey + 56} Z", BEAK_D, INK, 3.5)
    else:
        b += path(f"M-20,{ey + 26} Q0,{ey + 20} 20,{ey + 26} L0,{ey + 46} Z", BEAK, INK, 3.5)
        b += path(f"M-12,{ey + 34} L12,{ey + 34}", "none", BEAK_D, 3)
    b += blush(-76, ey + 30, 22) + blush(76, ey + 30, 22)
    # 腳
    for s in (-1, 1):
        b += path(f"M{s * 30},150 L{s * 90},150 Q{s * 96},168 {s * 74},170 L{s * 40},170 Q{s * 24},166 {s * 30},150 Z", BEAK, INK, 4)
    if m < 2:
        b += beanie(0, -106, 1.0, -6 if m == 0 else 6)
    body = g(wob(b))
    if m == 2 and frame == 1:
        body += g(tears(-56, ey + 34, 34, -1) + tears(56, ey + 34, 34, 1))
    return Sprite(f"boss_penguin_m{m}_{frame}", *SIZE, VIEW, squash(body, frame))


def lost_beanie():
    return Sprite("lost_beanie", 180, 140, "-90 -72 180 140", g(wob(beanie(0, 8, 0.95))))


# ───────────── 小怪 ─────────────

def mini(name, body, size=180, view="-60 -60 120 120"):
    return Sprite(name, size, size, view, g(wob(body)))


def slider(frame):
    """滑行小企鵝：肚子貼地往前（往下）滑，自毀俯衝型。frame 1 翅膀張開。"""
    b = ""
    flap = 34 if frame else 18
    for s in (-1, 1):
        b += f'<g transform="rotate({s * flap} {s * 22} -6)">' + ellipse(s * 30, 6, 10, 26, NAVY_F, INK, 3.5) + "</g>"
    b += ellipse(0, 0, 32, 46, NAVY_F, INK, 4)
    b += ellipse(0, 10, 22, 34, BELLY, "none")
    b += dot_eye(-10, 18, 4.5) + dot_eye(10, 18, 4.5)
    b += path("M-7,28 L7,28 L0,38 Z", BEAK, INK, 2.5)
    b += blush(-18, 28, 7, lines=False) + blush(18, 28, 7, lines=False)
    # 往下滑：雪花往後（往上）噴
    for dx, dy in ((-18, -54), (0, -62), (18, -54)):
        b += ellipse(dx, dy - (6 if frame else 0), 5, 5, SNOW, SNOW_D, 2)
    return mini(f"min_penguin_{frame}", b, 150, "-50 -72 100 115")


def snowman():
    """雪人砲台：戴水桶帽的雪人，鼻子是胡蘿蔔、手是樹枝。地面固定型。"""
    b = ellipse(0, 22, 40, 34, SNOW, INK, 4) + ellipse(0, 30, 30, 18, SNOW_D, "none")
    b += ellipse(0, -22, 30, 27, SNOW, INK, 4)
    b += path("M-24,-40 L-18,-66 L18,-66 L24,-40 Z", "#7FA7D9", INK, 3.5)
    b += path("M-30,-40 L30,-40", "none", "#5C86BE", 6)
    b += dot_eye(-10, -26, 4) + dot_eye(10, -26, 4)
    b += path("M-2,-16 L18,-12 L-2,-10 Z", "#FF8A2B", INK, 2.5)
    b += blush(-20, -14, 6, lines=False) + blush(20, -14, 6, lines=False)
    for s in (-1, 1):
        b += path(f"M{s * 36},12 L{s * 62},-8 M{s * 52},0 L{s * 58},-14", "none", "#8D6E63", 4)
    for y in (12, 26):
        b += ellipse(0, y, 3.5, 3.5, "#5C6B80", "none")
    b += path("M-30,2 Q0,10 30,2", "none", SCARF, 6)
    return mini("min_snowman", b, 170, "-68 -76 136 136")


def elite_seal(frame):
    """菁英怪：海豹教官。灰色海豹、戴教官帽、脖子上掛哨子，坐在小冰塊上。"""
    b = ellipse(0, 70, 70, 18, "#CFEAFF", INK, 3.5)
    b += path(mochi_path(0, 10, 136, 120), SEAL, INK, 4.5)
    b += ellipse(0, 40, 44, 26, "#E5ECF3", "none")
    for dx, dy in ((-30, -24), (-6, -36), (22, -20), (36, 10), (-40, 18)):
        b += ellipse(dx, dy, 4, 3, SEAL_D, "none")
    # 教官帽
    b += path("M-46,-44 Q0,-74 46,-44 L40,-30 L-40,-30 Z", "#3E4A63", INK, 4)
    b += path("M-50,-30 L50,-30 Q40,-20 0,-20 Q-40,-20 -50,-30 Z", "#2A3247", INK, 3.5)
    b += star(0, -48, 9, "#FFD15C", INK, 2)
    b += brows("brave", -22, 22, -8, w=10, sw=4)
    b += dot_eye(-22, 4, 7) + dot_eye(22, 4, 7)
    b += ellipse(0, 22, 18, 12, "#E5ECF3", INK, 3) + ellipse(0, 16, 6, 4.5, "#3E2E33", "none")
    for s in (-1, 1):
        b += path(f"M{s * 14},24 L{s * 40},20 M{s * 14},28 L{s * 38},32", "none", SEAL_D, 2.5)
    b += blush(-38, 18, 10) + blush(38, 18, 10)
    # 哨子
    b += path("M-20,46 Q0,56 20,46", "none", "#FF5C6E", 3)
    b += ellipse(4 if frame else -4, 56, 9, 7, WHISTLE, INK, 3)
    # 鰭（frame 1 舉起來吹哨）
    b += ellipse(-58, 44, 14, 24, SEAL, INK, 3.5)
    b += ellipse(58, 30 if frame else 44, 14, 24, SEAL, INK, 3.5)
    return mini(f"elite_seal_{frame}", b, 240, "-90 -100 180 190")


def all_sprites():
    out = []
    for m in range(3):
        for f in range(2):
            out.append(penguin_boss(m, f))
    out.append(lost_beanie())
    for f in range(2):
        out += [slider(f), elite_seal(f)]
    out.append(snowman())
    return out
