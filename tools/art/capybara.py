"""第 4 關：溫泉河畔的卡皮巴拉（Capybara）。

設計依據：日本動物園讓卡皮巴拉泡溫泉（冬至還會泡柚子湯），Bandai「カピバラさん」悠哉、
愛泡溫泉、常放空的個性，以及 2022 年後「淡定走進畫面」迷因帶來的療癒、無所謂形象。
→ 麻糬土司形身體、面無表情的放空臉、頭上頂毛巾和柚子、坐在檜木桶裡泡湯。
Boss 在半血進入「裝甲」：淡定地戴上護目鏡、裝上肩甲，拿出柚子砲。
"""
from common import *

CAPY = "#A8744F"
CAPY_D = "#8A5B3B"
MUZZLE = "#C79A70"
NOSE = "#4E3022"
WOOD = "#E9C48E"
WOOD_D = "#C99A5E"
WATER = "#8EDBD4"
YUZU = "#FFC233"
YUZU_D = "#F2A516"
LEAF = "#7ED957"
TOWEL = "#FFFFFF"
STEEL = "#5B6378"
CORAL = "#FF7A45"

VIEW = "-180 -175 360 350"
SIZE = (540, 525)


def yuzu(cx, cy, r=26):
    out = ellipse(cx, cy, r, r * 0.9, YUZU, INK, 4)
    for dx, dy in ((-8, -6), (6, -10), (10, 4), (-4, 8), (-12, 4), (2, -2)):
        out += ellipse(cx + dx * r / 26, cy + dy * r / 26, 1.6, 1.6, YUZU_D, "none")
    out += path(f"M{cx - 2},{cy - r * 0.85} Q{cx + 8},{cy - r * 1.5} {cx + r * 0.9},{cy - r * 1.2} Q{cx + 10},{cy - r} {cx + 2},{cy - r * 0.8} Z", LEAF, INK, 3)
    out += path(f"M{cx - r * 0.5},{cy - r * 0.35} Q{cx - r * 0.3},{cy - r * 0.6} {cx},{cy - r * 0.62}", "none", "#FFE89A", 4)
    return out


def towel(cx, cy, w=90):
    out = path(f"M{cx - w / 2},{cy + 8} Q{cx - w / 2 - 6},{cy - 10} {cx - w / 2 + 10},{cy - 14} L{cx + w / 2 - 10},{cy - 14} Q{cx + w / 2 + 6},{cy - 10} {cx + w / 2},{cy + 8} Q{cx},{cy + 16} {cx - w / 2},{cy + 8} Z",
               TOWEL, INK, 4)
    out += path(f"M{cx - w / 2 + 6},{cy - 2} Q{cx},{cy + 6} {cx + w / 2 - 6},{cy - 2}", "none", "#8FC8FF", 5)
    return out


def steam(frame, x0=0, spread=1.0):
    out = ""
    for i, (x, y) in enumerate(((-110, -60), (120, -40), (-60, -120), (90, -130))):
        dy = -8 if (frame + i) % 2 else 0
        out += path(f"M{x0 + x * spread},{y + 30 + dy} Q{x0 + x * spread - 14},{y + 10 + dy} {x0 + x * spread},{y - 6 + dy} Q{x0 + x * spread + 14},{y - 22 + dy} {x0 + x * spread},{y - 40 + dy}",
                    "none", "#FFFFFF", 7, 'opacity="0.75"')
    return out


