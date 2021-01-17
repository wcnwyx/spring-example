package com.wcnwyx.spring.ioc.example.context;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Test {
    public static void main(String[] args) {
//        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("context/applicationContext.xml");
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(ContextConfig.class);
        ComponentBean componentBean = applicationContext.getBean("componentBean", ComponentBean.class);
        System.out.println(componentBean);

    }
}
