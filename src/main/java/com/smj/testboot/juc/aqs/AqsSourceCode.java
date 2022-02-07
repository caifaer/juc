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
 *             // 当前节点node的前驱指向 tail节点
 *             node.prev = pred;
 *              如果队列尾节点存在，就使用CAS（compareAndSetTail）将tail设置为node
 *              其实只是 指针的更改 相当于让原来的tail节点指向 node节点  此时node节点就是尾节点了
 *             if (compareAndSetTail(pred, node)) {
 *                      // 后继
 *                   //如果CAS尝试成功，就说明"设置当前节点node的前驱"与"CAS设置tail"之间 没有别的线程设置tail成功
 *                   // (cas 就很巧妙 判断尾节点有没有被其他线程取代  如果没有  就使用node节点取代tail)
 *                 //只需要将"之前的tail"的后继节点指向node即可
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
 *     // 补充
 *     //     1. 初始时       head = tail = null
 *     //     2. 初始化       head = tail = new Node()  哑节点
 *     //     3. 添加新节点   Node t = tail;  node.prev = t;   tail = node;   t.next = node;  return t;
 *
 *
 *
 *     private final boolean compareAndSetHead(Node update) {
 *          //当前的head字段，和null值比对，默认是null，所以相等，所以赋值为update，也就是new node()
 *          return unsafe.compareAndSwapObject(this, headOffset, null, update);
 *      }
 *
 *
 *      private final boolean compareAndSetTail(Node expect, Node update) {
 *          //当前的tail字段和期望值exepct，即t进行比较，一定是相等的啊，因为t=tail，所以更新赋值为update，
 *          //即新传进来的node（Thread A）  也就是让tail 指向新的节点node
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
 *                     return interrupted; // 退出自旋
 *                 }
 *
 *                 // 获取锁失败后  进行暂停
 *                 if (shouldParkAfterFailedAcquire(p, node) &&
 *                     parkAndCheckInterrupt())
 *                     //线程在处于等待期间被中断  所以后续会进行自我中断一次
 *                     interrupted = true;
 *             }
 *         } finally {
 *             if (failed)
 *             //如果tryAcquire出现异常那么取消当前结点的获取，毕竟tryAcquire是留给子类实现的
 *                 cancelAcquire(node);
 *         }
 *     }
 *
 *     private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
 *         int ws = pred.waitStatus;
 *         if (ws == Node.SIGNAL) { // Node.SIGNAL = -1;
 *              return true;
 *         }
 *         if(ws>0){
 *         // do while循环 作用就是清除掉  无用的节点   ws>0 即Node.CANCELLED = 1 被取消了
 *              do{
 *                  node.prev=pred=pred.prev;
 *              }while(pred.waitStatus>0);
 *          pred.next=node;
 *        }else{
 *              // 走到这里就是说 你的状态是0    一开始状态都是 0
 *             // 然后要把前驱节点的 状态 由 ws 变成 -1 （-1代表 有责任唤醒后继节点） 然后下一次循环的时候 就是true了
 *              compareAndSetWaitStatus(pred,ws,Node.SIGNAL);
 *          }
 *              return false;
 *     }
 *
 *
 *       private final boolean parkAndCheckInterrupt() {
 *         //  所以就是对头线程节点 尝试了很多次还是没有获取锁  就会被park 住  等待前驱节点去unpark
 *         LockSupport.park(this);
 *         // 返回打断标记 并清除打断标记    这里是说 如果你这个线程在外面被打断了   此时还要进行一次自我打断
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
 *             // 释放之后 要唤醒后继节点  前提就是 头节点不为null 并且状态不等于0(等于 -1)
 *             if (h != null && h.waitStatus != 0)
 *                 unparkSuccessor(h);
 *             return true;
 *         }
 *         return false;
 *     }
 *
 *      头节点不为null 并且状态不等于0  就会来这里 进行解锁
 *      此时 被解锁的线程 如果是 队头节点  就会被唤醒  此时就会更新interrupted状态  就是parkAndCheckInterrupt 这个方法这里 继续循环
 *      然后 运行到 tryAcquire 这里  会获取到锁  此时会进入if块  重新设置哑节点  也就是头节点  相当关于在这里重新设置头节点 将原来等待的线程节点释放
 *      将原来的哑节点断开连接  将之前释放的锁的节点设置为哑节点
 *     private void unparkSuccessor(Node node) {
 *          int ws=node.waitStatus;
 *          if(ws< 0) {
 *              compareAndSetWaitStatus(node,ws,0);
 *          }
 *          Node s=node.next;
 *          // 若后继结点为空，或状态为CANCEL = 1（已失效），则从后尾部往前遍历找到最前的一个处于正常阻塞状态的结点进行唤醒，直到节点重合（即等于当前节点）
 *          if(s==null||s.waitStatus>0){
 *              s=null;
 *              for(Node t=tail;t!=null&&t!=node;t=t.prev) {
 *                  if(t.waitStatus<=0) {
 *                      s=t;
 *                  }
 *              }
 *          }
 *          if(s!=null) {
 *              // 存在下一个节点  并且该节点状态是-1  则唤醒下一个节点
 *              LockSupport.unpark(s.thread);
 *          }
 *      }
 *       //  为什么是 从尾部开始遍历？
 *  //                问题出在enq 方法中  在初始话节点的时候
 *      private Node enq(final Node node) {
 *         for (;;) {
 *             Node t = tail;
 *             if (t == null) { // Must initialize
 *                 if (compareAndSetHead(new Node()))
 *                     tail = head;
 *             } else {
 *                 node.prev = t;
 *                 if (compareAndSetTail(t, node)) {
 *                     t.next = node;
 *                     return t;
 *                 }
 *             }
 *         }
 *     }
 *     添加节点的时候 是cas操作  但是if语句中并没有线程安全的操作
 *     线程cas操作成功之后  要更新后继节点的时候  是会发生线程切换的  此时如果发生线程切换  该线程的后继几点还是null
 *     此时如果某个线程去执行 unparkSuccessor的时候  从头到尾遍历  就会漏掉节点。
 *     那为什么尾部可以列？  其最根本的原因在于：node.prev = t;先于CAS执行，也就是说，你在将当前节点置为尾部之前就已经把前驱节点赋值了，自然不会出现prev=null的情况
 *
 */


