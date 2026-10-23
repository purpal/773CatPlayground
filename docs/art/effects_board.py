"""產生攻擊特效設定板（v2 麻糬萌風）SVG。"""
import math

INK = "#6B4C52"
W, H = 1200, 1760
out = []
add = out.append

def mochi_cat(x, y, s=1.0, tiger=False, roar=False):
    fur = "#FFB25B" if not tiger else "#FF9A2E"
    stripe = "#C9682A" if not tiger else "#3A2A24"
    g = [f'<g transform="translate({x} {y}) scale({s})" stroke="{INK}" stroke-linecap="round" stroke-linejoin="round">']
    # 耳朵
    for sx in (-1, 1):
        g.append(f'<path d="M{sx*30},-38 Q{sx*44},-78 {sx*58},-34 Z" fill="{fur}" stroke-width="5"/>')
        g.append(f'<path d="M{sx*36},-42 Q{sx*44},-64 {sx*51},-40 Z" fill="#FFCADA" stroke="none"/>')
    # 麻糬身體
    g.append(f'<path d="M0,-50 C52,-50 74,-20 75,14 C76,52 44,66 0,66 C-44,66 -76,52 -75,14 C-74,-20 -52,-50 0,-50 Z" fill="{fur}" stroke-width="5"/>')
    g.append('<path d="M-60,30 C-40,60 40,60 60,30 C40,50 -40,50 -60,30 Z" fill="#E08A3C" opacity="0.35" stroke="none"/>')
    for k in (-1, 0, 1):
        g.append(f'<path d="M{k*12-4},-48 L{k*12+4},-48 L{k*12},{-34+abs(k)*4} Z" fill="{stripe}" stroke="none"/>')
    for sx in (-1, 1):
        g.append(f'<path d="M{sx*74},0 L{sx*60},4 M{sx*74},14 L{sx*61},15" stroke="{stripe}" stroke-width="5" fill="none"/>')
    if tiger:
        g.append('<ellipse cx="0" cy="22" rx="30" ry="18" fill="#FFF6E8" stroke="none"/>')
    # 眼睛
    if roar:
        for sx in (-1, 1):
            g.append(f'<path d="M{sx*30},-8 L{sx*12},-2" stroke-width="6" fill="none"/>')
            g.append(f'<ellipse cx="{sx*21}" cy="6" rx="7" ry="8" fill="#2E2128" stroke="none"/>')
            g.append(f'<circle cx="{sx*21+2}" cy="3" r="2.5" fill="#fff" stroke="none"/>')
        g.append('<path d="M-16,22 Q0,12 16,22 Q12,44 0,44 Q-12,44 -16,22 Z" fill="#7A2340" stroke-width="4"/>')
        g.append('<path d="M-10,36 Q0,30 10,36 Q8,43 0,43 Q-8,43 -10,36 Z" fill="#FF8FB1" stroke="none"/>')
        g.append('<path d="M-12,22 L-8,30 L-4,22 M4,22 L8,30 L12,22" fill="#fff" stroke-width="2"/>')
    else:
        for sx in (-1, 1):
            g.append(f'<ellipse cx="{sx*21}" cy="6" rx="7" ry="9" fill="#2E2128" stroke="none"/>')
            g.append(f'<circle cx="{sx*21+3}" cy="2" r="3" fill="#fff" stroke="none"/>')
        g.append('<path d="M-8,18 Q-4,24 0,19 Q4,24 8,18" fill="none" stroke-width="3.5"/>')
    g.append('<ellipse cx="0" cy="14" rx="4" ry="3" fill="#FF9DB8" stroke-width="2"/>')
    for sx in (-1, 1):
        g.append(f'<ellipse cx="{sx*40}" cy="22" rx="12" ry="7" fill="#FF8FB1" opacity="0.55" stroke="none"/>')
    g.append('</g>')
    return "".join(g)

