package com.wcnwyx.spring.aop.example.pointcut.scope;

import org.aspectj.lang.annotation.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 切面类
 */
@Component
@Aspect("perthis()")
@Scope("prototype")
public class DemoAspect {
    private int flag = 0;

    //前置通知
    @Before("execution(public int com.wcnwyx.spring.aop.example.pointcut.scope.*.*(..))")
    public void logBefore(){
        System.out.println("log begin. flag="+(++flag));
    }
}
