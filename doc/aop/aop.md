通过注解@EnableAspectJAutoProxy来开启AOP功能。
@EnableAspectJAutoProxy注解中定义了@Import(AspectJAutoProxyRegistrar.class)，导入AspectJAutoProxyRegistrar，
该类为ImportBeanDefinitionRegistrar接口的实现，在处理Import功能是会调用其registerBeanDefinitions方法来注入更多的beanDefinition

```java
class AspectJAutoProxyRegistrar implements ImportBeanDefinitionRegistrar {

	/**
	 * Register, escalate, and configure the AspectJ auto proxy creator based on the value
	 * of the @{@link EnableAspectJAutoProxy#proxyTargetClass()} attribute on the importing
	 * {@code @Configuration} class.
	 */
	@Override
	public void registerBeanDefinitions(
			AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

	    //注册一个beanName为org.springframework.aop.config.internalAutoProxyCreator
        //class为AnnotationAwareAspectJAutoProxyCreator的BeanDefinition
		AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry);

		AnnotationAttributes enableAspectJAutoProxy =
				AnnotationConfigUtils.attributesFor(importingClassMetadata, EnableAspectJAutoProxy.class);
		if (enableAspectJAutoProxy.getBoolean("proxyTargetClass")) {
		    //设置proxyTargetClass属性
			AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
		}
		if (enableAspectJAutoProxy.getBoolean("exposeProxy")) {
		    //设置exposeProxy属性
			AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
		}
	}

}
```

先看一下AnnotationAwareAspectJAutoProxyCreator这个类的父类结构以及实现了那些接口：  

``abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware``  
``abstract class AbstractAdvisorAutoProxyCreator extends AbstractAutoProxyCreator``  
``class AspectJAwareAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator``  
``class AnnotationAwareAspectJAutoProxyCreator extends AspectJAwareAdvisorAutoProxyCreator``  
可以看出AnnotationAwareAspectJAutoProxyCreator的父类AbstractAutoProxyCreator实现了SmartInstantiationAwareBeanPostProcessor接口  
SmartInstantiationAwareBeanPostProcessor接口及其父类接口主要在AOP中起作用的有以下几个接口方法:  
1：getEarlyBeanReference 用于获取早期的bean引用，可以返回一个代理对象，引用逻辑在BeanFactory.doCreateBean方法中，调用点在createBeanInstance之后，populateBean之前， 
通过ObjectFactory的包装放到了第三级缓存（singletonFactories）中。    
2：postProcessBeforeInstantiation 引用逻辑在BeanFactory.createBean方法中，在调用实际创建bean对象doCreateBean方法之前，可以直接返回一个代理对象  
3：postProcessAfterInitialization 引用逻辑在BeanFactory.initializeBean方法中，在处理过初始化方法（invokeInitMethods）之后，可以返回一个代理对象  

通过代码可以看出，这三个接口方法都可以生成一个代理对象，先简单写下每个方法在什么情况下会生成代理对象，后续再细看：  
1：getEarlyBeanReference 如果存在循环引用的情况下，会通过该方法生成代理对象。  
2：postProcessBeforeInstantiation 如果有TargetSourceCreator的情况下，该方法直接创建代理对象，都不会调用doCreateBean方法。  
3：postProcessAfterInitialization 默认情况下都是在该方法中返回了代理对象。  


```java
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
        implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {
    
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        //targetSourcedBeans包含该beanName说明已经生成过该bean的代理了
        //targetSourcedBeans在postProcessBeforeInstantiation该方法中创建代理时会add进去记录
        if (beanName != null && this.targetSourcedBeans.contains(beanName)) {
            return bean;
        }
        
        //该bean处理过并且是不需要增强的类
        if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
            return bean;
        }
        
        //isInfrastructureClass判断是否是基础设施类，判断bean.getClass()是否是Advice、Pointcut、Advisor、AopInfrastructureBean或者他们的父类或父接口
        //
        if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
            this.advisedBeans.put(cacheKey, Boolean.FALSE);
            return bean;
        }

        // Create proxy if we have advice.
        Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
        if (specificInterceptors != DO_NOT_PROXY) {
            this.advisedBeans.put(cacheKey, Boolean.TRUE);
            Object proxy = createProxy(
                    bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
            this.proxyTypes.put(cacheKey, proxy.getClass());
            return proxy;
        }

        this.advisedBeans.put(cacheKey, Boolean.FALSE);
        return bean;
    }    
}
```