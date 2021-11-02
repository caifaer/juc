package com.smj.testboot.juc.syn.reentrant_lock.activity;


import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "c.PhilosopherDinner")
public class PhilosopherDinner {
    /**
     * 有五位哲学家，围坐在圆桌旁。
     * 他们只做两件事，思考和吃饭，思考一会吃口饭，吃完饭后接着思考。
     * 吃饭时要用两根筷子吃，桌上共有 5 根筷子，每位哲学家左右手边各有一根筷子。
     * 如果筷子被身边的人拿着，自己就得等待
     */

    public static void main(String[] args) {
        Chopstick c1 = new Chopstick("1");
        Chopstick c2 = new Chopstick("2");
        Chopstick c3 = new Chopstick("3");
        Chopstick c4 = new Chopstick("4");
        Chopstick c5 = new Chopstick("5");
        new Philosopher("苏格拉底", c1, c2).start();
        new Philosopher("柏拉图", c2, c3).start();
        new Philosopher("亚里士多德", c3, c4).start();
        new Philosopher("赫拉克利特", c4, c5).start();
        new Philosopher("阿基米德", c5, c1).start();
    }


}

@Slf4j(topic = "c.Philosopher")
class Philosopher extends Thread {
    Chopstick left;
    Chopstick right;public Philosopher(String name, Chopstick left, Chopstick right) {
        super(name);
        this.left = left;
        this.right = right;
    }
    private void eat() throws InterruptedException {
        log.debug("eating...");
        Thread.sleep(1);
    }
    @Override
    public void run() {
        while (true) {
            // 获得左手筷子
            synchronized (left) {
                    // 获得右手筷子
                synchronized (right) {
                    // 吃饭
                    try {
                        eat();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // 放下右手筷子
            }
                // 放下左手筷子
        }
    }
}



class Chopstick {
    String name;
    public Chopstick(String name) {
        this.name = name;
    }
    @Override
    public String toString() {
        return "筷子{" + name + '}';
    }
}