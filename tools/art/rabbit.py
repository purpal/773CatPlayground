"""第 5 關：胡蘿蔔田的兔子。

Boss「兔兔隊長 蘿蔔」：麻糬身體、長耳朵、耳朵上綁粉紅蝴蝶結、門牙露出來。
兔子看起來膽小溫和，但「兔子急了也會咬人」——打到 40% 才「裝甲」：戴上科技護目鏡、
裝上肩甲，拿出胡蘿蔔鑽頭（道具由遊戲另外畫，見 carrot_drill）。
心情：m0 得意（瞇眼、門牙）、m1 裝甲暴怒（護目鏡、耳朵豎直）、m2 被打哭（耳朵垂下、蝴蝶結掉了）。
小怪：跳跳小兔（自毀俯衝型）、胡蘿蔔砲台（地面固定型）；菁英怪：野兔隊長。
"""
from common import *
from bosses import brows, crying_eyes, squash, sweat

FUR = "#FFF9F2"
FUR_D = "#EFE1EC"
EAR_IN = "#FFB8CE"
NOSE = "#FF8FB1"
RIBBON = "#FF7FA6"
RIBBON_D = "#E85A86"
CARROT = "#FF8A2B"
CARROT_D = "#E26B12"
LEAF = "#6FD36B"
LEAF_D = "#45A84A"
SOIL = "#9C6B45"
SOIL_D = "#7A4F30"
HARE = "#C98B5B"
HARE_D = "#A86E44"
STEEL = "#5B6378"
CORAL = "#FF7A45"

VIEW = "-180 -175 360 350"
SIZE = (540, 525)


def bow(cx, cy, s=1.0, rot=0):
    out = path(f"M0,0 L{-30 * s},{-18 * s} Q{-38 * s},0 {-30 * s},{18 * s} Z", RIBBON, INK, 3.5)
    out += path(f"M0,0 L{30 * s},{-18 * s} Q{38 * s},0 {30 * s},{18 * s} Z", RIBBON, INK, 3.5)
    out += path(f"M{-4 * s},{4 * s} L{-12 * s},{26 * s} M{4 * s},{4 * s} L{12 * s},{26 * s}", "none", RIBBON_D, 5)
    out += ellipse(0, 0, 9 * s, 9 * s, "#FFB3C7", INK, 3)
    return f'<g transform="translate({cx} {cy}) rotate({rot})">{out}</g>'


def ear(cx, cy, rot, length=86, width=30):
    out = ellipse(cx, cy, width, length, FUR, INK, 5)
    out += ellipse(cx, cy + 8, width * 0.55, length * 0.78, EAR_IN, "none")
    return f'<g transform="rotate({rot} {cx} {cy + length * 0.8})">{out}</g>'


