"""「誇張技能特效」示意圖：參考噠噠戰機的爽快感（粗雷射、滿天傷害數字、大量小怪、強烈光暈）。

執行：python3 docs/art/fx_burst_concept.py → docs/art/fx-burst-concept.svg，
再用 tools/art/render.mjs 轉成 PNG。圖中的角色、子彈都是遊戲裡現有的貼圖。
"""
import base64
import math
import os
import random

HERE = os.path.dirname(os.path.abspath(__file__))
ART = os.path.join(HERE, "..", "..", "app", "src", "main", "assets", "art")
CJK = "WenQuanYi Zen Hei"
CYAN = "#00E5FF"
NAVY = "#0A1630"
RED = "#FF3D5A"
_cache = {}


def img(name, x, y, w, rot=0, opacity=1.0, center=True):
    """把遊戲貼圖嵌進來；(x, y) 是中心點，w 是寬度。"""
    if name not in _cache:
        from PIL import Image
        path = os.path.join(ART, name + ".png")
        with Image.open(path) as im:
            ratio = im.height / im.width
        with open(path, "rb") as f:
            _cache[name] = (base64.b64encode(f.read()).decode(), ratio)
    data, ratio = _cache[name]
    h = w * ratio
    t = f' transform="rotate({rot} {x} {y})"' if rot else ""
    return (f'<image href="data:image/png;base64,{data}" x="{x - w / 2:.1f}" y="{y - h / 2:.1f}" width="{w:.1f}" height="{h:.1f}"'
            f' opacity="{opacity}"{t}/>')


def glow(content, blur=10, strength=1.0):
    """柔光：同一份內容先模糊再疊上原圖，整組用 screen 混色（遊戲裡會用預先做好的柔光貼圖＋相加混色）。"""
    return (f'<g style="mix-blend-mode:screen"><g filter="url(#blur{blur})" opacity="{strength}">{content}</g>{content}</g>')


def dmg(x, y, n, crit=False, size=None):
    size = size or (34 if crit else 20)
    fill = RED if crit else "#FFFFFF"
    extra = f'<text x="{x}" y="{y - size * 0.95}" font-family="Inter" font-weight="900" font-size="{size * 0.42}" fill="#FFE07A" stroke="{NAVY}" stroke-width="3" paint-order="stroke" text-anchor="middle">CRIT!</text>' if crit else ""
    return (f'<text x="{x}" y="{y}" font-family="Inter" font-weight="900" font-size="{size}" fill="{fill}" stroke="{NAVY}" '
            f'stroke-width="{5 if crit else 4}" paint-order="stroke" text-anchor="middle" font-style="italic">-{n}</text>' + extra)


def burst(x, y, r, core="#FFFFFF", mid="#FFB300", outer="#FF5C2A"):
    b = f'<circle cx="{x}" cy="{y}" r="{r}" fill="{outer}" opacity="0.55"/>'
    b += f'<circle cx="{x}" cy="{y}" r="{r * 0.68}" fill="{mid}"/>'
    b += f'<circle cx="{x}" cy="{y}" r="{r * 0.36}" fill="{core}"/>'
    for i in range(8):
        a = i * math.pi / 4 + r
        b += f'<path d="M{x + math.cos(a) * r * 0.6:.1f},{y + math.sin(a) * r * 0.6:.1f} L{x + math.cos(a) * r * 1.5:.1f},{y + math.sin(a) * r * 1.5:.1f}" stroke="{mid}" stroke-width="{r * 0.16:.1f}" stroke-linecap="round"/>'
    return glow(b, 6)


def beam(x, y0, y1, w, color=CYAN):
    b = f'<rect x="{x - w * 1.6}" y="{y1}" width="{w * 3.2}" height="{y0 - y1}" fill="{color}" opacity="0.35" rx="{w}"/>'
    b += f'<rect x="{x - w / 2}" y="{y1}" width="{w}" height="{y0 - y1}" fill="{color}" rx="{w / 2}"/>'
    b += f'<rect x="{x - w * 0.22}" y="{y1}" width="{w * 0.44}" height="{y0 - y1}" fill="#FFFFFF" rx="{w * 0.22}"/>'
    # 光束上的能量環
    for yy in range(int(y1) + 30, int(y0) - 40, 70):
        b += f'<ellipse cx="{x}" cy="{yy}" rx="{w * 1.25}" ry="{w * 0.3}" fill="none" stroke="#FFFFFF" stroke-width="3" opacity="0.8"/>'
    return glow(b, 14)


