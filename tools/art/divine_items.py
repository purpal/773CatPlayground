"""喵神封魔錄：法術、結界、神器的卡圖，卡面背景，以及介面用的小圖。圖示座標同 divine_kit（中心 (0,0)，約 ±110）。"""
import math
from divine_kit import *
from common import text


def sparkles(points, color="#FFFFFF"):
    out = ""
    for x, y, r in points:
        out += f'<path d="M{x},{y - r} Q{x + r * 0.18},{y - r * 0.18} {x + r},{y} Q{x + r * 0.18},{y + r * 0.18} {x},{y + r} Q{x - r * 0.18},{y + r * 0.18} {x - r},{y} Q{x - r * 0.18},{y - r * 0.18} {x},{y - r} Z" fill="{color}"/>'
    return out


def talisman(d, paper_top, paper_bottom, ink, glyph, line="#7A4A0C"):
    out = rrect(-44, -100, 88, 200, 6, d.lin([(0, paper_top), (1, paper_bottom)]), line, 4)
    out += rrect(-34, -90, 68, 180, 3, "none", ink, 2.5, 'opacity="0.7"')
    out += text(0, -34, glyph, 62, ink, stroke="none", sw=0)
    out += path("M-18,6 q18,-16 36,0 M0,-2 l0,70 M-20,30 l40,0 M-22,52 q22,22 44,0 M-12,74 l24,0", "none", ink, 5, 'stroke-linecap="round"')
    return out


def wuleifu(d):
    out = glow(d, 0, 0, 130, "#FFE45C", 0.7)
    out += rot(talisman(d, "#FFF7B8", "#F2C83A", "#C81E1E", "雷"), -8)
    for a, r, sc in ((-150, 104, 0.5), (-30, 108, 0.55), (150, 100, 0.45), (30, 104, 0.5), (-90, 118, 0.4)):
        x, y = math.cos(math.radians(a)) * r, math.sin(math.radians(a)) * r * 0.85
        out += move(rot(bolt(sc, 0, 0, "#FFFFFF", "#E0A21A"), a + 90), x, y)
    return out


def dingshen(d):
    out = glow(d, 0, 0, 130, "#7FD0FF", 0.7)
    for r in (112, 92):
        out += f'<circle cx="0" cy="0" r="{r}" fill="none" stroke="#BFE8FF" stroke-width="3" stroke-dasharray="14 10" opacity="0.8"/>'
    out += rot(talisman(d, "#F2FBFF", "#9FD8FF", "#1E4AA8", "定", "#1E3A6E"), 6)
    return out + sparkles([(-92, -70, 12), (96, -50, 10), (88, 78, 13), (-86, 84, 9)])


def pantao(d):
    out = glow(d, 0, 0, 130, "#FF9AC8", 0.75)
    g = d.rad([(0, "#FFF6E0"), (0.45, "#FFB8A0"), (1, "#F2507A")], 0.36, 0.34, 0.85)
    out += path("M0,-76 C-70,-96 -112,-30 -86,30 C-66,80 -22,98 0,100 C22,98 66,80 86,30 C112,-30 70,-96 0,-76 Z", g, "#A8204A", 4.5)
    out += path("M0,-74 C-14,-30 -12,40 0,98", "none", "#A8204A", 3, 'opacity="0.4" stroke-linecap="round"')
    out += path("M-60,-40 C-52,-62 -30,-72 -12,-68", "none", "#FFFFFF", 8, 'stroke-linecap="round" opacity="0.7"')
    leaf = d.lin([(0, "#9FE87A"), (1, "#2E8A3A")])
    out += path("M2,-74 C20,-116 70,-112 92,-92 C64,-66 26,-64 2,-74 Z", leaf, "#1A5A26", 3.5) + path("M10,-78 C36,-94 62,-96 84,-92", "none", "#1A5A26", 2.2, 'opacity="0.6"')
    out += path("M-2,-74 C-18,-108 -56,-106 -72,-92 C-50,-70 -22,-66 -2,-74 Z", leaf, "#1A5A26", 3.5)
    return out + sparkles([(-100, -40, 11), (104, 20, 13), (-80, 90, 9), (70, 96, 10)])


