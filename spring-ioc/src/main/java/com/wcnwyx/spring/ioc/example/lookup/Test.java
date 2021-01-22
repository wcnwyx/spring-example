package com.wcnwyx.spring.ioc.example.lookup;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Test {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ConfigLookup.class);
        BeanSingleton singleton = applicationContext.getBean("beanSingleton", BeanSingleton.class);
        BeanPrototype prototype = applicationContext.getBean("beanPrototype", BeanPrototype.class);
        System.out.println("singleton:"+singleton);
        System.out.println("prototype:"+prototype);
        System.out.println("prototype:"+singleton.getBeanPrototype());
        System.out.println("prototype:"+singleton.getBeanPrototype());
    }
}
