# 背景
前天看到一个由于GC时间过长引发接口超时的问题。最终排查到的结果是因为GC的Safe Point机制引起。其排查思路和过程值得学习，看完文章即着手重现问题，按作者思路排查一遍。
参考链接：https://juejin.cn/post/6844903878765314061

# 复现问题
构造两个线程

线程1负责输出，每隔1秒执行一次打印距离上次执行的时间间隔

线程2负责拖住JVM，间接影响线程1的输出。

# 限制内存为5M时的情况
从运行日志里初步复现了问题，观察到明显超时。同时观察到多消耗的时间用在了GC前的等待 。
```

27	1000

# SafepointSynchronize::begin: Timeout detected:
# SafepointSynchronize::begin: Timed out while spinning to reach a safepoint.
# SafepointSynchronize::begin: Threads which did not reach the safepoint:
# "main" #1 prio=5 os_prio=0 tid=0x00007f46f400c800 nid=0x3738 runnable [0x0000000000000000]
   java.lang.Thread.State: RUNNABLE

# SafepointSynchronize::begin: (End of list)
         vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
26.163: no vm operation                  [      12          1              1    ]      [  2681     0  2681     0     0    ]  1   
Total time for which application threads were stopped: 2.6811167 seconds, Stopping threads took: 2.6810797 seconds
28	2704
```
## VM 参数
-XX:+PrintGCDetails -XX:+PrintGC -Xmx5m -XX:+PrintGCApplicationStoppedTime


