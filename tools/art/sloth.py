"""第 10 關：雨林樹冠的樹懶。

Boss「樹懶郵差 閃電」：灰棕色麻糬身體、臉上天生的微笑、往下垂的深色眼紋、長長的爪子，
戴郵差帽、揹郵差包，兩隻手勾著空中的郵件纜線慢慢滑過去。
打到 35% 才「裝甲」（限時專送）：戴上護目鏡、穿上火箭滑板鞋，拿出快遞火箭筒（道具見 rocket_launcher）。
心情：m0 半閉眼微笑、m1 裝甲認真、m2 被打哭（郵差帽飛走、信件散落）。
小怪：樹蛙（自毀俯衝型）、豬籠草砲台（地面固定型）；菁英怪：巨嘴鳥飛行員。
"""
from common import *
from bosses import crying_eyes, squash
from scifi import GLOW, NAVY, AMBER, CORAL as SCORAL, lit

FUR = "#BBA791"
FUR_D = "#9C8670"
FACE = "#F1E6D3"
STRIPE = "#5B4636"
CLAW = "#F4EAD8"
CAP = "#3F6E8C"
CAP_D = "#2E5470"
GOLD = "#FFD15C"
BAG = "#C98B55"
BAG_D = "#A86E3E"
STEEL = "#5B6378"
CORAL = "#FF7A45"
FROG = "#7BD36B"
FROG_D = "#4FA84A"
PITCHER = "#9FD66B"
PITCHER_D = "#6FA845"
TOUCAN = "#2E2A3A"
BEAK = "#FFB23E"

VIEW = "-180 -175 360 350"
SIZE = (540, 525)


def envelope(cx, cy, s=1.0, rot=0):
    out = path(f"M{-18 * s},{-12 * s} L{18 * s},{-12 * s} L{18 * s},{12 * s} L{-18 * s},{12 * s} Z", "#FFFFFF", INK, 3)
    out += path(f"M{-18 * s},{-12 * s} L0,{2 * s} L{18 * s},{-12 * s}", "none", INK, 2.5)
    out += heart(0, 4 * s, 3.5 * s, "#FF8FB1", INK, 1.5)
    return f'<g transform="translate({cx} {cy}) rotate({rot})">{out}</g>'


def postman_cap(cx, cy, rot=0, s=1.0):
    out = path(f"M{-62 * s},{6 * s} C{-58 * s},{-46 * s} {58 * s},{-46 * s} {62 * s},{6 * s} Z", CAP, INK, 4.5)
    out += path(f"M{-66 * s},{4 * s} L{66 * s},{4 * s} L{66 * s},{14 * s} L{-66 * s},{14 * s} Z", CAP_D, INK, 4)
    out += path(f"M{-40 * s},{14 * s} Q0,{34 * s} {40 * s},{14 * s} Z", CAP_D, INK, 4)
    out += envelope(0, -18 * s, 0.75 * s)
    out += path(f"M{-18 * s},{-30 * s} L{18 * s},{-30 * s}", "none", GOLD, 0)
    return f'<g transform="translate({cx} {cy}) rotate({rot})">{out}</g>'


def hanging_arm(side):
    """往上舉、勾住纜線的手臂（三根長爪掛在纜線上）。"""
    sx, sy = side * 96, 10
    hx, hy = side * 74, -138
    out = path(f"M{sx - side * 26},{sy} Q{side * 120},{-60} {hx - side * 18},{hy} L{hx + side * 18},{hy} Q{side * 150},{-50} {sx + side * 18},{sy + 20} Z", FUR, INK, 4.5)
    for k in (-1, 0, 1):
        x = hx + k * 11
        out += path(f"M{x},{hy + 4} Q{x - 2},{hy - 26} {x + side * 14},{hy - 22}", "none", CLAW, 6)
        out += path(f"M{x},{hy + 4} Q{x - 2},{hy - 26} {x + side * 14},{hy - 22}", "none", INK, 1.5)
    return out


