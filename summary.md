# Emulator smoke test
- stage 1, cat BLACK, played 40s, screen 720x1600, probe ''
- crashes: none
- memory kills: 2

## gfxinfo
Total frames rendered: 588
Janky frames: 576 (97.96%)
Janky frames (legacy): 588 (100.00%)
50th percentile: 129ms
90th percentile: 200ms
95th percentile: 200ms
99th percentile: 350ms
Number Slow UI thread: 532
Number Slow bitmap uploads: 285
Number Slow issue draw commands: 576
50th gpu percentile: 4950ms
90th gpu percentile: 4950ms
95th gpu percentile: 4950ms
99th gpu percentile: 4950ms

## ShooterPerf
--------- beginning of main
10-24 18:43:58.027  4843  4843 I ShooterPerf: frames=61 avg=60.4ms p95=116.7ms max=200.0ms slow(>20ms)=59 phase=STAGE bullets=0 particles=0 enemies=0
10-24 18:44:03.042  4843  4843 I ShooterPerf: frames=74 avg=70.5ms p95=116.7ms max=266.7ms slow(>20ms)=70 phase=STAGE bullets=0 particles=0 enemies=10
10-24 18:44:08.070  4843  4843 I ShooterPerf: frames=70 avg=72.1ms p95=150.0ms max=216.7ms slow(>20ms)=69 phase=STAGE bullets=0 particles=8 enemies=4
10-24 18:44:13.116  4843  4843 I ShooterPerf: frames=69 avg=72.7ms p95=150.0ms max=250.0ms slow(>20ms)=63 phase=STAGE bullets=0 particles=74 enemies=8
10-24 18:44:18.195  4843  4843 I ShooterPerf: frames=70 avg=72.6ms p95=133.3ms max=200.0ms slow(>20ms)=68 phase=STAGE bullets=16 particles=21 enemies=3
10-24 18:44:23.241  4843  4843 I ShooterPerf: frames=82 avg=62.0ms p95=116.7ms max=133.3ms slow(>20ms)=75 phase=STAGE bullets=32 particles=48 enemies=1
10-24 18:44:24.078  4843  4843 I ShooterPerf: ULTIMATE uptimeMs=97655
10-24 18:44:28.244  4843  4843 I ShooterPerf: frames=50 avg=99.3ms p95=283.3ms max=333.3ms slow(>20ms)=49 phase=STAGE bullets=3 particles=411 enemies=0
10-24 18:44:33.286  4843  4843 I ShooterPerf: frames=69 avg=73.4ms p95=150.0ms max=183.3ms slow(>20ms)=64 phase=STAGE bullets=0 particles=90 enemies=7
10-24 18:44:38.287  4843  4843 I ShooterPerf: frames=98 avg=51.2ms p95=116.7ms max=133.3ms slow(>20ms)=84 phase=STAGE bullets=0 particles=0 enemies=1
