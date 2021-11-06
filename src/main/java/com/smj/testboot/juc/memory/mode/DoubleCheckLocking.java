package com.smj.testboot.juc.memory.mode;

public class DoubleCheckLocking {


}


final class Singleton {
    private Singleton() { }

    // 使用volatile 来避免 有序性问题
    // synchronized 可以保证有序性  但是其中还是会发生指令重排
    // 本质问题还是 创建对象的时候并不是原子性操作  还是会发生指令重排
    private static volatile Singleton INSTANCE = null;

    public static Singleton getInstance() {
        // 实例没创建，才会进入内部的 synchronized代码块
        if (INSTANCE == null) {
            synchronized (Singleton.class) { // t2
                // 也许有其它线程已经创建实例，所以再判断一次
                if (INSTANCE == null) { // t1
                    INSTANCE = new Singleton();
                }
            }
        }
        return INSTANCE;
    }
}