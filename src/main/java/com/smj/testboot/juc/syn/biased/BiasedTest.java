package com.smj.testboot.juc.syn.biased;


import lombok.extern.slf4j.Slf4j;
import org.openjdk.jol.info.ClassLayout;

@Slf4j(topic = "c.BiasedTest")
public class BiasedTest {

    /**
     * 一个对象创建时：
     * 如果开启了偏向锁（默认开启），那么对象创建后，markword 值为 0x05 即最后 3 位为 101，这时它的thread、epoch、age 都为 0
     * 偏向锁是默认是延迟的，不会在程序启动时立即生效，如果想避免延迟，可以加 VM 参数 -XX:BiasedLockingStartupDelay=0 来禁用延迟
     * 如果没有开启偏向锁，那么对象创建后，markword 值为 0x01 即最后 3 位为 001，这时它的 hashcode、age 都为 0，第一次用到 hashcode 时才会赋值
     */

    public static void main(String[] args) throws InterruptedException {
        /**
         * 可以 引入jol的jar包查看对象结构
         */
        // 延迟特性 -- 因为偏向锁是有延迟的  不会立即生效  可以加参数 -XX:BiasedLockingStartupDelay=0 生效
        Dog dog = new Dog();
        log.info(ClassLayout.parseInstance(dog).toPrintable());
        /**
         * OFFSET  SIZE   TYPE DESCRIPTION                               VALUE      可以看到这里是001  因为有延迟特性。
         *       0     4        (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
         *       4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
         *       8     4        (object header)                           a4 9b 01 20 (10100100 10011011 00000001 00100000) (536976292)
         *      12     4        (loss due to the next object alignment)
         * Instance size: 16 bytes
         * Space losses: 0 bytes internal + 4 bytes external = 4 bytes total
         */

       // 加锁  优先偏向锁
        synchronized (dog) {
            log.info(ClassLayout.parseInstance(dog).toPrintable());
        }

        // 解锁之后观察对象头   解锁之后会发现  线程id跟加锁时的线程id是一样的
        //  因为偏向锁 说明已经偏向主线程了  除非有人来竞争  所以线程id一直都是主线程的
        // 注意 线程id 是操作系统给的  跟java中的线程id 是不一样的。
        log.info(ClassLayout.parseInstance(dog).toPrintable());


        // 如果是在一个 多线程的环境下   偏向锁就没有意义 可以禁用掉
        // 使用参数 -XX:-UseBiasedLocking
        // 此时你再去看加锁时的  锁对象的 markWord  就是00  轻量级锁


        // 注意  当调用 hashCode方法的时候  偏向锁会被撤销
        // 观察 markWord的结构  会发现  hash码占用了31位  并且 hash码默认时0 只用使用的时候会将hash码存放到markWord中
        // 所以 当你使用了hash码时  会发现 无处安放  因为可能已经存储了线程id等数据   所以只能清掉这些数据
        // 所以也就 是撤销了偏向锁。
        dog.hashCode();


        // 还有一种撤销的方式 就是发生了竞争  由偏向锁 变成了 轻量级锁
        // 就那个例子嘛，在房间刻上名字。偏向第一个线程。
        //  但是这里注意 当你不在使用这个房间的时候  换句话说 就是这个线程执行完毕了  此时有其他的线程来加锁了
        // 此时就会变成 轻量级锁   也就是 000  解锁之后  就是 001


        // 还有一种撤销方式就是  wait/notify   因为这种方式只用 重量级锁才会有



    }

}



class Dog {

}
