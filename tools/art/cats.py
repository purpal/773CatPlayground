"""玩家貓咪：選單正面圖、遊玩時的背面奔跑圖（中／左／右 × 4 格）、變身後的老虎／黑豹、大絕按鈕頭像。"""
import math
from common import *

PALETTES = {
    "orange": dict(fur="#FFB25B", shade="#F0954A", stripe="#D9742F", belly="#FFF4E3", ear="#FFCADA", ink=INK, eye=None),
    "black": dict(fur="#4B4560", shade="#3A3450", stripe=None, belly="#F2EEFA", ear="#FF9FBF", ink=DARK_INK, eye="#FFD84D"),
    "tiger": dict(fur="#FF9A2E", shade="#E07A1E", stripe="#3A2A24", belly="#FFF6E8", ear="#FFF6E8", ink=DARK_INK, eye="#9BE05A"),
    "panther": dict(fur="#2B2740", shade="#1F1B30", stripe=None, belly="#2B2740", ear="#6E5BD6", ink="#120F1C", eye="#FFE135", rim="#A894FF"),
}


# ───────────── 正面（選單、對話頭像） ─────────────

def front(kind):
    p = PALETTES[kind]
    ink = p["ink"]
    b = ""
    # 尾巴
    b += path("M52,40 C92,40 96,0 80,-20", "none", ink, 16) + path("M52,40 C92,40 96,0 80,-20", "none", p["fur"], 9)
    if p["stripe"]:
        b += path("M86,10 L96,6 M84,-6 L94,-12", "none", p["stripe"], 5)
    # 耳朵
    for s in (-1, 1):
        b += path(ear(s * 46, -88, s * 70, -40, s * 20, -58), p["fur"], ink, 5)
        b += path(ear(s * 45, -76, s * 60, -44, s * 28, -56), p["ear"], "none")
    # 麻糬身體
    b += path(mochi_path(0, 4, 168, 150), p["fur"], ink, 5)
    b += path("M-70,40 C-46,74 46,74 70,40 C46,62 -46,62 -70,40 Z", p["shade"], "none", None, 'opacity="0.55"')
    # 黑貓是整隻黑：沒有白肚子
    if kind != "black":
        b += ellipse(0, 46, 40, 24, p["belly"], "none", None, 'opacity="0.95"')
    if p["stripe"]:
        for k in (-1, 0, 1):
            b += path(f"M{k * 14 - 5},-70 L{k * 14 + 5},-70 L{k * 14},{-52 + abs(k) * 5} Z", p["stripe"], "none")
        for s in (-1, 1):
            b += path(f"M{s * 84},-6 L{s * 68},-2 M{s * 84},10 L{s * 70},12", "none", p["stripe"], 6)
    # 前腳
    for s in (-1, 1):
        b += ellipse(s * 30, 74, 20, 12, p["belly"] if kind == "orange" else p["fur"], ink, 4)
    # 臉
    if p["eye"]:
        b += color_eye(-30, 2, 13, p["eye"], slit=True) + color_eye(30, 2, 13, p["eye"], slit=True)
    else:
        b += dot_eye(-30, 2, 9) + dot_eye(30, 2, 9)
    # 黑貓沒有嘴巴（像大絕按鈕原本的黑貓剪影），只留眼睛
    if kind != "black":
        b += ellipse(0, 18, 6, 4.5, "#FF9DB8", ink, 2.5)
        b += omega_mouth(0, 27, 9)
    b += blush(-52, 26, 16) + blush(52, 26, 16)
    whisker = "#D8D2EE" if kind == "black" else "#9C8590"
    for s in (-1, 1):
        b += path(f"M{s * 40},20 Q{s * 66},14 {s * 88},18 M{s * 40},28 Q{s * 64},30 {s * 86},38", "none", whisker, 2.5)
    return Sprite(f"cat_{kind}_front", 320, 320, "-110 -110 220 220", g(wob(b), ink=ink))


