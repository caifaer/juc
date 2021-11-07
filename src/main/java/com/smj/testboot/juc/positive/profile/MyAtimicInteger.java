package com.smj.testboot.juc.positive.profile;


import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class MyAtimicInteger {

    private volatile int value;

    private static final long valueOffset;

    private static final Unsafe unsafe;

    static {
        unsafe = UnsafeAccessor.getUnsafe();
        try {
            valueOffset = unsafe.objectFieldOffset(MyAtimicInteger.class.getDeclaredField("value"));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }


    public int get() {
        return this.value;
    }

    public void decrement(int amount) {
        while (true) {
            int prev = this.value;
            int next = prev - amount;
            if (unsafe.compareAndSwapInt(this, valueOffset, prev, next)) {
                break;
            }
        }
    }

}

class UnsafeAccessor {
    private static final Unsafe unsafe;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    public static Unsafe getUnsafe() {
        return unsafe;
    }
}


