package com.smj.testboot.juc;


import lombok.extern.slf4j.Slf4j;
import org.openjdk.jmh.annotations.*;

@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations=3)
@Measurement(iterations=5)
@Slf4j(topic = "c")
public class Syn {



    public static void main(String[] args) {

        log.info("do something");

    }
}
