package com.smj.testboot.juc.syn.wait_notify;


import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "c.SleepAndWait_2")
public class SleepAndWait_2 {

    static final Object room = new Object();
    static boolean hasCigarette = false;
    static boolean hasTakeout = false;

    //  直接将sleep 变成 wait/notify即可
    // 但是 此时 又会有新的问题   notify只是叫醒 其中一个 如果有多个线程在等待  是会出现错误唤醒的 --- 虚假唤醒

    public static void main(String[] args) throws InterruptedException {

        new Thread(() -> {
            synchronized (room) {
                log.debug("有烟没？[{}]", hasCigarette);
                if (!hasCigarette) {
                    log.debug("没烟，先歇会！");
                    try {
                        room.wait();
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

        new Thread(() -> {
            synchronized (room) {
                hasCigarette = true;
                log.debug("烟到了噢！");
                room.notify();
            }
        }, "送烟的").start();
    }
}
