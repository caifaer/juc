package com.smj.testboot.juc.thread.mode;

import lombok.extern.slf4j.Slf4j;


@Slf4j(topic = "c.TwoPhase")
public class TwoPhase {

    /**
     * 两阶段终止模式 在一个线程 T1 中如何“优雅”终止线程 T2？这里的【优雅】指的是给 T2 一个料理后事的机会。
     *
     *   总的来说  两阶段 终止模式 就是使用 interrupt
     *              所以有两种情况  一种就是 打断  sleep/wait/join(会清除打断标记)
     *                              一种就是 打断 普通线程(不会清除)
     *
     */



    public static void main(String[] args) {

        TwoPhaseTermination tpt = new TwoPhaseTermination();
        tpt.start();


        try {
            Thread.sleep(3500);
            tpt.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}


@Slf4j(topic = "c.TwoPhaseTermination")
class TwoPhaseTermination {

    private Thread monitor;

    // 启动监控线程
    public void start() {
        monitor = new Thread(() -> {
            while (true) {
                Thread thread = Thread.currentThread();
                if (thread.isInterrupted()) {
                    log.info("料理后事");
                    break;
                }
                try {
                    Thread.sleep(1000);// 情况1被打断  这里打断就是 catch  然后清除 打断标记
                    // 执行监控操作
                    log.info("执行监控操作");// 情况2被打断  就是将打断标记 标记为真
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    // 在catch中 会清除 打断标记  此时可以在打断一次  设置打断标记
                    thread.interrupt();
                }
            }
        });
        monitor.start();
    }

    // 停止监控线程
    public void stop() {
        monitor.interrupt();
    }


}
