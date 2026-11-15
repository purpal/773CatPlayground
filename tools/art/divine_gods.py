"""喵神封魔錄的神靈（貓咪扮相）。每個函式回傳一張圖的 SVG 內容，座標見 divine_kit。"""
import math
from divine_kit import *


def laurel(d, y=-52, color=None):
    """月桂冠：兩排葉子沿著頭頂彎上去。"""
    out = ""
    fill = color or gold(d)
    line = GOLD_LINE if color is None else "#2E6B3A"
    for s in (-1, 1):
        for k in range(5):
            x = s * (66 - k * 12)
            yy = y + 8 - math.sin(math.radians(20 + k * 20)) * 30
            leaf = path(f"M{x},{yy} q{s * 9},-11 0,-21 q{-s * 9},10 0,21 Z", fill, line, 2.2)
            out += rot(leaf, s * (38 - k * 13), x, yy)
    return out


# ───────────── 東方 ─────────────

def wukong(d):
    """孫悟空：金毛、猴臉、金箍、雉雞翎、紅圍巾、虎皮裙，踩著筋斗雲，手拿金箍棒。"""
    fur = "gold"
    p = FURS[fur]
    L = []
    L.append(cloud(0, 92, 62, "#FFFFFF", "#B9C6E4", 3))
    L.append(cloud(-48, 100, 30, "#FFF4D6", "#E0B878", 2.5))
    # 雉雞翎
    for s in (-1, 1):
        f = d.lin([(0, "#FF5A4A"), (0.5, "#FFC14A"), (1, "#8A3B1E")])
        L.append(path(f"M{s * 22},-70 C{s * 40},-118 {s * 84},-140 {s * 112},-128 C{s * 86},-126 {s * 52},-100 {s * 34},-66 Z", f, "#7A2A12", 3))
        for k in range(4):
            x, y = s * (48 + k * 16), -108 - k * 6
            L.append(path(f"M{x - s * 6},{y + 8} L{x + s * 4},{y - 2}", "none", "#7A2A12", 2, 'opacity="0.6"'))
    L.append(cat_body(d, fur))
    # 猴臉：心形的淺色臉
    L.append(path("M0,-34 C-22,-52 -62,-40 -58,-2 C-56,30 -24,46 0,46 C24,46 56,30 58,-2 C62,-40 22,-52 0,-34 Z", "#FFEFD0", "none", None, 'opacity="0.92"'))
    L.append(robe(d, "#E23B2E", "#A81E1E", trim="#FFD24A", line="#6B1414", neck=40))
    # 虎皮裙的斑紋
    clip = d.clip(BODY)
    L.append(f'<g clip-path="{clip}">' + path("M-100,72 Q0,92 100,72 L100,100 L-100,100 Z", "#FFB23E", "#8A4B0C", 2.5)
             + "".join(path(f"M{x},78 l6,14 l6,-12", "none", "#5A2C10", 4, 'stroke-linecap="round" stroke-linejoin="round"') for x in (-60, -30, 0, 30, 60)) + "</g>")
    L.append(path(BODY, "none", p["line"], 4.5))
    L.append(face(d, fur, iris="#E8621E", mood="fierce", brow="#8A4B0C"))
    # 金箍
    L.append(path("M-62,-48 Q0,-70 62,-48", "none", GOLD_LINE, 12, 'stroke-linecap="round"'))
    L.append(path("M-62,-48 Q0,-70 62,-48", "none", gold(d, False), 7, 'stroke-linecap="round"'))
    L.append(path("M-14,-60 q-8,-14 4,-14 q10,0 6,10 M14,-60 q8,-14 -4,-14 q-10,0 -6,10", "none", GOLD_LINE, 6, 'stroke-linecap="round"'))
    L.append(path("M-14,-60 q-8,-14 4,-14 q10,0 6,10 M14,-60 q8,-14 -4,-14 q-10,0 -6,10", "none", "#FFD24A", 3, 'stroke-linecap="round"'))
    # 金箍棒
    staff = shaft(d, 0, -128, 96, 13, d.lin([(0, "#FF6A4A"), (0.5, "#D62A1E"), (1, "#8A1410")], 0, 0, 1, 0), "#5A0E0A", caps=gold(d))
    L.append(glow(d, 92, -110, 30, "#FFE27A", 0.9))
    L.append(move(rot(staff, 10), 84, 0))
    L.append(paws(fur, hold=[(80, 30)]))
    return figure(d, L, aura="#FFD24A")


def menshen(d):
    """門神：灰虎斑、怒目、將軍盔（紅纓）、金紅鎧甲，手持大斧。"""
    fur = "gray"
    p = FURS[fur]
    L = []
    # 背後的靠旗
    for s in (-1, 1):
        f = d.lin([(0, "#FF5A4A"), (1, "#A81E1E")])
        L.append(rot(poly([(s * 46, -40), (s * 96, -118), (s * 70, -40)], f, "#6B1414", 3), s * 6, s * 50, -40))
        L.append(path(f"M{s * 62},-52 L{s * 84},-96", "none", "#FFD24A", 3, 'opacity="0.9"'))
    L.append(cat_body(d, fur))
    L.append(robe(d, "#D8352B", "#8E1717", trim=gold(d, False), line="#5A1010", neck=36, sash="#FFC93C"))
    # 護心鏡
    L.append(f'<circle cx="0" cy="62" r="17" fill="{gold(d)}" stroke="{GOLD_LINE}" stroke-width="3"/>' + gem(d, 0, 62, 9, "#62D8FF"))
    # 肩甲
    for s in (-1, 1):
        L.append(path(f"M{s * 52},36 C{s * 78},22 {s * 100},40 {s * 96},64 C{s * 80},56 {s * 62},56 {s * 52},36 Z", gold(d), GOLD_LINE, 3))
        L.append(path(f"M{s * 64},40 C{s * 78},36 {s * 88},44 {s * 90},54", "none", "#FFFFFF", 2.5, 'opacity="0.7" stroke-linecap="round"'))
    L.append(face(d, fur, iris="#F2B02A", mood="fierce", brow="#2A2E44"))
    # 頭盔
    helm = d.lin([(0, "#FFE9A0"), (0.5, "#F2A82A"), (1, "#B8690F")])
    L.append(path("M-66,-44 C-62,-92 62,-92 66,-44 C40,-58 -40,-58 -66,-44 Z", helm, GOLD_LINE, 3.5))
    L.append(path("M-50,-62 C-30,-80 30,-80 50,-62", "none", "#FFFFFF", 3, 'opacity="0.6" stroke-linecap="round"'))
    L.append(gem(d, 0, -58, 9, "#FF5A6E"))
    L.append(path("M0,-88 C-16,-112 -6,-128 0,-136 C6,-128 16,-112 0,-88 Z", d.lin([(0, "#FF8A6A"), (1, "#C81E1E")]), "#6B1414", 3))
    L.append(rrect(-6, -94, 12, 12, 3, gold(d), GOLD_LINE, 2.5))
    # 大斧
    L.append(move(rot(shaft(d, 0, -96, 96, 11, d.lin([(0, "#B8793E"), (1, "#6B3E1A")], 0, 0, 1, 0), "#3A1E0A"), -8), -88, 0))
    blade = d.lin([(0, "#FFFFFF"), (0.5, "#C9D3E2"), (1, "#6E7A90")], 0, 0, 1, 0)
    L.append(path("M-78,-92 C-128,-104 -136,-52 -112,-36 C-112,-58 -96,-66 -80,-62 Z", blade, STEEL_LINE, 3.5))
    L.append(path("M-116,-84 C-126,-70 -124,-52 -114,-44", "none", "#FFFFFF", 3, 'opacity="0.8" stroke-linecap="round"'))
    L.append(paws(fur, hold=[(-84, 28)]))
    return figure(d, L, aura="#FF8A5A")


