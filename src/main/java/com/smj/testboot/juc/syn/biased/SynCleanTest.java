package com.smj.testboot.juc.syn.biased;


import lombok.extern.slf4j.Slf4j;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations=3)
@Measurement(iterations=5)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Slf4j(topic = "c.SynCleanTest")
public class SynCleanTest {

    /**
     * 锁消除  下面两个方法的性能几乎差不多
     * 原因是因为 有个jit即时编译器 会分析热点代码
     * 此时就会发现 o这个对象 属于局部变量 不会被共享  就会进行优化 消除掉锁
     * 有个变量  java -XX:-EliminateLocks -jar benchmarks.jar  可以设置
     */

    static int x = 0;
    @Benchmark
    public void a() throws Exception {
        x++;
    }

    @Benchmark
    public void b() throws Exception {
        Object o = new Object();
        synchronized (o) {
            x++;
        }
    }
}
