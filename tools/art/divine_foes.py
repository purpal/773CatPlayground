"""喵神封魔錄的妖魔：圓滾滾但帶點邪氣，配色偏冷、偏暗，和神靈的暖金色區隔。"""
import math
from divine_kit import *

FOE_LINE = "#1E1430"


def blob(d, light, base, dark, w=150, h=132, cy=14, line=FOE_LINE):
    shape = mochi_path(0, cy, w, h)
    g = d.rad([(0, light), (0.5, base), (1, dark)], 0.38, 0.3, 0.82)
    clip = d.clip(shape)
    inner = ellipse(0, cy + h * 0.52, w * 0.55, h * 0.22, dark, "none", None, 'opacity="0.7"')
    inner += path(f"M{-w * 0.36},{cy - h * 0.3} C{-w * 0.28},{cy - h * 0.44} {-w * 0.1},{cy - h * 0.48} {w * 0.04},{cy - h * 0.48}", "none", "#FFFFFF", 6, 'stroke-linecap="round" opacity="0.45"')
    return path(shape, g, line, 4.5) + f'<g clip-path="{clip}">{inner}</g>', shape


def evil_eye(d, cx, cy, r, iris="#FFE45C", s=1):
    """斜斜的壞壞眼。"""
    g = d.rad([(0, "#FFFFFF"), (0.3, iris), (1, "#3A1010")], 0.5, 0.7, 0.8)
    out = ellipse(cx, cy, r, r * 1.05, g, FOE_LINE, 2.8)
    out += ellipse(cx, cy + r * 0.1, r * 0.3, r * 0.6, "#140A14", "none")
    out += ellipse(cx + r * 0.35, cy - r * 0.4, r * 0.28, r * 0.28, "#FFFFFF", "none")
    out += path(f"M{cx - s * (r + 5)},{cy - r * 0.2} L{cx + s * (r + 3)},{cy - r * 1.25}", "none", FOE_LINE, 6, 'stroke-linecap="round"')
    return out


def grin(cx, cy, w, fangs=True):
    out = path(f"M{cx - w},{cy} Q{cx},{cy + w * 0.9} {cx + w},{cy} Q{cx},{cy + w * 0.3} {cx - w},{cy} Z", "#3A0A1E", FOE_LINE, 3)
    if fangs:
        for s in (-1, 1):
            out += poly([(cx + s * w * 0.62, cy + 2), (cx + s * w * 0.42, cy + w * 0.5), (cx + s * w * 0.25, cy + 5)], "#FFFFFF", FOE_LINE, 1.8)
    return out


def imp(d):
    """小鬼：紅皮膚、一支角、亂髮、虎皮兜襠、小狼牙棒。"""
    L = []
    club = shaft(d, 0, -20, 70, 9, "#8A5A3A", FOE_LINE) + path("M-14,-20 C-18,-62 18,-62 14,-20 Z", d.lin([(0, "#8A93A8"), (1, "#3F485C")]), FOE_LINE, 3.5)
    for x, y in ((-12, -34), (12, -34), (0, -52), (-9, -48), (9, -48)):
        club += poly([(x - 4, y), (x, y - 9), (x + 4, y)], "#DCE2F2", FOE_LINE, 1.8)
    L.append(move(rot(club, 22), 78, 14))
    body, shape = blob(d, "#FF9A8A", "#E0503E", "#8A1E1E")
    L.append(body)
    clip = d.clip(shape)
    skirt = path("M-90,58 Q0,76 90,58 L90,100 L-90,100 Z", "#FFB23E", "#5A2C10", 3) + "".join(path(f"M{x},66 l6,12 l6,-10", "none", "#5A2C10", 4, 'stroke-linecap="round" stroke-linejoin="round"') for x in (-54, -24, 6, 36))
    L.append(f'<g clip-path="{clip}">{skirt}</g>' + path(shape, "none", FOE_LINE, 4.5))
    L.append(poly([(-8, -48), (4, -92), (16, -46)], d.lin([(0, "#FFFBE0"), (1, "#E0B25A")]), FOE_LINE, 3.5))
    for s in (-1, 1):
        L.append(path(f"M{s * 24},-46 q{s * 10},-20 {s * 26},-14 q-{s * 2},10 {-s * 6},22 Z".replace("--", ""), "#3A1A2A", FOE_LINE, 3))
        L.append(evil_eye(d, s * 28, 2, 12, "#FFE45C", s))
    L.append(grin(0, 26, 20))
    L.append(ellipse(-34, 68, 16, 10, "#FF9A8A", FOE_LINE, 3.5) + ellipse(34, 68, 16, 10, "#FF9A8A", FOE_LINE, 3.5) + ellipse(72, 34, 13, 11, "#FF9A8A", FOE_LINE, 3.5))
    return figure(d, L)


