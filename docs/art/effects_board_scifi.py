"""攻擊特效設定板 v3：「外表萌萌，出手是硬派科幻武器」。

執行：python3 docs/art/effects_board_scifi.py → 輸出 docs/art/effects-board-scifi.svg，
再用 tools/art/render.mjs 的同一套 Playwright 轉成 PNG（見 docs/art-style.md）。
角色直接沿用 tools/art 的麻糬萌貓咪與小老鼠，對比出手的科技感。
"""
import math
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "..", "tools", "art"))
from common import COMMON_DEFS  # noqa: E402
import cats  # noqa: E402
import minions  # noqa: E402

W, H = 1200, 2030
PW, PH = 560, 420
CYAN = "#00E5FF"
BLUE = "#2979FF"
VIOLET = "#B388FF"
MAGENTA = "#FF2BD6"
WHITE = "#FFFFFF"
ENEMY = "#FF3D5A"
AMBER = "#FFB300"
MONO = "JetBrains Mono, DejaVu Sans Mono, monospace"
CJK = "WenQuanYi Zen Hei"

out = []
add = out.append
rnd = random.Random(7)


def cat_back(x, y, s=0.55, kind="orange"):
    sp = cats.run(kind, "c", 1)
    return f'<g transform="translate({x} {y}) scale({s})">{sp.body}</g>'


def mouse(x, y, s=0.8, rot=0):
    sp = minions.mouse(0)
    return f'<g transform="translate({x} {y}) rotate({rot}) scale({s})">{sp.body}</g>'


def glow(content, std=5, opacity=1.0):
    """發光：模糊的同色光暈墊在原圖下面，以濾色（screen）疊加。"""
    return (f'<g style="mix-blend-mode:screen" opacity="{opacity}">'
            f'<g filter="url(#blur{std})">{content}</g>{content}</g>')


def bolt(x, y, angle, length, width, color, core=WHITE):
    """電漿彈：拉長的膠囊，白熱核心＋色光外殼＋後方拖曳的光軌。"""
    g = (f'<rect x="{-length}" y="{-width * 0.18}" width="{length * 1.9}" height="{width * 0.36}" rx="{width * 0.18}" fill="url(#trail_{color[1:]})"/>'
         f'<rect x="{-width}" y="{-width / 2}" width="{width * 2.4}" height="{width}" rx="{width / 2}" fill="{color}"/>'
         f'<rect x="{-width * 0.6}" y="{-width * 0.22}" width="{width * 1.8}" height="{width * 0.44}" rx="{width * 0.22}" fill="{core}"/>')
    return f'<g transform="translate({x} {y}) rotate({angle})">{glow(g, 4)}</g>'


def hexagon(x, y, r, rot=0):
    pts = []
    for i in range(6):
        a = math.radians(60 * i + rot)
        pts.append(f"{x + math.cos(a) * r:.1f},{y + math.sin(a) * r:.1f}")
    return " ".join(pts)


def lightning(x0, y0, x1, y1, segs=9, jitter=14, color=CYAN, width=2.5):
    pts = [(x0, y0)]
    for i in range(1, segs):
        t = i / segs
        nx, ny = -(y1 - y0), (x1 - x0)
        d = math.hypot(nx, ny) or 1
        off = (rnd.random() - 0.5) * 2 * jitter
        pts.append((x0 + (x1 - x0) * t + nx / d * off, y0 + (y1 - y0) * t + ny / d * off))
    pts.append((x1, y1))
    p = " ".join(f"{a:.1f},{b:.1f}" for a, b in pts)
    return glow(f'<polyline points="{p}" fill="none" stroke="{color}" stroke-width="{width}" stroke-linejoin="bevel"/>'
                f'<polyline points="{p}" fill="none" stroke="{WHITE}" stroke-width="{width * 0.4}"/>', 3)


def sparks(x, y, n, length, color, spread=360, base=0):
    s = ""
    for _ in range(n):
        a = math.radians(base + (rnd.random() - 0.5) * spread)
        l = length * (0.4 + rnd.random() * 0.8)
        d = 6 + rnd.random() * 10
        s += (f'<line x1="{x + math.cos(a) * d:.1f}" y1="{y + math.sin(a) * d:.1f}" '
              f'x2="{x + math.cos(a) * (d + l):.1f}" y2="{y + math.sin(a) * (d + l):.1f}" '
              f'stroke="{color}" stroke-width="{1.5 + rnd.random() * 1.5:.1f}" stroke-linecap="round"/>')
    return glow(s, 3)


