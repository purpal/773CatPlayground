"""喵神封魔錄的繪圖工具：比「麻糬萌」多一層質感——漸層體積、邊緣光、金屬與寶石、發光。

每張圖用一個 Defs 收集漸層與濾鏡（id 自動編號），最後交給 Sprite。
角色都畫在以 (0,0) 為身體中心的座標裡：頭頂約 y=-70、腳底約 y=88。
"""
import math
from common import Sprite, ellipse, path, mochi_path, ear, star_points

VIEW = "-140 -160 280 280"


class Defs:
    def __init__(self):
        self.items = []
        self.n = 0

    def _id(self):
        self.n += 1
        return f"d{self.n}"

    def lin(self, stops, x1=0, y1=0, x2=0, y2=1):
        i = self._id()
        body = "".join(f'<stop offset="{o}" stop-color="{c}"/>' if len(s) == 2 else f'<stop offset="{o}" stop-color="{c}" stop-opacity="{s[2]}"/>'
                       for s in stops for o, c in [s[:2]])
        self.items.append(f'<linearGradient id="{i}" x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}">{body}</linearGradient>')
        return f"url(#{i})"

    def rad(self, stops, cx=0.5, cy=0.5, r=0.5, fx=None, fy=None):
        i = self._id()
        body = "".join(f'<stop offset="{o}" stop-color="{c}"/>' if len(s) == 2 else f'<stop offset="{o}" stop-color="{c}" stop-opacity="{s[2]}"/>'
                       for s in stops for o, c in [s[:2]])
        f = f' fx="{fx}" fy="{fy}"' if fx is not None else ""
        self.items.append(f'<radialGradient id="{i}" cx="{cx}" cy="{cy}" r="{r}"{f}>{body}</radialGradient>')
        return f"url(#{i})"

    def blur(self, std):
        i = self._id()
        self.items.append(f'<filter id="{i}" x="-60%" y="-60%" width="220%" height="220%"><feGaussianBlur stdDeviation="{std}"/></filter>')
        return f"url(#{i})"

    def clip(self, d):
        i = self._id()
        self.items.append(f'<clipPath id="{i}"><path d="{d}"/></clipPath>')
        return f"url(#{i})"

    def text(self):
        return "".join(self.items)


# ───────────── 材質 ─────────────

def gold(d, vertical=True):
    stops = [(0, "#FFF6C2"), (0.35, "#FFD24A"), (0.7, "#E79A1C"), (1, "#B8690F")]
    return d.lin(stops) if vertical else d.lin(stops, 0, 0, 1, 0)


def steel(d):
    return d.lin([(0, "#FFFFFF"), (0.4, "#C9D3E2"), (0.75, "#8793A8"), (1, "#5E6980")])


def jade(d):
    return d.lin([(0, "#D6FFE9"), (0.5, "#5FD39A"), (1, "#1F8A5B")])


GOLD_LINE = "#8A4B0C"
STEEL_LINE = "#3F485C"


def glow(d, cx, cy, r, color, opacity=0.9):
    g = d.rad([(0, color, opacity), (0.5, color, opacity * 0.35), (1, color, 0)])
    return f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{g}"/>'


def gem(d, cx, cy, r, color, line="#3A2140"):
    g = d.rad([(0, "#FFFFFF"), (0.35, color), (1, line)], 0.35, 0.3, 0.8)
    return (f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{g}" stroke="{line}" stroke-width="{max(1.5, r * 0.22):.1f}"/>'
            f'<circle cx="{cx - r * 0.3:.1f}" cy="{cy - r * 0.35:.1f}" r="{r * 0.22:.1f}" fill="#FFFFFF" opacity="0.9"/>')


def shine(d_path, width=3, opacity=0.6):
    return f'<path d="{d_path}" fill="none" stroke="#FFFFFF" stroke-width="{width}" stroke-linecap="round" opacity="{opacity}"/>'


def poly(points, fill, stroke, sw=3.5, extra=""):
    pts = " ".join(f"{x:.1f},{y:.1f}" for x, y in points)
    return f'<polygon points="{pts}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}" stroke-linejoin="round" {extra}/>'


def rrect(x, y, w, h, r, fill, stroke, sw=3.5, extra=""):
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}" {extra}/>'


def rot(content, deg, cx=0, cy=0):
    return f'<g transform="rotate({deg} {cx} {cy})">{content}</g>'


def move(content, dx, dy, scale=1.0):
    return f'<g transform="translate({dx} {dy}) scale({scale})">{content}</g>'