def sloth_boss(m, frame):
    b = ""
    b += hanging_arm(-1) + hanging_arm(1)
    b += path(mochi_path(0, 44, 250, 230), FUR, INK, 5)
    b += path("M-100,96 C-70,150 70,150 100,96 C60,132 -60,132 -100,96 Z", FUR_D, "none", None, 'opacity="0.7"')
    # 淺色的臉
    b += ellipse(0, 18, 92, 70, FACE, INK, 3.5)
    ey = 8
    # 往下垂的眼紋
    for s in (-1, 1):
        b += path(f"M{s * 18},{ey - 14} Q{s * 54},{ey - 22} {s * 74},{ey + 2} Q{s * 86},{ey + 30} {s * 66},{ey + 36} Q{s * 40},{ey + 18} {s * 18},{ey + 4} Z", STRIPE, "none")
    if m == 0:
        for s in (-1, 1):
            b += path(f"M{s * 34},{ey + 4} Q{s * 48},{ey + 12} {s * 62},{ey + 4}", "none", "#FFFFFF", 5)
    elif m == 1:
        # 護目鏡
        b += path(f"M-128,{ey} L128,{ey}", "none", STEEL, 12)
        for s in (-1, 1):
            b += path(f"M{s * 18},{ey - 22} L{s * 86},{ey - 20} L{s * 82},{ey + 24} L{s * 22},{ey + 24} Z", STEEL, INK, 4)
            b += path(f"M{s * 26},{ey - 14} L{s * 78},{ey - 13} L{s * 75},{ey + 16} L{s * 29},{ey + 16} Z", CORAL, "none")
            b += path(f"M{s * 34},{ey - 7} L{s * 54},{ey - 8}", "none", "#FFE0CC", 4)
            b += path(f"M{s * 24},{ey - 34} L{s * 70},{ey - 44}", "none", None, 6)
        b += anger_mark(112, -48, 18)
    else:
        for s in (-1, 1):
            b += path(f"M{s * 28},{ey - 34} Q{s * 48},{ey - 32} {s * 66},{ey - 22}", "none", None, 5)
        b += crying_eyes(-48, 48, ey + 8, 10)
        b += tears(-62, ey + 24, 48, -1) + tears(62, ey + 24, 48, 1)
    # 鼻子與嘴（樹懶天生就在微笑）
    b += ellipse(0, ey + 30, 14, 10, STRIPE, INK, 3)
    b += ellipse(-4, ey + 27, 4, 2.5, "#8A7262", "none")
    if m == 1:
        b += open_mouth(0, ey + 48, 18, 16)
    elif m == 2:
        b += wavy_mouth(0, ey + 54, 14)
    else:
        b += path(f"M-30,{ey + 44} Q0,{ey + 66} 30,{ey + 44}", "none", None, 4)
    b += blush(-80, ey + 40, 20) + blush(80, ey + 40, 20)
    # 郵差包（斜背帶＋包包，信封露出來）
    b += path("M-112,84 Q-20,128 80,122", "none", BAG_D, 12)
    b += path("M60,100 L132,90 L138,146 L66,154 Z", BAG, INK, 4)
    b += path("M60,100 L132,90 L128,112 L64,120 Z", BAG_D, INK, 3)
    if m < 2:
        b += envelope(98, 88, 0.8, -12)
    # 腳：裝甲後穿火箭滑板鞋
    for s in (-1, 1):
        if m == 1:
            b += path(f"M{s * 30},150 L{s * 96},146 L{s * 100},170 L{s * 28},172 Z", STEEL, INK, 4)
            b += ellipse(s * 40, 176, 9, 9, "#2E2433", INK, 2.5) + ellipse(s * 88, 176, 9, 9, "#2E2433", INK, 2.5)
            b += path(f"M{s * 100},152 L{s * 128},158 L{s * 100},166 Z", AMBER, INK, 2.5)
        else:
            b += ellipse(s * 62, 156, 34, 16, FUR, INK, 4)
            for k in (-1, 0, 1):
                b += path(f"M{s * 62 + k * 10},164 L{s * 62 + k * 12},176", "none", CLAW, 5)
    if m < 2:
        b += postman_cap(0, -78, -4 if m == 0 else 0)
    body = g(wob(b))
    if m == 2:
        # 散落的信件
        body += g(envelope(-130, 120, 0.8, -30) + envelope(140, 40, 0.7, 24))
        if frame == 1:
            body += g(tears(-62, ey + 42, 34, -1) + tears(62, ey + 42, 34, 1))
    return Sprite(f"boss_sloth_m{m}_{frame}", *SIZE, VIEW, squash(body, frame))


def lost_postcap():
    return Sprite("lost_postcap", 200, 120, "-80 -50 160 96", g(wob(postman_cap(0, 6, 0, 1.0))))


def rocket_launcher():
    """樹懶郵差裝甲後拿出的「快遞火箭筒」：鋼製發射筒，前端一個紙箱造型的彈頭（貼著愛心郵票）。朝 +x。"""
    b = path("M-4,-15 L70,-15 L70,15 L-4,15 Z", "url(#steel)", NAVY, 3)
    b += path("M8,-15 L8,15 M60,-15 L60,15", "none", "#8A96B0", 3)
    b += path("M70,-22 L82,-22 L82,22 L70,22 Z", STEEL, NAVY, 3)
    b += path("M82,-18 L126,-18 L126,18 L82,18 Z", "#D9A066", NAVY, 3)
    b += path("M82,0 L126,0", "none", "#B97F45", 3)
    b += f'<rect x="100" y="-12" width="14" height="10" fill="#FF8FB1" stroke="{NAVY}" stroke-width="2"/>'
    b += lit(f'<path d="M126,-18 L140,0 L126,18" fill="none" stroke="{AMBER}" stroke-width="4"/>', 2)
    return Sprite("prop_rocket_head", 300, 110, "-8 -27 150 54", b, GLOW)


