```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader
        implements ConfigurableApplicationContext, DisposableBean {
    //通知子类刷新内部的beanFactory
    protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
        //该方法为抽象方法，各个子类实现自己的逻辑
        refreshBeanFactory();

        //该方法为抽象方法，各个子类实现自己的逻辑
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        if (logger.isDebugEnabled()) {
            logger.debug("Bean factory for " + getDisplayName() + ": " + beanFactory);
        }
        return beanFactory;
    }
}

```