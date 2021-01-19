spring的ApplicationContext都是从这个refresh()方法来初始化的。  
refresh方法为一个常见的模板模式的应用，各个子类可以差异性的重写里面每一步的方法，但是整体的refresh流程必须按照这个顺序来进行。    
这一篇先大概看一下整个refresh方法里具体的执行步骤，简单的方法就直接在该篇中写了，逻辑多点的后续篇章再单独介绍。  
可以先看一下基础概念那篇，简单的描述了一些常用的接口是用来做什么的，先一个简单的概念，可能会更好理解一点。  

```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader
        implements ConfigurableApplicationContext, DisposableBean {

    public void refresh() throws BeansException, IllegalStateException {
        synchronized (this.startupShutdownMonitor) {
            //为Context的刷新操作准备一些数据（此篇后部分介绍）
            prepareRefresh();

            //通知子类刷新内部的BeanFactory并返回，通过参数可以看出内部用的是ConfigurableListableBeanFactory（后续单独文章中介绍）
            ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

            //准备BeanFactory，（此篇后部分介绍）
            prepareBeanFactory(beanFactory);

            try {
                //此类中该方法为空，允许子类重写该方法（空方法，不再罗列）
                postProcessBeanFactory(beanFactory);

                //调用执行该Context中定义的所有BeanFactoryPostProcessor（后续单独文章中介绍）
                invokeBeanFactoryPostProcessors(beanFactory);

                //注册并实例化所有的BeanPostProcessor（后续单独文章中介绍）
                registerBeanPostProcessors(beanFactory);

                // Initialize message source for this context.
                initMessageSource();

                //初始化事件多播器（后续单独文章中介绍）
                initApplicationEventMulticaster();

                //该类中此方法为空，允许子类来重写该方法（空方法，不再罗列）
                onRefresh();

                //注册事件监听器（后续单独文章中介绍）
                registerListeners();

                //实例化所有剩余的非lazy-init的singleton bean（后续单独文章中介绍）
                finishBeanFactoryInitialization(beanFactory);

                //最后一步，处理LifecycleProcessor、发布事件ContextRefreshedEvent，（此篇后部分介绍）
                finishRefresh();
            }

            catch (BeansException ex) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Exception encountered during context initialization - " +
                            "cancelling refresh attempt: " + ex);
                }

                //销毁已经实例化好的singleton bean
                destroyBeans();

                //重设active参数状态
                cancelRefresh(ex);

                throw ex;
            }

            finally {
                // Reset common introspection caches in Spring's core, since we
                // might not ever need metadata for singleton beans anymore...
                resetCommonCaches();
            }
        }
    }

    
    
    protected void prepareRefresh() {
        //设置启动时间一起状态参数
        this.startupDate = System.currentTimeMillis();
        this.closed.set(false);
        this.active.set(true);

        if (logger.isInfoEnabled()) {
            logger.info("Refreshing " + this);
        }

        //该类中此方法为空，允许子类来重写该方法
        initPropertySources();

        //获取并创建Environment，校验一些必须的属性
        getEnvironment().validateRequiredProperties();

        //定义一个集合来存储早期的ApplicationEvent数据
        //何为早期的ApplicationEvent呢？就是在事件多播器和监听器初始化之前需要发布的事件，因为多播器和监听器都还没有初始化，只能先暂存在这里，
        //等到initApplicationEventMulticaster()和registerListeners()这两个方法执行过后，多播器和监听器就都初始化完成了，会把该集合中的事件全部发布出去
        this.earlyApplicationEvents = new LinkedHashSet<ApplicationEvent>();
    }
    

    //准备BeanFactory
    protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        //设置BeanClassLoader
        beanFactory.setBeanClassLoader(getClassLoader());
        //设置BeanExpressionResolver
        beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
        //设置PropertyEditorRegistrar
        beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

        //添加一个BeanPostProcessor（ApplicationContextAwareProcessor），用于处理Aware接口的逻辑
        beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
        
        //在autowiring的时候忽略哪些接口
        beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
        beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
        beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
        beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
        beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
        beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);

        //在autowiring的时候，如果是这些接口，那就采用后面参数的bean来装配
        beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
        beanFactory.registerResolvableDependency(ResourceLoader.class, this);
        beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
        beanFactory.registerResolvableDependency(ApplicationContext.class, this);

        //添加一个BeanPostProcessor（ApplicationListenerDetector），用于在bean实例化完成并且调用完初始化方法后，如果是ApplicationListener，就注册到多播器中
        beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));

        //添加一个BeanPostProcessor（LoadTimeWeaverAwareProcessor），用于处理LoadTimeWeaverAware接口的逻辑
        if (beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
            beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
            // Set a temporary ClassLoader for type matching.
            beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
        }

        //注册一些固定的singleton bean 
        if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
            beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
        }
        if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
            beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
        }
        if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
            beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
        }
    }

    //refresh结束
    protected void finishRefresh() {
        //初始化并注册LifecycleProcessor
        initLifecycleProcessor();

        //调用上一步初始化的LifecycleProcessor的onRefresh()方法
        getLifecycleProcessor().onRefresh();

        //发布事件ContextRefreshedEvent
        publishEvent(new ContextRefreshedEvent(this));

        //将ApplicationContext注册到LiveBeansView中
        LiveBeansView.registerApplicationContext(this);
    }
}
```