def mochi_mouse(x, y, s=1.0, sx=1.0, sy=1.0, crying=False, ko=False):
    g = [f'<g transform="translate({x} {y}) scale({s*sx} {s*sy})" stroke="{INK}" stroke-linecap="round" stroke-linejoin="round">']
    for e in (-1, 1):
        g.append(f'<circle cx="{e*34}" cy="-40" r="24" fill="#F4EFFA" stroke-width="5"/>')
        g.append(f'<circle cx="{e*34}" cy="-38" r="14" fill="#FFCADA" stroke="none"/>')
    g.append('<path d="M0,-46 C44,-46 62,-20 63,10 C64,42 38,54 0,54 C-38,54 -64,42 -63,10 C-62,-20 -44,-46 0,-46 Z" fill="#F4EFFA" stroke-width="5"/>')
    if ko:
        for e in (-1, 1):
            g.append(f'<path d="M{e*18-7},-2 l14,12 M{e*18+7},-2 l-14,12" stroke-width="4" fill="none"/>')
    else:
        for e in (-1, 1):
            g.append(f'<ellipse cx="{e*18}" cy="4" rx="6" ry="8" fill="#2E2128" stroke="none"/><circle cx="{e*18+2}" cy="1" r="2.5" fill="#fff" stroke="none"/>')
        if crying:
            g.append('<path d="M-26,12 Q-18,18 -10,12 Q-11,20 -18,20 Q-25,20 -26,12 Z" fill="#BDE8FF" stroke="#7FC4E8" stroke-width="2"/>')
    g.append('<path d="M-8,20 Q-4,16 0,20 Q4,24 8,20" fill="none" stroke-width="3"/>')
    g.append('<ellipse cx="0" cy="14" rx="4" ry="3" fill="#FF9DB8" stroke-width="2"/>')
    for e in (-1, 1):
        g.append(f'<ellipse cx="{e*34}" cy="18" rx="10" ry="6" fill="#FF8FB1" opacity="0.55" stroke="none"/>')
    g.append('</g>')
    return "".join(g)

def star(x, y, r, fill="#FFE07A", rot=0, stroke=INK):
    pts = []
    for i in range(10):
        rr = r if i % 2 == 0 else r * 0.45
        a = -math.pi / 2 + i * math.pi / 5
        pts.append(f"{x + math.cos(a) * rr:.1f},{y + math.sin(a) * rr:.1f}")
    return f'<polygon points="{" ".join(pts)}" fill="{fill}" stroke="{stroke}" stroke-width="3" stroke-linejoin="round" transform="rotate({rot} {x} {y})"/>'

def candy(x, y, color, rot=0, s=1.0):
    return (f'<g transform="translate({x} {y}) rotate({rot}) scale({s})" stroke="{INK}" stroke-width="3" stroke-linejoin="round">'
            f'<path d="M-14,0 L-28,-10 L-26,10 Z" fill="{color}"/><path d="M14,0 L28,-10 L26,10 Z" fill="{color}"/>'
            f'<ellipse cx="0" cy="0" rx="16" ry="12" fill="{color}"/><path d="M-6,-8 Q0,0 -6,8" stroke="#fff" fill="none" stroke-width="3"/></g>')

def paw_shot(x, y, s=1.0, color="#FF9A3C"):
    t = [f'<g transform="translate({x} {y}) scale({s})">',
         f'<path d="M-14,10 Q0,110 14,10 Z" fill="url(#trailOrange)"/>',
         f'<circle r="34" fill="url(#glowOrange)"/>',
         f'<g stroke="#fff" stroke-width="7" stroke-linejoin="round">',
         f'<ellipse cx="0" cy="6" rx="15" ry="12" fill="{color}"/>',
         ''.join(f'<circle cx="{dx}" cy="{dy}" r="6" fill="{color}"/>' for dx, dy in ((-14, -8), (-5, -16), (5, -16), (14, -8))),
         '</g>',
         f'<g fill="{color}"><ellipse cx="0" cy="6" rx="15" ry="12"/>',
         ''.join(f'<circle cx="{dx}" cy="{dy}" r="6"/>' for dx, dy in ((-14, -8), (-5, -16), (5, -16), (14, -8))),
         '</g><ellipse cx="-4" cy="3" rx="5" ry="3" fill="#FFF3D6"/></g>']
    return "".join(t)

def enemy_bullet(x, y, r=12):
    return (f'<circle cx="{x}" cy="{y}" r="{r*2.1}" fill="url(#glowPink)"/>'
            f'<circle cx="{x}" cy="{y}" r="{r+3}" fill="#5B1E46"/>'
            f'<circle cx="{x}" cy="{y}" r="{r}" fill="#FF4FA0"/>'
            f'<circle cx="{x-r*0.15}" cy="{y-r*0.15}" r="{r*0.48}" fill="#fff"/>')

def sfx(x, y, text, size, color, rot=0):
    return (f'<text x="{x}" y="{y}" font-family="WenQuanYi Zen Hei" font-weight="bold" font-size="{size}" fill="{color}" '
            f'stroke="#fff" stroke-width="{size*0.16:.1f}" paint-order="stroke" stroke-linejoin="round" transform="rotate({rot} {x} {y})">{text}</text>')

