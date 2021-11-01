package com.smj.testboot.juc.syn.syn_pattern;


import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j(topic = "c.GuardedSuspensionTime")
public class GuardedSuspensionTime {

    /**
     * 进阶版本 增加超时时间
     */

    private Object response;
    private final Object lock = new Object();

    // millis 表示等待多久 不再等待
    public Object get(long millis) {
        synchronized (lock) {
            // 1) 记录最初时间
            long begin = System.currentTimeMillis();
            // 2) 已经经历的时间
            long timePassed = 0;
            while (response == null) {
                // 4) 假设 millis 是 1000，结果在 400 时唤醒了，那么还有 600 要等
                // 这里可能会有 虚假唤醒   所以一定要减去timePasses
                // 因为虚假唤醒的时候  你被提前唤醒了  所以while会继续循环  此时 还是会wait
                //   但是要思考  等待的时间该是多少呢  如果还是传过来的等待时间  就会多等待。
                long waitTime = millis - timePassed;
                log.debug("waitTime: {}", waitTime);
                if (waitTime <= 0) { // 这里也就是 timePassed >= millis  超过了超时时间
                    log.debug("break...");
                    break;
                }
                try {
                    lock.wait(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 3) 如果提前被唤醒，这时已经经历的时间假设为 400
                // 获得经历的时间
                timePassed = System.currentTimeMillis() - begin;
                log.debug("timePassed: {}, object is null {}",
                        timePassed, response == null);
            }
            return response;
        }
    }
    public void complete(Object response) {
        synchronized (lock) {
            // 条件满足，通知等待线程
            this.response = response;
            log.debug("notify...");
            lock.notifyAll();
        }
    }


    public static void main(String[] args) {
        GuardedSuspensionTime v2 = new GuardedSuspensionTime();
        new Thread(() -> {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            v2.complete(null);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            v2.complete(Arrays.asList("a", "b", "c"));
        }).start();
        Object response = v2.get(2500);
        if (response != null) {
            log.debug("get response: [{}] lines", ((List<String>) response).size());
        } else {
            log.debug("can't get response");
        }
    }
}