# ───────────── 希臘 ─────────────

def zeus(d):
    """宙斯：白毛大鬍子、金月桂冠、白袍紫金披帶，手握發光的閃電，腳下是暴風雲。"""
    fur = "white"
    p = FURS[fur]
    L = []
    L.append(halo(d, -12, 98, "#9FD8FF", rays=12))
    L.append(cloud(0, 94, 70, "#7C88B8", "#3F4878", 3))
    L.append(cloud(52, 102, 34, "#A9B4DC", "#4A5488", 2.5))
    L.append(cat_body(d, fur))
    L.append(robe(d, "#FFFFFF", "#D9DDF2", trim=gold(d, False), line="#5A5E88", neck=38))
    clip = d.clip(BODY)
    L.append(f'<g clip-path="{clip}">' + path("M-100,30 L-40,100 L-8,100 L-78,22 Z", d.lin([(0, "#9A6BFF"), (1, "#5A2EB8")]), "#33186E", 3)
             + path("M-90,34 L-28,100", "none", "#FFD24A", 3) + "</g>")
    L.append(path(BODY, "none", p["line"], 4.5))
    L.append(face(d, fur, iris="#3FA8FF", mood="fierce", brow="#FFFFFF", whisker=False))
    for s in (-1, 1):
        L.append(path(f"M{s * 14},-22 Q{s * 34},-34 {s * 52},-20", "none", "#8E94BE", 9, 'stroke-linecap="round"'))
        L.append(path(f"M{s * 14},-22 Q{s * 34},-34 {s * 52},-20", "none", "#FFFFFF", 5.5, 'stroke-linecap="round"'))
    L.append(beard(d, "#FFFFFF", "#C9D0EC", "#6B6F92", long=True, wide=True))
    L.append(laurel(d))
    L.append(gem(d, 0, -68, 7, "#9FD8FF"))
    # 閃電
    L.append(move(rot(bolt(1.1, 0, 0, "#FFFFFF", "#3F8CFF", "#7FD0FF", d), 10), 90, -30))
    L.append(paws(fur, hold=[(84, 26)]))
    return figure(d, L, aura="#7FB8FF")


def tudigong(d):
    """土地公：奶油色、笑瞇眼、白鬍子白眉、員外帽、金黃長袍，捧著金元寶，拄著拐杖。"""
    fur = "cream"
    p = FURS[fur]
    L = []
    L.append(move(rot(shaft(d, 0, -70, 96, 10, d.lin([(0, "#C98A55"), (1, "#7A4A22")], 0, 0, 1, 0), "#4A2810"), -6), -90, 0))
    L.append(path("M-96,-66 q-10,-22 10,-26 q16,-2 14,14", "none", "#4A2810", 9, 'stroke-linecap="round"') + path("M-96,-66 q-10,-22 10,-26 q16,-2 14,14", "none", "#B87A45", 5, 'stroke-linecap="round"'))
    L.append(cat_body(d, fur))
    L.append(robe(d, "#FFD65C", "#E89A1E", trim="#C8321E", line="#7A4A0C", neck=36, collar="#FFFFFF"))
    L.append(face(d, fur, mood="smile", whisker=False))
    for s in (-1, 1):
        L.append(path(f"M{s * 16},-16 Q{s * 36},-30 {s * 60},-10", "none", "#A8A6B8", 8, 'stroke-linecap="round"'))
        L.append(path(f"M{s * 16},-16 Q{s * 36},-30 {s * 60},-10", "none", "#FFFFFF", 5, 'stroke-linecap="round"'))
    L.append(beard(d, "#FFFFFF", "#DAD8E6", "#8A8698", long=True))
    # 員外帽
    hat = d.lin([(0, "#5A4A7A"), (1, "#2A2140")])
    L.append(path("M-52,-52 L-46,-92 Q0,-104 46,-92 L52,-52 Q0,-66 -52,-52 Z", hat, "#17122A", 3.5))
    L.append(path("M-52,-52 Q0,-66 52,-52", "none", gold(d, False), 8, 'stroke-linecap="round"'))
    for s in (-1, 1):
        L.append(path(f"M{s * 50},-62 C{s * 76},-70 {s * 96},-58 {s * 104},-44 C{s * 86},-50 {s * 68},-50 {s * 52},-52 Z", hat, "#17122A", 3))
    L.append(gem(d, 0, -74, 8, "#FF5A6E"))
    # 金元寶
    ingot = path("M50,58 C52,42 70,40 78,48 C86,40 104,42 106,58 C104,74 52,74 50,58 Z", gold(d), GOLD_LINE, 3)
    L.append(glow(d, 78, 52, 34, "#FFE27A", 0.8) + ingot + ellipse(78, 48, 11, 8, "#FFF6C2", GOLD_LINE, 2.5))
    L.append(paws(fur, hold=[(-86, 30), (62, 66)]))
    return figure(d, L, aura="#FFE08A")


