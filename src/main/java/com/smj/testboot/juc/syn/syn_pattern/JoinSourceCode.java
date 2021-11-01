package com.smj.testboot.juc.syn.syn_pattern;

public class JoinSourceCode {


    public static void main(String[] args) {

        // join的底层 就是wait  即 保护性暂停模式
        // join 跟其他的保护性暂停模式不同的地方在于，这里等待的结果是线程是否结束
        // 但其实现方式是一样的，此处的 Guarded Object 是 join 的线程对象，
        // 被 join 的线程等待 join 的线程结束的结果，只是恰好这个线程对象就是 Guarded Object。

        // 线程死亡的时候会自动调用自己的 notifyAll 方法，
        // 将关联此线程对象的 Monitor 上 WaitSet 中所有的线程唤醒，
        // 简而言之就是唤醒所有对自己线程对象加锁的线程中处于WAITING状态的线程，这个是由JVM底层实现的，源码中没有，
        // 不过我们可以通过分析源码的逻辑看出这个机制，这个机制是实现很多同步方法的基础。
        // 所以当 join 的线程执行结束就会唤醒被 join 的线程，这是 wait(0) 的执行流程。


        //   while 这段代码的逻辑就是带超时的保护性暂停模式。
        //   isAlive方法就是一个标志位，判断当前线程是否处于正在运行（Running）或就绪（Runnable）的状态，
        //  注意当前线程指的是当前加锁的线程对象指向的线程，所以.isAlive()方法代表现在加锁的这个线程对象指向的线程是否执行结束，也就是 join 的线程。
        // 如果还在运行就一直等待，有两种情况退出等待：
        //                          1. 标志位为假，即 join 的线程结束，调用 notifyAll，并且标志位为假（这种情况代表正确唤醒）；
        //                          2. 时间耗尽，退出等待。

        /**
         * public final synchronized void join(long millis)
         *     throws InterruptedException {
         *         long base = System.currentTimeMillis();
         *         long now = 0;
         *
         *         if (millis < 0) {
         *             throw new IllegalArgumentException("timeout value is negative");
         *         }
         *
         *         if (millis == 0) {
         *             while (isAlive()) {
         *                 wait(0);
         *             }
         *         } else {
         *             while (isAlive()) {
         *                 long delay = millis - now;
         *                 if (delay <= 0) {
         *                     break;
         *                 }
         *                 wait(delay);
         *                 now = System.currentTimeMillis() - base;
         *             }
         *         }
         *     }
         */

    }
}
