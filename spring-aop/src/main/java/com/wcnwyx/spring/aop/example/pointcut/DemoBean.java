package com.wcnwyx.spring.aop.example.pointcut;

import org.springframework.stereotype.Component;

@Component
public class DemoBean{
    public int div(int a, int b){
        System.out.println("do div.");
        return a/b;
    }

    public int add(int a, int b){
        System.out.println("do add.");
        return a+b;
    }
}
