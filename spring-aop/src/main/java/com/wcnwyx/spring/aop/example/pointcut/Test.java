package com.wcnwyx.spring.aop.example.pointcut;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import java.util.concurrent.atomic.AtomicInteger;

public class Test {
    public static void main(String[] args) {
//        AbstractApplicationContext applicationContext = new AnnotationConfigApplicationContext(Config.class);
//        DemoBean demoBean = applicationContext.getBean("demoBean", DemoBean.class);
//        demoBean.div(new MyInt(1), new MyInt(0));
//
//        applicationContext.close();

        Object[] array = new Object[]{new MyInt(1)};
        System.out.println(array.hashCode()+" "+array[0]);
        Object[] arrayClone = array.clone();
        System.out.println(arrayClone.hashCode()+" "+arrayClone[0]);

        MyInt myInt = new MyInt(2);
        System.out.println(myInt);
        System.out.println(myInt.clone());

//        Object[] array = new Object[]{new MyInt(1)};
//        System.out.println(array[0]);
//        test(array);
//        System.out.println(array[0]);

//        Object[] array = new Object[]{1};
//        System.out.println(array[0]);
//        test1(array);
//        System.out.println(array[0]);
    }

    public static void test(Object[] array){
        array[0] = new MyInt(2);
        System.out.println(array[0]);
    }

    public static void test1(Object[] array){
        array[0] = 2;
        System.out.println(array[0]);
    }
}
