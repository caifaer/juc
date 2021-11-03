package com.smj.testboot.juc.syn.reentrant_lock.exercise;

import java.util.concurrent.locks.LockSupport;

public class AlternatePrintPark {


    static Thread t1;
    static Thread t2;
    static Thread t3;

    public static void main(String[] args) {

        Park park = new Park(5);

        t1 = new Thread(() -> {
            park.print("a", t2);

        });

        t2 = new Thread(() -> {
            park.print("b", t3);


        });
        t3 = new Thread(() -> {
            park.print("c", t1);
        });
        t1.start();
        t2.start();
        t3.start();

        LockSupport.unpark(t1);
    }



}

class Park  {

    private int loopNumber;

    public Park(int loopNumber) {
        this.loopNumber = loopNumber;
    }
    // park 针对的是某个线程  所以指定下一个线程即可
    public void print(String str, Thread next) {

        for (int i = 0; i < loopNumber; i++) {
            LockSupport.park();
            System.out.print(str);
            LockSupport.unpark(next);
        }

    }
}
