package com.smj.testboot.juc.syn.reentrant_lock.activity;


import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "c.LiveLock")
public class LiveLock {

    /**
     *   活锁出现在两个线程互相改变对方的结束条件，最后谁也无法结束，
     *   下例 就是活锁的情况   一个在不停的加  一个在不停的减  相互的结果被改变了  导致无法结束
     *   可以让一个线程 睡眠一会  只要无法去更改另外一个线程的结果就可以了
     */
    static volatile int count = 10;
    static final Object lock = new Object();

    public static void main(String[] args) {
        new Thread(() -> {
            // 期望减到 0 退出循环
            while (count > 0) {
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                count--;
                log.debug("count: {}", count);
            }
        }, "t1").start();
        new Thread(() -> {
            // 期望超过 20 退出循环
            while (count < 20) {
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                count++;
                log.debug("count: {}", count);
            }
        }, "t2").start();
    }


}