def flip(content):
    return f'<g transform="scale(-1 1)">{content}</g>'


def shaft(d, x, y1, y2, w, fill, line, caps=None):
    """直立的棍子／槍桿；caps 是兩端的金屬箍顏色。"""
    out = rrect(x - w / 2, y1, w, y2 - y1, w / 2, fill, line, 3)
    out += f'<line x1="{x - w * 0.18}" y1="{y1 + 6}" x2="{x - w * 0.18}" y2="{y2 - 6}" stroke="#FFFFFF" stroke-width="{w * 0.18:.1f}" stroke-linecap="round" opacity="0.55"/>'
    if caps:
        for y in (y1, y2 - 16):
            out += rrect(x - w / 2 - 2, y, w + 4, 16, 4, caps, GOLD_LINE, 2.5)
    return out


def bolt(points_scale=1.0, cx=0, cy=0, fill="#FFF7A8", line="#D98A1A", glow_color="#FFE45C", d=None):
    pts = [(-8, -60), (16, -60), (2, -16), (22, -16), (-14, 60), (-4, 6), (-24, 6)]
    pts = [(cx + x * points_scale, cy + y * points_scale) for x, y in pts]
    out = ""
    if d is not None:
        out += glow(d, cx, cy, 70 * points_scale, glow_color, 0.8)
    return out + poly(pts, fill, line, 3)


def cloud(cx, cy, s, fill="#FFFFFF", line="#B9C6E4", sw=3, opacity=1.0):
    """一朵圓滾滾的雲（祥雲、筋斗雲、暴風雲）。"""
    out = f'<g opacity="{opacity}">'
    blobs = [(-0.9, 0.1, 0.5), (-0.35, -0.25, 0.62), (0.35, -0.18, 0.58), (0.9, 0.12, 0.46), (0, 0.2, 0.7)]
    for bx, by, br in blobs:
        out += f'<circle cx="{cx + bx * s:.1f}" cy="{cy + by * s:.1f}" r="{br * s:.1f}" fill="{fill}" stroke="{line}" stroke-width="{sw}"/>'
    for bx, by, br in blobs:
        out += f'<circle cx="{cx + bx * s:.1f}" cy="{cy + by * s:.1f}" r="{br * s - sw / 2:.1f}" fill="{fill}"/>'
    out += f'<path d="M{cx - s * 0.5:.1f},{cy + s * 0.1:.1f} q{s * 0.25:.1f},{-s * 0.3:.1f} {s * 0.5:.1f},0" fill="none" stroke="{line}" stroke-width="{sw * 0.8}" stroke-linecap="round" opacity="0.7"/>'
    return out + "</g>"


def flame(d, cx, cy, s, inner="#FFF3A0", mid="#FF9A2E", outer="#E8421E"):
    """一簇火焰，底在 (cx, cy)，高約 s。"""
    g = d.lin([(0, inner), (0.5, mid), (1, outer)], 0, 1, 0, 0)
    body = (f"M{cx},{cy} C{cx - s * 0.55},{cy - s * 0.1} {cx - s * 0.45},{cy - s * 0.6} {cx - s * 0.1},{cy - s * 0.75} "
            f"C{cx - s * 0.15},{cy - s * 0.5} {cx + s * 0.1},{cy - s * 0.55} {cx + s * 0.05},{cy - s} "
            f"C{cx + s * 0.5},{cy - s * 0.7} {cx + s * 0.6},{cy - s * 0.2} {cx},{cy} Z")
    core = (f"M{cx},{cy - s * 0.05} C{cx - s * 0.25},{cy - s * 0.1} {cx - s * 0.2},{cy - s * 0.4} {cx},{cy - s * 0.55} "
            f"C{cx + s * 0.22},{cy - s * 0.4} {cx + s * 0.25},{cy - s * 0.1} {cx},{cy - s * 0.05} Z")
    return f'<path d="{body}" fill="{g}" stroke="{outer}" stroke-width="2.5" stroke-linejoin="round"/><path d="{core}" fill="{inner}" opacity="0.9"/>'


def ground_shadow(d, w=74, y=92):
    f = d.blur(5)
    return f'<ellipse cx="0" cy="{y}" rx="{w}" ry="13" fill="#1A1030" opacity="0.45" filter="{f}"/>'


# ───────────── 貓咪本體 ─────────────

