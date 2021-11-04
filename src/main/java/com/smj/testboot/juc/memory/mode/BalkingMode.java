package com.smj.testboot.juc.memory.mode;


import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "c.BalkingMode")
public class BalkingMode {


    /**
     * Balking （犹豫）模式用在一个线程发现另一个线程或本线程已经做了某一件相同的事，那么本线程就无需再做
     * 了，直接结束返回。
     */


    public static void main(String[] args) {
        TwoPhaseTermination tpt = new TwoPhaseTermination();
        // 一直调用的话 会一直执行。
        tpt.start();
        tpt.start();
        tpt.start();
    }




}



@Slf4j(topic = "c.TwoPhaseTermination")
class TwoPhaseTermination {
    // 监控线程
    private Thread monitorThread;
    // 停止标记
    private volatile boolean stop = false;
    // 判断是否执行过 start 方法
    private boolean starting = false;

    // 启动监控线程
    public void start() {

        //一定要加锁  这这里加一个标志即可 这样就可以保证一个线程只执行一次。
        synchronized (this) {
            if (starting) { // false
                return;
            }
            starting = true;
        }
        monitorThread = new Thread(() -> {
            while (true) {
                Thread current = Thread.currentThread();
                // 是否被打断
                if (stop) {
                    log.debug("料理后事");
                    break;
                }
                try {
                    Thread.sleep(1000);
                    log.debug("执行监控记录");
                } catch (InterruptedException e) {
                }
            }
        }, "monitor");
        monitorThread.start();
    }

    // 停止监控线程
    public void stop() {
        stop = true;
        monitorThread.interrupt();
    }
}