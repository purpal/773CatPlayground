"""小怪與菁英怪（正面，朝玩家）。frame 0/1 為兩格小動畫。"""
from common import *

MOUSE = "#EDE6F5"
MOUSE_D = "#D9CDE8"
PINK = "#FFCADA"
CHEESE = "#FFE07A"
CHEESE_HOLE = "#F2B84B"
SHIBA = "#F5B76A"
CREAM = "#FFF4E3"
BEAR = "#C9976B"
BEAR_D = "#A8784F"
MUZZLE = "#F5E1C8"
HONEY = "#FFB300"
BEE = "#FFD84D"
BEE_STRIPE = "#5A4148"


def sprite(name, body, size=180, view="-60 -60 120 120"):
    return Sprite(name, size, size, view, g(wob(body)))


def little_feet(y, frame, color, dx=18):
    off = 4 if frame else -4
    return ellipse(-dx, y + off, 11, 7, color, INK, 3.5) + ellipse(dx, y - off, 11, 7, color, INK, 3.5)


def mouse_face(cy, r=7, mood="happy"):
    out = ""
    if mood == "angry":
        out += path(f"M-24,{cy - 14} L-10,{cy - 8} M24,{cy - 14} L10,{cy - 8}", "none", None, 4)
    out += dot_eye(-16, cy, r) + dot_eye(16, cy, r)
    out += ellipse(0, cy + 10, 5, 4, "#FF9DB8", INK, 2.5)
    out += omega_mouth(0, cy + 17, 6, 3)
    out += blush(-30, cy + 12, 10, lines=False) + blush(30, cy + 12, 10, lines=False)
    return out


def mouse(frame):
    b = path("M10,30 C30,46 40,30 36,50", "none", INK, 7) + path("M10,30 C30,46 40,30 36,50", "none", PINK, 3)
    for s in (-1, 1):
        b += ellipse(s * 30, -26, 20, 19, MOUSE, INK, 4) + ellipse(s * 30, -25, 12, 11, PINK, "none")
    b += little_feet(40, frame, MOUSE)
    b += path(mochi_path(0, 6, 86, 76), MOUSE, INK, 4)
    # 頭上的小起司
    b += path("M-14,-28 L16,-40 L18,-24 Z", CHEESE, INK, 3) + ellipse(6, -30, 3, 3, CHEESE_HOLE, "none")
    b += mouse_face(4)
    return sprite(f"min_mouse_{frame}", b)


def plane(frame):
    """玩具飛機（朝右），駕駛艙裡坐著一隻小老鼠。"""
    b = path("M-56,-2 L-70,-26 L-52,-24 L-40,-6 Z", "#FFB3C7", INK, 3.5)
    b += path("M-60,4 C-60,-14 -30,-22 10,-22 C40,-22 58,-10 58,4 C58,18 40,24 10,24 C-30,24 -60,20 -60,4 Z", "#FF8FB1", INK, 4)
    b += path("M-6,8 L-20,44 L12,44 L20,8 Z", "#FFE07A", INK, 3.5)
    b += path("M-40,-6 C-20,-10 20,-10 40,-6", "none", "#FFD1DF", 4)
    b += ellipse(58, 2, 8, 8, "#F2F0F8", INK, 3)
    blade = (6, 34) if frame == 0 else (6, 14)
    b += ellipse(64, 2, blade[0], blade[1], "#FFFFFF", INK, 2.5, 'opacity="0.8"')
    # 駕駛
    for s in (-1, 1):
        b += ellipse(-4 + s * 14, -44, 10, 10, MOUSE, INK, 3) + ellipse(-4 + s * 14, -43, 6, 6, PINK, "none")
    b += ellipse(-4, -30, 22, 18, MOUSE, INK, 3.5)
    b += dot_eye(-12, -32, 4) + dot_eye(4, -32, 4) + ellipse(-4, -25, 3, 2.5, "#FF9DB8", INK, 2)
    b += path("M-24,-22 Q-4,-12 16,-22", "#B8E6FF", INK, 3, 'opacity="0.85"')
    return sprite(f"min_plane_{frame}", b, 210, "-80 -70 160 160")