def leigong(d):
    """雷公：深藍毛、豎起的閃電毛、背後一圈小鼓、小翅膀，一手槌一手鑿，電光四射。"""
    fur = "navy"
    p = FURS[fur]
    L = []
    # 背後的連鼓
    for k in range(7):
        a = math.radians(200 + k * 23.5)
        x, y = math.cos(a) * 104, -14 + math.sin(a) * 104
        L.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="15" fill="{d.rad([(0, "#FFF3C2"), (1, "#E0A23A")])}" stroke="#7A2A12" stroke-width="4"/>')
        L.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="6" fill="none" stroke="#C8321E" stroke-width="2.5"/>')
    L.append(path("M-100,-52 A104,104 0 0 1 100,-52", "none", "#7A2A12", 4, 'opacity="0.8"'))
    for s in (-1, 1):
        L.append(wing(d, s, s * 56, 16, 0.9, "#BFE0FF", "#5C8FE0", "#23426E"))
    L.append(cat_body(d, fur))
    L.append(robe(d, "#3F8CFF", "#1E3F9A", trim="#FFE45C", line="#14204A", neck=40, sash="#FFD24A"))
    L.append(face(d, fur, iris="#FFE45C", mood="fierce", brow="#BFE0FF"))
    # 閃電形的頭毛
    L.append(poly([(-18, -62), (-8, -96), (2, -76), (12, -110), (20, -74), (30, -90), (26, -60)], d.lin([(0, "#FFFFFF"), (1, "#7FD0FF")]), "#23426E", 3))
    # 槌與鑿
    hammer = shaft(d, 0, -40, 40, 9, "#C98A55", "#4A2810") + rrect(-22, -62, 44, 28, 6, steel(d), STEEL_LINE, 3.5)
    L.append(move(rot(hammer, 24), 96, 4))
    L.append(move(rot(poly([(-6, -40), (6, -40), (4, 30), (0, 44), (-4, 30)], steel(d), STEEL_LINE, 3), -30), -94, 12))
    L.append(move(bolt(0.5, 0, 0, "#FFFFFF", "#E0A21A", "#FFE45C", d), -108, -64))
    L.append(move(bolt(0.42, 0, 0, "#FFFFFF", "#E0A21A", "#FFE45C", d), 112, -70))
    L.append(paws(fur, hold=[(-88, 34), (86, 30)]))
    return figure(d, L, aura="#6FA8FF")


def nezha(d):
    """哪吒：橘色小貓、雙髻紅絲帶、混天綾、乾坤圈、火尖槍，腳踩風火輪。"""
    fur = "orange"
    p = FURS[fur]
    L = []
    # 混天綾
    silk = d.lin([(0, "#FF8A7A"), (1, "#E0263A")])
    L.append(path("M-70,20 C-120,-10 -122,-80 -84,-100 C-104,-70 -92,-30 -52,-4 Z", silk, "#8A1420", 3))
    L.append(path("M70,20 C122,0 128,-60 100,-92 C108,-56 92,-20 52,-2 Z", silk, "#8A1420", 3))
    # 風火輪
    for s in (-1, 1):
        x = s * 46
        L.append(flame(d, x - 14, 100, 30) + flame(d, x + 14, 100, 30))
        L.append(f'<circle cx="{x}" cy="92" r="20" fill="{gold(d)}" stroke="{GOLD_LINE}" stroke-width="3.5"/><circle cx="{x}" cy="92" r="9" fill="#FF6A3A" stroke="{GOLD_LINE}" stroke-width="2.5"/>')
        for k in range(6):
            a = math.radians(k * 60)
            L.append(f'<line x1="{x + math.cos(a) * 9:.1f}" y1="{92 + math.sin(a) * 9:.1f}" x2="{x + math.cos(a) * 19:.1f}" y2="{92 + math.sin(a) * 19:.1f}" stroke="{GOLD_LINE}" stroke-width="2.5"/>')
    L.append(cat_body(d, fur))
    L.append(robe(d, "#FF5A4A", "#C81E2E", trim="#FFD24A", line="#7A1018", neck=44))
    L.append(path("M-18,60 L0,78 L18,60 L0,48 Z", gold(d), GOLD_LINE, 2.5) + gem(d, 0, 62, 6, "#62D8FF"))
    L.append(face(d, fur, iris="#C8321E", mood="calm", brow="#8A4320"))
    L.append(f'<circle cx="0" cy="-22" r="5" fill="#E0263A" stroke="#7A1018" stroke-width="1.5"/>')
    # 雙髻
    for s in (-1, 1):
        L.append(f'<circle cx="{s * 40}" cy="-86" r="19" fill="{d.rad([(0, "#6A4A3A"), (1, "#2A1A14")], 0.4, 0.35, 0.7)}" stroke="#17100C" stroke-width="3.5"/>')
        L.append(path(f"M{s * 24},-74 Q{s * 40},-64 {s * 56},-74", "none", "#E0263A", 7, 'stroke-linecap="round"'))
        L.append(path(f"M{s * 56},-74 q{s * 14},8 {s * 10},26", "none", "#E0263A", 5, 'stroke-linecap="round"'))
    # 火尖槍
    spear = shaft(d, 0, -92, 100, 9, d.lin([(0, "#FF8A6A"), (1, "#B8201E")], 0, 0, 1, 0), "#5A0E0A")
    spear += flame(d, 0, -100, 34) + poly([(0, -132), (11, -100), (0, -90), (-11, -100)], steel(d), STEEL_LINE, 3)
    spear += path("M-12,-90 q-10,12 0,22 M12,-90 q10,12 0,22", "none", "#E0263A", 5, 'stroke-linecap="round"')
    L.append(move(rot(spear, 12), 92, 6))
    # 乾坤圈
    L.append(f'<circle cx="-92" cy="24" r="24" fill="none" stroke="{GOLD_LINE}" stroke-width="12"/><circle cx="-92" cy="24" r="24" fill="none" stroke="{gold(d)}" stroke-width="7"/>')
    L.append(path("M-108,10 A24,24 0 0 1 -84,2", "none", "#FFFFFF", 2.5, 'opacity="0.8" stroke-linecap="round"'))
    L.append(paws(fur, hold=[(-74, 36), (86, 34)]))
    return figure(d, L, aura="#FF8A4A", shadow=False)


