package com.wcnwyx.spring.aop.example.introduction;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

public class Test {
    public static void main(String[] args) {
        AbstractApplicationContext applicationContext = new AnnotationConfigApplicationContext(Config.class);
        FruitInterface fruitInterface = applicationContext.getBean("person", FruitInterface.class);
        System.out.println(fruitInterface);
        fruitInterface.printColor();
    }
}