def zombie(d):
    """殭屍：青白皮膚、清朝官帽、額頭貼符、雙手往前伸、長指甲。"""
    L = []
    body, shape = blob(d, "#E6FFF0", "#A8D8C4", "#5E9A8A", 150, 138)
    L.append(body)
    clip = d.clip(shape)
    robe_ = path("M-90,40 Q0,62 90,40 L90,100 L-90,100 Z", d.lin([(0, "#3A4A8A"), (1, "#1A2250")]), FOE_LINE, 3)
    robe_ += path("M0,54 L0,100", "none", "#FFD24A", 3) + "".join(f'<circle cx="0" cy="{y}" r="4" fill="#FFD24A" stroke="{FOE_LINE}" stroke-width="1.5"/>' for y in (62, 78))
    L.append(f'<g clip-path="{clip}">{robe_}</g>' + path(shape, "none", FOE_LINE, 4.5))
    for s in (-1, 1):
        L.append(ellipse(s * 28, 6, 13, 13, "#1E2A3A", FOE_LINE, 2.5) + ellipse(s * 28, 8, 5, 5, "#FF5A6E", "none") + ellipse(s * 30, 4, 2.4, 2.4, "#FFFFFF", "none"))
    L.append(path("M-14,30 q14,-8 28,0", "none", FOE_LINE, 3.5, 'stroke-linecap="round"'))
    L.append(poly([(-8, 28), (-5, 36), (-2, 28)], "#FFFFFF", FOE_LINE, 1.5) + poly([(2, 28), (5, 36), (8, 28)], "#FFFFFF", FOE_LINE, 1.5))
    # 官帽
    L.append(ellipse(0, -46, 72, 15, d.lin([(0, "#3A3A5A"), (1, "#14142A")]), FOE_LINE, 3.5))
    L.append(path("M-40,-50 C-38,-86 38,-86 40,-50 Z", d.lin([(0, "#E0503E"), (1, "#8A1E1E")]), FOE_LINE, 3.5))
    L.append(gem(d, 0, -86, 8, "#62D8FF"))
    # 符
    L.append(rot(rrect(-13, -46, 26, 56, 3, d.lin([(0, "#FFF3A0"), (1, "#F2C83A")]), "#8A4B0C", 2.5), 4, 0, -40))
    L.append(path("M-4,-34 l8,0 M0,-36 l0,34 M-6,-22 l12,0 M-6,-10 q6,6 12,0", "none", "#C81E1E", 2.6, 'stroke-linecap="round"'))
    # 往前伸的手
    for s in (-1, 1):
        L.append(rrect(s * 62 - 14, 26, 28, 20, 9, "#C9F0DE", FOE_LINE, 3.5))
        for k in range(3):
            x = s * 62 - 9 + k * 9
            L.append(poly([(x - 3, 46), (x, 58), (x + 3, 46)], "#F4FFF8", FOE_LINE, 1.6))
    return figure(d, L)


