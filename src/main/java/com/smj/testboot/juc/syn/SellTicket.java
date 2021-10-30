package com.smj.testboot.juc.syn;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;

/**
 * for /L %n in (1,1,10) do java -cp ".;C:\Users\manyh\.m2\repository\ch\qos\logback\logbackclassic\1.2.3\logback-classic-1.2.3.jar;C:\Users\manyh\.m2\repository\ch\qos\logback\logbackcore\1.2.3\logback-core-1.2.3.jar;C:\Users\manyh\.m2\repository\org\slf4j\slf4japi\1.7.25\slf4j-api-1.7.25.jar" cn.itcast.n4.exercise.ExerciseSell
 * 测试脚本  可以看下
 */






@Slf4j(topic = "c.SellTicket")
public class SellTicket {


    public static void main(String[] args) {
        Window window = new Window(1000);
        List<Thread> list = new ArrayList<>();

        List<Integer> sellCount = new Vector<>();
        for (int i = 0; i < 2000; i++) {
            Thread thread = new Thread(() -> {
                // 竞态条件 --- 多个线程 对共享变量 有读写操作
                // 这里 共享变量就是 票数count
                int count = window.sell(randomAmount());
                // 卖出的票数
                sellCount.add(count);
            });
        }
        // 主线程要 等待所有线程执行完毕
        list.forEach( thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // 买出去的票求和
        log.debug("selled count:{}",sellCount.stream().mapToInt(c -> c).sum());
        // 剩余票数
        log.debug("remainder count:{}", window.getCount());

    }

    // Random 为线程安全
    static Random random = new Random();
    // 随机 1~5
    public static int randomAmount() {
        return random.nextInt(5) + 1;
    }

}


class Window {

    private int count;

    public Window(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    // 标准做法  在这里加一个  synchronized 即可。 锁住共享变量
    public int sell(int amount) {
        if (this.count >= amount) {
            this.count -= amount;
            return amount;
        } else {
            return 0;
        }
    }


}
