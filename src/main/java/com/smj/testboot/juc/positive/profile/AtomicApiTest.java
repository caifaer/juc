package com.smj.testboot.juc.positive.profile;


import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.function.IntUnaryOperator;

@Slf4j(topic = "c.AtomicApiTest")
public class AtomicApiTest {


    public static void main(String[] args) throws InterruptedException {


        stamped();


    }


    public static void add() {
        AtomicInteger i = new AtomicInteger(0);

        System.out.println(i.incrementAndGet()); // 等价于 ++i   1
        System.out.println(i.getAndIncrement()); // i++   2

        System.out.println(i.getAndAdd(5)); // 2 , 7
        System.out.println(i.addAndGet(5)); // 12, 12

    }
    public static void update() {

        AtomicInteger i = new AtomicInteger(5);

        //  接收一个函数式接口  其实就是一个表达式  然后 对入参进行修改
        //              读取到    设置值
        i.updateAndGet(value -> value * 10);

        // 等价于上面 底层原理
        updateAndGet(i, value -> value * 10);

    }

    private static void updateAndGet(AtomicInteger atomicInteger, IntUnaryOperator updateFunction) {
        while (true) {
            // 获取到当前值
            int prev = atomicInteger.get();
            // 但是这里你要知道  这个操作你是写死了 要有其他的操作 就得再写一个方法 所以使用lambda将操作参数化
            int next = updateFunction.applyAsInt(prev);
            if (atomicInteger.compareAndSet(prev, next)) {
                break;
            }
        }
    }

    // 原子引用 支持泛型 其实跟上面的差不多
    public static void reference() {

        AtomicReference<BigDecimal> atomicReference = new AtomicReference("1000");

        while (true) {
            // 获取到当前值
            BigDecimal prev = atomicReference.get();
            // 但是这里你要知道  这个操作你是写死了 要有其他的操作 就得再写一个方法 所以使用lambda将操作参数化
            BigDecimal next = prev.subtract(prev);
            if (atomicReference.compareAndSet(prev, next)) {
                break;
            }
        }

    }



    static AtomicReference<String> ref = new AtomicReference("A");

    public static void ABA() throws InterruptedException {

        // 先来看 ABA 问题
        // 线程在cas过程中 能否知道 该值有没有被其他线程修改过
        // 因为 假如你原来的值 是A 后来线程b 修改为了B 线程c又修改为了A  此时当前线程时无法感知的。
        String prev = ref.get();
        other();
        Thread.sleep(1);
        // 尝试改为 C
        log.debug("change A->C {}", ref.compareAndSet(prev, "C"));// 会输出 C


    }

    private static void other() {
        new Thread(() -> {
            log.debug("change A->B {}", ref.compareAndSet(ref.get(), "B"));
        }, "t1").start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            log.debug("change B->A {}", ref.compareAndSet(ref.get(), "A"));
        }, "t2").start();
    }


    static AtomicStampedReference<String> stamped = new AtomicStampedReference("A", 0);

    // 此时就可以 加上版本号来处理  可以使用AtomicStampedReference;
    // 不仅比较值 还要比较版本号
    public static void stamped() throws InterruptedException {

        String prev = stamped.getReference();
        //获取版本号
        int stamp = stamped.getStamp();
        log.info("{}", stamp);
        otherStamped();
        Thread.sleep(1);
        // 尝试改为 C   传入版本号 以及版本号+1
        log.debug("change A->C {}", stamped.compareAndSet(prev, "C", stamp, stamp + 1));


    }

    private static void otherStamped() {
        new Thread(() -> {
            int stamp = stamped.getStamp();
            log.info("{}", stamp);
            log.debug("change A->B {}", stamped.compareAndSet(stamped.getReference(), "B", stamp, stamp + 1));
        }, "t1").start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            int stamp = stamped.getStamp();
            log.info("{}", stamp);
            log.debug("change B->A {}", stamped.compareAndSet(stamped.getReference(), "A", stamp, stamp + 1));
        }, "t2").start();
    }


    /**
     * AtomicStampedReference 可以给原子引用加上版本号，追踪原子引用整个的变化过程，
     * 如： A -> B -> A ->C ，
     * 通过AtomicStampedReference，我们可以知道，引用变量中途被更改了几次。
     * 但是有时候，并不关心引用变量更改了几次，只是单纯的关心是否更改过，所以就有了
     * AtomicMarkableReference
     *
     * 比较值 以及比较布尔值
     */







}
