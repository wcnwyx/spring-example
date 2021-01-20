package com.wcnwyx.spring.ioc.example.lifecycle;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Test {
    public static void main(String[] args) {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(LifecycleConfig.class);
        DemoA demoA = applicationContext.getBean("demoA", DemoA.class);
        DemoB demoB = applicationContext.getBean("demoB", DemoB.class);
        System.out.println("demoA:"+demoA);
        System.out.println("demoB:"+demoB);
        System.out.println("getDemoA:"+demoB.getDemoA());
        System.out.println("getDemoB:"+demoA.getDemoB());
    }
}