def bagua(d):
    out = glow(d, 0, 0, 132, "#B8F2D8", 0.65)
    pts = [(math.cos(math.radians(22.5 + k * 45)) * 112, math.sin(math.radians(22.5 + k * 45)) * 112) for k in range(8)]
    out += poly(pts, d.rad([(0, "#2E3A5A"), (1, "#141A30")]), "#FFD24A", 5)
    pts2 = [(x * 0.62, y * 0.62) for x, y in pts]
    out += poly(pts2, "none", "#FFD24A", 2.5, 'opacity="0.8"')
    trigrams = ["111", "110", "101", "100", "011", "010", "001", "000"]
    for k, tri in enumerate(trigrams):
        a = k * 45 - 90
        bars = ""
        for j, bit in enumerate(tri):
            y = -98 + j * 11
            if bit == "1":
                bars += f'<line x1="-17" y1="{y}" x2="17" y2="{y}" stroke="#FFE9A0" stroke-width="6" stroke-linecap="round"/>'
            else:
                bars += f'<line x1="-17" y1="{y}" x2="-4" y2="{y}" stroke="#FFE9A0" stroke-width="6" stroke-linecap="round"/><line x1="4" y1="{y}" x2="17" y2="{y}" stroke="#FFE9A0" stroke-width="6" stroke-linecap="round"/>'
        out += rot(bars, a)
    r = 46
    out += f'<circle cx="0" cy="0" r="{r}" fill="#FFFFFF" stroke="#141A30" stroke-width="4"/>'
    out += path(f"M0,{-r} A{r},{r} 0 0 1 0,{r} A{r / 2},{r / 2} 0 0 1 0,0 A{r / 2},{r / 2} 0 0 0 0,{-r} Z", "#141A30", "none")
    out += f'<circle cx="0" cy="{-r / 2}" r="7" fill="#141A30"/><circle cx="0" cy="{r / 2}" r="7" fill="#FFFFFF"/>'
    return out


def ziqi(d):
    out = glow(d, 0, -20, 136, "#C88BFF", 0.8)
    out += f'<circle cx="0" cy="-30" r="52" fill="{d.rad([(0, "#FFFFFF"), (0.5, "#FFE27A"), (1, "#FF9A2E")])}" stroke="{GOLD_LINE}" stroke-width="4"/>'
    for k in range(12):
        a = math.radians(k * 30)
        out += f'<line x1="{math.cos(a) * 62:.1f}" y1="{-30 + math.sin(a) * 62:.1f}" x2="{math.cos(a) * 78:.1f}" y2="{-30 + math.sin(a) * 78:.1f}" stroke="#FFD24A" stroke-width="6" stroke-linecap="round"/>'
    out += cloud(-58, 44, 52, "#D8B0FF", "#6A2EB8", 3.5) + cloud(62, 54, 46, "#E6C8FF", "#6A2EB8", 3.5) + cloud(0, 78, 60, "#B88AF2", "#4A1E8A", 3.5)
    return out + sparkles([(-100, -70, 10), (96, -84, 12), (112, 0, 8)], "#FFE9A0")


def jingubang(d):
    out = glow(d, 0, 0, 132, "#FFD24A", 0.75)
    out += cloud(-54, 70, 44, "#FFFFFF", "#E0B878", 3) + cloud(60, -66, 36, "#FFFFFF", "#E0B878", 3)
    staff = shaft(d, 0, -118, 118, 22, d.lin([(0, "#FF7A5A"), (0.5, "#D62A1E"), (1, "#8A1410")], 0, 0, 1, 0), "#5A0E0A")
    for y in (-118, 92):
        staff += rrect(-15, y, 30, 26, 5, gold(d), GOLD_LINE, 3) + path(f"M-12,{y + 9} l24,0 M-12,{y + 17} l24,0", "none", GOLD_LINE, 2)
    out += rot(staff, 38)
    return out + sparkles([(-84, -70, 13), (90, 60, 12), (30, 104, 8)], "#FFF6C2")


def fenghuolun(d):
    out = glow(d, 0, 0, 132, "#FF8A3A", 0.8)
    for k in range(10):
        a = k * 36
        out += rot(flame(d, 0, -72, 54), a)
    out += f'<circle cx="0" cy="0" r="74" fill="{gold(d)}" stroke="{GOLD_LINE}" stroke-width="5"/><circle cx="0" cy="0" r="54" fill="{d.rad([(0, "#FFF3A0"), (1, "#FF6A2A")])}" stroke="{GOLD_LINE}" stroke-width="4"/>'
    for k in range(8):
        a = math.radians(k * 45)
        out += f'<line x1="{math.cos(a) * 18:.1f}" y1="{math.sin(a) * 18:.1f}" x2="{math.cos(a) * 54:.1f}" y2="{math.sin(a) * 54:.1f}" stroke="{GOLD_LINE}" stroke-width="5"/>'
    out += f'<circle cx="0" cy="0" r="18" fill="{gold(d)}" stroke="{GOLD_LINE}" stroke-width="4"/>' + gem(d, 0, 0, 9, "#FF5A3A")
    return out