def reticle(x, y, r, color=MAGENTA, label="LOCK"):
    c = r * 0.45
    s = ""
    for sx, sy in ((-1, -1), (1, -1), (1, 1), (-1, 1)):
        s += f'<path d="M{x + sx * r},{y + sy * (r - c)} L{x + sx * r},{y + sy * r} L{x + sx * (r - c)},{y + sy * r}" fill="none" stroke="{color}" stroke-width="2.5"/>'
    s += f'<circle cx="{x}" cy="{y}" r="{r * 0.18}" fill="none" stroke="{color}" stroke-width="1.5"/>'
    s += f'<line x1="{x - r * 0.35}" y1="{y}" x2="{x - r * 0.12}" y2="{y}" stroke="{color}" stroke-width="1.5"/><line x1="{x + r * 0.12}" y1="{y}" x2="{x + r * 0.35}" y2="{y}" stroke="{color}" stroke-width="1.5"/>'
    s += f'<text x="{x + r + 4}" y="{y - r + 10}" font-family="{MONO}" font-size="11" font-weight="700" fill="{color}">{label}</text>'
    return glow(s, 2)


def hud_text(x, y, text, size=14, color=CYAN, anchor="start", weight=700, opacity=1):
    return (f'<text x="{x}" y="{y}" font-family="{MONO}" font-size="{size}" font-weight="{weight}" fill="{color}" '
            f'text-anchor="{anchor}" letter-spacing="1" opacity="{opacity}">{text}</text>')


def glitch_text(x, y, text, size, color=WHITE):
    """色差文字：洋紅、青色錯位疊在白字下面。"""
    base = f'font-family="{MONO}" font-size="{size}" font-weight="800" text-anchor="middle" letter-spacing="2"'
    return (f'<g style="mix-blend-mode:screen"><text x="{x - 3}" y="{y}" {base} fill="{MAGENTA}" opacity="0.85">{text}</text>'
            f'<text x="{x + 3}" y="{y + 1}" {base} fill="{CYAN}" opacity="0.85">{text}</text></g>'
            f'<text x="{x}" y="{y}" {base} fill="{color}">{text}</text>')


def enemy_bullet(x, y, r=9, kind="diamond"):
    """敵彈：暖色（紅／洋紅）＋白核心＋硬邊，和玩家的冷色一眼分開。"""
    if kind == "needle":
        g = (f'<path d="M{x},{y - r * 2.2} L{x + r * 0.55},{y} L{x},{y + r * 2.2} L{x - r * 0.55},{y} Z" fill="{ENEMY}"/>'
             f'<path d="M{x},{y - r * 1.4} L{x + r * 0.22},{y} L{x},{y + r * 1.4} L{x - r * 0.22},{y} Z" fill="{WHITE}"/>')
    else:
        g = (f'<path d="M{x},{y - r * 1.3} L{x + r * 1.3},{y} L{x},{y + r * 1.3} L{x - r * 1.3},{y} Z" fill="{ENEMY}"/>'
             f'<path d="M{x},{y - r * 0.6} L{x + r * 0.6},{y} L{x},{y + r * 0.6} L{x - r * 0.6},{y} Z" fill="{WHITE}"/>')
    return glow(g, 4)


def voxel(x, y, s, color, rot):
    return f'<rect x="{x - s / 2:.1f}" y="{y - s / 2:.1f}" width="{s:.1f}" height="{s:.1f}" fill="{color}" transform="rotate({rot} {x:.1f} {y:.1f})"/>'