def cheese_wheel():
    b = ellipse(0, 0, 54, 54, CHEESE, INK, 4) + ellipse(0, 0, 44, 44, "#FFEB9C", "none")
    for k in range(6):
        import math
        a = k * math.pi / 3
        b += ellipse(math.cos(a) * 32, math.sin(a) * 32, 7, 6, CHEESE_HOLE, INK, 2.5)
    b += path("M-40,-20 Q-30,-40 -6,-46", "none", "#FFFFFF", 5, 'opacity="0.7"')
    return sprite("min_cheese_wheel", b, 180, "-60 -60 120 120")


def cheese_pilot():
    b = ""
    for s in (-1, 1):
        b += ellipse(s * 16, -16, 11, 11, MOUSE, INK, 3) + ellipse(s * 16, -15, 7, 7, PINK, "none")
    b += ellipse(0, 2, 26, 22, MOUSE, INK, 3.5)
    b += dot_eye(-9, 0, 4.5) + dot_eye(9, 0, 4.5) + ellipse(0, 8, 3, 2.5, "#FF9DB8", INK, 2)
    b += path("M-20,-22 L20,-22 L18,-30 L-18,-30 Z", "#8FD3FF", INK, 3)
    return sprite("min_cheese_pilot", b, 120, "-40 -40 80 80")


def puppy(frame):
    b = ""
    for s in (-1, 1):
        rot = s * (10 if frame else -6)
        b += f'<g transform="rotate({rot} {s * 30} -30)">' + path(ear(s * 34, -54, s * 50, -16, s * 14, -26), SHIBA, INK, 4) + path(ear(s * 33, -44, s * 42, -20, s * 20, -26), PINK, "none") + "</g>"
    b += little_feet(40, frame, CREAM)
    b += path(mochi_path(0, 6, 92, 80), SHIBA, INK, 4)
    b += path("M-40,8 C-30,-4 -14,0 0,8 C14,0 30,-4 40,8 C40,38 20,46 0,46 C-20,46 -40,38 -40,8 Z", CREAM, "none")
    for s in (-1, 1):
        b += ellipse(s * 16, -12, 6, 4, CREAM, INK, 2)
    b += dot_eye(-16, 2, 6) + dot_eye(16, 2, 6)
    b += path("M-6,12 Q0,8 6,12 Q4,18 0,18 Q-4,18 -6,12 Z", "#3A2A30", INK, 2)
    b += omega_mouth(0, 22, 6, 3)
    b += blush(-30, 16, 10, lines=False) + blush(30, 16, 10, lines=False)
    # 嘴裡叼的小骨頭
    b += path("M-14,30 L14,30", "none", INK, 8) + path("M-14,30 L14,30", "none", "#FFF8EC", 4)
    for s in (-1, 1):
        b += ellipse(s * 16, 27, 4, 4, "#FFF8EC", INK, 2) + ellipse(s * 16, 33, 4, 4, "#FFF8EC", INK, 2)
    return sprite(f"min_puppy_{frame}", b)


def frisbee():
    b = ellipse(0, 0, 54, 54, "#8FD3FF", INK, 4) + ellipse(0, 0, 40, 40, "#BDE8FF", INK, 3)
    b += paw(0, 2, 20, "#FFFFFF", INK, 2.5)
    b += path("M-42,-18 Q-30,-42 -4,-48", "none", "#FFFFFF", 5, 'opacity="0.8"')
    return sprite("min_frisbee", b)


def turret():
    """網球發球機，前面趴著一隻小狗在操作。"""
    b = ellipse(0, 10, 54, 40, "#C2F5D9", INK, 4)
    b += path("M-54,10 Q0,40 54,10", "none", "#8FD9B4", 6)
    b += ellipse(0, -4, 30, 26, "#FFFFFF", INK, 3.5)
    for s in (-1, 1):
        b += path(ear(s * 30, 2, s * 42, 30, s * 18, 22), SHIBA, INK, 3)
    b += ellipse(0, 30, 26, 20, SHIBA, INK, 3.5)
    b += ellipse(0, 36, 16, 10, CREAM, "none")
    b += dot_eye(-10, 28, 4) + dot_eye(10, 28, 4) + ellipse(0, 36, 3.5, 2.5, "#3A2A30", "none")
    return sprite("min_turret", b, 180, "-60 -50 120 100")


