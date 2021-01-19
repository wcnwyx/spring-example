package com.wcnwyx.spring.ioc.example.listener;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.*;

@EnableAsync
@Configuration
@ComponentScan(basePackages = {"com.wcnwyx.spring.ioc.example.listener"})
public class ListenerConfig {

//    @Bean
//    public ApplicationEventMulticaster applicationEventMulticaster(){
//        //可以不自定义此bean
//        //自定义ApplicationEventMulticaster并设置taskExecutor，事件的监听器执行将采用异步方式，默认是同步的（同一个线程里执行）
//        Executor executor = new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS, new ArrayBlockingQueue(100));
//        SimpleApplicationEventMulticaster applicationEventMulticaster = new SimpleApplicationEventMulticaster();
//        applicationEventMulticaster.setTaskExecutor(executor);
//        return applicationEventMulticaster;
//    }
}