def panel(px, py, no, title, sub, body):
    add(f'<g transform="translate({px} {py})">')
    add(f'<clipPath id="pc{no}"><rect width="{PW}" height="{PH}" rx="6"/></clipPath>')
    add(f'<g clip-path="url(#pc{no})">')
    add(f'<rect width="{PW}" height="{PH}" fill="url(#panelBg)"/>')
    add(f'<rect width="{PW}" height="{PH}" fill="url(#grid)"/>')
    add(body)
    add(f'<rect width="{PW}" height="{PH}" fill="url(#scan)"/>')
    add('</g>')
    # 科技感外框：切角＋角落標記
    c = 18
    add(f'<path d="M{c},0 L{PW},0 L{PW},{PH - c} L{PW - c},{PH} L0,{PH} L0,{c} Z" fill="none" stroke="{CYAN}" stroke-opacity="0.55" stroke-width="2"/>')
    add(f'<path d="M0,{c + 30} L0,{c} L{c},0 L{c + 30},0" fill="none" stroke="{CYAN}" stroke-width="4"/>')
    add(f'<path d="M{PW},{PH - c - 30} L{PW},{PH - c} L{PW - c},{PH} L{PW - c - 30},{PH}" fill="none" stroke="{CYAN}" stroke-width="4"/>')
    add(hud_text(30, 34, f"FX-{no:02d}", 13, CYAN, opacity=0.8))
    add(f'<text x="88" y="35" font-family="{CJK}" font-weight="bold" font-size="22" fill="{WHITE}">{title}</text>')
    add(f'<rect x="0" y="{PH - 46}" width="{PW}" height="46" fill="#05070D" opacity="0.82"/>')
    add(f'<text x="22" y="{PH - 17}" font-family="{CJK}" font-size="17" fill="#9FB3C8">{sub}</text>')
    add('</g>')


