package com.wcnwyx.spring.tx.example.propagation;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Test {
    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(TxConfig.class);
        OuterDao outerDao = applicationContext.getBean("outerDao", OuterDao.class);
        outerDao.outer();
        applicationContext.close();
    }
}
