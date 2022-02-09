package com.smj.testboot.juc.aqs;

public class SemaphoreSourceCode {


    /**
     *
     * 在创建 Semaphore对象的时候 就需要传 int 类型的 permits   允许的最大线程数
     *       这个值 其实就是 state  所以加锁的时候就要判断 permits是否用完了
     *
     * //  true  permits未用完   直接用
     * //  false  permists用完了  进入同步队列中自旋获取锁
     * final int nonfairTryAcquireShared(int acquires) {
     *     for (;;) {
     *         int available = getState();
     *         int remaining = available - acquires;
     *         if (remaining < 0 ||
     *             compareAndSetState(available, remaining))
     *             return remaining;
     *     }
     * }
     *
     *
     * //  其实还是 共享锁的加锁流程
     * private void doAcquireSharedInterruptibly(int arg)
     *     throws InterruptedException {
     *     final Node node = addWaiter(Node.SHARED);
     *     boolean failed = true;
     *     try {
     *         for (;;) {
     *             final Node p = node.predecessor();
     *             if (p == head) {
     *                 int r = tryAcquireShared(arg);
     *                 if (r >= 0) {
     *                     setHeadAndPropagate(node, r);
     *                     p.next = null; // help GC
     *                     failed = false;
     *                     return;
     *                 }
     *             }
     *             if (shouldParkAfterFailedAcquire(p, node) &&
     *                 parkAndCheckInterrupt())
     *                 throw new InterruptedException();
     *         }
     *     } finally {
     *         if (failed)
     *             cancelAcquire(node);
     *     }
     * }
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     */





}
