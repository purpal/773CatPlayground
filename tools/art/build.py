"""產生所有遊戲美術：寫出 SVG，再用 render.mjs 渲染成 PNG 放到 app/src/main/assets/art/。

用法（在 repo 根目錄）：
    python3 tools/art/build.py            # 全部
    python3 tools/art/build.py cat_ run_  # 只輸出名稱以這些字首開頭的圖
另外會輸出 build/art/preview.png 方便一次檢視。
"""
import json
import os
import shutil
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))

import cats  # noqa: E402

MODULES = [cats]
for name in ("bosses", "minions", "items", "effects", "ultimate", "scifi", "capybara", "icons"):
    try:
        MODULES.append(__import__(name))
    except ModuleNotFoundError:
        pass


def main():
    prefixes = sys.argv[1:]
    sprites = [s for m in MODULES for s in m.all_sprites()]
    if prefixes:
        sprites = [s for s in sprites if any(s.name.startswith(p) for p in prefixes)]
    svg_dir = os.path.join(ROOT, "build", "art", "svg")
    os.makedirs(svg_dir, exist_ok=True)
    for f in os.listdir(svg_dir):
        os.remove(os.path.join(svg_dir, f))
    for s in sprites:
        with open(os.path.join(svg_dir, f"{s.name}.svg"), "w") as fh:
            fh.write(s.svg())
    with open(os.path.join(svg_dir, "manifest.json"), "w") as fh:
        json.dump([{"name": s.name, "width": s.width, "height": s.height} for s in sprites], fh)
    out_dir = os.path.join(ROOT, "app", "src", "main", "assets", "art")
    node_modules = os.environ.get("NODE_PATH", "/opt/node-tools/node_modules")
    env = dict(os.environ, NODE_PATH=node_modules)
    subprocess.run(["node", os.path.join(HERE, "render.mjs"), svg_dir, out_dir], check=True, env=env)
    preview(out_dir, [s.name for s in sprites])
    install_app_icon(out_dir)


# 自適應圖示前景（108dp）在各密度的像素大小
ICON_SIZES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}


def install_app_icon(out_dir):
    """app_icon_fg 放到 res/mipmap-*，預覽圖不打包進 assets。"""
    from PIL import Image
    src = os.path.join(out_dir, "app_icon_fg.png")
    if os.path.exists(src):
        im = Image.open(src).convert("RGBA")
        for density, px in ICON_SIZES.items():
            d = os.path.join(ROOT, "app", "src", "main", "res", f"mipmap-{density}")
            os.makedirs(d, exist_ok=True)
            im.resize((px, px), Image.LANCZOS).save(os.path.join(d, "ic_launcher_foreground.png"))
        shutil.copy(src, os.path.join(ROOT, "build", "art", "app_icon_fg.png"))
        os.remove(src)
    for f in os.listdir(out_dir):
        if f.startswith("preview_"):
            shutil.move(os.path.join(out_dir, f), os.path.join(ROOT, "build", "art", f))


def preview(out_dir, names):
    try:
        from PIL import Image
    except ImportError:
        return
    tiles = []
    for n in names:
        im = Image.open(os.path.join(out_dir, f"{n}.png")).convert("RGBA")
        im.thumbnail((160, 160))
        tiles.append(im)
    cols = 8
    rows = (len(tiles) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * 170, rows * 170), (253, 239, 245, 255))
    for i, im in enumerate(tiles):
        x, y = (i % cols) * 170 + (170 - im.width) // 2, (i // cols) * 170 + (170 - im.height) // 2
        sheet.alpha_composite(im, (x, y))
    sheet.save(os.path.join(ROOT, "build", "art", "preview.png"))


if __name__ == "__main__":
    main()
