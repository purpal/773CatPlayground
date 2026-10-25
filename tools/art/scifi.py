"""v3 攻擊特效：「外表萌萌，出手是硬派科幻」（見 docs/art/effects-board-scifi.png）。

玩家火力一律冷色（青／藍／白），每種武器保留一個和貓咪有關的萌點；
Boss 的攻擊道具改成科技武器，用暖色能量（洋紅／紅／琥珀／珊瑚），和玩家火力分得開。
"""
import math
from common import Sprite, INK, ellipse, path, paw, g, wob

CYAN = "#00E5FF"
BLUE = "#2979FF"
VIOLET = "#B388FF"
WHITE = "#FFFFFF"
NAVY = "#0A1630"
AMBER = "#FFB300"
MAGENTA = "#FF2BD6"
RED = "#FF3D5A"
CORAL = "#FF7A45"
STEEL = "#3A4256"
STEEL_HI = "#8A96B0"
MONO = "JetBrains Mono, DejaVu Sans Mono, monospace"
CJK = "WenQuanYi Zen Hei"

GLOW = """
<filter id="g3" x="-60%" y="-60%" width="220%" height="220%"><feGaussianBlur stdDeviation="3"/></filter>
<filter id="g6" x="-80%" y="-80%" width="260%" height="260%"><feGaussianBlur stdDeviation="6"/></filter>
<radialGradient id="singular"><stop offset="0" stop-color="#000"/><stop offset="0.55" stop-color="#05000D"/><stop offset="0.72" stop-color="#B388FF"/><stop offset="0.84" stop-color="#fff"/><stop offset="1" stop-color="#B388FF" stop-opacity="0"/></radialGradient>
<radialGradient id="cyanFlash"><stop offset="0" stop-color="#fff"/><stop offset="0.4" stop-color="#00E5FF" stop-opacity="0.6"/><stop offset="1" stop-color="#00E5FF" stop-opacity="0"/></radialGradient>
<linearGradient id="steel" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#9AA6C0"/><stop offset="0.5" stop-color="#4A5370"/><stop offset="1" stop-color="#2A3042"/></linearGradient>
"""


def lit(content, std=3, color_override=None):
    """發光：先墊一層模糊的同一張圖，再疊原圖。"""
    return f'<g filter="url(#g{std})" opacity="0.9">{content}</g>{content}'


def hexagon(x, y, r, rot=30):
    return " ".join(f"{x + math.cos(math.radians(60 * i + rot)) * r:.1f},{y + math.sin(math.radians(60 * i + rot)) * r:.1f}" for i in range(6))


# ───────────── 玩家武器 ─────────────

def plasma_bolt():
    """散射：電漿膠囊彈（朝上）。深藍描邊讓它在明亮背景上也清楚。"""
    body = (f'<rect x="-8" y="-26" width="16" height="52" rx="8" fill="{NAVY}"/>'
            f'<rect x="-6" y="-24" width="12" height="48" rx="6" fill="{CYAN}"/>'
            f'<rect x="-3" y="-20" width="6" height="40" rx="3" fill="{WHITE}"/>')
    return Sprite("shot_plasma", 56, 120, "-14 -30 28 60", lit(body, 3), GLOW)


def casing():
    """萌點：退出的彈殼上印著小肉球。"""
    b = f'<rect x="-11" y="-6" width="22" height="12" rx="2.5" fill="{AMBER}" stroke="#7A4A08" stroke-width="2"/>'
    b += f'<rect x="-11" y="-6" width="5" height="12" fill="#E09400"/>'
    b += paw(3, 0.5, 4.2, "#7A4A08", "none", 0)
    return Sprite("fx_casing", 60, 36, "-15 -9 30 18", b)


def cat_holo():
    """萌點：磁軌光束命中處浮現的貓頭全息標誌（六角框裡的貓頭輪廓）。"""
    head = (f'<path d="M-22,-4 L-24,-26 L-10,-15 Q0,-19 10,-15 L24,-26 L22,-4 Q22,18 0,20 Q-22,18 -22,-4 Z" fill="none" stroke="{CYAN}" stroke-width="3" stroke-linejoin="round"/>'
            f'<circle cx="-8" cy="0" r="2.6" fill="{CYAN}"/><circle cx="8" cy="0" r="2.6" fill="{CYAN}"/>'
            f'<path d="M-4,7 Q0,10 4,7" fill="none" stroke="{CYAN}" stroke-width="2"/>'
            f'<polygon points="{hexagon(0, 0, 36)}" fill="none" stroke="{WHITE}" stroke-width="2"/>')
    b = f'<circle r="44" fill="url(#cyanFlash)" opacity="0.8"/>' + lit(head, 3)
    return Sprite("fx_catholo", 200, 200, "-50 -50 100 100", b, GLOW)


