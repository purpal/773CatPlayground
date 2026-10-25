"""第 8 關：清澈溪流的水獺姊妹。

Boss「水獺姊妹 啾啾＆嚕嚕」：兩隻小水獺共用一條血量。水獺會手牽手睡覺、會拿石頭敲貝殼、愛玩石頭雜耍，
所以拿的是「重力石錘」（道具由遊戲另外畫，見 scifi.stone_hammer）。
啾啾：淺棕色、頭上夾粉紅貝殼；嚕嚕：深棕色、夾藍色貝殼（畫成左右相反，石錘拿在左手）。
45% 被打到「卸甲」：石錘飛走，之後手牽手旋轉、丟貝殼迴力鏢、潛水再浮出來。
心情：m0 開心、m1 生氣、m2 被打哭（貝殼髮夾掉了）。
小怪：跳躍魚（自毀俯衝型）、蛤蜊砲台（地面固定型）；菁英怪：河狸工程師。
"""
from common import *
from bosses import brows, crying_eyes, squash

SISTERS = {
    # 名稱：毛色、深色、貝殼、貝殼深色
    "otter": ("#B07A55", "#8E5C3C", "#FF9EC0", "#E5739A"),
    "otter2": ("#7E5238", "#5F3B27", "#7EC8FF", "#4FA3E0"),
}
FACE = "#F3DFC4"
NOSE = "#3B2A2A"
FISH = "#7EC8FF"
FISH_D = "#4FA3E0"
CLAM = "#F4C9A8"
CLAM_D = "#D9A07C"
BEAVER = "#A0663D"
BEAVER_D = "#7C4A27"
HELMET = "#FFD15C"

VIEW = "-180 -175 360 350"
SIZE = (540, 525)


def shell_clip(cx, cy, fill, dark, s=1.0, rot=0):
    d = f"M0,{14 * s} L{-24 * s},{-6 * s} Q0,{-30 * s} {24 * s},{-6 * s} Z"
    out = path(d, fill, INK, 3.5)
    for k in (-2, -1, 0, 1, 2):
        out += path(f"M0,{12 * s} L{k * 9 * s},{-14 * s}", "none", dark, 2.5)
    out += ellipse(0, 14 * s, 7 * s, 5 * s, dark, INK, 2.5)
    return f'<g transform="translate({cx} {cy}) rotate({rot})">{out}</g>'


