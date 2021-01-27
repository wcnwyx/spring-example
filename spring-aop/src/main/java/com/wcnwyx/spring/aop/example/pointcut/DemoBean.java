package com.wcnwyx.spring.aop.example.pointcut;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "prototype")
public class DemoBean implements DisposableBean {
    public int div(int a, int b){
        System.out.println("do div.");
        return a/b;
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("destroy");
    }
}
