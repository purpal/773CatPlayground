"""世界巡迴畫面的美術：世界地圖（等距圓柱投影）、13 國＋台灣的國旗、載著貓咪的小飛機。

地圖範圍與遊戲裡的換算一致（見 TourScreen.kt 的 MAP_LAT_TOP／MAP_LAT_BOTTOM）：
經度 -180～180 對應寬度，緯度 84～-86 對應高度。陸地輪廓來自 data/land-110m.json（Natural Earth，見 data/README.md）。
"""
import json
import math
import os

from common import *

HERE = os.path.dirname(os.path.abspath(__file__))
LAT_TOP = 84.0
LAT_BOTTOM = -86.0
MAP_W = 1600
MAP_H = round(MAP_W * (LAT_TOP - LAT_BOTTOM) / 360.0)

OCEAN = "#BDE8FF"
OCEAN_D = "#9AD3F2"
LAND = "#C8EBB4"
LAND_D = "#9FD28A"
SHORE = "#6B4C52"


def load_rings():
    """讀 TopoJSON，回傳每個多邊形的外環與內環（經緯度座標的點列）。"""
    with open(os.path.join(HERE, "data", "land-110m.json")) as fh:
        topo = json.load(fh)
    (sx, sy), (tx, ty) = topo["transform"]["scale"], topo["transform"]["translate"]
    arcs = []
    for arc in topo["arcs"]:
        x = y = 0
        pts = []
        for dx, dy in arc:
            x += dx
            y += dy
            pts.append((x * sx + tx, y * sy + ty))
        arcs.append(pts)

    def ring(indices):
        out = []
        for i in indices:
            pts = arcs[i] if i >= 0 else arcs[~i][::-1]
            out.extend(pts if not out else pts[1:])
        return out

    polygons = []
    for geo in topo["objects"]["land"]["geometries"]:
        if geo["type"] == "Polygon":
            polygons.append([ring(r) for r in geo["arcs"]])
        elif geo["type"] == "MultiPolygon":
            for poly in geo["arcs"]:
                polygons.append([ring(r) for r in poly])
    return polygons


def unwrap(ring):
    """把跨過換日線的環（經度跳 360°）接成連續的經度。"""
    out = []
    shift = 0.0
    prev = None
    for lon, lat in ring:
        if prev is not None:
            if lon + shift - prev > 180:
                shift -= 360
            elif lon + shift - prev < -180:
                shift += 360
        out.append((lon + shift, lat))
        prev = lon + shift
    return out


def map_path(polygons, w, h):
    """等距圓柱投影的陸地路徑；跨過換日線的部分在另一邊再畫一次（超出畫面的地方會被裁掉）。"""
    d = ""
    for poly in polygons:
        for r in poly:
            r = unwrap(r)
            lons = [lon for lon, _ in r]
            offsets = [0.0]
            if min(lons) < -180:
                offsets.append(360.0)
            if max(lons) > 180:
                offsets.append(-360.0)
            for off in offsets:
                for i, (lon, lat) in enumerate(r):
                    x = (lon + off + 180.0) / 360.0 * w
                    y = (LAT_TOP - lat) / (LAT_TOP - LAT_BOTTOM) * h
                    d += ("M" if i == 0 else "L") + f"{x:.1f},{y:.1f}"
                d += "Z"
    return d


def world_map():
    polygons = load_rings()
    d = map_path(polygons, MAP_W, MAP_H)
    defs = (f'<linearGradient id="sea" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{OCEAN}"/>'
            f'<stop offset="1" stop-color="{OCEAN_D}"/></linearGradient>'
            f'<filter id="landShadow"><feGaussianBlur stdDeviation="3"/></filter>')
    b = f'<rect x="0" y="0" width="{MAP_W}" height="{MAP_H}" fill="url(#sea)"/>'
    # 經緯線
    for k in range(1, 12):
        x = MAP_W * k / 12
        b += f'<path d="M{x:.0f},0 L{x:.0f},{MAP_H}" stroke="#FFFFFF" stroke-opacity="0.35" stroke-width="2"/>'
    for lat in (60, 30, 0, -30, -60):
        y = (LAT_TOP - lat) / (LAT_TOP - LAT_BOTTOM) * MAP_H
        b += f'<path d="M0,{y:.0f} L{MAP_W},{y:.0f}" stroke="#FFFFFF" stroke-opacity="{0.6 if lat == 0 else 0.35}" stroke-width="2"/>'
    b += f'<path d="{d}" fill="{LAND_D}" transform="translate(4 6)" filter="url(#landShadow)" opacity="0.7"/>'
    b += f'<path d="{d}" fill="{LAND}" stroke="{SHORE}" stroke-width="2.5" stroke-linejoin="round"/>'
    return Sprite("tour_map", MAP_W, MAP_H, f"0 0 {MAP_W} {MAP_H}", b, defs)


