package com.wcnwyx.spring.ioc.example.listener;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 通过实现ApplicationListener接口来表示订阅了DemoEvent事件
 */
@Component
public class Demo1Listener implements ApplicationListener<DemoEvent> {

    @Override
    public void onApplicationEvent(DemoEvent event) {
        System.out.println(this.toString()+" "+Thread.currentThread()+" "+event.toString());
    }

}
