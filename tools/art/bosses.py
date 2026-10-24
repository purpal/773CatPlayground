"""Boss：每隻 3 種心情（m0 逞強、m1 生氣、m2 被打哭＋道具掉了）× 2 格，另外輸出道具與掉落物。

道具（叉子、骨頭、蜂蜜棒）不畫在本體上，由遊戲依攻擊狀態另外畫（拿在手上或伸出去攻擊）。
"""
from common import *

CHEESE = "#FFE07A"
CHEESE_D = "#FFD15C"
CHEESE_HOLE = "#F2B84B"
FUR = "#F7F2FB"
FUR_SHADE = "#E6DCF0"
PINK = "#FFCADA"
TEAR = "#BDE8FF"
SWEAT = "#A8DDFF"

VIEW = "-180 -175 360 350"
SIZE = (540, 525)


def sweat(cx, cy, s=1.0):
    return path(f"M{cx},{cy - 16 * s} Q{cx + 11 * s},{cy} {cx},{cy + 8 * s} Q{cx - 11 * s},{cy} {cx},{cy - 16 * s} Z", SWEAT, INK, 3)


def brows(kind, lx, rx, y, w=16, sw=5):
    """kind: brave（逞強：微微皺眉）、angry（倒八）、worried（困擾眉）。"""
    out = ""
    for s, x in ((-1, lx), (1, rx)):
        if kind == "angry":
            out += path(f"M{x - s * w},{y - 8} L{x + s * w * 0.9},{y + 6}", "none", None, sw + 1)
        elif kind == "worried":
            # 內側高、外側低：八字困擾眉
            out += path(f"M{x - s * w},{y - 10} Q{x},{y - 6} {x + s * w},{y + 4}", "none", None, sw)
        else:
            out += path(f"M{x - s * w},{y - 4} Q{x},{y - 8} {x + s * w * 0.9},{y + 3}", "none", None, sw)
    return out


def crying_eyes(lx, rx, y, r=11):
    out = ""
    for x in (lx, rx):
        out += ellipse(x, y, r * 1.15, r * 1.3, "#2E2128", "none")
        out += ellipse(x + r * 0.35, y - r * 0.45, r * 0.5, r * 0.5, "#FFFFFF", "none")
        out += ellipse(x - r * 0.4, y + r * 0.35, r * 0.25, r * 0.25, "#FFFFFF", "none")
        out += path(f"M{x - r * 1.3},{y + r * 0.6} Q{x},{y + r * 1.4} {x + r * 1.3},{y + r * 0.6}", "url(#tearG)", "#7FC4E8", 2.5, 'opacity="0.9"')
    return out


def squash(body, frame):
    if frame == 0:
        return body
    return f'<g transform="translate(0 5) scale(1.03 0.97)">{body}</g>'


def paper_crown(cx, cy, rot, s=1.0):
    d = (f"M{-34 * s},{14 * s} L{-38 * s},{-16 * s} L{-20 * s},{-2 * s} L{-6 * s},{-24 * s} L{8 * s},{-2 * s} "
         f"L{26 * s},{-20 * s} L{36 * s},{14 * s} Z")
    out = path(d, "#FFF6D6", INK, 4)
    out += path(f"M{-34 * s},{6 * s} L{35 * s},{6 * s}", "none", "#FFB3C7", 4)
    out += ellipse(-6 * s, -6 * s, 4 * s, 4 * s, "#FF8FB1", INK, 2) + ellipse(-24 * s, 0, 3 * s, 3 * s, "#8FD3FF", INK, 2)
    out += ellipse(20 * s, -2 * s, 3 * s, 3 * s, "#B9A7FF", INK, 2)
    return f'<g transform="translate({cx} {cy}) rotate({rot})">{out}</g>'


# ───────────── 第 1 關：小起司（起司鼠王） ─────────────

