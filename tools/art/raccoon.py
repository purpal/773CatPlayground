"""第 11 關：月夜小鎮的浣熊。

Boss「月夜怪盜 基德」：灰色麻糬身體、天生的黑色眼罩紋、條紋尾巴；扮成怪盜——高禮帽、深紫色披風、單片眼鏡，
揹著裝滿贓物的布袋（布袋另外畫，見 loot_bag）。拿電漿抓鉤槍偷畫面上的道具。
50%「卸甲」：抓鉤槍被打飛（道具見 grapple_head），慌張地變出分身、翻垃圾桶丟桶蓋、洗手手。
心情：m0 得意（單眼眨眼）、m1 生氣、m2 被打哭（高禮帽飛走）。
小怪：負鼠（自毀俯衝型，會先裝死）、垃圾桶砲台（地面固定型）；菁英怪：臭鼬。
"""
from common import *
from bosses import crying_eyes, squash
from scifi import GLOW, NAVY, AMBER, MAGENTA, lit

FUR = "#A3A9B4"
FUR_D = "#858C99"
WHITE = "#F6F4F8"
MASK = "#3A3442"
HAT = "#2E2A4A"
HAT_BAND = "#9B5DE5"
CAPE = "#4B2F6B"
CAPE_IN = "#E5484D"
GOLD = "#FFD15C"
SACK = "#D9C29A"
SACK_D = "#B89E72"
POSSUM = "#D8D2DA"
CAN = "#9AA6B4"
CAN_D = "#76828F"
SKUNK = "#3A3442"

VIEW = "-180 -175 360 350"
SIZE = (540, 525)


def top_hat(cx, cy, rot=0, s=1.0):
    out = path(f"M{-40 * s},{-80 * s} L{40 * s},{-80 * s} L{36 * s},0 L{-36 * s},0 Z", HAT, INK, 4.5)
    out += path(f"M{-38 * s},{-26 * s} L{38 * s},{-26 * s} L{37 * s},{-10 * s} L{-37 * s},{-10 * s} Z", HAT_BAND, INK, 3)
    out += ellipse(0, 2 * s, 62 * s, 12 * s, HAT, INK, 4.5)
    # 插在帽帶上的撲克牌（愛心）
    out += f'<g transform="translate({24 * s} {-30 * s}) rotate(18)">' + path(f"M{-9 * s},{-14 * s} L{9 * s},{-14 * s} L{9 * s},{14 * s} L{-9 * s},{14 * s} Z", "#FFFFFF", INK, 2.5) + heart(0, 0, 5 * s, "#E5484D", INK, 1.5) + "</g>"
    return f'<g transform="translate({cx} {cy}) rotate({rot})">{out}</g>'


