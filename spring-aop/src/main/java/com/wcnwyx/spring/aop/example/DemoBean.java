package com.wcnwyx.spring.aop.example;

import org.springframework.stereotype.Component;

@Component
public class DemoBean {
    public int div(int a, int b){
        System.out.println("do div.");
        return a/b;
    }
}