def barrel():
    b = path("M0,-12 L58,-14 L58,14 L0,12 Z", "#F2F0F8", INK, 4)
    b += path("M8,-6 L50,-8", "none", "#FFFFFF", 3)
    b += ellipse(62, 0, 14, 14, "#D7F56A", INK, 3.5) + path("M54,-8 Q62,0 54,8 M70,-8 Q62,0 70,8", "none", "#FFFFFF", 2.5)
    # 以砲台中心為旋轉軸，所以左半邊留空
    return Sprite("min_barrel", 320, 60, "-80 -15 160 30", g(wob(b)))


def bee(frame):
    b = ""
    wing = (-26, -30) if frame == 0 else (-22, -18)
    for s in (-1, 1):
        b += ellipse(s * 24, wing[0], 18, 13, "#FFFFFF", INK, 3, f'opacity="0.85" transform="rotate({s * (30 if frame == 0 else 10)} {s * 24} {wing[0]})"')
    b += path("M-6,-30 Q-12,-46 -18,-48 M6,-30 Q12,-46 18,-48", "none", INK, 3) + ellipse(-18, -48, 4, 4, INK, "none") + ellipse(18, -48, 4, 4, INK, "none")
    b += ellipse(0, 4, 36, 32, BEE, INK, 4)
    b += path("M-34,8 Q0,16 34,8 M-30,22 Q0,30 30,22", "none", BEE_STRIPE, 6)
    b += path("M-6,34 L0,46 L6,34 Z", BEE_STRIPE, INK, 2.5)
    b += dot_eye(-12, -6, 5) + dot_eye(12, -6, 5) + omega_mouth(0, 4, 4, 2.5)
    b += blush(-22, 2, 7, lines=False) + blush(22, 2, 7, lines=False)
    return sprite(f"min_bee_{frame}", b, 150, "-50 -55 100 110")


def honeypot(frame):
    b = path("M-40,-30 C-56,-6 -54,34 -30,46 C-10,54 10,54 30,46 C54,34 56,-6 40,-30 Z", "#F2C46A", INK, 4)
    b += path("M-44,-36 L44,-36 L40,-24 L-40,-24 Z", "#E3A94A", INK, 3.5)
    b += path("M-32,-36 Q-30,-54 0,-54 Q30,-54 32,-36 Z", HONEY, INK, 3.5)
    drip = 18 if frame else 8
    b += path(f"M-20,-24 Q-18,{-10 + drip} -12,{-8 + drip} Q-6,{-10 + drip} -6,-24", HONEY, INK, 2.5)
    b += path(f"M16,-24 Q18,-6 24,-4 Q30,-6 30,-24", HONEY, INK, 2.5)
    b += dot_eye(-14, 12, 5.5) + dot_eye(14, 12, 5.5) + omega_mouth(0, 24, 6, 3)
    b += blush(-28, 22, 9, lines=False) + blush(28, 22, 9, lines=False)
    b += path("M-38,0 Q-40,20 -30,34", "none", "#FFE7A8", 5)
    return sprite(f"min_honeypot_{frame}", b)


def bearcub(frame):
    b = ""
    for s in (-1, 1):
        b += ellipse(s * 32, -30, 15, 14, BEAR, INK, 4) + ellipse(s * 32, -28, 8, 8, BEAR_D, "none")
    b += little_feet(42, frame, BEAR, 20)
    b += path(mochi_path(0, 8, 94, 82), BEAR, INK, 4)
    b += ellipse(0, 20, 22, 16, MUZZLE, INK, 3)
    b += ellipse(0, 12, 6, 4, "#4A3036", "none") + omega_mouth(0, 22, 6, 3)
    b += dot_eye(-18, 2, 5.5) + dot_eye(18, 2, 5.5)
    b += blush(-32, 16, 10, lines=False) + blush(32, 16, 10, lines=False)
    # 頭上的葉子帽
    b += path("M-4,-32 C-20,-50 10,-58 22,-44 C14,-38 6,-32 -4,-32 Z", "#9BE8C4", INK, 3) + path("M-2,-34 L16,-46", "none", "#5FB98F", 2.5)
    return sprite(f"min_bearcub_{frame}", b)


