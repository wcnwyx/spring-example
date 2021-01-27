package com.wcnwyx.spring.aop.example.customTargetSource;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class DemoBean{
    public int div(int a, int b){
        System.out.println("do div.");
        return a/b;
    }
}
