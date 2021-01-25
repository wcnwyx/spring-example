package com.wcnwyx.spring.aop.example;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

public class Test {
    public static void main(String[] args) {
        AbstractApplicationContext applicationContext = new AnnotationConfigApplicationContext(Config.class);
        DemoBean demoBean = applicationContext.getBean("demoBean", DemoBean.class);
        System.out.println(demoBean);
        demoBean.div(1, 1);
    }
}
