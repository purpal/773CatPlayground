// 把 build/svg/*.svg 依 manifest.json 渲染成透明背景的 PNG（同一個瀏覽器，速度快）。
// 用法：node render.mjs <svg 目錄> <輸出目錄>
import { createRequire } from "node:module";
import fs from "node:fs";
import path from "node:path";

// ESM 的 import 不看 NODE_PATH，改用 require 才能找到全域安裝的 playwright。
const require = createRequire(path.join(process.env.NODE_PATH || process.cwd(), "noop.js"));
const { chromium } = require("playwright");

const [svgDir, outDir] = process.argv.slice(2);
const manifest = JSON.parse(fs.readFileSync(path.join(svgDir, "manifest.json"), "utf8"));
fs.mkdirSync(outDir, { recursive: true });

const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH || undefined });
const page = await browser.newPage();
for (const item of manifest) {
  const svg = fs.readFileSync(path.join(svgDir, `${item.name}.svg`), "utf8");
  await page.setViewportSize({ width: item.width, height: item.height });
  await page.setContent(`<html><body style="margin:0;background:transparent">${svg}</body></html>`);
  await page.screenshot({ path: path.join(outDir, `${item.name}.png`), omitBackground: true, clip: { x: 0, y: 0, width: item.width, height: item.height } });
}
await browser.close();
console.log(`rendered ${manifest.length} sprites`);