def beehive():
    b = path("M-30,-44 Q0,-56 30,-44 Q50,-20 46,10 Q40,44 0,48 Q-40,44 -46,10 Q-50,-20 -30,-44 Z", "#FFD15C", INK, 4)
    for y in (-26, -6, 14, 32):
        b += path(f"M-44,{y} Q0,{y + 10} 44,{y}", "none", "#E3A253", 4)
    b += ellipse(0, 4, 10, 9, "#7A4A2E", INK, 3)
    b += dot_eye(-16, -14, 4.5) + dot_eye(16, -14, 4.5)
    return sprite("min_beehive", b)


def balloon():
    """氣球在上半部，線往下垂到 y=60（道具掛在那裡）。"""
    b = path("M0,10 Q-8,30 4,44 Q12,56 0,66", "none", INK, 2.5)
    b += path("M0,-74 C34,-74 46,-46 40,-24 C34,-4 16,6 0,10 C-16,6 -34,-4 -40,-24 C-46,-46 -34,-74 0,-74 Z", "#FFCADA", INK, 4)
    b += path("M-6,10 L6,10 L4,16 L-4,16 Z", "#FF8FB1", INK, 2.5)
    b += path("M-26,-50 Q-20,-62 -6,-64", "none", "#FFFFFF", 6)
    return Sprite("min_balloon", 150, 240, "-50 -80 100 160", g(wob(b)))


# ───────────── 菁英怪 ─────────────

def elite_mouse(frame):
    """起司騎士：銀色頭盔＋羽毛、起司盾牌。"""
    b = ""
    b += little_feet(56, frame, MOUSE, 24)
    b += path(mochi_path(0, 14, 120, 104), MOUSE, INK, 4.5)
    # 頭盔
    b += path("M-56,-2 C-60,-52 -30,-70 0,-70 C30,-70 60,-52 56,-2 Q30,-12 0,-12 Q-30,-12 -56,-2 Z", "#E6E8F2", INK, 4.5)
    b += path("M-40,-40 Q-24,-58 0,-60", "none", "#FFFFFF", 6)
    for s in (-1, 1):
        b += ellipse(s * 46, -56, 16, 15, MOUSE, INK, 4) + ellipse(s * 46, -55, 9, 8, PINK, "none")
    plume = "M0,-68 C-6,-96 26,-104 34,-86 C20,-90 10,-82 8,-68 Z" if frame == 0 else "M0,-68 C-2,-98 32,-100 38,-82 C22,-88 12,-80 8,-68 Z"
    b += path(plume, "#FF8FB1", INK, 3.5)
    b += mouse_face(10, 7.5, "angry")
    # 起司盾
    b += path("M30,20 L76,10 L80,50 Q60,78 40,70 Q28,50 30,20 Z", "#FFE07A", INK, 4)
    for (x, y) in ((50, 30), (62, 50), (46, 54)):
        b += ellipse(x, y, 5, 4, CHEESE_HOLE, "none")
    # 小劍
    b += path("M-60,40 L-82,-10", "none", INK, 9) + path("M-60,40 L-82,-10", "none", "#F2F0F8", 5)
    b += path("M-70,30 L-54,22", "none", "#FFB300", 6)
    return sprite(f"elite_mouse_{frame}", b, 240, "-90 -110 180 180")


