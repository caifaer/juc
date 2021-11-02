package com.smj.testboot.juc.syn.reentrant_lock;


import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j(topic = "c.WaitTimeReentrant")
public class WaitTimeReentrant {


    static ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) {

        Thread t1 = new Thread(() -> {
            log.debug("启动...");
            // tryLock 返回一个boolean值 来判断是否获取到锁
            // 还有一个重载的方法  tryLock(long timeout, TimeUnit unit)
            // 并且tryLock 也时可以打断的
            boolean b = false;
            try {
                b = lock.tryLock(1, TimeUnit.SECONDS);
                if (!b) {
                    log.debug("获取立刻失败，返回");
                    return;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                log.debug("获得了锁");
            } finally {
                lock.unlock();
            }
        }, "t1");
        lock.lock();
        log.debug("获得了锁");
        t1.start();
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

}
