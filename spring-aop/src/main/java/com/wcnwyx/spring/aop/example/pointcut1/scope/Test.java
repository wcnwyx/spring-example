package com.wcnwyx.spring.aop.example.pointcut1.scope;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

public class Test {
    public static void main(String[] args) {
        AbstractApplicationContext applicationContext = new AnnotationConfigApplicationContext(Config.class);
        DemoBean1 demoBean1 = applicationContext.getBean("demoBean1", DemoBean1.class);
        demoBean1.div(1, 1);
        demoBean1.div(1, 1);

        DemoBean2 demoBean2 = applicationContext.getBean("demoBean2", DemoBean2.class);
        demoBean2.div(1, 1);
        demoBean2.div(1, 1);
        applicationContext.close();
    }
}