def thunderbolt(d):
    out = glow(d, 0, 10, 136, "#7FD0FF", 0.8)
    out += cloud(0, -70, 76, "#6E7AB0", "#2E3660", 4) + cloud(-56, -52, 40, "#8E9AD0", "#2E3660", 3)
    out += move(bolt(1.5, 0, 0, "#FFFFFF", "#2E7AFF"), 6, 30)
    out += move(rot(bolt(0.6, 0, 0, "#DFF4FF", "#2E7AFF"), -24), -70, 46) + move(rot(bolt(0.55, 0, 0, "#DFF4FF", "#2E7AFF"), 26), 78, 40)
    return out


def olympus(d):
    out = glow(d, 0, -30, 136, "#FFB02A", 0.85)
    marble = d.lin([(0, "#FFFFFF"), (1, "#B8C0DC")], 0, 0, 1, 0)
    out += rrect(-30, 20, 60, 86, 3, marble, "#5A5E88", 3.5)
    for x in (-16, 0, 16):
        out += f'<line x1="{x}" y1="26" x2="{x}" y2="100" stroke="#8E94BE" stroke-width="3" opacity="0.6"/>'
    out += rrect(-42, 98, 84, 14, 3, marble, "#5A5E88", 3.5) + rrect(-40, 10, 80, 14, 3, marble, "#5A5E88", 3.5)
    out += path("M-62,10 C-60,-18 60,-18 62,10 Z", gold(d), GOLD_LINE, 4)
    out += flame(d, -26, -4, 70) + flame(d, 28, -4, 64) + flame(d, 0, -2, 112)
    return out + sparkles([(-84, -70, 9), (90, -50, 11), (-60, -108, 7)], "#FFF3A0")


def feast(d):
    out = glow(d, 0, 0, 132, "#C86BFF", 0.7)
    out += path("M-52,-70 C-52,0 -24,26 0,28 C24,26 52,0 52,-70 Z", gold(d), GOLD_LINE, 4.5)
    out += ellipse(0, -70, 52, 13, d.lin([(0, "#A83AE0"), (1, "#5A1890")]), GOLD_LINE, 4)
    out += path("M-36,-74 q10,-6 22,-2", "none", "#FFFFFF", 3, 'opacity="0.7" stroke-linecap="round"')
    out += rrect(-7, 26, 14, 46, 4, gold(d, False), GOLD_LINE, 3.5) + ellipse(0, 78, 40, 12, gold(d), GOLD_LINE, 4)
    out += path("M-38,-40 C-30,-6 -16,8 -4,12", "none", "#FFFFFF", 5, 'opacity="0.55" stroke-linecap="round"')
    grape = d.rad([(0, "#E0A8FF"), (0.5, "#9A4AE0"), (1, "#4A1890")], 0.35, 0.3, 0.8)
    for row, n in enumerate((4, 3, 2, 1)):
        for k in range(n):
            x = 74 + (k - (n - 1) / 2) * 22
            y = 10 + row * 20
            out += f'<circle cx="{x}" cy="{y}" r="12.5" fill="{grape}" stroke="#2E0E5A" stroke-width="3"/><circle cx="{x - 4}" cy="{y - 4}" r="3" fill="#FFFFFF" opacity="0.8"/>'
    leaf = d.lin([(0, "#9FE87A"), (1, "#2E8A3A")])
    out += path("M74,-6 C60,-40 86,-52 104,-40 C116,-30 110,-10 92,-8 Z", leaf, "#1A5A26", 3.5)
    out += path("M-92,40 C-110,10 -90,-20 -66,-10 C-50,-2 -52,24 -70,34 Z", leaf, "#1A5A26", 3.5)
    return out


def labyrinth(d):
    out = glow(d, 0, 0, 132, "#BFD0E8", 0.6)
    out += f'<circle cx="0" cy="0" r="112" fill="{d.rad([(0, "#E6DCC8"), (1, "#A89878")])}" stroke="#4A3E2A" stroke-width="5"/>'
    gaps = [(98, 40), (80, 200), (62, 110), (44, 290), (26, 20)]
    for r, gap in gaps:
        a1, a2 = math.radians(gap + 22), math.radians(gap - 22 + 360)
        x1, y1, x2, y2 = math.cos(a1) * r, math.sin(a1) * r, math.cos(a2) * r, math.sin(a2) * r
        out += f'<path d="M{x1:.1f},{y1:.1f} A{r},{r} 0 1 1 {x2:.1f},{y2:.1f}" fill="none" stroke="#4A3E2A" stroke-width="9" stroke-linecap="round"/>'
        out += f'<path d="M{x1:.1f},{y1:.1f} A{r},{r} 0 1 1 {x2:.1f},{y2:.1f}" fill="none" stroke="#7A6A4A" stroke-width="4" stroke-linecap="round"/>'
    for r1, r2, a in ((98, 80, 130), (80, 62, 20), (62, 44, 210), (44, 26, 120)):
        x1, y1 = math.cos(math.radians(a)) * r1, math.sin(math.radians(a)) * r1
        x2, y2 = math.cos(math.radians(a)) * r2, math.sin(math.radians(a)) * r2
        out += f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="#4A3E2A" stroke-width="9" stroke-linecap="round"/>'
    out += glow(d, 0, 0, 24, "#FFE27A", 1) + gem(d, 0, 0, 10, "#FFD24A", GOLD_LINE)
    return out