def capy_face(m, ey=-22, dx=0):
    """m0 放空、m1 裝甲（護目鏡）、m2 裝甲被打哭（鏡片裂開）。"""
    out = ""
    # 口鼻
    out += path(f"M{dx - 78},20 Q{dx - 80},-10 {dx - 46},-12 L{dx + 46},-12 Q{dx + 80},-10 {dx + 78},20 Q{dx + 76},70 {dx},74 Q{dx - 76},70 {dx - 78},20 Z", MUZZLE, INK, 4)
    out += path(f"M{dx - 38},6 Q{dx - 40},-8 {dx - 20},-8 L{dx + 20},-8 Q{dx + 40},-8 {dx + 38},6 Q{dx + 34},22 {dx},24 Q{dx - 34},22 {dx - 38},6 Z", NOSE, INK, 3)
    out += ellipse(dx - 18, 6, 7, 5, "#2E1C14", "none") + ellipse(dx + 18, 6, 7, 5, "#2E1C14", "none")
    out += path(f"M{dx - 26},-2 Q{dx - 14},-6 {dx - 6},-2", "none", "#8A6A5C", 3)
    if m == 2:
        out += wavy_mouth(dx, 44, 14)
    else:
        out += path(f"M{dx - 10},40 L{dx + 10},40", "none", None, 4)
    if m == 0:
        # 放空的半閉眼
        for s in (-1, 1):
            out += path(f"M{dx + s * 62},{ey} Q{dx + s * 50},{ey + 6} {dx + s * 38},{ey}", "none", None, 5)
            out += path(f"M{dx + s * 64},{ey - 4} L{dx + s * 36},{ey - 4}", "none", None, 3, 'opacity="0.6"')
    else:
        # 科技護目鏡
        out += path(f"M{dx - 96},{ey - 18} L{dx + 96},{ey - 18} L{dx + 90},{ey + 16} L{dx - 90},{ey + 16} Z", STEEL, INK, 4)
        out += path(f"M{dx - 84},{ey - 10} L{dx + 84},{ey - 10} L{dx + 80},{ey + 9} L{dx - 80},{ey + 9} Z", CORAL, "none")
        out += path(f"M{dx - 70},{ey - 6} L{dx - 20},{ey - 6}", "none", "#FFE0CC", 4)
        if m == 2:
            out += path(f"M{dx + 20},{ey - 10} L{dx + 30},{ey + 2} L{dx + 22},{ey + 9} M{dx + 30},{ey + 2} L{dx + 46},{ey - 6}", "none", "#FFFFFF", 2.5)
            out += tears(dx - 56, ey + 16, 40, -1) + tears(dx + 56, ey + 16, 40, 1)
    out += blush(dx - 98, 26, 20) + blush(dx + 98, 26, 20)
    return out


def boss_capy(m, frame):
    b = steam(frame)
    # 檜木桶後緣＋水面
    b += ellipse(0, 108, 170, 40, WOOD_D, INK, 5)
    b += ellipse(0, 108, 154, 30, WATER, "none")
    # 小耳朵
    for s in (-1, 1):
        b += ellipse(s * 100, -112, 20, 16, CAPY, INK, 4) + ellipse(s * 100, -110, 10, 8, CAPY_D, "none")
    # 土司形身體（頭身一體）
    b += path("M-132,-70 Q-132,-130 -60,-134 L60,-134 Q132,-130 132,-70 L136,90 L-136,90 Z", CAPY, INK, 5)
    b += path("M-112,-110 Q-60,-126 0,-124", "none", "#C08A62", 8, 'opacity="0.7"')
    b += capy_face(m)
    # 裝甲：肩甲
    if m >= 1:
        for s in (-1, 1):
            b += path(f"M{s * 108},40 L{s * 150},30 L{s * 160},86 L{s * 112},92 Z", STEEL, INK, 4)
            b += path(f"M{s * 118},48 L{s * 146},42", "none", CORAL, 5)
    # 頭上的毛巾＋柚子（被打哭時柚子掉了）
    b += towel(-6, -136, 96)
    if m < 2:
        b += yuzu(4, -162, 26)
    # 桶子前緣
    b += path("M-170,108 Q0,156 170,108 L164,150 Q0,192 -164,150 Z", WOOD, INK, 5)
    for x in (-110, -55, 0, 55, 110):
        b += path(f"M{x},{126 + abs(x) * -0.05} L{x},{168 - abs(x) * 0.12}", "none", WOOD_D, 4)
    b += path("M-166,128 Q0,174 166,128", "none", "#8A6A40", 5)
    # 前腳搭在桶邊
    for s in (-1, 1):
        b += ellipse(s * 70, 112, 26, 15, CAPY, INK, 4)
    body = f'<g transform="translate(0 16) scale(0.93)">{g(wob(b))}</g>'
    if frame == 1:
        body = f'<g transform="translate(0 4) scale(1.02 0.98)">{body}</g>'
    return Sprite(f"boss_capy_m{m}_{frame}", *SIZE, VIEW, body)


def lost_yuzu():
    return Sprite("lost_yuzu", 120, 120, "-40 -46 80 80", g(wob(yuzu(0, 0, 26))))


# ───────────── 小怪 ─────────────

def mini(name, body, size=180, view="-60 -60 120 120"):
    return Sprite(name, size, size, view, g(wob(body)))


