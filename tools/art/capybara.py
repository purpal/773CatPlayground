"""第 4 關：溫泉河畔的卡皮巴拉（Capybara）。

設計依據：日本動物園讓卡皮巴拉泡溫泉（冬至還會泡柚子湯；遊戲裡頭上頂的是橘子），Bandai「カピバラさん」悠哉、
愛泡溫泉、常放空的個性，以及 2022 年後「淡定走進畫面」迷因帶來的療癒、無所謂形象。
→ 造型 v4（參考卡皮巴拉玩偶與「卡皮巴拉小黃豚」，見 docs/art/capybara-v4-concept.png）：
頭身一體的豆子形、一大塊深色方口鼻（「)(」鼻孔＋「人」字嘴）、很小的瞇瞇眼、頭頂兩側深色小圓耳，
頭上頂一顆橘子、坐在檜木桶裡泡湯。
Boss 在半血進入「裝甲」：淡定地戴上護目鏡、裝上肩甲，拿出橘子砲。
"""
from common import *

CAPY = "#D39A5B"
CAPY_D = "#B47B41"
CAPY_L = "#E6B77E"
MUZZLE = "#8E5A33"
MUZZLE_D = "#5E3A22"
WOOD = "#E9C48E"
WOOD_D = "#C99A5E"
WATER = "#8EDBD4"
# 頭上的是橘子（沿用 yuzu 這個名字，貼圖名稱不變）
YUZU = "#FF9A2E"
YUZU_D = "#E5761A"
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
    out += path(f"M{cx - r * 0.5},{cy - r * 0.35} Q{cx - r * 0.3},{cy - r * 0.6} {cx},{cy - r * 0.62}", "none", "#FFD3A0", 4)
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


def capy_body(m, arms=True):
    """正面、頭身一體的卡皮巴拉（v4），中心 (0,0)，頭頂約 y=-184、屁股約 y=134。
    m0 放空、m1 裝甲（護目鏡）、m2 裝甲被打哭（鏡片裂開）。"""
    b = ""
    # 頭頂兩側角落的深色小圓耳
    for s in (-1, 1):
        b += ellipse(s * 118, -158, 22, 20, MUZZLE, INK, 4) + ellipse(s * 116, -154, 10, 9, MUZZLE_D, "none")
    # 豆子形：圓角方頭直接接到圓圓的身體（沒有脖子）
    b += path("M-138,-110 C-140,-160 -100,-182 -40,-184 L40,-184 C100,-182 140,-160 138,-110 "
              "C136,-60 150,-10 156,40 C162,100 110,132 0,134 C-110,132 -162,100 -156,40 C-150,-10 -136,-60 -138,-110 Z", CAPY, INK, 5)
    b += path("M-110,-160 C-60,-180 60,-180 110,-160 C60,-170 -60,-170 -110,-160 Z", CAPY_D, "none", None, 'opacity="0.6"')
    b += ellipse(0, 70, 90, 56, CAPY_L, "none", None, 'opacity="0.7"')
    # 一大塊深色方口鼻：眼睛下方蓋到下巴
    b += path("M-56,-118 C-24,-128 24,-128 56,-118 C80,-110 84,-76 82,-48 C80,-18 56,-4 0,-4 C-56,-4 -80,-18 -82,-48 C-84,-76 -80,-110 -56,-118 Z",
              MUZZLE, INK, 4)
    # 鼻孔「)(」＋「人」字嘴
    b += path("M-24,-106 Q-12,-98 -18,-86 M24,-106 Q12,-98 18,-86", "none", MUZZLE_D, 5)
    b += path("M0,-82 L0,-50", "none", MUZZLE_D, 4)
    if m == 2:
        b += path("M-20,-30 Q-10,-46 0,-50 Q10,-46 20,-30", "none", MUZZLE_D, 4)
    else:
        b += path("M-22,-36 Q-8,-38 0,-50 Q8,-38 22,-36", "none", MUZZLE_D, 4)
    ey = -128
    if m == 0:
        # 很小的瞇瞇眼，長在頭的上側、離得很開
        for s in (-1, 1):
            b += path(f"M{s * 84},{ey} L{s * 112},{ey - 2}", "none", None, 5)
    else:
        # 科技潛水護目鏡：鋼框＋暖色鏡片
        b += path(f"M-140,{ey} L140,{ey}", "none", STEEL, 12)
        for s in (-1, 1):
            b += path(f"M{s * 66},{ey - 24} L{s * 132},{ey - 22} L{s * 128},{ey + 22} L{s * 70},{ey + 22} Z", STEEL, INK, 4)
            b += path(f"M{s * 74},{ey - 16} L{s * 124},{ey - 15} L{s * 121},{ey + 14} L{s * 77},{ey + 14} Z", CORAL, "none")
            b += path(f"M{s * 80},{ey - 9} L{s * 100},{ey - 10}", "none", "#FFE0CC", 4)
        b += path(f"M-66,{ey - 4} Q0,{ey - 16} 66,{ey - 4}", "none", STEEL, 6)
        if m == 2:
            b += path(f"M86,{ey - 15} L98,{ey} L88,{ey + 13} M98,{ey} L118,{ey - 8}", "none", "#FFFFFF", 2.5)
            b += tears(-100, ey + 22, 50, -1) + tears(100, ey + 22, 50, 1)
    b += blush(-112, -72, 22) + blush(112, -72, 22)
    for s in ((-1, 1) if arms else ()):
        b += path(f"M{s * 138},10 C{s * 150},28 {s * 140},48 {s * 116},46 C{s * 104},38 {s * 112},18 {s * 126},10 Z", CAPY, INK, 4)
        b += ellipse(s * 122, 40, 12, 9, MUZZLE, "none")
    return b


