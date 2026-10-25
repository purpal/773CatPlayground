"""第 7 關：尤加利樹林的無尾熊。

Boss「無尾熊長老 尤加」：灰色毛茸茸、超大圓耳（耳朵裡是白色長毛）、大大的黑鼻子，
一天睡 20 小時——戴著綠色條紋睡帽、掛在樹枝上盪來盪去，打著瞌睡也照樣攻擊。
打到 30% 才「被吵醒」而「裝甲」：戴上護目鏡、裝上肩甲，拿出尤加利葉刃發射器（道具由遊戲另外畫，見 scifi.leaf_launcher）。
心情：m0 打瞌睡（閉眼）、m1 被吵醒暴怒（護目鏡）、m2 被打哭（睡帽飛走）。
小怪：葉片手裏劍（自毀俯衝型）、樹洞貓頭鷹（地面固定型）；菁英怪：袋鼠拳擊手。
"""
from common import *
from bosses import brows, crying_eyes, squash

GREY = "#A9B3C2"
GREY_D = "#8B95A6"
GREY_L = "#D3DAE4"
EAR_IN = "#F4F6FA"
NOSE = "#3B3440"
CAP = "#8BD18A"
CAP_D = "#5DAE5C"
LEAF = "#7CCB8A"
LEAF_D = "#4E9E63"
BARK = "#8D6E63"
BARK_D = "#6D4C41"
OWL = "#B08968"
OWL_D = "#8A6748"
ROO = "#D9A066"
ROO_D = "#B97F45"
GLOVE = "#FF5C6E"
STEEL = "#5B6378"
CORAL = "#FF7A45"

VIEW = "-180 -175 360 350"
SIZE = (540, 525)


def fluffy_ear(cx, cy, r, side):
    """毛茸茸的大圓耳：外圈鋸齒狀的毛，裡面白色長毛。"""
    d = ""
    n = 14
    for i in range(n + 1):
        a = math.radians(i * 360 / n)
        rr = r * (1.0 if i % 2 == 0 else 0.9)
        d += ("M" if i == 0 else "L") + f"{cx + math.cos(a) * rr:.1f},{cy + math.sin(a) * rr:.1f} "
    out = path(d + "Z", GREY, INK, 4.5)
    out += ellipse(cx + side * 8, cy + 6, r * 0.58, r * 0.6, EAR_IN, "none")
    for k in range(-1, 2):
        out += path(f"M{cx + side * 8 + k * 10},{cy + 22} Q{cx + side * 16 + k * 10},{cy} {cx + side * 8 + k * 12},{cy - 18}", "none", "#DCE2EA", 3)
    return out


def nightcap(cx, cy, rot=0, s=1.0):
    out = path(f"M{-62 * s},{14 * s} C{-50 * s},{-50 * s} {40 * s},{-70 * s} {96 * s},{-10 * s} C{70 * s},{-26 * s} {40 * s},{-10 * s} {62 * s},{14 * s} Z", CAP, INK, 4)
    for k in range(3):
        x = -30 + k * 30
        out += path(f"M{x * s},{10 * s} Q{(x + 10) * s},{-24 * s} {(x + 24) * s},{-44 * s}", "none", CAP_D, 7)
    out += path(f"M{-66 * s},{8 * s} Q0,{-6 * s} {66 * s},{8 * s} L{66 * s},{24 * s} Q0,{12 * s} {-66 * s},{24 * s} Z", "#FFFFFF", INK, 4)
    out += ellipse(98 * s, -6 * s, 14 * s, 14 * s, "#FFF59D", INK, 3.5)
    return f'<g transform="translate({cx} {cy}) rotate({rot})">{out}</g>'


