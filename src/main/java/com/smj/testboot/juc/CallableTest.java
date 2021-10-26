package com.smj.testboot.juc;


import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

@Slf4j(topic = "c.CallableTest")
public class CallableTest {

    public static void main(String[] args) {

        FutureTask<Integer> futureTask = new FutureTask<>(() -> {
            log.info("running");
            Thread.sleep(1000);
            return 100;
        });


        Thread thread = new Thread(futureTask, "t1");
        thread.start();

        try {
            // 主线程的get方法就会等待  等待到 拿到结果
            Integer integer = futureTask.get();
            log.info("{}", integer);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }


    }
}
