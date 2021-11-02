package com.smj.testboot.juc.syn.reentrant_lock;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "c.DeadLock")
public class DeadLock {

    /**
     *    有这样的情况：一个线程需要同时获取多把锁，这时就容易发生死锁
     *    比如下面的 t1 线程 获得 A对象 锁，接下来想获取 B对象的锁 t2 线程 获得 B对象 锁，接下来想获取 A对象的锁
     *
     *    解决死锁  可以让加锁的顺序一样  但是又会有  饥饿 的问题
     *              或者 在外面套一个大锁
     */
    public static void main(String[] args) {
        Object A = new Object();
        Object B = new Object();
        Thread t1 = new Thread(() -> {
            synchronized (A) {
                log.debug("lock A");
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (B) {
                    log.debug("lock B");
                    log.debug("操作...");
                }
            }
        }, "t1");
        Thread t2 = new Thread(() -> {
            synchronized (B) {
                log.debug("lock B");
                try {
                    Thread.sleep((long) 0.5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (A) {
                    log.debug("lock A");
                    log.debug("操作...");
                }
            }
        }, "t2");
        t1.start();
        t2.start();
    }


}
