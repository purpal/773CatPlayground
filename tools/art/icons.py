"""大廳的遊戲圖示與 App 圖示（v2 麻糬萌風）。

App 圖示：橘色麻糬貓的大頭，項圈掛著寫「773」的鈴鐺吊牌——「773喵」。
自適應圖示的前景畫在 108×108 的畫布，重要內容放在中央直徑 66 的安全區。
"""
from common import *
import cats

ORANGE = cats.PALETTES["orange"]


def cat_head(cx, cy, s=1.0, ink=INK):
    p = ORANGE
    out = ""
    for k in (-1, 1):
        out += path(ear(cx + k * 30 * s, cy - 40 * s, cx + k * 44 * s, cy - 8 * s, cx + k * 12 * s, cy - 22 * s), p["fur"], ink, 3.5 * s)
        out += path(ear(cx + k * 29 * s, cy - 32 * s, cx + k * 38 * s, cy - 10 * s, cx + k * 16 * s, cy - 20 * s), p["ear"], "none")
    out += path(mochi_path(cx, cy + 4 * s, 100 * s, 84 * s), p["fur"], ink, 3.5 * s)
    for k in (-1, 0, 1):
        out += path(f"M{cx + k * 9 * s - 3 * s},{cy - 36 * s} L{cx + k * 9 * s + 3 * s},{cy - 36 * s} L{cx + k * 9 * s},{cy - 24 * s + abs(k) * 3 * s} Z", p["stripe"], "none")
    out += ellipse(cx, cy + 22 * s, 24 * s, 14 * s, p["belly"], "none")
    out += dot_eye(cx - 18 * s, cy + 2 * s, 6 * s) + dot_eye(cx + 18 * s, cy + 2 * s, 6 * s)
    out += ellipse(cx, cy + 12 * s, 3.6 * s, 2.8 * s, "#FF9DB8", ink, 1.8 * s)
    out += omega_mouth(cx, cy + 17 * s, 5 * s, 2.2 * s)
    out += blush(cx - 32 * s, cy + 14 * s, 9 * s) + blush(cx + 32 * s, cy + 14 * s, 9 * s)
    return out


def app_foreground():
    b = cat_head(0, -10, 0.6)
    # 項圈＋「773」吊牌（文字不套手繪抖動，小尺寸也清楚）
    b += path("M-24,11 Q0,20 24,11", "none", "#FF5C8A", 4.5)
    b += path("M-15,16 L15,16 L16,34 Q0,41 -16,34 Z", "#FFE07A", INK, 2.2)
    b += ellipse(0, 16, 3.2, 2.6, "#FFD15C", INK, 1.6)
    label = f'<text x="0" y="31.5" text-anchor="middle" font-family="Inter" font-weight="900" font-size="13" letter-spacing="-0.5" fill="{INK}">773</text>'
    return Sprite("app_icon_fg", 432, 432, "-54 -54 108 108", g(wob(b), width=2.5) + label)


def app_preview():
    """預覽用：前景疊在背景漸層上、裁成圓形。"""
    bg = '<defs><radialGradient id="ibg" cx="50%" cy="35%" r="70%"><stop offset="0" stop-color="#FFE3EE"/><stop offset="1" stop-color="#C7B3FF"/></radialGradient></defs>'
    fg = app_foreground()
    b = bg + '<circle r="54" fill="url(#ibg)"/>' + fg.body
    return Sprite("preview_app_icon", 216, 216, "-54 -54 108 108", b)


def lobby_watersort():
    """彩虹倒水：貓咪抱著裝了彩虹三層水的試管。"""
    b = ""
    tube = "M-14,-46 L14,-46 L14,30 Q14,46 0,46 Q-14,46 -14,30 Z"
    b += f'<clipPath id="tubeClip"><path d="{tube}"/></clipPath>'
    b += f'<g clip-path="url(#tubeClip)"><rect x="-20" y="-20" width="40" height="22" fill="#8FD3FF"/><rect x="-20" y="2" width="40" height="22" fill="#FFE07A"/><rect x="-20" y="24" width="40" height="30" fill="#FF8FB1"/></g>'
    b += path(tube, "none", INK, 3.5)
    b += path("M-18,-46 L18,-46", "none", INK, 4)
    b += path("M-8,-36 L-8,20", "none", "#FFFFFF", 3, 'opacity="0.8"')
    head = cat_head(-26, 6, 0.55)
    b = head + b
    b += ellipse(-8, 30, 9, 7, ORANGE["fur"], INK, 3) + ellipse(8, 32, 9, 7, ORANGE["fur"], INK, 3)
    for (x, y, r) in ((34, -30, 5), (28, -44, 3.5), (40, -16, 3)):
        b += ellipse(x, y, r, r, "#BDE8FF", INK, 2)
    return Sprite("lobby_watersort", 256, 256, "-60 -60 120 120", g(wob(b), width=3))


def lobby_shooter():
    """喵星萌王爭霸戰：萌貓舉著發光的科幻電漿槍（外表萌、出手硬派）。"""
    b = ""
    beam = '<g opacity="0.9"><path d="M22,-10 L60,-48" stroke="#00E5FF" stroke-width="10" stroke-linecap="round" opacity="0.4"/><path d="M22,-10 L60,-48" stroke="#00E5FF" stroke-width="5" stroke-linecap="round"/><path d="M22,-10 L60,-48" stroke="#FFFFFF" stroke-width="2" stroke-linecap="round"/></g>'
    b += beam
    b += cat_head(-10, 8, 0.6)
    gun = path("M2,8 L24,-12 L32,-4 L12,16 Z", "#5B6378", "#0A1630", 2.5)
    gun += path("M8,12 L4,22 L10,24 L14,16 Z", "#3A4256", "#0A1630", 2)
    gun += path("M14,2 L24,-8", "none", "#00E5FF", 2.5)
    b += gun
    b += ellipse(6, 20, 8, 7, ORANGE["fur"], INK, 3)
    b += star(-44, -40, 6, "#FFE07A", INK, 2)
    return Sprite("lobby_shooter", 256, 256, "-60 -60 120 120", g(wob(b), width=3))


def all_sprites():
    return [app_foreground(), app_preview(), lobby_watersort(), lobby_shooter()]
