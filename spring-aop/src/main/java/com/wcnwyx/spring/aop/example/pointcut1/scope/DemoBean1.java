package com.wcnwyx.spring.aop.example.pointcut1.scope;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
//@Scope("prototype")
public class DemoBean1 {
    public int div(int a, int b){
        System.out.println("do div.");
        return a/b;
    }
}
