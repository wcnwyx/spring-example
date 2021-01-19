package com.wcnwyx.spring.ioc.example.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 通过@EventListener注解表示订阅了DemoEvent事件
 * @Async 表示该方法逻辑会通过异步的方式处理
 */
@Component
public class Demo2Listener{

    @Async
    @EventListener
    public void onApplicationEvent(DemoEvent event) {
        System.out.println(this.toString()+" " + Thread.currentThread()+" "+event.toString());
    }
}
