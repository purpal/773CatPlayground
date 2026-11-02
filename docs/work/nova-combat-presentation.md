# Nova combat presentation and encounter pacing

Changes apply to game 3. Original game 2 keeps its artwork button, cinematics, enemy scripts and music.

## Overdrive

The existing cat-head mouth-cannon silhouettes sit in an angular metal housing with cyan edge lights and segmented charge cells. Touch target is 76 dp. Empty charges disable the control. The awakening emblem grows with an eased expansion, stays below 48% opacity and fades fully. The former dark full-screen overlay and closing color flash were removed from the Nova cinematic; the underlying damage/blast timing is unchanged.

## Sloth and mouse

Sloth emits 15 rounds every 3 seconds instead of 20 every 2 seconds: half as many rounds per second. Horizontal spacing is approximately 66.7 world units; actual collision radii still prevent even black cat from slipping between adjacent rounds. Rows remain slow and persist through transformation. Shields still carve space.

Mouse sends two wings of 48 pursuers. They continuously re-aim at the player, first forming ahead of the ship and then closing in; moving once no longer invalidates the wave. Turning is limited to 1.5 radians/second, approach speed reduces within 450 units, and each drone expires after 14 seconds. Base HP is 65, scaled by the existing difficulty. This gives piercing fire time to clear several ranks without making spread sufficient against the entire formation. Transform/death cleanup and the separate locked rail lance remain.

## Authored wave schedules

Each stage has its own opening delay, count, spacing and location, followed by differently selected mid-stage formations. All retain two native elites, weapon/repair/overdrive supplies and their existing encounter lessons. Scripts last 70–76 seconds before normal transition/cleanup rules.

| Stage | Opening |
| --- | --- |
| Mouse | Fast paired pincer |
| Dog | Left interception line |
| Bear | Wide heavy wedge |
| Capybara | Left-weighted staggered columns |
| Rabbit | Rear overtaking along the left lane |
| Penguin | Fast right convoy |
| Koala | Sparse right-hand weaving patrol |
| Otter | Widely spaced paired pincer |
| Panda | Right-weighted columns |
| Sloth | Slow left-biased wedge |
| Raccoon | Rear overtaking along the right lane |
| Monkey | Dense left-hand weaving patrol |
| Pig | Slower right-side guard line |

Rear arrivals are harmless during ascent, pass ahead, visibly rotate, wait before firing and descend. They cannot spawn a collision or fire from underneath the player. The renderer handles Nova raiders before original enemy-specific casts.

## Music

Two dedicated, original synthesized metal loops: a 116 BPM stage groove and a 172 BPM boss riff. Distorted harmonic guitar/power chords, bass, snare, cymbals and double-kick patterns replace Nova's old melodies. Boss warning switches to the intense loop, including boss retries. Music remains on the existing background render/playback path and respects the existing mute setting. Distortion DC is removed; loop edges are ramped and normalized with headroom.

## Validation

Model tests cover all thirteen distinct opening signatures, supply/elite retention, rear-entry safety on three viewport heights, continuous mouse retargeting and lifetime, exact 48/48 wing split, actual sloth-row collision spacing, translucent overdrive alpha, music switching, DC/headroom/loop edges, and the existing full-level stationary/moving boss matrix. Counterplay is a deterministic model policy, not a replacement for human playtesting. No emulator/device run was performed, as requested.

The 114 full-level cases and four targeted counterplay cases are recorded in the adjacent `nova-combat-presentation-balance.csv` and `nova-combat-presentation-counterplay.csv`. Both cats clear mouse with piercing/shield plus rail-line dodging (~100 seconds) and sloth with shield-cleared space and height changes (~117 seconds), without invulnerability, pickups or overdrive. Stationary spread/homing and stationary black special/shield still fail these encounters. Older balance reports are historical snapshots.

The row-spacing regression also catches a floating-point boundary double-fire at the initial volley. Sloth now uses an explicit next-volley deadline so each scheduled wave fires once.

Final verification: `testDebugUnitTest assembleDebug assembleRelease` passed in 4m 14s (144 tests, zero failures/errors/skips). No emulator/device playtest.
