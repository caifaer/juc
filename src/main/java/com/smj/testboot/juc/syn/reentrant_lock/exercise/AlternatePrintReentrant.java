package com.smj.testboot.juc.syn.reentrant_lock.exercise;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class AlternatePrintReentrant {


    public static void main(String[] args) throws InterruptedException {

        WaitNotifyReentrant waitNotifyReentrant = new WaitNotifyReentrant(5);
        Condition condition1 = waitNotifyReentrant.newCondition();
        Condition condition2 = waitNotifyReentrant.newCondition();
        Condition condition3 = waitNotifyReentrant.newCondition();
        new Thread(() -> {
            waitNotifyReentrant.print("a", condition1, condition2);
        }).start();
        new Thread(() -> {
            waitNotifyReentrant.print("b", condition2, condition3);
        }).start();
        new Thread(() -> {
            waitNotifyReentrant.print("c", condition3, condition1);
        }).start();

        Thread.sleep(1000);
        waitNotifyReentrant.lock();
        try {
            System.out.println("开始...");
            condition1.signal();
        } finally {
            waitNotifyReentrant.unlock();
        }
    }





}

class WaitNotifyReentrant extends ReentrantLock {

    private int loopNumber;

    public WaitNotifyReentrant(int loopNumber) {
        this.loopNumber = loopNumber;
    }
    //                 参数1 打印内容， 参数2 进入哪一间休息室, 参数3 下一间休息室
    public void print(String str, Condition current, Condition next) {

        for (int i = 0; i < loopNumber; i++) {

            lock();
            try {
                current.await();
                System.out.print(str);
                next.signal();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                unlock();
            }
        }

    }
}
