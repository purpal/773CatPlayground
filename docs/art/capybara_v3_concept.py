"""卡皮巴拉 v3 造型設定稿：v1 像熊、v2 仍不像水豚，v3 改成正側面剪影。

執行：python3 docs/art/capybara_v3_concept.py → docs/art/capybara-v3-concept.svg，
再用 tools/art/render.mjs 轉成 PNG。

v2 失敗的原因：深色圓鼻墊＋淺色吻部＝狗/熊的臉；頭比身體高大、像熊坐著。
v3 抓水豚的剪影：正側面、頭是橫長的「長方形吐司」、和背部一樣高連成一條線（沒有脖子）；
吻部又高又鈍、和臉同色（沒有淺色口鼻、沒有黑鼻頭），只有吻部頂端一道鼻孔縫；
眼睛是小豆豆、位置高、在頭的中後段；耳朵只露出一點點小半圓；泡在水裡只剩頭和背。
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "..", "tools", "art"))
from common import COMMON_DEFS, INK, blush, dot_eye, ellipse, g, path, tears, wob  # noqa: E402
import capybara as v1  # noqa: E402

FUR = "#B07A4C"
FUR_D = "#8C5D38"
FUR_L = "#C99566"
NOSTRIL = "#3A2418"
WOOD = "#E9C48E"
WOOD_D = "#C99A5E"
WATER = "#8EDBD4"
WATER_D = "#6CC5BE"
STEEL = "#5B6378"
CORAL = "#FF7A45"
CJK = "WenQuanYi Zen Hei"


def capy(mood=0, tub=True):
    """正側面（朝左）的卡皮巴拉泡在檜木桶裡，水面在 y=30。mood：0 放空、1 裝甲、2 裝甲被打哭。"""
    b = ""
    for (x, y) in ((-200, -190), (190, -170), (90, -240)):
        b += path(f"M{x},{y + 40} Q{x - 16},{y + 18} {x},{y} Q{x + 16},{y - 18} {x},{y - 40}", "none", "#FFFFFF", 8, 'opacity="0.8"')
    if tub:
        b += ellipse(40, 30, 320, 54, WOOD_D, INK, 5) + ellipse(40, 30, 302, 42, WATER, "none")
    # 頭＋背是同一個剪影、只描一圈外框：橫長的頭、眼睛那裡微微隆起、
    # 頭頂和背幾乎一樣高、中間只微微凹一下（沒有脖子）、背像吐司圓圓收尾（沒有尾巴）
    sil = ("M-206,30 C-230,22 -243,4 -246,-24 C-250,-62 -246,-98 -228,-116 "
           "C-206,-138 -164,-147 -122,-150 C-112,-151 -106,-158 -92,-158 C-78,-158 -72,-152 -58,-150 "
           "C-20,-146 0,-140 20,-136 C80,-152 220,-152 290,-92 C318,-62 322,-10 316,30 Z")
    b += path(sil, FUR, INK, 5)
    # 頭的份量感：只在接縫處淡淡一筆陰影
    b += path("M18,-128 C30,-96 30,-50 14,0", "none", FUR_D, 6, 'opacity="0.3"')
    # 粗硬的毛：幾撇短毛
    for (x, y, dx) in ((-190, -128, 14), (-150, -138, 14), (-30, -138, 14), (80, -138, 16), (160, -136, 16), (240, -110, 14)):
        b += path(f"M{x},{y} l{dx},6", "none", FUR_D, 3.5, 'opacity="0.7"')
    # 鈍鈍的大鼻塊：淡淡一筆輪廓，臉頰再一筆
    b += path("M-192,-132 C-172,-96 -172,-44 -200,-6", "none", FUR_D, 5, 'opacity="0.4"')
    b += path("M-150,-14 C-110,-6 -60,-20 -40,-50", "none", FUR_D, 5, 'opacity="0.3"')
    # 鼻孔：吻部頂端一個小小的斜橢圓
    b += ellipse(-228, -104, 8, 4.5, NOSTRIL, "none", None, 'transform="rotate(-25 -228 -104)"')
    # 上唇裂＋小嘴：在前端下方
    b += path("M-246,-32 Q-236,-28 -233,-18", "none", None, 3.5)
    b += path("M-238,-10 Q-222,-2 -206,-8", "none", None, 3.5)
    # 眼睛：小豆豆、半閉，長在頭頂的小隆起上（中後段）
    ex, ey = -92, -132
    if mood == 0:
        b += path(f"M{ex - 9},{ey - 2} A9,9 0 0 0 {ex + 9},{ey - 2} Z", "#2E2128", "none")
        b += path(f"M{ex - 13},{ey - 2} L{ex + 13},{ey - 2}", "none", None, 4)
    else:
        b += path(f"M-236,{ey + 6} C-160,{ey - 14} -40,{ey - 16} 24,{ey - 2}", "none", STEEL, 12)
        b += path(f"M{ex - 26},{ey - 20} L{ex + 26},{ey - 22} L{ex + 28},{ey + 14} L{ex - 24},{ey + 16} Z", STEEL, INK, 4)
        b += path(f"M{ex - 18},{ey - 13} L{ex + 19},{ey - 14} L{ex + 20},{ey + 7} L{ex - 17},{ey + 9} Z", CORAL, "none")
        b += path(f"M{ex - 12},{ey - 7} L{ex + 6},{ey - 8}", "none", "#FFE0CC", 4)
        if mood == 2:
            b += path(f"M{ex + 4},{ey - 14} L{ex + 12},{ey - 2} L{ex + 4},{ey + 8}", "none", "#FFFFFF", 2.5)
            b += tears(ex - 6, ey + 20, 46, -1)
    b += blush(-150, -62, 24)
    # 耳朵：頭頂後方只露出一點小半圓
    b += path("M-34,-148 C-38,-172 -8,-176 -4,-146", FUR, INK, 4) + path("M-28,-152 C-28,-164 -14,-166 -10,-152", "none", FUR_D, 4)
    # 頭頂一顆柚子；水面上再漂兩顆（柚子湯）
    if mood < 2:
        b += v1.yuzu(-160, -172, 26)
    # 裝甲：背上的甲殼
    if mood >= 1:
        b += path("M90,-146 C170,-158 250,-136 290,-92 L272,-66 C232,-108 162,-126 96,-120 Z", STEEL, INK, 4)
        b += path("M116,-132 C176,-140 230,-122 266,-92", "none", CORAL, 5)
    # 水面：一條波浪線蓋住身體下緣
    b += path("M-262,30 Q-232,20 -202,30 T-142,30 T-82,30 T-22,30 T38,30 T98,30 T158,30 T218,30 T278,30 T338,30", "none", WATER_D, 6)
    if tub:
        b += v1.yuzu(-210, 48, 18) + v1.yuzu(240, 52, 18)
        b += path("M-290,36 Q40,112 370,36 L360,96 Q40,172 -280,96 Z", WOOD, INK, 5)
        for x in (-180, -80, 20, 120, 220):
            b += path(f"M{x + 20},{68 + abs(x) * 0.04} L{x + 20},{130 - abs(x) * 0.14}", "none", WOOD_D, 4)
        b += path("M-286,64 Q40,140 366,64", "none", "#8A6A40", 5)
    return g(wob(b))


def label(x, y, s, size=20, color="#7D6470", anchor="start"):
    return f'<text x="{x}" y="{y}" font-family="{CJK}" font-size="{size}" fill="{color}" text-anchor="{anchor}">{s}</text>'


def callout(x1, y1, x2, y2, s):
    """右側標註：文字接在線尾。左側標註：文字分行、靠左對齊放在線尾上方。"""
    line = (path(f"M{x1},{y1} L{x2},{y2}", "none", "#FF8FB1", 2.5, 'stroke-dasharray="5 5"')
            + ellipse(x1, y1, 5, 5, "#FF8FB1", "none"))
    if x2 > x1:
        return line + label(x2 + 8, y2 + 7, s, 19, INK)
    rows = s.split("|")
    return line + "".join(label(40, y2 - 12 - (len(rows) - 1 - i) * 26, r, 19, INK) for i, r in enumerate(rows))


if __name__ == "__main__":
    W, H = 1400, 1060
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">']
    out.append(f'<defs>{COMMON_DEFS}<radialGradient id="bg" cx="50%" cy="40%" r="80%"><stop offset="0" stop-color="#FFFBF2"/><stop offset="1" stop-color="#E3F4F1"/></radialGradient></defs>')
    out.append(f'<rect width="{W}" height="{H}" fill="url(#bg)"/>')
    out.append(label(60, 80, "第 4 關 Boss「溫泉大王 卡皮」造型 v3", 44, INK))
    out.append(label(62, 122, "改用正側面剪影：橫長的吐司頭＋沒有脖子＋同色的鈍吻部", 24, "#B48A95"))

    cx, cy, k = 540, 560, 1.25
    out.append(f'<g transform="translate({cx} {cy}) scale({k})">{capy(0)}</g>')

    def at(x, y):
        return cx + x * k, cy + y * k

    out.append(callout(*at(-228, -104), 200, 330, "鼻孔在吻部頂端|只有小小一個"))
    out.append(callout(*at(-244, -50), 170, 540, "吻部又高又鈍|和臉同色|不是黑鼻頭"))
    out.append(callout(*at(-92, -132), 1000, 250, "眼睛是小豆豆：位置高、在頭的中後段"))
    out.append(callout(*at(-20, -160), 1000, 320, "耳朵只露出一點小半圓"))
    out.append(callout(*at(-170, -146), 1000, 180, "頭是橫長的長方形，頭頂平"))
    out.append(callout(*at(18, -136), 1000, 390, "頭和背一樣高、直接連在一起，沒有脖子"))
    out.append(callout(*at(290, -70), 1000, 460, "背像一條吐司，圓圓收尾、沒有尾巴"))
    out.append(callout(*at(-210, 48), 200, 760, "柚子湯：頭頂一顆、|水上漂兩顆"))

    # 版本對照
    out.append('<rect x="1010" y="540" width="350" height="290" rx="20" fill="#FFFFFF" opacity="0.8" stroke="#E0C9D0" stroke-width="2"/>')
    out.append(label(1185, 578, "之前的版本", 20, "#B48A95", "middle"))
    out.append(f'<g transform="translate(1100 700) scale(0.4)">{v1.boss_capy(0, 0).body}</g>')
    import capybara_v2_concept as v2  # noqa: E402
    out.append(f'<g transform="translate(1272 700) scale(0.3)">{v2.capy(0)}</g>')
    out.append('<path d="M1030,610 L1340,800 M1340,610 L1030,800" stroke="#FF5C7A" stroke-width="6" opacity="0.5"/>')
    out.append(label(1100, 812, "v1 正面＝熊", 17, "#B48A95", "middle"))
    out.append(label(1272, 812, "v2 黑鼻頭＝狗/熊", 17, "#B48A95", "middle"))

    out.append(label(60, 890, "表情與裝甲：", 22, INK))
    for i, (m, name) in enumerate(((0, "放空（裝甲前）"), (1, "裝甲：護目鏡＋背甲"), (2, "裝甲被打哭：鏡片裂、柚子掉"))):
        x = 320 + i * 360
        out.append(f'<g transform="translate({x} 955) scale(0.38)">{capy(m)}</g>')
        out.append(label(x, 1045, name, 18, "#7D6470", "middle"))
    out.append("</svg>")

    path_svg = os.path.join(HERE, "capybara-v3-concept.svg")
    with open(path_svg, "w") as f:
        f.write("\n".join(out))
    print(path_svg)
