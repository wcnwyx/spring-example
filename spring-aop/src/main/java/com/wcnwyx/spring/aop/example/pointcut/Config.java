package com.wcnwyx.spring.aop.example.pointcut;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.ImportResource;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan(value = {"com.wcnwyx.spring.aop.example.pointcut"})
public class Config {
}