def background(seed, top="#141B4A", bottom="#2A1B5E"):
    rnd = random.Random(seed)
    b = f'<defs><linearGradient id="bg{seed}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{top}"/><stop offset="1" stop-color="{bottom}"/></linearGradient></defs>'
    b += f'<rect width="440" height="860" fill="url(#bg{seed})"/>'
    for _ in range(70):
        b += f'<circle cx="{rnd.uniform(0, 440):.0f}" cy="{rnd.uniform(0, 860):.0f}" r="{rnd.uniform(0.8, 2.2):.1f}" fill="#FFFFFF" opacity="{rnd.uniform(0.3, 0.9):.2f}"/>'
    for _ in range(6):
        y = rnd.uniform(0, 860)
        b += f'<path d="M{rnd.uniform(0, 440):.0f},{y:.0f} l0,{rnd.uniform(40, 90):.0f}" stroke="#7FA8FF" stroke-width="2" opacity="0.35"/>'
    return b


def hud(stage_text, wave):
    b = f'<rect x="0" y="0" width="440" height="58" fill="{NAVY}" opacity="0.65"/>'
    b += f'<text x="16" y="36" font-family="{CJK}" font-size="18" fill="#FFFFFF">{stage_text}</text>'
    b += f'<text x="424" y="36" font-family="{CJK}" font-size="18" fill="#FFE07A" text-anchor="end">{wave}</text>'
    return b


def enemy_bullets(seed, n, area):
    rnd = random.Random(seed)
    x0, y0, x1, y1 = area
    b = ""
    for _ in range(n):
        x, y = rnd.uniform(x0, x1), rnd.uniform(y0, y1)
        a = rnd.uniform(-25, 25)
        b += f'<rect x="{x - 4}" y="{y - 11}" width="8" height="22" rx="4" fill="{RED}" transform="rotate({a:.0f} {x:.0f} {y:.0f})"/>'
        b += f'<rect x="{x - 1.6}" y="{y - 7}" width="3.2" height="14" rx="1.6" fill="#FFD0D8" transform="rotate({a:.0f} {x:.0f} {y:.0f})"/>'
    return glow(b, 6)


def panel_main():
    """主武器全開：粗雷射貫穿整排小怪，旁邊電漿彈扇形掃射，滿天傷害數字。"""
    b = background(1)
    # 一大片排列整齊的小怪
    for r in range(7):
        for c in range(8):
            x, y = 40 + c * 51, 110 + r * 52
            if c in (3, 4) and r >= 1:
                continue  # 中間那兩排已經被雷射清掉
            b += img("min_plane_0" if (r + c) % 3 else "min_mouse_0", x, y, 44)
            if (r * 8 + c) % 5 == 0:
                b += f'<rect x="{x - 18}" y="{y - 30}" width="36" height="5" rx="2" fill="#2A2A3A"/><rect x="{x - 18}" y="{y - 30}" width="{14 + (r * 7 + c) % 20}" height="5" rx="2" fill="#FF5C7A"/>'
    b += enemy_bullets(11, 14, (20, 160, 420, 560))
    # 雷射路徑上連成一串的爆炸
    for i, y in enumerate((150, 205, 262, 318, 372, 430)):
        b += burst(220 + (i % 2) * 22 - 11, y, 22 + (i % 3) * 6)
    b += beam(220, 700, 60, 40)
    # 扇形電漿彈
    for k in range(-5, 6):
        if k == 0:
            continue
        a = k * 7
        for d in (120, 230, 340):
            x = 220 + math.sin(math.radians(a)) * d
            y = 690 - math.cos(math.radians(a)) * d
            b += img("shot_plasma", x, y, 20, rot=a)
    b += glow("".join(f'<circle cx="{220 + math.sin(math.radians(k * 7)) * 30:.1f}" cy="{690 - math.cos(math.radians(k * 7)) * 30:.1f}" r="6" fill="{CYAN}"/>' for k in range(-5, 6)), 6)
    # 腳印彈殼往兩側噴
    for (x, y, r) in ((170, 720, -30), (150, 700, -60), (275, 715, 35), (300, 690, 70)):
        b += img("fx_casing", x, y, 18, rot=r)
    b += img("run_orange_c_0", 220, 745, 92)
    # 滿天傷害數字
    for (x, y, n) in ((262, 140, 688), (186, 200, 688), (264, 258, 688), (188, 312, 688), (262, 368, 688), (186, 424, 688),
                      (90, 300, 145), (360, 250, 145), (120, 420, 145), (340, 470, 145), (60, 190, 145), (390, 160, 145)):
        b += dmg(x, y, n)
    b += dmg(150, 470, 1788, True) + dmg(320, 345, 2286, True, 38)
    # 掉落金幣
    for (x, y) in ((140, 520), (300, 540), (250, 600), (110, 610)):
        b += img("item_coin", x, y, 26)
    b += hud("第 2 關 雷射 Lv.5", "第 13/15 波")
    return b