def sanctuary(d):
    out = glow(d, 0, 0, 136, "#9FD8FF", 0.75)
    marble = d.lin([(0, "#FFFFFF"), (1, "#C4CCE6")])
    out += poly([(-84, -30), (0, -84), (84, -30)], marble, "#5A5E88", 4) + rrect(-88, -32, 176, 14, 3, marble, "#5A5E88", 4)
    for x in (-66, -22, 22, 66):
        out += rrect(x - 11, -18, 22, 92, 4, marble, "#5A5E88", 3.5)
        out += f'<line x1="{x - 4}" y1="-12" x2="{x - 4}" y2="68" stroke="#8E94BE" stroke-width="2.5" opacity="0.6"/><line x1="{x + 4}" y1="-12" x2="{x + 4}" y2="68" stroke="#8E94BE" stroke-width="2.5" opacity="0.6"/>'
    out += rrect(-92, 74, 184, 14, 3, marble, "#5A5E88", 4) + rrect(-100, 88, 200, 12, 3, marble, "#5A5E88", 4)
    out += gem(d, 0, -50, 9, "#FFD24A", GOLD_LINE)
    dome = d.rad([(0, "#DFF4FF", 0.05), (0.8, "#7FD0FF", 0.3), (1, "#FFFFFF", 0.85)])
    out += f'<path d="M-118,104 A118,128 0 0 1 118,104 Z" fill="{dome}" stroke="#DFF4FF" stroke-width="4" opacity="0.95"/>'
    out += path("M-96,10 A110,118 0 0 1 -30,-100", "none", "#FFFFFF", 5, 'opacity="0.8" stroke-linecap="round"')
    return out


def trident(d):
    out = glow(d, 0, 0, 132, "#5CE0FF", 0.75)
    water = d.lin([(0, "#DFF8FF"), (1, "#3F9AE0")])
    out += path("M-108,70 C-80,30 -40,40 -20,66 C0,40 40,36 60,60 C80,40 104,50 112,76 C80,104 -80,104 -108,70 Z", water, "#1E5A9A", 4)
    tri = shaft(d, 0, -40, 124, 14, gold(d, False), GOLD_LINE)
    tri += poly([(0, -132), (14, -84), (0, -40), (-14, -84)], gold(d), GOLD_LINE, 3.5)
    for s in (-1, 1):
        tri += path(f"M{s * 5},-44 C{s * 44},-48 {s * 50},-86 {s * 42},-124 C{s * 30},-104 {s * 28},-74 {s * 5},-66 Z", gold(d), GOLD_LINE, 3.5)
    tri += gem(d, 0, -40, 9, "#5CE0FF", "#1E5A9A")
    out += rot(tri, -32)
    return out + sparkles([(-90, -60, 11), (96, -20, 9), (60, -100, 12)], "#DFF8FF")


def aegis(d):
    out = glow(d, 0, 0, 134, "#FFD24A", 0.7)
    out += f'<circle cx="0" cy="0" r="106" fill="{gold(d)}" stroke="{GOLD_LINE}" stroke-width="5"/>'
    out += f'<circle cx="0" cy="0" r="84" fill="{d.rad([(0, "#9FD8FF"), (0.6, "#3F6BC8"), (1, "#1E2E6E")], 0.4, 0.35, 0.85)}" stroke="{GOLD_LINE}" stroke-width="4"/>'
    for k in range(12):
        a = math.radians(k * 30)
        out += f'<circle cx="{math.cos(a) * 95:.1f}" cy="{math.sin(a) * 95:.1f}" r="5.5" fill="#FFF6C2" stroke="{GOLD_LINE}" stroke-width="2"/>'
    # 盾面上的貓臉紋章（貓版的戈爾貢）
    face_ = poly([(-38, -22), (-48, -62), (-14, -40)], gold(d), GOLD_LINE, 3) + poly([(38, -22), (48, -62), (14, -40)], gold(d), GOLD_LINE, 3)
    face_ += ellipse(0, 6, 50, 44, gold(d), GOLD_LINE, 3.5)
    face_ += ellipse(-18, 0, 8, 10, "#1E2E6E", "none") + ellipse(18, 0, 8, 10, "#1E2E6E", "none")
    face_ += path("M-10,22 Q-5,30 0,22 Q5,30 10,22", "none", GOLD_LINE, 3, 'stroke-linecap="round"')
    for s in (-1, 1):
        face_ += path(f"M{s * 44},-10 q{s * 20},-10 {s * 22},-30 M{s * 48},14 q{s * 22},4 {s * 28},-10", "none", GOLD_LINE, 5, 'stroke-linecap="round"')
    out += face_
    out += path("M-78,-50 A92,92 0 0 1 -20,-90", "none", "#FFFFFF", 6, 'opacity="0.75" stroke-linecap="round"')
    return out


