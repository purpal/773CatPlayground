# Emulator smoke test
- stage 8, cat BLACK, played 230s, screen 720x1600, probe ''
- crashes: none
- memory kills: 8

## gfxinfo
Total frames rendered: 3658
Janky frames: 3589 (98.11%)
Janky frames (legacy): 3658 (100.00%)
50th percentile: 113ms
90th percentile: 150ms
95th percentile: 150ms
99th percentile: 250ms
Number Slow UI thread: 2921
Number Slow bitmap uploads: 1363
Number Slow issue draw commands: 3589
50th gpu percentile: 4950ms
90th gpu percentile: 4950ms
95th gpu percentile: 4950ms
99th gpu percentile: 4950ms

## ShooterPerf
--------- beginning of main
10-25 19:31:40.595  4432  4432 I ShooterPerf: frames=60 avg=83.6ms p95=150.0ms max=966.7ms slow(>20ms)=56 phase=INTRO bullets=0 particles=0 enemies=0
10-25 19:31:45.693  4432  4432 I ShooterPerf: frames=63 avg=79.9ms p95=166.7ms max=233.3ms slow(>20ms)=63 phase=STAGE bullets=0 particles=5 enemies=10
10-25 19:31:50.720  4432  4432 I ShooterPerf: frames=60 avg=84.7ms p95=266.7ms max=350.0ms slow(>20ms)=59 phase=STAGE bullets=0 particles=0 enemies=2
10-25 19:31:55.761  4432  4432 I ShooterPerf: frames=75 avg=67.3ms p95=133.3ms max=150.0ms slow(>20ms)=75 phase=STAGE bullets=32 particles=0 enemies=14
10-25 19:32:00.792  4432  4432 I ShooterPerf: frames=65 avg=77.4ms p95=150.0ms max=166.7ms slow(>20ms)=64 phase=STAGE bullets=50 particles=45 enemies=17
10-25 19:32:05.794  4432  4432 I ShooterPerf: frames=62 avg=81.2ms p95=150.0ms max=183.3ms slow(>20ms)=61 phase=STAGE bullets=22 particles=27 enemies=3
10-25 19:32:08.012  4432  4432 I ShooterPerf: ULTIMATE uptimeMs=90788
10-25 19:32:10.875  4432  4432 I ShooterPerf: frames=67 avg=74.6ms p95=150.0ms max=250.0ms slow(>20ms)=64 phase=STAGE bullets=14 particles=74 enemies=3
10-25 19:32:15.906  4432  4432 I ShooterPerf: frames=72 avg=71.1ms p95=200.0ms max=283.3ms slow(>20ms)=61 phase=STAGE bullets=0 particles=0 enemies=3
10-25 19:32:20.959  4432  4432 I ShooterPerf: frames=85 avg=58.6ms p95=100.0ms max=150.0ms slow(>20ms)=78 phase=STAGE bullets=22 particles=108 enemies=1
10-25 19:32:25.942  4432  4432 I ShooterPerf: frames=93 avg=53.8ms p95=100.0ms max=116.7ms slow(>20ms)=88 phase=STAGE bullets=22 particles=52 enemies=8
10-25 19:32:31.007  4432  4432 I ShooterPerf: frames=80 avg=63.1ms p95=116.7ms max=150.0ms slow(>20ms)=70 phase=STAGE bullets=12 particles=69 enemies=0
10-25 19:32:34.634  4432  4432 I ShooterPerf: ULTIMATE uptimeMs=117411
10-25 19:32:36.037  4432  4432 I ShooterPerf: frames=68 avg=74.3ms p95=133.3ms max=183.3ms slow(>20ms)=65 phase=STAGE bullets=5 particles=0 enemies=0
10-25 19:32:41.056  4432  4432 I ShooterPerf: frames=63 avg=80.2ms p95=166.7ms max=216.7ms slow(>20ms)=61 phase=STAGE bullets=0 particles=160 enemies=1
10-25 19:32:46.059  4432  4432 I ShooterPerf: frames=75 avg=66.0ms p95=116.7ms max=150.0ms slow(>20ms)=70 phase=STAGE bullets=6 particles=120 enemies=0
10-25 19:32:51.083  4432  4432 I ShooterPerf: frames=76 avg=66.2ms p95=116.7ms max=150.0ms slow(>20ms)=70 phase=STAGE bullets=2 particles=66 enemies=0
10-25 19:32:56.104  4432  4432 I ShooterPerf: frames=71 avg=71.4ms p95=116.7ms max=150.0ms slow(>20ms)=68 phase=STAGE bullets=1 particles=31 enemies=1
10-25 19:33:01.124  4432  4432 I ShooterPerf: frames=76 avg=65.8ms p95=133.3ms max=166.7ms slow(>20ms)=70 phase=STAGE bullets=12 particles=17 enemies=2
10-25 19:33:06.139  4432  4432 I ShooterPerf: frames=72 avg=69.9ms p95=116.7ms max=333.3ms slow(>20ms)=70 phase=STAGE bullets=12 particles=77 enemies=7
10-25 19:33:11.144  4432  4432 I ShooterPerf: frames=76 avg=65.6ms p95=133.3ms max=150.0ms slow(>20ms)=71 phase=STAGE bullets=4 particles=13 enemies=1
10-25 19:33:16.197  4432  4432 I ShooterPerf: frames=77 avg=65.6ms p95=116.7ms max=300.0ms slow(>20ms)=72 phase=STAGE bullets=28 particles=0 enemies=1
10-25 19:33:21.233  4432  4432 I ShooterPerf: frames=75 avg=67.3ms p95=133.3ms max=150.0ms slow(>20ms)=69 phase=STAGE bullets=37 particles=12 enemies=7
10-25 19:33:26.287  4432  4432 I ShooterPerf: frames=79 avg=63.5ms p95=100.0ms max=200.0ms slow(>20ms)=74 phase=STAGE bullets=41 particles=0 enemies=3
10-25 19:33:31.476  4432  4432 I ShooterPerf: frames=78 avg=65.0ms p95=133.3ms max=150.0ms slow(>20ms)=75 phase=WARNING bullets=9 particles=73 enemies=0
10-25 19:33:36.472  4432  4432 I ShooterPerf: frames=58 avg=89.4ms p95=183.3ms max=216.7ms slow(>20ms)=54 phase=WARNING bullets=2 particles=4 enemies=2
10-25 19:33:41.511  4432  4432 I ShooterPerf: frames=64 avg=78.4ms p95=133.3ms max=166.7ms slow(>20ms)=63 phase=BOSS bullets=4 particles=0 enemies=2
10-25 19:33:46.538  4432  4432 I ShooterPerf: frames=74 avg=67.6ms p95=100.0ms max=133.3ms slow(>20ms)=73 phase=BOSS bullets=26 particles=9 enemies=2
10-25 19:33:51.563  4432  4432 I ShooterPerf: frames=79 avg=64.1ms p95=116.7ms max=166.7ms slow(>20ms)=74 phase=BOSS bullets=57 particles=2 enemies=2
10-25 19:33:56.591  4432  4432 I ShooterPerf: frames=70 avg=71.2ms p95=116.7ms max=133.3ms slow(>20ms)=66 phase=BOSS bullets=26 particles=0 enemies=8
10-25 19:34:01.634  4432  4432 I ShooterPerf: frames=71 avg=71.1ms p95=150.0ms max=200.0ms slow(>20ms)=67 phase=BOSS bullets=18 particles=6 enemies=2
10-25 19:34:06.662  4432  4432 I ShooterPerf: frames=81 avg=62.1ms p95=100.0ms max=133.3ms slow(>20ms)=78 phase=BOSS bullets=59 particles=4 enemies=2
10-25 19:34:11.683  4432  4432 I ShooterPerf: frames=76 avg=65.8ms p95=100.0ms max=133.3ms slow(>20ms)=74 phase=BOSS bullets=57 particles=22 enemies=2
10-25 19:34:16.706  4432  4432 I ShooterPerf: frames=81 avg=62.1ms p95=100.0ms max=116.7ms slow(>20ms)=76 phase=BOSS bullets=17 particles=30 enemies=7
10-25 19:34:21.766  4432  4432 I ShooterPerf: frames=80 avg=63.5ms p95=116.7ms max=166.7ms slow(>20ms)=75 phase=BOSS bullets=22 particles=9 enemies=2
10-25 19:34:26.824  4432  4432 I ShooterPerf: frames=77 avg=65.4ms p95=116.7ms max=150.0ms slow(>20ms)=74 phase=BOSS bullets=50 particles=6 enemies=2
10-25 19:34:31.849  4432  4432 I ShooterPerf: frames=75 avg=66.9ms p95=116.7ms max=116.7ms slow(>20ms)=72 phase=BOSS bullets=30 particles=4 enemies=8
10-25 19:34:36.858  4432  4432 I ShooterPerf: frames=97 avg=52.1ms p95=66.7ms max=100.0ms slow(>20ms)=96 phase=BOSS bullets=19 particles=4 enemies=2
10-25 19:34:41.901  4432  4432 I ShooterPerf: frames=92 avg=54.5ms p95=83.3ms max=100.0ms slow(>20ms)=92 phase=BOSS bullets=66 particles=0 enemies=2
10-25 19:34:46.942  4432  4432 I ShooterPerf: frames=86 avg=58.7ms p95=83.3ms max=83.3ms slow(>20ms)=86 phase=BOSS bullets=40 particles=4 enemies=8
10-25 19:34:51.949  4432  4432 I ShooterPerf: frames=107 avg=46.7ms p95=66.7ms max=83.3ms slow(>20ms)=105 phase=BOSS bullets=16 particles=0 enemies=2
10-25 19:34:56.982  4432  4432 I ShooterPerf: frames=107 avg=47.0ms p95=66.7ms max=66.7ms slow(>20ms)=107 phase=BOSS bullets=55 particles=0 enemies=2
10-25 19:35:02.007  4432  4432 I ShooterPerf: frames=104 avg=48.4ms p95=66.7ms max=83.3ms slow(>20ms)=103 phase=BOSS bullets=37 particles=4 enemies=8
10-25 19:35:07.033  4432  4432 I ShooterPerf: frames=106 avg=47.5ms p95=66.7ms max=100.0ms slow(>20ms)=102 phase=BOSS bullets=17 particles=2 enemies=2
10-25 19:35:12.040  4432  4432 I ShooterPerf: frames=108 avg=46.3ms p95=66.7ms max=66.7ms slow(>20ms)=107 phase=BOSS bullets=61 particles=5 enemies=2
10-25 19:35:17.048  4432  4432 I ShooterPerf: frames=103 avg=48.5ms p95=66.7ms max=66.7ms slow(>20ms)=103 phase=BOSS bullets=35 particles=6 enemies=8
10-25 19:35:22.060  4432  4432 I ShooterPerf: frames=100 avg=50.2ms p95=83.3ms max=116.7ms slow(>20ms)=99 phase=BOSS bullets=19 particles=8 enemies=2
10-25 19:35:27.070  4432  4432 I ShooterPerf: frames=107 avg=46.9ms p95=66.7ms max=100.0ms slow(>20ms)=105 phase=BOSS bullets=61 particles=8 enemies=2
10-25 19:35:32.097  4432  4432 I ShooterPerf: frames=125 avg=40.3ms p95=66.7ms max=83.3ms slow(>20ms)=104 phase=BOSS bullets=38 particles=0 enemies=8