def panel_ult():
    """大絕 OVERDRIVE：全螢幕肉球衝擊波＋軌道雷射＋魚形飛彈齊射，Boss 身上爆出巨大爆擊數字。"""
    b = background(2, "#0E1A40", "#1B2C6E")
    b += img("boss_dog_m1_0", 220, 230, 300)
    # 三道軌道雷射從天而降
    for x in (90, 220, 350):
        b += beam(x, 860, 0, 26)
    # 全螢幕肉球衝擊波
    b += glow(img("fx_pawstamp", 220, 330, 380, opacity=0.55), 16, 0.6)
    b += glow(f'<circle cx="220" cy="300" r="250" fill="none" stroke="{CYAN}" stroke-width="10" opacity="0.8"/>'
              f'<circle cx="220" cy="300" r="320" fill="none" stroke="#FFFFFF" stroke-width="4" opacity="0.6"/>', 10)
    # 魚形飛彈齊射（每顆都有尾焰）
    for i in range(12):
        x0 = 220 + (i - 5.5) * 30
        y0 = 660 - abs(i - 5.5) * 16
        a = -90 + (i - 5.5) * 6
        trail = f'<path d="M{x0:.0f},{y0 + 18:.0f} L{x0 - (i - 5.5) * 4:.0f},{y0 + 90:.0f}" stroke="{CYAN}" stroke-width="7" stroke-linecap="round" opacity="0.7"/>'
        b += glow(trail, 6)
        b += img("shot_fish2", x0, y0, 66, rot=a)
    for (x, y) in ((140, 270), (300, 250), (110, 380), (330, 380)):
        b += burst(x, y, 18, mid="#7FF6FF", outer=CYAN)
    b += dmg(220, 150, 24680, True, 54)
    for (x, y, n) in ((110, 200, 3120), (340, 180, 3120), (150, 420, 3120), (300, 430, 3120), (220, 470, 3120)):
        b += dmg(x, y, n, size=24)
    # 畫面邊緣的青色暈影
    b += ('<defs><radialGradient id="vig" cx="50%" cy="45%" r="70%"><stop offset="0.6" stop-color="#00E5FF" stop-opacity="0"/>'
          '<stop offset="1" stop-color="#00E5FF" stop-opacity="0.45"/></radialGradient></defs><rect width="440" height="860" fill="url(#vig)"/>')
    b += img("sfx_overdrive", 220, 545, 330)
    b += img("run_tiger_c_0", 220, 760, 100)
    b += hud("第 2 關 BOSS 柴犬將軍", "OVERDRIVE!")
    return b


