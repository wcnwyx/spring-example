package com.wcnwyx.spring.aop.example.pointcut;

import org.springframework.context.annotation.*;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan(value = {"com.wcnwyx.spring.aop.example.pointcut"},
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.wcnwyx.spring.aop.example.pointcut.scope.*"))
public class Config {
}
