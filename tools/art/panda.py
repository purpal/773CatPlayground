"""第 9 關：翠綠竹林的熊貓。

Boss「竹林武館 熊貓師父 波波」：黑白麻糬身體、往下垂的八字黑眼圈，穿紅色唐裝（大襟＋金色盤扣）、
戴黑色小圓帽，下巴用粉紅膠帶黏著一大把白色假鬍子——其實是裝成大人的熊貓寶寶。
40%「卸甲」：電漿竹節棍被打飛（道具見 bamboo_staff），變回滾來滾去的寶寶。
心情：m0 逞強（瞇眼摸鬍子）、m1 生氣（鬍子翹起來）、m2 被打哭（假鬍子被打飛，下巴只剩膠帶）。
"""
from common import *
from bosses import brows, crying_eyes, squash
from scifi import GLOW, NAVY, AMBER, CORAL, lit

FUR = "#FBF8F4"
FUR_SHADE = "#ECE6EE"
BLACK = "#3D3546"
BLACK_HI = "#5A5066"
SUIT = "#F57C73"
SUIT_D = "#D95F5A"
GOLD = "#FFD15C"
CAP = "#4A3F5C"
BEARD = "#FFFFFF"
BEARD_LINE = "#D9D4E0"
TAPE = "#FFCADA"
BAMBOO = "#8BC97A"
BAMBOO_D = "#5E9E55"

VIEW = "-180 -175 360 350"
SIZE = (540, 525)
BODY = mochi_path(0, 34, 270, 250)


def eye_patch(cx, cy, side):
    """往下垂的八字黑眼圈：內側高、外側低。"""
    return ellipse(cx, cy, 30, 40, BLACK, "none", None, f'transform="rotate({side * -32} {cx} {cy})"')


def skullcap(cx, cy, rot=0):
    """黑色小圓帽（瓜皮帽）：六片拼接、金色帽緣、頂上紅色帽結。"""
    out = path("M-64,6 C-62,-44 62,-44 64,6 Q0,-8 -64,6 Z", CAP, INK, 4.5)
    for x in (-30, 0, 30):
        out += path(f"M{x * 0.4},-33 Q{x},-16 {x * 1.2},{-2 if x else -4}", "none", "#665A7A", 3)
    out += path("M-66,4 Q0,-10 66,4 L66,16 Q0,2 -66,16 Z", GOLD, INK, 4)
    out += ellipse(0, -38, 11, 10, SUIT, INK, 3.5)
    return f'<g transform="translate({cx} {cy}) rotate({rot})">{out}</g>'


def beard(cx, cy, flip_up=False, s=1.0):
    """假鬍子：一大把波浪白鬍子，兩邊用粉紅膠帶黏著。flip_up 時整把甩起來（生氣）。"""
    d = ("M-46,0 Q-56,30 -38,48 Q-50,72 -24,82 Q-24,102 0,112 Q24,102 24,82 "
         "Q50,72 38,48 Q56,30 46,0 Q0,14 -46,0 Z")
    out = path(d, BEARD, INK, 4)
    for x0, x1, y1 in ((-26, -16, 70), (0, 0, 98), (26, 16, 70)):
        out += path(f"M{x0},12 Q{x0 - 8},40 {x1},{y1}", "none", BEARD_LINE, 3.5)
    if flip_up:
        out = f'<g transform="rotate(-28)">{out}</g>'
        for k in range(3):
            out += path(f"M{58 + k * 8},{40 + k * 16} l14,-4", "none", INK, 3)
    for side in (-1, 1):
        out += f'<rect x="-10" y="-6" width="20" height="12" rx="2" fill="{TAPE}" stroke="{INK}" stroke-width="2.5" transform="translate({side * 42} 4) rotate({side * 35})"/>'
    return f'<g transform="translate({cx} {cy}) scale({s})">{out}</g>'


def tang_suit():
    """紅色唐裝：立領＋大襟（往右斜的衣襟）＋金色盤扣；用身體輪廓裁切。"""
    out = path("M-160,80 Q0,116 160,80 L160,200 L-160,200 Z", SUIT, "none")
    out += path("M-160,152 Q0,176 160,152 L160,200 L-160,200 Z", SUIT_D, "none", None, 'opacity="0.35"')
    out += path("M-62,84 Q0,104 62,84", "none", INK, 4)
    out += path("M-58,96 Q0,116 58,96", "none", GOLD, 5)
    out += path("M30,102 Q78,108 100,150", "none", INK, 4)
    for x, y in ((56, 108), (80, 122), (95, 142)):
        out += path(f"M{x - 10},{y} L{x + 10},{y}", "none", GOLD, 5)
        out += ellipse(x + 12, y, 4.5, 4.5, GOLD, INK, 2)
    return f'<clipPath id="pandaBody"><path d="{BODY}"/></clipPath><g clip-path="url(#pandaBody)">{out}</g>'


def sleeve_paw(cx, cy, rot=0):
    out = path("M-26,-24 L26,-24 L30,12 L-30,12 Z", SUIT, INK, 4)
    out += path("M-30,6 L30,6", "none", GOLD, 5)
    out += ellipse(0, 24, 24, 20, BLACK, INK, 4)
    return f'<g transform="translate({cx} {cy}) rotate({rot})">{out}</g>'


