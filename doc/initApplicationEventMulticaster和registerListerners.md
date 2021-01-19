初始化方法refresh()中调用的这两个方法都是来处理spring内部事件发布订阅的逻辑。  
initApplicationEventMulticaster() 用来初始化一个事件多播器。  
registerListeners() 将事件监听器注册到多播器里。  
注册Listener并不是只有这一个地方在处理，这里处理的都是实现了ApplicationListener接口的bean，  
还有通过注解@EventListener声明的事件监听器，注解实现的监听器注册是通过EventListenerMethodProcessor这个处理器来实现的，该逻辑我们后续再看。

```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader
		implements ConfigurableApplicationContext, DisposableBean {

    //初始化一个事件多播器
    protected void initApplicationEventMulticaster() {
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
            //如果已经有了applicationEventMulticaster这个beanName的singleton或者BeanDefinition，直接getBean获取赋值给当前累的变量
            this.applicationEventMulticaster =
                    beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
            if (logger.isDebugEnabled()) {
                logger.debug("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
            }
        }
        else {
            //如果还没有，new出来一个SimpleApplicationEventMulticaster来使用，并注册到factory中
            this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
            beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to locate ApplicationEventMulticaster with name '" +
                        APPLICATION_EVENT_MULTICASTER_BEAN_NAME +
                        "': using default [" + this.applicationEventMulticaster + "]");
            }
        }
    }

    //将事件监听器注册到多播器里
    protected void registerListeners() {
        // 添加内部变量applicationListeners中已有的监听器
        for (ApplicationListener<?> listener : getApplicationListeners()) {
            getApplicationEventMulticaster().addApplicationListener(listener);
        }

        //将所有ApplicationListener类型的bean获取到逐个注册到多播器里
        String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
        for (String listenerBeanName : listenerBeanNames) {
            getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
        }

        // 之前可以会有ApplicationEvent，但是因为Multicaster和Listener都还未初始化，所以会暂存在earlyApplicationEvents中
        //这里就可以将earlyApplicationEvents里的时间进行多播处理了
        //prepareRefresh（）发放中初始化了earlyApplicationEvents该变量
        Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
        this.earlyApplicationEvents = null;
        if (earlyEventsToProcess != null) {
            for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
                getApplicationEventMulticaster().multicastEvent(earlyEvent);
            }
        }
    }
    
}
```