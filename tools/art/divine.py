"""喵神封魔錄的所有美術（名稱一律以 dv_ 開頭）：

    dv_unit_<id>  場上的神靈          dv_foe_<id>   妖魔
    dv_card_<id>  卡面的圖（400×300）  dv_field      戰場背景
    dv_incense / dv_coin / dv_back / dv_pack / dv_star_0|1 / lobby_divine  介面
"""
from divine_kit import unit_sprite, Defs, move
from common import Sprite
import divine_gods
import divine_foes
import divine_items as items


def all_sprites():
    out = []
    for cid, fn in divine_gods.ALL.items():
        out.append(unit_sprite(f"dv_unit_{cid}", fn))
        backdrop = items.backdrop_east if cid in divine_gods.EAST else items.backdrop_greek
        out.append(items.card_art(f"dv_card_{cid}", backdrop, fn, 0.98, 6))
    for cid, fn in items.ICONS.items():
        accent = items.ACCENT[cid]
        if cid in ("bagua", "ziqi", "labyrinth", "sanctuary"):
            backdrop = lambda d, a=accent: items.backdrop_enchant(d, a)
        elif cid in ("jingubang", "fenghuolun", "trident", "aegis"):
            backdrop = lambda d, a=accent: items.backdrop_equip(d, a)
        else:
            backdrop = lambda d, a=accent: items.backdrop_spell(d, a)
        out.append(items.card_art(f"dv_card_{cid}", backdrop, fn, 1.0, 0))
    for eid, fn in divine_foes.ALL.items():
        if eid == "bat":
            out.append(unit_sprite("dv_foe_bat_0", lambda d: divine_foes.bat(d, 0)))
            out.append(unit_sprite("dv_foe_bat_1", lambda d: divine_foes.bat(d, 1)))
        else:
            out.append(unit_sprite(f"dv_foe_{eid}", fn, 420 if eid == "ninetail" else 300))
    square = "-140 -160 280 280"
    out.append(items.simple("dv_incense", items.incense, 160, 160, "-130 -165 260 280"))
    out.append(items.simple("dv_coin", items.coin, 128, 128, "-125 -125 250 250"))
    out.append(items.simple("dv_back", items.card_back, 300, 420, "-155 -215 310 430"))
    out.append(items.simple("dv_pack", items.pack, 400, 400, "-200 -200 400 400"))
    out.append(items.simple("dv_star_1", lambda d: items.star_icon(d, True), 96, 96, "-115 -115 230 230"))
    out.append(items.simple("dv_star_0", lambda d: items.star_icon(d, False), 96, 96, "-115 -115 230 230"))
    out.append(items.simple("lobby_divine", items.lobby_icon, 320, 320, square))
    out.append(items.simple("dv_field", items.field, 1000, 1400, "0 0 1000 1400"))
    return out
