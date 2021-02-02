package com.wcnwyx.spring.tx.example;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Test {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(TxConfig.class);
        DemoDao demoDao = applicationContext.getBean("demoDao", DemoDao.class);
        demoDao.insert("test", 1);
        applicationContext.close();
    }
}
