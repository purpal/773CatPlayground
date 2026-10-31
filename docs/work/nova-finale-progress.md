# Remaining Nova work

Branch: feat/nova-final-sectors. Not merged or published yet.

Implemented: Nova route LazyRow initializes and scrolls to cleared/current opponent, clamped to last item for completed tours. Nova-only World.novaBalance flag supplied by ShooterGameScreen reduces homing damage 16→12.8 and increases all sonic wall HP by 20%; original game defaults unchanged. Two new tests verify all levels and isolation. 123 tests and assembleDebug passed; log /workspace/.setup/nova-final-sectors.log.

Generated preliminary boss art: docs/art/nova-finale/boss-concepts.png (OpenAI image generation, source exec-bf97058b-bb72-47b5-82a1-4c5617bdb24c.png). NOT integrated. Needs transparent gutter correction before any 3x2 slicing: objects crowd/cross middle horizontal boundary. Rows: pig tracked drill / unfolding / six-leg platform; mouse fortress / split train / awakened core.

Remaining: implement NovaPigBoss (45% transform; telegraphed player-position area blasts with bounded persistent areas and escape lanes) and NovaMouseBoss (60% split fortress; 30% ancient core awakening; destructible gate components and fixed safe routes). Follow docs/work/nova-boss-diversity-plan.md sections 13 and 1. Generate backgrounds, enemies, effects, pilots; finish artwork manifest, rendering, sector routing, outro and completed tour list. Preserve original shooter. Add mechanics tests, run full unit/debug/release checks, merge main and push to trigger release per existing authorization. No emulator/device testing.