def rabbit_boss(m, frame):
    b = ""
    # 長耳朵：m0 一隻尖端微彎、m1 豎直張開、m2 垂到兩側
    if m == 0:
        b += ear(-54, -150, -10) + ear(54, -156, 14)
    elif m == 1:
        b += ear(-58, -158, -16, 92) + ear(58, -158, 16, 92)
    else:
        b += ear(-96, -104, -46, 78) + ear(96, -104, 46, 78)
    # 身體
    b += path(mochi_path(0, 34, 256, 216), FUR, INK, 5)
    b += path("M-110,80 C-80,140 80,140 110,80 C70,120 -70,120 -110,80 Z", FUR_D, "none", None, 'opacity="0.85"')
    b += ellipse(0, 112, 70, 34, "#FFFFFF", "none", None, 'opacity="0.8"')
    # 蝴蝶結（m2 被打掉了）
    if m < 2:
        b += bow(-60, -78, 1.1, -14)
    ey = 30
    if m == 0:
        # 得意：瞇瞇眼＋微挑眉
        b += brows("brave", -46, 46, ey - 26)
        for s in (-1, 1):
            b += path(f"M{s * 46 - 15},{ey + 2} Q{s * 46},{ey - 12} {s * 46 + 15},{ey + 2}", "none", None, 6)
        b += sweat(112, -8)
    elif m == 1:
        # 裝甲：科技護目鏡（鋼框＋暖色鏡片），耳朵根部也套上護甲環
        b += brows("angry", -46, 46, ey - 30, sw=6)
        b += path(f"M-128,{ey} L128,{ey}", "none", STEEL, 12)
        for s in (-1, 1):
            b += path(f"M{s * 18},{ey - 22} L{s * 84},{ey - 20} L{s * 80},{ey + 22} L{s * 22},{ey + 22} Z", STEEL, INK, 4)
            b += path(f"M{s * 26},{ey - 14} L{s * 76},{ey - 13} L{s * 73},{ey + 14} L{s * 29},{ey + 14} Z", CORAL, "none")
            b += path(f"M{s * 32},{ey - 7} L{s * 52},{ey - 8}", "none", "#FFE0CC", 4)
            b += path(f"M{s * 40},-100 L{s * 76},-104 L{s * 74},-84 L{s * 42},-80 Z", STEEL, INK, 3.5)
        b += anger_mark(104, -36, 18)
    else:
        b += brows("worried", -46, 46, ey - 26)
        b += crying_eyes(-46, 46, ey)
        b += tears(-60, ey + 14, 50, -1) + tears(60, ey + 14, 50, 1)
    # 鼻子、「人」字嘴、兩顆門牙
    b += path(f"M-8,{ey + 26} L8,{ey + 26} L0,{ey + 35} Z", NOSE, INK, 2.5)
    b += path(f"M0,{ey + 35} L0,{ey + 42} M-12,{ey + 47} Q-4,{ey + 48} 0,{ey + 42} Q4,{ey + 48} 12,{ey + 47}", "none", None, 3.5)
    if m < 2:
        teeth_h = 16 if m == 1 else 12
        b += path(f"M-9,{ey + 46} L-9,{ey + 46 + teeth_h} L0,{ey + 46 + teeth_h} L0,{ey + 44} Z M0,{ey + 44} L0,{ey + 46 + teeth_h} L9,{ey + 46 + teeth_h} L9,{ey + 46} Z",
                  "#FFFFFF", INK, 2.5)
    else:
        b += wavy_mouth(0, ey + 52, 12)
    blush_w = 26 if m == 1 else 22
    b += blush(-84, ey + 28, blush_w) + blush(84, ey + 28, blush_w)
    for s in (-1, 1):
        b += path(f"M{s * 44},{ey + 32} L{s * 92},{ey + 22} M{s * 44},{ey + 40} L{s * 90},{ey + 44}", "none", "#C9B8C8", 3)
    # 裝甲：肩甲
    if m == 1:
        for s in (-1, 1):
            b += path(f"M{s * 96},40 L{s * 150},28 L{s * 160},90 L{s * 110},98 Z", STEEL, INK, 4)
            b += path(f"M{s * 106},50 L{s * 144},42", "none", CORAL, 5)
    # 大大的兔腳＋前爪
    for s in (-1, 1):
        b += ellipse(s * 62, 146, 42, 18, FUR, INK, 4) + ellipse(s * 62, 148, 22, 9, EAR_IN, "none", None, 'opacity="0.7"')
    b += ellipse(-98, 96, 20, 18, FUR, INK, 4)
    b += ellipse(106, 88, 22, 20, FUR, INK, 4)  # 右手（拿胡蘿蔔鑽頭的位置）
    body = g(wob(b))
    if m == 2 and frame == 1:
        body += g(tears(-60, ey + 30, 32, -1) + tears(60, ey + 30, 32, 1))
    # 長耳朵很高：整隻縮小一點、往下移，耳朵才不會被裁掉（遊戲裡的手的位置跟著換算）
    body = f'<g transform="translate(0 30) scale(0.82)">{body}</g>'
    return Sprite(f"boss_rabbit_m{m}_{frame}", *SIZE, VIEW, squash(body, frame))


def lost_ribbon():
    return Sprite("lost_ribbon", 160, 120, "-45 -34 90 68", g(wob(bow(0, 0, 1.15))))


# ───────────── 小怪 ─────────────

def mini(name, body, size=180, view="-60 -60 120 120"):
    return Sprite(name, size, size, view, g(wob(body)))


