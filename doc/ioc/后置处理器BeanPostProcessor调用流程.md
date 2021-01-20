BeanPostProcessor是spring的一种后置处理器，它是在bean实例化之后被调用，因此可以用来给bean实例自定义一些操作。
BeanPostProcessor接口方法展示
```java
public interface BeanPostProcessor {
    //bean实例化时，调用初始化方法之前调用
	Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException;
    //bean实例化时，执行过初始化方法之后调用
	Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException;
}
```

BeanPostProcessor接口方法的调用时机，从

创建bean的doCreateBean方法展示，可以清晰看到两个方法的调用时机
```java
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory
        implements AutowireCapableBeanFactory {
    
    protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final Object[] args)
            throws BeanCreationException {
        //创建bean实例
        createBeanInstance(beanName, mbd, args);
        //装载bean属性数据
        populateBean(beanName, mbd, instanceWrapper);
        //初始化bean
        initializeBean(beanName, exposedObject, mbd);

        return exposedObject;
    }

    protected Object initializeBean(final String beanName, final Object bean, RootBeanDefinition mbd) {

        Object wrappedBean = bean;
        if (mbd == null || !mbd.isSynthetic()) {
            //执行BeanPostProcessor的postProcessBeforeInitialization方法
            wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
        }

        try {
            //调用初始化方法
            invokeInitMethods(beanName, wrappedBean, mbd);
        }

        if (mbd == null || !mbd.isSynthetic()) {
            //执行BeanPostProcessor的postProcessAfterInitialization方法
            wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
        }
        return wrappedBean;
    }
}

```

BeanPostProcessor的一个很熟悉的实现类就是ApplicationContextAwareProcessor，就是用来处理各种Aware接口的，看一下代码就知道了。
```java
class ApplicationContextAwareProcessor implements BeanPostProcessor {
    
    @Override
    public Object postProcessBeforeInitialization(final Object bean, String beanName) throws BeansException {
        //删除了部分访问控制的代码
        invokeAwareInterfaces(bean);
        return bean;
    }

    private void invokeAwareInterfaces(Object bean) {
        if (bean instanceof Aware) {
            if (bean instanceof EnvironmentAware) {
                ((EnvironmentAware) bean).setEnvironment(this.applicationContext.getEnvironment());
            }
            if (bean instanceof EmbeddedValueResolverAware) {
                ((EmbeddedValueResolverAware) bean).setEmbeddedValueResolver(this.embeddedValueResolver);
            }
            if (bean instanceof ResourceLoaderAware) {
                ((ResourceLoaderAware) bean).setResourceLoader(this.applicationContext);
            }
            if (bean instanceof ApplicationEventPublisherAware) {
                ((ApplicationEventPublisherAware) bean).setApplicationEventPublisher(this.applicationContext);
            }
            if (bean instanceof MessageSourceAware) {
                ((MessageSourceAware) bean).setMessageSource(this.applicationContext);
            }
            if (bean instanceof ApplicationContextAware) {
                ((ApplicationContextAware) bean).setApplicationContext(this.applicationContext);
            }
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}
```