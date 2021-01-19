package com.wcnwyx.spring.ioc.example.listener;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component("applicationEventMulticaster")
public class CloseableSimpleApplicationEventMulticaster extends SimpleApplicationEventMulticaster implements ApplicationListener<ContextClosedEvent> {
    @PostConstruct
    public void init(){
        Executor executor = new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS, new ArrayBlockingQueue(100));
        super.setTaskExecutor(executor);
    }
//    @PreDestroy
//    public void close(){
//        Executor executor = super.getTaskExecutor();
//        if(executor!=null){
//            ((ThreadPoolExecutor) executor).shutdown();
//        }
//        System.out.println("close");
//    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        Executor executor = super.getTaskExecutor();
        if(executor!=null){
            ((ThreadPoolExecutor) executor).shutdown();
        }
    }
}
