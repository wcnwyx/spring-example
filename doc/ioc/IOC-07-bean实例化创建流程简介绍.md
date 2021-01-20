本文描述了singleton bean实例化、填充数据、初始化的精简流程，删除掉了很多的细节部分，先对整体的流程有一个大概的认知，后续再细究。  
本文中涉及到的一些spring的其它概念，比如扩展点，不熟悉的先忽略，文章后面会给到这些概念的介绍连接。FactoryBean创建的Bean不在此流程里介绍。

<br/>
###步骤一：  

下面代码展示的是入口类AbstractApplicationContext的refresh()和finishBeanFactoryInitialization()方法

这两个方法列出来的内容只是展示了一下调用链

```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader
        implements ConfigurableApplicationContext, DisposableBean {
    
    @Override
    public void refresh() throws BeansException, IllegalStateException {
        //实例化所有剩余的非lazy-init的singleton bean
        finishBeanFactoryInitialization(beanFactory);
    }

    protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
        //实例化所有剩余的非lazy-init的singleton bean
        beanFactory.preInstantiateSingletons();
    }
}


```

<br/>
###步骤二：  

下面代码展示的是类DefaultListableBeanFactory的preInstantiateSingletons方法介绍

该方法是循环处理之前流程中解析好的BeanDefinition，逐个调用getBean()方法来获取bean实例（如果获取不到就会创建）

BeanDefinition可以理解为把xml文件里的&lt;bean&gt;标签配置内容，解析成一个BeanDefinition类进行封装存储了

```java
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
		implements ConfigurableListableBeanFactory, BeanDefinitionRegistry, Serializable {
    @Override
    public void preInstantiateSingletons() throws BeansException {
        //beanDefinitionNames是在初始化bean配置信息的步骤完成的
        List<String> beanNames = new ArrayList<String>(this.beanDefinitionNames);

        //循环所有的beandefiniation，逐个调用getBean()
        for (String beanName : beanNames) {
            RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
            if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
                //非抽象 && scope=singleton && 非懒加载，通过getBean()方法继续初始化
                getBean(beanName);
            }
        }

        // Trigger post-initialization callback for all applicable beans...
        //[扩展点]初始化完成后 触发执行SmartInitializingSingleton接口的afterSingletonsInstantiated()方法
        for (String beanName : beanNames) {
            Object singletonInstance = getSingleton(beanName);
            if (singletonInstance instanceof SmartInitializingSingleton) {
                final SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;
                smartSingleton.afterSingletonsInstantiated();
            }
        }
    }
}


```

<br/>
###步骤三：  

下面代码展示的是Bean工厂类（AbstractBeanFactory）的getBean()方法介绍

doGetBean方法中的很多获取Bean的逻辑都忽略了，涉及到dependsOn的处理，以及singleton和prototype类型的bean创建调用链


```java
public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {
    @Override
    public Object getBean(String name) throws BeansException {
        return doGetBean(name, null, null, false);
    }

    protected <T> T doGetBean(
            final String name, final Class<T> requiredType, final Object[] args, boolean typeCheckOnly)
            throws BeansException {

        //处理FactoryBean的前缀符号&，FactoryBean的处理不在此进行梳理
        final String beanName = transformedBeanName(name);

        Object sharedInstance = getSingleton(beanName);
        if (sharedInstance != null && args == null) {
            //如果已经初始化过了，直接获取返回
        }
        else {
            try {
                // 循环处理该bean dependsOn的其它bean，也是调用getBean()方法进行处理
                String[] dependsOn = mbd.getDependsOn();
                if (dependsOn != null) {
                    for (String dep : dependsOn) {
                        getBean(dep);
                    }
                }

                if (mbd.isSingleton()) {
                    //创建scope=singleton类型的bean
                    //注意：singleton类型的bean不是直接调用的createBean()方法创建的，而是调用的getSingleton()方法去获取，获取不到再调用createBean()方法创建
                    //getSingleton()方法先忽略掉不看，后面再看，下一步先看createBean()方法
                    sharedInstance = getSingleton(beanName, new ObjectFactory<Object>() {
                        @Override
                        public Object getObject() throws BeansException {
                                //调用createBean方法创建bean
                            return createBean(beanName, mbd, args);
                        }
                    });
                }

                else if (mbd.isPrototype()) {
                    // It's a prototype -> create a new instance.
                    //创建scope=prototype类型的bean,prototype类型的bean不会直接触发创建，会在其他bean引用这个bean的时候被触发创建
                    Object prototypeInstance = null;
                    //调用createBean方法创建bean
                    prototypeInstance = createBean(beanName, mbd, args);
                }
            }catch (BeansException ex) {
            }
        }

        return (T) bean;
    }
}


```

<br/>
###步骤四：  

下面代码展示的是AbstractAutowireCapableBeanFactory类的createBean()和doCreateBean()方法介绍

在createBean方法中，实际创建bean之前会尝试调用BeanPostProcessors生成一个代理对象返回。

doCreateBean方法中，生成bean主要分为三步：

