package com.wcnwyx.spring.aop.example;

import org.aspectj.lang.ProceedingJoinPoint;

public class DemoAspectXml {
    public void before(){
        System.out.println("before....");
    }
    public void after(){
        System.out.println("after....");
    }
    public void afterReturn(){
        System.out.println("after return....");
    }
    public void afterThrowing(){
        System.out.println("after throwing");
    }
    public void around(ProceedingJoinPoint joinPoint){
        try {
            long timeBegin = System.currentTimeMillis();
            joinPoint.proceed();
            long timeEnd = System.currentTimeMillis();
            System.out.println("method invoke cost "+(timeEnd-timeBegin)+"ms");
        } catch (Throwable throwable) {
            afterThrowing();
        }
    }
}
