package com.smj.testboot.juc.syn.reentrant_lock;


import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.ReentrantLock;

@Slf4j(topic = "c.CanInterrupt")
public class CanInterrupt {

    // synchronized 和 lock方法都是不可中断的
    static final ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {

            log.debug("启动...");
            try {
                //  lockInterruptibly 是可中断的
                // 如果没有竞争的话 就直接获取 lock 锁对象
                // 有竞争的话 就会进入阻塞队列  可以被其他线程用 interrupt打断。
                lock.lockInterruptibly();
            } catch (InterruptedException e) {// 获取打断异常  然后终止
                e.printStackTrace();
                log.debug("等锁的过程中被打断");
                return;
            }
            try {
                log.debug("获得了锁");
            } finally {
                lock.unlock();
            }
        }, "t1");

        lock.lock(); // 主线程加锁  此时有竞争了 上面的线程就会进入到 阻塞队列中
        log.debug("获得了锁");
        t1.start();

        try {
            Thread.sleep(1);
            t1.interrupt(); // 执行打断 抛出异常
            log.debug("执行打断");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

    }


}