def otter_boss(name, m, frame):
    fur, fur_d, shell, shell_d = SISTERS[name]
    b = ""
    # 小圓耳
    for s in (-1, 1):
        b += ellipse(s * 112, -54, 20, 18, fur, INK, 4.5) + ellipse(s * 112, -51, 10, 9, fur_d, "none")
    # 身體（長一點的麻糬）
    b += path(mochi_path(0, 32, 246, 286, 1.05), fur, INK, 5)
    b += ellipse(0, 116, 76, 40, FACE, "none", None, 'opacity="0.75"')
    # 臉：奶油色的口鼻＋眉上兩撮淺毛
    b += path("M0,-6 C56,-6 92,20 92,52 C92,86 50,100 0,100 C-50,100 -92,86 -92,52 C-92,20 -56,-6 0,-6 Z", FACE, INK, 3.5)
    for s in (-1, 1):
        b += ellipse(s * 46, -34, 18, 9, FACE, "none", None, 'opacity="0.8"')
    ey = -12
    if m == 0:
        b += brows("brave", -46, 46, ey - 26)
        b += dot_eye(-46, ey, 10) + dot_eye(46, ey, 10)
    elif m == 1:
        b += brows("angry", -46, 46, ey - 28, sw=6)
        b += dot_eye(-46, ey + 2, 9) + dot_eye(46, ey + 2, 9)
        b += anger_mark(104, -50, 16)
    else:
        b += brows("worried", -46, 46, ey - 24)
        b += crying_eyes(-46, 46, ey)
        b += tears(-58, ey + 14, 46, -1) + tears(58, ey + 14, 46, 1)
    # 鼻子、嘴、鬍鬚
    for s in (-1, 1):
        b += ellipse(s * 24, 42, 26, 20, "#FBEBD6", INK, 3)
        for dx, dy in ((-8, -4), (2, 4), (10, -6)):
            b += ellipse(s * 24 + dx, 42 + dy, 2, 2, "#A08470", "none")
    b += path("M-18,22 Q0,14 18,22 Q14,38 0,40 Q-14,38 -18,22 Z", NOSE, INK, 3)
    b += ellipse(-6, 24, 5, 3, "#6A5555", "none")
    if m == 1:
        b += open_mouth(0, 60, 14, 16)
    elif m == 2:
        b += wavy_mouth(0, 70, 11)
    else:
        b += omega_mouth(0, 62, 10)
    for s in (-1, 1):
        for dy in (-4, 8):
            b += path(f"M{s * 44},{38 + dy} L{s * 96},{30 + dy * 1.8}", "none", "#A08470", 3)
    b += blush(-70, 40, 20) + blush(70, 40, 20)
    # 手（右手拿石錘）、腳、尾巴尖
    b += ellipse(-100, 104, 20, 18, fur, INK, 4)
    b += ellipse(104, 96, 22, 20, fur, INK, 4)
    for s in (-1, 1):
        b += ellipse(s * 54, 158, 36, 15, fur_d, INK, 4)
    if m < 2:
        b += shell_clip(-70, -96, shell, shell_d, 1.1, -18)
    body = g(wob(b))
    if m == 2 and frame == 1:
        body += g(tears(-58, ey + 32, 30, -1) + tears(58, ey + 32, 30, 1))
    if name == "otter2":
        body = f'<g transform="scale(-1 1)">{body}</g>'
    return Sprite(f"boss_{name}_m{m}_{frame}", *SIZE, VIEW, squash(body, frame))


def lost_shell():
    return Sprite("lost_shell", 110, 90, "-30 -26 60 50", g(wob(shell_clip(0, 0, SISTERS["otter"][2], SISTERS["otter"][3], 1.0))))


# ───────────── 小怪 ─────────────

def mini(name, body, size=180, view="-60 -60 120 120"):
    return Sprite(name, size, size, view, g(wob(body)))


def leap_fish(frame):
    """跳躍魚：從溪裡跳出來朝玩家衝過去，自毀俯衝型（頭朝下）。frame 1 尾巴擺動。"""
    flick = 14 if frame else -14
    b = f'<g transform="rotate({flick} 0 -26)">' + path("M0,-26 L-18,-52 L18,-52 Z", FISH_D, INK, 3) + "</g>"
    b += ellipse(0, 0, 22, 34, FISH, INK, 4)
    b += path("M-18,-6 Q0,2 18,-6", "none", FISH_D, 3)
    for k in (-1, 1):
        b += path(f"M{k * 22},-2 L{k * 34},{-12}  L{k * 26},8 Z", FISH_D, INK, 2.5)
    b += dot_eye(-9, 14, 4) + dot_eye(9, 14, 4)
    b += ellipse(0, 26, 5, 3.5, "#FF8FB1", INK, 2)
    for dx, dy in ((-24, -40), (24, -36)):
        b += ellipse(dx, dy, 4, 4, "#E4F7FF", "#7FC4E8", 2)
    return mini(f"min_fish_{frame}", b, 140, "-46 -62 92 100")