def raccoon_boss(m, frame):
    b = ""
    # 披風（在身體後面）
    b += path("M-120,-10 Q-170,120 -150,170 L150,170 Q170,120 120,-10 Z", CAPE, INK, 4.5)
    b += path("M-140,160 Q0,130 140,160 L150,170 L-150,170 Z", CAPE_IN, "none")
    # 條紋尾巴
    tail = path("M100,120 Q190,110 170,30 Q164,0 140,8 Q156,60 96,96 Z", FUR, INK, 4)
    for k in range(3):
        tail += path(f"M{150 + k * 4},{24 + k * 26} q16,4 22,-4", "none", MASK, 9)
    b += tail
    # 耳朵
    for s in (-1, 1):
        b += ellipse(s * 86, -70, 30, 30, FUR, INK, 4.5) + ellipse(s * 86, -66, 16, 16, MASK, "none")
        b += path(f"M{s * 62},-86 Q{s * 86},-104 {s * 110},-86", "none", WHITE, 5)
    b += path(mochi_path(0, 36, 252, 228), FUR, INK, 5)
    b += ellipse(0, 112, 80, 40, FUR_D, "none", None, 'opacity="0.6"')
    ey = 6
    # 白色的眉毛區與口鼻，中間是黑色眼罩紋
    for s in (-1, 1):
        b += path(f"M{s * 14},{ey - 40} Q{s * 60},{ey - 56} {s * 104},{ey - 28} Q{s * 70},{ey - 30} {s * 14},{ey - 22} Z", WHITE, "none")
    b += path(f"M-112,{ey - 10} Q-60,{ey - 34} 0,{ey - 14} Q60,{ey - 34} 112,{ey - 10} Q110,{ey + 30} 70,{ey + 34} Q30,{ey + 30} 0,{ey + 16} Q-30,{ey + 30} -70,{ey + 34} Q-110,{ey + 30} -112,{ey - 10} Z", MASK, "none")
    b += ellipse(0, ey + 46, 50, 32, WHITE, "none")
    if m == 0:
        # 得意：右眼眨眼、左眼戴單片眼鏡
        b += ellipse(-50, ey + 4, 12, 13, "#FFFFFF", "none") + dot_eye(-50, ey + 5, 8)
        b += ellipse(-50, ey + 4, 20, 20, "none", GOLD, 4)
        b += path(f"M-32,{ey + 18} Q-20,{ey + 50} -24,{ey + 70}", "none", GOLD, 2.5)
        b += path(f"M38,{ey + 6} Q52,{ey - 4} 66,{ey + 6}", "none", "#FFFFFF", 5)
    elif m == 1:
        for s in (-1, 1):
            b += ellipse(s * 50, ey + 4, 13, 14, "#FFFFFF", "none") + dot_eye(s * 50, ey + 6, 8)
            b += path(f"M{s * 34},{ey + 2} L{s * 66},{ey - 12} L{s * 66},{ey - 16} L{s * 34},{ey - 16} Z", MASK, "none")
            b += path(f"M{s * 26},{ey - 26} L{s * 72},{ey - 40}", "none", None, 6)
        b += ellipse(-50, ey + 4, 20, 20, "none", GOLD, 4)
        b += anger_mark(112, -40, 18)
    else:
        for s in (-1, 1):
            b += path(f"M{s * 26},{ey - 30} Q{s * 46},{ey - 28} {s * 66},{ey - 18}", "none", None, 5)
            b += ellipse(s * 50, ey + 4, 15, 16, "#FFFFFF", "none")
        b += crying_eyes(-50, 50, ey + 5, 10)
        b += tears(-64, ey + 22, 50, -1) + tears(64, ey + 22, 50, 1)
    b += path(f"M-12,{ey + 30} Q0,{ey + 26} 12,{ey + 30} Q10,{ey + 42} 0,{ey + 44} Q-10,{ey + 42} -12,{ey + 30} Z", MASK, INK, 3)
    if m == 1:
        b += open_mouth(0, ey + 54, 14, 14)
    elif m == 2:
        b += wavy_mouth(0, ey + 60, 12)
    else:
        b += path(f"M-16,{ey + 54} Q0,{ey + 64} 18,{ey + 50}", "none", None, 4)
    b += blush(-92, ey + 42, 20) + blush(92, ey + 42, 20)
    # 披風的領結
    b += path("M-24,96 L0,108 L24,96 L24,120 L0,110 L-24,120 Z", CAPE_IN, INK, 3.5)
    b += ellipse(0, 108, 7, 7, GOLD, INK, 2.5)
    # 小手（右手拿抓鉤槍）
    b += ellipse(-110, 104, 22, 20, FUR, INK, 4) + ellipse(-110, 108, 12, 9, MASK, "none")
    b += ellipse(106, 96, 22, 20, FUR, INK, 4) + ellipse(106, 100, 12, 9, MASK, "none")
    for s in (-1, 1):
        b += ellipse(s * 58, 156, 36, 16, MASK, INK, 4)
    if m < 2:
        b += top_hat(-4, -84, -8 if m == 0 else 4)
    body = g(wob(b))
    if m == 2 and frame == 1:
        body += g(tears(-64, ey + 40, 34, -1) + tears(64, ey + 40, 34, 1))
    return Sprite(f"boss_raccoon_m{m}_{frame}", *SIZE, VIEW, squash(body, frame))


