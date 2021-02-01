##步骤一：通过注解@EnableAspectJAutoProxy来开启AOP功能。  
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
		    //设置proxyTargetClass属性，是否强制使用cglib生成代理对象
			AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
		}
		if (enableAspectJAutoProxy.getBoolean("exposeProxy")) {
		    //设置exposeProxy属性
			AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
		}
	}

}
```

##步骤二：AnnotationAwareAspectJAutoProxyCreator详解  
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


下面通过代码展示看下这三个接口方法具体的逻辑：  
```java
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
        implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {

    public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
        //获取beanName，主要是特殊处理了FactoryBean这种类型的前缀符号
        Object cacheKey = getCacheKey(bean.getClass(), beanName);
        //将早期的bean实例放到map中缓存，在postProcessAfterInitialization方法中会再次使用
        this.earlyProxyReferences.put(cacheKey, bean);
        //必要时包装为一个代理返回
        return wrapIfNecessary(bean, beanName, cacheKey);
    }

    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        //获取beanName，主要是特殊处理了FactoryBean这种类型的前缀符号
        Object cacheKey = getCacheKey(beanClass, beanName);
        
        //初始时targetSourcedBeans是不包含任何beanName的，只有走到后续的生成代理对象了，才会将beanName放到targetSourcedBeans里
        if (beanName == null || !this.targetSourcedBeans.contains(beanName)) {
            if (this.advisedBeans.containsKey(cacheKey)) {
                //已经处理过该beanName了
                return null;
            }
            
            //isInfrastructureClass判断是否是基础设施类，判断bean.getClass()是否是Advice、Pointcut、Advisor、AopInfrastructureBean或者他们的父类或父接口
            //shouldSkip的逻辑为找到所有的Advisor，如果该bean是这些advisor里的aspect，则跳过该beanName
            if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName)) {
                //记录到advisedBeans集合中，记录内容为false，不用生成代理
                this.advisedBeans.put(cacheKey, Boolean.FALSE);
                return null;
            }
        }

        if (beanName != null) {
            TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
            // 如果存在CustomTargetSource就生成代理返回，这个后续再讲
            if (targetSource != null) {
                //targetSourcedBeans集合增加记录
                this.targetSourcedBeans.add(beanName);
                //找到可以加到该bean上的advisor,该方法后面再详细看
                Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
                //生成代理对象（后面步骤再看）
                Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);
                this.proxyTypes.put(cacheKey, proxy.getClass());
                return proxy;
            }
        }

        return null;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean != null) {
            Object cacheKey = getCacheKey(bean.getClass(), beanName);
            //这个比较的逻辑记录一下：首先这个方法的调用时机是在bean已经实例化、已经赋值属性，已经调用过init方法了。
            //比如说有A、B两个bean，A需要代理
            //情况1：不存在循环引用，创建A的doCreateBean一路走下来，根本不会调用getEarlyBeanReference方法，earlyProxyReferences这个集合里也不会有该bean，这里结果为不相等
            //情况2：A、B存在循环引用， 
            // 2.1: 创建A实例
            // 2.2：将getEarlyBeanReference（A）封装到ObjectFactory中并放到了第三级缓存中
            // 2.3：自动装配A中引用的B时触发创建B（A的后续创建流程被暂停）;  
            // 2.4：创建B实例; 
            // 2.5：自动装备B中引用的A时调用到了getBean(A), 这时会从三级缓存中依次获取，最终在第三级缓存拿到了ObjectFactory（2.2步骤放进去的），并执行getEarlyBeanReference（A），并将执行的结果放到了二级缓存里;
            //      earlyProxyReferences方法执行，将bean的原生对象加入到了earlyProxyReferences中，但是返回的是A的代理对象ProxyA;
            // 2.6: B顺利创建完成。
            // 2.7: A的创建流程还是会继续从2.3逻辑之后继续进行，处理完自动装配B完成，再处理initBean是会走到该方法（postProcessAfterInitialization）
            //      这时从earlyProxyReferences中remove出来的A和传入进来的A是一样的，就不会再次处理生成代理类ProxyA的逻辑了。因为A的代理类ProxyA已经在2.5步骤是被放到了二级缓存了。
            if (this.earlyProxyReferences.remove(cacheKey) != bean) {
                //必要时包装为一个代理返回
                return wrapIfNecessary(bean, beanName, cacheKey);
            }
        }
        return bean;
    }
    
    
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
        
        //上面方法中已介绍
        if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
            this.advisedBeans.put(cacheKey, Boolean.FALSE);
            return bean;
        }

        //找到可以加到该bean上的advisor,该方法后面再详细看
        Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
        if (specificInterceptors != DO_NOT_PROXY) {
            //如果找到了可用的advisor，表示该类需要被代理，advisedBeans记录信息
            this.advisedBeans.put(cacheKey, Boolean.TRUE);
            //生成代理对象（后面步骤再看）
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

##步骤三：Aspect、Advice、Pointcut、Advisor的解析
这一步主要顺着getAdvicesAndAdvisorsForBean这个方法看就可以了，该方法主要是找适合一个bean的所有的advisor。可以分为2小步：  
1：找到所有系统中定义的Advice  
2：根据Advice的pointcut，按照表达式判断是否可以匹配到目标类  

```java
public abstract class AbstractAdvisorAutoProxyCreator extends AbstractAutoProxyCreator {
    
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource targetSource) {
        List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
        if (advisors.isEmpty()) {
            return DO_NOT_PROXY;
        }
        return advisors.toArray();
    }

    protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
        //找到所有的Advisor
        List<Advisor> candidateAdvisors = findCandidateAdvisors();
        //查看哪些Advisor可以应用到此类上
        List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
        //如果可用的Advisor集合不为空，添加一个ExposeInvocationInterceptor放到集合里（后面再看）
        extendAdvisors(eligibleAdvisors);
        if (!eligibleAdvisors.isEmpty()) {
            //排序
            eligibleAdvisors = sortAdvisors(eligibleAdvisors);
        }
        return eligibleAdvisors;
    }
}
```

###步骤3.1 找到所有系统中定义的Advice
下面看下查找Advisors方法findCandidateAdvisors
```java
public class AnnotationAwareAspectJAutoProxyCreator extends AspectJAwareAdvisorAutoProxyCreator {
    protected List<Advisor> findCandidateAdvisors() {
        //调用父类的findCandidateAdvisors查找（用来获取xml配置的方式生成好的Advisor）
        List<Advisor> advisors = super.findCandidateAdvisors();
        //通过BeanFactoryAspectJAdvisorsBuilderAdapter查找切面Aspect来构建Advisor
        advisors.addAll(this.aspectJAdvisorsBuilder.buildAspectJAdvisors());
        return advisors;
    }
}
```

下面是父类AbstractAdvisorAutoProxyCreator种查找findCandidateAdvisors介绍  
```java
public abstract class AbstractAdvisorAutoProxyCreator extends AbstractAutoProxyCreator {
    private BeanFactoryAdvisorRetrievalHelper advisorRetrievalHelper = new BeanFactoryAdvisorRetrievalHelperAdapter(beanFactory);;
    
    protected List<Advisor> findCandidateAdvisors() {
        //通过advisorRetrievalHelper继续查找
        return this.advisorRetrievalHelper.findAdvisorBeans();
    }
}

public class BeanFactoryAdvisorRetrievalHelper {
    public List<Advisor> findAdvisorBeans() {
        //第一次处理过后会缓存到内部cachedAdvisorBeanNames中
        String[] advisorNames = this.cachedAdvisorBeanNames;
        
        //还未处理过
        if (advisorNames == null) {
            //从ListableBeanFactory的查询到所有类型为Advisor.class的beanName
            advisorNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                    this.beanFactory, Advisor.class, true, false);
            
            //将查询结果缓存起来
            this.cachedAdvisorBeanNames = advisorNames;
        }
        if (advisorNames.length == 0) {
            return new ArrayList<Advisor>();
        }

        List<Advisor> advisors = new ArrayList<Advisor>();
        for (String name : advisorNames) {
            //默认是true
            if (isEligibleBean(name)) {
                
                if (this.beanFactory.isCurrentlyInCreation(name)) {
                    //如果该Advisor正在创建过程中，不处理
                    if (logger.isDebugEnabled()) {
                        logger.debug("Skipping currently created advisor '" + name + "'");
                    }
                }
                else {
                    try {
                        //将已经创建好的Advisor的bean获取到放到集合中
                        advisors.add(this.beanFactory.getBean(name, Advisor.class));
                    }
                    catch (BeanCreationException ex) {
                        throw ex;
                    }
                }
            }
        }
        return advisors;
    }
}
```

下面是从BeanFactoryAspectJAdvisorsBuilderAdapter中构建Advisor
```java
public class BeanFactoryAspectJAdvisorsBuilder {
    public List<Advisor> buildAspectJAdvisors() {
        //切面类aspect的beanName会解析一次后缓存起来
        List<String> aspectNames = this.aspectBeanNames;

        if (aspectNames == null) {
            //初始的时候还没有aspectNames,加锁处理
            synchronized (this) {
                aspectNames = this.aspectBeanNames;
                if (aspectNames == null) {
                    List<Advisor> advisors = new LinkedList<Advisor>();
                    aspectNames = new LinkedList<String>();
                    
                    //从beanFactory中找到所有的Object.class类型的beanName,然后再循环判断是否是aspect
                    String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                            this.beanFactory, Object.class, true, false);
                    for (String beanName : beanNames) {
                        //处理<aop:include>标签逻辑，看是否匹配
                        if (!isEligibleBean(beanName)) {
                            continue;
                        }
                        
                        //获取该beanName的Class
                        Class<?> beanType = this.beanFactory.getType(beanName);
                        if (beanType == null) {
                            continue;
                        }
                        //根据是否是有@Aspect注解，这里的advisorFactory就是前面接口篇章讲到的了。
                        if (this.advisorFactory.isAspect(beanType)) {
                            aspectNames.add(beanName);
                            AspectMetadata amd = new AspectMetadata(beanType, beanName);
                            //kind属性时从@Aspect注解的value属性里解析出来的，默认是SINGLETON
                            if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
                                MetadataAwareAspectInstanceFactory factory =
                                        new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);
                                
                                //获取所有的Advisor，在前面接口篇章中已经梳理过这个advisor工厂获取advisor的逻辑了。大体逻辑如下：
                                //将定义有Around、Before、After、AfterReturning、AfterThrowing的注解方法找到，或者通过@DeclareParents注解定义的变量
                                //并将Pointcut和这些advice方法一起拼装在一个Advisor里（InstantiationModelAwarePointcutAdvisorImpl或者DeclareParentsAdvisor）
                                List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
                                if (this.beanFactory.isSingleton(beanName)) {
                                    //如果该aspect bean是singleton的，将解析好的Advisor全部缓存到advisorsCache中，后续直接使用
                                    this.advisorsCache.put(beanName, classAdvisors);
                                }
                                else {
                                    //如果该aspect bean是非singleton的，将factory缓存起来，后续直接通过factory.getAdvisors()来获取
                                    this.aspectFactoryCache.put(beanName, factory);
                                }
                                advisors.addAll(classAdvisors);
                            }
                            else {
                                //如果@Aspect注解中配置的是perThis、perTarget这些，但是beanFactory中该bean的定义确实singleton，直接抛出异常
                                if (this.beanFactory.isSingleton(beanName)) {
                                    throw new IllegalArgumentException("Bean with name '" + beanName +
                                            "' is a singleton, but aspect instantiation model is not singleton");
                                }
                                
                                //定义一个PrototypeAspectInstanceFactory用来生成Aspect实例
                                //注意，虽然@Aspect注解里配置了perThis，但是如果@Scope配置了singleton也会抛出异常的
                                MetadataAwareAspectInstanceFactory factory =
                                        new PrototypeAspectInstanceFactory(this.beanFactory, beanName);
                                //缓存aspectFactory
                                this.aspectFactoryCache.put(beanName, factory);
                                //生成Advisor
                                advisors.addAll(this.advisorFactory.getAdvisors(factory));
                            }
                        }
                    }
                    this.aspectBeanNames = aspectNames;
                    return advisors;
                }
            }
        }

        if (aspectNames.isEmpty()) {
            return Collections.emptyList();
        }
        List<Advisor> advisors = new LinkedList<Advisor>();
        for (String aspectName : aspectNames) {
            List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
            if (cachedAdvisors != null) {
                //Singleton 类型的aspect直接从cache中获取
                advisors.addAll(cachedAdvisors);
            }
            else {
                //cache中没有，从缓存中获取到AspectFactory直接构建
                MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
                advisors.addAll(this.advisorFactory.getAdvisors(factory));
            }
        }
        return advisors;
    }
}
```

###步骤3.2 根据Advice的pointcut，按照表达式判断是否可以匹配到目标类  
```java
public abstract class AbstractAdvisorAutoProxyCreator extends AbstractAutoProxyCreator {

    protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
        //找到所有的Advisor
        List<Advisor> candidateAdvisors = findCandidateAdvisors();
        //查看哪些Advisor可以应用到此类上
        List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
        //如果可用的Advisor集合不为空，添加一个ExposeInvocationInterceptor放到集合里
        //该类中该方法为空，子类可以扩展，AspectJAwareAdvisorAutoProxyCreator这个子类重写了该方法
        extendAdvisors(eligibleAdvisors);
        if (!eligibleAdvisors.isEmpty()) {
            //排序
            eligibleAdvisors = sortAdvisors(eligibleAdvisors);
        }
        return eligibleAdvisors;
    }
    
    protected List<Advisor> findAdvisorsThatCanApply(
            List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {
        //做标记表示该beanName正在处理代理逻辑
        ProxyCreationContext.setCurrentProxiedBeanName(beanName);
        try {
            return AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
        }
        finally {
            //移除标记
            ProxyCreationContext.setCurrentProxiedBeanName(null);
        }
    }
}

public abstract class AopUtils {
    
    public static List<Advisor> findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> clazz) {
        if (candidateAdvisors.isEmpty()) {
            return candidateAdvisors;
        }
        List<Advisor> eligibleAdvisors = new LinkedList<Advisor>();
        for (Advisor candidate : candidateAdvisors) {
            //先处理IntroductionAdvisor接口的，通过@DeclareParents生成的Advisor就是这种
            if (candidate instanceof IntroductionAdvisor && canApply(candidate, clazz)) {
                eligibleAdvisors.add(candidate);
            }
        }
        boolean hasIntroductions = !eligibleAdvisors.isEmpty();
        for (Advisor candidate : candidateAdvisors) {
            if (candidate instanceof IntroductionAdvisor) {
                //IntroductionAdvisor接口的上一个循环已经处理过了，这里忽略处理
                continue;
            }
            if (canApply(candidate, clazz, hasIntroductions)) {
                eligibleAdvisors.add(candidate);
            }
        }
        return eligibleAdvisors;
    }

    public static boolean canApply(Advisor advisor, Class<?> targetClass, boolean hasIntroductions) {
        if (advisor instanceof IntroductionAdvisor) {
            //根据@DeclareParents注解配置的value表达式匹配
            return ((IntroductionAdvisor) advisor).getClassFilter().matches(targetClass);
        }
        else if (advisor instanceof PointcutAdvisor) {
            //PointcutAdvisor接口的，可以获取到Pointcut信息，根据Pointcut的表达式来判断是否可以
            PointcutAdvisor pca = (PointcutAdvisor) advisor;
            return canApply(pca.getPointcut(), targetClass, hasIntroductions);
        }
        else {
            //该advisor没有配置Pointcut，所有类都使用
            return true;
        }
    }
}
```

看一下添加ExposeInvocationInterceptor的逻辑
```java
public class AspectJAwareAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator {
    
    /**
     * Adds an {@link ExposeInvocationInterceptor} to the beginning of the advice chain.
     * These additional advices are needed when using AspectJ expression pointcuts
     * and when using AspectJ-style advice.
     */
    @Override
    protected void extendAdvisors(List<Advisor> candidateAdvisors) {
        AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(candidateAdvisors);
    }
}


public abstract class AspectJProxyUtils {

    public static boolean makeAdvisorChainAspectJCapableIfNecessary(List<Advisor> advisors) {
        if (!advisors.isEmpty()) {
            boolean foundAspectJAdvice = false;
            for (Advisor advisor : advisors) {
                // Be careful not to get the Advice without a guard, as
                // this might eagerly instantiate a non-singleton AspectJ aspect
                if (isAspectJAdvice(advisor)) {
                    foundAspectJAdvice = true;
                }
            }
            if (foundAspectJAdvice && !advisors.contains(ExposeInvocationInterceptor.ADVISOR)) {
                //如果有AspectJ类型的Advisor就将ExposeInvocationInterceptor添加到集合头部
                //ExposeInvocationInterceptor这个拦截器会保存当前的Invocation在ThreadLocal中，实际应用在Advice-Interceptor接口中有讲到
                advisors.add(0, ExposeInvocationInterceptor.ADVISOR);
                return true;
            }
        }
        return false;
    }

    private static boolean isAspectJAdvice(Advisor advisor) {
        return (advisor instanceof InstantiationModelAwarePointcutAdvisor ||
                advisor.getAdvice() instanceof AbstractAspectJAdvice ||
                (advisor instanceof PointcutAdvisor &&
                        ((PointcutAdvisor) advisor).getPointcut() instanceof AspectJExpressionPointcut));
    }

}
```

##步骤4：创建代理对象：
```java
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
		implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {
    /**
     * Create an AOP proxy for the given bean.
     */
    protected Object createProxy(
            Class<?> beanClass, String beanName, Object[] specificInterceptors, TargetSource targetSource) {

        if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
            AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName, beanClass);
        }

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.copyFrom(this);

        if (!proxyFactory.isProxyTargetClass()) {
            if (shouldProxyTargetClass(beanClass, beanName)) {
                proxyFactory.setProxyTargetClass(true);
            }
            else {
                evaluateProxyInterfaces(beanClass, proxyFactory);
            }
        }

        Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
        proxyFactory.addAdvisors(advisors);
        proxyFactory.setTargetSource(targetSource);
        customizeProxyFactory(proxyFactory);

        proxyFactory.setFrozen(this.freezeProxy);
        if (advisorsPreFiltered()) {
            proxyFactory.setPreFiltered(true);
        }

        return proxyFactory.getProxy(getProxyClassLoader());
    }
}
```

##步骤5：执行过程  
通过jdk的aop代理来看下就好，主要是看下拦截器链的创建和Invocation的创建
```java
final class JdkDynamicAopProxy implements AopProxy, InvocationHandler, Serializable {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodInvocation invocation;
        Object oldProxy = null;
        boolean setProxyContext = false;

        TargetSource targetSource = this.advised.targetSource;
        Class<?> targetClass = null;
        Object target = null;

        try {

            Object retVal;

            //获取目标对象
            target = targetSource.getTarget();
            if (target != null) {
                targetClass = target.getClass();
            }

            //获取拦截器链，这个方法不细看了，大概逻辑就是：
            //根据advisor获取到advice，如果advice本身就是MethodInterceptor的实现，直接使用，如果不是，封装一下再使用
            List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);

            if (chain.isEmpty()) {
                //如果没有拦截器链，直接反射调用即可，mehtod.invoke(target,args)
                Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
                retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
            }
            else {
                //创建一个MethodInvocation,内部依次调用，这个在前面的接口文章里已经详细说过了
                invocation = new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
                retVal = invocation.proceed();
            }

            return retVal;
        }finally {
            
        }
    }
}
```