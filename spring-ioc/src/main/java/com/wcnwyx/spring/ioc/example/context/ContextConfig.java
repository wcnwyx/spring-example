package com.wcnwyx.spring.ioc.example.context;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = {"com.wcnwyx.spring.ioc.example.context"})
@Import({com.wcnwyx.spring.ioc.example.context.ImportBean.class})
public class ContextConfig {
}
