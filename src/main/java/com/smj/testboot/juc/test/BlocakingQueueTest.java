package com.smj.testboot.juc.test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BlocakingQueueTest {







}

class ThreadPool {

    private int coreSize;

    private BlockingQueue<Runnable> takeQueue;

    private HashSet<Worker> workers = new HashSet();

    private long timeout;

    private TimeUnit unit;

    public ThreadPool(int coreSize, BlockingQueue<Runnable> takeQueue, long timeout, TimeUnit unit) {
        this.coreSize = coreSize;
        this.takeQueue = takeQueue;
        this.timeout = timeout;
        this.unit = unit;
    }

    public void execute(Runnable task) {
        // 有线程直接执行 没有就放到队列中
        synchronized (workers) {

            if (coreSize > workers.size()) {
                // 得到一个线程
                Worker worker = new Worker(task);
                // 放到线程池中
                workers.add(worker);
                // 执行这个线程
                worker.start();
            }
        }
    }




    class Worker extends Thread{

        private Runnable task;

        public Worker(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            // 有任务就执行  没有就从队列中获取任务执行
            while (task != null || (task = takeQueue.take()) != null) {
                try {
                    task.run();
                } finally {
                    task = null;
                }
            }

            synchronized (workers) {
                workers.remove(this);
            }
        }
    }



}


class BlockingQueue<T> {


    // 阻塞队列
    private Deque<T> queue = new ArrayDeque<>();

    //锁
    private ReentrantLock lock = new ReentrantLock();

    // 条件变量
    Condition full = lock.newCondition();
    Condition empty = lock.newCondition();

    // 容量
    private int capacity;

    public BlockingQueue(int capacity) {
        this.capacity = capacity;
    }


    // 取操作
    public T take() {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                try {
                    empty.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            T t = queue.removeFirst();
            full.signal();
            return t;

        } finally {
            lock.unlock();
        }
    }

    // 取操作  超时
    public T take(long timeout, TimeUnit unit) {
        lock.lock();
        try {

            long nanos = unit.toNanos(timeout);
            while (queue.isEmpty()) {
                try {
                    if (nanos <= 0) {
                        return null;
                    }
                    nanos = empty.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            T t = queue.removeFirst();
            full.signal();
            return t;

        } finally {
            lock.unlock();
        }
    }


    // 放操作
    public void put(T task) {
        lock.lock();

        try {
            while (queue.size() == capacity) {
                try {
                    full.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            queue.addLast(task);
            empty.signal();
        } finally {
            lock.unlock();
        }

    }


    // 放操作
    public boolean offer(T task, long timeout, TimeUnit unit) {
        lock.lock();

        try {
            long nanos = unit.toNanos(timeout);
            while (queue.size() == capacity) {
                try {
                    if (nanos <= 0) {
                        return false;
                    }
                    nanos = full.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            queue.addLast(task);
            empty.signal();
            return true;
        } finally {
            lock.unlock();
        }

    }

    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

}