ICONS = {"wuleifu": wuleifu, "dingshen": dingshen, "pantao": pantao, "bagua": bagua, "ziqi": ziqi, "jingubang": jingubang, "fenghuolun": fenghuolun,
         "thunderbolt": thunderbolt, "olympus": olympus, "feast": feast, "labyrinth": labyrinth, "sanctuary": sanctuary, "trident": trident, "aegis": aegis}
EAST_ICONS = {"wuleifu", "dingshen", "pantao", "bagua", "ziqi", "jingubang", "fenghuolun"}


# ───────────── 卡面背景（400×300，中心 (0,0)） ─────────────

def backdrop_east(d):
    """東方：晚霞、朝陽、遠山與祥雲。"""
    sky = d.lin([(0, "#3A1450"), (0.45, "#C8324A"), (0.8, "#FF9A4A"), (1, "#FFE08A")])
    out = f'<rect x="-200" y="-150" width="400" height="300" fill="{sky}"/>'
    out += glow(d, 0, 40, 190, "#FFE27A", 0.8)
    out += f'<circle cx="0" cy="50" r="92" fill="{d.rad([(0, "#FFFBE0"), (1, "#FFC14A")])}" opacity="0.9"/>'
    out += path("M-200,150 L-200,60 L-150,10 L-110,50 L-70,-10 L-20,60 L20,30 L70,70 L120,10 L160,48 L200,20 L200,150 Z", "#7A1E4A", "none", None, 'opacity="0.55"')
    out += path("M-200,150 L-200,100 L-140,70 L-90,104 L-30,78 L40,110 L100,84 L150,108 L200,90 L200,150 Z", "#4A1040", "none", None, 'opacity="0.7"')
    out += cloud(-140, -84, 44, "#FFE9D0", "#E08A6A", 2.5, 0.85) + cloud(150, -50, 38, "#FFE9D0", "#E08A6A", 2.5, 0.8) + cloud(-164, 120, 50, "#FFF3E0", "#E0A27A", 2.5, 0.9) + cloud(170, 126, 46, "#FFF3E0", "#E0A27A", 2.5, 0.9)
    return out


def backdrop_greek(d):
    """希臘：晴空、神殿石柱與橄欖枝。"""
    sky = d.lin([(0, "#2E4AA8"), (0.5, "#6FA8F2"), (1, "#E6F4FF")])
    out = f'<rect x="-200" y="-150" width="400" height="300" fill="{sky}"/>'
    out += glow(d, 0, 30, 190, "#FFFFFF", 0.75)
    out += cloud(-120, -90, 46, "#FFFFFF", "#9FB8E8", 2.5, 0.9) + cloud(130, -70, 40, "#FFFFFF", "#9FB8E8", 2.5, 0.85)
    marble = d.lin([(0, "#FFFFFF"), (0.5, "#E6EAF8"), (1, "#A8B0D0")], 0, 0, 1, 0)
    for s in (-1, 1):
        x = s * 168
        out += f'<rect x="{x - 26}" y="-150" width="52" height="300" fill="{marble}" stroke="#6B7098" stroke-width="3"/>'
        for k in (-14, 0, 14):
            out += f'<line x1="{x + k}" y1="-150" x2="{x + k}" y2="150" stroke="#8E94BE" stroke-width="3" opacity="0.5"/>'
        out += f'<rect x="{x - 34}" y="112" width="68" height="38" fill="{marble}" stroke="#6B7098" stroke-width="3"/>'
    out += path("M-200,150 L-200,120 Q0,96 200,120 L200,150 Z", "#F2F4FB", "#8E94BE", 2.5)
    return out


def backdrop_spell(d, color, dark="#1A1030"):
    """法術：暗底放射光。"""
    bg = d.rad([(0, color), (0.55, dark), (1, "#0A0618")], 0.5, 0.5, 0.75)
    out = f'<rect x="-200" y="-150" width="400" height="300" fill="{bg}"/>'
    for k in range(18):
        a = math.radians(k * 20)
        a2 = math.radians(k * 20 + 7)
        out += f'<path d="M0,0 L{math.cos(a) * 320:.1f},{math.sin(a) * 320:.1f} L{math.cos(a2) * 320:.1f},{math.sin(a2) * 320:.1f} Z" fill="{color}" opacity="0.22"/>'
    return out


