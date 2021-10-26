package com.smj.testboot.juc;


import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "c.RunnableTest")
public class RunnableTest {

    public static void main(String[] args) {

        Thread thread1 = new Thread() {
            // 重写 run 方法
            @Override
            public void run() {
                super.run();

            }
        };


        // 接收一个 runnable对象   是一个函数式接口  使用lambda表达式   t1是设置线程名称
        Runnable runnable = () -> log.info("2323");
        // 接收一个对象的时候 会有一个run方法  runnable对象是否为空。
        Thread thread = new Thread(runnable, "t1");
        // 线程启动
        thread.start();

        log.info("56");

    }
}
