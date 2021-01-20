package com.wcnwyx.spring.ioc.example.lifecycle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DemoB {
    @Autowired
    private DemoA demoA;

    public DemoA getDemoA() {
        return demoA;
    }
}
