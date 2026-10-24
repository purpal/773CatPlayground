"""開發商品牌 Zuipower Studio 的 logo，以及取其形的大絕按鈕「貓吼剪影」。

logo：藍色圓章裡一隻張嘴吼叫的白色貓頭側影（朝左），上下顎延伸成兩道橫條直到圓邊。
大絕按鈕沿用同一個剪影，換成各貓的色系（橘貓：橘金、黑貓：紫），外加能量光暈。
"""
from common import *

BRAND_BLUE = "#0088FE"
BRAND_BG = "#F2F3F5"


def roar_head():
    """160×160 座標裡的貓吼剪影（朝左），左側兩道顎條超出畫面，交給圓形裁切。"""
    return ("M-20,62 L66,62 C67,57 70,55 75,54 C79,53 81,51 82,48 C84,40 92,36 100,36 "
            "L103,27 C104,24 107,24 109,27 L118,39 C122,39 126,40 129,42 "
            "L136,30 C138,27 141,28 141,31 L141,52 C146,64 145,80 139,92 "
            "C134,101 131,110 132,122 L138,180 L62,180 C66,148 72,126 82,114 "
            "L-20,114 L-20,98 L80,98 L96,90 L80,84 L78,84 L74,92 L70,84 L-20,84 Z")


def roar_eye(fill):
    return path("M90,61 Q98,54 106,58 Q99,63 90,61 Z", fill, "none")


def roar(eye_fill):
    """剪影往左下收一點，耳朵才不會被圓邊裁掉。"""
    return f'<g transform="translate(-13 9) scale(0.94)"><path d="{roar_head()}" fill="#FFFFFF"/>{roar_eye(eye_fill)}</g>'


def logo_mark(size=512, name="brand_logo"):
    clip = '<clipPath id="roarClip"><circle cx="80" cy="80" r="66"/></clipPath>'
    b = f'<circle cx="80" cy="80" r="77" fill="{BRAND_BLUE}"/>'
    b += '<circle cx="80" cy="80" r="70.5" fill="#FFFFFF"/>'
    b += f'<circle cx="80" cy="80" r="66" fill="{BRAND_BLUE}"/>'
    b += f'<g clip-path="url(#roarClip)">{roar(BRAND_BLUE)}</g>'
    return Sprite(name, size, size, "0 0 160 160", b, clip)


ULT_COLORS = {
    # 外圈、內圈漸層（亮→暗）、光暈
    "orange": ("#FFE07A", "#FFB300", "#FF6D00", "#FFD54F"),
    "black": ("#D9CCFF", "#8E6BFF", "#3A2A8C", "#B39DFF"),
}


def ult_button(kind):
    rim, c0, c1, glow = ULT_COLORS[kind]
    defs = (f'<radialGradient id="ug{kind}" cx="40%" cy="35%" r="75%"><stop offset="0" stop-color="{c0}"/><stop offset="1" stop-color="{c1}"/></radialGradient>'
            f'<clipPath id="uc{kind}"><circle cx="80" cy="80" r="66"/></clipPath>')
    b = f'<circle cx="80" cy="80" r="77" fill="{glow}" opacity="0.55" filter="url(#softGlow)"/>'
    b += f'<circle cx="80" cy="80" r="76" fill="{rim}"/>'
    b += '<circle cx="80" cy="80" r="70.5" fill="#FFFFFF"/>'
    b += f'<circle cx="80" cy="80" r="66" fill="url(#ug{kind})"/>'
    # 吼聲的能量波（從嘴往左擴散的弧）
    waves = "".join(f'<path d="M{34 - i * 12},{74 - i * 14} Q{22 - i * 14},90 {34 - i * 12},{106 + i * 14}" fill="none" stroke="#FFFFFF" stroke-width="4" stroke-linecap="round" opacity="{0.7 - i * 0.2}"/>' for i in range(3))
    b += f'<g clip-path="url(#uc{kind})">{waves}{roar(c1)}</g>'
    return Sprite(f"ult_btn_{kind}", 220, 220, "0 0 160 160", b, defs)


def all_sprites():
    return [logo_mark(), ult_button("orange"), ult_button("black")]