## 运行日志
```
～/.jdks/adopt-openjdk-1.8.0_292/bin/java -XX:+PrintGCDetails -XX:+PrintGC -Xmx5m -XX:+PrintGCApplicationStoppedTime 

[GC (Allocation Failure) [PSYoungGen: 1024K->384K(1536K)] 1024K->392K(5632K), 0.0081730 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
         vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
0.052: ParallelGCFailedAllocation       [       9          0              2    ]      [     0     2     2     0     8    ]  0   
Total time for which application threads were stopped: 0.0112408 seconds, Stopping threads took: 0.0029314 seconds
[GC (Allocation Failure) [PSYoungGen: 1408K->480K(1536K)] 1416K->544K(5632K), 0.0016054 secs] [Times: user=0.00 sys=0.00, real=0.01 secs] 
         vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
0.094: ParallelGCFailedAllocation       [      10          0              0    ]      [     0     0     0     0     1    ]  0   
Total time for which application threads were stopped: 0.0017255 seconds, Stopping threads took: 0.0000256 seconds
[GC (Allocation Failure) [PSYoungGen: 1504K->496K(1536K)] 1568K->632K(5632K), 0.0027406 secs] [Times: user=0.01 sys=0.00, real=0.00 secs] 
         vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
0.128: ParallelGCFailedAllocation       [      10          0              0    ]      [     0     0     0     0     2    ]  0   
Total time for which application threads were stopped: 0.0029311 seconds, Stopping threads took: 0.0000606 seconds
[GC (Allocation Failure) [PSYoungGen: 1520K->480K(1536K)] 1656K->1304K(5632K), 0.0036221 secs] [Times: user=0.01 sys=0.00, real=0.00 secs] 
         vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
0.143: ParallelGCFailedAllocation       [      11          0              1    ]      [     0     0     0     0     3    ]  0   
Total time for which application threads were stopped: 0.0042159 seconds, Stopping threads took: 0.0000533 seconds
[GC (Allocation Failure) [PSYoungGen: 1504K->512K(1536K)] 2328K->2304K(5632K), 0.0032430 secs] [Times: user=0.00 sys=0.01, real=0.00 secs] 
         vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
0.149: ParallelGCFailedAllocation       [      11          0              1    ]      [     0     0     0     0     3    ]  0   
Total time for which application threads were stopped: 0.0033755 seconds, Stopping threads took: 0.0000338 seconds
[GC (Allocation Failure) [PSYoungGen: 1536K->512K(1536K)] 3328K->3312K(5632K), 0.0037838 secs] [Times: user=0.01 sys=0.00, real=0.01 secs] 
[Full GC (Ergonomics) [PSYoungGen: 512K->0K(1536K)] [ParOldGen: 2800K->3178K(4096K)] 3312K->3178K(5632K), [Metaspace: 4467K->4467K(1056768K)], 0.1330129 secs] [Times: user=0.59 sys=0.00, real=0.13 secs] 
         vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
0.154: ParallelGCFailedAllocation       [      11          0              1    ]      [     0     0     0     0   136    ]  0   
Total time for which application threads were stopped: 0.1369698 seconds, Stopping threads took: 0.0000387 seconds
[Full GC (Ergonomics) [PSYoungGen: 1024K->0K(1536K)] [ParOldGen: 3178K->4065K(4096K)] 4202K->4065K(5632K), [Metaspace: 4579K->4579K(1056768K)], 0.0299203 secs] [Times: user=0.11 sys=0.00, real=0.02 secs] 
         vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
0.295: ParallelGCFailedAllocation       [      11          0              1    ]      [     0     0     0     0    29    ]  0   
Total time for which application threads were stopped: 0.0300521 seconds, Stopping threads took: 0.0000401 seconds
1	66
2	808
[Full GC (Ergonomics) [PSYoungGen: 1024K->661K(1536K)] [ParOldGen: 4065K->4064K(4096K)] 5089K->4725K(5632K), [Metaspace: 4957K->4957K(1056768K)], 0.0488316 secs] [Times: user=0.34 sys=0.00, real=0.05 secs] 
         vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
1.192: ParallelGCFailedAllocation       [      12          1              1    ]      [     0     0     0     0    48    ]  1   
Total time for which application threads were stopped: 0.0491086 seconds, Stopping threads took: 0.0000741 seconds
[Full GC (Ergonomics) [PSYoungGen: 1024K->886K(1536K)] [ParOldGen: 4064K->4004K(4096K)] 5088K->4891K(5632K), [Metaspace: 5067K->5061K(1056768K)], 0.0455902 secs] [Times: user=0.32 sys=0.00, real=0.04 secs] 
         vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
1.249: ParallelGCFailedAllocation       [      12          1              2    ]      [     0     0     0     0    45    ]  1   
Total time for which application threads were stopped: 0.0459089 seconds, Stopping threads took: 0.0002113 seconds
3	999
         vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
2.295: no vm operation                  [      12          1              1    ]      [     0     0     0     0     0    ]  1   
Total time for which application threads were stopped: 0.0001338 seconds, Stopping threads took: 0.0000829 seconds
4	1000
         vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
4.080: EnableBiasedLocking              [      12          1              1    ]      [     0     0     0     0     0    ]  1   
Total time for which application threads were stopped: 0.0002403 seconds, Stopping threads took: 0.0001469 seconds
5	1000
6	1002
7	998
8	1000
9	1000
10	1000
11	1000
12	1000
13	1000
14	1000
15	1000
16	1000
17	1000
18	1000
19	1000
20	1000
21	1000
         vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
21.082: no vm operation                  [      12          1              1    ]      [     0     0     0     0     0    ]  1   
Total time for which application threads were stopped: 0.0002565 seconds, Stopping threads took: 0.0001748 seconds
22	1000
[Full GC (Ergonomics) [PSYoungGen: 1024K->856K(1536K)] [ParOldGen: 4004K->4004K(4096K)] 5028K->4860K(5632K), [Metaspace: 5069K->5069K(1056768K)], 0.0227979 secs] [Times: user=0.16 sys=0.00, real=0.02 secs] 
         vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
22.140: ParallelGCFailedAllocation       [      12          1              1    ]      [     0     0     0     0    22    ]  1   
Total time for which application threads were stopped: 0.0230258 seconds, Stopping threads took: 0.0001074 seconds
23	1000
24	977
         vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
23.163: no vm operation                  [      12          1              1    ]      [     0     0     0     0     0    ]  1   
Total time for which application threads were stopped: 0.0000927 seconds, Stopping threads took: 0.0000435 seconds
25	1000
26	1000
27	1000

# SafepointSynchronize::begin: Timeout detected:
# SafepointSynchronize::begin: Timed out while spinning to reach a safepoint.
# SafepointSynchronize::begin: Threads which did not reach the safepoint:
# "main" #1 prio=5 os_prio=0 tid=0x00007f46f400c800 nid=0x3738 runnable [0x0000000000000000]
   java.lang.Thread.State: RUNNABLE

# SafepointSynchronize::begin: (End of list)
         vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
26.163: no vm operation                  [      12          1              1    ]      [  2681     0  2681     0     0    ]  1   
Total time for which application threads were stopped: 2.6811167 seconds, Stopping threads took: 2.6810797 seconds
28	2704
29	0
30	295
31	1000
32	1000
33	1000
34	1000
35	1000
36	1000

# SafepointSynchronize::begin: Timeout detected:
# SafepointSynchronize::begin: Timed out while spinning to reach a safepoint.
# SafepointSynchronize::begin: Threads which did not reach the safepoint:
# "main" #1 prio=5 os_prio=0 tid=0x00007f46f400c800 nid=0x3738 runnable [0x0000000000000000]
   java.lang.Thread.State: RUNNABLE

# SafepointSynchronize::begin: (End of list)
         vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
35.845: no vm operation                  [      12          1              1    ]      [  4327     0  4327     0     0    ]  1   
Total time for which application threads were stopped: 4.3274325 seconds, Stopping threads took: 4.3273947 seconds
37	5033
38	0
39	0
40	0
41	0
42	967
43	1000
44	1000
45	1000
46	1000
47	1000
48	1000
49	1000
50	1000
51	1000
52	1000
53	1000
         vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
52.174: no vm operation                  [      12          1              1    ]      [   178     0   178     0     0    ]  1   
Total time for which application threads were stopped: 0.1789313 seconds, Stopping threads took: 0.1787719 seconds
54	1000
55	1000
56	1000
57	1000
58	1000
59	1000
60	1000
61	1000

# SafepointSynchronize::begin: Timeout detected:
# SafepointSynchronize::begin: Timed out while spinning to reach a safepoint.
# SafepointSynchronize::begin: Threads which did not reach the safepoint:
# "main" #1 prio=5 os_prio=0 tid=0x00007f46f400c800 nid=0x3738 runnable [0x0000000000000000]
   java.lang.Thread.State: RUNNABLE

# SafepointSynchronize::begin: (End of list)
         vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
60.353: no vm operation                  [      12          1              1    ]      [  4361     0  4361     0     0    ]  1   
Total time for which application threads were stopped: 4.3620089 seconds, Stopping threads took: 4.3619396 seconds
62	4575
63	0
64	0
65	0
66	424
67	1000

# SafepointSynchronize::begin: Timeout detected:
# SafepointSynchronize::begin: Timed out while spinning to reach a safepoint.
# SafepointSynchronize::begin: Threads which did not reach the safepoint:
# "main" #1 prio=5 os_prio=0 tid=0x00007f46f400c800 nid=0x3738 runnable [0x0000000000000000]
   java.lang.Thread.State: RUNNABLE

# SafepointSynchronize::begin: (End of list)

进程已结束，退出代码为 137 (interrupted by signal 9: SIGKILL)


```

# 排查问题
perf 


# 解决问题

## 方法一 修改VM参数
-XX:+UseCountedLoopSafepoints
增加参数后观察日志发现，超时现象消除了

但同时出现了另一个现象，线程1执行间隔少了1毫秒
```
138	1000
139	999
140	1000
141	999
142	1000
143	999
144	999
```

## 方法二 修改循环变量类型解决
循环变量i的类型从 int 改为 long

变量i类型修改为long 后，超时现象消除，稳定的输出1000，但也存在一两次打印999的情况。
```
88	1000
89	1000
90	1000
91	1000
92	999
93	1000
94	1000
95	1000
96	1000
97	1000
         vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
96.155: no vm operation                  [      12          1              1    ]      [     0     0     0     0     0    ]  1   
Total time for which application threads were stopped: 0.0001258 seconds, Stopping threads took: 0.0000749 seconds
98	1000
99	1000
100	1000
101	1000
```

## 方法三 尽量避免大循环
避免代码里出现耗时长的大循环。


# 遗留问题
- [ ] 了解更多perf工具使用详情
- [ ] 排查999的原因