package com.smj.testboot.juc.syn.exercise;


import lombok.extern.slf4j.Slf4j;

import java.util.Random;


@Slf4j(topic = "c.TransferMoney")
public class TransferMoney {


    // Random 为线程安全
    static Random random = new Random();
    // 随机 1~100
    public static int randomAmount() {
        return random.nextInt(100) +1;
    }
    public static void main(String[] args) throws InterruptedException {

        Account a = new Account(1000);
        Account b = new Account(1000);
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                a.transfer(b, randomAmount());
            }
        }, "t1");

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                b.transfer(a, randomAmount());
            }
        }, "t2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        // 查看转账2000次后的总金额
        log.debug("total:{}",(a.getMoney() + b.getMoney()));

    }



}


class Account {

    private int money;

    public Account(int money) {
        this.money = money;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }


    /**
     * 这里 有两个 共享变量
     * this.money 当前账户的余额   以及  传过来的 账户target的余额都是
     * 所以 此时 要对这两个 加锁
     * 可以 对两个对象加锁  但是可能会死锁  synchronized(this, target)
     * 此时 可以 使用类锁  不过效率会很低  因为在当前时刻 就只能操作一个人
     */
    public void transfer(Account target, int amount) {
        synchronized (Account.class) {
            if (this.money > amount) {
                setMoney(this.getMoney() - amount);
                target.setMoney(target.getMoney() + amount);
            }
        }
    }















}