FURS = {
    "cream": dict(base="#FFF3DC", light="#FFFFFF", dark="#E6C9A0", line="#8A6248", inner="#FFC2D1", stripe=None, belly="#FFFFFF"),
    "white": dict(base="#FBFBFF", light="#FFFFFF", dark="#C9CEE6", line="#6B6F92", inner="#FFC2D1", stripe=None, belly="#FFFFFF"),
    "orange": dict(base="#FFB45C", light="#FFD79A", dark="#E8853A", line="#8A4320", inner="#FFD0DA", stripe="#D96F27", belly="#FFF4E0"),
    "gold": dict(base="#FFD166", light="#FFF0B8", dark="#E79A2B", line="#8A5312", inner="#FFDDE3", stripe="#E08A1E", belly="#FFF8DF"),
    "brown": dict(base="#C98A55", light="#E8B585", dark="#9A5F33", line="#5A3018", inner="#FFC2C9", stripe="#8A5229", belly="#F6DFC4"),
    "gray": dict(base="#AEB6CC", light="#DCE2F2", dark="#7C84A0", line="#3F4560", inner="#FFC2D1", stripe="#727A98", belly="#F2F4FB"),
    "silver": dict(base="#DDE4F2", light="#FFFFFF", dark="#A6B0CC", line="#555E80", inner="#FFCBDA", stripe=None, belly="#FFFFFF"),
    "black": dict(base="#4A4563", light="#6E688C", dark="#2C2840", line="#17142A", inner="#C77DA6", stripe=None, belly="#5A5478"),
    "blue": dict(base="#7FB6E8", light="#BFE0FF", dark="#4A7FBE", line="#23426E", inner="#FFD0E0", stripe="#4F86C6", belly="#E6F4FF"),
    "teal": dict(base="#5FCFC0", light="#B4F2E6", dark="#2E9C92", line="#135A58", inner="#FFD0E0", stripe="#2FA39A", belly="#E2FFF8"),
    "red": dict(base="#E8705A", light="#FFA58C", dark="#B8432F", line="#6B1E14", inner="#FFD0C8", stripe=None, belly="#FFC6B4"),
    "navy": dict(base="#5C6FA8", light="#8EA0D6", dark="#394878", line="#1B2444", inner="#F0A8C8", stripe=None, belly="#8EA0D6"),
}

BODY = mochi_path(0, 10, 172, 152)


def cat_body(d, fur, tail=True, stripes=True, tail_side=1):
    """尾巴、耳朵、麻糬身體（漸層＋暗面＋邊緣光）。"""
    p = FURS[fur]
    line = p["line"]
    out = ""
    if tail:
        t = f"M{tail_side * 60},52 C{tail_side * 104},50 {tail_side * 112},4 {tail_side * 92},-18"
        out += path(t, "none", line, 19, 'stroke-linecap="round"') + path(t, "none", p["base"], 12, 'stroke-linecap="round"')
        out += path(f"M{tail_side * 96},-8 C{tail_side * 102},-14 {tail_side * 98},-20 {tail_side * 92},-18", "none", p["light"], 6, 'stroke-linecap="round" opacity="0.8"')
    ear_fill = d.lin([(0, p["light"]), (1, p["base"])])
    for s in (-1, 1):
        out += path(ear(s * 48, -94, s * 74, -42, s * 20, -60), ear_fill, line, 4.5, 'stroke-linejoin="round"')
        out += path(ear(s * 47, -80, s * 62, -47, s * 30, -58), p["inner"], "none")
    body_fill = d.rad([(0, p["light"]), (0.45, p["base"]), (1, p["dark"])], 0.38, 0.3, 0.8)
    out += path(BODY, body_fill, line, 4.5)
    clip = d.clip(BODY)
    inner = ""
    # 底部的暗面與肚子
    inner += ellipse(0, 96, 96, 34, p["dark"], "none", None, 'opacity="0.55"')
    inner += ellipse(0, 58, 46, 30, p["belly"], "none", None, 'opacity="0.9"')
    if stripes and p["stripe"]:
        for k in (-1, 0, 1):
            inner += path(f"M{k * 15 - 5},-70 L{k * 15 + 5},-70 L{k * 15},{-50 + abs(k) * 6} Z", p["stripe"], "none")
        for s in (-1, 1):
            inner += path(f"M{s * 90},-4 L{s * 66},0 M{s * 90},14 L{s * 68},15", "none", p["stripe"], 6, 'stroke-linecap="round"')
    # 邊緣光
    inner += path("M-62,-36 C-48,-58 -20,-66 6,-66", "none", "#FFFFFF", 7, 'stroke-linecap="round" opacity="0.5"')
    out += f'<g clip-path="{clip}">{inner}</g>'
    return out


