"""最終關（瑞士）：萌王 鼠鼠的道具。

Boss 本體沿用起司鼠王的圖（bosses.py）；這裡畫萌王的瑞士刀、被打飛的瑞士刀、老鼠夾，以及萌王覺醒時搬出來的起司火鍋。
"""
from common import *
from scifi import GLOW, NAVY, AMBER, MAGENTA, lit

RED = "#E5484D"
RED_D = "#B8323A"
BLADE = "#D7DEE8"
WOOD = "#C98B55"
WOOD_D = "#9C6B45"
CHEESE = "#FFD15C"
CHEESE_D = "#E0A92E"
POT = "#E5484D"


def swiss_knife_head():
    """萌王的「電漿瑞士刀」：紅色刀柄（白色十字）、展開的刀刃、剪刀和開瓶器，刀鋒發出洋紅色的光。朝 +x。"""
    b = path("M-4,-14 L64,-14 Q76,0 64,14 L-4,14 Q-12,0 -4,-14 Z", RED, NAVY, 3)
    b += path("M24,-6 L32,-6 L32,-2 L36,-2 L36,6 L32,6 L32,10 L24,10 L24,6 L20,6 L20,-2 L24,-2 Z", "#FFFFFF", "none")
    # 開瓶器與剪刀（往上下張開的小工具）
    b += path("M10,-14 Q4,-30 14,-34 Q24,-30 16,-22 Q10,-26 14,-30", "none", BLADE, 4)
    b += path("M40,14 L52,30 M46,14 L60,28", "none", BLADE, 4)
    # 主刀刃
    b += path("M66,-10 L132,-4 L146,0 L132,6 L66,10 Z", BLADE, NAVY, 3)
    b += lit(f'<path d="M70,6 L132,4 L146,0" fill="none" stroke="{MAGENTA}" stroke-width="3"/>', 2)
    return Sprite("prop_swissknife_head", 320, 150, "-14 -38 164 76", b, GLOW)


def lost_swissknife():
    return Sprite("lost_swissknife", 320, 150, "-14 -38 164 76", f'<g>{swiss_knife_head().body}</g>', GLOW)


def mousetrap(frame):
    """老鼠夾：木板＋彈簧鐵桿，上面放著一小塊起司當誘餌；frame 1 是夾下去的樣子。"""
    b = path("M-46,-30 L46,-30 L46,30 L-46,30 Z", WOOD, INK, 4)
    b += path("M-40,-22 L40,-22 M-40,22 L40,22", "none", WOOD_D, 3)
    b += ellipse(0, 0, 10, 10, "#9AA3AE", INK, 3)
    if frame:
        b += path("M-34,-24 L-34,24 L34,24 L34,-24", "none", "#B8C2D0", 5)
        for x, y in ((-50, -38), (52, -34), (0, -46)):
            b += star(x, y, 7, "#FFE07A", INK, 2)
    else:
        b += path("M-34,0 L-34,-50 L34,-50 L34,0", "none", "#B8C2D0", 5)
        b += path("M10,10 L30,4 L30,20 Z", CHEESE, INK, 2.5)
    return Sprite(f"min_mousetrap_{frame}", 140, 160, "-60 -66 120 136", g(wob(b)))


def fondue():
    """萌王覺醒時搬出來的起司火鍋：紅色的鍋子、滿滿融化的起司，插著兩支長叉子。"""
    b = path("M-92,0 Q-90,70 0,76 Q90,70 92,0 Z", POT, INK, 4.5)
    b += path("M-84,10 Q0,40 84,10", "none", RED_D, 5)
    b += ellipse(0, 0, 94, 22, CHEESE, INK, 4)
    for x, r in ((-40, 8), (10, 11), (46, 7)):
        b += ellipse(x, -2, r, r * 0.6, "#FFE9A8", "none")
    b += path("M-60,-4 L-96,-90 M40,-4 L76,-92", "none", "#9AA3AE", 5)
    b += ellipse(-96, -90, 9, 9, CHEESE, INK, 2.5) + ellipse(76, -92, 9, 9, CHEESE, INK, 2.5)
    b += path("M-100,4 Q-116,10 -104,26 M100,4 Q116,10 104,26", "none", INK, 5)
    for k in (-1, 1):
        b += path(f"M{k * 30},76 L{k * 40},100", "none", "#5B6378", 7)
    b += path("M-50,100 L50,100", "none", "#5B6378", 7)
    return Sprite("fx_fondue", 300, 280, "-120 -110 240 224", g(wob(b)))


def all_sprites():
    out = [swiss_knife_head(), lost_swissknife(), fondue()]
    for f in range(2):
        out.append(mousetrap(f))
    return out
