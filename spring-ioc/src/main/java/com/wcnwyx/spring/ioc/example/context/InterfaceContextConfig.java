package com.wcnwyx.spring.ioc.example.context;

import org.springframework.context.annotation.Bean;

public interface InterfaceContextConfig {
    @Bean
    default InterfaceBean interfaceBean(){
        return new InterfaceBean();
    }
}