def mouse_boss(m, frame):
    b = ""
    # 尾巴（在身後捲起來）
    tail = "M100,110 C140,124 164,96 152,70 C142,48 158,30 170,40"
    b += path(tail, "none", INK, 11) + path(tail, "none", PINK, 5)
    # 耳朵（從起司帽的洞伸出來）
    ear_y = -92 if m < 2 else -84
    for s in (-1, 1):
        ex = s * 78
        rot = s * (8 if m < 2 else 26)
        b += f'<g transform="rotate({rot} {ex} {ear_y})">' + ellipse(ex, ear_y, 44, 42, FUR, INK, 5) + ellipse(ex, ear_y + 3, 28, 27, PINK, "none") + "</g>"
    # 身體
    b += path(mochi_path(0, 34, 256, 216), FUR, INK, 5)
    b += path("M-110,80 C-80,140 80,140 110,80 C70,120 -70,120 -110,80 Z", FUR_SHADE, "none", None, 'opacity="0.8"')
    # 起司帽（m2 被打歪）
    hood = ("M-124,18 C-132,-62 -70,-114 0,-114 C70,-114 132,-62 124,18 "
            "Q104,2 86,20 Q64,2 42,18 Q20,4 0,18 Q-20,4 -42,18 Q-64,2 -86,20 Q-104,2 -124,18 Z")
    hood_g = path(hood, CHEESE, INK, 5)
    hood_g += path("M-110,-20 C-100,-70 -50,-100 0,-102", "none", "#FFF3B8", 8, 'opacity="0.8"')
    for (hx, hy, r) in ((-40, -60, 13), (30, -78, 9), (60, -30, 15), (-80, -10, 9), (8, -30, 7)):
        hood_g += ellipse(hx, hy, r, r * 0.85, CHEESE_HOLE, INK, 3)
    # 帽子上讓耳朵伸出來的洞
    for s in (-1, 1):
        hood_g += path(f"M{s * 52},-82 Q{s * 78},-104 {s * 104},-78", "none", CHEESE_D, 7)
    if m == 2:
        hood_g = f'<g transform="rotate(-9 0 -40) translate(-6 6)">{hood_g}</g>'
    b += hood_g
    # 王冠（m2 已被打掉）
    if m < 2:
        b += paper_crown(26, -122, 14, 1.15)
    # 臉
    ey = 44
    if m == 0:
        b += brows("brave", -42, 42, ey - 26)
        b += dot_eye(-42, ey, 11) + dot_eye(42, ey, 11)
        b += omega_mouth(0, ey + 30, 10)
        b += sweat(108, -10)
    elif m == 1:
        b += brows("angry", -42, 42, ey - 24, sw=6)
        b += dot_eye(-42, ey, 11) + dot_eye(42, ey, 11)
        b += open_mouth(0, ey + 26, 18, 22, fangs=True)
        b += anger_mark(96, -30, 18)
    else:
        b += brows("worried", -42, 42, ey - 24)
        b += crying_eyes(-42, 42, ey)
        b += tears(-56, ey + 14, 48, -1) + tears(56, ey + 14, 48, 1)
        b += wavy_mouth(0, ey + 34, 14)
    b += ellipse(0, ey + 16, 8, 6, "#FF9DB8", INK, 3)
    blush_w = 24 if m == 1 else 20
    b += blush(-80, ey + 24, blush_w) + blush(80, ey + 24, blush_w)
    for s in (-1, 1):
        b += path(f"M{s * 60},{ey + 18} L{s * 100},{ey + 10} M{s * 60},{ey + 26} L{s * 98},{ey + 32}", "none", "#B8A6BE", 3)
    # 手腳
    for s in (-1, 1):
        b += ellipse(s * 50, 140, 26, 14, FUR, INK, 4)
    if m == 2:
        # 舉著「手下留情」牌子
        sign = path("M-6,104 L-6,150", "none", "#B88A5A", 9)
        sign += f'<rect x="-74" y="96" width="148" height="56" rx="10" fill="#FFF8EC" stroke="{INK}" stroke-width="4" transform="rotate(-5 0 124)"/>'
        sign += text(0, 124, "手下留情…", 21, INK, "none", 0, rot=-5)
        sign += text(0, 145, "起司分你一半", 14, "#B48A95", "none", 0, rot=-5)
        b += sign
        for s in (-1, 1):
            b += ellipse(s * 70, 118, 18, 16, FUR, INK, 4)
    else:
        b += ellipse(-96, 104, 20, 18, FUR, INK, 4)
        # 右手握拳（拿叉子的位置）
        b += ellipse(104, 92, 22, 20, FUR, INK, 4)
    body = g(wob(b))
    if m == 2 and frame == 1:
        body += g(tears(-56, ey + 30, 30, -1) + tears(56, ey + 30, 30, 1))
    return Sprite(f"boss_mouse_m{m}_{frame}", *SIZE, VIEW, squash(body, frame))


# ───────────── 第 2 關：柴犬隊長 ─────────────

SHIBA = "#F5B76A"
SHIBA_D = "#E39A4C"
CREAM = "#FFF4E3"
NAVY = "#5B6FB5"


