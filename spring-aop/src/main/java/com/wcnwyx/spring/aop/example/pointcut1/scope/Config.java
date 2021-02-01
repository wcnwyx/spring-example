package com.wcnwyx.spring.aop.example.pointcut1.scope;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan(value = {"com.wcnwyx.spring.aop.example.pointcut.scope"})
public class Config {
}