def graviton():
    """橘貓專武：重力肉球砲。黑洞核心＋紫色吸積環；萌點是中央浮現的全息肉球。"""
    b = '<circle r="34" fill="url(#singular)"/>'
    ring = (f'<ellipse rx="46" ry="11" fill="none" stroke="{VIOLET}" stroke-width="4" transform="rotate(-18)"/>'
            f'<ellipse rx="38" ry="7" fill="none" stroke="{WHITE}" stroke-width="1.6" transform="rotate(-18)"/>')
    b += lit(ring, 3)
    return Sprite("shot_graviton", 220, 220, "-55 -55 110 110", b, GLOW)


def graviton_paw():
    """重力球中央的橘色肉球（另外疊上去、不跟著黑洞旋轉，永遠正向朝上）。"""
    pad = "M0,-2 C9,-2 15,5 14,11 C13,16 7,16 0,15 C-7,16 -13,16 -14,11 C-15,5 -9,-2 0,-2 Z"
    beans = ((-13, -9, 4.6, 5.6), (-5, -15, 4.6, 5.8), (5, -15, 4.6, 5.8), (13, -9, 4.6, 5.6))
    glow = f'<circle r="22" fill="#FF9A2E" opacity="0.35" filter="url(#g3)"/>'
    b = glow + path(pad, "#FF9A2E", NAVY, 2.2)
    b += "".join(ellipse(x, y, rx, ry, "#FF9A2E", NAVY, 2.2) for x, y, rx, ry in beans)
    b += path("M-6,3 Q0,0 6,3", "none", "#FFD3A0", 2.4)
    b += "".join(ellipse(x - 1, y - 2, 1.4, 1.8, "#FFD3A0", "none") for x, y, _, _ in beans)
    return Sprite("shot_graviton_paw", 120, 120, "-27 -27 54 54", b, GLOW)


def mono_claw():
    """黑貓專武：單分子爪刃。三道極細雷射刃＋殘影（爪痕本身就是萌點）。"""
    b = ""
    for k in (-1, 0, 1):
        x = k * 13
        for ghost in range(3):
            op = 0.9 - ghost * 0.3
            off = ghost * 9
            line = (f'<path d="M{x - 10},{30 + off} L{x + 10},{-34 + off}" stroke="{NAVY}" stroke-width="{6 - ghost}" stroke-linecap="square"/>'
                    f'<path d="M{x - 10},{30 + off} L{x + 10},{-34 + off}" stroke="{CYAN}" stroke-width="{3.5 - ghost}" stroke-linecap="square"/>'
                    f'<path d="M{x - 10},{30 + off} L{x + 10},{-34 + off}" stroke="{WHITE}" stroke-width="1.2"/>')
            b += f'<g opacity="{op:.2f}">{lit(line, 3)}</g>'
    return Sprite("shot_monoclaw", 110, 150, "-36 -42 72 96", b, GLOW)


def fish_missile():
    """追蹤彈：微型飛彈。萌點是小魚乾造型的金屬彈體，朝 +x。"""
    body = "M-16,0 C-6,-9 10,-9 20,0 C10,9 -6,9 -16,0 Z"
    tail = "M-14,0 L-26,-9 L-23,0 L-26,9 Z"
    b = f'<path d="M-46,0 L-24,0" stroke="{CYAN}" stroke-width="6" stroke-linecap="round" filter="url(#g3)"/><path d="M-40,0 L-24,0" stroke="{WHITE}" stroke-width="2.5" stroke-linecap="round"/>'
    b += path(tail, "url(#steel)", NAVY, 2) + path(body, "url(#steel)", NAVY, 2)
    b += path("M-4,-6 Q0,0 -4,6 M4,-7 Q8,0 4,7", "none", CYAN, 1.6)
    b += ellipse(12, -2, 2.2, 2.2, CYAN, "none")
    return Sprite("shot_fish2", 150, 50, "-48 -16 96 32", b, GLOW)


