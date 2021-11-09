package com.smj.testboot.juc.thread_pool;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


@Slf4j(topic = "c.TestPool")
class TestPool {

    public static void main(String[] args) {

//        ThreadPool threadPool = new ThreadPool(2, 1000, TimeUnit.MILLISECONDS, 10);

//
//        for (int i = 0; i <= 5; i++) {
//            int j = i;
//            threadPool.execute(() -> {
//                log.info("{}", j);
//            });
//        }

        // 任务队列满了  此时就会被阻塞住   put方法就会被阻塞住  此时就可以有拒绝策略
//        for (int i = 0; i <= 15; i++) {
//            int j = i;
//            threadPool.execute(() -> {
//                try {
//                    Thread.sleep(100000L);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                log.info("{}", j);
//            });
//        }


        // 此时就有 拒绝策略 --  采用策略模式
        // 1. 死等
        // 2) 带超时等待
        // 3) 让调用者放弃任务执行
        // 4) 让调用者抛出异常
        // 5) 让调用者自己执行任务
        ThreadPool threadPool = new ThreadPool(1, 1000, TimeUnit.MILLISECONDS, 1,
                //1. 死等
//                ((queue, task) -> { queue.put(task)})
                //2. 超时
//                   ((queue, task) -> {
//                       queue.offer(task, 1500, TimeUnit.MILLISECONDS);})

                // 3) 让调用者放弃任务执行
//                ((queue, task) -> { log.info("放弃{}", task);})

                // 4) 让调用者抛出异常 -- 剩下的任务就不执行
//                ((queue, task) -> {
//                    throw new RuntimeException("任务执行失败" + task);})
                // 5) 让调用者自己执行任务
                ((queue, task) -> { task.run();})


        );



        for (int i = 0; i < 3; i++) {
            int j = i;
            threadPool.execute(() -> {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                log.info("{}", j);
            });
        }


    }
}

// 拒绝策略接口
@FunctionalInterface
interface RejectPolicy<T> {

    void reject(BlockingQueue<T> queue, T task);

}



@Slf4j(topic = "c.ThreadPool")
public class ThreadPool {

    // 任务队列
    private BlockingQueue<Runnable> takeQueue;

    // 线程集合
    private HashSet<Worker> works = new HashSet();

    // 核心线程数
    private int coreSize;

    // 获取任务的超时时间
    private long timeout;

    private TimeUnit timeUnit;

    private RejectPolicy<Runnable> rejectPolicy;

    public ThreadPool(int coreSize, long timeout, TimeUnit timeUnit, int queueCapacity, RejectPolicy<Runnable> rejectPolicy) {
        this.takeQueue = new BlockingQueue<>(queueCapacity);
        this.coreSize = coreSize;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        this.rejectPolicy = rejectPolicy;
    }

    // 执行任务
    public void execute(Runnable task) {

        // 当任务数没有超过coreSize 直接交给worker执行  否则就放到队列中
        synchronized (works) {
            if (works.size() < coreSize) {
                Worker worker = new Worker(task);
                log.debug("新增 worker{}, {}", worker, task);
                works.add(worker);
                worker.start();
            } else {
                // 放到队列中
//                takeQueue.put(task);

                // 最终是在这里 体现的 策略
                // 1. 死等
                // 2) 带超时等待
                // 3) 让调用者放弃任务执行
                // 4) 让调用者抛出异常
                // 5) 让调用者自己执行任务
                takeQueue.tryPut(rejectPolicy, task);
            }
        }


    }

    class Worker extends Thread {
        private Runnable task;

        public Worker(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {

            // 执行任务
            //   1. task不为空  执行
            //   2. 执行完毕  到队列中获取任务执行
//            while (task != null || (task = takeQueue.take()) != null) {  //这里会被一直阻塞住
            while (task != null || (task = takeQueue.pool(timeout, timeUnit)) != null) {
                try {
                    log.debug("正在执行...{}", task);
                    task.run();
                } finally {
                    task = null;
                }
            }
            synchronized (works) {
                log.debug("worker 被移除{}", this);
                works.remove(this);
            }
        }
    }

}


@Slf4j(topic = "c.BlockingQueue")
class BlockingQueue<T> {

    // 任务队列
    private Deque<T> queue = new ArrayDeque<>();
    // 锁
    private ReentrantLock lock = new ReentrantLock();
    // 生产者条件变量
    private Condition fullWaitSet = lock.newCondition();
    // 消费者条件变量
    private Condition emptyWaitSet = lock.newCondition();
    // 容量
    private int capacity;

    public BlockingQueue(int capacity) {
        this.capacity = capacity;
    }




    //阻塞获取 -- 超时等待
    public T pool(long timeout, TimeUnit timeUnit) {
        lock.lock();
        try {
            // 转换成 纳秒
            long nanos = timeUnit.toNanos(timeout);
            while (queue.isEmpty()) {
                try {
                    // 这里这样写的原因是  可能会有虚假唤醒  所以你再次循环的时候 可能会多等待
                    // 测试你要知道 这个方法的返回值的含义 -- 返回的就是 剩余的时间 -- 如果在nanosTimesout之前唤醒，那么返回值 = nanosTimeout - 消耗时间。
                    // 所以直接覆盖掉原本的 nanos就好了
                    if (nanos <= 0) {
                        return null;
                    }
                    nanos = emptyWaitSet.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            T t = queue.removeFirst();
            fullWaitSet.signal();
            return t;
        } finally {
            lock.unlock();
        }
    }


    //阻塞获取
    public T take() {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                try {
                     emptyWaitSet.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            T t = queue.removeFirst();
            fullWaitSet.signal();
            return t;
        } finally {
            lock.unlock();
        }
    }

    // 阻塞添加
    public void put(T task) {
        lock.lock();
        try {
            while (queue.size() == capacity) {
                try {
                    log.debug("等待加入任务队列 {} ...", task);
                    fullWaitSet.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            log.debug("加入任务队列 {}", task);
            queue.addLast(task);
            emptyWaitSet.signal();
        } finally {
            lock.unlock();
        }
    }

    // 阻塞添加 --- 超时时间
    public boolean offer(T task, long timeout, TimeUnit unit) {
        lock.lock();
        try {
            long nanos = unit.toNanos(timeout);
            while (queue.size() == capacity) {
                try {
                    log.debug("等待加入任务队列 {} ...", task);
                    if (nanos <= 0) {
                        return false;
                    }
                    nanos = fullWaitSet.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            log.debug("加入任务队列 {}", task);
            queue.addLast(task);
            emptyWaitSet.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    // 获取大小
    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    public void tryPut(RejectPolicy<T> rejectPolicy, T task) {
        lock.lock();
        try {
            // 判断队列是否 已满
            if (queue.size() == capacity) {
                // 最终还是 实现类来做
                rejectPolicy.reject(this, task);
            } else {
                log.debug("加入任务队列 {}", task);
                queue.addLast(task);
                emptyWaitSet.signal();
            }



        } finally {
            lock.unlock();

        }

    }
}