def boss_capy(m, frame):
    b = ellipse(0, 72, 220, 46, WOOD_D, INK, 5) + ellipse(0, 72, 202, 34, WATER, "none")
    b += capy_body(m, arms=False)
    if m < 2:
        b += yuzu(0, -206, 28)
    if m >= 1:
        # 裝甲：肩甲
        for s in (-1, 1):
            b += path(f"M{s * 100},-30 L{s * 160},-40 L{s * 168},20 L{s * 112},22 Z", STEEL, INK, 4)
            b += path(f"M{s * 112},-20 L{s * 154},-28", "none", CORAL, 5)
    b += path("M-220,72 Q-190,62 -160,72 T-100,72 T-40,72 T20,72 T80,72 T140,72 T200,72 T230,72", "none", "#6CC5BE", 6)
    # 桶子前緣
    b += path("M-226,76 Q0,140 226,76 L216,140 Q0,206 -216,140 Z", WOOD, INK, 5)
    for x in (-140, -70, 0, 70, 140):
        b += path(f"M{x},{100 + abs(x) * 0.02} L{x},{170 - abs(x) * 0.12}", "none", WOOD_D, 4)
    b += path("M-222,104 Q0,170 222,104", "none", "#8A6A40", 5)
    # 小手搭在桶邊
    for s in (-1, 1):
        b += ellipse(s * 92, 84, 26, 15, CAPY, INK, 4) + ellipse(s * 92, 88, 12, 6, MUZZLE, "none")
    if m == 2:
        b += yuzu(170, 96, 22)  # 被打哭：橘子滾到水裡
    body = f'<g transform="translate(0 6) scale(0.72)">{g(wob(b))}</g>'
    if frame == 1:
        body = f'<g transform="translate(0 4) scale(1.02 0.98)">{body}</g>'
    body = g(steam(frame, spread=1.1)) + body
    return Sprite(f"boss_capy_m{m}_{frame}", *SIZE, VIEW, body)


def lost_yuzu():
    return Sprite("lost_yuzu", 120, 120, "-40 -46 80 80", g(wob(yuzu(0, 0, 26))))


# ───────────── 小怪 ─────────────

def mini(name, body, size=180, view="-60 -60 120 120"):
    return Sprite(name, size, size, view, g(wob(body)))


def baby_capy(frame):
    b = ellipse(0, 30, 50, 16, "#FF8FB1", INK, 4) + ellipse(0, 30, 34, 9, WATER, "none")  # 游泳圈
    for s in (-1, 1):
        b += ellipse(s * 32, -40, 8, 7, MUZZLE, INK, 3)
    b += path("M-38,-26 C-38,-44 -26,-48 -12,-48 L12,-48 C26,-48 38,-44 38,-26 C38,-10 42,10 42,26 L-42,26 C-42,10 -38,-10 -38,-26 Z", CAPY, INK, 4)
    b += path("M-16,-30 C-6,-33 6,-33 16,-30 C23,-27 24,-16 23,-8 C22,0 16,4 0,4 C-16,4 -22,0 -23,-8 C-24,-16 -23,-27 -16,-30 Z", MUZZLE, INK, 3)
    b += path("M-7,-27 Q-3,-24 -5,-20 M7,-27 Q3,-24 5,-20 M0,-18 L0,-10 M-6,-6 Q-2,-7 0,-10 Q2,-7 6,-6", "none", MUZZLE_D, 2.5)
    b += path("M-31,-34 L-23,-35 M31,-34 L23,-35", "none", None, 3.5)
    b += blush(-30, -14, 8, lines=False) + blush(30, -14, 8, lines=False)
    b += path("M-60,36 Q-50,30 -40,36 M40,36 Q50,30 60,36", "none", "#BDE8FF", 4) if frame else path("M-58,38 Q-48,34 -38,38 M38,38 Q48,34 58,38", "none", "#BDE8FF", 4)
    b += yuzu(0, -56, 10)
    return mini(f"min_babycapy_{frame}", b, view="-64 -72 128 128")


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
