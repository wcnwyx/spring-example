package com.wcnwyx.spring.ioc.example.listener;

import org.springframework.context.ApplicationEvent;

public class DemoEvent extends ApplicationEvent {
    public DemoEvent(Object source) {
        super(source);
    }
}
