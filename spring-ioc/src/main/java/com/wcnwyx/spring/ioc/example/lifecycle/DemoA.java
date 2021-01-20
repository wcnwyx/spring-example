package com.wcnwyx.spring.ioc.example.lifecycle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DemoA {
    @Autowired
    private DemoB demoB;

    public DemoB getDemoB() {
        return demoB;
    }
}