def reticle():
    """鎖定框（洋紅）。"""
    r = 34
    c = 14
    s = ""
    for sx, sy in ((-1, -1), (1, -1), (1, 1), (-1, 1)):
        s += f'<path d="M{sx * r},{sy * (r - c)} L{sx * r},{sy * r} L{sx * (r - c)},{sy * r}" fill="none" stroke="{MAGENTA}" stroke-width="4"/>'
    s += f'<circle r="6" fill="none" stroke="{MAGENTA}" stroke-width="2"/>'
    s += f'<text x="{-r}" y="{-r - 5}" font-family="{MONO}" font-size="13" font-weight="800" fill="{MAGENTA}">LOCK</text>'
    return Sprite("fx_reticle", 200, 200, "-50 -50 100 100", lit(s, 3), GLOW)


def hex_paw():
    """聲波力場盾擋下子彈時亮起的六角格；萌點是格子裡的肉球。"""
    b = f'<polygon points="{hexagon(0, 0, 24)}" fill="{CYAN}" fill-opacity="0.35" stroke="{WHITE}" stroke-width="2.5"/>'
    b += paw(0, 1, 9, WHITE, "none", 0)
    return Sprite("fx_hexpaw", 120, 120, "-30 -30 60 60", lit(b, 3), GLOW)


def hit_mark():
    s = "".join(f'<line x1="{sx * 6}" y1="{sy * 6}" x2="{sx * 16}" y2="{sy * 16}" stroke="{WHITE}" stroke-width="4" stroke-linecap="square"/>' for sx, sy in ((-1, -1), (1, -1), (1, 1), (-1, 1)))
    return Sprite("fx_hitmark", 80, 80, "-20 -20 40 40", lit(s, 3), GLOW)


def paw_stamp():
    """大絕：軌道砲落地時的巨大肉球形衝擊波（萌點）。"""
    s = ""
    for dx, dy, rx, ry in ((0, 18, 34, 28), (-34, -16, 13, 15), (-12, -36, 13, 15), (12, -36, 13, 15), (34, -16, 13, 15)):
        s += f'<ellipse cx="{dx}" cy="{dy}" rx="{rx}" ry="{ry}" fill="none" stroke="{AMBER}" stroke-width="5"/>'
        s += f'<ellipse cx="{dx}" cy="{dy}" rx="{rx}" ry="{ry}" fill="none" stroke="{WHITE}" stroke-width="1.6"/>'
    return Sprite("fx_pawstamp", 260, 260, "-62 -62 124 124", lit(s, 6), GLOW)


def data_bit():
    """消彈後的資料方塊（取代糖果）。"""
    b = f'<rect x="-8" y="-8" width="16" height="16" fill="{NAVY}"/><rect x="-6" y="-6" width="12" height="12" fill="{CYAN}"/><rect x="-3" y="-3" width="6" height="6" fill="{WHITE}"/>'
    return Sprite("item_data", 48, 48, "-12 -12 24 24", lit(b, 3), GLOW)


def tech_text(name, s, color, size=40, cjk=False):
    """色差 HUD 文字：洋紅、青色錯位疊在下面，外加深藍描邊。"""
    w = len(s) * size * (1.0 if cjk else 0.64) + 30
    h = size * 1.5
    font = CJK if cjk else MONO
    base = f'font-family="{font}" font-size="{size}" font-weight="800" text-anchor="middle" letter-spacing="{0 if cjk else 2}"'
    y = size * 0.36
    b = (f'<text x="0" y="{y}" {base} fill="none" stroke="{NAVY}" stroke-width="7" stroke-linejoin="round">{s}</text>'
         f'<text x="-3" y="{y}" {base} fill="{MAGENTA}" opacity="0.9">{s}</text>'
         f'<text x="3" y="{y + 1}" {base} fill="{CYAN}" opacity="0.9">{s}</text>'
         f'<text x="0" y="{y}" {base} fill="{color}">{s}</text>')
    return Sprite(name, int(w * 2), int(h * 2), f"{-w / 2:.0f} {-h / 2:.0f} {w:.0f} {h:.0f}", b)


