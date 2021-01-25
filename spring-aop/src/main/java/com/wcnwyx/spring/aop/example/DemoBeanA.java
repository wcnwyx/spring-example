package com.wcnwyx.spring.aop.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DemoBeanA {
    @Autowired
    private DemoBean demoBean;

    public DemoBean getDemoBean() {
        return demoBean;
    }
}
