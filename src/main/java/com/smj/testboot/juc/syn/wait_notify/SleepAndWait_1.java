package com.smj.testboot.juc.syn.wait_notify;


import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "c.SleepAndWait_1")
public class SleepAndWait_1 {

    static final Object room = new Object();
    static boolean hasCigarette = false;
    static boolean hasTakeout = false;

    /**
     * 1. 其它干活的线程，都要一直阻塞，效率太低
     * 2. 小南线程必须睡足 2s 后才能醒来，就算烟提前送到，也无法立刻醒来
     * 3. 加了 synchronized (room) 后，就好比小南在里面反锁了门睡觉，烟根本没法送进门，主线程 没加synchronized 就好像 main 线程是翻窗户进来的
     *        优化------使用 wait - notify 机制
     */



    public static void main(String[] args) throws InterruptedException {

        new Thread(() -> {
            synchronized (room) {
                log.debug("有烟没？[{}]", hasCigarette);
                if (!hasCigarette) {
                    log.debug("没烟，先歇会！");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                log.debug("有烟没？[{}]", hasCigarette);
                if (hasCigarette) {
                    log.debug("可以开始干活了");
                }
            }
        }, "小南").start();
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                synchronized (room) {
                    log.debug("可以开始干活了");
                }
            }, "其它人").start();
        }

        Thread.sleep(1000);
        new Thread(() -> {
            // 这里能不能加 synchronized (room)？  是不能加的  否则就无法更改 值
            hasCigarette = true;
            log.debug("烟到了噢！");
        }, "送烟的").start();
    }
}