def baby_capy(frame):
    b = ellipse(0, 30, 50, 16, "#FF8FB1", INK, 4) + ellipse(0, 30, 34, 9, WATER, "none")  # 游泳圈
    for s in (-1, 1):
        b += ellipse(s * 30, -38, 9, 7, CAPY, INK, 3)
    b += path("M-40,-20 Q-40,-46 -16,-46 L16,-46 Q40,-46 40,-20 L42,26 L-42,26 Z", CAPY, INK, 4)
    b += path("M-28,4 Q-28,-8 -14,-8 L14,-8 Q28,-8 28,4 Q26,22 0,22 Q-26,22 -28,4 Z", MUZZLE, INK, 3)
    b += path("M-12,0 Q-12,-6 -4,-6 L4,-6 Q12,-6 12,0 Q10,8 0,8 Q-10,8 -12,0 Z", NOSE, "none")
    b += path("M-24,-24 Q-18,-20 -12,-24 M12,-24 Q18,-20 24,-24", "none", None, 3.5)
    b += blush(-32, 4, 8, lines=False) + blush(32, 4, 8, lines=False)
    b += path("M-60,36 Q-50,30 -40,36 M40,36 Q50,30 60,36", "none", "#BDE8FF", 4) if frame else path("M-58,38 Q-48,34 -38,38 M38,38 Q48,34 58,38", "none", "#BDE8FF", 4)
    b += yuzu(14, -54, 10)
    return mini(f"min_babycapy_{frame}", b)


def duck(frame):
    b = ""
    wing = -6 if frame else 4
    for s in (-1, 1):
        b += ellipse(s * 34, 8 + wing, 14, 20, "#FFE07A", INK, 3.5, f'transform="rotate({s * 30} {s * 34} {8 + wing})"')
    b += ellipse(0, 14, 34, 28, "#FFE07A", INK, 4)
    b += ellipse(0, -20, 26, 24, "#FFE07A", INK, 4)
    b += path("M-12,-10 Q0,-2 12,-10 Q8,0 0,2 Q-8,0 -12,-10 Z", "#FF9A3C", INK, 3)
    b += dot_eye(-10, -24, 4.5) + dot_eye(10, -24, 4.5)
    b += blush(-20, -12, 7, lines=False) + blush(20, -12, 7, lines=False)
    b += towel(0, -44, 34)
    return mini(f"min_duck_{frame}", b, 150, "-50 -60 100 100")


def yuzu_drone():
    b = yuzu(0, 4, 40)
    b += dot_eye(-14, 6, 5) + dot_eye(14, 6, 5) + omega_mouth(0, 18, 6, 3)
    b += blush(-26, 16, 8, lines=False) + blush(26, 16, 8, lines=False)
    return mini("min_yuzu", b, 150, "-50 -50 100 100")


def bucket():
    """檜木水桶，裡面躲著一隻小青蛙。"""
    b = ellipse(0, -10, 44, 14, WOOD_D, INK, 4) + ellipse(0, -10, 36, 9, WATER, "none")
    b += ellipse(-6, -18, 16, 11, "#9BE8C4", INK, 3) + dot_eye(-12, -22, 3.5) + dot_eye(0, -22, 3.5)
    b += path("M-44,-10 L-36,40 Q0,52 36,40 L44,-10 Q0,4 -44,-10 Z", WOOD, INK, 4)
    for x in (-22, 0, 22):
        b += path(f"M{x},{-4 + abs(x) * 0.1} L{x * 0.85},{44}", "none", WOOD_D, 3)
    b += path("M-42,6 Q0,20 42,6 M-38,30 Q0,42 38,30", "none", "#8A6A40", 4)
    return mini("min_bucket", b, 150, "-50 -50 100 100")


def monkey(frame):
    """菁英怪：溫泉猴老大（雪猴），紅臉、頭頂毛巾。"""
    b = ""
    for s in (-1, 1):
        arm = -10 if frame else 0
        b += ellipse(s * 62, 30 + arm, 18, 26, "#C9C2C8", INK, 4)
    b += path(mochi_path(0, 14, 124, 112), "#D9D2D8", INK, 4.5)
    for s in (-1, 1):
        b += ellipse(s * 52, -18, 12, 14, "#F2A0A0", INK, 3)
    b += path("M-34,-20 Q-34,-44 0,-44 Q34,-44 34,-20 Q36,18 0,22 Q-36,18 -34,-20 Z", "#F07C7C", INK, 4)
    b += path("M-24,-30 L-8,-24 M24,-30 L8,-24", "none", None, 4)
    b += dot_eye(-14, -14, 5) + dot_eye(14, -14, 5)
    b += path("M-8,4 Q0,0 8,4", "none", None, 3) + ellipse(-4, -4, 2, 2, INK, "none") + ellipse(4, -4, 2, 2, INK, "none")
    b += towel(0, -52, 70)
    b += ellipse(0, 66, 60, 12, WATER, INK, 3, 'opacity="0.9"')
    return mini(f"elite_monkey_{frame}", b, 240, "-90 -90 180 180")


def all_sprites():
    out = []
    for m in range(3):
        for f in range(2):
            out.append(boss_capy(m, f))
    out.append(lost_yuzu())
    for f in range(2):
        out += [baby_capy(f), duck(f), monkey(f)]
    out += [yuzu_drone(), bucket()]
    return out