def fox(d):
    """狐妖：紫白色的小狐狸、尖耳、額頭火焰紋、尾巴尖是紫色狐火。"""
    L = []
    tail = d.lin([(0, "#F4E6FF"), (1, "#B06BFF")])
    L.append(path("M50,60 C112,60 120,-6 86,-50 C92,-8 70,22 44,26 Z", tail, FOE_LINE, 4))
    L.append(glow(d, 88, -60, 34, "#C88BFF", 0.85) + flame(d, 86, -42, 34, "#FFFFFF", "#D8A8FF", "#7A3AD6"))
    g = d.lin([(0, "#FFFFFF"), (1, "#D8C8F2")])
    for s in (-1, 1):
        L.append(poly([(s * 24, -44), (s * 54, -104), (s * 72, -36)], g, FOE_LINE, 4.5))
        L.append(poly([(s * 36, -48), (s * 54, -86), (s * 64, -42)], "#B06BFF", FOE_LINE, 0))
    body, shape = blob(d, "#FFFFFF", "#EBDFFB", "#A88BD8", 146, 126)
    L.append(body)
    L.append(path("M-12,26 L0,40 L12,26 C8,14 -8,14 -12,26 Z", "#FFFFFF", FOE_LINE, 3))
    for s in (-1, 1):
        L.append(evil_eye(d, s * 30, 0, 11, "#FF5ACD", s))
        L.append(path(f"M{s * 44},16 q{s * 14},-2 {s * 22},6", "none", "#C81E6E", 3, 'stroke-linecap="round"'))
    L.append(ellipse(0, 20, 6, 4.5, "#3A0A2A", "none"))
    L.append(path("M-9,30 Q0,38 9,30", "none", FOE_LINE, 3, 'stroke-linecap="round"'))
    L.append(path("M0,-44 C-8,-34 -6,-26 0,-20 C6,-26 8,-34 0,-44 Z", "#E0263A", "#7A1018", 2))
    for s in (-1, 1):
        L.append(ellipse(s * 30, 68, 16, 10, "#FFFFFF", FOE_LINE, 3.5))
    return figure(d, L)


def bat(d, frame=0):
    """蝙蝠精：紫色圓身、大耳、小獠牙；frame 0 翅膀張開、1 往下拍。"""
    L = []
    wing_fill = d.lin([(0, "#8A5AD6"), (1, "#3A1E6E")])
    tilt = -10 if frame == 0 else 26
    for s in (-1, 1):
        w = path("M0,0 C30,-44 80,-50 112,-30 C100,-20 100,-8 108,2 C92,-2 82,6 84,18 C68,10 56,16 54,28 C40,16 16,12 0,0 Z", wing_fill, FOE_LINE, 4)
        w += path("M4,0 C36,-22 76,-30 104,-26 M8,4 C36,-6 62,-2 82,12", "none", FOE_LINE, 2.5, 'opacity="0.55"')
        L.append(f'<g transform="translate({s * 44} 0) scale({s} 1) rotate({tilt})">{w}</g>')
    for s in (-1, 1):
        L.append(poly([(s * 16, -36), (s * 44, -92), (s * 58, -30)], d.lin([(0, "#B08AF2"), (1, "#6A3AB8")]), FOE_LINE, 4.5))
        L.append(poly([(s * 28, -40), (s * 43, -74), (s * 51, -36)], "#FF9AC8", FOE_LINE, 0))
    body, shape = blob(d, "#C8A8FF", "#8A5AD6", "#4A2A8A", 112, 104, 10)
    L.append(body)
    for s in (-1, 1):
        L.append(evil_eye(d, s * 22, 4, 11, "#FF5A6E", s))
    L.append(grin(0, 26, 16))
    for s in (-1, 1):
        L.append(ellipse(s * 16, 62, 9, 6, "#4A2A8A", FOE_LINE, 3))
    return move(figure(d, L, shadow=False), 0, 0, 0.8)


