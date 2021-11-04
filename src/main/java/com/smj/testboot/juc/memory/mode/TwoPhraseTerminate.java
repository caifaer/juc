package com.smj.testboot.juc.memory.mode;


import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "c.TwoPhraseTerminate")
public class TwoPhraseTerminate {


    public static void main(String[] args) {

        TwoPhraseTerminate tpt = new TwoPhraseTerminate();
        tpt.start();


        try {
            Thread.sleep(3500);
            tpt.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private volatile Boolean stop = false;

    private Thread monitor;

    // 启动监控线程
    public void start() {
        monitor = new Thread(() -> {
            while (true) {
                Thread thread = Thread.currentThread();
                if (stop) {  // 使用volatile 可以轻松解决
                    log.info("料理后事");
                    break;
                }
                try {
                    Thread.sleep(1000);// 情况1被打断  这里打断就是 catch  然后清除 打断标记
                    // 执行监控操作
                    log.info("执行监控操作");// 情况2被打断  就是将打断标记 标记为真
                } catch (InterruptedException e) {
                    // 此时就不需要 打断标记
//                    e.printStackTrace();
//                    // 在catch中 会清除 打断标记  此时可以在打断一次  设置打断标记
//                    thread.interrupt();
                }
            }
        });
        monitor.start();
    }

    // 停止监控线程
    public void stop() {
        stop = true;
    }




}