def lost_tophat():
    return Sprite("lost_tophat", 160, 140, "-66 -88 132 104", g(wob(top_hat(0, 0))))


def loot_bag(frame):
    """揹在背上的贓物布袋：鼓鼓的布袋綁著繩子，補丁上是肉球；frame 1 被打到時鼓起來（金幣露出來）。"""
    b = path("M-40,-30 Q-62,10 -48,44 Q0,66 48,44 Q62,10 40,-30 Z", SACK, INK, 4)
    b += path("M-40,-30 Q0,-20 40,-30 L30,-44 Q0,-36 -30,-44 Z", SACK_D, INK, 3.5)
    b += path("M-28,-36 Q0,-60 30,-38", "none", "#8D6E63", 5)
    b += paw(4, 12, 14, "#FFE9B0", INK, 2.5)
    if frame:
        for x, y in ((-24, -40), (0, -48), (22, -42)):
            b += ellipse(x, y, 9, 9, GOLD, INK, 2.5)
    return Sprite(f"min_lootbag_{frame}", 150, 150, "-66 -66 132 132", g(wob(b)))


def grapple_head():
    """怪盜的「電漿抓鉤槍」：鋼製槍管前端三爪抓鉤，爪尖洋紅色能量。朝 +x。"""
    b = path("M-4,-11 L54,-11 L54,11 L-4,11 Z", "url(#steel)", NAVY, 3)
    b += ellipse(60, 0, 12, 12, "#8A96B0", NAVY, 3)
    claws = ""
    for a in (-40, 0, 40):
        claws += f'<g transform="rotate({a} 60 0)"><path d="M66,0 Q92,-4 100,-16 Q98,0 86,8 Z" fill="#B8C2D0" stroke="{NAVY}" stroke-width="3"/></g>'
    b += claws
    b += lit(f'<circle cx="60" cy="0" r="5" fill="{MAGENTA}"/>', 2)
    return Sprite("prop_grapple_head", 230, 180, "-8 -45 115 90", b, GLOW)


def lost_grapple():
    h = path("M-90,-7 L-30,-7 L-30,7 L-90,7 Z", "url(#steel)", NAVY, 3)
    h += f'<g transform="translate(-34 0)">{grapple_head().body}</g>'
    return Sprite("lost_grapple", 300, 180, "-95 -45 190 90", h, GLOW)


# ───────────── 小怪 ─────────────

def mini(name, body, size=180, view="-60 -60 120 120"):
    return Sprite(name, size, size, view, g(wob(body)))


def opossum(frame):
    """負鼠：淺灰色、白臉粉紅鼻、細長尾巴；frame 1 裝死（四腳朝天、×× 眼）。自毀俯衝型。"""
    b = path("M24,26 Q52,44 46,64", "none", "#F2B8C6", 5)
    if frame:
        b += ellipse(0, 6, 34, 26, POSSUM, INK, 4)
        for x in (-20, -8, 8, 20):
            b += path(f"M{x},-16 L{x},-30", "none", "#F2B8C6", 6)
        for s in (-1, 1):
            b += path(f"M{s * 12 - 4},0 L{s * 12 + 4},8 M{s * 12 + 4},0 L{s * 12 - 4},8", "none", None, 2.5)
        b += path("M-6,16 Q0,12 6,16", "none", None, 2)
        b += path("M4,16 Q6,24 2,26", "none", "#FF8FB1", 3)
    else:
        for s in (-1, 1):
            b += ellipse(s * 22, -24, 10, 10, POSSUM, INK, 3) + ellipse(s * 22, -24, 5, 5, "#F2B8C6", "none")
        b += ellipse(0, 4, 32, 28, POSSUM, INK, 4)
        b += ellipse(0, -2, 22, 18, WHITE, "none")
        b += dot_eye(-10, -4, 4) + dot_eye(10, -4, 4)
        b += ellipse(0, 8, 5, 3.5, "#FF8FB1", INK, 2)
        b += blush(-20, 8, 6, lines=False) + blush(20, 8, 6, lines=False)
    return mini(f"min_possum_{frame}", b, 140, "-50 -44 100 104")