# ───────────── 開頭 ─────────────
add(f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">')
trails = "".join(
    f'<linearGradient id="trail_{c[1:]}" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="{c}" stop-opacity="0"/>'
    f'<stop offset="1" stop-color="{c}" stop-opacity="0.9"/></linearGradient>' for c in (CYAN, BLUE, VIOLET, AMBER, MAGENTA))
add(f'''<defs>{COMMON_DEFS}{trails}
<filter id="blur2" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="2"/></filter>
<filter id="blur3" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="3"/></filter>
<filter id="blur4" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="4"/></filter>
<filter id="blur5" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="5"/></filter>
<filter id="blur10" x="-80%" y="-80%" width="260%" height="260%"><feGaussianBlur stdDeviation="10"/></filter>
<filter id="blur20" x="-100%" y="-100%" width="300%" height="300%"><feGaussianBlur stdDeviation="20"/></filter>
<radialGradient id="bg" cx="50%" cy="30%" r="80%"><stop offset="0" stop-color="#0E1530"/><stop offset="1" stop-color="#03050B"/></radialGradient>
<linearGradient id="panelBg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#0A1022"/><stop offset="1" stop-color="#060912"/></linearGradient>
<pattern id="grid" width="28" height="28" patternUnits="userSpaceOnUse"><path d="M28,0 L0,0 L0,28" fill="none" stroke="{CYAN}" stroke-opacity="0.07" stroke-width="1"/></pattern>
<pattern id="scan" width="4" height="4" patternUnits="userSpaceOnUse"><rect width="4" height="1" fill="#000" opacity="0.28"/></pattern>
<pattern id="hexgrid" width="34" height="58.9" patternUnits="userSpaceOnUse"><polygon points="{hexagon(17, 14.7, 16, 30)}" fill="none" stroke="{CYAN}" stroke-width="1.5"/><polygon points="{hexagon(0, 44.2, 16, 30)}" fill="none" stroke="{CYAN}" stroke-width="1.5"/><polygon points="{hexagon(34, 44.2, 16, 30)}" fill="none" stroke="{CYAN}" stroke-width="1.5"/></pattern>
<radialGradient id="flash"><stop offset="0" stop-color="#fff"/><stop offset="0.35" stop-color="{CYAN}" stop-opacity="0.7"/><stop offset="1" stop-color="{CYAN}" stop-opacity="0"/></radialGradient>
<radialGradient id="flashV"><stop offset="0" stop-color="#fff"/><stop offset="0.35" stop-color="{VIOLET}" stop-opacity="0.7"/><stop offset="1" stop-color="{VIOLET}" stop-opacity="0"/></radialGradient>
<radialGradient id="flashR"><stop offset="0" stop-color="#fff"/><stop offset="0.3" stop-color="{AMBER}" stop-opacity="0.8"/><stop offset="1" stop-color="{ENEMY}" stop-opacity="0"/></radialGradient>
<radialGradient id="singularity"><stop offset="0" stop-color="#000"/><stop offset="0.55" stop-color="#000"/><stop offset="0.7" stop-color="{VIOLET}"/><stop offset="0.82" stop-color="#fff"/><stop offset="1" stop-color="{VIOLET}" stop-opacity="0"/></radialGradient>
<linearGradient id="beamV" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="{BLUE}" stop-opacity="0"/><stop offset="0.3" stop-color="{CYAN}"/><stop offset="0.5" stop-color="#fff"/><stop offset="0.7" stop-color="{CYAN}"/><stop offset="1" stop-color="{BLUE}" stop-opacity="0"/></linearGradient>
<linearGradient id="orbital" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="{AMBER}" stop-opacity="0"/><stop offset="0.3" stop-color="{AMBER}"/><stop offset="0.5" stop-color="#fff"/><stop offset="0.7" stop-color="{AMBER}"/><stop offset="1" stop-color="{AMBER}" stop-opacity="0"/></linearGradient>
</defs>''')
add(f'<rect width="{W}" height="{H}" fill="url(#bg)"/><rect width="{W}" height="{H}" fill="url(#grid)"/>')
add(f'<text x="60" y="92" font-family="{CJK}" font-weight="bold" font-size="50" fill="{WHITE}">攻擊特效設定板 v3</text>')
add(f'<text x="62" y="138" font-family="{CJK}" font-size="25" fill="{CYAN}">外表萌萌，出手是硬派科幻武器 ─ CUTE SHELL / HARDCORE PAYLOAD</text>')
add(hud_text(1140, 70, "SYS.FX // REV.3", 14, CYAN, "end", opacity=0.7))
add(hud_text(1140, 92, "PALETTE LOCKED", 14, CYAN, "end", opacity=0.7))
# 色票
sw = [("玩家主色", CYAN), ("玩家深藍", BLUE), ("能量紫", VIOLET), ("白熱核心", WHITE), ("敵彈警示紅", ENEMY), ("大絕琥珀", AMBER), ("鎖定洋紅", MAGENTA)]
for i, (n, c) in enumerate(sw):
    x = 62 + i * 156
    add(f'<rect x="{x}" y="160" width="40" height="22" fill="{c}"/>')
    add(f'<text x="{x + 48}" y="177" font-family="{CJK}" font-size="15" fill="#C9D6E3">{n}</text>')

col = (40, 600)
row = [210, 650, 1090, 1530]

# ───────────── 1. 主武器：電漿散射 ─────────────
b = []
b.append(f'<circle cx="280" cy="300" r="60" fill="url(#flash)" style="mix-blend-mode:screen"/>')
for i, a in enumerate((-24, -12, 0, 12, 24)):
    for k in range(3):
        d = 70 + k * 68
        x = 280 + math.sin(math.radians(a)) * d
        y = 290 - math.cos(math.radians(a)) * d
        b.append(bolt(x, y, a - 90, 30, 9, CYAN))
# 槍口：V 形噴焰＋六角閃光
b.append(glow(f'<path d="M262,276 L280,240 L298,276 L280,262 Z" fill="{WHITE}"/><polygon points="{hexagon(280, 262, 16)}" fill="none" stroke="{CYAN}" stroke-width="2"/>', 5))
# 彈殼：退出的發光彈殼（上面印著小肉球，唯一的萌點）
for (x, y, r) in ((345, 318, 30), (365, 345, 70), (205, 330, -40)):
    b.append(f'<g transform="translate({x} {y}) rotate({r})">' + glow(f'<rect x="-7" y="-3.5" width="14" height="7" rx="1.5" fill="{AMBER}"/>', 2) + '<circle cx="0" cy="0" r="1.6" fill="#5A3A10"/></g>')
b.append(cat_back(280, 340, 0.55))
b.append(hud_text(400, 90, "PLASMA.SCATTER", 13, CYAN))
b.append(hud_text(400, 108, "ROF 600 / 5-WAY", 12, CYAN, opacity=0.6))
panel(col[0], row[0], 1, "主武器・散射：電漿散彈", "白熱核心＋青色外殼的膠囊彈，槍口 V 形噴焰，退出發光彈殼", "".join(b))

# ───────────── 2. 主武器：磁軌光束 ─────────────
b = []
bx, top, bottom = 280, 150, 330
b.append(f'<rect x="{bx - 40}" y="{top}" width="80" height="{bottom - top}" fill="url(#beamV)" opacity="0.5" filter="url(#blur10)" style="mix-blend-mode:screen"/>')
b.append(f'<rect x="{bx - 16}" y="{top}" width="32" height="{bottom - top}" fill="url(#beamV)" style="mix-blend-mode:screen"/>')
b.append(f'<rect x="{bx - 3}" y="{top}" width="6" height="{bottom - top}" fill="#fff"/>')
# 螺旋線圈
coil = ""
for i in range(14):
    y = top + 18 + i * 15
    coil += f'<ellipse cx="{bx}" cy="{y}" rx="26" ry="5" fill="none" stroke="{CYAN}" stroke-width="2" opacity="{0.35 + 0.05 * (i % 4)}"/>'
b.append(glow(coil, 2))
# 電弧
for k in range(4):
    y0 = top + 40 + k * 50
    side = 1 if k % 2 else -1
    b.append(lightning(bx + side * 14, y0, bx + side * (60 + rnd.random() * 30), y0 + 25, segs=6, jitter=10))
# 命中：熔融火花＋六角衝擊＋目標
b.append(f'<circle cx="{bx}" cy="{top}" r="70" fill="url(#flash)" style="mix-blend-mode:screen"/>')
b.append(glow(f'<polygon points="{hexagon(bx, top, 46)}" fill="none" stroke="{CYAN}" stroke-width="3"/><polygon points="{hexagon(bx, top, 30, 30)}" fill="none" stroke="#fff" stroke-width="2"/>', 4))
b.append(sparks(bx, top, 26, 60, "#BFF8FF", spread=200, base=-90))
b.append(mouse(bx, 118, 0.75))
b.append(hud_text(bx + 70, top - 6, "HIT ▸ 1,280 DPS", 12, CYAN))
b.append(cat_back(bx, 372, 0.5))
b.append(hud_text(400, 350, "RAILGUN.MK2", 13, CYAN))
panel(col[1], row[0], 2, "主武器・集中：磁軌光束", "白色細核心＋螺旋線圈＋側邊電弧；命中處六角衝擊與熔融火花", "".join(b))

# ───────────── 3. 專武 ─────────────
b = []
# 左：橘貓 重力肉球砲（奇異點）
cx, cy = 150, 170
b.append(f'<circle cx="{cx}" cy="{cy}" r="78" fill="url(#flashV)" opacity="0.6" style="mix-blend-mode:screen"/>')
b.append(f'<circle cx="{cx}" cy="{cy}" r="52" fill="url(#singularity)"/>')
ring = f'<ellipse cx="{cx}" cy="{cy}" rx="70" ry="16" fill="none" stroke="{VIOLET}" stroke-width="3" transform="rotate(-18 {cx} {cy})"/>'
ring += f'<ellipse cx="{cx}" cy="{cy}" rx="58" ry="11" fill="none" stroke="#fff" stroke-width="1.5" transform="rotate(-18 {cx} {cy})"/>'
b.append(glow(ring, 4))
# 黑洞中央浮現的全息肉球
paw_h = "".join(f'<circle cx="{cx + dx}" cy="{cy + dy}" r="{r}" fill="none" stroke="{VIOLET}" stroke-width="1.6"/>' for dx, dy, r in ((0, 6, 11), (-12, -9, 5), (-4, -15, 5), (4, -15, 5), (12, -9, 5)))
b.append(glow(paw_h, 2, 0.9))
for k in range(10):
    a = math.radians(k * 36)
    b.append(f'<line x1="{cx + math.cos(a) * 110:.0f}" y1="{cy + math.sin(a) * 110:.0f}" x2="{cx + math.cos(a) * 75:.0f}" y2="{cy + math.sin(a) * 75:.0f}" stroke="{VIOLET}" stroke-width="2" stroke-dasharray="4 6" opacity="0.7"/>')
b.append(hud_text(cx, 290, "GRAVITON PAW", 13, VIOLET, "middle"))
b.append(hud_text(cx, 308, "吸入敵彈→爆縮", 12, VIOLET, "middle", 400) .replace(MONO, CJK))
b.append(cat_back(cx, 345, 0.45))
# 右：黑貓 單分子爪刃
rx = 410
for k in range(3):
    x = rx - 40 + k * 40
    for ghost in range(3):
        op = 1 - ghost * 0.3
        off = ghost * 14
        b.append(f'<g opacity="{op}">' + glow(f'<path d="M{x - 30},{250 + off} L{x + 30},{90 + off}" stroke="{CYAN}" stroke-width="{4 - ghost}" stroke-linecap="square"/><path d="M{x - 30},{250 + off} L{x + 30},{90 + off}" stroke="#fff" stroke-width="1"/>', 3) + '</g>')
# 切開時的像素碎裂
for k in range(22):
    x = rx - 60 + rnd.random() * 130
    y = 110 + rnd.random() * 140
    b.append(voxel(x, y, 3 + rnd.random() * 5, rnd.choice([CYAN, "#fff", BLUE]), rnd.random() * 45))
b.append(hud_text(rx, 290, "MONO-EDGE CLAW", 13, CYAN, "middle"))
b.append(hud_text(rx, 308, "殘影三連斬", 12, CYAN, "middle", 400).replace(MONO, CJK))
b.append(cat_back(rx, 345, 0.45, "black"))
b.append(f'<line x1="280" y1="70" x2="280" y2="350" stroke="{CYAN}" stroke-opacity="0.25" stroke-dasharray="4 6"/>')
panel(col[0], row[1], 3, "專武：重力肉球砲／單分子爪刃", "橘貓＝黑洞奇異點＋全息肉球；黑貓＝雷射刃殘影＋像素碎裂", "".join(b))

# ───────────── 4. 副武器：微型追蹤飛彈 ─────────────
b = []
targets = [(130, 110), (300, 90), (440, 150)]
for (tx, ty) in targets:
    b.append(mouse(tx, ty, 0.6))
    b.append(reticle(tx, ty, 34))
starts = [(250, 320), (300, 320), (270, 330)]
for i, ((tx, ty), (sx, sy)) in enumerate(zip(targets, starts)):
    mx, my = (sx + tx) / 2 + (-120 if i == 0 else 110 if i == 2 else 20), (sy + ty) / 2 + 40
    path = f"M{sx},{sy} Q{mx},{my} {tx},{ty + 40}"
    b.append(f'<path d="{path}" fill="none" stroke="#9FB3C8" stroke-width="5" stroke-opacity="0.25" filter="url(#blur3)"/>')
    b.append(glow(f'<path d="{path}" fill="none" stroke="{CYAN}" stroke-width="1.5" stroke-dasharray="2 5"/>', 2))
    a = math.degrees(math.atan2(ty + 40 - my, tx - mx))
    b.append(f'<g transform="translate({tx} {ty + 40}) rotate({a})">' + glow(f'<path d="M-30,0 L-6,0" stroke="{AMBER}" stroke-width="6" stroke-linecap="round"/><rect x="-8" y="-3" width="16" height="6" rx="2" fill="#E6EEF7"/><path d="M8,-3 L14,0 L8,3 Z" fill="{MAGENTA}"/>', 3) + '</g>')
b.append(cat_back(275, 375, 0.45))
b.append(hud_text(30, 76, "TARGETS 3/3 ▸ FIRE", 13, MAGENTA))
panel(col[1], row[1], 4, "副武器：微型追蹤飛彈", "先在目標上畫鎖定框（LOCK），再射出細長飛彈，琥珀尾焰＋淡煙軌", "".join(b))

# ───────────── 5. 副武器：六角力場盾 ─────────────
b = []
b.append(f'<path d="M80,250 Q280,60 480,250" fill="none" stroke="{CYAN}" stroke-width="44" stroke-opacity="0.12"/>')
b.append(f'<clipPath id="shieldClip"><path d="M70,262 Q280,48 490,262 L470,262 Q280,90 90,262 Z"/></clipPath>')
b.append(f'<g clip-path="url(#shieldClip)" style="mix-blend-mode:screen"><rect width="{PW}" height="{PH}" fill="url(#hexgrid)" opacity="0.55"/></g>')
b.append(glow(f'<path d="M70,262 Q280,48 490,262" fill="none" stroke="#fff" stroke-width="2.5"/>', 4))
# 被擋下的敵彈：漣漪＋亮起的六角
for (x, y) in ((190, 130), (330, 122), (420, 190)):
    b.append(glow(f'<polygon points="{hexagon(x, y + 14, 16, 30)}" fill="{CYAN}" fill-opacity="0.35" stroke="#fff" stroke-width="2"/>', 4))
    for r in (22, 36):
        b.append(f'<circle cx="{x}" cy="{y + 14}" r="{r}" fill="none" stroke="{CYAN}" stroke-width="1.5" opacity="{0.8 - r / 60}"/>')
    b.append(sparks(x, y + 10, 8, 18, ENEMY, spread=120, base=-90))
for (x, y, k) in ((150, 70, "diamond"), (260, 60, "needle"), (380, 80, "diamond"), (470, 110, "needle")):
    b.append(enemy_bullet(x, y, 8, k))
b.append(cat_back(280, 330, 0.5))
b.append(hud_text(30, 360, "SHIELD 82%", 13, CYAN))
panel(col[0], row[2], 5, "副武器：六角力場盾", "半透明六角網格的弧形力場；擋彈時該格亮起＋漣漪＋紅色火花", "".join(b))

# ───────────── 6. 命中與擊破 ─────────────
b = []
cx, cy = 290, 170
b.append(f'<circle cx="{cx}" cy="{cy}" r="120" fill="url(#flash)" style="mix-blend-mode:screen"/>')
b.append(glow(f'<circle cx="{cx}" cy="{cy}" r="96" fill="none" stroke="{CYAN}" stroke-width="4"/><circle cx="{cx}" cy="{cy}" r="128" fill="none" stroke="#fff" stroke-width="1.5" stroke-dasharray="10 8"/>', 5))
b.append(sparks(cx, cy, 34, 110, "#BFF8FF"))
# 小老鼠被「數位化」：一半還在，一半碎成方塊
b.append(f'<clipPath id="dissolve"><rect x="{cx - 60}" y="{cy - 60}" width="60" height="140"/></clipPath>')
b.append(f'<g clip-path="url(#dissolve)">{mouse(cx, cy, 0.95)}</g>')
for k in range(60):
    x = cx + rnd.random() * 150
    y = cy - 60 + rnd.random() * 130
    s = 3 + rnd.random() * 9
    c = rnd.choice(["#EDE6F5", "#D9CDE8", CYAN, "#fff", "#FFCADA"])
    b.append(voxel(x, y, s, c, rnd.random() * 90))
b.append(glitch_text(cx + 90, 70, "+500", 30))
b.append(hud_text(cx - 160, 300, "TARGET NEUTRALIZED", 14, CYAN))
b.append(hud_text(cx - 160, 320, "DMG 1,280 ▸ CRIT", 12, MAGENTA))
# 命中標記
hm = "".join(f'<line x1="{60 + sx * 10}" y1="{110 + sy * 10}" x2="{60 + sx * 22}" y2="{110 + sy * 22}" stroke="#fff" stroke-width="3"/>' for sx, sy in ((-1, -1), (1, -1), (1, 1), (-1, 1)))
b.append(glow(hm, 3))
b.append(hud_text(60, 150, "HIT", 12, WHITE, "middle"))
panel(col[1], row[2], 6, "命中與擊破：數位分解", "命中 X 標記＋火花；擊破：衝擊環＋體素碎塊＋色差分數", "".join(b))

# ───────────── 7. 消彈：資料化 ─────────────
b = []
for i in range(12):
    x, y = 70 + (i % 4) * 50, 90 + (i // 4) * 50
    b.append(enemy_bullet(x, y, 8, "diamond" if i % 2 else "needle"))
b.append(glow(f'<path d="M290,140 L340,140" stroke="{CYAN}" stroke-width="3"/><path d="M340,130 L355,140 L340,150 Z" fill="{CYAN}"/>', 3))
b.append(hud_text(322, 122, "DECOMPILE", 11, CYAN, "middle"))
bits = ""
for k in range(40):
    x = 380 + rnd.random() * 140
    y = 70 + rnd.random() * 160
    bits += f'<rect x="{x:.0f}" y="{y:.0f}" width="6" height="6" fill="{rnd.choice([CYAN, "#fff", BLUE])}"/>'
    if k % 4 == 0:
        bits += f'<text x="{x + 8:.0f}" y="{y + 6:.0f}" font-family="{MONO}" font-size="10" fill="{CYAN}" opacity="0.7">{rnd.choice(["01", "10", "FF", "0x3"])}</text>'
b.append(glow(bits, 2))
b.append(f'<path d="M440,200 Q380,300 300,330" fill="none" stroke="{CYAN}" stroke-width="2" stroke-dasharray="3 6" opacity="0.8"/>')
b.append(f'<path d="M500,160 Q420,290 310,340" fill="none" stroke="{CYAN}" stroke-width="2" stroke-dasharray="3 6" opacity="0.8"/>')
b.append(cat_back(280, 350, 0.45))
b.append(hud_text(360, 300, "+10 ×42", 16, CYAN))
b.append(hud_text(360, 320, "DATA ABSORBED", 12, CYAN, opacity=0.7))
panel(col[0], row[3], 7, "消彈：子彈分解成資料", "敵彈碎成青色資料方塊與十六進位字元，被貓咪吸收加分", "".join(b))

# ───────────── 8. 大絕：OVERDRIVE ─────────────
b = []
b.append(f'<rect width="{PW}" height="{PH}" fill="#000" opacity="0.4"/>')
# 軌道砲：從天而降的琥珀光柱
b.append(f'<rect x="230" y="50" width="100" height="270" fill="url(#orbital)" opacity="0.5" filter="url(#blur10)" style="mix-blend-mode:screen"/>')
b.append(f'<rect x="258" y="50" width="44" height="270" fill="url(#orbital)" style="mix-blend-mode:screen"/>')
b.append(f'<rect x="276" y="50" width="8" height="270" fill="#fff"/>')
b.append(f'<circle cx="280" cy="300" r="120" fill="url(#flashR)" style="mix-blend-mode:screen"/>')
for r in (90, 150, 210):
    b.append(glow(f'<polygon points="{hexagon(280, 300, r, 30)}" fill="none" stroke="{AMBER}" stroke-width="{4 - r / 80:.1f}" opacity="{1 - r / 260:.2f}"/>', 4))
# HUD 框
b.append(glow(f'<path d="M40,70 L40,50 L60,50 M500,50 L520,50 L520,70 M40,330 L40,350 L60,350 M500,350 L520,350 L520,330" fill="none" stroke="{AMBER}" stroke-width="3"/>', 3))
b.append(f'<rect x="60" y="66" width="440" height="80" fill="#05070D" opacity="0.85"/>')
b.append(glitch_text(280, 110, "OVERDRIVE", 44))
b.append(hud_text(280, 138, "ORBITAL STRIKE ▸ AUTHORIZED", 13, AMBER, "middle"))
for i in range(10):
    b.append(f'<rect x="{150 + i * 27}" y="152" width="20" height="8" fill="{AMBER if i < 10 else "#333"}" opacity="{0.4 + i * 0.06:.2f}"/>')
b.append(cat_back(280, 330, 0.5, "tiger"))
b.append(hud_text(60, 330, "吼～！", 22, "#fff").replace(MONO, CJK))
panel(col[1], row[3], 8, "大絕：OVERDRIVE 軌道砲", "寫實猛虎切入後，HUD 解鎖 → 天降琥珀光柱＋六角衝擊波清屏", "".join(b))

# ───────────── 原則 ─────────────
for i, line in enumerate(("原則：玩家＝冷色（青／藍／白）、敵人＝暖色（紅／洋紅），永不混用 ／ 硬邊、切角、六角形取代圓角與貼紙描邊",
                          "角色維持麻糬萌，只有「出手」是科技 ／ 特效依事件大小分級，敵彈永遠最清楚 ／ 大型擊破停格 80ms＋色差＋掃描線閃爍")):
    add(f'<text x="60" y="{1975 + i * 26}" font-family="{CJK}" font-size="18" fill="#9FB3C8">{line}</text>')
add("</svg>")

path = os.path.join(HERE, "effects-board-scifi.svg")
with open(path, "w") as f:
    f.write("\n".join(out))
print(path)
