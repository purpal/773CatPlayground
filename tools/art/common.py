"""v2「麻糬萌」風格的 SVG 小工具（見 docs/art-style.md）。"""
import math

INK = "#6B4C52"
DARK_INK = "#2E2433"


class Sprite:
    """一張要輸出的圖：name 為檔名（不含副檔名），width/height 為輸出像素，view 為 SVG viewBox。"""

    def __init__(self, name, width, height, view, body, defs=""):
        self.name, self.width, self.height, self.view, self.body, self.defs = name, width, height, view, body, defs

    def svg(self):
        return (
            f'<svg xmlns="http://www.w3.org/2000/svg" width="{self.width}" height="{self.height}" viewBox="{self.view}">'
            f"<defs>{COMMON_DEFS}{self.defs}</defs>{self.body}</svg>"
        )


COMMON_DEFS = """
<filter id="wobble" x="-10%" y="-10%" width="120%" height="120%">
  <feTurbulence type="fractalNoise" baseFrequency="0.035" numOctaves="2" seed="3" result="n"/>
  <feDisplacementMap in="SourceGraphic" in2="n" scale="2.5" xChannelSelector="R" yChannelSelector="G"/>
</filter>
<filter id="softGlow" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="6"/></filter>
<radialGradient id="blushG"><stop offset="0" stop-color="#FF8FB1" stop-opacity="0.75"/><stop offset="1" stop-color="#FF8FB1" stop-opacity="0"/></radialGradient>
<linearGradient id="tearG" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#E4F7FF"/><stop offset="1" stop-color="#9ADBFF"/></linearGradient>
"""


def g(content, transform="", ink=INK, width=5, extra=""):
    t = f' transform="{transform}"' if transform else ""
    return f'<g{t} stroke="{ink}" stroke-width="{width}" stroke-linecap="round" stroke-linejoin="round" {extra}>{content}</g>'


def wob(content):
    return f'<g filter="url(#wobble)">{content}</g>'


def ellipse(cx, cy, rx, ry, fill, stroke=None, sw=None, extra=""):
    s = ""
    if stroke == "none":
        s = ' stroke="none"'
    elif stroke:
        s = f' stroke="{stroke}"'
    if sw is not None:
        s += f' stroke-width="{sw}"'
    return f'<ellipse cx="{cx:.1f}" cy="{cy:.1f}" rx="{rx:.1f}" ry="{ry:.1f}" fill="{fill}"{s} {extra}/>'


def path(d, fill="none", stroke=None, sw=None, extra=""):
    s = ""
    if stroke == "none":
        s = ' stroke="none"'
    elif stroke:
        s = f' stroke="{stroke}"'
    if sw is not None:
        s += f' stroke-width="{sw}"'
    return f'<path d="{d}" fill="{fill}"{s} {extra}/>'


def mochi_path(cx, cy, w, h, bottom_bulge=1.0):
    """麻糬形：上窄下寬、底部微扁的圓。"""
    hw, hh = w / 2, h / 2
    return (
        f"M{cx},{cy - hh} "
        f"C{cx + hw * 0.72},{cy - hh} {cx + hw},{cy - hh * 0.4} {cx + hw},{cy + hh * 0.1} "
        f"C{cx + hw * bottom_bulge},{cy + hh * 0.75} {cx + hw * 0.6},{cy + hh} {cx},{cy + hh} "
        f"C{cx - hw * 0.6},{cy + hh} {cx - hw * bottom_bulge},{cy + hh * 0.75} {cx - hw},{cy + hh * 0.1} "
        f"C{cx - hw},{cy - hh * 0.4} {cx - hw * 0.72},{cy - hh} {cx},{cy - hh} Z"
    )


def ear(tx, ty, lx, ly, rx, ry):
    return (
        f"M{lx},{ly} Q{(lx + tx) / 2 - (ry - ty) * 0.1},{(ly + ty) / 2} {tx},{ty} "
        f"Q{(rx + tx) / 2 + (ry - ty) * 0.1},{(ry + ty) / 2} {rx},{ry} Z"
    )


def dot_eye(cx, cy, r=7, color="#2E2128"):
    return (
        ellipse(cx, cy, r, r * 1.25, color, "none")
        + ellipse(cx + r * 0.38, cy - r * 0.42, r * 0.42, r * 0.42, "#FFFFFF", "none")
        + ellipse(cx - r * 0.3, cy + r * 0.45, r * 0.18, r * 0.18, "#FFFFFF", "none")
    )


def color_eye(cx, cy, r, iris, pupil="#1E1622", slit=False):
    pw = r * (0.28 if slit else 0.55)
    return (
        ellipse(cx, cy, r, r * 1.15, iris, None, 3)
        + ellipse(cx, cy + r * 0.05, pw, r * 0.85 if slit else r * 0.6, pupil, "none")
        + ellipse(cx + r * 0.35, cy - r * 0.4, r * 0.3, r * 0.3, "#FFFFFF", "none")
    )


def arc_eye(cx, cy, w, up=True, sw=4.5):
    """閉眼／笑眼的弧線。"""
    dy = -w * 0.55 if up else w * 0.55
    return path(f"M{cx - w},{cy} Q{cx},{cy + dy} {cx + w},{cy}", "none", None, sw)


def blush(cx, cy, w, lines=True, color="#FF7FA6"):
    out = ellipse(cx, cy, w, w * 0.6, "url(#blushG)", "none")
    if lines:
        for k in (-1, 0, 1):
            x = cx + k * w * 0.38
            out += path(f"M{x - w * 0.12},{cy + w * 0.22} L{x + w * 0.12},{cy - w * 0.22}", "none", color, max(2, w * 0.1))
    return out