def panda_boss(m, frame):
    b = ""
    for s in (-1, 1):
        b += ellipse(s * 96, -64, 38, 36, BLACK, INK, 4.5)
        b += ellipse(s * 96, -60, 20, 18, BLACK_HI, "none")
    b += path(BODY, FUR, INK, 5)
    b += ellipse(0, 112, 96, 40, FUR_SHADE, "none", None, 'opacity="0.8"')
    b += tang_suit()
    b += path(BODY, "none", INK, 5)
    ey = 4
    for s in (-1, 1):
        b += eye_patch(s * 52, ey + 6, s)
    if m == 0:
        # 逞強：得意瞇眼（白色弧線才看得到）
        b += brows("brave", -52, 52, ey - 44)
        for s in (-1, 1):
            b += path(f"M{s * 52 - 14},{ey + 4} Q{s * 52},{ey - 8} {s * 52 + 14},{ey + 4}", "none", "#FFFFFF", 6)
    elif m == 1:
        # 生氣：倒八眉（內側低），眼睛上半被斜斜的眼皮蓋住
        for s in (-1, 1):
            b += path(f"M{s * 30},{ey - 32} L{s * 72},{ey - 48}", "none", None, 7)
            b += ellipse(s * 50, ey + 4, 13, 14, "#FFFFFF", "none") + dot_eye(s * 50, ey + 6, 8)
            b += path(f"M{s * 34},{ey + 2} L{s * 66},{ey - 12} L{s * 66},{ey - 16} L{s * 34},{ey - 16} Z", BLACK, "none")
        b += anger_mark(112, -36, 18)
    else:
        b += brows("worried", -52, 52, ey - 40)
        for s in (-1, 1):
            b += ellipse(s * 50, ey + 4, 15, 16, "#FFFFFF", "none")
        b += crying_eyes(-50, 50, ey + 5, 10)
        b += tears(-64, ey + 22, 50, -1) + tears(64, ey + 22, 50, 1)
    # 口鼻
    b += ellipse(0, 52, 42, 30, "#FFFFFF", "none")
    b += path("M-13,36 Q0,32 13,36 Q12,48 0,52 Q-12,48 -13,36 Z", BLACK, INK, 3)
    b += ellipse(-4, 38, 4, 2.5, BLACK_HI, "none")
    if m == 1:
        b += open_mouth(0, 58, 14, 14)
    elif m == 2:
        b += wavy_mouth(0, 64, 12)
    else:
        b += omega_mouth(0, 60, 10)
    b += blush(-92, ey + 40, 22) + blush(92, ey + 40, 22)
    # 鬍子：哭了之後被打飛，下巴只剩兩條膠帶
    if m < 2:
        b += beard(0, 72, flip_up=(m == 1), s=0.9)
    else:
        for s in (-1, 1):
            b += f'<rect x="-9" y="-5" width="18" height="10" rx="2" fill="{TAPE}" stroke="{INK}" stroke-width="2.5" transform="translate({s * 30} 80) rotate({s * 35})"/>'
    # 手：右手（畫面右側）拿竹節棍；逞強時左手摸鬍子
    if m == 0:
        b += sleeve_paw(-62, 126, 35)
    else:
        b += sleeve_paw(-112, 92, 10)
    b += sleeve_paw(110, 86, -10)
    for s in (-1, 1):
        b += ellipse(s * 60, 154, 40, 20, BLACK, INK, 4)
        b += ellipse(s * 60, 156, 16, 9, BLACK_HI, "none")
    b += skullcap(4 if m == 2 else 0, -88, 10 if m == 2 else -4)
    body = g(wob(b))
    if m == 2 and frame == 1:
        body += g(tears(-64, ey + 40, 34, -1) + tears(64, ey + 40, 34, 1))
    return Sprite(f"boss_panda_m{m}_{frame}", *SIZE, VIEW, squash(body, frame))


def lost_beard():
    return Sprite("lost_beard", 200, 213, "-62 -16 124 132", g(wob(beard(0, 0))))


def bamboo_staff():
    """熊貓師父的「電漿竹節棍」：鋼製握柄＋一節節竹子，竹節環發出琥珀色的光。朝 +x。"""
    b = path("M-4,-9 L22,-9 L22,9 L-4,9 Z", "url(#steel)", NAVY, 3)
    rings = ""
    for k in range(4):
        x = 22 + k * 30
        b += path(f"M{x},-10 L{x + 30},-10 L{x + 30},10 L{x},10 Z", BAMBOO, NAVY, 3)
        b += path(f"M{x + 4},-5 L{x + 26},-5", "none", "#C3EBB6", 3)
        rings += f'<path d="M{x + 30},-13 L{x + 30},13" stroke="{AMBER}" stroke-width="5" fill="none"/>'
    b += path("M142,-12 L156,0 L142,12 Z", CORAL, NAVY, 3)
    b += lit(rings, 3)
    return Sprite("prop_bamboo_head", 330, 70, "-8 -17 165 34", b, GLOW)


def all_sprites():
    out = []
    for m in range(3):
        for f in range(2):
            out.append(panda_boss(m, f))
    out += [lost_beard(), bamboo_staff()]
    return out
