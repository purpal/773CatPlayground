"""道具徽章：主武器（圓形）、副武器（圓角方形）、補給品，以及消彈變成的糖果。"""
import math
from common import *

CANDY_COLORS = ["#FF8FB1", "#8FD3FF", "#FFE07A", "#B9A7FF", "#9BE8C4"]


def badge(name, fill, rim, icon, square=False):
    if square:
        frame = f'<rect x="-42" y="-42" width="84" height="84" rx="22" fill="{fill}" stroke="#FFFFFF" stroke-width="9"/>'
        frame += f'<rect x="-46" y="-46" width="92" height="92" rx="25" fill="none" stroke="{INK}" stroke-width="4"/>'
        frame += f'<rect x="-36" y="-36" width="72" height="72" rx="18" fill="none" stroke="{rim}" stroke-width="4"/>'
    else:
        frame = ellipse(0, 0, 42, 42, fill, "#FFFFFF", 9) + ellipse(0, 0, 46.5, 46.5, "none", INK, 4) + ellipse(0, 0, 36, 36, "none", rim, 4)
    shine = path("M-28,-18 Q-22,-32 -6,-34", "none", "#FFFFFF", 6, 'opacity="0.85"')
    return Sprite(name, 150, 150, "-50 -50 100 100", g(frame + g(wob(icon), width=3) + shine))


def spread_icon():
    out = ""
    for a in (-28, 0, 28):
        r = math.radians(a - 90)
        out += paw(math.cos(r) * 18, math.sin(r) * 18 + 10, 10, "#FFFFFF", INK, 2.5)
    return out


def focus_icon():
    out = f'<rect x="-10" y="-30" width="20" height="56" rx="10" fill="#FFFFFF" stroke="{INK}" stroke-width="3"/>'
    out += f'<rect x="-4" y="-28" width="8" height="52" rx="4" fill="#C9B8FF"/>'
    out += ellipse(0, 26, 16, 6, "none", INK, 3) + star(0, -30, 10, "#FFE07A", INK, 2.5)
    return out


def special_icon():
    return paw(0, 4, 24, "#FFFFFF", INK, 3) + star(22, -22, 9, "#FFE07A", INK, 2.5)


def homing_icon():
    out = path("M-26,0 C-14,-16 12,-16 22,0 C12,16 -14,16 -26,0 Z", "#FFFFFF", INK, 3)
    out += path("M-26,0 L-36,-12 L-36,12 Z", "#FFFFFF", INK, 3)
    out += ellipse(10, -3, 3, 3, INK, "none")
    out += path("M-10,22 Q4,30 18,22", "none", INK, 2.5, 'stroke-dasharray="4 4"')
    return f'<g transform="rotate(-40)">{out}</g>'


def sonic_icon():
    out = ""
    for k, r in enumerate((14, 24, 34)):
        out += path(f"M{-r * 0.8},{8 - k * 6} Q0,{-r - k * 4} {r * 0.8},{8 - k * 6}", "none", "#FFFFFF", 6 - k)
    out += ellipse(0, 18, 7, 7, "#FFFFFF", INK, 2.5)
    return out


def repair_icon():
    return heart(0, 0, 24, "#FFFFFF", INK, 3) + path("M-8,0 L8,0 M0,-8 L0,8", "none", "#FF5C93", 5)


def bomb_icon():
    return text(0, 14, "吼", 38, "#FFFFFF", INK, 4)


def coin():
    b = ellipse(0, 0, 40, 40, "#FFD15C", INK, 4) + ellipse(0, 0, 30, 30, "#FFE07A", "#E3A253", 3)
    b += paw(0, 2, 14, "#E3A253", "none", 0)
    b += path("M-24,-16 Q-18,-28 -4,-30", "none", "#FFFFFF", 5)
    return Sprite("item_coin", 120, 120, "-45 -45 90 90", g(wob(b)))


def candy(i):
    c = CANDY_COLORS[i]
    b = path("M-14,0 L-30,-12 L-28,12 Z", c, INK, 3) + path("M14,0 L30,-12 L28,12 Z", c, INK, 3)
    b += ellipse(0, 0, 17, 13, c, INK, 3.5)
    b += path("M-7,-9 Q1,0 -7,9", "none", "#FFFFFF", 3.5)
    return Sprite(f"candy_{i}", 96, 64, "-36 -24 72 48", g(wob(b)))


def all_sprites():
    out = [
        badge("item_spread", "#FF9EB8", "#FFD1DF", spread_icon()),
        badge("item_focus", "#8FC8FF", "#C9E6FF", focus_icon()),
        badge("item_special", "#FFC24D", "#FFE7A8", special_icon()),
        badge("item_homing", "#6FD6A8", "#C2F5D9", homing_icon(), square=True),
        badge("item_sonic", "#A894FF", "#D9CFFF", sonic_icon(), square=True),
        badge("item_repair", "#FF8FB1", "#FFD1DF", repair_icon()),
        badge("item_bomb", "#FF7A5C", "#FFC2B3", bomb_icon()),
        coin(),
    ]
    out += [candy(i) for i in range(len(CANDY_COLORS))]
    return out