def panel(px, py, title, sub, body):
    add(f'<g transform="translate({px} {py})">')
    add('<clipPath id="pc{0}"><rect width="540" height="420" rx="26"/></clipPath>'.format(px + py))
    add(f'<g clip-path="url(#pc{px + py})">')
    add('<rect width="540" height="420" fill="url(#panelBg)"/>')
    add(body)
    add('</g>')
    add(f'<rect width="540" height="420" rx="26" fill="none" stroke="{INK}" stroke-width="5"/>')
    add(f'<rect x="16" y="16" width="{24 + len(title) * 30}" height="46" rx="23" fill="#fff" stroke="{INK}" stroke-width="3"/>')
    add(f'<text x="30" y="49" font-family="WenQuanYi Zen Hei" font-weight="bold" font-size="26" fill="{INK}">{title}</text>')
    add('<rect x="0" y="376" width="540" height="44" fill="#FFFFFF" opacity="0.88"/>')
    add(f'<text x="22" y="405" font-family="WenQuanYi Zen Hei" font-size="20" fill="#7D6470">{sub}</text>')
    add('</g>')

# ───── 開頭 ─────
add(f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">')
add('''<defs>
<radialGradient id="bg" cx="50%" cy="40%" r="80%"><stop offset="0" stop-color="#FFFBF2"/><stop offset="1" stop-color="#F6DCE6"/></radialGradient>
<linearGradient id="panelBg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#EAF3FF"/><stop offset="1" stop-color="#FDEFF5"/></linearGradient>
<radialGradient id="glowOrange"><stop offset="0" stop-color="#FFE7A8" stop-opacity="0.95"/><stop offset="1" stop-color="#FF9A3C" stop-opacity="0"/></radialGradient>
<linearGradient id="trailOrange" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FFB45C" stop-opacity="0.9"/><stop offset="1" stop-color="#FFB45C" stop-opacity="0"/></linearGradient>
<radialGradient id="glowPink"><stop offset="0" stop-color="#FF8CC6" stop-opacity="0.7"/><stop offset="1" stop-color="#FF8CC6" stop-opacity="0"/></radialGradient>
<radialGradient id="flash"><stop offset="0" stop-color="#fff"/><stop offset="0.6" stop-color="#FFF6C8" stop-opacity="0.8"/><stop offset="1" stop-color="#FFF6C8" stop-opacity="0"/></radialGradient>
<linearGradient id="rainbow" x1="0" y1="0" x2="1" y2="0">
 <stop offset="0" stop-color="#B5E3FF" stop-opacity="0"/><stop offset="0.12" stop-color="#B5E3FF"/><stop offset="0.25" stop-color="#C9B8FF"/>
 <stop offset="0.38" stop-color="#FFC2E1"/><stop offset="0.5" stop-color="#FFFFFF"/><stop offset="0.62" stop-color="#FFE7A8"/>
 <stop offset="0.75" stop-color="#C2F5D9"/><stop offset="0.88" stop-color="#B5E3FF"/><stop offset="1" stop-color="#B5E3FF" stop-opacity="0"/></linearGradient>
<linearGradient id="cutin" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#FF9A2E"/><stop offset="0.5" stop-color="#FFD66B"/><stop offset="1" stop-color="#FF9A2E"/></linearGradient>
<filter id="paper"><feTurbulence type="fractalNoise" baseFrequency="0.9" numOctaves="3" seed="7"/><feColorMatrix type="matrix" values="0 0 0 0 0.45 0 0 0 0 0.35 0 0 0 0 0.35 0 0 0 0.06 0"/></filter>
<filter id="blur"><feGaussianBlur stdDeviation="6"/></filter>
</defs>''')
add(f'<rect width="{W}" height="{H}" fill="url(#bg)"/>')
add(f'<text x="60" y="92" font-family="WenQuanYi Zen Hei" font-weight="bold" font-size="52" fill="{INK}">攻擊特效設定板</text>')
add(f'<text x="64" y="140" font-family="WenQuanYi Zen Hei" font-size="26" fill="#B48A95">萌系爆裂魔法 ─ 喵星萌王爭霸戰（v2 麻糬萌風）</text>')
add(star(1080, 90, 30) + star(1010, 130, 16, "#FFCADA"))

# ───── 1. 肉球砲彈幕 ─────
b = []
for i, a in enumerate(range(-30, 31, 15)):
    for k in range(2):
        d = 110 + k * 95
        x = 270 + math.sin(math.radians(a)) * d
        y = 300 - math.cos(math.radians(a)) * d
        b.append(f'<g transform="rotate({a} {x:.0f} {y:.0f})">{paw_shot(round(x), round(y), 0.8 - k * 0.05)}</g>')
b.append('<circle cx="270" cy="255" r="50" fill="url(#flash)"/>')
b.append(mochi_cat(270, 310, 0.8))
b.append(sfx(380, 330, "ドドド！", 40, "#FF7A3C", -8))
panel(40, 190, "① 主武器：肉球砲彈幕", "白色貼紙描邊＋光暈＋長拖尾，速度快、像糖果一樣亮", "".join(b))

# ───── 2. 命中 ─────
b = ['<circle cx="270" cy="210" r="120" fill="url(#flash)"/>']
b.append(mochi_mouse(270, 220, 1.4, 1.22, 0.8, crying=True))
for (x, y, r, rot) in ((150, 140, 18, 10), (390, 130, 22, -15), (420, 260, 14, 20), (130, 280, 12, 0), (300, 90, 12, 30)):
    b.append(star(x, y, r, rot=rot))
b.append(paw_shot(270, 330, 0.7))
b.append(sfx(330, 120, "ポコッ！", 52, "#FF5C93", 10))
b.append('<g stroke="#6B4C52" stroke-width="4" fill="none" stroke-linecap="round"><path d="M150,200 l-30,-6 M150,230 l-34,4 M390,200 l30,-6 M390,230 l34,4"/></g>')
panel(620, 190, "② 命中：果凍回彈", "敵人擠壓回彈＋星星＋漫畫音效字，重擊加 60ms 停頓", "".join(b))

# ───── 3. 擊破 ─────
b = ['<circle cx="270" cy="220" r="150" fill="url(#flash)"/>',
     '<circle cx="270" cy="220" r="118" fill="none" stroke="#FFE07A" stroke-width="12"/>',
     '<circle cx="270" cy="220" r="150" fill="none" stroke="#fff" stroke-width="5" opacity="0.8"/>']
import random
random.seed(4)
colors = ["#FF8FB1", "#8FD3FF", "#FFE07A", "#B9A7FF", "#9BE8C4"]
for i in range(26):
    a = random.random() * math.tau
    d = 60 + random.random() * 170
    x, y = 270 + math.cos(a) * d, 220 + math.sin(a) * d * 0.8
    b.append(f'<rect x="{x:.0f}" y="{y:.0f}" width="16" height="9" rx="2" fill="{random.choice(colors)}" transform="rotate({random.randint(0, 180)} {x:.0f} {y:.0f})"/>')
for i, (x, y) in enumerate(((150, 160), (400, 180), (190, 320), (360, 320), (270, 100))):
    b.append(candy(x, y, colors[i], rot=i * 35))
b.append(mochi_mouse(270, 230, 0.9, ko=True))
b.append(sfx(330, 150, "+500", 54, "#FFB300", -6))
b.append(sfx(40, 350, "ぱぁん！", 40, "#FF5C93", -8))
panel(40, 640, "③ 擊破：彩紙糖果爆開", "彩紙＋糖果四散、分數彈跳放大；大型擊破震動＋閃白", "".join(b))

# ───── 4. 消彈變糖果 ─────
b = []
for i in range(9):
    x, y = 80 + (i % 3) * 70, 120 + (i // 3) * 70
    b.append(enemy_bullet(x, y, 12))
b.append(f'<path d="M260,190 L320,190" stroke="{INK}" stroke-width="6"/><path d="M320,175 L345,190 L320,205 Z" fill="{INK}"/>')
for i, (x, y) in enumerate(((390, 120), (450, 165), (400, 220), (470, 260), (380, 290), (440, 330))):
    b.append(candy(x, y, colors[i % 5], rot=i * 40, s=0.9))
b.append('<path d="M390,120 Q300,250 210,300 M470,260 Q330,300 230,320" stroke="#FFB3CF" stroke-width="5" fill="none" stroke-dasharray="6 10"/>')
b.append(mochi_cat(170, 320, 0.7))
b.append(sfx(390, 100, "SWEET!", 36, "#FF7AB0", 6))
b.append(sfx(250, 300, "+10", 26, "#FFB300", 0) + sfx(300, 250, "+10", 26, "#FFB300", 0))
panel(620, 640, "④ 消彈：子彈變糖果", "敵彈統一糖果粉＋深梅描邊；消彈時變糖果飛向貓咪加分", "".join(b))

# ───── 5. 彩虹棉花糖光束 ─────
b = ['<rect x="200" y="175" width="140" height="125" fill="url(#rainbow)" opacity="0.55" filter="url(#blur)"/>',
     '<rect x="225" y="175" width="90" height="125" fill="url(#rainbow)"/>',
     '<rect x="258" y="175" width="24" height="125" fill="#fff"/>']
for k in range(3):
    yy = 205 + k * 36
    b.append(f'<ellipse cx="270" cy="{yy}" rx="56" ry="9" fill="none" stroke="#fff" stroke-width="4" opacity="0.8"/>')
for (x, y, r) in ((200, 230, 9), (340, 260, 7), (190, 290, 6), (350, 200, 8)):
    b.append(f'<circle cx="{x}" cy="{y}" r="{r}" fill="#fff" stroke="#C9B8FF" stroke-width="3"/>')
# 命中處的花朵爆光
for k in range(8):
    a = k * math.pi / 4
    b.append(f'<ellipse cx="{270 + math.cos(a) * 34:.0f}" cy="{175 + math.sin(a) * 34:.0f}" rx="24" ry="14" fill="#FFE3F1" stroke="#FF8FB1" stroke-width="3" transform="rotate({k * 45} {270 + math.cos(a) * 34:.0f} {175 + math.sin(a) * 34:.0f})"/>')
b.append(mochi_mouse(270, 118, 0.8, 1.15, 0.85, crying=True))
b.append('<circle cx="270" cy="175" r="26" fill="#FFF6C8" stroke="#FFC94A" stroke-width="4"/>')
b.append(mochi_cat(270, 325, 0.62))
b.append(sfx(345, 300, "ビーーム！", 32, "#8C7BFF", -6))
panel(40, 1090, "⑤ 集中：彩虹棉花糖光束", "白色核心＋粉彩彩虹邊＋能量環與泡泡，命中處開花", "".join(b))

# ───── 6. 大絕切入 ─────
b = ['<rect width="540" height="420" fill="#2A1A34"/>']
for k in range(28):
    a = k * math.tau / 28
    b.append(f'<line x1="{270 + math.cos(a) * 90:.0f}" y1="{210 + math.sin(a) * 90:.0f}" x2="{270 + math.cos(a) * 520:.0f}" y2="{210 + math.sin(a) * 520:.0f}" stroke="#FFE7A8" stroke-width="{6 if k % 3 == 0 else 2}" opacity="0.6"/>')
b.append('<g transform="rotate(-8 270 210)"><rect x="-60" y="120" width="660" height="180" fill="url(#cutin)"/>'
         '<rect x="-60" y="112" width="660" height="8" fill="#fff"/><rect x="-60" y="300" width="660" height="8" fill="#fff"/></g>')
b.append(mochi_cat(250, 215, 1.35, tiger=True, roar=True))
b.append(sfx(330, 175, "吼～！", 64, "#FF5A1F", -8))
b.append(sfx(320, 350, "猛虎咆哮", 34, "#FFB300", -8))
panel(620, 1090, "⑥ 大絕：漫畫分格切入", "麻糬老虎咆哮＋集中線，衝擊波把子彈全變糖果", "".join(b))

# ───── 原則 ─────
add('<g font-family="WenQuanYi Zen Hei">')
add(f'<rect x="40" y="1540" width="1120" height="190" rx="26" fill="#fff" opacity="0.9" stroke="#E7C3D2" stroke-width="4"/>')
add(f'<text x="70" y="1590" font-size="28" font-weight="bold" fill="{INK}">設計原則（參考 CAVE 系彈幕、Cotton Reboot!、遊戲手感研究）</text>')
lines = ["‧ 速度帶來力道：玩家子彈要快、拖尾要長；每次命中都有回饋",
         "‧ 敵彈統一一種顏色家族並會脈動，再亂也看得清楚",
         "‧ 獎勵要看得見：分數彈跳、COMBO 計數放大、消彈變糖果",
         "‧ 特效分級：小命中只閃一下，大事件才停頓＋震動＋閃白"]
for i, line in enumerate(lines):
    add(f'<text x="70" y="{1630 + i * 28}" font-size="22" fill="#7D6470">{line}</text>')
add('</g>')
add(f'<rect width="{W}" height="{H}" filter="url(#paper)"/>')
add('</svg>')

open('fx_board.svg', 'w').write("\n".join(out))
print("ok")
