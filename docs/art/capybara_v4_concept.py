"""卡皮巴拉 v4 造型設定稿：參考使用者提供的卡皮巴拉玩偶與「卡皮巴拉小黃豚」插畫。

執行：python3 docs/art/capybara_v4_concept.py → docs/art/capybara-v4-concept.svg，
再用 tools/art/render.mjs 轉成 PNG。

參考圖的辨識點（正面也不會像熊的關鍵）：
- 頭身一體的「豆子／梨形」：頭和身體一樣寬，沒有脖子，頭是圓角方塊。
- 一大塊深色的方形口鼻，從眼睛下方一路蓋到下巴（熊是突出的淺色小口鼻＋黑圓鼻頭）。
- 鼻孔只是口鼻上方兩道「)(」小縫，下面接「人」字嘴。
- 眼睛是很小的瞇瞇線，長在頭的上側、離得很開。
- 小圓耳在頭頂兩側角落、深色；短短的小手、深色腳掌。
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "..", "tools", "art"))
from common import COMMON_DEFS, INK, blush, ellipse, g, path, tears, wob  # noqa: E402
import capybara as v1  # noqa: E402
import scifi  # noqa: E402

FUR = "#D39A5B"
FUR_D = "#B47B41"
FUR_L = "#E6B77E"
MUZZLE = "#8E5A33"
MUZZLE_D = "#5E3A22"
WOOD = "#E9C48E"
WOOD_D = "#C99A5E"
WATER = "#8EDBD4"
WATER_D = "#6CC5BE"
STEEL = "#5B6378"
CORAL = "#FF7A45"
CJK = "WenQuanYi Zen Hei"


def capy_body(mood=0, arms=True):
    """正面、頭身一體的卡皮巴拉（不含木桶），中心在 (0,0)，頭頂約 y=-180、屁股約 y=130。"""
    b = ""
    # 小圓耳：頭頂兩側角落
    for s in (-1, 1):
        b += ellipse(s * 118, -158, 22, 20, MUZZLE, INK, 4) + ellipse(s * 116, -154, 10, 9, MUZZLE_D, "none")
    # 豆子形：圓角方頭直接接到圓圓的身體
    sil = ("M-138,-110 C-140,-160 -100,-182 -40,-184 L40,-184 C100,-182 140,-160 138,-110 "
           "C136,-60 150,-10 156,40 C162,100 110,132 0,134 C-110,132 -162,100 -156,40 C-150,-10 -136,-60 -138,-110 Z")
    b += path(sil, FUR, INK, 5)
    # 頭頂稍深的毛色、肚子稍淺
    b += path("M-110,-160 C-60,-180 60,-180 110,-160 C60,-170 -60,-170 -110,-160 Z", FUR_D, "none", None, 'opacity="0.6"')
    b += ellipse(0, 70, 90, 56, FUR_L, "none", None, 'opacity="0.7"')
    # 一大塊深色方形口鼻：眼睛下方蓋到下巴
    b += path("M-56,-118 C-24,-128 24,-128 56,-118 C80,-110 84,-76 82,-48 C80,-18 56,-4 0,-4 C-56,-4 -80,-18 -82,-48 C-84,-76 -80,-110 -56,-118 Z",
              MUZZLE, INK, 4)
    # 鼻孔「)(」＋「人」字嘴
    b += path("M-24,-106 Q-12,-98 -18,-86 M24,-106 Q12,-98 18,-86", "none", MUZZLE_D, 5)
    b += path("M0,-82 L0,-50", "none", MUZZLE_D, 4)
    if mood == 2:
        b += path("M-20,-30 Q-10,-46 0,-50 Q10,-46 20,-30", "none", MUZZLE_D, 4)
    else:
        b += path("M-22,-36 Q-8,-38 0,-50 Q8,-38 22,-36", "none", MUZZLE_D, 4)
    # 眼睛：很小的瞇瞇線，在頭的上側、離得很開
    ey = -128
    if mood == 0:
        for s in (-1, 1):
            b += path(f"M{s * 84},{ey} L{s * 112},{ey - 2}", "none", None, 5)
    else:
        # 科技潛水護目鏡：鋼框＋暖色鏡片（敵方道具走暖色）
        b += path(f"M-140,{ey} L140,{ey}", "none", STEEL, 12)
        for s in (-1, 1):
            b += path(f"M{s * 66},{ey - 24} L{s * 132},{ey - 22} L{s * 128},{ey + 22} L{s * 70},{ey + 22} Z", STEEL, INK, 4)
            b += path(f"M{s * 74},{ey - 16} L{s * 124},{ey - 15} L{s * 121},{ey + 14} L{s * 77},{ey + 14} Z", CORAL, "none")
            b += path(f"M{s * 80},{ey - 9} L{s * 100},{ey - 10}", "none", "#FFE0CC", 4)
        b += path(f"M-66,{ey - 4} Q0,{ey - 16} 66,{ey - 4}", "none", STEEL, 6)
        if mood == 2:
            b += path(f"M86,{ey - 15} L98,{ey} L88,{ey + 13} M98,{ey} L118,{ey - 8}", "none", "#FFFFFF", 2.5)
            b += tears(-100, ey + 22, 50, -1) + tears(100, ey + 22, 50, 1)
    b += blush(-112, -72, 22) + blush(112, -72, 22)
    # 短短的小手（泡在桶裡時改成搭在桶邊）
    for s in ((-1, 1) if arms else ()):
        b += path(f"M{s * 138},10 C{s * 150},28 {s * 140},48 {s * 116},46 C{s * 104},38 {s * 112},18 {s * 126},10 Z", FUR, INK, 4)
        b += ellipse(s * 122, 40, 12, 9, MUZZLE, "none")
    return b


def capy(mood=0):
    """泡在檜木桶裡的版本（遊戲 Boss 用）：水面在 y=70。"""
    b = ""
    for (x, y) in ((-190, -150), (200, -130), (150, -220)):
        b += path(f"M{x},{y + 40} Q{x - 16},{y + 18} {x},{y} Q{x + 16},{y - 18} {x},{y - 40}", "none", "#FFFFFF", 8, 'opacity="0.8"')
    b += ellipse(0, 72, 220, 46, WOOD_D, INK, 5) + ellipse(0, 72, 202, 34, WATER, "none")
    b += capy_body(mood, arms=False)
    if mood < 2:
        b += v1.yuzu(0, -206, 28)
    if mood >= 1:
        # 肩甲
        for s in (-1, 1):
            b += path(f"M{s * 100},-30 L{s * 160},-40 L{s * 168},20 L{s * 112},22 Z", STEEL, INK, 4)
            b += path(f"M{s * 112},-20 L{s * 154},-28", "none", CORAL, 5)
    b += path("M-220,72 Q-190,62 -160,72 T-100,72 T-40,72 T20,72 T80,72 T140,72 T200,72 T230,72", "none", WATER_D, 6)
    b += path("M-226,76 Q0,140 226,76 L216,140 Q0,206 -216,140 Z", WOOD, INK, 5)
    for x in (-140, -70, 0, 70, 140):
        b += path(f"M{x},{100 + abs(x) * 0.02} L{x},{170 - abs(x) * 0.12}", "none", WOOD_D, 4)
    b += path("M-222,104 Q0,170 222,104", "none", "#8A6A40", 5)
    # 小手搭在桶邊
    for s in (-1, 1):
        b += ellipse(s * 92, 84, 26, 15, FUR, INK, 4) + ellipse(s * 92, 88, 12, 6, MUZZLE, "none")
    if mood == 2:
        # 被打哭：橘子滾到水裡
        b += v1.yuzu(170, 96, 22)
    return g(wob(b))


def cannon(x, y, s=1.0):
    """借用遊戲裡的科技橘子砲（scifi.prop_yuzu_head）。"""
    sp = scifi.yuzu_cannon()
    return (f'<svg x="{x}" y="{y}" width="{270 * s}" height="{140 * s}" viewBox="{sp.view}" overflow="visible">'
            f'<defs>{sp.defs}</defs>{sp.body}</svg>')


def label(x, y, s, size=20, color="#7D6470", anchor="start"):
    return f'<text x="{x}" y="{y}" font-family="{CJK}" font-size="{size}" fill="{color}" text-anchor="{anchor}">{s}</text>'


def callout(x1, y1, x2, y2, s):
    line = (path(f"M{x1},{y1} L{x2},{y2}", "none", "#FF8FB1", 2.5, 'stroke-dasharray="5 5"')
            + ellipse(x1, y1, 5, 5, "#FF8FB1", "none"))
    if x2 > x1:
        return line + label(x2 + 8, y2 + 7, s, 19, INK)
    rows = s.split("|")
    return line + "".join(label(40, y2 - 12 - (len(rows) - 1 - i) * 26, r, 19, INK) for i, r in enumerate(rows))


if __name__ == "__main__":
    W, H = 1400, 1100
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">']
    out.append(f'<defs>{COMMON_DEFS}<radialGradient id="bg" cx="50%" cy="40%" r="80%"><stop offset="0" stop-color="#FFFBF2"/><stop offset="1" stop-color="#E3F4F1"/></radialGradient></defs>')
    out.append(f'<rect width="{W}" height="{H}" fill="url(#bg)"/>')
    out.append(label(60, 80, "第 4 關 Boss「溫泉大王 卡皮」造型 v4", 44, INK))
    out.append(label(62, 122, "參考卡皮巴拉玩偶＋小黃豚插畫：頭身一體的豆子＋一大塊深色方口鼻＋瞇瞇眼", 24, "#B48A95"))

    cx, cy, k = 560, 560, 1.3
    out.append(f'<g transform="translate({cx} {cy}) scale({k})">{capy(0)}</g>')

    def at(x, y):
        return cx + x * k, cy + y * k

    out.append(callout(*at(-98, -128), 260, 300, "眼睛：很小的瞇瞇線|長在頭的上側、離很開"))
    out.append(callout(*at(-60, -70), 260, 470, "一大塊深色方口鼻|從眼下蓋到下巴|（熊是淺色小口鼻）"))
    out.append(callout(*at(-18, -96), 260, 600, "鼻孔只是「)(」兩道縫|下面接「人」字嘴"))
    out.append(callout(*at(118, -158), 960, 220, "小圓耳在頭頂兩側角落、深色"))
    out.append(callout(*at(130, -110), 960, 300, "頭是圓角方塊，和身體一樣寬"))
    out.append(callout(*at(150, 30), 960, 470, "頭身一體的豆子形，沒有脖子"))
    out.append(callout(*at(18, -214), 960, 160, "頭頂一顆橘子（橘子湯）"))
    out.append(callout(*at(92, 84), 960, 560, "短短小手搭在桶邊，深色掌心"))

    # 裝甲模式：學參考圖拿著誇張的科技水槍
    out.append('<rect x="980" y="620" width="380" height="300" rx="20" fill="#FFFFFF" opacity="0.8" stroke="#E0C9D0" stroke-width="2"/>')
    out.append(label(1170, 656, "裝甲模式：拿出科技橘子砲", 20, "#B48A95", "middle"))
    out.append(f'<g transform="translate(1110 790) scale(0.5)">{g(wob(capy_body(1)))}</g>')
    out.append(cannon(1100, 812, 0.62))
    out.append(g(wob(ellipse(1118, 834, 14, 11, FUR, INK, 4) + ellipse(1176, 834, 14, 11, FUR, INK, 4)), width=4))
    out.append(label(1170, 905, "外表萌萌，出手是硬派科幻武器", 17, "#B48A95", "middle"))

    out.append(label(60, 950, "表情與裝甲：", 22, INK))
    for i, (m, name) in enumerate(((0, "放空（裝甲前）"), (1, "裝甲：護目鏡＋肩甲"), (2, "裝甲被打哭：鏡片裂、橘子掉"))):
        x = 300 + i * 340
        out.append(f'<g transform="translate({x} 1000) scale(0.3)">{capy(m)}</g>')
        out.append(label(x, 1085, name, 18, "#7D6470", "middle"))
    out.append("</svg>")

    path_svg = os.path.join(HERE, "capybara-v4-concept.svg")
    with open(path_svg, "w") as f:
        f.write("\n".join(out))
    print(path_svg)
