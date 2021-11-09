package com.smj.testboot.juc.thread_pool;


import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


@Slf4j(topic = "c.ThreadHungry")
public class ThreadHungry {

    static final List<String> MENU = Arrays.asList("地三鲜", "宫保鸡丁", "辣子鸡丁", "烤鸡翅");

    static Random RANDOM = new Random();

    static String cooking() {
        return MENU.get(RANDOM.nextInt(MENU.size()));
    }

    public static void main(String[] args) {



//        ExecutorService executorService = Executors.newFixedThreadPool(2);
        ExecutorService waitPool = Executors.newFixedThreadPool(2);
        ExecutorService cookPool = Executors.newFixedThreadPool(2);


        // 此时就会有饥饿问题  --- 线程数量不够  两个线程都去点餐了 没人做饭了 所以就堵塞住了
        // 解决办法  不同任务类型应该使用不同的线程池，这样能够避免饥饿，并能提升效率

        waitPool.execute(() -> {
            log.debug("处理点餐...");
            Future<String> f = cookPool.submit(() -> {
                log.debug("做菜");
                return cooking();
            });
            try {
                log.debug("上菜: {}", f.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        waitPool.execute(() -> {
        log.debug("处理点餐...");
            Future<String> f = cookPool.submit(() -> {
                log.debug("做菜");
                return cooking();
            });
            try {
                log.debug("上菜: {}", f.get());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
        });
    }
}