package com.smj.testboot.juc.syn.biased;


import lombok.extern.slf4j.Slf4j;
import org.openjdk.jol.info.ClassLayout;

import java.util.Vector;
import java.util.concurrent.locks.LockSupport;

@Slf4j(topic = "c.BatchCancelTest")
public class BatchCancelTest {

    static Thread t1,t2,t3;

    public static void main(String[] args) throws InterruptedException {
        /**
         * 当撤销偏向锁阈值超过 40 次后，jvm 会这样觉得，自己确实偏向错了，根本就不该偏向。于是整个类的所有对象
         * 都会变为不可偏向的，新建的对象也是不可偏向的
         *
         * t1的时候 都是偏向锁
         * t2的时候 前20个会撤销偏向锁 变成一个轻量级锁  然后执行完之后就是无状态  剩下的19个就会偏向t2线程
         * t3的时候  无状态锁 变成 轻量级锁  剩下的19个进行撤销
         * 最后超过40次撤销 就会将这个类的所有对象的状态撤销。
         *
         *
         */
            Vector<Dog> list = new Vector<>();
            int loopNumber = 39;
            t1 = new Thread(() -> {
                for (int i = 0; i < loopNumber; i++) {
                    Dog d = new Dog();
                    list.add(d);
                    synchronized (d) {
                        log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintable());
                    }
                }
                LockSupport.unpark(t2);
            }, "t1");
            t1.start();
        t2 = new Thread(() -> {
            LockSupport.park();
            log.debug("===============> ");
            for (int i = 0; i < loopNumber; i++) {
                Dog d = list.get(i);
                log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintable());
                synchronized (d) {
                    log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintable());
                }
                log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintable());
            }
            LockSupport.unpark(t3);
        }, "t2");
        t2.start();
        t3 = new Thread(() -> {
            LockSupport.park();
            log.debug("===============> ");
            for (int i = 0; i < loopNumber; i++) {
                Dog d = list.get(i);
                log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintable());
                synchronized (d) {
                    log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintable());
                }
                log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintable());
            }
        }, "t3");
        t3.start();
        t3.join();
        log.debug(ClassLayout.parseInstance(new Dog()).toPrintable());
    }


}
