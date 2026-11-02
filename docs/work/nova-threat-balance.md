# Nova threat and full-level balance

Scope: game 3 only. No emulator/device testing. Original shooter defaults remain unchanged.

## Tour and presentation

New tours shuffle the nine ordinary opponents, then use otter / sloth / capybara, then mouse. Legacy saves preserve cleared/revealed destinations and migrate the unseen suffix once; kings already revealed remain in place. Dialogue no longer promises a particular next ordinary opponent.

Enemy attack telegraphs default on and persist in Nova settings. Turning them off hides warning rays, landing/path markers and guide lines, not active attacks, physical enemies or their wind-up delays. Original shooter entry restores telegraphs on.

## Encounters

Sloth emits 20 staggered rounds every 2 seconds at 26 world units/second, independent of tour bullet-speed scaling. Its transformed phase adds horizontal 22-unit/second occupation rounds. Existing rounds survive transformation; victory still clears the screen. Slow rounds consume one quarter of normal force-wall durability, making shield-cleared space useful without making the player invulnerable. Beacon enemies introduce the same slow rounds.

Mouse sends 96 drones in six rows toward positions locked at spawn. They have no random coin drops or score reward. The center paw penetrates eight enemies; the beam damages every aligned target, deduplicating multipart shared health. A separately locked rail lance forces relocation even after the swarm is cleared. Transform/death cancels owned drones and gate parts.

## Numerical changes

- Spread: damage ×0.85, interval 0.10 → 0.12 seconds.
- Homing: keeps the previous 20% damage reduction; interval 0.6 → 0.8 seconds.
- Vibro palm: damage ×0.8.
- Beam: damage ×1.15, penetrates aligned targets.
- Central orange paw: 2 → 8 penetration passes.
- Spawned non-boss HP and tour boss HP: ×1.25; sloth additionally ×1.25.
- Orange temporary armor: ×2, duration 0.3 → 1.2 seconds. Activation rules unchanged.

## Reproducible assessment

`NovaThreatBalanceTest` surveys level-5 main/sub weapons without invulnerability or pickups. Sloth and mouse cover all weapon combinations and both cats, stationary and moving. Other stages cover spread/homing and black special/sonic. Random-pool encounters are tested at their latest slot; kings and champion use their actual slots. Black special starts within melee range to avoid falsely treating zero damage at long range as balance.

`nova-balance.csv` records 114 deterministic model runs, capped at 180 seconds. A BOSS result means timeout, not victory. The generic movement sweep is deliberately simple and can perform worse than standing still; it is not a human skill estimate. Specific counterplay runs separately use beam/shield, avoid locked laser lines, and move through shield-cleared space. These tests establish mechanical counterplay and prevent the two reported builds from clearing the new threat encounters while idle; they do not claim every loadout is equally strong or that every boss defeats an idle player.

Real play feel, frame rate on devices and accessibility with telegraphs disabled still require user playtesting. No emulator was used, as requested.

Counterplay reference (no ultimate, no temporary-armor activation, no pickups):

| Encounter | Loadout | Orange | Black |
| --- | --- | --- | --- |
| Mouse | Level-5 beam + shield; dodge locked rail line | Clear ~100 s, armor 64 | Clear ~100 s, armor 4 |
| Sloth | Level-5 beam + shield; change height around nearby side rounds | Clear ~117 s, armor 76 | Clear ~117 s, armor 40 |

Regression assertions require both cats to win these counterplay runs. Stationary spread/homing and stationary black special/shield must lose against both sloth and mouse. The accompanying CSVs retain the unsuccessful generic movement cases as well as successes.

Validation: `testDebugUnitTest assembleDebug assembleRelease` passed (137 tests, zero failures/skips; 4m 26s). The 114 survey cases plus four counterplay cases are captured in `nova-balance.csv` and `nova-counterplay.csv` beside this report.