def backdrop_enchant(d, color):
    """結界：深色底上的法陣。"""
    bg = d.rad([(0, "#3A2A6E"), (1, "#0E0A24")], 0.5, 0.5, 0.8)
    out = f'<rect x="-200" y="-150" width="400" height="300" fill="{bg}"/>'
    for r, dash in ((176, "4 10"), (150, "24 12"), (128, "2 8")):
        out += f'<circle cx="0" cy="0" r="{r}" fill="none" stroke="{color}" stroke-width="2.5" stroke-dasharray="{dash}" opacity="0.6"/>'
    return out + glow(d, 0, 0, 170, color, 0.45)


def backdrop_equip(d, color):
    """神器：聚光燈下的展示台。"""
    bg = d.lin([(0, "#141A3A"), (1, "#2E2A5A")])
    out = f'<rect x="-200" y="-150" width="400" height="300" fill="{bg}"/>'
    out += f'<path d="M-60,-150 L60,-150 L170,150 L-170,150 Z" fill="{d.lin([(0, color, 0.5), (1, color, 0.04)])}"/>'
    out += ellipse(0, 128, 150, 26, d.rad([(0, color, 0.6), (1, color, 0)]), "none")
    return out + sparkles([(-150, -90, 9), (140, -110, 11), (160, 20, 7), (-164, 40, 8)], "#FFFFFF")


ACCENT = {"wuleifu": "#E0A21A", "dingshen": "#3F8CE0", "pantao": "#E05A8A", "bagua": "#3FA88A", "ziqi": "#8A4AE0", "jingubang": "#E0A21A", "fenghuolun": "#E0621A",
          "thunderbolt": "#3F7AE0", "olympus": "#E0821A", "feast": "#8A3AC8", "labyrinth": "#8A9AB8", "sanctuary": "#5CB8FF", "trident": "#2EA8D8", "aegis": "#E0B02A"}


def card_art(name, backdrop, content, scale=0.98, dy=4):
    d = Defs()
    body = backdrop(d) + move(content(d), 0, dy, scale)
    return Sprite(name, 400, 300, "-200 -150 400 300", body, d.text())


# ───────────── 介面小圖 ─────────────

def incense(d):
    """香火：冒著煙的小香爐。"""
    out = glow(d, 0, 10, 110, "#FFB86A", 0.6)
    for x, h in ((-16, 96), (0, 116), (16, 96)):
        out += f'<line x1="{x}" y1="-6" x2="{x}" y2="{-h + 20}" stroke="#C8321E" stroke-width="5" stroke-linecap="round"/>'
        out += glow(d, x, -h + 18, 14, "#FFE27A", 1) + f'<circle cx="{x}" cy="{-h + 18}" r="4.5" fill="#FFF3A0"/>'
    out += path("M0,-104 C-22,-122 16,-134 -4,-150 M18,-84 C34,-100 12,-112 26,-128", "none", "#FFFFFF", 5, 'stroke-linecap="round" opacity="0.7"')
    out += path("M-74,-8 C-80,54 -50,84 0,84 C50,84 80,54 74,-8 Z", gold(d), GOLD_LINE, 5)
    out += ellipse(0, -8, 74, 16, d.lin([(0, "#8A5A1E"), (1, "#4A2A0A")]), GOLD_LINE, 4.5)
    out += path("M-50,30 C-40,58 -20,68 0,70", "none", "#FFFFFF", 6, 'opacity="0.5" stroke-linecap="round"')
    for s in (-1, 1):
        out += path(f"M{s * 72},4 q{s * 26},4 {s * 22},30 q{-s * 6},10 {-s * 18},6", "none", GOLD_LINE, 11, 'stroke-linecap="round"') + path(f"M{s * 72},4 q{s * 26},4 {s * 22},30 q{-s * 6},10 {-s * 18},6", "none", "#FFD24A", 5.5, 'stroke-linecap="round"')
        out += rrect(s * 40 - 9, 78, 18, 24, 5, gold(d), GOLD_LINE, 4)
    return out


def coin(d):
    out = glow(d, 0, 0, 120, "#FFE27A", 0.6)
    out += f'<circle cx="0" cy="0" r="96" fill="{gold(d)}" stroke="{GOLD_LINE}" stroke-width="6"/><circle cx="0" cy="0" r="76" fill="none" stroke="{GOLD_LINE}" stroke-width="3.5" opacity="0.8"/>'
    out += ellipse(0, 18, 30, 25, "#8A4B0C", "none")
    for dx, dy in ((-34, -8), (-13, -32), (13, -32), (34, -8)):
        out += ellipse(dx, dy, 13, 15, "#8A4B0C", "none")
    out += path("M-66,-40 A80,80 0 0 1 -10,-80", "none", "#FFFFFF", 8, 'opacity="0.8" stroke-linecap="round"')
    return out


