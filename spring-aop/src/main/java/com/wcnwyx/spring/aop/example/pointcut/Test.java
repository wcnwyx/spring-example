package com.wcnwyx.spring.aop.example.pointcut;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Test {
    public static void main(String[] args){
        AbstractApplicationContext applicationContext = new AnnotationConfigApplicationContext(Config.class);
        DemoBean demoBean = applicationContext.getBean("demoBean", DemoBean.class);
        demoBean.div(new MyInt(1), new MyInt(0));

        applicationContext.close();
    }
}
