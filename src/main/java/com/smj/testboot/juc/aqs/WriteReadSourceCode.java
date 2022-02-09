package com.smj.testboot.juc.aqs;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WriteReadSourceCode {


    public static void main(String[] args) {
        ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();


        reentrantReadWriteLock.readLock().lock();


    }
}

/**
 *
 *          ReentrantReadWriteLock中的state代表了 读锁的数量 和 写锁的持有与否。
 *                                             高16位代表 读锁。    低16位代表 写锁。
 *
 *
 *
 *      // 写锁重写了 这个方法
 *      protected final boolean tryAcquire(int acquires) {
 *                  Thread current = Thread.currentThread();
 *                 int c = getState();
 *                 // 获得写锁的次数
 *                 int w = exclusiveCount(c);
 *                 // 即加锁
 *                 //    加的是读锁 也有可能是 写锁
 *                 if (c != 0) {
 *                     // w等于0就是说没加写锁 加的是读锁 即已经有线程加了读锁了  读写互斥  直接false
 *                     // 如果加的是写锁  此时要判断是不是当前线程加的  因为可能会有重入
 *                     if (w == 0 || current != getExclusiveOwnerThread())
 *                         return false;
 *                      //  只有16位  超过了就抛出异常   很少  不会重入这么多次的
 *                     if (w + exclusiveCount(acquires) > MAX_COUNT)
 *                         throw new Error("Maximum lock count exceeded");
 *                     // 重置state
 *                     setState(c + acquires);
 *                     return true;
 *                 }
 *                 // 写锁是否应该阻塞  --- 非公平锁直接false  公平锁的话判断队列是否为空
 *                 // 走到这里 意味着  c=0
 *                 if (writerShouldBlock() ||
 *                     !compareAndSetState(c, c + acquires))
 *                     return false;
 *
 *                 setExclusiveOwnerThread(current);
 *                 return true;
 *      }
 *
 *      writerShouldBlock()
 *          1. 非公平锁 直接false
 *          2. 公平锁  hasQueuedPredecessors()方法
 *                 返回false   队列为空  或者  队列不为空 并且head后有线程节点 并且该节点 是当前线程
 *                 返回true    队列不为空 并且 (只有head节点  或者  队列中head节点后的节点 不是是当前线程)
 *
 *
 *          static final int SHARED_SHIFT   = 16;
 *         static final int SHARED_UNIT    = (1 << SHARED_SHIFT);
 *         static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;
 *         static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;
 *
 *      // 1 左移16位减1  就是  0000 0000 0000 0000 1111 1111 1111 1111
 *      // 16进制就是  0x00001111   即 低位 都是 1
 *      // 此时与state状态求 与& 就可以获得 写锁的次数   也就是获得同步状态的 低16位
 *      static int exclusiveCount(int c) {
 *          return c & EXCLUSIVE_MASK;   // 即 c & (1 << 16) - 1;
 *      }
 *      // 该方法是获取读锁被获取的次数，是将同步状态（int c）右移16次，即取同步状态的高16位
 *      static int sharedCount(int c)    {
 *          return c >>> SHARED_SHIFT;  // 即 c >>> 16
 *      }
 *
 *
 *
 *
 *      //  关于 读锁 ---  读锁也是可以重入的 ，是通过一个 内部类 HoldCounter 来进行计数
 *      // 某一个线程获取读锁时，他会获得一个HoldCounter，然后当他再次获取这把读锁时，就不会再次获得HoldCounter，而是将以前获得的这个HoldCounter的count自增1。
 *      // 当失去的时候，就自减1，如果当这个count为0时，这个HoldCounter就会被ReentrantReadWriteLock抛弃。
 *      //  此时思考一个问题，如何实现这样的一个 线程私用的 HoldCounter，并且还能够被ReentrantReadWriteLock?
 *      //     答案:  使用 ThreadLocal
 *          static final class HoldCounter {
 *             int count = 0;
 *             // 使用id 而不是引用变量 避免垃圾回收
 *             final long tid = getThreadId(Thread.currentThread());
 *         }
 *
 *          //  这里 就可以看出来  ThreadLocal中的存的 value 就是HoldCounter
 *         static final class ThreadLocalHoldCounter extends ThreadLocal<HoldCounter> {
 *                  public HoldCounter initialValue() {
 *                      return new HoldCounter();
 *                  }
 *         }
 *
 *
 *      //  读锁加锁   ---  共享锁
 *      public final void acquireShared(int arg) {
 *         if (tryAcquireShared(arg) < 0)
 *             doAcquireShared(arg);
 *     }
 *
 *
 *
 *      // 返回 -1 表示失败
 *      // 0 表示成功，但后继节点不会继续唤醒
 *      // 正数表示成功，而且数值表示 还有几个后继节点需要唤醒，读写锁返回 1
 *
 *      // 对于读写锁来说 就会返回 1 或者 -1, 只有这两种情况  上面的0  是信号量那边的
 *      protected final int tryAcquireShared(int unused) {
 *
 *          Thread current=Thread.currentThread();
 *          int c=getState();
 *          // true --- 写锁  并且 加写锁的线程不是当前线程  即另外有线程加了写锁  写写互斥
 *          // false ---
 *          //           1. 没有线程加写锁  读读并发
 *          //           2. 同一个线程加了写锁  然后自己又来加读锁   写锁是可以降级的
 *          if(exclusiveCount(c)!=0 && getExclusiveOwnerThread()!=current) {
 *              return-1;
 *          }
 *          // 获取高位的数值 即读锁的状态次数 也就是读锁的个数
 *          int r=sharedCount(c);
 *          // 读 不被阻塞  --- 注:  writerShouldBlock和readerShouldBlock方法都表示当有别的线程也在尝试获取锁时，是否应该阻塞。
 *          // 读锁个数小于 最大值
 *          // compareAndSetState(c,c+SHARED_UNIT) 即高位加1
 *          if(!readerShouldBlock() && r<MAX_COUNT && compareAndSetState(c,c+SHARED_UNIT)){
 *              /  进去if块内的 主要目的就是 给读锁计数
 *              // 如果当前读锁为0
 *              if(r==0){
 *                  firstReader=current;
 *                  firstReaderHoldCount=1;
 *              } // 读锁大于0 并且当前节点 和头节点相同 表示  重入了
 *              else if(firstReader==current){
 *                  firstReaderHoldCount++;
 *              }else{
 *                  // 读锁大于0 并且当前节点 和头节点不相同 表示 没有重入  --- 记录每一个线程读的次数
 *                  // 记录的方式 就是获取 对应线程的 HoldCounter
 *                  HoldCounter rh=cachedHoldCounter;
 *                  if(rh==null||rh.tid!=getThreadId(current)) {
 *                      cachedHoldCounter=rh=readHolds.get();
 *                  }else if(rh.count==0) {
 *                      readHolds.set(rh);
 *                  }
 *                  rh.count++;
 *              }
 *                  return 1;
 *          }
 *           //  否则 就自旋 进行加锁
 *          return fullTryAcquireShared(current);
 *      }
 *
 *
 *       final int fullTryAcquireShared(Thread current) {
 *              HoldCounter rh = null;
 *              for (;;) {
 *                  int c = getState();
 *                  if (exclusiveCount(c) != 0) {
 *                       // 持有 写锁
 *                      if (getExclusiveOwnerThread() != current)
 *                          return -1;
 *                  } else if (readerShouldBlock()) {
 *                      // 写锁堵塞
 *                      if (firstReader == current) {
 *                          // assert firstReaderHoldCount > 0;
 *                      } else {
 *                          if (rh == null) {
 *                              rh = cachedHoldCounter;
 *                              if (rh == null || rh.tid != getThreadId(current)) {
 *                                  rh = readHolds.get();
 *                                  // 读锁为0  直接删掉
 *                                  if (rh.count == 0)
 *                                      readHolds.remove();
 *                              }
 *                          }
 *                          if (rh.count == 0)
 *                              return -1;
 *                      }
 *
 *                  }
 *                  if (sharedCount(c) == MAX_COUNT)
 *                      throw new Error("Maximum lock count exceeded");
 *
 *                   // 加锁成功
 *                  if (compareAndSetState(c, c + SHARED_UNIT)) {
 *                      if (sharedCount(c) == 0) {
 *                          firstReader = current;
 *                          firstReaderHoldCount = 1;
 *                      } else if (firstReader == current) {
 *                          firstReaderHoldCount++;
 *                      } else {
 *                          if (rh == null)
 *                              rh = cachedHoldCounter;
 *                          if (rh == null || rh.tid != getThreadId(current))
 *                              rh = readHolds.get();
 *                          else if (rh.count == 0)
 *                              readHolds.set(rh);
 *                          rh.count++;
 *                          cachedHoldCounter = rh; // cache for release
 *                      }
 *                      return 1;
 *                  }
 *              }
 *            }
 *
 *
 *
 *
 *
 *
 *
 *       // writerShouldBlock和readerShouldBlock方法都表示当有别的线程也在尝试获取锁时，是否应该阻塞。
 *      static final class NonfairSync extends Sync {
 *              // 对于非公平锁来说  writerShouldBlock  直接 返回false  即不需要阻塞
 *              final boolean writerShouldBlock() {
 *                  return false;
 *              }
 *              final boolean readerShouldBlock() {
 *                   return apparentlyFirstQueuedIsExclusive();
 *          }
 *      }
 *       // 该方法在当前线程是写锁占用的线程时，返回true；否则返回false。
 *       // 也就说明，如果当前有一个写线程正在写，那么该读线程应该阻塞  返回true
 *      final boolean apparentlyFirstQueuedIsExclusive() {
 *         Node h, s;
 *         return (h = head) != null &&
 *             (s = h.next)  != null &&
 *             !s.isShared()  // 是一个写锁  非共享锁
 *             && s.thread != null;
 *     }
 *
 *     // 公平锁 就简单了  直接判断队列中有没有合适的节点  有就阻塞 没有就不阻塞
 *     static final class FairSync extends Sync {
 *          private static final long serialVersionUID = -2274990926593161451L;
 *          final boolean writerShouldBlock() {
 *              return hasQueuedPredecessors();
 *          }
 *          final boolean readerShouldBlock() {
 *              return hasQueuedPredecessors();
 *          }
 *      }
 *
 *
 *
 *
 *
 *
 *      返回-1 时
 *      private void doAcquireShared(int arg) {
 *          // 共享状态的节点
 *         final Node node = addWaiter(Node.SHARED);
 *         boolean failed = true;
 *         try {
 *             boolean interrupted = false;
 *             for (;;) {
 *                 final Node p = node.predecessor();
 *                 // 头节点的下一个 节点才有资格争夺锁
 *                 if (p == head) {
 *                     int r = tryAcquireShared(arg);
 *                     // 读写锁 加锁成功会返回1
 *                     if (r >= 0) {
 *                         setHeadAndPropagate(node, r);
 *                         p.next = null; // help GC
 *                         if (interrupted)
 *                             selfInterrupt();
 *                         failed = false;
 *                         return;
 *                     }
 *                 }
 *                 // 阻塞 改状态
 *                 if (shouldParkAfterFailedAcquire(p, node) &&
 *                     parkAndCheckInterrupt())
 *                     interrupted = true;
 *             }
 *         } finally {
 *             if (failed)
 *                 cancelAcquire(node);
 *         }
 *     }
 *
 *
 *
 *     private void setHeadAndPropagate(Node node, int propagate) {
 *         Node h = head; // Record old head for check below
 *         setHead(node);
 *         if (propagate > 0 || h == null || h.waitStatus < 0 ||
 *             (h = head) == null || h.waitStatus < 0) {
 *             Node s = node.next;
 *             if (s == null || s.isShared())
 *             //  共享锁的话 就一并唤醒了
 *                 doReleaseShared();
 *         }
 *     }
 *
 *     private void doReleaseShared() {
 *          for (;;) {
 *             Node h = head;
 *             if (h != null && h != tail) {
 *                 int ws = h.waitStatus;
 *                 if (ws == Node.SIGNAL) {
 *                 // 将头节点状态 由-1 改为0  目的是防止其他线程来再次唤醒  TODO  怎么就干扰了
 *                     if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
 *                         continue;            // loop to recheck cases
 *                         //  读 和 读的状态 是可以并行的  太妙了
 *                     unparkSuccessor(h);
 *                 }
 *                 else if (ws == 0 &&
 *                          !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
 *                     continue;                // loop on failed CAS
 *             }
 *             if (h == head)                   // loop if head changed
 *                 break;
 *         }
 *     }
 *
 *
 *
 *
 *
 *
 *     public void unlock() {
 *             sync.release(1);
 *    }
 *
 *
 *    public final boolean release(int arg) {
 *         if (tryRelease(arg)) {
 *             Node h = head;
 *             if (h != null && h.waitStatus != 0)
 *                 unparkSuccessor(h);
 *             return true;
 *         }
 *         return false;
 *     }
 *
 *
 *     protected final boolean tryRelease(int releases) {
 *             if (!isHeldExclusively())
 *                 throw new IllegalMonitorStateException();
 *             int nextc = getState() - releases;
 *             // 判断低8位 即写锁 是否是0
 *             boolean free = exclusiveCount(nextc) == 0;
 *             if (free)
 *                 setExclusiveOwnerThread(null);
 *             setState(nextc);
 *             return free;
 *    }
 *
 *
 *
 */
