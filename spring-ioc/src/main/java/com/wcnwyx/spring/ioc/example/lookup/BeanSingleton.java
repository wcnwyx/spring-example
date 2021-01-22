package com.wcnwyx.spring.ioc.example.lookup;

import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class BeanSingleton {

    @Lookup
    //Lookup注解，可以解决singleton里要要引用prototype类型的bean问题
    //使用了该注解，该bean将被cglib生成一个代理bean
    //解析@Lookup的逻辑在AutowiredAnnotationBeanPostProcessor中
    public BeanPrototype getBeanPrototype(){
        return null;
    }
}