def card_back(d):
    """卡背：深紫底、金框、中間是貓掌與八卦／月桂。"""
    bg = d.lin([(0, "#4A2E9A"), (1, "#1E1250")])
    out = f'<rect x="-150" y="-210" width="300" height="420" rx="26" fill="{bg}" stroke="{GOLD_LINE}" stroke-width="8"/>'
    out += f'<rect x="-132" y="-192" width="264" height="384" rx="18" fill="none" stroke="#FFD24A" stroke-width="4"/>'
    out += glow(d, 0, 0, 150, "#B88AFF", 0.6)
    for r, dash in ((112, "6 10"), (96, "30 10")):
        out += f'<circle cx="0" cy="0" r="{r}" fill="none" stroke="#FFD24A" stroke-width="3" stroke-dasharray="{dash}" opacity="0.85"/>'
    out += f'<circle cx="0" cy="0" r="78" fill="{d.rad([(0, "#6A4AD0"), (1, "#2A1A6E")])}" stroke="#FFD24A" stroke-width="4"/>'
    out += ellipse(0, 20, 30, 25, "#FFD24A", GOLD_LINE, 3)
    for dx, dy in ((-34, -6), (-13, -30), (13, -30), (34, -6)):
        out += ellipse(dx, dy, 13, 15, "#FFD24A", GOLD_LINE, 3)
    for sx in (-1, 1):
        for sy in (-1, 1):
            out += sparkles([(sx * 104, sy * 164, 14)], "#FFE9A0")
    return out


def pack(d):
    """卡包。"""
    out = glow(d, 0, 0, 180, "#FFD24A", 0.55)
    body = d.lin([(0, "#FF7A5A"), (0.5, "#C8324A"), (1, "#5A1450")], 0, 0, 1, 1)
    out += path("M-110,-170 L-90,-180 L-70,-170 L-50,-180 L-30,-170 L-10,-180 L10,-170 L30,-180 L50,-170 L70,-180 L90,-170 L110,-180 L110,180 L90,170 L70,180 L50,170 L30,180 L10,170 L-10,180 L-30,170 L-50,180 L-70,170 L-90,180 L-110,170 Z", body, "#3A0A2A", 6)
    out += path("M-110,-140 L110,-140 M-110,140 L110,140", "none", "#FFD24A", 4)
    out += f'<circle cx="0" cy="-10" r="74" fill="{d.rad([(0, "#FFE9A0"), (1, "#E0921A")])}" stroke="{GOLD_LINE}" stroke-width="5"/>'
    out += ellipse(0, 6, 28, 23, "#8A4B0C", "none")
    for dx, dy in ((-32, -18), (-12, -40), (12, -40), (32, -18)):
        out += ellipse(dx, dy, 12, 14, "#8A4B0C", "none")
    out += path("M-92,-150 C-70,-60 -70,60 -92,150", "none", "#FFFFFF", 10, 'opacity="0.25" stroke-linecap="round"')
    out += text(0, 112, "神靈卡包", 34, "#FFF6C2", stroke="#3A0A2A", sw=6)
    return out


def star_icon(d, on=True):
    fill = gold(d) if on else "#3A3460"
    line = GOLD_LINE if on else "#1E1A3A"
    out = glow(d, 0, 0, 110, "#FFE27A", 0.6) if on else ""
    out += f'<polygon points="{star_points(0, 4, 96, 0.48)}" fill="{fill}" stroke="{line}" stroke-width="8" stroke-linejoin="round"/>'
    if on:
        out += path("M-40,-10 L-14,-34 L-4,-70", "none", "#FFFFFF", 8, 'opacity="0.7" stroke-linecap="round" stroke-linejoin="round"')
    return out


def lobby_icon(d):
    """大廳圖示：孫悟空與宙斯背對背。"""
    import divine_gods
    out = glow(d, 0, 0, 130, "#FFD24A", 0.5)
    out += move(divine_gods.zeus(d), 46, 10, 0.62) + move(divine_gods.wukong(d), -46, 14, 0.62)
    return out


def simple(name, draw, w, h, view):
    d = Defs()
    body = draw(d)
    return Sprite(name, w, h, view, body, d.text())


# ───────────── 戰場（1000×1400） ─────────────