1：创建bean实例 createBeanInstance()

2：bean属性赋值 populateBean()

3：初始化bean initializeBean()


```java
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory
        implements AutowireCapableBeanFactory {
    protected Object createBean(String beanName, RootBeanDefinition mbd, Object[] args) throws BeanCreationException {
        try {
            // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
            //给BeanPostProcessors 一个机会 返回一个代理对象（AOP的代理对象就是在这里创建返回的，知道一下就好，这次不看这里的逻辑）
            Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
            if (bean != null) {
                return bean;
            }
        }

        //调用doCreateBean创建bean
        Object beanInstance = doCreateBean(beanName, mbdToUse, args);
        return beanInstance;
    }



    protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final Object[] args)
            throws BeanCreationException {

        // Instantiate the bean.
        //创建bean实例
        BeanWrapper instanceWrapper = null;
        if (instanceWrapper == null) {
            instanceWrapper = createBeanInstance(beanName, mbd, args);
        }


        // Allow post-processors to modify the merged bean definition.
        //允许PostProcessor再此处修改MergedBeanDefinition
        applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);


        //通过ObjectFactory缓存下来bean实例，用来解决循环引用，就是加到了singletonFactories这个map中去
        //此时缓存下来的bean还未赋值和init()
        addSingletonFactory(beanName, new ObjectFactory<Object>() {
            @Override
            public Object getObject() throws BeansException {
                return getEarlyBeanReference(beanName, mbd, bean);
            }
        });


        // Initialize the bean instance.
        Object exposedObject = bean;
        //把BeanDefinition中定义的属性赋值给BeanWrapper,<bean>标签中配置的<property>属性就是再此方法内赋值
        populateBean(beanName, mbd, instanceWrapper);

        if (exposedObject != null) {
            //初始化bean（调用对应的init方法）
            exposedObject = initializeBean(beanName, exposedObject, mbd);
        }


        // Register bean as disposable.
        //处理销毁方法逻辑 destory-method、instanceof DisposableBean
        registerDisposableBeanIfNecessary(beanName, bean, mbd);

        return exposedObject;
    }

    protected void populateBean(String beanName, RootBeanDefinition mbd, BeanWrapper bw) {
        PropertyValues pvs = mbd.getPropertyValues();

        //[扩展点 PostProcessor]
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof InstantiationAwareBeanPostProcessor) {
                InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
                    continueWithPropertyPopulation = false;
                    break;
                }
            }
        }


        // Add property values based on autowire by name if applicable.
        //处理autowireByName逻辑
        if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME) {
            autowireByName(beanName, mbd, bw, newPvs);
        }

        // Add property values based on autowire by type if applicable.
        //处理autowireByType逻辑
        if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
            autowireByType(beanName, mbd, bw, newPvs);
        }

        //[扩展点 PostProcessor]
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof InstantiationAwareBeanPostProcessor) {
                InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                pvs = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
                if (pvs == null) {
                    return;
                }
            }
        }

        //处理property赋值逻辑
        applyPropertyValues(beanName, mbd, bw, pvs);
    }

    protected Object initializeBean(final String beanName, final Object bean, RootBeanDefinition mbd) {
        //处理Aware接口逻辑
        //如果该bean实现了这三个Aware接口的任何一个（BeanNameAware、BeanClassLoaderAware、BeanFactoryAware），则会在此方法内部调用对应的接口方法。
        invokeAwareMethods(beanName, bean);

        //[扩展点]触发执行BeanPostProcessor接口的postProcessBeforeInitialization()方法
        applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);

        //调用init方法(InitializingBean接口的触发，或者配置的init-method)
        invokeInitMethods(beanName, wrappedBean, mbd);

        //[扩展点]触发执行BeanPostProcessor接口的postProcessAfterInitialization()方法
        applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);

        return wrappedBean;
    }
}

```

###补充1：

步骤三中提及到的：singleton类型的bean不是直接调用的createBean()方法创建的，而是调用的getSingleton()方法去获取，获取不到再调用createBean()方法创建。

下面代码展示的是DefaultSingletonBeanRegistry类的getSingleton方法，先尝试去singletonObjects缓存中获取，获取不到在创建。

DefaultSingletonBeanRegistry为一个SingletonBean的注册表类，创建好的Singleton Bean都会放到该类里singletonObjects（一个Map数据结构）变量中进行缓存

```java
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {
    public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
        synchronized (this.singletonObjects) {
            Object singletonObject = this.singletonObjects.get(beanName);
            if (singletonObject == null) {
                //调用ObjectFactory的getObject方法，该方法的内容就是调用下面的AbstractAutowireCapableBeanFactory.crateBean()方法创建bean
                singletonObject = singletonFactory.getObject();
                newSingleton = true;

                if (newSingleton) {
                    //完全初始化好的bean实例放入缓存singletonObjects中
                    addSingleton(beanName, singletonObject);
                }
            }
            return (singletonObject != NULL_OBJECT ? singletonObject : null);
        }
    }
}
```