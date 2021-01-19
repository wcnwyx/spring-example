package com.wcnwyx.spring.ioc.example.listener;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Test {
    public static void main(String[] args) throws InterruptedException {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ListenerConfig.class);
        applicationContext.publishEvent(new DemoEvent("test"));
        Thread.sleep(1000);
        applicationContext.destroy();
//        applicationContext.registerShutdownHook();
    }
}
