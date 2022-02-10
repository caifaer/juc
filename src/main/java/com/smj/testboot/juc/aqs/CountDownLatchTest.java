package com.smj.testboot.juc.aqs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;


@Slf4j(topic = "c.CountDownLatchTest")
public class CountDownLatchTest {

    /**
     *   CountDownLatch的作用很简单，就是一个或者一组线程在开始执行操作之前，必须要等到其他线程执行完才可以。
     *   我们举一个例子来说明，在考试的时候，老师必须要等到所有人交了试卷才可以走。此时老师就相当于等待线程，而学生就好比是执行的线程。
     *
     *   java中还有一个同步工具类叫做CyclicBarrier，他的作用和CountDownLatch类似。同样是等待其他线程都完成了，才可以进行下一步操作.
     *   我们再举一个例子，在打王者的时候，在开局前所有人都必须要加载到100%才可以进入。否则所有玩家都相互等待。
     *
     *   我们看一下区别：
     *      CountDownLatch: 一个线程(或者多个)， 等待另外N个线程完成某个事情之后才能执行。
     *      CyclicBarrier : N个线程相互等待，任何一个线程完成之前，所有的线程都必须等待。
     *
     * 关键点其实就在于那N个线程
     *
     * （1）CountDownLatch里面N个线程就是学生，学生做完了试卷就可以走了，不用等待其他的学生是否完成
     *
     * （2）CyclicBarrier 里面N个线程就是所有的游戏玩家，一个游戏玩家加载到100%还不可以，必须要等到其他的游戏玩家都加载到100%才可以开局
     *
     *
     *    涉及到的方法:
     *     // 其实就是 共享锁的加锁操作
     *     public void await() throws InterruptedException {
     *         sync.acquireSharedInterruptibly(1);
     *     }
     *     public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
     *          if (Thread.interrupted())
     *              throw new InterruptedException();
     *          if (tryAcquireShared(arg) < 0)
     *              doAcquireSharedInterruptibly(arg);
     *      }
     *
     *      //  就是看 状态是否是0
     *      //  等于0  就说明所有线程都执行完了  返回1  然后去获取锁
     *      //  不等于 就说明还有线程再执行   获取锁失败 就入同步队列自旋  自旋不到就阻塞  等待被唤醒
     *      protected int tryAcquireShared(int acquires) {
     *             return (getState() == 0) ? 1 : -1;
     *      }
     *
     *
     *      共享值减1
     *      其实就是共享锁的 释放锁操作。
     *       public void countDown() {
     *            sync.releaseShared(1);
     *       }
     *
     *       public final boolean releaseShared(int arg) {
     *          if (tryReleaseShared(arg)) {
     *              doReleaseShared();
     *              return true;
     *          }
     *          return false;
     *      }
     *
     *      //  判断 共享值 是否是0
     *      // 减到0  就说明没有线程执行了 此时可以解锁  然后去唤醒下一个节点
     *      // 原本就是0   返回false
     *       protected boolean tryReleaseShared(int releases) {
     *     // Decrement count; signal when transition to zero
     *          for (;;) {
     *               int c = getState();
     *               if (c == 0)
     *                   return false;
     *               int nextc = c-1;
     *               if (compareAndSetState(c, nextc))
     *                   return nextc == 0;
     *          }
     *      }
     *
     *
     *
     */














    static CountDownLatch countDownLatch = new CountDownLatch(2);

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        System.out.println("全班同学开始考试：一共两个学生");
        new Thread(() -> {
            System.out.println("第一个学生交卷，countDownLatch减1");
            countDownLatch.countDown();
        }).start();
        new Thread(() -> {
            System.out.println("第二个学生交卷，countDownLatch减1");
            countDownLatch.countDown();
        }).start();

        //这里也就是说  只有当countDownLatch从2 减为0 的时候  主线程才能继续运行  否则就堵塞。
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("老师清点试卷，在此之前，只要一个学生没交，"
                + "countDownLatch不为0，不能离开考场");

//        test3();
    }

    private static void test5() {
        CountDownLatch latch = new CountDownLatch(3);
        ExecutorService service = Executors.newFixedThreadPool(4);
        service.submit(() -> {
            log.debug("begin...");
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            latch.countDown();
            log.debug("end...{}", latch.getCount());
        });
        service.submit(() -> {
            log.debug("begin...");
            try {
                sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            latch.countDown();
            log.debug("end...{}", latch.getCount());
        });
        service.submit(() -> {
            log.debug("begin...");
            try {
                sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            latch.countDown();
            log.debug("end...{}", latch.getCount());
        });
        service.submit(()->{
            try {
                log.debug("waiting...");
                latch.await();
                log.debug("wait end...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private static void test4() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);

        new Thread(() -> {
            log.debug("begin...");
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            latch.countDown();
            log.debug("end...{}", latch.getCount());
        }).start();

        new Thread(() -> {
            log.debug("begin...");
            try {
                sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            latch.countDown();
            log.debug("end...{}", latch.getCount());
        }).start();

        new Thread(() -> {
            log.debug("begin...");
            try {
                sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            latch.countDown();
            log.debug("end...{}", latch.getCount());
        }).start();

        log.debug("waiting...");
        latch.await();
        log.debug("wait end...");
    }

    private static void test3() throws InterruptedException, ExecutionException {
        RestTemplate restTemplate = new RestTemplate();
        log.debug("begin");
        ExecutorService service = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(4);
        Future<Map<String,Object>> f1 = service.submit(() -> {
            Map<String, Object> response = restTemplate.getForObject("http://localhost:8080/order/{1}", Map.class, 1);
            return response;
        });
        Future<Map<String, Object>> f2 = service.submit(() -> {
            Map<String, Object> response1 = restTemplate.getForObject("http://localhost:8080/product/{1}", Map.class, 1);
            return response1;
        });
        Future<Map<String, Object>> f3 = service.submit(() -> {
            Map<String, Object> response1 = restTemplate.getForObject("http://localhost:8080/product/{1}", Map.class, 2);
            return response1;
        });
        Future<Map<String, Object>> f4 = service.submit(() -> {
            Map<String, Object> response3 = restTemplate.getForObject("http://localhost:8080/logistics/{1}", Map.class, 1);
            return response3;
        });

        // 需要返回结果时  使用future 比较合适
        System.out.println(f1.get());
        System.out.println(f2.get());
        System.out.println(f3.get());
        System.out.println(f4.get());
        log.debug("执行完毕");
        service.shutdown();
    }

    private static void test2() throws InterruptedException {
        AtomicInteger num = new AtomicInteger(0);
        ExecutorService service = Executors.newFixedThreadPool(10, (r) -> {
            return new Thread(r, "t" + num.getAndIncrement());
        });
        CountDownLatch latch = new CountDownLatch(10);
        String[] all = new String[10];
        Random r = new Random();
        for (int j = 0; j < 10; j++) {
            int x = j;
            service.submit(() -> {
                for (int i = 0; i <= 100; i++) {
                    try {
                        sleep(r.nextInt(100));
                    } catch (InterruptedException e) {
                    }
                    all[x] = Thread.currentThread().getName() + "(" + (i + "%") + ")";
                    // 这个操作 太秀了
                    System.out.print("\r" + Arrays.toString(all));
                }
                latch.countDown();
            });
        }
        latch.await();
        System.out.println("\n游戏开始...");
        service.shutdown();
    }

}
