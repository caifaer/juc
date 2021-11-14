package com.smj.testboot.juc.aqs;

public class StampedLockTest {


    /**
     *
     * 特点是在使用读锁、写锁时都必须配合【戳】使用
     * 缺点
     *      StampedLock 不支持条件变量
     *      StampedLock 不支持可重入
     *
     *  乐观读，StampedLock 支持 tryOptimisticRead() 方法（乐观读），读取完毕后需要做一次 戳校验 如果校验通
     * 过，表示这期间确实没有写操作，数据可以安全使用，如果校验没通过，需要重新获取读锁，保证数据安全。
     *
     * lock.validate(stamp)  验证戳
     */
}