# ───────────── 背面俯看奔跑 ─────────────

def run(kind, pose, frame):
    """pose: c（直走）、l（往左）、r（往右）；frame 0～3 奔跑循環。"""
    if pose == "r":
        sprite = run(kind, "l", frame)
        sprite.name = sprite.name.replace("_l_", "_r_")
        sprite.body = f'<g transform="scale(-1 1)">{sprite.body}</g>'
        return sprite
    p = PALETTES[kind]
    ink = p["ink"]
    beast = kind in ("tiger", "panther")
    big = 1.12 if beast else 1.0
    stride = math.sin(frame * math.pi / 2)
    lean = -10 if pose == "l" else 0
    head_dx = -8 if pose == "l" else 0
    b = ""
    # 腳下揚起的小煙塵
    for i, (x, y) in enumerate(((-30, 96), (28, 100), (0, 108))):
        r = 7 + ((frame + i) % 4) * 2
        b += ellipse(x + (i - 1) * stride * 6, y + ((frame + i) % 4) * 4, r, r * 0.8, "#FFFFFF", "none", None, f'opacity="{0.55 - ((frame + i) % 4) * 0.1:.2f}"')
    # 尾巴（往反方向甩）
    sway = 26 * stride + (16 if pose == "l" else 0)
    tail = f"M0,62 C{sway * 0.3},84 {sway},92 {sway * 0.9},112"
    b += path(tail, "none", ink, 15) + path(tail, "none", p["fur"], 8)
    if p["stripe"]:
        b += path(f"M{sway * 0.55 - 6},92 L{sway * 0.55 + 6},96 M{sway * 0.85 - 6},104 L{sway * 0.85 + 6},106", "none", p["stripe"], 5)
    if beast and kind == "tiger":
        b += ellipse(sway * 0.9, 112, 7, 6, "#FFFFFF", ink, 3)
    # 後腳（交錯）
    for s, phase in ((-1, 1), (1, -1)):
        dy = phase * stride * 10
        b += ellipse(s * 27, 56 + dy, 13, 17, p["fur"], ink, 4.5)
        b += ellipse(s * 27, 66 + dy, 9, 6, p["belly"] if kind != "panther" else p["shade"], "none")
    # 身體
    body = ellipse(0, 22, 47 * big, 50 * big, p["fur"], ink, 5)
    b += body
    if p["stripe"]:
        for k, y in enumerate((0, 20, 40)):
            w = 34 - abs(k - 1) * 4
            b += path(f"M{-w},{y} Q0,{y - 10} {w},{y}", "none", p["stripe"], 7 if beast else 6)
    b += path(f"M-30,58 Q0,74 30,58", "none", p["shade"], 6, 'opacity="0.6"')
    if kind == "panther":
        b += path("M34,-6 Q48,22 36,54", "none", p["rim"], 4, 'opacity="0.9"')
    # 前腳（與後腳反相）
    for s, phase in ((-1, -1), (1, 1)):
        dy = phase * stride * 12
        b += ellipse(s * 37, -8 + dy, 12, 16, p["fur"], ink, 4.5)
    # 頭（背面），往左時稍微轉頭露出側臉
    hx = head_dx
    hw, hh = 45 * big, 40 * big
    for s in (-1, 1):
        ex = hx + s * 23 * big + (-6 if pose == "l" else 0)
        b += path(ear(ex, -80 * big, ex + s * 20, -50 * big, ex - s * 18, -58 * big), p["fur"], ink, 5)
        b += path(ear(ex, -72 * big, ex + s * 13, -54 * big, ex - s * 10, -58 * big), p["shade"], "none", None, 'opacity="0.6"')
    b += ellipse(hx, -40, hw, hh, p["fur"], ink, 5)
    if p["stripe"]:
        for k in (-1, 0, 1):
            b += path(f"M{hx + k * 13},-74 L{hx + k * 13},{-58 + abs(k) * 4}", "none", p["stripe"], 6)
        b += path(f"M{hx - 38},-38 Q{hx - 30},-34 {hx - 26},-28 M{hx + 38},-38 Q{hx + 30},-34 {hx + 26},-28", "none", p["stripe"], 5)
    if kind == "panther":
        b += path(f"M{hx + 20},-72 Q{hx + 42},-56 {hx + 40},-30", "none", p["rim"], 4)
    whisker = "#D8D2EE" if kind in ("black", "panther") else "#9C8590"
    for s in (-1, 1):
        b += path(f"M{hx + s * 40},-34 L{hx + s * 62},-40 M{hx + s * 40},-28 L{hx + s * 60},-24", "none", whisker, 2.5)
    if pose == "l":
        # 側臉：一隻眼睛、鼻尖、腮紅
        if p["eye"]:
            b += color_eye(hx - 30, -38, 7, p["eye"], slit=True)
        else:
            b += dot_eye(hx - 30, -38, 6)
        b += ellipse(hx - 43, -28, 4, 3, "#FF9DB8", ink, 2)
        b += blush(hx - 30, -22, 9, lines=False)
    if beast:
        b += path(f"M{hx - 22},-58 L{hx - 10},-50 M{hx + 22},-58 L{hx + 10},-50", "none", ink, 4)
    return Sprite(f"run_{kind}_{pose}_{frame}", 240, 288, "-100 -120 200 240", g(wob(b), ink=ink))