def guanyu(d):
    """關公：紅臉、長黑鬚、綠巾綠袍金邊，手持青龍偃月刀。"""
    fur = "red"
    p = FURS[fur]
    L = []
    # 青龍偃月刀
    blade = d.lin([(0, "#FFFFFF"), (0.5, "#C9E6DC"), (1, "#5E9A8A")], 0, 0, 1, 0)
    guandao = shaft(d, 0, -60, 100, 11, d.lin([(0, "#D84A3A"), (1, "#7A1410")], 0, 0, 1, 0), "#4A0C0A", caps=gold(d))
    guandao += path("M-4,-60 C-10,-96 4,-132 34,-146 C46,-120 40,-84 14,-60 Z", blade, STEEL_LINE, 3.5)
    guandao += path("M8,-70 C6,-98 16,-122 30,-134", "none", "#FFFFFF", 3, 'opacity="0.85" stroke-linecap="round"')
    guandao += path("M-6,-64 C-22,-70 -26,-84 -18,-94 C-16,-82 -10,-76 -2,-76 Z", jade(d), "#1F6B4A", 2.5)
    guandao += path("M-10,-58 q-14,10 -6,26 M10,-58 q14,10 6,26", "none", "#E0263A", 5, 'stroke-linecap="round"')
    L.append(move(rot(guandao, 4), 88, 14))
    L.append(cat_body(d, fur))
    L.append(robe(d, "#3FB06A", "#1C6B44", trim=gold(d, False), line="#0E3A26", neck=36, collar="#FFFFFF"))
    L.append(face(d, fur, iris="#3A2418", mood="cool", brow="#2A1A14", whisker=False))
    for s in (-1, 1):
        L.append(path(f"M{s * 14},-20 Q{s * 34},-34 {s * 56},-22", "none", "#17100C", 7, 'stroke-linecap="round"'))
    L.append(beard(d, "#3A2A2A", "#17100C", "#0A0606", long=True))
    for s in (-1, 1):
        L.append(path(f"M{s * 10},24 Q{s * 34},30 {s * 44},54", "none", "#17100C", 6, 'stroke-linecap="round"'))
    # 綠巾
    cap = d.lin([(0, "#5FD08A"), (1, "#1C6B44")])
    L.append(path("M-60,-46 C-58,-90 58,-90 60,-46 C36,-60 -36,-60 -60,-46 Z", cap, "#0E3A26", 3.5))
    L.append(path("M-60,-46 Q0,-64 60,-46", "none", gold(d, False), 7, 'stroke-linecap="round"'))
    L.append(path("M-20,-84 C-10,-106 10,-106 20,-84 C8,-90 -8,-90 -20,-84 Z", cap, "#0E3A26", 3))
    L.append(gem(d, 0, -58, 8, "#FF5A6E"))
    L.append(path("M-58,-52 C-84,-46 -92,-24 -86,-6", "none", "#0E3A26", 9, 'stroke-linecap="round"') + path("M-58,-52 C-84,-46 -92,-24 -86,-6", "none", "#3FB06A", 5, 'stroke-linecap="round"'))
    L.append(paws(fur, fill="#FFE2D6", hold=[(88, 32)]))
    return figure(d, L, aura="#5FD08A")


def erlang(d):
    """二郎神：銀白毛、額頭第三隻眼、三山飛鳳冠、銀甲藍披風，手持三尖兩刃刀。"""
    fur = "silver"
    p = FURS[fur]
    L = []
    cape = d.lin([(0, "#5C8FFF"), (1, "#23328A")])
    L.append(path("M-70,-10 C-124,10 -128,84 -96,104 L96,104 C128,84 124,10 70,-10 Z", cape, "#141E5A", 3.5))
    L.append(path("M-96,104 C-104,70 -96,30 -70,-10", "none", "#9FC0FF", 3, 'opacity="0.6"'))
    blade = steel(d)
    weapon = shaft(d, 0, -70, 100, 10, d.lin([(0, "#8A93B8"), (1, "#3F4878")], 0, 0, 1, 0), "#1E2444", caps=gold(d))
    weapon += poly([(0, -140), (12, -98), (0, -72), (-12, -98)], blade, STEEL_LINE, 3)
    for s in (-1, 1):
        weapon += path(f"M{s * 8},-76 C{s * 30},-84 {s * 34},-110 {s * 22},-124 C{s * 22},-104 {s * 14},-94 {s * 4},-92 Z", blade, STEEL_LINE, 3)
    L.append(move(rot(weapon, -4), -90, 14))
    L.append(cat_body(d, fur))
    L.append(robe(d, "#E6ECFA", "#9AA6C8", trim="#5C8FFF", line="#3F4878", neck=38))
    clip = d.clip(BODY)
    scales = "".join(path(f"M{x},{y} q9,10 18,0", "none", "#5E6A90", 2.5, 'opacity="0.6"') for y in (62, 76, 90) for x in range(-80, 80, 18))
    L.append(f'<g clip-path="{clip}">{scales}</g>')
    L.append(gem(d, 0, 52, 9, "#5C8FFF"))
    L.append(face(d, fur, iris="#3F6BE0", mood="calm", brow="#555E80"))
    # 天眼
    L.append(path("M0,-40 C-7,-32 -7,-22 0,-14 C7,-22 7,-32 0,-40 Z", d.rad([(0, "#FFFFFF"), (0.5, "#FFE45C"), (1, "#E0721A")]), "#7A3A0C", 2.5))
    L.append(ellipse(0, -27, 2.4, 6, "#3A1A0A", "none") + glow(d, 0, -27, 22, "#FFE45C", 0.7))
    # 三山冠
    crown = gold(d)
    L.append(path("M-52,-52 L-46,-78 L-28,-64 L-16,-98 L0,-70 L16,-98 L28,-64 L46,-78 L52,-52 Q0,-66 -52,-52 Z", crown, GOLD_LINE, 3.5))
    L.append(gem(d, 0, -60, 6, "#FF5A6E") + gem(d, -30, -58, 4.5, "#62D8FF") + gem(d, 30, -58, 4.5, "#62D8FF"))
    for s in (-1, 1):
        L.append(path(f"M{s * 16},-98 C{s * 30},-126 {s * 60},-128 {s * 74},-112", "none", "#C8321E", 4, 'stroke-linecap="round"'))
        L.append(f'<circle cx="{s * 74}" cy="-112" r="6" fill="#FF5A4A" stroke="#7A1018" stroke-width="2"/>')
    L.append(paws(fur, hold=[(-88, 30)]))
    return figure(d, L, aura="#8FB8FF")


def longwang(d):
    """東海龍王：青綠毛、鹿角龍角、長龍鬚、冕旒冠、藍色波浪袍，捧著發光的龍珠，水花環繞。"""
    fur = "teal"
    p = FURS[fur]
    L = []
    water = d.lin([(0, "#BFF3FF"), (1, "#3F9AE0")])
    for s in (-1, 1):
        L.append(path(f"M{s * 60},96 C{s * 110},90 {s * 130},40 {s * 104},10 C{s * 112},44 {s * 96},64 {s * 70},66 Z", water, "#1E5A9A", 3, 'opacity="0.92"'))
        L.append(f'<circle cx="{s * 114}" cy="-2" r="6" fill="#DFF8FF" stroke="#1E5A9A" stroke-width="2"/><circle cx="{s * 124}" cy="24" r="4" fill="#DFF8FF" stroke="#1E5A9A" stroke-width="2"/>')
    L.append(cat_body(d, fur))
    L.append(robe(d, "#3F8CE0", "#1A3F8A", trim=gold(d, False), line="#0E2452", neck=38))
    clip = d.clip(BODY)
    waves = "".join(path(f"M{x},84 q8,-12 16,0 q8,12 16,0", "none", "#BFF3FF", 3, 'opacity="0.8" stroke-linecap="round"') for x in range(-96, 96, 32))
    L.append(f'<g clip-path="{clip}">{waves}</g>')
    L.append(face(d, fur, iris="#FFB02A", mood="calm", brow="#135A58", whisker=False))
    # 龍鬚
    for s in (-1, 1):
        L.append(path(f"M{s * 12},22 C{s * 50},18 {s * 78},40 {s * 96},20 C{s * 104},10 {s * 100},-2 {s * 92},0", "none", "#135A58", 5.5, 'stroke-linecap="round"'))
        L.append(path(f"M{s * 12},22 C{s * 50},18 {s * 78},40 {s * 96},20 C{s * 104},10 {s * 100},-2 {s * 92},0", "none", "#E2FFF8", 2.5, 'stroke-linecap="round"'))
    # 龍角
    horn = d.lin([(0, "#FFF6C2"), (1, "#E0A23A")])
    for s in (-1, 1):
        L.append(path(f"M{s * 30},-70 C{s * 30},-100 {s * 44},-118 {s * 62},-126 C{s * 56},-112 {s * 56},-104 {s * 66},-98 C{s * 54},-96 {s * 50},-88 {s * 54},-78 C{s * 46},-78 {s * 42},-72 {s * 42},-64 Z", horn, GOLD_LINE, 3))
    # 冕旒冠
    L.append(rrect(-50, -84, 100, 14, 5, d.lin([(0, "#4A3F8A"), (1, "#1E1A4A")]), "#0E0A2A", 3.5))
    L.append(rrect(-22, -72, 44, 16, 5, gold(d), GOLD_LINE, 3))
    for k in range(5):
        x = -40 + k * 20
        for j, c in enumerate(("#FF5A6E", "#FFE45C", "#62D8FF")):
            L.append(f'<circle cx="{x}" cy="{-62 + j * 9}" r="3.6" fill="{c}" stroke="#1E1A4A" stroke-width="1.4"/>')
    # 龍珠
    L.append(glow(d, 0, 66, 46, "#9FF3FF", 0.95) + gem(d, 0, 64, 17, "#5CE0FF", "#1E5A9A"))
    L.append(paws(fur, y=78, spread=26))
    return figure(d, L, aura="#5CD8FF")


