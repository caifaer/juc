package com.smj.testboot.juc.aqs;

public class AqsSourceCode {




}

/**
 *
 *      非公平锁  --  NonfairSync
 *          final void lock() {
 *          1. 首先尝试获取独占锁tryAcquire，如果获取成功就返回
 *              if (compareAndSetState(0, 1))
 *                 setExclusiveOwnerThread(Thread.currentThread());
 *             else
 *         2. 尝试获取失败就将当前线程以独占模式加入队列addWaiter，再次尝试获取，获取成功就返回，获取失败就阻塞当前线程
 *                 acquire(1);
 *         }
 *
 *         public final void acquire(int arg) {
 *         if (!tryAcquire(arg) &&
 *             acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
 *
 *            // 没有获取到锁  并且acquireQueued返回true的时候 才会来会来到这里
 *            // acquireQueued 什么时候会返会true  只有被park的线程 被其他非阻塞队列中的线程唤醒  也就是调用了 interrupt方法
 *            // 此时 会有 Thread.interrupted方法  返回true  并清除打断标记
 *            // 此时会返回true
 *             selfInterrupt();
 *          }
 *
 *          不可打断模式 在此模式下，即使它被打断，仍会驻留在 AQS 队列中，一直要等到获得锁后方能得知自己被打断了
 *          static void selfInterrupt() {
 *              //  重新产生中断
 *              Thread.currentThread().interrupt();
 *          }
 *
 *          // 不可打断模式 就是换成了 抛出异常的方式
 *
 *
 *
 *          // 比如非公平锁的acquire方法   使用cas 进行加锁
 *          // 如果加锁失败 就会进入addWaiter方法
 *          当尝试获取锁失败，就根据当前线程以独占模式创建Node加入队列
 *          如果队列尾节点存在，就使用CAS（compareAndSetTail）将node直接放入队列
 *          如果2执行失败或tail为null，执行enq，使用CAS自旋方式将node加入队列，死循环直到成功
 *          3.1 如果尾节点为null，说明队列为空，head也为null，node先CAS加入队列，head指向node
 *          3.2 如果队列尾节点存在，就使用CAS（compareAndSetTail）将node直接放入队列
 *          3.3 死循环执行，直到执行成功
 *
 *      private Node addWaiter(Node mode) {
 *         Node node = new Node(Thread.currentThread(), mode);
 *         Node pred = tail;
 *         // 队列尾节点存在
 *         if (pred != null) {
 *         // 前驱
 *             node.prev = pred;
 *              如果队列尾节点存在，就使用CAS（compareAndSetTail）将node直接放入队列
 *             if (compareAndSetTail(pred, node)) {
 *             // 后继
 *                 pred.next = node;
 *                 return node;
 *             }
 *         }
 *         如果尾节点为null，说明队列为空，head也为null，node先CAS加入队列，head指向node
 *         enq(node);
 *         return node;
 *     }
 *
 *
 *      最开始时AQS队列还是空的，即 head和tail都为null，死循环处理：
 *      1. 首先初始化队列，初始head和tail
 *      2. 队列初始化完成，继续循环，node节点添加进队列
 *     private Node enq(final Node node) {
 *         for (;;) {   // 自旋
 *             Node t = tail;
 *             if (t == null) { // 队列为空，必须先初始化  然后还会自旋 会进入else进行一个加入队列的操作
 *                  // 设置头节点   其实这里就是 初始化 哑节点
 *                 if (compareAndSetHead(new Node()))   // 初始化队列
 *                     tail = head;
 *             } else {
 *                 node.prev = t;
 *                 // 这里使用cas 就很巧妙 判断尾节点有没有被其他线程取代  如果没有  就使用node节点取代tail
 *                 if (compareAndSetTail(t, node)) {  //作用为将tail的引用指向node  参数t中的内容是tail的旧引用对象地址，node是新节点的地址
 *                     t.next = node;
 *                     return t;
 *                 }
 *             }
 *         }
 *     }
 *
 *     private final boolean compareAndSetHead(Node update) {
 *          //当前的head字段，和null值比对，默认是null，所以相等，所以赋值为update，也就是new node()
 *          return unsafe.compareAndSwapObject(this, headOffset, null, update);
 *      }
 *
 *
 *      private final boolean compareAndSetTail(Node expect, Node update) {
 *          //当前的tail字段和期望值exepct，即t进行比较，一定是相等的啊，以为t=tail么，所以更新赋值为update，
 *          //即新传进来的node（Thread A）
 *          return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
 *      }
 *
 *      addWaiter(node)方法将当前线程加入队列尾部了，此时node已经在队列中了，接下来就出阻塞当前线程，等待锁的获取
 *      当前线程在“死循环”中尝试获取同步状态，而只有前驱节点是头节点才能够尝试获取同步状态，原因有二：
 *          1. 头节点是成功获取到同步状态的节点，而头节点的线程释放了同步状态之后，将会唤醒其后继节点，后继节点的线程被唤醒后需要检查自己的前驱节点是否是头节点
 *          2. 维护同步队列的FIFO原则
 *
 *          如果线程A从同步队列获取到锁，则此时线程A对应的节点Node是首节点，当线程A执行完成释放锁，
 *          会唤醒线程A对应节点的后继节点 - 线程B对应节点，
 *          线程B节点在自旋时获取到锁（acquireQueued），此时头结点head会指向线程B节点，切断线程A节点的next引用，
 *          则线程A对应节点就从同步队列移除了
 *     final boolean acquireQueued(final Node node, int arg) {
 *         boolean failed = true;
 *         try {
 *             boolean interrupted = false;
 *             for (;;) { // 自旋
 *                 final Node p = node.predecessor();
 *                 // 前驱节点是头节点 意味着你是第一个线程节点 只有这样的节点才可以再尝试一次获取锁  如果此时你拿到了锁  就取出队列
 *                 //  否则的话  就走下面的if
 *                 if (p == head && tryAcquire(arg)) {
 *                     setHead(node);  // 因为已经获取到锁了 所以这个线程节点就没用了  此时清空线程节点数据 充当头节点
 *                     p.next = null; // help GC   将原来的头节点 断开连接
 *                     failed = false;
 *                     return interrupted;
 *                 }
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
 *     private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
 *         int ws = pred.waitStatus;
 *         if (ws == Node.SIGNAL) {
 *              return true;
 *         }
 *         if(ws>0){
 *         // do while循环 作用就是清除掉  无用的节点
 *              do{
 *                  node.prev=pred=pred.prev;
 *              }while(pred.waitStatus>0);
 *          pred.next=node;
 *        }else{
 *             // 这里就是说 你要把前驱节点的 状态 由 ws 变成 -1 （-1代表 有责任唤醒后继节点） 然后下一次循环的时候 就是true了
 *             // 一开始状态都是 0
 *              compareAndSetWaitStatus(pred,ws,Node.SIGNAL);
 *          }
 *              return false;
 *     }
 *
 *
 *       private final boolean parkAndCheckInterrupt() {
 *         //  所以就是对头线程节点 尝试了很多次还是没有获取锁  就会被park 住  等待前驱节点去unpark
 *         LockSupport.park(this);
 *         // 返回打断标记 并清除打断标记
 *         return Thread.interrupted();
 *     }
 *
 *
 *
 *线程调用LockSupport.park(this)被挂起，下面三种情况会唤醒线程：
 *      1. 其他线程中以被挂起线程为目标调用unpark
 *      2. 其他线程中中断当前就线程
 *      3. 虚假呼叫，即无理由返回
 *
 *      public void unlock() {
 *         sync.release(1);
 *     }
 *
 *     public final boolean release(int arg) {
 *     //  简单来说 就是更改 state的状态为0  因为是可重如锁的缘故 state可能会大于1  然后owner属性置为null
 *         if (tryRelease(arg)) {
 *             Node h = head;
 *             // 释放之后 要唤醒后继节点  前提就是 头节点不为null 并且状态不等于0
 *             if (h != null && h.waitStatus != 0)
 *                 unparkSuccessor(h);
 *             return true;
 *         }
 *         return false;
 *     }
 *
 *      头节点不为null 并且状态不等于0  就会来这里 进行解锁
 *      此时 被解锁的线程 如果是 对头节点  就是parkAndCheckInterrupt 这个方法这里 继续循环
 *      然后 运行到 tryAcquire 这里  会获取到锁  此时会进入if块  重新设置哑节点  也就是头节点
 *      将原来的哑节点断开连接  将之前释放的锁的节点设置为哑节点
 *     private void unparkSuccessor(Node node) {
 *          int ws=node.waitStatus;
 *          if(ws< 0) {
 *              compareAndSetWaitStatus(node,ws,0);
 *          }
 *          Node s=node.next;
 *          if(s==null||s.waitStatus>0){
 *              s=null;
 *              for(Node t=tail;t!=null&&t!=node;t=t.prev) {
 *                  if(t.waitStatus<=0) {
 *                      s=t;
 *                  }
 *              }
 *          }
 *          if(s!=null) {
 *              LockSupport.unpark(s.thread);
 *          }
 *      }
 *
 *
 *

 *
 *
 *
 *
 */