def panel_chain():
    """連鎖閃電＋肉球護盾：一道閃電在小怪群裡跳十幾下，每跳一下都有命中標記和數字。"""
    b = background(3, "#1A1340", "#3A1B4E")
    rnd = random.Random(7)
    pts = []
    for r in range(6):
        for c in range(7):
            x, y = 45 + c * 58 + (r % 2) * 20, 130 + r * 62
            b += img("min_bee_0" if (r + c) % 2 else "min_puppy_0", x, y, 46)
            pts.append((x, y))
    b += enemy_bullets(21, 10, (20, 200, 420, 560))
    # 閃電路徑：從貓咪出發，在怪群之間跳來跳去
    hops = [(220, 690), (205, 502), (148, 440), (90, 378), (160, 316), (238, 254), (313, 316), (370, 378), (333, 440), (275, 502), (380, 192), (300, 130)]

    def zig(p, q):
        (x0, y0), (x1, y1) = p, q
        d = f"M{x0},{y0}"
        for t in (0.25, 0.5, 0.75):
            d += f" L{x0 + (x1 - x0) * t + rnd.uniform(-12, 12):.0f},{y0 + (y1 - y0) * t + rnd.uniform(-12, 12):.0f}"
        return d + f" L{x1},{y1}"
    bolt = "".join(f'<path d="{zig(hops[i], hops[i + 1])}" fill="none" stroke="{CYAN}" stroke-width="9" stroke-linejoin="round" opacity="0.6"/>'
                   f'<path d="{zig(hops[i], hops[i + 1])}" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-linejoin="round"/>' for i in range(len(hops) - 1))
    b += glow(bolt, 10)
    for i, (x, y) in enumerate(hops[1:]):
        b += glow(img("fx_hitmark", x, y, 52), 8)
        b += dmg(x + 22, y - 18, 2900 if i in (3, 7) else 1450, crit=(i in (3, 7)), size=None)
    # 肉球六角護盾
    shield = ""
    for k in range(10):
        a = k * 36
        shield += img("fx_hexpaw", 220 + math.cos(math.radians(a)) * 74, 740 + math.sin(math.radians(a)) * 74, 40, opacity=0.9)
    b += glow(f'<circle cx="220" cy="740" r="78" fill="{CYAN}" opacity="0.12"/><circle cx="220" cy="740" r="78" fill="none" stroke="{CYAN}" stroke-width="3"/>' + shield, 8)
    b += img("run_black_c_0", 220, 745, 92)
    b += img("sfx_chain", 220, 600, 170)
    b += f'<text x="300" y="612" font-family="Inter" font-weight="900" font-size="34" fill="#FFE07A" stroke="{NAVY}" stroke-width="5" paint-order="stroke" font-style="italic">x11</text>'
    b += hud("第 3 關 連鎖閃電 Lv.4", "第 9/15 波")
    return b


def label(x, y, s, size=20, color="#7D6470", anchor="start", weight="normal"):
    return f'<text x="{x}" y="{y}" font-family="{CJK}" font-size="{size}" fill="{color}" text-anchor="{anchor}" font-weight="{weight}">{s}</text>'


if __name__ == "__main__":
    W, H = 1500, 1160
    defs = "".join(f'<filter id="blur{s}" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="{s}"/></filter>' for s in (6, 8, 10, 14, 16))
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}"><defs>{defs}</defs>']
    out.append(f'<rect width="{W}" height="{H}" fill="#FFFBF2"/>')
    out.append(label(60, 72, "技能特效強化示意（參考噠噠戰機的爽快感）", 40, "#6B4C52"))
    out.append(label(62, 110, "粗雷射＋強烈光暈、滿天傷害數字、一整片小怪被掃光；角色和子彈都是遊戲裡現有的貼圖", 21, "#B48A95"))
    panels = ((panel_main, "① 主武器全開：雷射貫穿整排小怪"), (panel_ult, "② 大絕 OVERDRIVE：肉球衝擊波＋飛彈齊射"), (panel_chain, "③ 連鎖閃電跳 11 下＋肉球護盾"))
    for i, (fn, title) in enumerate(panels):
        x = 60 + i * 470
        out.append(f'<rect x="{x - 8}" y="{142}" width="456" height="876" rx="34" fill="#2E2433"/>')
        out.append(f'<svg x="{x}" y="150" width="440" height="860" viewBox="0 0 440 860">{fn()}</svg>')
        out.append(label(x + 220, 1052, title, 21, "#6B4C52", "middle", "bold"))
    out.append(label(60, 1110, "做法：柔光貼圖＋相加混色做出光暈（不即時模糊，省效能）；傷害數字可在設定關閉；特效數量有上限，並提供「特效品質 高／低」。", 18, "#7D6470"))
    out.append("</svg>")
    with open(os.path.join(HERE, "fx-burst-concept.svg"), "w") as f:
        f.write("\n".join(out))
    print("ok")