def skeleton(d):
    """骷髏弓手：圓圓的骷髏頭、破斗篷、拉著一把骨弓。"""
    L = []
    cloak = d.lin([(0, "#4A5A7A"), (1, "#1E2A44")])
    L.append(path("M-78,10 C-104,60 -92,96 -70,102 L-52,90 L-34,104 L-12,90 L10,104 L32,90 L52,104 L72,96 C96,84 100,50 78,10 Z", cloak, FOE_LINE, 4))
    skull, shape = blob(d, "#FFFFFF", "#F2EEDC", "#B8B094", 136, 118, -6)
    L.append(skull)
    for s in (-1, 1):
        L.append(ellipse(s * 27, -6, 15, 17, "#1E1430", FOE_LINE, 2.5))
        L.append(glow(d, s * 27, -4, 16, "#62FFD8", 0.95) + f'<circle cx="{s * 27}" cy="-4" r="4.5" fill="#BFFFF0"/>')
        L.append(path(f"M{s * 10},-26 L{s * 44},-36", "none", FOE_LINE, 5.5, 'stroke-linecap="round"'))
    L.append(poly([(-5, 16), (0, 6), (5, 16)], "#1E1430", FOE_LINE, 1.5))
    L.append(path("M-26,32 Q0,44 26,32", "none", FOE_LINE, 3.5, 'stroke-linecap="round"'))
    for x in (-16, -6, 4, 14):
        L.append(path(f"M{x},33 l0,9", "none", FOE_LINE, 2.5, 'stroke-linecap="round"'))
    L.append(path("M-30,-52 l10,12 l8,-10", "none", FOE_LINE, 3, 'stroke-linecap="round" stroke-linejoin="round" opacity="0.7"'))
    bow = path("M0,-70 C40,-44 40,44 0,70", "none", FOE_LINE, 12, 'stroke-linecap="round"') + path("M0,-70 C40,-44 40,44 0,70", "none", "#F2EEDC", 7, 'stroke-linecap="round"')
    bow += path("M0,-70 L0,70", "none", "#DCE8FF", 2) + path("M-22,0 L52,0", "none", "#8A93A8", 4, 'stroke-linecap="round"') + poly([(52, -7), (70, 0), (52, 7)], "#62FFD8", FOE_LINE, 2.5)
    L.append(move(rot(bow, 80), 70, 60, 0.8))
    L.append(ellipse(-30, 70, 14, 9, "#F2EEDC", FOE_LINE, 3.5) + ellipse(52, 58, 12, 10, "#F2EEDC", FOE_LINE, 3.5))
    return figure(d, L)


def oxhead(d):
    """牛頭：棕色大塊頭、彎牛角、鼻環、鐵鍊護肩，扛著一把大斧。"""
    L = []
    axe = shaft(d, 0, -70, 100, 12, d.lin([(0, "#8A5A3A"), (1, "#4A2A14")], 0, 0, 1, 0), FOE_LINE)
    blade = d.lin([(0, "#FFFFFF"), (0.5, "#B8C4D8"), (1, "#5E6980")], 0, 0, 1, 0)
    for s in (-1, 1):
        axe += path(f"M{s * 4},-96 C{s * 56},-116 {s * 70},-60 {s * 44},-36 C{s * 42},-58 {s * 24},-66 {s * 4},-62 Z", blade, FOE_LINE, 4)
        axe += path(f"M{s * 50},-96 C{s * 60},-80 {s * 58},-60 {s * 48},-48", "none", "#FFFFFF", 3, 'opacity="0.8" stroke-linecap="round"')
    L.append(move(rot(axe, 14), 96, 6))
    horn = d.lin([(0, "#FFFBE0"), (1, "#C89A4A")])
    for s in (-1, 1):
        L.append(path(f"M{s * 44},-48 C{s * 80},-56 {s * 104},-84 {s * 96},-124 C{s * 80},-96 {s * 60},-84 {s * 34},-70 Z", horn, FOE_LINE, 4.5))
        L.append(ellipse(s * 72, -34, 22, 13, "#A8693A", FOE_LINE, 4, f'transform="rotate({s * 24} {s * 72} -34)"'))
    body, shape = blob(d, "#D69A6A", "#A8693A", "#5A2C14", 178, 160, 12)
    L.append(body)
    clip = d.clip(shape)
    armor = path("M-110,54 Q0,78 110,54 L110,110 L-110,110 Z", d.lin([(0, "#5A4A6E"), (1, "#2A2140")]), FOE_LINE, 3.5)
    armor += "".join(f'<circle cx="{x}" cy="70" r="6" fill="{steel(d)}" stroke="{FOE_LINE}" stroke-width="2"/>' for x in (-60, -30, 0, 30, 60))
    L.append(f'<g clip-path="{clip}">{armor}</g>' + path(shape, "none", FOE_LINE, 4.5))
    for s in (-1, 1):
        L.append(evil_eye(d, s * 36, -14, 13, "#FF5A3A", s))
    L.append(ellipse(0, 26, 44, 28, d.lin([(0, "#F2C8A0"), (1, "#C88A5A")]), FOE_LINE, 4))
    for s in (-1, 1):
        L.append(ellipse(s * 16, 24, 7, 9, "#5A2C14", "none"))
    L.append(f'<circle cx="0" cy="42" r="13" fill="none" stroke="{GOLD_LINE}" stroke-width="7"/><circle cx="0" cy="42" r="13" fill="none" stroke="#FFD24A" stroke-width="3.5"/>')
    L.append(ellipse(-44, 84, 22, 13, "#D69A6A", FOE_LINE, 4) + ellipse(44, 84, 22, 13, "#D69A6A", FOE_LINE, 4) + ellipse(92, 40, 17, 15, "#D69A6A", FOE_LINE, 4))
    return figure(d, L)