def bunny(frame):
    """跳跳小兔：自毀俯衝型。frame 1 跳在空中（身體拉長、耳朵往後）。"""
    stretch = 1.12 if frame else 1.0
    b = ""
    tilt = 18 if frame else 6
    for s in (-1, 1):
        b += f'<g transform="rotate({s * tilt} {s * 14} -20)">' + ellipse(s * 14, -46, 10, 26, FUR, INK, 3.5) + ellipse(s * 14, -44, 5, 18, EAR_IN, "none") + "</g>"
    b += f'<g transform="scale(1 {stretch})">' + path(mochi_path(0, 6, 76, 66), FUR, INK, 4) + "</g>"
    b += dot_eye(-14, 2, 5) + dot_eye(14, 2, 5)
    b += path("M-4,12 L4,12 L0,16 Z", NOSE, INK, 2)
    b += path("M0,16 L0,20 M-6,22 Q-2,23 0,20 Q2,23 6,22", "none", None, 2.5)
    b += blush(-24, 14, 8, lines=False) + blush(24, 14, 8, lines=False)
    foot_y = 44 if frame else 38
    b += ellipse(-18, foot_y, 14, 7, FUR, INK, 3) + ellipse(18, foot_y, 14, 7, FUR, INK, 3)
    return mini(f"min_bunny_{frame}", b, 160, "-50 -80 100 130")


def carrot_turret():
    """胡蘿蔔砲台：從土堆裡冒出來的大胡蘿蔔（有小臉），葉子是瞄準器。地面固定型。"""
    b = ellipse(0, 30, 50, 18, SOIL, INK, 4) + ellipse(0, 26, 40, 10, SOIL_D, "none")
    b += path("M-24,26 C-28,-10 -18,-36 0,-40 C18,-36 28,-10 24,26 Z", CARROT, INK, 4)
    for y in (-18, -2, 12):
        b += path(f"M-20,{y} L-8,{y + 2} M8,{y - 2} L20,{y}", "none", CARROT_D, 3)
    for dx, rot in ((-10, -24), (0, 0), (10, 24)):
        b += f'<g transform="rotate({rot} {dx} -40)">' + path(f"M{dx},-40 C{dx - 8},-58 {dx - 4},-72 {dx},-76 C{dx + 4},-72 {dx + 8},-58 {dx},-40 Z", LEAF, INK, 3) + "</g>"
    b += dot_eye(-8, -10, 4) + dot_eye(8, -10, 4)
    b += path("M-5,0 Q0,4 5,0", "none", None, 2.5)
    b += blush(-16, -2, 6, lines=False) + blush(16, -2, 6, lines=False)
    return mini("min_carrot", b, 160, "-55 -85 110 125")


def elite_hare(frame):
    """菁英怪：野兔隊長。棕色野兔、額頭上推著飛行護目鏡、圍著紅圍巾。"""
    b = ""
    lean = 4 if frame else -4
    for s in (-1, 1):
        b += f'<g transform="rotate({s * 10 + lean} {s * 26} -52)">' + ellipse(s * 26, -96, 16, 46, HARE, INK, 4) + ellipse(s * 26, -92, 8, 34, "#F2C9A8", "none") + "</g>"
    b += path(mochi_path(0, 14, 130, 116), HARE, INK, 4.5)
    b += ellipse(0, 40, 40, 26, "#F5E3D0", "none")
    # 飛行護目鏡推在額頭
    b += path("M-54,-36 Q0,-50 54,-36", "none", "#6B4C3A", 7)
    for s in (-1, 1):
        b += ellipse(s * 20, -40, 16, 13, "#FFD15C", INK, 3.5) + ellipse(s * 20, -40, 9, 7, "#FFF3B8", "none")
    b += brows("brave", -22, 22, -12, w=10, sw=4)
    b += dot_eye(-22, 2, 6) + dot_eye(22, 2, 6)
    b += path("M-5,14 L5,14 L0,19 Z", NOSE, INK, 2)
    b += path("M0,19 L0,23 M-7,26 Q-2,27 0,23 Q2,27 7,26", "none", None, 3)
    b += path("M-5,26 L-5,33 L5,33 L5,26", "#FFFFFF", INK, 2)
    b += blush(-38, 16, 10) + blush(38, 16, 10)
    # 紅圍巾（飄動）
    flap = 6 if frame else -6
    b += path("M-46,48 Q0,62 46,48 L44,60 Q0,74 -44,60 Z", "#FF5C6E", INK, 3.5)
    b += path(f"M30,58 L58,{82 + flap} L44,{88 + flap} L22,64 Z", "#FF5C6E", INK, 3)
    b += ellipse(-30, 70, 16, 10, HARE, INK, 3) + ellipse(30, 70, 16, 10, HARE, INK, 3)
    return mini(f"elite_hare_{frame}", b, 240, "-90 -150 180 230")


def all_sprites():
    out = []
    for m in range(3):
        for f in range(2):
            out.append(rabbit_boss(m, f))
    out.append(lost_ribbon())
    for f in range(2):
        out += [bunny(f), elite_hare(f)]
    out.append(carrot_turret())
    return out
