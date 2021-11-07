package com.smj.testboot.juc.positive.profile;

import lombok.Data;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeTest {


    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {

        // 通过反射 获取Unsafe 对象
        Field thenUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        thenUnsafe.setAccessible(true);
        Unsafe unsafe = (Unsafe) thenUnsafe.get(null);
        System.out.println(unsafe);

        // 1. 获取域的偏移地址
        long idOffset = unsafe.objectFieldOffset(Teacher.class.getDeclaredField("id"));
        long nameOffset = unsafe.objectFieldOffset(Teacher.class.getDeclaredField("name"));

        Teacher t = new Teacher();
        // 2. 执行 cas 操作
        unsafe.compareAndSwapInt(t, idOffset, 0, 1);
        unsafe.compareAndSwapObject(t, nameOffset, null, "张三");

        // 3. 验证
        System.out.println(t);
    }
}


@Data
class Teacher {
    volatile int id;
    volatile String name;
}
