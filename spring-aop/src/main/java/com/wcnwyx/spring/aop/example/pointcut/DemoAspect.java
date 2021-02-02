package com.wcnwyx.spring.aop.example.pointcut;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 切面类
 */
@Component
@Aspect
public class DemoAspect {

    //抽象出公共的切入点表达式
    @Pointcut("execution(public int com.wcnwyx.spring.aop.example.pointcut.DemoBean.*(..))")
    public void pointCut(){
    }

    //前置通知
    @Before("pointCut()")
    public void logBefore(JoinPoint joinPoint){
        System.out.println("log begin... 方法名:" + joinPoint.getSignature()+" 参数："+ Arrays.asList(joinPoint.getArgs()));
    }

    //后置通知
    @After("pointCut()")
    public void logAfter(){
        System.out.println("log end... ");
    }

    //返回通知
    @AfterReturning(value = "pointCut()", returning = "result")
    public void logAfterReturning(Object result){
        System.out.println("log return. result:"+result);
    }

    //异常通知
    //多个参数的情况下，JoinPoint必须在第一位
    @AfterThrowing(value = "pointCut()", throwing = "throwable")
    public void logAfterThrowing(Throwable throwable){
        System.out.println("log exception:"+throwable.getMessage());
    }

    @Around(value = "pointCut()")
    public Object logAround(ProceedingJoinPoint joinPoint){
        Object result = null;
        try {
            System.out.println("logAround... joinPoint"+joinPoint.hashCode());
            System.out.println("logAround begin. 方法名:" + joinPoint.getSignature()+" 参数："+ Arrays.asList(joinPoint.getArgs()));
            result = joinPoint.proceed();
            System.out.println("logAround. result:"+result);
        } catch (Throwable throwable) {
            System.out.println("logAround 执行异常"+throwable);
            if(throwable instanceof ArithmeticException){
                Object[] args = joinPoint.getArgs();
                MyInt myInt = (MyInt)args[1];
                myInt.setA(1);
//                args[1] = new MyInt(1);
//                args[1] = 1;
                result = joinPoint.proceed(args);
            }
        }finally {
            System.out.println("logAround end");
            return result;
        }
    }
}