def field(d):
    """幽冥鬼域：上方是鬼門，五條石板路往下通到神殿前的廣場。"""
    import random
    rnd = random.Random(7)
    bg = d.lin([(0, "#150B2E"), (0.35, "#2A1650"), (0.75, "#3A2466"), (1, "#4A3478")])
    out = f'<rect x="0" y="0" width="1000" height="1400" fill="{bg}"/>'
    # 遠方的鬼火與霧
    out += glow(d, 500, 60, 520, "#7A3AD6", 0.55)
    for _ in range(26):
        x, y, r = rnd.uniform(0, 1000), rnd.uniform(120, 820), rnd.uniform(60, 150)
        out += f'<ellipse cx="{x:.0f}" cy="{y:.0f}" rx="{r:.0f}" ry="{r * 0.4:.0f}" fill="#8A6BD6" opacity="0.07"/>'
    # 五條石板路
    for lane in range(5):
        x0 = lane * 200
        road = d.lin([(0, "#2E2152"), (1, "#5A4A8E")])
        out += f'<rect x="{x0 + 14}" y="0" width="172" height="1330" rx="18" fill="{road}" stroke="#1A1034" stroke-width="5"/>'
        out += f'<rect x="{x0 + 22}" y="0" width="156" height="1330" rx="14" fill="none" stroke="#8A7AC8" stroke-width="2" opacity="0.35"/>'
        y = 30
        row = 0
        while y < 1300:
            h = rnd.uniform(70, 96)
            off = 78 if row % 2 else 0
            out += f'<line x1="{x0 + 22}" y1="{y:.0f}" x2="{x0 + 178}" y2="{y:.0f}" stroke="#1A1034" stroke-width="3" opacity="0.45"/>'
            for xx in (x0 + 60 + off * 0.5, x0 + 138 - off * 0.5):
                out += f'<line x1="{xx:.0f}" y1="{y:.0f}" x2="{xx:.0f}" y2="{y + h:.0f}" stroke="#1A1034" stroke-width="3" opacity="0.35"/>'
            if rnd.random() < 0.3:
                cx, cy = x0 + rnd.uniform(40, 160), y + h * 0.5
                out += f'<path d="M{cx:.0f},{cy:.0f} l10,8 l-4,10 l12,6" fill="none" stroke="#1A1034" stroke-width="2.5" opacity="0.5" stroke-linecap="round"/>'
            y += h
            row += 1
    # 車道之間的石燈籠
    for gx in (200, 400, 600, 800):
        for gy in (260, 560):
            out += glow(d, gx, gy, 70, "#62FFD8", 0.5)
            out += rrect(gx - 9, gy - 4, 18, 44, 3, "#3A2E5E", "#140C28", 3) + rrect(gx - 17, gy - 30, 34, 28, 5, "#62FFD8", "#140C28", 3, 'opacity="0.95"')
            out += poly([(gx - 24, gy - 30), (gx, gy - 52), (gx + 24, gy - 30)], "#4A3E72", "#140C28", 3)
    # 鬼門
    gate = d.lin([(0, "#8A1E3A"), (1, "#3A0A1E")])
    out += f'<rect x="0" y="0" width="1000" height="74" fill="{d.lin([(0, "#0A0518"), (1, "#2A1650")])}"/>'
    out += path("M-20,74 L-20,40 Q500,-40 1020,40 L1020,74 Q500,6 -20,74 Z", gate, "#14051A", 5)
    out += path("M0,56 Q500,-12 1000,56", "none", "#FFD24A", 4, 'opacity="0.8"')
    for x in (100, 300, 500, 700, 900):
        out += glow(d, x, 60, 60, "#FF5A9A", 0.6) + f'<circle cx="{x}" cy="60" r="11" fill="#FF7AB0" stroke="#3A0A1E" stroke-width="3"/>'
    # 格子（可以放生物的地方）
    for lane in range(5):
        for ry in (880, 1040, 1200):
            cx = lane * 200 + 100
            out += f'<ellipse cx="{cx}" cy="{ry + 46}" rx="78" ry="34" fill="{d.rad([(0, "#FFE9A0", 0.32), (1, "#FFE9A0", 0)])}"/>'
            out += f'<ellipse cx="{cx}" cy="{ry + 46}" rx="70" ry="28" fill="none" stroke="#FFE27A" stroke-width="2.5" stroke-dasharray="10 9" opacity="0.5"/>'
    # 神殿前的臺階
    out += f'<rect x="0" y="1318" width="1000" height="82" fill="{d.lin([(0, "#FFE9A0"), (0.2, "#E8B04A"), (1, "#8A4B1E")])}"/>'
    out += f'<rect x="0" y="1318" width="1000" height="10" fill="#FFF6C2"/><line x1="0" y1="1352" x2="1000" y2="1352" stroke="#8A4B0C" stroke-width="3" opacity="0.6"/>'
    out += glow(d, 500, 1330, 520, "#FFD24A", 0.35)
    for x in range(40, 1000, 80):
        out += poly([(x - 16, 1340), (x, 1326), (x + 16, 1340), (x, 1354)], "#C8321E", "#5A1010", 2.5, 'opacity="0.9"')
    return out