def trash_can(frame):
    """垃圾桶砲台：鐵皮垃圾桶，蓋子掀起來時（frame 1）小浣熊探出頭。地面固定型。"""
    b = path("M-34,-14 L34,-14 L28,44 L-28,44 Z", CAN, INK, 4)
    for k in (-1, 0, 1):
        b += path(f"M{k * 16},-8 L{k * 13},38", "none", CAN_D, 3)
    if frame:
        b += ellipse(0, -18, 22, 18, FUR, INK, 3.5)
        b += path("M-22,-20 Q0,-30 22,-20 L20,-14 Q0,-22 -20,-14 Z", MASK, "none")
        b += dot_eye(-8, -18, 3) + dot_eye(8, -18, 3)
        b += f'<g transform="rotate(-30 -36 -24)">' + ellipse(0, -30, 40, 8, CAN_D, INK, 3.5) + "</g>"
    else:
        b += ellipse(0, -16, 40, 9, CAN_D, INK, 3.5)
        b += ellipse(0, -24, 10, 5, CAN, INK, 2.5)
        b += dot_eye(-10, 14, 3.5) + dot_eye(10, 14, 3.5)
    return mini(f"min_trashcan_{frame}", b, 140, "-56 -76 112 124")


def elite_skunk(frame):
    """菁英怪：臭鼬。黑色身體、頭頂到尾巴一條白線，蓬蓬的大尾巴，戴紫色防毒面具；frame 1 尾巴翹起來噴氣。"""
    up = -30 if frame else 0
    b = f'<g transform="rotate({up} 40 50)">' + path("M30,60 Q100,60 96,-10 Q92,-50 60,-50 Q80,0 30,30 Z", SKUNK, INK, 4) + path("M70,-40 Q88,0 50,40", "none", WHITE, 10) + "</g>"
    b += path(mochi_path(0, 14, 124, 120), SKUNK, INK, 4.5)
    b += path("M0,-46 Q8,-10 0,20", "none", WHITE, 12)
    for s in (-1, 1):
        b += ellipse(s * 40, -40, 14, 13, SKUNK, INK, 3.5)
    b += ellipse(0, 22, 34, 22, WHITE, "none")
    b += dot_eye(-22, 0, 6) + dot_eye(22, 0, 6)
    b += path("M-34,-14 L-12,-8 M34,-14 L12,-8", "none", WHITE, 4)
    # 防毒面具
    b += ellipse(0, 26, 22, 16, "#9B5DE5", INK, 3.5)
    for s in (-1, 1):
        b += ellipse(s * 26, 32, 10, 10, "#6B3FA0", INK, 3)
    b += blush(-36, 12, 9, lines=False) + blush(36, 12, 9, lines=False)
    if frame:
        for x, y in ((80, -70), (104, -56), (92, -88)):
            b += ellipse(x, y, 10, 10, "#C9E07A", "none", None, 'opacity="0.8"')
    b += ellipse(-46, 60, 13, 10, SKUNK, INK, 3.5) + ellipse(46, 60, 13, 10, SKUNK, INK, 3.5)
    return mini(f"elite_skunk_{frame}", b, 240, "-90 -100 200 200")


def all_sprites():
    out = []
    for m in range(3):
        for f in range(2):
            out.append(raccoon_boss(m, f))
    out += [lost_tophat(), grapple_head(), lost_grapple()]
    for f in range(2):
        out += [loot_bag(f), opossum(f), trash_can(f), elite_skunk(f)]
    return out
