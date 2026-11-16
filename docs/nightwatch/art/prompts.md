# Image prompts and provenance

Route: built-in `image_gen`, `art_source: host_image`, transparent background requested. Model identifier and monetary estimate were not returned. No API credentials were configured or added. Original style references: the user's five uploaded cat-warrior portraits; identity direction: the white-haired Zeus cat created in this chat.

## Initial cat atlas direction

Landscape 3:2 sprite atlas, exactly six individual full-body chibi cat gods in a rigid 3-column, 2-row grid. Huge angular cat heads, 2.5-head-tall bodies, pointed furry cheeks, triangular ears, sly narrow feline eyes, rough variable-width black ink outlines, saturated flat colors, hard two-tone cel shadows. No 3D, realism, text, labels, scenery or pedestal. Reading order: Zeus (white mane/beard, gold laurel, white toga, purple cape, lightning); Poseidon (blue tabby, turquoise Greek robes, seaweed crown, trident, fishlike tail); Nezha (orange tabby, twin buns, pink ribbons, red-gold Chinese armor, fire wheels, spear); Athena (gray tabby, green eyes, gold/red Greek helmet, owl shield, ivory-gold armor); Wind (tuxedo cat, green scarf/robes, cream cloud shapes, fan); Earth (chubby brown tabby, white brows, golden hat, ochre Chinese robes, jade staff, ingot). Transparent background, weapons wholly inside cells.

## Final cats-source edit prompt

Edit this SIX cat sprite atlas. Preserve the six character designs and hand-drawn style exactly. ONLY fix layout: shrink EACH character to 75 percent of its current size around the center of its own cell. Rigid 3 columns by 2 rows, each cell equally sized. Entire ears, hair, helmets, weapons, lightning, ribbons, feet, tails must stay WELL INSIDE its cell with a generous transparent gutter above below left right. No parts of any sprite near cell boundaries. No stray fragments from neighboring sprites. Full clean silhouette per cell. Keep reading order Zeus Poseidon Nezha / Athena Wind Earth. Transparent background everywhere outside character, no glow or colored background or text.

## Initial foe atlas direction

3x3 atlas of nine full-body chibi yokai with bold angular black ink outlines, saturated flat colors and hard cel shadows, matching the cat atlas. No text, grid, scenery or overlap, transparent background. Reading order: hungry purple ghost rat; one-eyed red paper umbrella yokai with tongue and sandal foot; heavy jade stone guardian lion; tanuki thief with green scarf and sack; blue clock ghost with brass bell; turquoise water goblin with fin ears; wide-mouthed glutton in ragged purple raincoat; black-red crow tengu with long nose, fan and wind; indigo eastern dragon with golden antlers, cyan mane and curled tail.

## Final foes-source edit prompt

Edit this NINE yokai enemy sprite atlas. Preserve the nine designs and bold flat cel shaded black-outline hand-drawn style. ONLY fix layout. EXACT rigid 3 columns by 3 rows, equally sized cells. Shrink EACH monster to 70% of original size centered in its cell. Keep full body, every ear tongue hat horn foot fan tail. Leave a HUGE transparent gutter all around EACH monster, especially at row boundaries: at least 15% blank cell margin each side. Zero overlap or stray parts from adjacent rows. Reading order hungry ghost rat / paper umbrella / jade stone lion; tanuki thief / blue clock ghost / water goblin; raincoat glutton boss / black-red crow tengu boss / indigo gold-horn dragon boss. Characters alone, NO glow or colored background or scenery or text. True transparent background.

## Final processing

Tool: Agent Sprite Forge `generate2dsprite.py process`, v0.6.0, source commit 25203d03900b.

Common flags: `--target asset --mode sheet --background-mode native_alpha --min-component-area 100 --cell-size 384 --align center --fit-scale .88 --strict-qc`.

Cats: `--rows 2 --cols 3`; foes: `--rows 3 --cols 3 --grid-rounding nearest`. All six cats and nine foes are native-alpha PNGs. No animation generated; runtime motion and effects are independent Canvas rendering. The corresponding pipeline manifests record hashes and all quality checks.