/**
 *
 *
 *           前提是获取到锁  所以是线程安全的操作  类似于  wait notify
 *           每个条件变量其实就对应着一个等待队列，其实现类是 ConditionObject
 *                 开始 Thread-0 持有锁，调用 await，进入 ConditionObject 的 addConditionWaiter 流程
 *                 创建新的 Node 状态为 -2（Node.CONDITION），关联 Thread-0，加入 等待队列尾部
 *
 *           public final void await() throws InterruptedException {
 *             // 标记有打断标记  抛出异常
 *             if (Thread.interrupted())
 *                 throw new InterruptedException();
 *
 *             //  创建一个新的节点加入condition队列中，节点状态为condition
 *             Node node = addConditionWaiter();
 *
 *             // 释放该线程持有的锁  并唤醒 同步队列中的下一个节点
 *             int savedState = fullyRelease(node);
 *
 *             int interruptMode = 0;
 *             while (!isOnSyncQueue(node)) {
 *                  // 当前线程 进行阻塞
 *                 LockSupport.park(this);
 *                 // 上面已经park了   如果此时 unpark  要判断是在哪种情况下unpark的
 *                 if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
 *                     break;
 *             }
 *             if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
 *                 interruptMode = REINTERRUPT;
 *             if (node.nextWaiter != null) // clean up if cancelled
 *                 unlinkCancelledWaiters();
 *             if (interruptMode != 0)
 *                 reportInterruptAfterWait(interruptMode);
 *         }
 *
 *
 *         private Node addConditionWaiter() {
 *             Node t = lastWaiter;
 *             // 最后一个节点是取消状态  清理掉
 *             if (t != null && t.waitStatus != Node.CONDITION) {
 *                 unlinkCancelledWaiters();
 *                 //指向 清理之后 状态是 CONDITION的节点
 *                 t = lastWaiter;
 *             }
 *             // 创建新的 wait节点  状态是 -2    单向链表
 *             Node node = new Node(Thread.currentThread(), Node.CONDITION);
 *             if (t == null)
 *                 firstWaiter = node;
 *             else
 *                 t.nextWaiter = node;
 *             lastWaiter = node;
 *             return node;
 *         }
 *
 *
 *         // 清理掉链表中的状态为cancelled状态的节点
 *         private void unlinkCancelledWaiters() {
 *              Node t = firstWaiter;
 *              Node trail = null;
 *              while (t != null) {
 *                  // 保存下一个节点
 *                  Node next = t.nextWaiter;
 *                  // 如果该节点的状态不等于conditon,则该节点需要在链表中删除
 *                  if (t.waitStatus != Node.CONDITION) {
 *                      // 断开连接
 *                      t.nextWaiter = null;
 *
 *                      if (trail == null)
 *                          firstWaiter = next;
 *                      else
 *                          trail.nextWaiter = next;
 *                      if (next == null)
 *                          lastWaiter = trail;
 *                  }
 *                  else
 *                      trail = t;
 *
 *                  t = next;
 *
 *          }
 *
 *         // 释放掉锁  因为可能加了多次锁（锁重入）  多以要
 *         final int fullyRelease(Node node) {
 *         boolean failed = true;
 *         try {
 *             int savedState = getState();
 *
 *             // 彻底释放锁，并且唤醒同步队列中的下一个线程
 *             if (release(savedState)) {
 *                 failed = false;
 *                 return savedState;
 *             } else {
 *                 throw new IllegalMonitorStateException();
 *             }
 *         } finally {
 *             if (failed)
 *                 node.waitStatus = Node.CANCELLED;
 *         }
 *     }
 *     // tryRelease 减为0  穿参的时候 传的就是加锁的次数 所以就直接减为0 了
 *     //  然后唤醒 等待队列中的 线程节点
 *     public final boolean release(int arg) {
 *         if (tryRelease(arg)) {
 *             Node h = head;
 *             if (h != null && h.waitStatus != 0)
 *                 unparkSuccessor(h);
 *             return true;
 *         }
 *         return false;
 *     }
 *
 *     // 判断当前节点是否在 同步队列 中，返回 false 表示不在，返回true 表示如果不在。   其实本质还是判断该节点有没有被 signal
 *     // 为什么要判断是否在 同步队列中？
 *     //     因为当线程调用signal或signalAll时,会从firstWaiter节点开始，将节点依次从 等待队列 中移除，并通过enq方法重新添加到 同步队列 中
 *                因此当其他线程调用signal或者signalAll方法时，该线程可能从条件（等待）队列中移除，并重新加入到同步队列中
 *                1. 如果没有，则阻塞当前线程，同时调用checkInterruptWhileWaiting检测当前线程在等待过程中是否发生中断，设置interruptMode表示中断状态。
 *                2. 如果isOnSyncQueue方法判断出当前线程已经处于同步队列中了，则跳出while循环
 *
 *
 *     final boolean isOnSyncQueue(Node node) {
 *           //  同步队列中的状态 不会是CONDITION   同步队列中的前驱节点 不会为null  因为有一个哑节点
 *          if (node.waitStatus == Node.CONDITION || node.prev == null)
 *              return false;
 *          // 在等待队列里，node.next 是等于空的，不等于空就是在同步队列当中  这里注意  prev和next专属于同步队列
 *          if (node.next != null) // If has successor, it must be on queue
 *              return true;
 *            // 否则就遍历整个 同步队列 判断是否在同步队列中
 *           return findNodeFromTail(node);
 *      }
 *
 *
 *      // checkInterruptWhileWaiting方法根据中断发生的时机返回后续需要处理这次中断的方式，如果发生中断，退出循环
 *      //  THROW_IE = -1  中断在 signalled之前   (在退出等待时抛出异常)
 *      //  REINTERRUPT = 1 再次中断在 signalled之后   (在退出等待时重新设置打断状态)
 *      //  0     没有中断
 *      private int checkInterruptWhileWaiting(Node node) {
 *          return Thread.interrupted() ?
 *              (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) : 0;
 *      }
 *
 *      final boolean transferAfterCancelledWait(Node node) {
 *           //  cas成功  则说明中断发生时，没有signal的调用，因为signal方法会将状态设置为0；此时加到同步队列中  返回true  表示中断在signal之前；
 *           //  cas失败  则判断该节点是否在同步队列中 如果不在 让步其他线程 直到当前的node已经被signal方法添加到同步队列中；
 *          if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
 *              enq(node);
 *              return true;
 *          }
 *           while(!isOnSyncQueue(node)) {
 *             Thread.yield();
 *           }
 *
 *          return false;
 *     }
 *
 *          //  signal 的主要作用就是 将等待队列中的节点 放到同步队列中  然后在同步队列中进行竞争。
 *          public final void signal() {
 *          //前提是持有锁 不然抛出异常
 *             if (!isHeldExclusively())
 *                 throw new IllegalMonitorStateException();
 *             Node first = firstWaiter;
 *             if (first != null)
 *                 doSignal(first);
 *         }
 *
 *         // 其实就是从 条件变量的队列中 断开 放到 同步队列队尾中
 *         // 这里只会唤醒一个  所以有   first.nextWaiter = null;  后续就会跳出循环
 *         private void doSignal(Node first) {
 *             do {
 *                 // 将头节点从 等待队列中 移除
 *                 if ( (firstWaiter = first.nextWaiter) == null) {
 *                    lastWaiter = null;
 *                 }
 *                 first.nextWaiter = null;
 *                 // 再调用transferForSignal()方法将节点添加到同步队列中:
 *             } while (!transferForSignal(first) &&
 *                      (first = firstWaiter) != null);
 *                      // 如果转移失败 因为可能会被打断 或 超时
 *                      // 如果还有下一个节点  就尝试唤醒下一个 节点
 *         }
 *
 *
 *         final boolean transferForSignal(Node node) {
 *              //  cas失败  说明已经被取消 继续循环 换一个节点
 *              //  如果成功之后 被取消   那也没关系  后面获取锁的时候 依旧会被移除掉
 *              if(!compareAndSetWaitStatus(node,Node.CONDITION,0)) {
 *                  return false;
 *              }
 *              // 连接到 同步队列 队尾  返回其前驱节点
 *              Node p=enq(node);
 *              int ws=p.waitStatus;
 *              //  ws大于0 也就是取消了   或者  cas 改为 -1失败   执行unpark
 *              if(ws>0||!compareAndSetWaitStatus(p,ws,Node.SIGNAL)) {
 *                  LockSupport.unpark(node.thread);
 *              }
 *              return true;
 *          }
 *
 *
 *
 *
 *          // 主要是 公平锁 会使用     判断队列中 是否有等待的线程
 *          //  返回true  表示有   所以要进行排队
 *          //  返回false  表示没有
 *          public final boolean hasQueuedPredecessors() {
 *              Node t = tail; // Read fields in reverse initialization order
 *              Node h = head;
 *              Node s;
 *              return h != t && ((s = h.next) == null || s.thread != Thread.currentThread());
 *              // 返回false的情况
 *              //      1. h != t 返回false 即 h == t 等于null是空链表   无需等待
 *              //      2. h != t 返回true  说明头尾指向头一个节点  即只有一个节点。 也不需要排队  因为第一个节点持有同步状态, 不参与排队。 如果是第三个节点就只能等待前面的节点释放同步状态。
 *                              // 2.1 即(s = h.next) == null返回false 以及 s.thread != Thread.currentThread()返回false
 *                              // 2.2 (s = h.next) == null返回false 即有等待的节点
 *                              // 2.3 s.thread != Thread.currentThread()返回false  即 当前线程和等待线程是相同的    不相同自然就得排队
 *     }
 *
 */