def tech_digit(ch, name):
    return tech_text(name, ch, WHITE, 44)


# ───────────── Boss 的科技道具（暖色能量） ─────────────

def fork_head():
    """小起司的電漿叉：鋼製叉身＋洋紅能量叉尖。朝 +x；x=0 接握柄、x=130 為尖端。"""
    b = path("M0,-12 L30,-12 Q40,-12 44,-30 L44,30 Q40,12 30,12 L0,12 Z", "url(#steel)", NAVY, 3)
    b += path("M44,-34 L58,-34 L60,-30 L60,30 L58,34 L44,34 Z", "url(#steel)", NAVY, 3)
    tines = ""
    for y in (-24, 0, 24):
        tines += f'<path d="M60,{y - 6} L116,{y - 4} L128,{y} L116,{y + 4} L60,{y + 6} Z" fill="{MAGENTA}"/>'
        tines += f'<path d="M62,{y - 2} L118,{y - 1} L124,{y} L118,{y + 1} L62,{y + 2} Z" fill="{WHITE}"/>'
    b += lit(tines, 3)
    b += f'<rect x="8" y="-4" width="20" height="8" fill="{MAGENTA}" opacity="0.8"/>'
    for x in (48, 54):
        b += f'<line x1="{x}" y1="-26" x2="{x}" y2="26" stroke="{MAGENTA}" stroke-width="1.5" opacity="0.8"/>'
    return Sprite("prop_fork_head", 280, 160, "-10 -40 140 80", b, GLOW)


def bone_end():
    """柴犬隊長的硬光骨頭：半透明紅色能量球＋鋼製端蓋。朝 +x；x=0 接骨幹。"""
    b = ""
    for cy in (-20, 20):
        b += f'<circle cx="36" cy="{cy}" r="23" fill="{RED}" fill-opacity="0.55" stroke="{RED}" stroke-width="3"/>'
        b += f'<polygon points="{hexagon(36, cy, 12)}" fill="none" stroke="{WHITE}" stroke-width="1.6" opacity="0.8"/>'
    b = lit(b, 3)
    b += path("M0,-15 L26,-15 L26,15 L0,15 Z", "url(#steel)", NAVY, 3)
    b += f'<rect x="4" y="-4" width="18" height="8" fill="{RED}"/>'
    return Sprite("prop_bone_end", 150, 210, "-5 -52 75 105", b, GLOW)


def dipper_head():
    """熊熊大王的熱熔蜂蜜鑽：琥珀色熾熱鑽頭＋散熱孔。朝 +x。"""
    b = path("M10,-30 L70,-36 Q110,-30 112,0 Q110,30 70,36 L10,30 Z", "url(#steel)", NAVY, 3)
    glow_core = ""
    for x in (30, 50, 70, 90):
        glow_core += f'<path d="M{x},-28 Q{x + 8},0 {x},28" fill="none" stroke="{AMBER}" stroke-width="5"/>'
    glow_core += path("M96,-18 Q120,0 96,18 Q104,0 96,-18 Z", "#FFE07A", "none")
    b += lit(glow_core, 3)
    b += path("M60,34 Q64,52 70,54 Q76,52 74,34", AMBER, NAVY, 2)
    return Sprite("prop_dipper_head", 250, 200, "-8 -50 125 100", b, GLOW)


def carrot_drill():
    """兔兔隊長裝甲後拿出的「胡蘿蔔鑽頭」：鋼製軸環＋胡蘿蔔色的錐形鑽頭，螺旋溝發出琥珀色的光，尾端兩片能量葉。朝 +x。"""
    b = path("M-4,-20 L14,-20 L14,20 L-4,20 Z", "url(#steel)", NAVY, 3)
    for y in (-14, 14):
        b += path(f"M-2,{y} L-14,{y * 2.2} L-6,{y * 0.6} Z", "#6FD36B", NAVY, 2)
    b += path("M14,-28 L124,-3 Q130,0 124,3 L14,28 Z", "#FF8A2B", NAVY, 3)
    b += path("M14,-28 L124,-3 L118,-2 L14,-14 Z", "#FFB067", "none")
    grooves = ""
    for k in range(5):
        x0 = 22 + k * 20
        h = 26 * (1 - (x0 - 14) / 112)
        grooves += f'<path d="M{x0},{-h} Q{x0 + 12},0 {x0 + 4},{h}" fill="none" stroke="{AMBER}" stroke-width="3.5"/>'
    b += lit(grooves, 2)
    b += lit(f'<circle cx="126" cy="0" r="5" fill="{WHITE}"/>', 3)
    return Sprite("prop_carrot_head", 270, 140, "-14 -35 135 70", b, GLOW)