def captain_hat(cx, cy, rot=0, s=1.0):
    out = path(f"M{-76 * s},{6 * s} Q{-78 * s},{-56 * s} 0,{-62 * s} Q{78 * s},{-56 * s} {76 * s},{6 * s} Z", NAVY, INK, 5)
    out += path(f"M{-74 * s},{-2 * s} L{74 * s},{-2 * s}", "none", "#FFD15C", 7)
    out += path(f"M{-84 * s},{6 * s} Q0,{30 * s} {84 * s},{6 * s} Q0,{16 * s} {-84 * s},{6 * s} Z", "#3A3F6E", INK, 4)
    out += star(0, -30 * s, 15 * s, "#FFE07A", INK, 3)
    out += path(f"M{-50 * s},{-40 * s} Q{-30 * s},{-52 * s} {-6 * s},{-54 * s}", "none", "#8FA0E0", 5, 'opacity="0.8"')
    return f'<g transform="translate({cx} {cy}) rotate({rot})">{out}</g>'


def dog_boss(m, frame):
    b = ""
    # 捲捲尾巴
    b += path("M100,70 C150,60 160,10 130,-4 C104,-16 92,14 112,26 C126,34 136,18 128,10", SHIBA, INK, 5)
    b += path("M118,8 C126,16 120,22 114,18", "none", CREAM, 5)
    # 耳朵（m2 垂下來）
    for s in (-1, 1):
        if m < 2:
            b += path(ear(s * 76, -132, s * 116, -54, s * 30, -84), SHIBA, INK, 5)
            b += path(ear(s * 74, -112, s * 100, -62, s * 44, -82), PINK, "none")
        else:
            b += path(f"M{s * 40},-80 Q{s * 120},-96 {s * 132},-40 Q{s * 100},-40 {s * 86},-58 Z", SHIBA, INK, 5)
            b += path(f"M{s * 56},-76 Q{s * 108},-84 {s * 116},-50 Q{s * 98},-52 {s * 88},-62 Z", PINK, "none")
    b += path(mochi_path(0, 30, 262, 222), SHIBA, INK, 5)
    # 柴犬的白色臉頰與下巴
    b += path("M-118,30 C-110,0 -70,-6 -46,14 C-30,26 -10,28 0,26 C10,28 30,26 46,14 C70,-6 110,0 118,30 "
              "C118,110 70,140 0,140 C-70,140 -118,110 -118,30 Z", CREAM, "none")
    # 紅色領巾
    b += path("M-70,112 Q0,136 70,112 L40,150 Q0,168 -40,150 Z", "#FF8FB1", INK, 4)
    for (x, y) in ((-30, 132), (0, 146), (30, 132)):
        b += ellipse(x, y, 4, 4, "#FFFFFF", "none")
    # 麻呂眉
    for s in (-1, 1):
        b += ellipse(s * 44, -14 + (6 if m == 1 else 0), 12, 8, CREAM, INK, 3,
                     f'transform="rotate({s * (24 if m == 1 else -10 if m == 2 else 0)} {s * 44} -14)"')
    ey = 18
    if m == 0:
        b += arc_eye(-44, ey, 13, up=True, sw=5.5) + arc_eye(44, ey, 13, up=True, sw=5.5)
        b += omega_mouth(0, ey + 40, 13)
        b += path(f"M-8,{ey + 46} Q0,{ey + 68} 8,{ey + 46}", "#FF8FB1", INK, 3)
        b += sweat(-112, -30, 0.9)
    elif m == 1:
        b += dot_eye(-44, ey, 11) + dot_eye(44, ey, 11)
        b += open_mouth(0, ey + 36, 24, 28, fangs=True)
        b += anger_mark(100, -40, 18)
    else:
        b += crying_eyes(-44, 44, ey)
        b += tears(-58, ey + 14, 56, -1) + tears(58, ey + 14, 56, 1)
        b += wavy_mouth(0, ey + 48, 16)
    b += path(f"M-14,{ey + 22} Q0,{ey + 14} 14,{ey + 22} Q10,{ey + 34} 0,{ey + 36} Q-10,{ey + 34} -14,{ey + 22} Z", "#3A2A30", INK, 2)
    b += ellipse(-4, ey + 22, 4, 2.5, "#FFFFFF", "none")
    b += blush(-84, ey + 34, 20) + blush(84, ey + 34, 20)
    # 手腳
    for s in (-1, 1):
        b += ellipse(s * 56, 156, 28, 14, CREAM, INK, 4)
    b += ellipse(-110, 96, 22, 20, CREAM, INK, 4)
    b += ellipse(112, 86, 22, 20, CREAM, INK, 4)
    if m < 2:
        b += captain_hat(0, -92, -6 if m == 0 else 4)
    body = g(wob(b))
    if m == 2 and frame == 1:
        body += g(tears(-58, ey + 34, 34, -1) + tears(58, ey + 34, 34, 1))
    return Sprite(f"boss_dog_m{m}_{frame}", *SIZE, VIEW, squash(body, frame))


