package com.smj.testboot.juc.memory.profile;

public class TestVisibility {



//    static  boolean run = true;
    volatile static  boolean run = true; // 解决办法 就是加上 volatile  保证了多个线程对于这个值的 可见性。

    public static void main(String[] args) throws InterruptedException {

        Thread t = new Thread(()->{
            while(true){
// ....
                if (!run) {
                    break;
                }

            }
        });

        t.start();

        Thread.sleep(1000);

        run = false; // 线程t不会如预想的停下来
    }

}

class Syn {

    static  boolean run = true; // 还可以使用synchronized来解决 可见性  不过是重量级锁 影响性能

    private static Object lock = new Object();

    public static void main(String[] args) throws InterruptedException {

        Thread t = new Thread(() -> {
            while (true) {
                synchronized (lock) {
                    if (!run) {
                        System.out.println(run); // println方法加了锁  所以可以读取到正确的值
                        break;
                    }
                }
            }
        });

        t.start();

        Thread.sleep(1000);
        synchronized (lock) {
            run = false;
        }
    }
}