def eye(d, cx, cy, r, iris, look=0.0):
    """有神的大眼睛：虹膜漸層、瞳孔、兩顆高光。"""
    g = d.rad([(0, "#FFFFFF"), (0.25, iris), (1, "#1B1226")], 0.5, 0.75, 0.75)
    out = ellipse(cx, cy, r, r * 1.18, g, "#1B1226", 2.8)
    out += ellipse(cx + look, cy + r * 0.05, r * 0.42, r * 0.62, "#140D1E", "none")
    out += ellipse(cx + r * 0.36, cy - r * 0.45, r * 0.34, r * 0.34, "#FFFFFF", "none")
    out += ellipse(cx - r * 0.34, cy + r * 0.5, r * 0.16, r * 0.16, "#FFFFFF", "none", None, 'opacity="0.85"')
    return out


def face(d, fur, iris="#6B3F2A", mood="calm", eye_r=12, brow=None, whisker=True, nose="#FF8FA8"):
    """mood: calm（平靜）、smile（笑瞇眼）、fierce（怒目）、cool（半閉）。"""
    p = FURS[fur]
    line = p["line"]
    out = ""
    for s in (-1, 1):
        x = s * 31
        if mood == "smile":
            out += path(f"M{x - 11},2 Q{x},-10 {x + 11},2", "none", line, 4.5, 'stroke-linecap="round"')
        else:
            out += eye(d, x, 2, eye_r, iris, look=-s * 0.5)
            if mood == "fierce":
                out += path(f"M{x - s * 15},-9 L{x + s * 14},-20", "none", brow or line, 6, 'stroke-linecap="round"')
            elif mood == "cool":
                out += path(f"M{x - 14},-8 L{x + 14},-8 L{x + 14},-16 L{x - 14},-16 Z", p["base"], "none")
                out += path(f"M{x - 13},-7 L{x + 13},-7", "none", line, 4, 'stroke-linecap="round"')
            elif brow:
                out += path(f"M{x - 11},-18 Q{x},-23 {x + 11},-18", "none", brow, 5, 'stroke-linecap="round"')
    blush = d.rad([(0, "#FF7FA6", 0.7), (1, "#FF7FA6", 0)])
    for s in (-1, 1):
        out += ellipse(s * 54, 24, 17, 10, blush, "none")
    out += ellipse(0, 17, 5.5, 4, nose, line, 2)
    if mood == "fierce":
        out += path("M-10,27 Q-5,32 0,26 Q5,32 10,27", "none", line, 3.2, 'stroke-linecap="round"')
        out += path("M-6,28 L-4,33 L-2,28 Z M6,28 L4,33 L2,28 Z", "#FFFFFF", line, 1.2)
    else:
        out += path("M-9,25 Q-4.5,32 0,25 Q4.5,32 9,25", "none", line, 3.2, 'stroke-linecap="round"')
    if whisker:
        for s in (-1, 1):
            out += path(f"M{s * 42},18 Q{s * 66},12 {s * 88},16 M{s * 42},26 Q{s * 64},28 {s * 86},36", "none", line, 2, 'stroke-linecap="round" opacity="0.55"')
    return out


def robe(d, top, bottom, trim=None, line="#5A2A18", neck=34, sash=None, collar=None):
    """衣服：蓋住身體下半部（用身體輪廓裁切），neck 是領口的高度。"""
    clip = d.clip(BODY)
    g = d.lin([(0, top), (1, bottom)])
    shape = f"M-100,{neck - 6} Q0,{neck + 26} 100,{neck - 6} L100,100 L-100,100 Z"
    inner = path(shape, g, line, 3.5)
    # 布料的摺痕
    inner += path(f"M-40,{neck + 22} Q-44,70 -36,96 M40,{neck + 22} Q44,70 36,96", "none", line, 2.5, 'opacity="0.28" stroke-linecap="round"')
    if trim:
        inner += path(f"M-100,{neck - 6} Q0,{neck + 26} 100,{neck - 6}", "none", trim, 8, 'stroke-linecap="round"')
        inner += path(f"M-100,{neck - 10} Q0,{neck + 22} 100,{neck - 10}", "none", "#FFFFFF", 2, 'opacity="0.5"')
    if collar:
        inner += path(f"M-26,{neck + 10} L0,{neck + 44} L26,{neck + 10}", "none", collar, 8, 'stroke-linejoin="round" stroke-linecap="round"')
    if sash:
        inner += path("M-100,66 Q0,84 100,66 L100,80 Q0,98 -100,80 Z", sash, line, 2.5)
    out = f'<g clip-path="{clip}">{inner}</g>'
    return out + path(BODY, "none", line, 4.5)