def yuzu_cannon():
    """卡皮巴拉裝甲後拿出的「橘子砲」：鋼管砲身，砲口是發光的橘子核心。朝 +x。"""
    b = path("M0,-16 L80,-20 L80,20 L0,16 Z", "url(#steel)", NAVY, 3)
    b += path("M80,-26 L96,-26 L96,26 L80,26 Z", "url(#steel)", NAVY, 3)
    for x in (16, 36, 56):
        b += f'<rect x="{x}" y="-17" width="8" height="34" fill="{CORAL}" opacity="0.85"/>'
    core = f'<circle cx="108" cy="0" r="18" fill="{CORAL}"/><circle cx="108" cy="0" r="11" fill="#FFE07A"/><circle cx="108" cy="0" r="5" fill="{WHITE}"/>'
    core += path("M106,-18 Q112,-30 122,-26 Q114,-22 110,-17 Z", "#7ED957", NAVY, 1.5)
    b += lit(core, 3)
    return Sprite("prop_yuzu_head", 270, 140, "-6 -35 135 70", b, GLOW)


def cryo_gun():
    """企鵝艦長的「急凍光線槍」：鋼製槍身＋冰藍線圈，槍口是發光的冰晶（外圈警示紅）。朝 +x。"""
    b = path("M0,-15 L74,-18 L74,18 L0,15 Z", "url(#steel)", NAVY, 3)
    for x in (12, 30, 48):
        b += f'<rect x="{x}" y="-19" width="9" height="38" rx="3" fill="#8FE3FF" stroke="{NAVY}" stroke-width="2"/>'
    b += path("M74,-24 L90,-24 L90,24 L74,24 Z", "url(#steel)", NAVY, 3)
    crystal = f'<circle cx="106" cy="0" r="20" fill="none" stroke="{RED}" stroke-width="4"/>'
    crystal += path("M106,-18 L118,0 L106,18 L94,0 Z", "#BDF3FF", NAVY, 2)
    crystal += path("M106,-10 L112,0 L106,10 L100,0 Z", WHITE, "none")
    b += lit(crystal, 3)
    return Sprite("prop_cryo_head", 270, 140, "-6 -35 135 70", b, GLOW)


def leaf_launcher():
    """無尾熊長老裝甲後拿出的「尤加利葉刃發射器」：鋼製軸環＋一片大尤加利葉形的刃，葉脈發出琥珀色的光。朝 +x。"""
    b = path("M-4,-18 L16,-18 L16,18 L-4,18 Z", "url(#steel)", NAVY, 3)
    b += path("M16,0 C40,-34 96,-30 128,0 C96,30 40,34 16,0 Z", "#7CCB8A", NAVY, 3)
    b += path("M16,0 C40,-34 96,-30 128,0 C96,-18 40,-20 16,0 Z", "#A8E6B0", "none")
    veins = f'<path d="M18,0 L124,0" stroke="{AMBER}" stroke-width="3.5" fill="none"/>'
    for k in range(4):
        x = 36 + k * 22
        veins += f'<path d="M{x},0 L{x + 14},-14 M{x},0 L{x + 14},14" stroke="{AMBER}" stroke-width="2.5" fill="none"/>'
    b += lit(veins, 2)
    b += lit(f'<path d="M16,0 C40,-34 96,-30 128,0 C96,30 40,34 16,0 Z" fill="none" stroke="{CORAL}" stroke-width="2"/>', 2)
    return Sprite("prop_leaf_head", 270, 140, "-8 -35 135 70", b, GLOW)