def xiaotian(d):
    """哮天犬：黑色細犬、白胸、尖耳、紅項圈金鈴鐺。"""
    L = []
    line = "#14121E"
    fill = d.rad([(0, "#6A6688"), (0.5, "#3C3852"), (1, "#1E1B2E")], 0.38, 0.3, 0.8)
    L.append(path("M56,56 C96,50 104,14 88,-6", "none", line, 15, 'stroke-linecap="round"') + path("M56,56 C96,50 104,14 88,-6", "none", "#3C3852", 9, 'stroke-linecap="round"'))
    for s in (-1, 1):
        L.append(path(f"M{s * 30},-52 C{s * 34},-92 {s * 52},-116 {s * 66},-120 C{s * 72},-92 {s * 68},-62 {s * 58},-40 Z", fill, line, 4.5))
        L.append(path(f"M{s * 42},-58 C{s * 46},-84 {s * 54},-100 {s * 62},-106 C{s * 64},-86 {s * 60},-66 {s * 54},-50 Z", "#C77DA6", "none"))
    L.append(path(BODY, fill, line, 4.5))
    clip = d.clip(BODY)
    L.append(f'<g clip-path="{clip}">' + ellipse(0, 70, 44, 40, "#FFFFFF", "none", None, 'opacity="0.95"') + ellipse(0, 26, 34, 26, "#FFFFFF", "none", None, 'opacity="0.95"')
             + path("M-62,-36 C-48,-58 -20,-66 6,-66", "none", "#FFFFFF", 7, 'stroke-linecap="round" opacity="0.35"') + "</g>")
    for s in (-1, 1):
        L.append(eye(d, s * 32, -4, 11, "#FFB02A"))
        L.append(path(f"M{s * 18},-12 L{s * 46},-22", "none", line, 5, 'stroke-linecap="round"'))
    L.append(ellipse(0, 14, 9, 6.5, "#17142A", "none") + ellipse(-3, 12, 3, 1.8, "#FFFFFF", "none", None, 'opacity="0.7"'))
    L.append(path("M-12,26 Q-6,34 0,24 Q6,34 12,26", "none", line, 3.2, 'stroke-linecap="round"'))
    L.append(path("M-4,30 q4,12 8,0 Z", "#FF8FA8", line, 2))
    L.append(path("M-60,44 Q0,66 60,44", "none", "#7A1018", 12, 'stroke-linecap="round"') + path("M-60,44 Q0,66 60,44", "none", "#E0263A", 7, 'stroke-linecap="round"'))
    L.append(f'<circle cx="0" cy="62" r="9" fill="{gold(d)}" stroke="{GOLD_LINE}" stroke-width="2.5"/>')
    for s in (-1, 1):
        L.append(ellipse(s * 32, 76, 19, 12, "#FFFFFF", line, 3.5))
    return figure(d, L)


# ───────────── 希臘（續） ─────────────

def chiton(d, top="#FFFFFF", bottom="#DCE2F5", trim=None, line="#5A5E88", neck=40, shoulder=None):
    """希臘長衫：白色為主，可以加一條斜披的布。"""
    out = robe(d, top, bottom, trim=trim, line=line, neck=neck)
    if shoulder:
        clip = d.clip(BODY)
        g = d.lin([(0, shoulder[0]), (1, shoulder[1])])
        out += f'<g clip-path="{clip}">' + path("M100,26 L40,100 L8,100 L78,20 Z", g, shoulder[2], 3) + "</g>" + path(BODY, "none", line, 4.5)
    return out


def hermes(d):
    """荷米斯：奶油色、有翅膀的旅行帽、有翅膀的涼鞋、雙蛇杖，白短衫藍邊。"""
    fur = "cream"
    L = []
    for s in (-1, 1):
        L.append(wing(d, s, s * 40, 84, 0.62))
    L.append(cat_body(d, fur))
    L.append(chiton(d, trim="#3FA8FF", shoulder=("#7FD0FF", "#2E7AD6", "#1A3F8A")))
    L.append(face(d, fur, iris="#2E9A6A", mood="calm", brow="#8A6248"))
    # 帽子
    hat = d.lin([(0, "#FFE9A0"), (1, "#E0A23A")])
    L.append(ellipse(0, -52, 74, 15, hat, GOLD_LINE, 3.5))
    L.append(path("M-42,-56 C-40,-94 40,-94 42,-56 Z", hat, GOLD_LINE, 3.5))
    L.append(path("M-30,-72 C-18,-86 18,-86 30,-72", "none", "#FFFFFF", 3, 'opacity="0.65" stroke-linecap="round"'))
    L.append(path("M-41,-60 Q0,-70 41,-60", "none", "#3FA8FF", 6))
    for s in (-1, 1):
        L.append(rot(wing(d, s, s * 38, -66, 0.8), s * -24, s * 38, -66))
    # 雙蛇杖
    rod = shaft(d, 0, -84, 96, 8, gold(d, False), GOLD_LINE)
    rod += f'<circle cx="0" cy="-92" r="9" fill="{gold(d)}" stroke="{GOLD_LINE}" stroke-width="2.5"/>'
    for s in (-1, 1):
        rod += path(f"M0,-20 C{s * 22},-34 {s * 22},-48 0,-56 C{-s * 18},-64 {-s * 16},-74 {s * 4},-80", "none", "#1F6B4A", 6.5, 'stroke-linecap="round"')
        rod += path(f"M0,-20 C{s * 22},-34 {s * 22},-48 0,-56 C{-s * 18},-64 {-s * 16},-74 {s * 4},-80", "none", "#7FE0A8", 3.5, 'stroke-linecap="round"')
        rod += rot(wing(d, s, s * 4, -88, 0.5), s * -30, 0, -88)
    L.append(move(rot(rod, 9), 92, 8))
    L.append(paws(fur, hold=[(88, 34)]))
    return figure(d, L, aura="#9FE0FF")


