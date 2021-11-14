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
     *
     * CountDownLatch的计数器只能使用一次，而CyclicBarrier的计数器可以使用reset()方法重置，
     * 可以使用多次，所以CyclicBarrier能够处理更为复杂的场景；
     * @param args
     */
    public static void main(String[] args) {
        ExecutorService service = Executors.newFixedThreadPool(2);
        // 个数为2时才会继续执行
        CyclicBarrier barrier = new CyclicBarrier(2, ()-> {
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
