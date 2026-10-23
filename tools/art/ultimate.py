"""大絕特寫：寫實漫畫風的猛虎／黑豹咆哮大頭，和平常的麻糬萌形成變身反差。

線條用「毛筆」錐形筆觸（兩端尖、中間粗），陰影用排線＋網點，眼睛有強烈的反光。
"""
import math
from common import Sprite

INK = "#140D12"


def quad(p0, p1, p2, t):
    a = (1 - t) ** 2
    b = 2 * (1 - t) * t
    c = t * t
    return (a * p0[0] + b * p1[0] + c * p2[0], a * p0[1] + b * p1[1] + c * p2[1])


def brush(p0, p1, p2, width, fill=INK, taper=0.75, extra="", n=18):
    """沿二次曲線的錐形筆觸（填色多邊形）。"""
    left, right = [], []
    for i in range(n + 1):
        t = i / n
        x, y = quad(p0, p1, p2, t)
        x2, y2 = quad(p0, p1, p2, min(1.0, t + 0.01)) if t < 1 else quad(p0, p1, p2, 1.0)
        if t >= 1:
            xa, ya = quad(p0, p1, p2, 0.99)
            dx, dy = x - xa, y - ya
        else:
            dx, dy = x2 - x, y2 - y
        d = math.hypot(dx, dy) or 1
        nx, ny = -dy / d, dx / d
        w = width * (math.sin(math.pi * t) ** taper) / 2
        left.append((x + nx * w, y + ny * w))
        right.append((x - nx * w, y - ny * w))
    pts = left + right[::-1]
    return f'<polygon points="{" ".join(f"{x:.1f},{y:.1f}" for x, y in pts)}" fill="{fill}" {extra}/>'


def mirror(fn):
    """畫左右對稱的一組筆觸：fn(s) 以 s=±1 產生。"""
    return fn(-1) + fn(1)


def hatch(x0, y0, x1, y1, count, length, angle, sw=3, color=INK, opacity=0.55):
    """一排平行排線。"""
    out = ""
    for i in range(count):
        t = i / max(1, count - 1)
        x = x0 + (x1 - x0) * t
        y = y0 + (y1 - y0) * t
        dx = math.cos(math.radians(angle)) * length
        dy = math.sin(math.radians(angle)) * length
        out += brush((x, y), (x + dx * 0.5, y + dy * 0.5), (x + dx, y + dy), sw, color, extra=f'opacity="{opacity}"', n=6)
    return out


def fur_outline(points, jag=26, seed=1, lean=0.35):
    """把一串輪廓點變成一撮撮往外、稍微往下垂的毛（回傳 path 的 d）。"""
    import random
    rnd = random.Random(seed)
    d = f"M{points[0][0]:.1f},{points[0][1]:.1f}"
    for i in range(len(points) - 1):
        (x0, y0), (x1, y1) = points[i], points[i + 1]
        dx, dy = x1 - x0, y1 - y0
        dist = math.hypot(dx, dy) or 1
        tx, ty = dx / dist, dy / dist
        nx, ny = ty, -tx
        t = 0.0
        while t < 1.0:
            step = (22 + rnd.random() * 30) / dist
            t2 = min(1.0, t + step)
            bx, by = x0 + dx * t2, y0 + dy * t2
            mx, my = x0 + dx * (t + t2) / 2, y0 + dy * (t + t2) / 2
            j = jag * (0.4 + rnd.random() * 1.0)
            tipx = mx + nx * j + tx * j * lean
            tipy = my + ny * j + ty * j * lean + j * 0.25
            c1x, c1y = x0 + dx * t + nx * j * 0.55, y0 + dy * t + ny * j * 0.55
            d += f" Q{c1x:.1f},{c1y:.1f} {tipx:.1f},{tipy:.1f} Q{mx + nx * j * 0.2:.1f},{my + ny * j * 0.2:.1f} {bx:.1f},{by:.1f}"
            t = t2
    return d + " Z"


def fur_path(points, fill, jag=26, seed=1, extra=""):
    return f'<path d="{fur_outline(points, jag, seed)}" fill="{fill}" {extra}/>'


def fur_strokes(count, color, seed, opacity=0.45):
    """臉上順著毛流的細筆觸，增加寫實質感。"""
    import random
    rnd = random.Random(seed)
    out = ""
    n = 0
    while n < count:
        x = (rnd.random() * 2 - 1) * 330
        y = -300 + rnd.random() * 600
        if (x / 340) ** 2 + ((y - 40) / 380) ** 2 > 0.9:
            continue
        if abs(x) < 230 and -150 < y < 0 and abs(abs(x) - 140) < 90:
            continue  # 眼睛
        if abs(x) < 220 and y > 0:
            continue  # 口鼻
        a = math.atan2(y + 60, x)
        L = 26 + rnd.random() * 30
        x2, y2 = x + math.cos(a) * L, y + math.sin(a) * L + L * 0.3
        out += brush((x, y), ((x + x2) / 2 + rnd.random() * 6, (y + y2) / 2), (x2, y2), 4 + rnd.random() * 3, color, extra=f'opacity="{opacity}"', n=6)
        n += 1
    return out