# ───────────── 國旗 ─────────────

def flag(name, ratio, body, defs=""):
    """ratio 為寬/高；統一高度 80px，圓角＋暖棕細框。座標系高度 60。"""
    h = 60
    w = h * ratio
    clip = f'<clipPath id="fc_{name}"><rect x="0" y="0" width="{w:.1f}" height="{h}" rx="5"/></clipPath>'
    out = f'<g clip-path="url(#fc_{name})">{body}</g>'
    out += f'<rect x="0" y="0" width="{w:.1f}" height="{h}" rx="5" fill="none" stroke="{SHORE}" stroke-width="2.5"/>'
    return Sprite(f"flag_{name}", round(80 * ratio), 80, f"-2 -2 {w + 4:.1f} {h + 4}", out, clip + defs)


def star_pts(cx, cy, r, n=5, inner=0.382, rot=-90):
    pts = []
    for i in range(n * 2):
        rr = r if i % 2 == 0 else r * inner
        a = math.radians(rot + i * 180 / n)
        pts.append(f"{cx + math.cos(a) * rr:.2f},{cy + math.sin(a) * rr:.2f}")
    return " ".join(pts)


def poly(points, fill):
    return f'<polygon points="{points}" fill="{fill}"/>'


def rect(x, y, w, h, fill):
    return f'<rect x="{x:.2f}" y="{y:.2f}" width="{w:.2f}" height="{h:.2f}" fill="{fill}"/>'


def union_jack(w, h):
    """米字旗，縮放到 w×h（原始比例 2:1）。"""
    s = f'<g transform="scale({w / 60:.4f} {h / 30:.4f})">'
    s += '<clipPath id="ujt"><path d="M30,15 h30 v15 z v15 h-30 z h-30 v-15 z v-15 h30 z"/></clipPath>'
    s += '<rect width="60" height="30" fill="#012169"/>'
    s += '<path d="M0,0 L60,30 M60,0 L0,30" stroke="#FFFFFF" stroke-width="6"/>'
    s += '<path d="M0,0 L60,30 M60,0 L0,30" clip-path="url(#ujt)" stroke="#C8102E" stroke-width="4"/>'
    s += '<path d="M30,0 v30 M0,15 h60" stroke="#FFFFFF" stroke-width="10"/>'
    s += '<path d="M30,0 v30 M0,15 h60" stroke="#C8102E" stroke-width="6"/>'
    return s + "</g>"


def flag_tw():
    w, h = 90, 60
    b = rect(0, 0, w, h, "#FE0000") + rect(0, 0, w / 2, h / 2, "#000095")
    cx, cy, R = w / 4, h / 4, 11.2
    rays = ""
    for k in range(12):
        a = math.radians(k * 30 - 90)
        a1, a2 = math.radians(k * 30 - 90 - 15), math.radians(k * 30 - 90 + 15)
        rays += f"M{cx + math.cos(a) * R:.2f},{cy + math.sin(a) * R:.2f} L{cx + math.cos(a1) * R * 0.53:.2f},{cy + math.sin(a1) * R * 0.53:.2f} L{cx + math.cos(a2) * R * 0.53:.2f},{cy + math.sin(a2) * R * 0.53:.2f} Z "
    b += f'<path d="{rays}" fill="#FFFFFF"/>'
    b += f'<circle cx="{cx}" cy="{cy}" r="{R * 0.5:.2f}" fill="#000095"/><circle cx="{cx}" cy="{cy}" r="{R * 0.43:.2f}" fill="#FFFFFF"/>'
    return flag("tw", 1.5, b)


def flag_jp():
    return flag("jp", 1.5, rect(0, 0, 90, 60, "#FFFFFF") + '<circle cx="45" cy="30" r="18" fill="#BC002D"/>')


