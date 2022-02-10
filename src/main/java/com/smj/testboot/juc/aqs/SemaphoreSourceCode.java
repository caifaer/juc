package com.smj.testboot.juc.aqs;

public class SemaphoreSourceCode {


    /**
     *
     * 在创建 Semaphore对象的时候 就需要传 int 类型的 permits --- 允许的最大线程数
     *       这个值 其实就是 state  所以加锁的时候就要判断 permits是否用完了
     *       其实本质来说就是一个  共享锁   共享锁和独占锁之间的区别就是: 同一时刻能否有多个线程 同时获取到同步状态
     *
     *
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
     * 重点在于setHeadAndPropagate
     * 了解propagate 的含义
     *          1. propagate 小于0  获取同步状态失败
     *          2. propagate 大于0  获取同步状态成功  并且 还有 剩余的同步状态 供其他线程使用
     *          3. propagate 等于0  获取同步状态成功  但是 没有 剩余的同步状态 供其他线程使用
     *
     *   propagate > 0 的情况，即当前线程获取同步状态成功了，还有剩余的同步状态可用于其他线程获取，
     *   那就要通知在等待队列的线程，让他们尝试获取剩余的同步状态
     *   private void setHeadAndPropagate(Node node, int propagate) {
     *          // h 保存现在的头节点(因为后面头节点的可能会变)
     *          Node h = head; // Record old head for check below
     *          // 重新设置头节点 -- 将当前节点 设置 为头节点
     *          setHead(node);
     *          // 这里的判断 我也是醉了
     *          // 其实有些是没用的  h ==null  (h =head)==null  是不会出现的   addWaiter初始化的时候至少会有一个哑节点
     *          if(propagate >0 || h ==null || h.waitStatus< 0 || (h =head)==null || h.waitStatus< 0){
     *                  Node s = node.next;
     *                  if (s == null || s.isShared())
     *                  doReleaseShared();
     *         }
     *    }
     *
     *    // 关于 上述的判断
     *    1. propagate >0  还有信号可获取  可唤醒
     *    2. propagate <= 0  但是  h.waitStatus< 0   这里的h是旧的头节点  即原来的头节点小于0  等于-3
     *              在  doReleaseShared 方法中  会对旧的头节点的状态进行更新  要么改成0  要么改成-3
     *              所以猜测，当前线程执行到 h.waitStatus < 0 的判断前，有另外一个线程刚好执行了 doReleaseShared() 方法，将 waitStatus 又设置成PROPAGATE = -3
     *    3. 现在的头节点小于0  即等于 -1
     *
     *
     *       这里是jdk1.5的代码   没有doReleaseShared这个方法
     *       public final boolean releaseShared(int arg) {
     *          if(tryReleaseShared(arg)){
     *              Node h = head;
     *              if(h != null && h.waitStatus != 0)
     *              unparkSuccessor(h);
     *              return true;
     *          }
     *          return false;
     *     }
     *       if (propagate > 0 && node.waitStatus != 0) {
     *          Node s = node.next;
     *          // 下一个
     *          if (s == null || s.isShared())
     *          unparkSuccessor(node);
     *       }
     *      之所以有这么多的 判断，也是因为可能会有bug。
     *      假设 共享值是2， t3，t4获取到了共享变量。    t1，t2进入同步队列
     *      此时t3释放共享锁，在unparkSuccessor方法中将头节点状态先由-1改为0.  然后唤醒t1，t1就会自旋的去获取锁
     *      但这里有个致命问题 如果此时  t4也释放共享锁了，但是头节点已经是0了   并且此时t1还没来得更换头节点  在unparkSuccessor方法唤醒的就还是t1  此时就是重复唤醒
     *      所以 t2 就无法被唤醒。
     *
     *      PROPAGATE(-3)保证当一个线程被唤醒获取锁成功 到 将这个线程节点当做新的哨兵节点，回收旧哨兵节点的过程中，如果又有资源得到释放，不会再执行多余的唤醒操作
     *      因为当这个线程获取锁后会尝试唤醒他的后继节点(共享模式的节点)
     *
     *
     *
     *
     *      private void doReleaseShared() {
     *          for(;;){
     *              Node h = head;
     *              if (h != null && h != tail) {
     *                  int ws = h.waitStatus;
     *                  if (ws == Node.SIGNAL) {
     *                  if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
     *                      continue;            // loop to recheck cases
     *                      unparkSuccessor(h);
     *                  }
     *                  // 状态等于0  已经有节点在唤醒后继几点了  防止重复唤醒  并且将标记置为0  为后续唤醒共享节点做准备
     *                  else if (ws == 0 &&
     *                      !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
     *                      continue;                // loop on failed CAS
     *              }
     *              // 跳出循环
     *              if (h == head)
     *                  break;
     *              }
     *      }
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
