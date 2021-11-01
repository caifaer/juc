package com.smj.testboot.juc.syn.syn_pattern;


import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 保护性暂停模式 --- 当线程在访问某个对象时，发现条件不满足时，就暂时挂起等待条件满足时再次访问。
 * 简单点就是 一个线程等待另一个线程的执行结果
 */
@Slf4j(topic = "c.GuardedSuspension")
public class GuardedSuspension {

    private Object response;
    private final Object lock = new Object();
    public Object get() {
        synchronized (lock) {
            // 条件不满足则等待
            while (response == null) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // 拿到对应的结果
            return response;
        }
    }
    public void complete(Object response) {
        synchronized (lock) {
            // 条件满足，通知等待线程
            this.response = response;
            lock.notifyAll();
        }
    }


    public static void main(String[] args) {
        // 记得去看图
        GuardedSuspension guardedObject = new GuardedSuspension();

        new Thread(() -> {
            // 子线程执行下载   假装有一个下载方法
            List<String> response = new ArrayList<>();
            log.debug("download complete...");
            // 下载完之后 去赋值  让等待的线程醒过来  拿到对应的结果
            guardedObject.complete(response);
        }).start();
        log.debug("waiting...");
        // 主线程阻塞等待
        Object response = guardedObject.get();
        log.debug("get response: [{}] lines", ((List<String>) response).size());
    }

}
