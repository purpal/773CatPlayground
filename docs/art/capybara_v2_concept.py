"""卡皮巴拉 v2 造型設定稿：修正 v1「像熊」的問題。

執行：python3 docs/art/capybara_v2_concept.py → docs/art/capybara-v2-concept.svg，
再用 tools/art/render.mjs 轉成 PNG。

v1 像熊的原因：正面圓臉、頭頂圓耳、圓黑鼻頭。v2 改成 3/4 側臉，抓住卡皮巴拉的辨識點：
長方形磚頭臉＋又長又平的吻部、鼻孔在吻部頂端、眼睛小而高、小耳朵貼在頭頂後方、沒有脖子的木桶身體。
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "..", "tools", "art"))
from common import COMMON_DEFS, INK, blush, dot_eye, ellipse, g, path, tears, text, wob  # noqa: E402
import capybara as v1  # noqa: E402

FUR = "#B9804F"
FUR_D = "#93603A"
FUR_L = "#D8AE84"
NOSE = "#5A3A2A"
WOOD = "#E9C48E"
WOOD_D = "#C99A5E"
WATER = "#8EDBD4"
STEEL = "#5B6378"
CORAL = "#FF7A45"
CJK = "WenQuanYi Zen Hei"


def capy(mood=0, s=1.0):
    """3/4 側臉（朝左）的卡皮巴拉泡在檜木桶裡。mood：0 放空、1 裝甲護目鏡、2 裝甲被打哭。"""
    b = ""
    # 蒸氣
    for (x, y) in ((-170, -170), (150, -150), (60, -230)):
        b += path(f"M{x},{y + 40} Q{x - 16},{y + 18} {x},{y} Q{x + 16},{y - 18} {x},{y - 40}", "none", "#FFFFFF", 8, 'opacity="0.8"')
    # 桶子後緣＋水
    b += ellipse(10, 112, 230, 46, WOOD_D, INK, 5) + ellipse(10, 112, 212, 34, WATER, "none")
    # 木桶般的身體（往右後方延伸），沒有脖子
    b += path("M-40,-40 C40,-90 200,-80 230,0 C250,60 220,110 150,120 L-40,120 Z", FUR, INK, 5)
    b += path("M60,-62 C130,-70 190,-50 214,-10", "none", FUR_L, 9, 'opacity="0.55"')
    # 遠側的小耳朵（頭後方）
    b += ellipse(-4, -150, 14, 12, FUR_D, INK, 4)
    # 長方形磚頭臉：頭頂平、前臉幾乎垂直、吻部又長又平
    head = ("M-40,-152 C30,-156 92,-126 98,-66 C104,-6 84,52 36,78 "
            "L-118,86 C-156,86 -172,58 -170,18 L-166,-62 C-164,-104 -136,-124 -104,-132 C-84,-146 -62,-152 -40,-152 Z")
    b += path(head, FUR, INK, 5)
    # 頭頂較深的毛色、吻部較淺
    b += path("M-110,-128 C-70,-150 10,-152 70,-120 C20,-136 -60,-136 -110,-118 Z", FUR_D, "none", None, 'opacity="0.6"')
    b += path("M-168,0 L-164,-50 C-150,-72 -110,-74 -96,-50 L-92,40 C-96,72 -120,84 -140,82 C-160,78 -170,50 -168,0 Z", FUR_L, "none", None, 'opacity="0.9"')
    # 近側的小耳朵：貼在頭頂後方
    b += ellipse(34, -146, 18, 15, FUR, INK, 4) + ellipse(34, -144, 9, 8, FUR_D, "none")
    # 鼻孔在吻部頂端：大大的深色鼻墊＋兩道縫
    b += path("M-170,-64 C-168,-92 -146,-100 -124,-92 C-114,-80 -116,-58 -128,-48 C-146,-40 -168,-44 -170,-64 Z", NOSE, INK, 3)
    b += path("M-160,-74 Q-152,-80 -146,-72 M-138,-80 Q-130,-84 -126,-74", "none", "#2B1A12", 4)
    # Y 字上唇＋小嘴
    b += path("M-142,-46 L-142,-8", "none", None, 3.5)
    if mood == 2:
        b += path("M-160,4 Q-151,14 -142,2 Q-133,14 -122,4", "none", None, 3.5)
    else:
        b += path("M-158,0 Q-150,8 -142,-6 Q-134,8 -126,0", "none", None, 3.5)
    # 眼睛：小、高、靠後
    ey = -92
    if mood == 0:
        b += path(f"M-44,{ey} Q-30,{ey + 8} -16,{ey}", "none", None, 5)
        b += path(f"M-46,{ey - 6} L-14,{ey - 6}", "none", None, 3, 'opacity="0.5"')
        b += path(f"M-122,{ey - 6} Q-114,{ey - 2} -108,{ey - 6}", "none", None, 3.5)
    else:
        # 護目鏡：一條帶子繞過頭，鏡片在眼睛上
        b += path(f"M-130,{ey - 22} C-60,{ey - 34} 40,{ey - 26} 96,{ey + 6}", "none", STEEL, 12)
        b += path(f"M-60,{ey - 24} L0,{ey - 26} L4,{ey + 14} L-56,{ey + 16} Z", STEEL, INK, 4)
        b += path(f"M-52,{ey - 16} L-6,{ey - 18} L-4,{ey + 8} L-50,{ey + 10} Z", CORAL, "none")
        b += path(f"M-46,{ey - 10} L-24,{ey - 11}", "none", "#FFE0CC", 4)
        if mood == 2:
            b += path(f"M-30,{ey - 16} L-22,{ey - 4} L-30,{ey + 6}", "none", "#FFFFFF", 2.5)
            b += tears(-40, ey + 18, 46, -1)
    b += blush(-20, -40, 22)
    # 鬍鬚（稀疏）
    b += path("M-156,-30 L-196,-40 M-154,-20 L-194,-14", "none", "#8A6A5C", 2.5)
    # 頭頂：毛巾＋柚子（被打哭時柚子掉了）
    b += v1.towel(-56, -156, 92)
    if mood < 2:
        b += v1.yuzu(-50, -184, 24)
    # 裝甲：肩甲在身體上
    if mood >= 1:
        b += path("M120,-56 L196,-40 L206,24 L124,10 Z", STEEL, INK, 4) + path("M132,-40 L190,-28", "none", CORAL, 5)
    # 桶子前緣
    b += path("M-220,112 Q10,170 240,112 L232,160 Q10,214 -212,160 Z", WOOD, INK, 5)
    for x in (-150, -75, 0, 75, 150):
        b += path(f"M{x},{134} L{x},{184 - abs(x) * 0.12}", "none", WOOD_D, 4)
    b += path("M-216,136 Q10,190 236,136", "none", "#8A6A40", 5)
    # 前腳搭在桶邊
    b += ellipse(-110, 118, 28, 15, FUR, INK, 4) + ellipse(-40, 122, 26, 14, FUR, INK, 4)
    return f'<g transform="scale({s})">{g(wob(b))}</g>'


def label(x, y, s, size=20, color="#7D6470", anchor="start"):
    return f'<text x="{x}" y="{y}" font-family="{CJK}" font-size="{size}" fill="{color}" text-anchor="{anchor}">{s}</text>'


def callout(x1, y1, x2, y2, s):
    """右側標註：文字接在線尾。左側標註：文字分行、靠左對齊放在線尾上方（避免超出畫布）。"""
    line = (path(f"M{x1},{y1} L{x2},{y2}", "none", "#FF8FB1", 2.5, 'stroke-dasharray="5 5"')
            + ellipse(x1, y1, 5, 5, "#FF8FB1", "none"))
    if x2 > x1:
        return line + label(x2 + 8, y2 + 7, s, 19, INK)
    rows = s.split("|")
    return line + "".join(label(40, y2 - 12 - (len(rows) - 1 - i) * 26, r, 19, INK) for i, r in enumerate(rows))


W, H = 1400, 1060
out = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">']
out.append(f'<defs>{COMMON_DEFS}<radialGradient id="bg" cx="50%" cy="40%" r="80%"><stop offset="0" stop-color="#FFFBF2"/><stop offset="1" stop-color="#E3F4F1"/></radialGradient></defs>')
out.append(f'<rect width="{W}" height="{H}" fill="url(#bg)"/>')
out.append(label(60, 80, "第 4 關 Boss「溫泉大王 卡皮」造型 v2", 44, INK))
out.append(label(62, 122, "修正 v1 像熊的問題：改用 3/4 側臉，抓住卡皮巴拉的辨識點", 24, "#B48A95"))

# 主圖
out.append(f'<g transform="translate(560 560)">{capy(0, 1.35)}</g>')
cx, cy, k = 560, 560, 1.35
pts = {
    "head": (-60 * k, -120 * k), "nose": (-150 * k, -70 * k), "eye": (-30 * k, -92 * k), "ear": (34 * k, -146 * k),
    "lip": (-142 * k, -20 * k), "body": (180 * k, -20 * k),
}
out.append(callout(cx + pts["nose"][0], cy + pts["nose"][1], 250, 380, "鼻孔在吻部頂端|（兩道縫，不是圓鼻頭）"))
out.append(callout(cx + pts["lip"][0], cy + pts["lip"][1], 250, 540, "Y 字上唇|嘴巴小小的"))
out.append(callout(cx - 165 * k, cy + 20 * k, 250, 680, "長方形磚頭臉：|前臉幾乎垂直、|吻部又長又平"))
out.append(callout(cx + pts["eye"][0], cy + pts["eye"][1], 960, 250, "眼睛小、位置高、靠後；半閉的放空眼"))
out.append(callout(cx + pts["ear"][0], cy + pts["ear"][1], 960, 330, "小耳朵貼在頭頂後方（不是頭頂兩側的圓耳）"))
out.append(callout(cx + pts["body"][0], cy + pts["body"][1], 1000, 470, "沒有脖子，木桶般的身體往後延伸"))
out.append(callout(cx - 50 * k, cy - 184 * k, 960, 170, "毛巾＋柚子（冬至柚子湯）"))

# v1 對照
out.append(f'<rect x="1080" y="560" width="280" height="250" rx="20" fill="#FFFFFF" opacity="0.8" stroke="#E0C9D0" stroke-width="2"/>')
v1_sprite = v1.boss_capy(0, 0)
out.append(f'<g transform="translate(1220 700) scale(0.55)">{v1_sprite.body}</g>')
out.append(label(1220, 600, "v1（像熊）", 20, "#B48A95", "middle"))
out.append(f'<path d="M1110,640 L1330,790 M1330,640 L1110,790" stroke="#FF5C7A" stroke-width="6" opacity="0.6"/>')
out.append(label(1220, 840, "正面圓臉＋圓耳＋圓鼻＝熊", 17, "#B48A95", "middle"))

# 三種心情
out.append(label(60, 890, "表情與裝甲：", 22, INK))
for i, (m, name) in enumerate(((0, "放空（裝甲前）"), (1, "裝甲：護目鏡＋肩甲"), (2, "裝甲被打哭：鏡片裂、柚子掉"))):
    x = 300 + i * 360
    out.append(f'<g transform="translate({x} 950) scale(0.42)">{capy(m)}</g>')
    out.append(label(x, 1045, name, 18, "#7D6470", "middle"))
out.append("</svg>")

path_svg = os.path.join(HERE, "capybara-v2-concept.svg")
with open(path_svg, "w") as f:
    f.write("\n".join(out))
print(path_svg)