# ───────────── 小怪 ─────────────

def mini(name, body, size=180, view="-60 -60 120 120"):
    return Sprite(name, size, size, view, g(wob(body)))


def tree_frog(frame):
    """樹蛙：亮綠色、紅色大眼睛，蹦一下再撲過來。自毀俯衝型。frame 1 腿伸直。"""
    b = ""
    if frame:
        for s in (-1, 1):
            b += path(f"M{s * 18},18 L{s * 34},44 L{s * 26},48", "none", FROG_D, 8)
    else:
        for s in (-1, 1):
            b += ellipse(s * 28, 22, 16, 9, FROG_D, INK, 3)
    b += ellipse(0, 4, 34, 28, FROG, INK, 4)
    b += ellipse(0, 14, 22, 12, "#E8F7C8", "none")
    for s in (-1, 1):
        b += ellipse(s * 18, -20, 13, 13, "#FF5C6E", INK, 3.5)
        b += ellipse(s * 18, -19, 5, 8, "#2E2128", "none") + ellipse(s * 20, -23, 2.5, 2.5, "#FFFFFF", "none")
        b += ellipse(s * 30, 8, 7, 5, "#FF8A3D", INK, 2.5)
    b += path("M-10,2 Q0,10 10,2", "none", None, 2.5)
    b += blush(-22, 4, 7, lines=False) + blush(22, 4, 7, lines=False)
    return mini(f"min_frog_{frame}", b, 140, "-50 -42 100 100")


def pitcher(frame):
    """豬籠草砲台：圓滾滾的瓶子形食蟲植物，蓋子打開時（frame 1）吐出黏液彈。地面固定型。"""
    b = path("M-30,40 Q-44,0 -24,-28 L24,-28 Q44,0 30,40 Z", PITCHER, INK, 4)
    for k in (-1, 0, 1):
        b += path(f"M{k * 14},36 Q{k * 18},6 {k * 10},-22", "none", PITCHER_D, 3)
    b += ellipse(0, -28, 26, 9, "#C93F6B", INK, 3.5)
    lid = -36 if frame else -6
    b += f'<g transform="rotate({lid} 22 -32)">' + path("M-26,-32 Q0,-56 26,-32 Z", PITCHER_D, INK, 3.5) + "</g>"
    b += dot_eye(-10, 6, 3.5) + dot_eye(10, 6, 3.5)
    b += path("M-4,14 Q0,17 4,14", "none", None, 2)
    b += path("M-36,40 Q0,50 36,40", "none", "#6D4C41", 6)
    return mini(f"min_pitcher_{frame}", b, 140, "-50 -62 100 110")


def elite_toucan(frame):
    """菁英怪：巨嘴鳥飛行員。黑色身體、白色胸口、超大的橘色鳥嘴，戴飛行帽和護目鏡。"""
    flap = -30 if frame else 10
    b = ""
    for s in (-1, 1):
        b += f'<g transform="rotate({s * flap} {s * 40} 0)">' + path(f"M{s * 40},-6 Q{s * 96},-20 {s * 92},28 Q{s * 66},20 {s * 40},26 Z", TOUCAN, INK, 4) + "</g>"
    b += path(mochi_path(0, 12, 116, 120), TOUCAN, INK, 4.5)
    b += ellipse(0, 36, 36, 30, "#FFF7E0", "none")
    # 飛行帽＋護目鏡
    b += path("M-52,-28 Q-50,-74 0,-76 Q50,-74 52,-28 Z", "#8D6E63", INK, 4)
    b += path("M-56,-30 L56,-30", "none", STEEL, 8)
    for s in (-1, 1):
        b += ellipse(s * 22, -32, 16, 12, "#BDE8FF", INK, 3)
    b += dot_eye(-22, -8, 5) + dot_eye(22, -8, 5)
    # 大鳥嘴
    b += path("M-18,4 Q30,-6 70,16 Q34,30 -16,20 Z", BEAK, INK, 4)
    b += path("M-16,14 Q30,12 66,18", "none", "#E5484D", 3)
    b += path("M50,10 L70,16 L54,22 Z", "#2E2128", "none")
    b += blush(-36, 10, 9, lines=False)
    b += path("M-60,40 L-80,62 M60,40 L80,62", "none", "#E5484D", 6)
    return mini(f"elite_toucan_{frame}", b, 240, "-100 -100 200 200")


def all_sprites():
    out = []
    for m in range(3):
        for f in range(2):
            out.append(sloth_boss(m, f))
    out += [lost_postcap(), rocket_launcher()]
    for f in range(2):
        out += [tree_frog(f), pitcher(f), elite_toucan(f)]
    return out