def ninetail(d):
    """九尾妖狐：大白狐、九條尾巴像扇子一樣展開（尾尖紫紅）、紅色妖紋、額頭寶珠、金色鈴鐺。"""
    L = []
    L.append(glow(d, 0, -10, 150, "#C88BFF", 0.55))
    for k in range(9):
        a = -80 + k * 20
        tail = d.lin([(0, "#FF7ACD"), (0.35, "#F4E6FF"), (1, "#FFFFFF")])
        t = path("M-17,0 C-34,-60 -24,-112 0,-140 C24,-112 34,-60 17,0 Z", tail, FOE_LINE, 4)
        t += path("M0,-128 C-9,-100 -8,-60 -4,-20", "none", "#FFFFFF", 4, 'opacity="0.7" stroke-linecap="round"')
        L.append(f'<g transform="translate(0 40) rotate({a})">{t}</g>')
    g = d.lin([(0, "#FFFFFF"), (1, "#E6D8F8")])
    for s in (-1, 1):
        L.append(poly([(s * 26, -46), (s * 60, -116), (s * 78, -38)], g, FOE_LINE, 4.5))
        L.append(poly([(s * 40, -50), (s * 60, -94), (s * 69, -44)], "#FF5A9A", FOE_LINE, 0))
    body, shape = blob(d, "#FFFFFF", "#F6EEFF", "#B8A0E0", 164, 142, 12)
    L.append(body)
    for s in (-1, 1):
        L.append(evil_eye(d, s * 33, 0, 14, "#FFB02A", s))
        L.append(path(f"M{s * 50},-14 q{s * 16},-10 {s * 30},-4 M{s * 52},20 q{s * 14},-2 {s * 24},8 M{s * 50},30 q{s * 12},2 {s * 18},12", "none", "#E0263A", 3.5, 'stroke-linecap="round"'))
    L.append(ellipse(0, 22, 7, 5, "#3A0A2A", "none") + path("M-11,33 Q0,42 11,33", "none", FOE_LINE, 3.2, 'stroke-linecap="round"'))
    L.append(glow(d, 0, -34, 26, "#FF5ACD", 0.9) + gem(d, 0, -34, 9, "#FF5ACD", "#6B1048"))
    L.append(path("M-66,52 Q0,76 66,52", "none", "#7A1018", 11, 'stroke-linecap="round"') + path("M-66,52 Q0,76 66,52", "none", "#E0263A", 6.5, 'stroke-linecap="round"'))
    L.append(f'<circle cx="0" cy="72" r="11" fill="{gold(d)}" stroke="{GOLD_LINE}" stroke-width="3"/>' + path("M-7,74 L7,74 M0,74 L0,81", "none", GOLD_LINE, 2.2))
    for s in (-1, 1):
        L.append(ellipse(s * 36, 84, 19, 11, "#FFFFFF", FOE_LINE, 3.5))
    return move(figure(d, L, shadow=False), 0, 6, 0.86)


ALL = {"imp": imp, "zombie": zombie, "fox": fox, "bat": bat, "skeleton": skeleton, "oxhead": oxhead, "ninetail": ninetail}
