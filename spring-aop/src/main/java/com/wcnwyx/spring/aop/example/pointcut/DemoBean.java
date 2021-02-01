package com.wcnwyx.spring.aop.example.pointcut;

import org.springframework.stereotype.Component;

@Component
public class DemoBean{
    public int div(MyInt a, MyInt b){
        System.out.println("do div.");
        return a.getA()/b.getA();
    }
}
