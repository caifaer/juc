package com.smj.testboot.juc.aqs;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

import static java.lang.Thread.sleep;

@Slf4j(topic = "c.TestCyclicBarrier")
public class TestCyclicBarrier {

    /**
     * CyclicBarrier
     * 循环栅栏，用来进行线程协作，等待线程满足某个计数。构造时设置『计数个数』，每个线程执
     * 行到某个需要“同步”的时刻调用 await() 方法进行等待，当等待的线程数满足『计数个数』时，继续执行
     * <p>
     * CountDownLatch的计数器只能使用一次，而CyclicBarrier的计数器可以使用reset()方法重置，
     * 可以使用多次，所以CyclicBarrier能够处理更为复杂的场景；
     * <p>
     * CountDownLatch的作用很简单，就是一个或者一组线程在开始执行操作之前，必须要等到其他线程执行完才可以。
     * 我们举一个例子来说明，在考试的时候，老师必须要等到所有人交了试卷才可以走。此时老师就相当于等待线程，而学生就好比是执行的线程。
     * java中还有一个同步工具类叫做CyclicBarrier，他的作用和CountDownLatch类似。同样是等待其他线程都完成了，才可以进行下一步操作.
     * 我们再举一个例子，在打王者的时候，在开局前所有人都必须要加载到100%才可以进入。否则所有玩家都相互等待。
     * 我们看一下区别：
     * CountDownLatch: 一个线程(或者多个)， 等待另外N个线程完成某个事情之后才能执行。
     * CyclicBarrier : N个线程相互等待，任何一个线程完成之前，所有的线程都必须等待。
     * 关键点其实就在于那N个线程
     * （1）CountDownLatch里面N个线程就是学生，学生做完了试卷就可以走了，不用等待其他的学生是否完成
     * （2）CyclicBarrier 里面N个线程就是所有的游戏玩家，一个游戏玩家加载到100%还不可以，必须要等到其他的游戏玩家都加载到100%才可以开局
     * <p>
     * <p>
     * CyclicBarrier
     * Barrier翻译过来就是 栅栏，  在全部线程都没到达栅栏时 都给我阻塞。 只有全部到达栅栏处  栅栏才会放行
     * CyclicBarrier 就是循环栅栏 这就是这个类的高明之处  可以重置  也就是设置多个栅栏  通过了栅栏A  然后通过栅栏B  等等。 循环使用
     * 可以想想 进去游戏的进度条。   一个人到达百分之百没啥用   得全部到才可以进入游戏。
     * <p>
     * <p>
     * <p>
     * 源码  就是混合  ReentrantLock 和 Condition
     * 1. 构造方法
     * // 初始化 --- 其参数parties 表示 屏障 拦截的线程数量。
     * // 每个线程使用 await()方法告诉CyclicBarrier我已经到达了屏障，然后当前线程被阻塞。
     * public CyclicBarrier(int parties) {
     * this(parties, null);
     * }
     * 这个构造方法是说  当到达屏障时 会优先执行 barrierAction任务
     * public CyclicBarrier(int parties, Runnable barrierAction) {
     * if (parties <= 0) throw new IllegalArgumentException();
     * this.parties = parties;
     * this.count = parties;
     * this.barrierCommand = barrierAction;
     * }
     * <p>
     * <p>
     * 2. await方法
     * 调用await方法的线程告诉CyclicBarrier自己已经到达同步点，然后当前线程被阻塞。
     * BrokenBarrierException 表示栅栏已经被破坏，破坏的原因可能是其中一个线程 await() 时被 中断或者超时
     * <p>
     * public int await() throws InterruptedException, BrokenBarrierException {
     * try {
     * // 不超时等待
     * return dowait(false, 0L);
     * } catch (TimeoutException toe) {
     * throw new Error(toe); // cannot happen
     * }
     * }
     * <p>
     * // 核心方法  --- 等待
     * private int dowait(boolean timed, long nanos) throws InterruptedException, BrokenBarrierException,TimeoutException {
     * <p>
     * // 获取独占锁
     * final ReentrantLock lock = this.lock;
     * lock.lock();
     * try {
     * // 当前代
     * final Generation g = generation;
     * // 如果这代损坏了，抛出异常
     * if (g.broken)
     * throw new BrokenBarrierException();
     * <p>
     * // 如果线程中断了，抛出异常
     * if (Thread.interrupted()) {
     * // 1. 将损坏状态设置为true
     * // 2. 唤醒所有被拦截的线程 signalAll
     * // 3. 抛出异常
     * breakBarrier();
     * throw new InterruptedException();
     * }
     * <p>
     * // 获取下标
     * int index = --count;
     * // 如果是 0，说明最后一个线程调用了该方法
     * //  此时则需唤醒所有线程并转换到下一代
     * if (index == 0) {  // tripped
     * boolean ranAction = false;
     * try {
     * final Runnable command = barrierCommand;
     * // 唤醒所有线程前 先执行指定的任务
     * if (command != null)
     * command.run();
     * ranAction = true;
     * // 更新一代，将count重置，将generation重置
     * // 1. 唤醒所有线程
     * // 2. 重置count parties
     * // 3. 转移到下一代   就比如游戏进入了下一局
     * nextGeneration();
     * return 0;
     * } finally {
     * // 如果执行栅栏任务的时候失败了，就将损坏状态设置为true
     * if (!ranAction)
     * breakBarrier();
     * }
     * }
     * <p>
     * //  不为0  即还有线程没有到达栅栏处
     * for (;;) {
     * try {
     * // 如果没有时间限制，则直接等待，直到被唤醒
     * // 这里 等待的过程 也是有可能被唤醒的 此时就会被抛出异常
     * if (!timed)
     * trip.await();
     * // 如果有时间限制，则等待指定时间
     * else if (nanos > 0L)
     * nanos = trip.awaitNanos(nanos);
     * } catch (InterruptedException ie) {
     * // 若当前线程在等待期间被中断 则打翻栅栏唤醒其他线程
     * if (g == generation && ! g.broken) {
     * // 让栅栏失效
     * breakBarrier();
     * throw ie;
     * } else {
     * // 上面条件不满足，说明这个线程不是这代的
     * // 就不会影响当前这代栅栏的执行，所以，就打个中断标记
     * Thread.currentThread().interrupt();
     * }
     * }
     * <p>
     * // 上述的判断意味着  一个被中断  全盘中断  直接游戏结束
     * //  对于后续 就要判断 其他线程是因为哪种方式被中断了
     * 1. 因为调用breakBarrier方法而被唤醒，如果是则抛出异常
     * 2. 是否是正常的换代操作而被唤醒，如果是则返回计数器的值；
     * 3. 是否因为超时而被唤醒，如果是的话就调用breakBarrier打破栅栏并抛出异常。
     * <p>
     * <p>
     * // 1. 当有任何一个线程中断了，就会调用breakBarrier方法.就会唤醒其他的线程，其他线程醒来后，也要抛出异常
     * if (g.broken)
     * throw new BrokenBarrierException();
     * <p>
     * // g != generation表示正常换代了，返回当前线程所在栅栏的下标
     * // 如果 g == generation，说明还没有换代，那为什么会醒了？
     * // 因为一个线程可以使用多个栅栏，当别的栅栏唤醒了这个线程，就会走到这里，所以需要判断是否是当前代。
     * // 正是因为这个原因，才需要generation来保证正确。
     * if (g != generation)
     * return index;
     * <p>
     * // 如果有时间限制，且时间小于等于0，销毁栅栏并抛出异常
     * if (timed && nanos <= 0L) {
     * breakBarrier();
     * throw new TimeoutException();
     * }
     * }
     * } finally {
     * // 释放独占锁
     * lock.unlock();
     * }
     * }
     * <p>
     * dowait(boolean, long)方法的主要逻辑处理比较简单，如果该线程不是最后一个调用await方法的线程，则它会一直处于等待状态，除非发生以下情况：
     * 1. 最后一个线程到达，即index == 0
     * 2. 某个参与线程等待超时
     * 3. 某个参与线程被中断
     * 4. 调用了CyclicBarrier的reset()方法。该方法会将屏障重置为初始状态
     */
    public static void main(String[] args) {
        ExecutorService service = Executors.newFixedThreadPool(2);
        // 个数为2时才会继续执行
        CyclicBarrier barrier = new CyclicBarrier(2, () -> {
            log.debug("task1, task2 finish...");
        });
        // 是可以做到复用的  但是这里要注意 线程池中的线程数 和  计数个数 最好一致
        for (int i = 0; i < 3; i++) { // task1  task2  task1
            service.submit(() -> {
                log.debug("task1 begin...");
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    barrier.await(); // 2-1=1
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            });
            service.submit(() -> {
                log.debug("task2 begin...");
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    barrier.await(); // 1-1=0
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            });
        }
        service.shutdown();

    }

    private static void test1() {
        ExecutorService service = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 3; i++) {
            // 每次都得创建一个对象  无法复用
            CountDownLatch latch = new CountDownLatch(2);
            service.submit(() -> {
                log.debug("task1 start...");
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                latch.countDown();
            });
            service.submit(() -> {
                log.debug("task2 start...");
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                latch.countDown();
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.debug("task1 task2 finish...");
        }
        service.shutdown();
    }
}