def koala_boss(m, frame):
    b = ""
    b += fluffy_ear(-116, -54, 66, -1) + fluffy_ear(116, -54, 66, 1)
    b += path(mochi_path(0, 34, 270, 250), GREY, INK, 5)
    b += ellipse(0, 112, 84, 46, GREY_L, "none", None, 'opacity="0.9"')
    ey = 6
    if m == 0:
        # 打瞌睡：閉眼、嘴角一點口水
        for s in (-1, 1):
            b += arc_eye(s * 50, ey, 16, up=False, sw=6)
        b += path("M28,82 Q34,92 30,100", "none", "#9ADBFF", 5)
    elif m == 1:
        b += brows("angry", -50, 50, ey - 30, sw=6)
        b += path(f"M-132,{ey} L132,{ey}", "none", STEEL, 12)
        for s in (-1, 1):
            b += path(f"M{s * 22},{ey - 22} L{s * 88},{ey - 20} L{s * 84},{ey + 22} L{s * 26},{ey + 22} Z", STEEL, INK, 4)
            b += path(f"M{s * 30},{ey - 14} L{s * 80},{ey - 13} L{s * 77},{ey + 14} L{s * 33},{ey + 14} Z", CORAL, "none")
            b += path(f"M{s * 36},{ey - 7} L{s * 56},{ey - 8}", "none", "#FFE0CC", 4)
        b += anger_mark(110, -40, 18)
    else:
        b += brows("worried", -50, 50, ey - 26)
        b += crying_eyes(-50, 50, ey)
        b += tears(-64, ey + 16, 50, -1) + tears(64, ey + 16, 50, 1)
    # 大黑鼻子
    b += path("M0,24 C26,24 32,44 30,64 C28,84 14,92 0,92 C-14,92 -28,84 -30,64 C-32,44 -26,24 0,24 Z", NOSE, INK, 3.5)
    b += ellipse(-10, 40, 7, 10, "#6B6274", "none")
    if m == 1:
        b += open_mouth(0, 100, 18, 18)
    elif m == 2:
        b += wavy_mouth(0, 106, 14)
    else:
        b += omega_mouth(0, 102, 12)
    b += blush(-90, ey + 36, 22) + blush(90, ey + 36, 22)
    # 裝甲：肩甲
    if m == 1:
        for s in (-1, 1):
            b += path(f"M{s * 96},48 L{s * 152},36 L{s * 162},98 L{s * 110},106 Z", STEEL, INK, 4)
            b += path(f"M{s * 106},58 L{s * 146},50", "none", CORAL, 5)
    # 手腳（右手拿葉刃發射器）
    b += ellipse(-108, 108, 24, 22, GREY, INK, 4)
    b += ellipse(108, 100, 24, 22, GREY, INK, 4)
    for s in (-1, 1):
        b += ellipse(s * 58, 154, 38, 18, GREY, INK, 4)
        for k in (-1, 0, 1):
            b += path(f"M{s * 58 + k * 12},146 L{s * 58 + k * 12},152", "none", GREY_D, 3)
    if m == 0:
        b += nightcap(-14, -104, -8)
    body = g(wob(b))
    if m == 2 and frame == 1:
        body += g(tears(-64, ey + 34, 34, -1) + tears(64, ey + 34, 34, 1))
    return Sprite(f"boss_koala_m{m}_{frame}", *SIZE, VIEW, squash(body, frame))


def lost_nightcap():
    return Sprite("lost_nightcap", 200, 130, "-80 -62 200 130", g(wob(nightcap(0, 20, 0, 0.95))))


# ───────────── 小怪 ─────────────

def mini(name, body, size=180, view="-60 -60 120 120"):
    return Sprite(name, size, size, view, g(wob(body)))


def leaf_star():
    """葉片手裏劍：四片尤加利葉排成十字、中間一張小臉。自毀俯衝型（遊戲裡會旋轉）。"""
    b = ""
    for a in (0, 90, 180, 270):
        b += f'<g transform="rotate({a})">' + path("M0,-8 C14,-20 14,-40 0,-52 C-14,-40 -14,-20 0,-8 Z", LEAF, INK, 3.5)
        b += path("M0,-12 L0,-46", "none", LEAF_D, 2.5) + "</g>"
    b += ellipse(0, 0, 16, 16, "#E9F7DF", INK, 3)
    b += arc_eye(-6, -1, 4, up=True, sw=2.5) + arc_eye(6, -1, 4, up=True, sw=2.5)
    b += path("M-3,5 Q0,8 3,5", "none", None, 2)
    return mini("min_leafstar", b, 150, "-60 -60 120 120")


