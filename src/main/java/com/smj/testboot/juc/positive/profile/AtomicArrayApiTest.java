package com.smj.testboot.juc.positive.profile;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class AtomicArrayApiTest {
    public static void main(String[] args) {
        Student stu = new Student();

        AtomicReferenceFieldUpdater updater =        // 修改的类 修改的变量类型   修改的变量名称
                AtomicReferenceFieldUpdater.newUpdater(Student.class, String.class, "name");

        // 其实原理都大差不大
        System.out.println(updater.compareAndSet(stu, null, "张三"));
        System.out.println(stu);
    }
}

class Student {

    // 必须要加上 volatile 关键字  否则会报错
    volatile String name;

    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                '}';
    }
}