def artemis(d):
    """阿提米絲：銀灰毛、新月頭飾、綠銀獵裝、背著箭筒，拉著一把銀弓。"""
    fur = "gray"
    L = []
    # 箭筒
    L.append(rot(rrect(40, -92, 26, 70, 8, d.lin([(0, "#B8793E"), (1, "#6B3E1A")]), "#3A1E0A", 3), 18, 52, -50))
    for k in range(3):
        L.append(rot(path(f"M{46 + k * 7},-92 l0,-22 M{43 + k * 7},-110 l3,-8 l3,8", "none", "#E6ECFA", 3, 'stroke-linecap="round"'), 18, 52, -50))
    L.append(cat_body(d, fur))
    L.append(robe(d, "#5FBF8A", "#2A7A56", trim="#DCE4F5", line="#123E2A", neck=40, sash="#C9D3E2"))
    L.append(face(d, fur, iris="#62D8B8", mood="calm", brow="#3F4560"))
    # 新月頭飾
    L.append(path("M-50,-52 Q0,-68 50,-52", "none", STEEL_LINE, 9, 'stroke-linecap="round"') + path("M-50,-52 Q0,-68 50,-52", "none", "#E6ECFA", 5, 'stroke-linecap="round"'))
    L.append(path("M-14,-64 A16,16 0 1 0 14,-64 A12,12 0 1 1 -14,-64 Z", d.lin([(0, "#FFFFFF"), (1, "#C9D8FF")]), "#555E80", 2.5))
    # 弓與箭
    bow = path("M0,-86 C46,-56 46,56 0,86", "none", STEEL_LINE, 11, 'stroke-linecap="round"') + path("M0,-86 C46,-56 46,56 0,86", "none", steel(d), 6.5, 'stroke-linecap="round"')
    bow += path("M0,-86 L0,86", "none", "#FFFFFF", 2, 'opacity="0.9"')
    bow += path("M-30,0 L58,0", "none", "#6B3E1A", 4.5, 'stroke-linecap="round"') + poly([(58, -8), (78, 0), (58, 8)], steel(d), STEEL_LINE, 2.5)
    bow += glow(d, 76, 0, 22, "#BFE0FF", 0.9)
    L.append(move(flip(bow), -74, 14, 0.7))
    L.append(paws(fur, hold=[(-84, 30)]))
    return figure(d, L, aura="#BFD8FF")


def hephaestus(d):
    """赫菲斯托斯：棕虎斑、頭巾、護目鏡、皮圍裙，舉著燒紅的鍛造鎚，旁邊是鐵砧與火花。"""
    fur = "brown"
    L = []
    # 鐵砧
    L.append(path("M-128,70 L-68,70 L-74,82 L-82,82 L-78,98 L-66,104 L-130,104 L-118,98 L-114,82 L-122,82 Z", steel(d), STEEL_LINE, 3))
    L.append(glow(d, -98, 64, 26, "#FFB02A", 0.9))
    for a, r in ((200, 30), (240, 38), (280, 34), (320, 28)):
        x, y = -98 + math.cos(math.radians(a)) * r, 66 + math.sin(math.radians(a)) * r
        L.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="3.2" fill="#FFF3A0" stroke="#E0721A" stroke-width="1.5"/>')
    L.append(cat_body(d, fur))
    L.append(robe(d, "#B8793E", "#6B3E1A", line="#3A1E0A", neck=34))
    L.append(path("M-30,40 L-44,10 M30,40 L44,10", "none", "#3A1E0A", 6, 'stroke-linecap="round"'))
    L.append(rrect(-16, 62, 32, 20, 5, "#8A5229", "#3A1E0A", 2.5))
    L.append(face(d, fur, iris="#E0721A", mood="calm", brow="#5A3018"))
    L.append(ellipse(-46, 12, 9, 5, "#3A2A2A", "none", None, 'opacity="0.45"'))
    # 頭巾與護目鏡
    L.append(path("M-64,-44 C-58,-80 58,-80 64,-44 C40,-56 -40,-56 -64,-44 Z", d.lin([(0, "#E8503A"), (1, "#9A2418")]), "#5A100A", 3.5))
    L.append(path("M-64,-46 Q0,-62 64,-46", "none", "#3A1E0A", 9, 'stroke-linecap="round"'))
    for s in (-1, 1):
        L.append(f'<circle cx="{s * 22}" cy="-56" r="15" fill="{d.rad([(0, "#FFFFFF"), (0.4, "#9FE8FF"), (1, "#2E7AD6")], 0.35, 0.3, 0.8)}" stroke="{GOLD_LINE}" stroke-width="5"/>')
        L.append(path(f"M{s * 14},-62 q{s * 6},-6 {s * 12},-2", "none", "#FFFFFF", 2.5, 'stroke-linecap="round" opacity="0.9"'))
    # 鍛造鎚
    hammer = shaft(d, 0, -30, 96, 10, d.lin([(0, "#C98A55"), (1, "#7A4A22")], 0, 0, 1, 0), "#3A1E0A")
    hammer += rrect(-30, -72, 60, 44, 8, steel(d), STEEL_LINE, 3.5)
    hammer += rrect(-30, -72, 16, 44, 6, d.lin([(0, "#FFF3A0"), (1, "#FF7A2A")]), "#B8421E", 3)
    hammer += glow(d, -24, -50, 30, "#FFB02A", 0.85)
    L.append(move(rot(hammer, 14), 92, 0))
    L.append(paws(fur, hold=[(88, 32)]))
    return figure(d, L, aura="#FFA05A")


