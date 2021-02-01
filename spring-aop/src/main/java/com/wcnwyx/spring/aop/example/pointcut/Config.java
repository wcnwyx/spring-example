package com.wcnwyx.spring.aop.example.pointcut;

import org.springframework.context.annotation.*;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan(value = {"com.wcnwyx.spring.aop.example.pointcut"})
public class Config {
}
