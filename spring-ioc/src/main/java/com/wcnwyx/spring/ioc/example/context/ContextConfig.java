package com.wcnwyx.spring.ioc.example.context;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@ComponentScan(basePackages = {"com.wcnwyx.spring.ioc.example.context"})
@Import({com.wcnwyx.spring.ioc.example.context.ImportBean.class})
@PropertySource(value = {"file:D:\\work\\code\\renhang-data\\config\\application-dev.properties"})
public class ContextConfig extends SuperContextConfig implements InterfaceContextConfig{

}