def poly(pts, fill, extra=""):
    return f'<polygon points="{" ".join(f"{x:.1f},{y:.1f}" for x, y in pts)}" fill="{fill}" {extra}/>'


def head(kind):
    tiger = kind == "tiger"
    fur_hi = "#FFC46B" if tiger else "#4A4366"
    fur_mid = "#F28A1E" if tiger else "#25213A"
    fur_lo = "#B4520E" if tiger else "#0E0B18"
    white = "#FFF6E8" if tiger else "#3A3552"
    eye = ("#FFE45C", "#E58A00") if tiger else ("#E9FF7A", "#7FCF1A")
    rim = "#FFFFFF" if tiger else "#A894FF"
    nose = "#E89A8A" if tiger else "#1A1524"

    defs = f"""
<radialGradient id="faceG" cx="50%" cy="38%" r="62%"><stop offset="0" stop-color="{fur_hi}"/><stop offset="0.55" stop-color="{fur_mid}"/><stop offset="1" stop-color="{fur_lo}"/></radialGradient>
<radialGradient id="padG" cx="50%" cy="35%" r="70%"><stop offset="0" stop-color="#FFFFFF"/><stop offset="1" stop-color="{white}"/></radialGradient>
<radialGradient id="throatG" cx="50%" cy="30%" r="70%"><stop offset="0" stop-color="#3A0612"/><stop offset="1" stop-color="#7A1426"/></radialGradient>
<linearGradient id="tongueG" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#D9475E"/><stop offset="1" stop-color="#FF8A9A"/></linearGradient>
<radialGradient id="irisG" cx="45%" cy="40%" r="60%"><stop offset="0" stop-color="{eye[0]}"/><stop offset="1" stop-color="{eye[1]}"/></radialGradient>
<linearGradient id="fangG" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FFFDF5"/><stop offset="1" stop-color="#E8DCC2"/></linearGradient>
<pattern id="tone" width="9" height="9" patternUnits="userSpaceOnUse" patternTransform="rotate(45)"><circle cx="4.5" cy="4.5" r="2.1" fill="{INK}"/></pattern>
<radialGradient id="eyeGlow"><stop offset="0" stop-color="{eye[0]}" stop-opacity="0.9"/><stop offset="1" stop-color="{eye[0]}" stop-opacity="0"/></radialGradient>
"""
    out = ""
    # ── 輪廓（含頰毛） ──
    outline = [(0, -330), (-150, -318), (-262, -250), (-318, -150), (-340, -30), (-372, 70), (-392, 160),
               (-340, 250), (-250, 330), (-140, 400), (0, 432), (140, 400), (250, 330), (340, 250), (392, 160),
               (372, 70), (340, -30), (318, -150), (262, -250), (150, -318), (0, -330)]
    seed = 7 if tiger else 9
    # 耳朵
    def ear(s):
        o = f'<path d="M{s * 140},-290 Q{s * 160},-440 {s * 262},-440 Q{s * 360},-430 {s * 336},-250 Z" fill="{fur_mid}" stroke="{INK}" stroke-width="14" stroke-linejoin="round"/>'
        o += f'<path d="M{s * 180},-300 Q{s * 196},-400 {s * 262},-404 Q{s * 318},-396 {s * 304},-290 Z" fill="{"#2A1A1A" if tiger else "#100C18"}"/>'
        o += f'<path d="M{s * 160},-330 Q{s * 200},-430 {s * 262},-432" fill="none" stroke="{rim}" stroke-width="6" opacity="0.6"/>' 
        for k in range(6):
            x = s * (205 + k * 16)
            o += brush((x, -300), (x + s * 4, -350), (x + s * 10, -385 + k * 6), 7, white, extra='opacity="0.9"', n=8)
        return o
    out += mirror(ear)
    out += fur_path(outline, rim, 34, seed, f'stroke="{rim}" stroke-width="34" stroke-linejoin="round" opacity="0.9"')
    out += fur_path(outline, "url(#faceG)", 34, seed, f'stroke="{INK}" stroke-width="12" stroke-linejoin="round"')
    out += fur_strokes(160, fur_lo if tiger else "#6A6290", seed, 0.5)

    # 臉側陰影（網點）
    out += fur_path([(-340, -30), (-372, 70), (-392, 160), (-340, 250), (-250, 330), (-250, 120), (-290, -30)], "url(#tone)", 22, 3, 'opacity="0.4"')
    out += fur_path([(340, -30), (372, 70), (392, 160), (340, 250), (250, 330), (250, 120), (290, -30)], "url(#tone)", 22, 4, 'opacity="0.4"')

    # 眼睛上方的白色眉斑、頰邊的白毛
    out += mirror(lambda s: f'<ellipse cx="{s * 160}" cy="-200" rx="64" ry="26" fill="{white}" opacity="0.8" transform="rotate({s * -14} {s * 160} -200)"/>')
    out += mirror(lambda s: fur_path([(s * 250, 40), (s * 330, 120), (s * 300, 240), (s * 220, 300), (s * 200, 160)], white, 18, 11, 'opacity="0.9"'))

    # ── 條紋（虎）／隱約的玫瑰斑（黑豹） ──
    if tiger:
        for y, w0, w1 in ((-300, 26, 90), (-262, 30, 120), (-224, 28, 110)):
            out += mirror(lambda s, y=y, w0=w0, w1=w1: brush((s * w0, y), (s * (w0 + w1) * 0.55, y - 24), (s * (w0 + w1), y + 6), 22))
        out += brush((0, -320), (0, -280), (0, -238), 22)
        out += mirror(lambda s: brush((s * 180, -230), (s * 240, -226), (s * 290, -180), 24))
        for (a, b, c, w) in (((-330, -40), (-280, -10), (-220, 20), 30), ((-360, 60), (-300, 80), (-240, 110), 32),
                             ((-372, 150), (-320, 168), (-262, 190), 28), ((-310, -120), (-270, -96), (-236, -60), 22)):
            out += brush(a, b, c, w) + brush((-a[0], a[1]), (-b[0], b[1]), (-c[0], c[1]), w)
    else:
        for (x, y, r) in ((-60, -260, 26), (60, -255, 24), (0, -215, 20), (-150, -230, 22), (150, -228, 22), (-110, -290, 18), (110, -292, 18),
                          (-300, 60, 24), (300, 60, 24), (-320, 150, 22), (320, 150, 22)):
            out += f'<circle cx="{x}" cy="{y}" r="{r}" fill="none" stroke="#000000" stroke-width="9" opacity="0.45" stroke-dasharray="18 10"/>'
        # 黑豹的邊緣光
        out += mirror(lambda s: brush((s * 300, -210), (s * 360, -60), (s * 380, 110), 18, rim, extra='opacity="0.85"'))
        out += mirror(lambda s: brush((s * 120, -318), (s * 230, -300), (s * 300, -230), 14, rim, extra='opacity="0.7"'))

    # ── 皺起的眉間與鼻樑 ──
    out += mirror(lambda s: brush((s * 30, -175), (s * 50, -130), (s * 40, -80), 12))
    # 往眉心壓下來的怒眉＋眉骨陰影
    out += mirror(lambda s: f'<path d="M{s * 40},-112 Q{s * 120},-176 {s * 240},-186 Q{s * 150},-150 {s * 60},-96 Z" fill="url(#tone)" opacity="0.55"/>')
    out += mirror(lambda s: brush((s * 36, -104), (s * 120, -160), (s * 240, -178), 26))
    out += mirror(lambda s: brush((s * 70, -128), (s * 130, -178), (s * 210, -206), 10))
    out += hatch(-60, -60, 60, -60, 1, 0, 0)
    for y in (-46, -16, 12):
        out += brush((-62, y + 6), (0, y - 14), (62, y + 6), 10)
    out += hatch(-70, -120, -70, 30, 8, 40, 200, 3, INK, 0.5) + hatch(70, -120, 70, 30, 8, 40, -20, 3, INK, 0.5)

    # ── 眼睛 ──
    def eye_fn(s):
        o = f'<ellipse cx="{s * 135}" cy="-96" rx="90" ry="60" fill="url(#eyeGlow)"/>'
        lid = [(s * 64, -72), (s * 110, -118), (s * 170, -132), (s * 214, -120), (s * 182, -86), (s * 120, -70)]
        o += poly(lid, "#FFFDF0", f'stroke="{INK}" stroke-width="6"')
        o += f'<clipPath id="eyeClip{s}"><polygon points="{" ".join(f"{x},{y}" for x, y in lid)}"/></clipPath>'
        o += f'<g clip-path="url(#eyeClip{s})"><circle cx="{s * 142}" cy="-100" r="38" fill="url(#irisG)" stroke="{INK}" stroke-width="5"/>'
        o += f'<circle cx="{s * 142}" cy="-100" r="15" fill="{INK}"/>'
        o += f'<rect x="{s * 142 - 60}" y="-140" width="120" height="24" fill="{INK}" opacity="0.35"/></g>'
        o += f'<circle cx="{s * 150}" cy="-110" r="7" fill="#FFFFFF"/><circle cx="{s * 132}" cy="-90" r="3.5" fill="#FFFFFF"/>'
        # 上眼線（粗）與往下延伸的淚痕
        o += brush((s * 56, -66), (s * 140, -142), (s * 226, -122), 18)
        o += brush((s * 66, -66), (s * 70, -20), (s * 86, 24), 16)
        o += brush((s * 120, -66), (s * 160, -74), (s * 196, -90), 8)
        return o
    out += mirror(eye_fn)

    # ── 口鼻 ──
    out += mirror(lambda s: f'<ellipse cx="{s * 112}" cy="96" rx="104" ry="78" fill="url(#padG)" stroke="{INK}" stroke-width="7"/>')
    for row, y in enumerate((70, 98, 126)):
        for k in range(4):
            out += mirror(lambda s, y=y, k=k, row=row: f'<circle cx="{s * (70 + k * 32 + row * 6)}" cy="{y + k * 4}" r="5" fill="{INK}" opacity="0.8"/>')
    # 上唇兩側的咆哮皺紋
    for k in range(3):
        out += mirror(lambda s, k=k: brush((s * (96 + k * 26), 30 + k * 6), (s * (120 + k * 30), 60 + k * 10), (s * (112 + k * 34), 96 + k * 14), 8))
    # 鼻頭
    out += f'<path d="M-82,22 Q0,0 82,22 Q60,64 14,84 L0,90 L-14,84 Q-60,64 -82,22 Z" fill="{nose}" stroke="{INK}" stroke-width="9" stroke-linejoin="round"/>'
    out += '<path d="M-44,30 Q-10,20 10,30" fill="none" stroke="#FFFFFF" stroke-width="8" stroke-linecap="round" opacity="0.6"/>'
    out += mirror(lambda s: brush((s * 6, 76), (s * 30, 64), (s * 54, 58), 12))
    out += brush((0, 86), (0, 108), (0, 130), 12)

    # 張大的嘴
    mouth = "M-150,170 Q-120,132 -40,130 Q0,140 40,130 Q120,132 150,170 Q150,270 110,330 Q60,372 0,374 Q-60,372 -110,330 Q-150,270 -150,170 Z"
    out += f'<path d="{mouth}" fill="url(#throatG)" stroke="{INK}" stroke-width="14" stroke-linejoin="round"/>'
    out += '<path d="M-96,330 Q-50,250 0,262 Q50,250 96,330 Q60,366 0,368 Q-60,366 -96,330 Z" fill="url(#tongueG)"/>'
    out += brush((0, 270), (0, 310), (0, 352), 7, "#A8283E")
    out += hatch(-120, 180, 120, 180, 12, 50, 90, 4, "#000000", 0.35)
    # 上排小門牙
    for k in range(-2, 3):
        out += f'<path d="M{k * 18 - 8},140 L{k * 18 + 8},140 L{k * 18 + 5},160 L{k * 18 - 5},160 Z" fill="url(#fangG)" stroke="{INK}" stroke-width="4"/>'
    # 犬齒
    out += mirror(lambda s: f'<path d="M{s * 72},138 Q{s * 112},140 {s * 118},150 Q{s * 104},220 {s * 86},284 Q{s * 76},220 {s * 72},138 Z" fill="url(#fangG)" stroke="{INK}" stroke-width="6" stroke-linejoin="round"/>')
    out += mirror(lambda s: f'<path d="M{s * 80},350 Q{s * 112},344 {s * 118},332 Q{s * 108},290 {s * 98},248 Q{s * 86},300 {s * 80},350 Z" fill="url(#fangG)" stroke="{INK}" stroke-width="6" stroke-linejoin="round"/>')
    out += mirror(lambda s: f'<path d="M{s * 84},150 Q{s * 92},200 {s * 88},250" fill="none" stroke="#FFFFFF" stroke-width="5" stroke-linecap="round" opacity="0.8"/>')
    # 上唇的黑邊
    out += mirror(lambda s: brush((s * 4, 128), (s * 90, 118), (s * 156, 176), 16))
    # 下巴白毛
    out += fur_path([(-110, 340), (-60, 392), (0, 410), (60, 392), (110, 340)], white, 20, 5, f'stroke="{INK}" stroke-width="6" stroke-linejoin="round"')

    # 鬍鬚
    for k, (dy, ang) in enumerate(((60, -8), (90, 2), (118, 12), (146, 22))):
        out += mirror(lambda s, dy=dy, ang=ang: brush((s * 170, dy), (s * 320, dy + ang * 2.5), (s * 480, dy + ang * 6), 5, "#FFFFFF" if not tiger else "#FFFDF5", 0.4))

    # 整體排線：額頭上方的速度感
    out += hatch(-200, -340, 200, -340, 14, 46, -70, 3, INK, 0.35)
    return Sprite(f"ult_{kind}", 900, 900, "-500 -480 1000 1000", out, defs)


def all_sprites():
    return [head("tiger"), head("panther")]