def flag_ca():
    w, h = 120, 60
    b = rect(0, 0, w, h, "#FFFFFF") + rect(0, 0, w / 4, h, "#D80621") + rect(w * 3 / 4, 0, w / 4, h, "#D80621")
    leaf = [(50, 4), (58, 20), (66, 16), (63, 40), (78, 26), (82, 34), (96, 32), (90, 48), (98, 52), (74, 70), (77, 80),
            (54, 76), (55, 96), (45, 96), (46, 76), (23, 80), (26, 70), (2, 52), (10, 48), (4, 32), (18, 34), (22, 26),
            (37, 40), (34, 16), (42, 20)]
    pts = " ".join(f"{30 + x * 0.6:.1f},{1 + y * 0.58:.1f}" for x, y in leaf)
    return flag("ca", 2.0, b + poly(pts, "#D80621"))


def flag_gb():
    return flag("gb", 2.0, union_jack(120, 60))


def flag_aq():
    """南極洲沒有官方國旗，用常見的非官方設計：藍底＋白色的南極大陸輪廓（從地圖資料用極地投影畫出來）。"""
    w, h = 90, 60
    d = ""
    for poly_rings in load_rings():
        for r in poly_rings:
            if max(lat for _, lat in r) > -58:
                continue
            for i, (lon, lat) in enumerate(r):
                rad = (90 + lat) / 30.0 * 27
                a = math.radians(lon)
                x = w / 2 + math.sin(a) * rad
                y = h / 2 + math.cos(a) * rad
                d += ("M" if i == 0 else "L") + f"{x:.2f},{y:.2f}"
            d += "Z"
    return flag("aq", 1.5, rect(0, 0, w, h, "#3A7DCE") + f'<path d="{d}" fill="#FFFFFF" fill-rule="evenodd"/>')


def flag_au():
    w, h = 120, 60
    b = rect(0, 0, w, h, "#012169") + union_jack(w / 2, h / 2)
    b += poly(star_pts(w / 4, h * 0.75, 9, n=7, inner=0.44), "#FFFFFF")
    for fx, fy, r in ((0.75, 0.15, 4.2), (0.62, 0.42, 4.2), (0.86, 0.36, 4.2), (0.75, 0.82, 4.6)):
        b += poly(star_pts(w * fx, h * fy, r, n=7, inner=0.44), "#FFFFFF")
    b += poly(star_pts(w * 0.8, h * 0.53, 2.4, n=5), "#FFFFFF")
    return flag("au", 2.0, b)


def flag_cn():
    w, h = 90, 60
    b = rect(0, 0, w, h, "#EE1C25") + poly(star_pts(15, 15, 9), "#FFFF00")
    for sx, sy in ((30, 6), (36, 12), (36, 21), (30, 27)):
        rot = math.degrees(math.atan2(15 - sy, 15 - sx))
        b += poly(star_pts(sx, sy, 3, rot=rot), "#FFFF00")
    return flag("cn", 1.5, b)


def flag_us():
    w, h = 114, 60
    b = ""
    for k in range(13):
        b += rect(0, k * h / 13, w, h / 13 + 0.2, "#B22234" if k % 2 == 0 else "#FFFFFF")
    cw, ch = w * 0.4, h * 7 / 13
    b += rect(0, 0, cw, ch, "#3C3B6E")
    for row in range(9):
        cols = 6 if row % 2 == 0 else 5
        for c in range(cols):
            x = cw / 12 * (2 * c + (1 if row % 2 == 0 else 2))
            y = ch / 10 * (row + 1)
            b += poly(star_pts(x, y, 1.3), "#FFFFFF")
    return flag("us", 1.9, b)


def flag_kh():
    w, h = 90, 60
    b = rect(0, 0, w, h, "#032EA1") + rect(0, h / 4, w, h / 2, "#E00025")
    # 吳哥窟：三座塔（中間最高）＋基座
    temple = "M24,42 L66,42 L66,39 L62,39 L62,33 L58,33 L58,27 L55,27 L54,20 L52,27 L50,27 L49,16 L45,9 L41,16 L40,27 L38,27 L36,20 L35,27 L32,27 L32,33 L28,33 L28,39 L24,39 Z"
    b += f'<path d="{temple}" fill="#FFFFFF" stroke="#2E2433" stroke-width="0.8"/>'
    for x in (34, 40, 45, 50, 56):
        b += f'<path d="M{x},35 L{x},41" stroke="#2E2433" stroke-width="0.7"/>'
    return flag("kh", 1.5, b)