def apollo(d):
    """阿波羅：金毛、金月桂冠、身後的太陽光環，抱著七弦琴，白衫金邊。"""
    fur = "gold"
    L = []
    L.append(halo(d, -20, 96, "#FFD24A", rays=16))
    L.append(cat_body(d, fur))
    L.append(chiton(d, trim=gold(d, False), shoulder=("#FFE27A", "#F2A02A", "#8A4B0C")))
    L.append(face(d, fur, iris="#FF9A2E", mood="smile"))
    L.append(laurel(d))
    L.append(gem(d, 0, -66, 6, "#FFB02A"))
    # 七弦琴
    lyre = path("M-24,30 C-36,-6 -34,-36 -18,-50 M24,30 C36,-6 34,-36 18,-50", "none", GOLD_LINE, 12, 'stroke-linecap="round"')
    lyre += path("M-24,30 C-36,-6 -34,-36 -18,-50 M24,30 C36,-6 34,-36 18,-50", "none", gold(d), 7, 'stroke-linecap="round"')
    lyre += rrect(-28, 22, 56, 18, 8, gold(d), GOLD_LINE, 3) + rrect(-26, -44, 52, 9, 4, gold(d), GOLD_LINE, 2.5)
    for k in range(5):
        x = -14 + k * 7
        lyre += f'<line x1="{x}" y1="-36" x2="{x}" y2="24" stroke="#FFFFFF" stroke-width="1.8" opacity="0.95"/>'
    L.append(glow(d, 84, 34, 44, "#FFE27A", 0.7))
    L.append(move(rot(lyre, 10), 84, 36))
    for x, y, r in ((114, -20, 5), (124, 2, 4), (104, -40, 4)):
        L.append(f'<path d="M{x},{y} l0,-16 l8,-3 l0,6 l-8,3" fill="none" stroke="#FFD24A" stroke-width="3" stroke-linejoin="round"/><ellipse cx="{x - 4}" cy="{y}" rx="{r}" ry="{r * 0.8:.1f}" fill="#FFD24A" stroke="{GOLD_LINE}" stroke-width="1.5"/>')
    L.append(paws(fur, hold=[(64, 50)]))
    return figure(d, L, aura="#FFD86A")


def heracles(d):
    """海克力斯：橘虎斑、披著獅子皮頭罩（鬃毛、獅耳），扛著一根大木棒。"""
    fur = "orange"
    L = []
    # 木棒
    club = path("M-10,96 L-16,-60 C-22,-96 22,-96 16,-60 L10,96 Z", d.lin([(0, "#D69A5A"), (0.5, "#A8693A"), (1, "#6B3E1A")], 0, 0, 1, 0), "#3A1E0A", 3.5)
    for y in (-66, -40, -12):
        club += path(f"M-12,{y} q12,8 24,0", "none", "#3A1E0A", 2.5, 'opacity="0.5"')
    for x, y in ((-18, -70), (18, -52), (-16, -34)):
        club += f'<circle cx="{x}" cy="{y}" r="5" fill="#8A5229" stroke="#3A1E0A" stroke-width="2.5"/>'
    L.append(move(rot(club, 16), 94, 4))
    # 獅子皮的鬃毛
    mane = d.rad([(0, "#E89A3A"), (1, "#9A5A1E")])
    pts = []
    for k in range(22):
        a = math.radians(160 + k * (220 / 21))
        r = 100 if k % 2 == 0 else 84
        pts.append((math.cos(a) * r, -14 + math.sin(a) * r * 0.92))
    L.append(poly(pts + [(84, 30), (-84, 30)], mane, "#5A2C10", 3.5))
    L.append(cat_body(d, fur))
    L.append(robe(d, "#E8B06A", "#B8793E", line="#5A2C10", neck=44, sash="#8A1E1E"))
    L.append(face(d, fur, iris="#8A5A2A", mood="fierce", brow="#8A4320"))
    # 獅頭罩
    hood = d.lin([(0, "#FFD08A"), (1, "#E0923A")])
    L.append(path("M-70,-34 C-74,-94 74,-94 70,-34 C52,-52 30,-46 22,-38 C10,-50 -10,-50 -22,-38 C-30,-46 -52,-52 -70,-34 Z", hood, "#5A2C10", 3.5))
    for s in (-1, 1):
        L.append(f'<circle cx="{s * 50}" cy="-84" r="14" fill="{hood}" stroke="#5A2C10" stroke-width="3.5"/><circle cx="{s * 50}" cy="-84" r="6.5" fill="#B8642A"/>')
        L.append(path(f"M{s * 24},-66 l{s * 14},6", "none", "#5A2C10", 4, 'stroke-linecap="round"'))
        L.append(poly([(s * 16, -40), (s * 22, -28), (s * 28, -42)], "#FFFFFF", "#5A2C10", 2))
    L.append(ellipse(0, -58, 9, 6, "#5A2C10", "none"))
    L.append(paws(fur, hold=[(88, 34)]))
    return figure(d, L, aura="#FFB45C")


def athena(d):
    """雅典娜：白毛灰藍眼、往上推的科林斯頭盔（紅色盔纓）、圓盾、長槍，肩上一隻小貓頭鷹。"""
    fur = "white"
    L = []
    spear = shaft(d, 0, -96, 100, 8, d.lin([(0, "#C98A55"), (1, "#7A4A22")], 0, 0, 1, 0), "#3A1E0A")
    spear += poly([(0, -138), (11, -104), (0, -92), (-11, -104)], steel(d), STEEL_LINE, 3)
    L.append(move(rot(spear, 6), 92, 12))
    L.append(cat_body(d, fur))
    L.append(chiton(d, trim=gold(d, False), neck=40))
    L.append(gem(d, 0, 58, 8, "#62D8FF"))
    L.append(face(d, fur, iris="#7F96C8", mood="cool", brow="#6B6F92"))
    # 頭盔
    helm = d.lin([(0, "#FFF0B8"), (0.5, "#F2B83A"), (1, "#B8751A")])
    L.append(path("M-12,-84 C-20,-128 20,-140 44,-118 C24,-116 14,-100 12,-84 Z", d.lin([(0, "#FF7A6A"), (1, "#C81E2E")]), "#6B1018", 3.5))
    for k in range(4):
        L.append(path(f"M{-4 + k * 9},-90 q{4 + k * 2},-18 {10 + k * 5},-28", "none", "#6B1018", 2, 'opacity="0.5"'))
    L.append(path("M-62,-42 C-60,-90 60,-90 62,-42 C40,-56 16,-54 8,-44 L0,-34 L-8,-44 C-16,-54 -40,-56 -62,-42 Z", helm, GOLD_LINE, 3.5))
    L.append(path("M-46,-62 C-28,-80 28,-80 46,-62", "none", "#FFFFFF", 3, 'opacity="0.65" stroke-linecap="round"'))
    # 圓盾
    L.append(f'<circle cx="-84" cy="40" r="42" fill="{gold(d)}" stroke="{GOLD_LINE}" stroke-width="4"/>')
    L.append(f'<circle cx="-84" cy="40" r="31" fill="{d.rad([(0, "#9FD8FF"), (1, "#2E5AA8")], 0.4, 0.35, 0.8)}" stroke="{GOLD_LINE}" stroke-width="3"/>')
    owl = ellipse(-84, 42, 15, 17, "#FFFFFF", "#1E3A6E", 2.5) + f'<circle cx="-90" cy="36" r="5.5" fill="#FFD24A" stroke="#1E3A6E" stroke-width="2"/><circle cx="-78" cy="36" r="5.5" fill="#FFD24A" stroke="#1E3A6E" stroke-width="2"/>'
    owl += f'<circle cx="-90" cy="36" r="2" fill="#1E1226"/><circle cx="-78" cy="36" r="2" fill="#1E1226"/>' + poly([(-87, 42), (-81, 42), (-84, 48)], "#FF9A2E", "#1E3A6E", 1.5)
    owl += poly([(-96, 28), (-92, 20), (-88, 28)], "#FFFFFF", "#1E3A6E", 2) + poly([(-80, 28), (-76, 20), (-72, 28)], "#FFFFFF", "#1E3A6E", 2)
    L.append(owl)
    L.append(path("M-114,18 A42,42 0 0 1 -92,0", "none", "#FFFFFF", 3, 'opacity="0.8" stroke-linecap="round"'))
    L.append(paws(fur, hold=[(88, 36)]))
    return figure(d, L, aura="#BFD8FF")


