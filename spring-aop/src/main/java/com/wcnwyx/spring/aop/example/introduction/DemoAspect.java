package com.wcnwyx.spring.aop.example.introduction;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareParents;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class DemoAspect {
    @DeclareParents(value = "com.wcnwyx.spring.aop.example.introduction.Person", defaultImpl = DefaultFruit.class)
    private FruitInterface fruitInterface;
}