def omega_mouth(cx, cy, w, sw=3.5):
    return path(f"M{cx - w},{cy - w * 0.15} Q{cx - w * 0.5},{cy + w * 0.6} {cx},{cy} Q{cx + w * 0.5},{cy + w * 0.6} {cx + w},{cy - w * 0.15}", "none", None, sw)


def wavy_mouth(cx, cy, w, sw=4):
    seg = w / 2
    d = f"M{cx - w},{cy}"
    for i in range(4):
        x = cx - w + seg * (i + 0.5)
        d += f" Q{x},{cy + (-6 if i % 2 == 0 else 6)} {cx - w + seg * (i + 1)},{cy}"
    return path(d, "none", None, sw)


def open_mouth(cx, cy, w, h, fangs=False, teeth_color="#FFFFFF"):
    d = f"M{cx - w},{cy} Q{cx},{cy - h * 0.2} {cx + w},{cy} Q{cx + w * 0.7},{cy + h} {cx},{cy + h} Q{cx - w * 0.7},{cy + h} {cx - w},{cy} Z"
    out = path(d, "#7A2340", None, 4)
    out += ellipse(cx, cy + h * 0.68, w * 0.55, h * 0.3, "#FF8FB1", "none")
    if fangs:
        out += path(f"M{cx - w * 0.75},{cy + 1} L{cx - w * 0.5},{cy + h * 0.5} L{cx - w * 0.3},{cy + 2} Z", teeth_color, None, 2.5)
        out += path(f"M{cx + w * 0.75},{cy + 1} L{cx + w * 0.5},{cy + h * 0.5} L{cx + w * 0.3},{cy + 2} Z", teeth_color, None, 2.5)
    return out


def tears(cx, cy, length=40, side=1):
    return (
        path(f"M{cx},{cy} Q{cx + side * 4},{cy + length * 0.5} {cx - side * 2},{cy + length}", "none", "#9ADBFF", 7, 'opacity="0.95"')
        + ellipse(cx - side * 2, cy + length + 5, 6, 7, "#BDE8FF", "#7FC4E8", 2)
    )


def tear_pool(cx, cy, w):
    return path(f"M{cx - w},{cy} Q{cx},{cy + w * 0.7} {cx + w},{cy} Q{cx + w * 0.9},{cy + w * 0.8} {cx},{cy + w * 0.85} Q{cx - w * 0.9},{cy + w * 0.8} {cx - w},{cy} Z",
                "url(#tearG)", "#7FC4E8", 2.5)


def anger_mark(cx, cy, s=14, color="#FF5C7A"):
    d = ""
    for a in (45, 135, 225, 315):
        r = math.radians(a)
        x1, y1 = cx + math.cos(r) * s * 0.35, cy + math.sin(r) * s * 0.35
        x2, y2 = cx + math.cos(r) * s, cy + math.sin(r) * s
        d += f"M{x1:.1f},{y1:.1f} Q{(x1 + x2) / 2 + math.sin(r) * s * 0.25:.1f},{(y1 + y2) / 2 - math.cos(r) * s * 0.25:.1f} {x2:.1f},{y2:.1f} "
    return path(d, "none", color, s * 0.28)


def star_points(cx, cy, r, inner=0.45, n=5, rot=-90):
    pts = []
    for i in range(n * 2):
        rr = r if i % 2 == 0 else r * inner
        a = math.radians(rot + i * 180 / n)
        pts.append(f"{cx + math.cos(a) * rr:.1f},{cy + math.sin(a) * rr:.1f}")
    return " ".join(pts)


def star(cx, cy, r, fill="#FFE07A", stroke=INK, sw=3, rot=-90):
    return f'<polygon points="{star_points(cx, cy, r, rot=rot)}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}" stroke-linejoin="round"/>'


def heart(cx, cy, s, fill="#FF8FB1", stroke=INK, sw=3):
    d = f"M{cx},{cy + s * 0.9} C{cx - s * 1.3},{cy} {cx - s * 0.9},{cy - s * 1.1} {cx},{cy - s * 0.45} C{cx + s * 0.9},{cy - s * 1.1} {cx + s * 1.3},{cy} {cx},{cy + s * 0.9} Z"
    return path(d, fill, stroke, sw)


def paw(cx, cy, s, fill, stroke=INK, sw=3):
    out = ellipse(cx, cy + s * 0.35, s * 0.62, s * 0.5, fill, stroke, sw)
    for dx, dy in ((-0.62, -0.25), (-0.22, -0.62), (0.22, -0.62), (0.62, -0.25)):
        out += ellipse(cx + dx * s, cy + dy * s, s * 0.27, s * 0.3, fill, stroke, sw)
    return out


def text(x, y, s, size, fill, stroke="#FFFFFF", sw=None, rot=0, anchor="middle", weight="bold", ink=None):
    sw = sw if sw is not None else size * 0.18
    out = ""
    if ink:
        out += (f'<text x="{x}" y="{y}" text-anchor="{anchor}" font-family="WenQuanYi Zen Hei" font-weight="{weight}" font-size="{size}" '
                f'fill="none" stroke="{ink}" stroke-width="{sw + 5}" stroke-linejoin="round" transform="rotate({rot} {x} {y})">{s}</text>')
    out += (f'<text x="{x}" y="{y}" text-anchor="{anchor}" font-family="WenQuanYi Zen Hei" font-weight="{weight}" font-size="{size}" '
            f'fill="{fill}" stroke="{stroke}" stroke-width="{sw}" paint-order="stroke" stroke-linejoin="round" transform="rotate({rot} {x} {y})">{s}</text>')
    return out