# ───────────── 第 3 關：熊熊大王 ─────────────

BEAR = "#C9976B"
BEAR_D = "#A8784F"
MUZZLE = "#F5E1C8"
HONEY = "#FFB300"
POT = "#F2C46A"


def honey_pot(cx, cy, rot=0, s=1.0, upside_down=True):
    out = path(f"M{-84 * s},{20 * s} C{-100 * s},{-50 * s} {-60 * s},{-96 * s} 0,{-96 * s} C{60 * s},{-96 * s} {100 * s},{-50 * s} {84 * s},{20 * s} Z", POT, INK, 5)
    out += path(f"M{-92 * s},{14 * s} Q0,{34 * s} {92 * s},{14 * s} L{92 * s},{30 * s} Q0,{50 * s} {-92 * s},{30 * s} Z", "#E3A94A", INK, 4)
    out += path(f"M{-60 * s},{-60 * s} Q{-40 * s},{-84 * s} {-6 * s},{-86 * s}", "none", "#FFE7A8", 8, 'opacity="0.9"')
    out += f'<rect x="{-36 * s}" y="{-60 * s}" width="{72 * s}" height="{44 * s}" rx="{8 * s}" fill="#FFF8EC" stroke="{INK}" stroke-width="3"/>'
    out += text(0, -26 * s, "蜜", 30 * s, "#C9762A", "none", 0)
    # 蜂蜜從罐口流下來
    out += path(f"M{-60 * s},{34 * s} Q{-56 * s},{60 * s} {-48 * s},{62 * s} Q{-40 * s},{60 * s} {-38 * s},{38 * s} "
                f"M{20 * s},{40 * s} Q{24 * s},{78 * s} {34 * s},{80 * s} Q{44 * s},{76 * s} {42 * s},{38 * s}", HONEY, INK, 3)
    t = f"translate({cx} {cy}) rotate({rot})" + ("" if upside_down else " scale(1 -1)")
    return f'<g transform="{t}">{out}</g>'


def bear_boss(m, frame):
    b = ""
    ear_y = -70 if m < 2 else -100
    ear_x = 112 if m < 2 else 86
    for s in (-1, 1):
        b += ellipse(s * ear_x, ear_y, 36, 34, BEAR, INK, 5) + ellipse(s * ear_x, ear_y + 4, 20, 19, BEAR_D, "none")
    b += path(mochi_path(0, 30, 280, 236), BEAR, INK, 5)
    b += path("M-120,80 C-90,150 90,150 120,80 C80,130 -80,130 -120,80 Z", BEAR_D, "none", None, 'opacity="0.55"')
    # 口鼻
    b += ellipse(0, 70, 60, 44, MUZZLE, INK, 4)
    b += path("M-18,46 Q0,36 18,46 Q14,62 0,64 Q-14,62 -18,46 Z", "#4A3036", INK, 2)
    b += ellipse(-5, 46, 5, 3, "#FFFFFF", "none")
    ey = 22
    if m == 0:
        # 睏睏的得意臉，舔著嘴邊的蜂蜜
        b += path(f"M-62,{ey} Q-46,{ey + 8} -30,{ey}", "none", None, 5.5) + path(f"M30,{ey} Q46,{ey + 8} 62,{ey}", "none", None, 5.5)
        b += path(f"M-66,{ey - 8} L-28,{ey - 8} M28,{ey - 8} L66,{ey - 8}", "none", None, 4)
        b += omega_mouth(0, 80, 14)
        b += path("M14,82 Q30,98 22,108 Q12,106 12,90 Z", "#FF8FB1", INK, 3)
        b += path("M26,84 Q34,92 32,102", "none", HONEY, 5)
    elif m == 1:
        b += brows("angry", -46, 46, ey - 22, sw=6)
        b += dot_eye(-46, ey, 11) + dot_eye(46, ey, 11)
        b += open_mouth(0, 76, 28, 34, fangs=True)
        b += anger_mark(110, -10, 20)
    else:
        b += brows("worried", -46, 46, ey - 22)
        b += crying_eyes(-46, 46, ey)
        b += tears(-60, ey + 16, 60, -1) + tears(60, ey + 16, 60, 1)
        b += wavy_mouth(0, 88, 16)
        # 頭上腫一個包＋轉圈圈的星星
        b += ellipse(-30, -96, 26, 20, BEAR, INK, 4) + path("M-44,-100 Q-30,-112 -16,-100", "none", "#E9BC92", 4)
        for (x, y, r) in ((-80, -128, 13), (10, -138, 11), (60, -120, 9)):
            b += star(x, y, r)
    b += blush(-96, ey + 30, 22) + blush(96, ey + 30, 22)
    for s in (-1, 1):
        b += ellipse(s * 62, 160, 32, 16, BEAR, INK, 4) + ellipse(s * 62, 160, 12, 7, MUZZLE, "none")
    b += ellipse(-124, 100, 24, 22, BEAR, INK, 4)
    b += ellipse(124, 92, 24, 22, BEAR, INK, 4)
    if m < 2:
        b += honey_pot(0, -78, -4 if m == 0 else 6, 1.05)
    body = g(wob(b))
    if m == 2 and frame == 1:
        body += g(tears(-60, ey + 36, 36, -1) + tears(60, ey + 36, 36, 1))
    return Sprite(f"boss_bear_m{m}_{frame}", *SIZE, VIEW, squash(body, frame))


