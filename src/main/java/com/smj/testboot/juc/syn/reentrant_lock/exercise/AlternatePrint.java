package com.smj.testboot.juc.syn.reentrant_lock.exercise;


import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "c.AlternatePrint")
public class AlternatePrint {


    public static void main(String[] args) {
        WaitNotify waitNotify = new WaitNotify(1, 5);

        new Thread(() -> {
            waitNotify.print("a", 1, 2);
        }).start();

        new Thread(() -> {
            waitNotify.print("b", 2, 3);
        }).start();

        new Thread(() -> {
            waitNotify.print("c", 3, 1);
        }).start();


    }




}


class WaitNotify {

    /*
    输出内容       等待标记     下一个标记
       a           1             2
       b           2             3
       c           3             1
     */
    private int flag;

    private int loopNumber;

    public WaitNotify(int flag, int loopNumber) {
        this.flag = flag;
        this.loopNumber = loopNumber;
    }

    public void print(String str, int waitFlag, int nextFlag) {

        for (int i = 0; i < loopNumber; i++) {

            synchronized (this) {
                while (flag != waitFlag) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.print(str);
                // 设置下一个等待标记
                flag = nextFlag;
                this.notifyAll();
            }
        }
    }
}