def paws(fur, y=74, spread=32, fill=None, hold=None):
    """前腳；hold 是 (x, y) 清單，表示抬起來握東西的那幾隻腳。"""
    p = FURS[fur]
    f = fill or p["belly"]
    out = ""
    for s in (-1, 1):
        out += ellipse(s * spread, y, 19, 12, f, p["line"], 3.5)
        out += ellipse(s * spread - 4, y - 3, 6, 3, "#FFFFFF", "none", None, 'opacity="0.6"')
    for hx, hy in hold or ():
        out += ellipse(hx, hy, 15, 13, f, p["line"], 3.5)
        out += ellipse(hx - 3, hy - 4, 5, 3, "#FFFFFF", "none", None, 'opacity="0.6"')
    return out


def beard(d, color, shade, line, long=False, wide=False):
    """鬍子：蓬蓬的，從嘴邊垂下來。"""
    g = d.lin([(0, color), (1, shade)])
    w = 44 if wide else 28
    bottom = 82 if long else 60
    n = 5 if wide else 3
    shape = f"M{-w},24 C{-w - 10},{(24 + bottom) / 2} {-w * 0.9},{bottom - 8} {-w * 0.8},{bottom - 6} "
    for k in range(n):
        x1 = -w * 0.8 + (k + 1) * (w * 1.6 / n)
        mid = (k + 0.5 - n / 2) / (n / 2)
        dip = bottom + 12 - abs(mid) * 10
        shape += f"Q{x1 - w * 0.8 / n:.1f},{dip + 8:.1f} {x1:.1f},{bottom - 6 if k < n - 1 else bottom - 6} "
    shape += f"C{w * 0.9},{bottom - 8} {w + 10},{(24 + bottom) / 2} {w},24 Q0,36 {-w},24 Z"
    out = path(shape, g, line, 3.5, 'stroke-linejoin="round"')
    for k in (-1, 0, 1):
        out += path(f"M{k * w * 0.42},40 Q{k * w * 0.48},{bottom - 16} {k * w * 0.3},{bottom - 2}", "none", line, 2, 'opacity="0.3" stroke-linecap="round"')
    return out


def halo(d, cy=-20, r=92, color="#FFE27A", rays=0):
    """身後的光環。"""
    out = glow(d, 0, cy, r * 1.25, color, 0.75)
    out += f'<circle cx="0" cy="{cy}" r="{r}" fill="none" stroke="{color}" stroke-width="5" opacity="0.9"/>'
    out += f'<circle cx="0" cy="{cy}" r="{r - 8}" fill="none" stroke="#FFFFFF" stroke-width="1.5" opacity="0.8"/>'
    for i in range(rays):
        a = math.radians(i * 360 / rays - 90)
        x1, y1 = math.cos(a) * (r + 6), cy + math.sin(a) * (r + 6)
        x2, y2 = math.cos(a) * (r + 22), cy + math.sin(a) * (r + 22)
        out += f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{color}" stroke-width="5" stroke-linecap="round" opacity="0.9"/>'
    return out


def wing(d, s, x, y, scale=1.0, top="#FFFFFF", bottom="#CFE4FF", line="#6B7AA8"):
    """一片小翅膀；s 是左右（-1／1）。"""
    g = d.lin([(0, top), (1, bottom)])
    shape = "M0,0 C14,-26 44,-34 62,-22 C52,-18 50,-12 56,-6 C46,-6 42,0 46,8 C36,6 30,10 30,18 C18,16 6,12 0,0 Z"
    body = path(shape, g, line, 3, 'stroke-linejoin="round"')
    body += path("M10,-2 C22,-16 38,-22 52,-18 M14,6 C24,-4 36,-6 46,-2", "none", line, 1.8, 'opacity="0.45" stroke-linecap="round"')
    return f'<g transform="translate({x} {y}) scale({s * scale} {scale})">{body}</g>'


def figure(d, layers, aura=None, shadow=True):
    """把各層疊起來；aura 是身後的光暈顏色。"""
    out = ""
    if aura:
        out += glow(d, 0, 0, 128, aura, 0.55)
    if shadow:
        out += ground_shadow(d)
    return out + "".join(layers)


def unit_sprite(name, draw, size=300):
    d = Defs()
    body = draw(d)
    return Sprite(name, size, size, VIEW, body, d.text())