def stone_hammer():
    """水獺姊妹的「重力石錘」：溪邊的大圓石綁在鋼柄上，外圍繞著洋紅色的重力環。朝 +x（錘頭和柄垂直）。"""
    b = path("M0,-7 L46,-7 L46,7 L0,7 Z", "url(#steel)", NAVY, 3)
    b += path("M40,-12 L54,-12 L54,12 L40,12 Z", "#8D6E63", NAVY, 2.5)
    b += f'<ellipse cx="74" cy="0" rx="26" ry="42" fill="#9AA3AE" stroke="{NAVY}" stroke-width="3"/>'
    b += f'<ellipse cx="68" cy="-12" rx="12" ry="18" fill="#C3CAD3"/>'
    b += path("M60,22 Q74,30 88,18", "none", "#6E7783", 3)
    rings = f'<ellipse cx="74" cy="0" rx="40" ry="14" fill="none" stroke="{MAGENTA}" stroke-width="3" transform="rotate(-20 74 0)"/>'
    rings += f'<ellipse cx="74" cy="0" rx="40" ry="14" fill="none" stroke="{MAGENTA}" stroke-width="3" transform="rotate(30 74 0)"/>'
    b += lit(rings, 2)
    return Sprite("prop_hammer_head", 240, 200, "-6 -50 120 100", b, GLOW)


def lost_props():
    """卸甲時被打飛的整把武器。"""
    out = []
    # 叉子
    f = path("M-90,-6 L-10,-6 L-10,6 L-90,6 Z", "url(#steel)", NAVY, 3)
    f += f'<g transform="translate(-10 0) scale(0.75)">{fork_head().body}</g>'
    out.append(Sprite("lost_fork", 400, 120, "-100 -30 200 60", f, GLOW))
    # 骨頭
    bn = path("M-50,-11 L50,-11 L50,11 L-50,11 Z", f"{RED}", NAVY, 3, 'fill-opacity="0.6"')
    bn += f'<g transform="translate(40 0) scale(0.75)">{bone_end().body}</g><g transform="translate(-40 0) scale(-0.75 0.75)">{bone_end().body}</g>'
    out.append(Sprite("lost_bone", 400, 180, "-100 -45 200 90", bn, GLOW))
    # 蜂蜜鑽
    d = path("M-90,-7 L0,-7 L0,7 L-90,7 Z", "#C98B55", NAVY, 3)
    d += f'<g transform="translate(-6 0) scale(0.75)">{dipper_head().body}</g>'
    out.append(Sprite("lost_dipper", 400, 160, "-100 -40 200 80", d, GLOW))
    # 急凍光線槍
    c = f'<g transform="translate(-60 0)">{cryo_gun().body}</g>'
    out.append(Sprite("lost_cryo", 300, 150, "-70 -35 140 70", c, GLOW))
    # 重力石錘
    h = path("M-90,-7 L-30,-7 L-30,7 L-90,7 Z", "url(#steel)", NAVY, 3)
    h += f'<g transform="translate(-36 0)">{stone_hammer().body}</g>'
    out.append(Sprite("lost_hammer", 300, 200, "-95 -50 190 100", h, GLOW))
    return out


def all_sprites():
    out = [plasma_bolt(), casing(), cat_holo(), graviton(), graviton_paw(), fish_missile(), reticle(), hex_paw(),
           hit_mark(), paw_stamp(), data_bit(), fork_head(), bone_end(), dipper_head(), yuzu_cannon(), carrot_drill(),
           cryo_gun(), leaf_launcher(), stone_hammer()]
    out += lost_props()
    out += [
        tech_text("sfx_hit", "HIT", WHITE, 34),
        tech_text("sfx_term", "TERMINATED", WHITE, 34),
        tech_text("sfx_crit", "CRITICAL", "#FFE07A", 38),
        tech_text("sfx_data", "DATA +", CYAN, 34),
        tech_text("sfx_elite", "ELITE DETECTED", "#FFB3E6", 30),
        tech_text("sfx_break", "卸甲！BREAK", "#FFE07A", 40, cjk=True),
        tech_text("sfx_armor", "裝甲！ARMOR UP", "#FFC1A6", 40, cjk=True),
        tech_text("sfx_chain", "CHAIN", CYAN, 30),
        tech_text("sfx_overdrive", "OVERDRIVE", WHITE, 60),
        tech_text("sfx_roar", "吼～！", "#FFF6D6", 60, cjk=True),
    ]
    out += [tech_digit(str(d), f"fx_d{d}") for d in range(10)]
    out += [tech_digit("+", "fx_plus"), tech_digit("×", "fx_x")]
    return out
