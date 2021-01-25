package com.wcnwyx.spring.aop.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DemoBean {
    @Autowired
    private DemoBeanA demoBeanA;
    public int div(int a, int b){
        System.out.println("do div");
        return a/b;
    }
}