# ───────────── 道具（攻擊時另外畫） ─────────────

def fork_head():
    """叉子的叉頭，朝 +x；x=0 接握柄、x=130 為尖端。"""
    b = path("M0,-12 L30,-12 Q40,-12 44,-28 L44,28 Q40,12 30,12 L0,12 Z", "#F2F0F8", INK, 4)
    b += path("M44,-32 L56,-32 Q60,-32 60,-28 L60,28 Q60,32 56,32 L44,32 Z", "#F2F0F8", INK, 4)
    for y in (-24, 0, 24):
        b += path(f"M60,{y - 6} L118,{y - 5} Q128,{y} 118,{y + 5} L60,{y + 6} Z", "#F2F0F8", INK, 4)
    b += path("M64,-26 L112,-26 M64,-2 L112,-2 M64,22 L112,22", "none", "#FFFFFF", 3)
    # 粉紅蝴蝶結
    b += path("M8,0 L-12,-18 L-14,18 Z M8,0 L26,-18 L28,18 Z", "#FF8FB1", INK, 3) + ellipse(8, 0, 7, 7, "#FFB3C7", INK, 3)
    return Sprite("prop_fork_head", 280, 140, "-10 -35 140 70", g(wob(b), width=4))


def bone_end():
    """骨頭的一端（兩顆圓球），朝 +x；x=0 接骨幹。"""
    b = ellipse(36, -20, 24, 22, "#FFF8EC", INK, 4) + ellipse(36, 20, 24, 22, "#FFF8EC", INK, 4)
    b += path("M0,-14 L34,-14 L34,14 L0,14 Z", "#FFF8EC", "none")
    b += path("M0,-14 L22,-14 M0,14 L22,14", "none", INK, 4)
    b += path("M26,-30 Q34,-38 44,-34", "none", "#FFFFFF", 4)
    return Sprite("prop_bone_end", 140, 200, "-5 -50 70 100", g(wob(b), width=4))


def dipper_head():
    """蜂蜜棒的頭（一圈圈的木球沾滿蜂蜜），朝 +x。"""
    b = ellipse(56, 0, 52, 38, "#E3A253", INK, 4)
    for x in (24, 44, 64, 84):
        b += path(f"M{x},-34 Q{x + 8},0 {x},34", "none", "#B87333", 5)
    b += path("M10,-26 C40,-50 80,-46 104,-22 C110,-8 96,6 92,20 Q86,46 76,46 Q66,44 70,24 Q60,30 50,40 Q40,52 32,40 Q28,28 20,22 Q4,10 10,-26 Z",
              HONEY, INK, 3, 'opacity="0.95"')
    b += path("M30,-28 Q52,-40 78,-30", "none", "#FFE7A8", 6)
    return Sprite("prop_dipper_head", 240, 200, "-10 -50 120 100", g(wob(b), width=4))


def lost_items():
    out = []
    out.append(Sprite("lost_crown", 200, 140, "-50 -35 100 70", g(wob(paper_crown(0, 0, 0, 1.2)))))
    out.append(Sprite("lost_hat", 240, 160, "-95 -70 190 110", g(wob(captain_hat(0, 10)))))
    out.append(Sprite("lost_pot", 260, 260, "-110 -110 220 220", g(wob(honey_pot(0, 0, 180, 1.0)))))
    return out


def all_sprites():
    out = []
    for fn in (mouse_boss, dog_boss, bear_boss):
        for m in range(3):
            for f in range(2):
                out.append(fn(m, f))
    # 叉子、骨頭、蜂蜜棒的頭改由 scifi.py 輸出（科技版）
    out += lost_items()
    return out