def elite_dog(frame):
    """牧羊犬教官：黑白邊境牧羊犬，戴教官帽、咬著哨子。"""
    b = ""
    for s in (-1, 1):
        rot = s * (12 if frame else 0)
        b += f'<g transform="rotate({rot} {s * 40} -40)">' + path(ear(s * 46, -72, s * 66, -24, s * 18, -38), "#3E3446", INK, 4) + "</g>"
    b += little_feet(56, frame, "#FFFFFF", 24)
    b += path(mochi_path(0, 12, 124, 108), "#3E3446", INK, 4.5)
    b += path("M-10,-40 C-6,-10 -40,10 -46,40 C-30,62 30,62 46,40 C40,10 6,-10 10,-40 Z", "#FFFFFF", "none")
    b += dot_eye(-22, 2, 7) + dot_eye(22, 2, 7)
    b += path("M-34,-12 L-12,-4 M34,-12 L12,-4", "none", "#FFFFFF", 5)
    b += path("M-8,14 Q0,10 8,14 Q6,20 0,22 Q-6,20 -8,14 Z", "#2E2433", INK, 2)
    # 哨子
    b += path("M2,30 L20,30 L22,40 L4,40 Z", "#FFD84D", INK, 3) + ellipse(24, 35, 6, 6, "#FFD84D", INK, 3)
    b += path("M14,40 Q34,60 50,46", "none", "#FF8FB1", 3)
    b += blush(-38, 18, 10, lines=False) + blush(38, 18, 10, lines=False)
    # 教官帽
    b += path("M-46,-46 Q-50,-78 0,-82 Q50,-78 46,-46 Z", "#8D9B6A", INK, 4)
    b += path("M-58,-44 Q0,-30 58,-44 Q0,-52 -58,-44 Z", "#6F7C50", INK, 3.5)
    b += star(0, -62, 10, "#FFE07A", INK, 2.5)
    return sprite(f"elite_dog_{frame}", b, 240, "-90 -100 180 180")


def elite_bee(frame):
    """蜂后：毛茸茸領子、王冠、權杖。"""
    b = ""
    wing = -20 if frame == 0 else -6
    for s in (-1, 1):
        b += ellipse(s * 46, wing - 20, 34, 22, "#FFFFFF", INK, 3.5, f'opacity="0.85" transform="rotate({s * 28} {s * 46} {wing - 20})"')
        b += ellipse(s * 40, wing + 10, 24, 16, "#FFFFFF", INK, 3, f'opacity="0.8" transform="rotate({s * 10} {s * 40} {wing + 10})"')
    b += ellipse(0, 28, 50, 46, BEE, INK, 4.5)
    b += path("M-48,30 Q0,42 48,30 M-42,52 Q0,64 42,52", "none", BEE_STRIPE, 8)
    b += path("M-8,72 L0,88 L8,72 Z", BEE_STRIPE, INK, 3)
    # 毛領
    for k in range(-3, 4):
        b += ellipse(k * 13, -6 + abs(k) * 2, 11, 10, "#FFF8EC", INK, 3)
    b += ellipse(0, -30, 38, 32, BEE, INK, 4)
    b += dot_eye(-14, -32, 6) + dot_eye(14, -32, 6)
    b += path("M-24,-46 L-8,-40 M24,-46 L8,-40", "none", None, 3.5)
    b += omega_mouth(0, -20, 6, 3) + blush(-24, -24, 8, lines=False) + blush(24, -24, 8, lines=False)
    b += f'<polygon points="-22,-58 -26,-80 -12,-68 0,-86 12,-68 26,-80 22,-58" fill="#FFE07A" stroke="{INK}" stroke-width="3.5" stroke-linejoin="round"/>'
    b += ellipse(0, -66, 4, 4, "#FF8FB1", INK, 2)
    # 權杖
    b += path("M60,60 L76,-8", "none", INK, 8) + path("M60,60 L76,-8", "none", "#E3A253", 4)
    b += star(78, -16, 14, "#FFE07A", INK, 3)
    return sprite(f"elite_bee_{frame}", b, 240, "-90 -100 180 200")


def all_sprites():
    out = []
    for f in range(2):
        out += [mouse(f), plane(f), puppy(f), bee(f), honeypot(f), bearcub(f), elite_mouse(f), elite_dog(f), elite_bee(f)]
    out += [cheese_wheel(), cheese_pilot(), frisbee(), turret(), barrel(), beehive(), balloon()]
    return out
