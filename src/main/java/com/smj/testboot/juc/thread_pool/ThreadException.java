package com.smj.testboot.juc.thread_pool;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


@Slf4j(topic = "c.ThreadException")
public class ThreadException {


    public static void main(String[] args) {

        ExecutorService executorService = Executors.newFixedThreadPool(1);

        executorService.execute(() -> {
            log.info("{}", 123);
            try {
                // 可以通过 catch 自己处理异常
                int i = 1 / 0;
            } catch (Exception e) {
                log.info("{}", e);
            }
        });

        Future<Boolean> submit = executorService.submit(() -> {
            log.info("{}", 123);
            int i = 1 / 0;
            return true;
        });

        try {
            // 如果没有异常 则返回结果  有异常就会返回异常信息
            Boolean aBoolean = submit.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

    }
}