def owl(frame):
    """樹洞貓頭鷹：躲在樹樁洞裡，探頭射擊。frame 1 眨眼。地面固定型。"""
    b = path("M-48,60 L-44,-30 Q0,-44 44,-30 L48,60 Z", BARK, INK, 4)
    b += ellipse(0, -32, 44, 12, "#C8A27C", INK, 3.5)
    for r in (8, 16, 24):
        b += ellipse(0, -32, r * 1.6, r * 0.4, "none", "#A8825C", 2)
    b += path("M-40,20 Q-20,14 -36,0 M38,30 Q20,24 34,10", "none", BARK_D, 3)
    b += ellipse(0, 18, 30, 34, "#3E2B26", INK, 3)
    b += ellipse(0, 20, 26, 26, OWL, INK, 3.5)
    b += path("M-22,0 L-16,-12 L-8,-2 M22,0 L16,-12 L8,-2", OWL, INK, 3)
    for s in (-1, 1):
        b += ellipse(s * 11, 14, 10, 10, "#FFF3D6", INK, 2.5)
        if frame:
            b += arc_eye(s * 11, 14, 6, up=True, sw=2.5)
        else:
            b += dot_eye(s * 11, 14, 4.5)
    b += path("M-4,22 L4,22 L0,30 Z", "#FFB74D", INK, 2)
    return mini(f"min_owl_{frame}", b, 160, "-60 -60 120 130")


def elite_kangaroo(frame):
    """菁英怪：袋鼠拳擊手。戴紅色拳擊手套、頭帶，育兒袋裡探出小袋鼠。"""
    b = ""
    for s in (-1, 1):
        b += f'<g transform="rotate({s * 14} {s * 24} -60)">' + ellipse(s * 26, -92, 13, 34, ROO, INK, 4) + ellipse(s * 26, -88, 6, 22, "#F2C9A8", "none") + "</g>"
    b += path(mochi_path(0, 16, 130, 124), ROO, INK, 4.5)
    b += ellipse(0, 46, 40, 30, "#F5DDBE", INK, 3)
    # 育兒袋裡的小袋鼠
    b += ellipse(0, 40, 14, 12, ROO, INK, 3) + dot_eye(-5, 38, 2.5) + dot_eye(5, 38, 2.5)
    b += path("M-34,34 Q0,46 34,34", "none", INK, 3)
    # 頭帶
    b += path("M-58,-34 Q0,-50 58,-34 L56,-22 Q0,-38 -56,-22 Z", "#FF5C6E", INK, 3.5)
    b += path("M54,-30 L76,-40 L72,-24 Z", "#FF5C6E", INK, 3)
    b += brows("angry", -22, 22, -8, w=10, sw=4)
    b += dot_eye(-22, 4, 6) + dot_eye(22, 4, 6)
    b += ellipse(0, 18, 7, 5, NOSE, "none")
    b += omega_mouth(0, 24, 7, 3)
    b += blush(-38, 16, 10) + blush(38, 16, 10)
    # 拳擊手套（frame 1 出拳）
    punch = 16 if frame else 0
    b += ellipse(-58, 20 - punch, 20, 18, GLOVE, INK, 4) + path(f"M-70,{26 - punch} L-50,{30 - punch}", "none", "#FFFFFF", 3)
    b += ellipse(58, 20 + punch - 16, 20, 18, GLOVE, INK, 4) + path(f"M50,{26 + punch - 16} L70,{30 + punch - 16}", "none", "#FFFFFF", 3)
    b += ellipse(-30, 82, 22, 9, ROO, INK, 3) + ellipse(30, 82, 22, 9, ROO, INK, 3)
    return mini(f"elite_kangaroo_{frame}", b, 240, "-90 -140 180 235")


def all_sprites():
    out = []
    for m in range(3):
        for f in range(2):
            out.append(koala_boss(m, f))
    out.append(lost_nightcap())
    for f in range(2):
        out += [owl(f), elite_kangaroo(f)]
    out.append(leaf_star())
    return out