def flag_fr():
    return flag("fr", 1.5, rect(0, 0, 30, 60, "#002395") + rect(30, 0, 30, 60, "#FFFFFF") + rect(60, 0, 30, 60, "#ED2939"))


def flag_sg():
    w, h = 90, 60
    b = rect(0, 0, w, h, "#FFFFFF") + rect(0, 0, w, h / 2, "#EF3340")
    b += '<circle cx="17" cy="15" r="10" fill="#FFFFFF"/><circle cx="21" cy="15" r="10" fill="#EF3340"/>'
    for k in range(5):
        a = math.radians(-90 + k * 72)
        b += poly(star_pts(27 + math.cos(a) * 5.2, 15 + math.sin(a) * 5.2, 2), "#FFFFFF")
    return flag("sg", 1.5, b)


def flag_cr():
    w, h = 100, 60
    b = rect(0, 0, w, h, "#002B7F") + rect(0, h / 6, w, h * 4 / 6, "#FFFFFF") + rect(0, h * 2 / 6, w, h * 2 / 6, "#CE1126")
    return flag("cr", 5 / 3, b)


def flag_br():
    w, h = 86, 60
    b = rect(0, 0, w, h, "#009C3B")
    b += poly(f"{w / 2},6 {w - 7},{h / 2} {w / 2},{h - 6} 7,{h / 2}", "#FFDF00")
    b += f'<circle cx="{w / 2}" cy="{h / 2}" r="14" fill="#002776"/>'
    b += f'<path d="M{w / 2 - 14},{h / 2 - 1} Q{w / 2},{h / 2 - 9} {w / 2 + 14},{h / 2 + 4}" stroke="#FFFFFF" stroke-width="3" fill="none"/>'
    for x, y in ((-6, 5), (2, 8), (7, 3), (-3, 10), (5, 11), (0, 4)):
        b += f'<circle cx="{w / 2 + x}" cy="{h / 2 + y}" r="0.9" fill="#FFFFFF"/>'
    return flag("br", w / h, b)


def flag_ch():
    b = rect(0, 0, 60, 60, "#DA291C") + rect(24, 12, 12, 36, "#FFFFFF") + rect(12, 24, 36, 12, "#FFFFFF")
    return flag("ch", 1.0, b)


def plane():
    """載著貓咪的小飛機：粉紅色機身、圓圓的座艙裡探出貓咪的頭，尾翼上有肉球。朝 +x。"""
    b = ""
    b += path("M-40,-6 L-60,-34 L-44,-34 L-20,-8 Z", "#FF8FB1", INK, 3.5)
    b += path("M-4,4 L-30,40 L-10,40 L22,6 Z", "#FF8FB1", INK, 3.5)
    b += path("M-52,-10 Q-40,-22 30,-18 Q62,-14 66,0 Q62,14 30,18 Q-40,22 -52,10 Z", "#FFF3F7", INK, 4)
    b += path("M-52,4 Q0,14 64,4", "none", "#FFCADA", 5)
    b += paw(-46, -26, 7, "#FFFFFF", INK, 2)
    b += ellipse(24, -14, 18, 14, "#BDE8FF", INK, 3)
    # 座艙裡的貓咪
    b += path("M12,-22 L14,-34 L22,-26 Z M28,-26 L36,-34 L36,-22 Z", "#FFB74D", INK, 2.5)
    b += ellipse(24, -16, 14, 11, "#FFB74D", INK, 2.5)
    b += dot_eye(19, -17, 2.4) + dot_eye(29, -17, 2.4)
    b += path("M22,-12 Q24,-10 26,-12", "none", None, 1.8)
    b += ellipse(66, 0, 5, 12, "#9AA3AE", INK, 2.5)
    for k in range(3):
        b += ellipse(-10 + k * 14, 4, 4, 4, "#BDE8FF", INK, 2)
    return Sprite("tour_plane", 200, 130, "-70 -44 150 98", g(b))


def all_sprites():
    return [world_map(), plane(), flag_tw(), flag_jp(), flag_ca(), flag_gb(), flag_aq(), flag_au(), flag_cn(), flag_us(),
            flag_kh(), flag_fr(), flag_sg(), flag_cr(), flag_br(), flag_ch()]
