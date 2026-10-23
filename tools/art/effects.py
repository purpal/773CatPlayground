"""武器與命中特效（依 docs/art-style.md 的特效設定板）。"""
import math
from common import *

GLOW_DEFS = """
<radialGradient id="glowO"><stop offset="0" stop-color="#FFE7A8" stop-opacity="0.95"/><stop offset="1" stop-color="#FF9A3C" stop-opacity="0"/></radialGradient>
<radialGradient id="glowV"><stop offset="0" stop-color="#EBDDFF" stop-opacity="0.95"/><stop offset="1" stop-color="#9C6BFF" stop-opacity="0"/></radialGradient>
<radialGradient id="glowW"><stop offset="0" stop-color="#FFFFFF"/><stop offset="0.55" stop-color="#FFF6C8" stop-opacity="0.8"/><stop offset="1" stop-color="#FFF6C8" stop-opacity="0"/></radialGradient>
"""

TOES = ((-14, -8), (-5, -16), (5, -16), (14, -8))


def sticker_paw(color, s=1.0, pad="#FFF3D6"):
    """白色貼紙描邊的肉球。"""
    out = f'<g transform="scale({s})"><g stroke="#FFFFFF" stroke-width="7" stroke-linejoin="round">'
    out += ellipse(0, 6, 15, 12, color) + "".join(ellipse(dx, dy, 6, 6.5, color) for dx, dy in TOES)
    out += "</g>"
    out += ellipse(0, 6, 15, 12, color, "none") + "".join(ellipse(dx, dy, 6, 6.5, color, "none") for dx, dy in TOES)
    out += ellipse(-4, 3, 5, 3, pad, "none") + "</g>"
    return out


def paw_shot(name, color, glow):
    b = ellipse(0, 0, 30, 30, f"url(#{glow})", "none") + sticker_paw(color, 1.0)
    return Sprite(name, 120, 120, "-32 -32 64 64", b, GLOW_DEFS)


def big_paw():
    b = ellipse(0, 0, 58, 58, "url(#glowO)", "none") + sticker_paw("#FF8A2E", 2.0)
    b += star(30, -30, 9, "#FFFFFF", "#FFB45C", 2.5) + star(-34, 18, 6, "#FFFFFF", "#FFB45C", 2)
    return Sprite("shot_paw_big", 240, 240, "-60 -60 120 120", b, GLOW_DEFS)


def claw():
    b = ellipse(0, 0, 40, 46, "url(#glowV)", "none")
    for k in (-1, 0, 1):
        d = f"M{k * 14 - 10},30 Q{k * 14 + 12},0 {k * 14 - 2},-36"
        b += path(d, "none", "#FFFFFF", 14) + path(d, "none", "#9C6BFF", 8) + path(d, "none", "#F3E9FF", 3)
    return Sprite("shot_claw", 120, 140, "-42 -50 84 100", b, GLOW_DEFS)


def fish():
    """追蹤彈：小魚乾飛彈，朝 +x。"""
    body = "M-20,0 C-8,-14 14,-14 24,0 C14,14 -8,14 -20,0 Z"
    tail = "M-18,0 L-32,-12 L-30,12 Z"
    b = f'<g stroke="#FFFFFF" stroke-width="7" stroke-linejoin="round">{path(body, "#6FD6A8")}{path(tail, "#6FD6A8")}</g>'
    b += path(body, "#6FD6A8", "none") + path(tail, "#4FBF8F", "none")
    b += path("M-6,-8 Q0,0 -6,8 M4,-9 Q10,0 4,9", "none", "#C2F5D9", 2.5)
    b += ellipse(14, -3, 3, 3, INK, "none")
    return Sprite("shot_fish", 100, 60, "-36 -18 72 36", b)


def flower():
    b = ellipse(0, 0, 56, 56, "url(#glowW)", "none")
    for k in range(8):
        a = k * 45
        x, y = math.cos(math.radians(a)) * 30, math.sin(math.radians(a)) * 30
        b += ellipse(x, y, 20, 12, "#FFE3F1", "#FF8FB1", 3, f'transform="rotate({a} {x:.1f} {y:.1f})"')
    b += ellipse(0, 0, 18, 18, "#FFF6C8", "#FFC94A", 4)
    return Sprite("fx_flower", 180, 180, "-60 -60 120 120", b, GLOW_DEFS)


def fx_star():
    return Sprite("fx_star", 80, 80, "-26 -26 52 52", g(star(0, 0, 22, "#FFE07A", INK, 3.5)))


def fx_heart():
    return Sprite("fx_heart", 80, 80, "-26 -26 52 52", g(heart(0, 0, 18, "#FF8FB1", INK, 3)))


SFX = {
    "sfx_poko": ("ポコッ!", "#FF5C93", 8),
    "sfx_paan": ("ぱぁん!", "#FF5C93", -8),
    "sfx_doon": ("ドーン!!", "#FF7A3C", -6),
    "sfx_sweet": ("SWEET!", "#FF7AB0", 6),
    "sfx_elite": ("ELITE!", "#B07CFF", -4),
    "sfx_combo": ("COMBO", "#FFB300", 0),
    "sfx_roar": ("吼～！", "#FFF6D6", -6, "#FF8A2E"),
    "sfx_beam": ("ビーム!", "#8C7BFF", -6),
}


def sfx(name, s, color, rot, stroke="#FFFFFF"):
    size = 60
    w = len(s) * size * (0.66 if s.isascii() else 1.12) + 60
    h = size * 1.6
    return Sprite(name, int(w * 2), int(h * 2), f"{-w / 2:.0f} {-h / 2:.0f} {w:.0f} {h:.0f}",
                  text(0, size * 0.36, s, size, color, stroke, size * 0.22, rot=rot, ink=INK))


def digit(ch, name):
    size = 60
    return Sprite(name, 84, 110, "-21 -28 42 55", text(0, 20, ch, size, "#FFC21A", "#FFFFFF", 12, ink=INK))


def all_sprites():
    out = [
        paw_shot("shot_paw_orange", "#FF9A3C", "glowO"),
        paw_shot("shot_paw_violet", "#A07CFF", "glowV"),
        big_paw(), claw(), fish(), flower(), fx_star(), fx_heart(),
    ]
    out += [sfx(k, *v) for k, v in SFX.items()]
    out += [digit(str(d), f"fx_d{d}") for d in range(10)]
    out += [digit("+", "fx_plus"), digit("×", "fx_x")]
    return out
