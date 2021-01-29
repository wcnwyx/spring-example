这一篇看下Advisor接口及其子接口。  

###Advisor接口
```java
/**
 * 该接口持有 一个Advice，可以通过接口方法获取。
 */
public interface Advisor {

	/**
     * 返回一个Advice，这个Advice可以是一个Interceptor、beforeAdvice 等
	 */
	Advice getAdvice();

	/**
	 * Return whether this advice is associated with a particular instance
	 * (for example, creating a mixin) or shared with all instances of
	 * the advised class obtained from the same Spring bean factory.
	 * <p><b>Note that this method is not currently used by the framework.</b>
	 * Typical Advisor implementations always return {@code true}.
	 * Use singleton/prototype bean definitions or appropriate programmatic
	 * proxy creation to ensure that Advisors have the correct lifecycle model.
	 * @return whether this advice is associated with a particular target instance
     * 返回此advice是与特定实例关联（例如，创建一个mixin）还是与从同一Spring bean工厂获得的advice类的所有实例共享。
     * 该框架目前未使用此方法。
     * 典型的Advisor实现总是返回true。
     * 使用 singleton/prototype bean定义和适当的程序化创建以确保 Advisor有正确的生命周期模型。
     * 默认的情况下不用，但是还是可以用的。
	 */
	boolean isPerInstance();

}
```

###PointcutAdvisor接口
Advice的子接口不止PointcutAdvisor,还有一个IntroductionAdvisor,用来给目标类动态添加接口的，这个先不看。  
```java
/**
 * Superinterface for all Advisors that are driven by a pointcut.
 * This covers nearly all advisors except introduction advisors,
 * 所有以pointcut驱动的Advisor的superinterface,
 * 该机接口几乎涵盖了除IntroductionAdvisors之外的所有advisor,
 * 就是说一个advice，如果是通过pointcut来定位使用的，都是这个接口的子类。
 */
public interface PointcutAdvisor extends Advisor {

	/**
	 * 获取驱动这个Advice的Pointcut
	 */
	Pointcut getPointcut();

}
```

###InstantiationModelAwarePointcutAdvisor接口
```java
/**
 * Interface to be implemented by Spring AOP Advisors wrapping AspectJ
 * aspects that may have a lazy initialization strategy. For example,
 * a perThis instantiation model would mean lazy initialization of the advice.
 * 该接口被封装了Aspect的Advisor实现，以拥有懒惰的初始化策略。
 * 例如：perThis 初始化模型就会懒惰的初始化advice。
 * perThis不常用，但是就是@Aspect注解里可以配置
 */
public interface InstantiationModelAwarePointcutAdvisor extends PointcutAdvisor {

    public InstantiationModelAwarePointcutAdvisorImpl(AspectJExpressionPointcut declaredPointcut,
                                                      Method aspectJAdviceMethod, AspectJAdvisorFactory aspectJAdvisorFactory,
                                                      MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) {
        this.declaredPointcut = declaredPointcut;
        this.declaringClass = aspectJAdviceMethod.getDeclaringClass();
        this.methodName = aspectJAdviceMethod.getName();
        this.parameterTypes = aspectJAdviceMethod.getParameterTypes();
        this.aspectJAdviceMethod = aspectJAdviceMethod;
        this.aspectJAdvisorFactory = aspectJAdvisorFactory;
        this.aspectInstanceFactory = aspectInstanceFactory;
        this.declarationOrder = declarationOrder;
        this.aspectName = aspectName;

        //根据Aspect的元数据来判断是否延迟初始化
        if (aspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
            Pointcut preInstantiationPointcut = Pointcuts.union(
                    aspectInstanceFactory.getAspectMetadata().getPerClausePointcut(), this.declaredPointcut);
            this.pointcut = new PerTargetInstantiationModelPointcut(
                    this.declaredPointcut, preInstantiationPointcut, aspectInstanceFactory);
            this.lazy = true;
        }
        else {
            // A singleton aspect.
            this.pointcut = this.declaredPointcut;
            this.lazy = false;
            this.instantiatedAdvice = instantiateAdvice(this.declaredPointcut);
        }
    }

	/**
	 * 该advisor的advice是否是延迟加载的
	 */
	boolean isLazy();

	/**
	 * 返回该advisor的advice是否已经实例化好了
	 */
	boolean isAdviceInstantiated();

}
```

###InstantiationModelAwarePointcutAdvisorImpl 具体实现类
这个类我们就看一下从Advisor这一套接口实现的方法  
```java
class InstantiationModelAwarePointcutAdvisorImpl
		implements InstantiationModelAwarePointcutAdvisor, AspectJPrecedenceInformation, Serializable {
    /**
     * Advisor接口中定义的方法
     * 因为也实现了InstantiationModelAwarePointcutAdvisor这个接口，所以可能是延迟加载的
     */
    @Override
    public synchronized Advice getAdvice() {
        if (this.instantiatedAdvice == null) {
            this.instantiatedAdvice = instantiateAdvice(this.declaredPointcut);
        }
        return this.instantiatedAdvice;
    }

    private Advice instantiateAdvice(AspectJExpressionPointcut pcut) {
        //调用工厂来生成Advice
        return this.aspectJAdvisorFactory.getAdvice(this.aspectJAdviceMethod, pcut,
                this.aspectInstanceFactory, this.declarationOrder, this.aspectName);
    }

    /**
     * Advisor接口中定义的方法
     * 根据Aspect的元数据来判断
     */
    @Override
    public boolean isPerInstance() {
        return (getAspectMetadata().getAjType().getPerClause().getKind() != PerClauseKind.SINGLETON);
    }

    /**
     * PointcutAdvisor接口中定义的方法
     * pointcut是通过构造方法传入的
     */
    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

    /**
     * InstantiationModelAwarePointcutAdvisor接口中定义的方法
     * lazy参数在构造方法中初始化了
     */
    @Override
    public boolean isLazy() {
        return this.lazy;
    }

    /**
     * InstantiationModelAwarePointcutAdvisor接口中定义的方法
     */
    @Override
    public synchronized boolean isAdviceInstantiated() {
        return (this.instantiatedAdvice != null);
    }
}
```

里面牵扯到了一些perThis这些的语法解析，可以先忽略，默认的就是singleton，这些再AspectInstanceFactory中有讲到。  
先理解下Advisor的定义，可以简单的理解为Advisor就是吧pointcut和advice给封装起来了。  
先有个大概的了解后，再深入看。   