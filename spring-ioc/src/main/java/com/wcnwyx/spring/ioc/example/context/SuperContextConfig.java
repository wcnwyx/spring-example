package com.wcnwyx.spring.ioc.example.context;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class SuperContextConfig {
    @Bean
    public SuperBean superBean(){
        return new SuperBean();
    }
}
