package com.wcnwyx.spring.aop.example;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 切面类
 */
@Component
@Aspect
public class DemoAspect {

    //抽象出公共的切入点表达式
    @Pointcut("execution(public int com.wcnwyx.spring.aop.example.DemoBean.*(..))")
    public void pointCut(){
    }

    //前置通知
    @Before("pointCut()")
    public void logBegin(JoinPoint joinPoint){
        System.out.println("beginJoinPoint:"+joinPoint.hashCode());
        System.out.println("log begin... 方法名:" + joinPoint.getSignature()+" 参数："+ Arrays.asList(joinPoint.getArgs()));
    }

    //后置通知
    @After("pointCut()")
    public void logEnd(JoinPoint joinPoint){
        System.out.println("endJoinPoint:"+joinPoint.hashCode());
        System.out.println("log end... 方法名:" + joinPoint.getSignature()+" 参数："+ Arrays.asList(joinPoint.getArgs()));
    }

    //返回通知
    @AfterReturning(value = "pointCut()", returning = "result")
    public void logReturn(JoinPoint joinPoint, Object result){
        System.out.println("returnJoinPoint:"+joinPoint.hashCode());
        System.out.println("log return. result:"+result);
    }

    //异常通知
    //多个参数的情况下，JoinPoint必须在第一位
    @AfterThrowing(value = "pointCut()", throwing = "exception")
    public void logException(JoinPoint joinPoint, Exception exception){
        System.out.println("exceptionJoinPoint:"+joinPoint);
        System.out.println("log exception:"+exception.getMessage()+" 方法名："+joinPoint.getSignature().getName());
    }

    public void logRound(){

    }
}