# ───────────── 大絕按鈕／選單用的頭像（正面、凶萌） ─────────────

def beast_icon(kind):
    p = PALETTES[kind]
    ink = p["ink"]
    b = ""
    for s in (-1, 1):
        b += path(ear(s * 48, -78, s * 72, -30, s * 22, -50), p["fur"], ink, 5)
        b += path(ear(s * 47, -66, s * 62, -34, s * 30, -48), p["ear"], "none")
    b += path(mochi_path(0, 4, 176, 156), p["fur"], ink, 5)
    if kind == "tiger":
        b += ellipse(0, 30, 52, 32, p["belly"], "none")
        for k in (-1, 0, 1):
            b += path(f"M{k * 15 - 6},-72 L{k * 15 + 6},-72 L{k * 15},{-48 + abs(k) * 6} Z", p["stripe"], "none")
        for s in (-1, 1):
            b += path(f"M{s * 88},-10 L{s * 64},-4 M{s * 88},8 L{s * 66},12 M{s * 86},26 L{s * 70},26", "none", p["stripe"], 7)
    else:
        b += path("M60,-50 Q84,-20 80,20", "none", p["rim"], 5)
        b += path("M-40,-62 Q-10,-74 20,-70", "none", p["rim"], 4, 'opacity="0.8"')
    for s in (-1, 1):
        b += path(f"M{s * 50},-26 L{s * 16},-12", "none", ink, 8)
        b += ellipse(s * 32, -2, 12, 13, p["eye"], ink, 3)
        b += ellipse(s * 32, 0, 4, 10, "#1E1622", "none")
        b += ellipse(s * 35, -6, 3, 3, "#FFFFFF", "none")
    b += ellipse(0, 18, 7, 5, "#FF9DB8", ink, 2.5)
    b += open_mouth(0, 30, 24, 30, fangs=True)
    for s in (-1, 1):
        b += path(f"M{s * 34},28 L{s * 86},20 M{s * 34},36 L{s * 84},44", "none", "#E8E2F8" if kind == "panther" else "#9C8590", 2.5)
    return Sprite(f"icon_{kind}", 200, 200, "-100 -100 200 200", g(wob(b), ink=ink))


def all_sprites():
    out = [front("orange"), front("black"), beast_icon("tiger"), beast_icon("panther")]
    for kind in ("orange", "black", "tiger", "panther"):
        for pose in ("c", "l", "r"):
            for f in range(4):
                out.append(run(kind, pose, f))
    return out