def hades(d):
    """黑帝斯：黑毛、紫色發光的眼睛、冒著藍色冥火的黑冠、暗色長袍，手持雙叉戟，身旁飄著魂火。"""
    fur = "black"
    L = []
    cape = d.lin([(0, "#3A2A6E"), (1, "#120A2A")])
    L.append(path("M-72,-6 C-128,14 -130,86 -100,104 L100,104 C130,86 128,14 72,-6 Z", cape, "#0A0618", 3.5))
    for x, y, s in ((-112, -30, 30), (116, -50, 26), (104, 26, 20)):
        L.append(glow(d, x, y - s * 0.4, s * 1.3, "#62D8FF", 0.7) + flame(d, x, y, s, "#FFFFFF", "#7FE0FF", "#2E7AD6"))
    bident = shaft(d, 0, -60, 100, 9, d.lin([(0, "#6E688C"), (1, "#2C2840")], 0, 0, 1, 0), "#0A0618")
    for s in (-1, 1):
        bident += path(f"M0,-56 C{s * 20},-60 {s * 22},-92 {s * 16},-128 C{s * 8},-108 {s * 8},-84 0,-76 Z", steel(d), STEEL_LINE, 3)
    bident += gem(d, 0, -58, 7, "#B06BFF")
    L.append(move(rot(bident, -6), -90, 12))
    L.append(cat_body(d, fur))
    L.append(robe(d, "#4A3A8A", "#1E1444", trim="#B06BFF", line="#0A0618", neck=38))
    L.append(gem(d, 0, 56, 9, "#62D8FF"))
    L.append(glow(d, -31, 2, 26, "#B06BFF", 0.8) + glow(d, 31, 2, 26, "#B06BFF", 0.8))
    L.append(face(d, fur, iris="#C88BFF", mood="cool", brow="#17142A"))
    # 冥王冠
    crown = d.lin([(0, "#6E688C"), (1, "#17142A")])
    L.append(path("M-54,-50 L-58,-86 L-36,-66 L-20,-100 L0,-68 L20,-100 L36,-66 L58,-86 L54,-50 Q0,-64 -54,-50 Z", crown, "#0A0618", 3.5))
    L.append(path("M-54,-50 Q0,-64 54,-50", "none", "#B06BFF", 5, 'stroke-linecap="round"'))
    for x, y in ((-20, -98), (20, -98), (0, -70)):
        L.append(flame(d, x, y, 22, "#FFFFFF", "#7FE0FF", "#2E7AD6"))
    L.append(paws(fur, hold=[(-86, 32)]))
    return figure(d, L, aura="#8A6BFF")


def poseidon(d):
    """波賽頓：藍毛、海浪般的青色鬍子、珊瑚金冠、三叉戟、海綠色長袍，腳邊浪花翻湧。"""
    fur = "blue"
    L = []
    water = d.lin([(0, "#DFF8FF"), (1, "#3F9AE0")])
    L.append(path("M-130,104 C-128,70 -100,56 -84,72 C-88,52 -64,44 -52,60 C-40,76 40,76 52,60 C64,44 88,52 84,72 C100,56 128,70 130,104 Z", water, "#1E5A9A", 3.5))
    for x, y in ((-112, 60), (-60, 44), (62, 46), (114, 58), (0, 80)):
        L.append(f'<circle cx="{x}" cy="{y}" r="5" fill="#FFFFFF" stroke="#1E5A9A" stroke-width="2"/>')
    trident = shaft(d, 0, -64, 100, 9, gold(d, False), GOLD_LINE)
    trident += poly([(0, -140), (10, -104), (0, -64), (-10, -104)], gold(d), GOLD_LINE, 3)
    for s in (-1, 1):
        trident += path(f"M{s * 4},-66 C{s * 30},-70 {s * 34},-96 {s * 28},-128 C{s * 20},-112 {s * 18},-92 {s * 4},-84 Z", gold(d), GOLD_LINE, 3)
    trident += glow(d, 0, -110, 40, "#9FF3FF", 0.75)
    L.append(move(rot(trident, 5), 90, 14))
    L.append(cat_body(d, fur))
    L.append(robe(d, "#5FD8C0", "#1E8A8A", trim=gold(d, False), line="#0E4A4E", neck=40))
    L.append(face(d, fur, iris="#1E8A8A", mood="fierce", brow="#DFF8FF", whisker=False))
    L.append(beard(d, "#BFF3FF", "#3FB8D8", "#1E5A9A", long=True, wide=True))
    L.append(path("M-30,44 q10,-10 20,0 q10,10 20,0 q10,-10 20,0", "none", "#FFFFFF", 3, 'opacity="0.8" stroke-linecap="round"'))
    # 珊瑚金冠
    L.append(path("M-52,-50 L-50,-74 L-34,-62 L-24,-90 L-10,-66 L0,-96 L10,-66 L24,-90 L34,-62 L50,-74 L52,-50 Q0,-64 -52,-50 Z", gold(d), GOLD_LINE, 3.5))
    L.append(gem(d, 0, -60, 7, "#5CE0FF") + gem(d, -28, -58, 4.5, "#FF8A9A") + gem(d, 28, -58, 4.5, "#FF8A9A"))
    L.append(paws(fur, hold=[(88, 34)]))
    return figure(d, L, aura="#5CC8FF", shadow=False)


EAST = {"tudigong": tudigong, "menshen": menshen, "leigong": leigong, "nezha": nezha, "guanyu": guanyu, "erlang": erlang,
        "wukong": wukong, "longwang": longwang, "xiaotian": xiaotian}
GREEK = {"hermes": hermes, "artemis": artemis, "hephaestus": hephaestus, "apollo": apollo, "heracles": heracles,
         "athena": athena, "hades": hades, "poseidon": poseidon, "zeus": zeus}
ALL = {**EAST, **GREEK}
