package com.smj.testboot.juc.positive.profile;


import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

@Slf4j(topic = "c.AtomicApiTest")
public class AtomicApiTest {


    public static void main(String[] args) {




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

}