def clam(frame):
    """蛤蜊砲台：殼張開時（frame 0）露出珍珠和小臉。地面固定型。"""
    b = path("M-44,10 Q0,40 44,10 Q40,34 0,38 Q-40,34 -44,10 Z", CLAM, INK, 4)
    for k in (-2, -1, 0, 1, 2):
        b += path(f"M{k * 8},34 L{k * 14},22", "none", CLAM_D, 2.5)
    if frame == 0:
        b += path("M-44,8 Q-40,-40 0,-44 Q40,-40 44,8 Q0,-6 -44,8 Z", CLAM, INK, 4)
        for k in (-2, -1, 0, 1, 2):
            b += path(f"M{k * 6},-6 L{k * 16},-38", "none", CLAM_D, 2.5)
        b += ellipse(0, 10, 34, 12, "#FFC7D6", INK, 3)
        b += ellipse(0, 6, 11, 11, "#FFFFFF", INK, 3) + ellipse(-3, 3, 4, 4, "#E8F4FF", "none")
    else:
        b += path("M-44,10 Q-40,-14 0,-16 Q40,-14 44,10 Q0,22 -44,10 Z", CLAM, INK, 4)
        b += arc_eye(-12, 2, 5, up=False, sw=2.5) + arc_eye(12, 2, 5, up=False, sw=2.5)
    b += ellipse(-30, 30, 7, 4, "#FF8FB1", "none", None, 'opacity="0.6"') + ellipse(30, 30, 7, 4, "#FF8FB1", "none", None, 'opacity="0.6"')
    return mini(f"min_clam_{frame}", b, 150, "-52 -52 104 100")


def elite_beaver(frame):
    """菁英怪：河狸工程師。戴黃色安全帽、拿著扳手，大門牙、扁扁的尾巴。"""
    swing = 20 if frame else -10
    b = f'<g transform="rotate({swing // 2} 0 70)">' + ellipse(0, 78, 30, 16, BEAVER_D, INK, 3.5) + "</g>"
    for k in (-1, 0, 1):
        b += path(f"M{k * 12 - 8},72 L{k * 12 + 8},84", "none", "#5A3A22", 2)
    b += path(mochi_path(0, 12, 132, 124), BEAVER, INK, 4.5)
    b += ellipse(0, 40, 40, 28, "#E8C9A4", "none")
    for s in (-1, 1):
        b += ellipse(s * 50, -38, 12, 11, BEAVER, INK, 3.5)
    # 安全帽
    b += path("M-50,-34 Q-48,-80 0,-82 Q48,-80 50,-34 Z", HELMET, INK, 4)
    b += path("M-60,-34 L60,-34 L58,-26 L-58,-26 Z", "#F2B62E", INK, 3.5)
    b += path("M0,-80 L0,-36", "none", "#F2B62E", 6)
    b += dot_eye(-22, -6, 6) + dot_eye(22, -6, 6)
    b += brows("brave", -22, 22, -18, w=10, sw=4)
    b += ellipse(0, 12, 16, 11, "#E8C9A4", INK, 3) + ellipse(0, 8, 7, 5, NOSE, "none")
    b += path("M-8,20 L-8,34 L8,34 L8,20 Z M0,20 L0,34", "#FFFFFF", INK, 2.5)
    b += blush(-38, 12, 10) + blush(38, 12, 10)
    # 扳手
    b += f'<g transform="rotate({-30 + swing} 56 30)">' + path("M52,30 L52,-20 L60,-20 L60,30 Z", "#B8C2D0", INK, 3)
    b += path("M46,-20 Q56,-40 66,-20 L62,-26 L50,-26 Z", "#B8C2D0", INK, 3) + "</g>"
    b += ellipse(56, 32, 14, 12, BEAVER, INK, 3.5)
    b += ellipse(-56, 30, 14, 12, BEAVER, INK, 3.5)
    return mini(f"elite_beaver_{frame}", b, 240, "-90 -100 180 200")


def all_sprites():
    out = []
    for name in SISTERS:
        for m in range(3):
            for f in range(2):
                out.append(otter_boss(name, m, f))
    out.append(lost_shell())
    for f in range(2):
        out += [leap_fish(f), clam(f), elite_beaver(f)]
    return out
