package com.smj.testboot.juc.thread_pool;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Slf4j(topic = "c.ThreadSchedule")
public class ThreadSchedule {


    public static void main(String[] args) {
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);

        // 定时执行   如果执行任务的时间大于了 间隔的时间  间隔的时间将无效  一个任务执行结束 立马执行下一个任务
        pool.scheduleAtFixedRate(() -> {
            log.debug("task1");
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.SECONDS);


        // 这个方法 是任务和任务之间的间隔时间  就是一个任务执行完毕了  然后间隔多久 执行下一个任务
        pool.scheduleWithFixedDelay(() -> {
             log.debug("task1");
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.SECONDS);



    }
}
