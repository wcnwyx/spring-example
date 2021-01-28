Advice在spring中就是常用的@before、@After、@AfterReturning、@AfterThrowing、@Around 这些通知方法的最上层接口。
还是从最上层往下梳理，最后看实际的实现类再整体理解。  
看这一篇之前一定要把上一篇Joinpoint、Invocation、MethodInvocation的接口概念看一下，这两条线的接口是交叉调用处理的，要不然不好理解。    

##Advice接口  
```java
/**
 * Tag interface for Advice. Implementations can be any type
 * of advice, such as Interceptors.
 * 
 * Advice只是一个标志接口，具体的实现可以是任何类型的通知，比如说拦截器（Interceptor）
 * 没有任何接口方法定义
 */
public interface Advice {

}
```
##Interceptor接口
Advice接口的子接口不止有Interceptor，只是先按照Interceptor这个分支往下梳理而已。  
```java
/**
 * This interface represents a generic interceptor.
 *
 * <p>A generic interceptor can intercept runtime events that occur
 * within a base program. Those events are materialized by (reified
 * in) joinpoints. Runtime joinpoints can be invocations, field
 * access, exceptions... 
 *
 * <p>This interface is not used directly. Use the sub-interfaces
 * to intercept specific events. For instance, the following class
 * implements some specific interceptors in order to implement a
 * debugger:
 *
 * 该接口也可以理解为只是一个标志接口，代表一个通用的拦截器，没有任何接口的实现。
 * 一个通用拦截器可以拦截基本程序中发生的运行时事件，这些事件通过joinpoint来实现（上一个接口文档里介绍的Joinpoint）。运行时的joinpint可以是一个调用（这些上一篇都有介绍过了）。
 * 这个接口不会被直接使用，通过其子接口来拦截特殊的时间。
 * 下面我们就看一个具体的子接口MethodInterceptor
 */
public interface Interceptor extends Advice {

}
```

##MethodInterceptor
```java
/**
 * Intercepts calls on an interface on its way to the target. These
 * are nested "on top" of the target.
 *
 * <p>The user should implement the {@link #invoke(MethodInvocation)}
 * method to modify the original behavior. 
 * 
 */
public interface MethodInterceptor extends Interceptor {
	
	/**
	 * Implement this method to perform extra treatments before and
	 * after the invocation. Polite implementations would certainly
	 * like to invoke {@link Joinpoint#proceed()}.
     * 实现此方法来达到在invocation之前或之后执行额外的处理。 比如说@Before类型的，就是在方法执行前调用处理。
	 * @param invocation the method invocation joinpoint（这个参数看上一篇介绍）
	 */
	Object invoke(MethodInvocation invocation) throws Throwable;

}
```

##五个具体的方法拦截器实现类
接下来我们看具体的MethodInterceptor实现类，分别对应着着五中通知@before、@After、@AfterReturning、@AfterThrowing、@Around。  
注意留一下各个类实现MethodInterceptor的invoke方法逻辑。  
```java
/**
 * @before
 * 该方法拦截器封装了一个MethodBeforeAdvice。
 * MethodBeforeAdvice是Advice的另外一个分支的子接口实现，后面再细看，可以直接看成@before注解标识的通知方法。
 *
 */
public class MethodBeforeAdviceInterceptor implements MethodInterceptor, BeforeAdvice, Serializable {

	private final MethodBeforeAdvice advice;

	//通过构造函数传入MethodBeforeAdvice
	public MethodBeforeAdviceInterceptor(MethodBeforeAdvice advice) {
		Assert.notNull(advice, "Advice must not be null");
		this.advice = advice;
	}

	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {
	    //这个方法的调用要留意下，先是调用了advice.before()方法，再调用mi.proceed().
        //这里就可以看出来，@before标识的通知方法是怎么在目标方法执行前被先调用的。
		this.advice.before(mi.getMethod(), mi.getArguments(), mi.getThis());
		return mi.proceed();
	}

}

/**
 *   @After
 *  这个方法拦截器也是实现了MethodInterceptor，用于@After注解使用。
 *  和MethodBeforeAdviceInterceptor的套路不太一样，一个是封装了一个MethodBeforeAdvice，
 *  这个类是封装了一个aspectJBeforeAdviceMethod（这个参数名的的定义有点迷惑人，为什么带一个Before呢？父类中定义的参数名称是aspectJAdviceMethod）
 *  其实就是一个aspectJ类型的AdviceMethod
 */
public class AspectJAfterAdvice extends AbstractAspectJAdvice
        implements MethodInterceptor, AfterAdvice, Serializable {

    public AspectJAfterAdvice(
            Method aspectJBeforeAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {
        super(aspectJBeforeAdviceMethod, pointcut, aif);
    }
    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        //这个方法的调用要留意下，先是调用了mi.proceed()方法，再调用invokeAdviceMethod.
        //这里就可以看出来，@after标识的通知方法是怎么在目标方法执行后再被调用的。
        try {
            return mi.proceed();
        }
        finally {
            //这个方法就是调用@After标识的通知方法，只不过在父类AbstractAspectJAdvice中实现了
            invokeAdviceMethod(getJoinPointMatch(), null, null);
        }
    }
}


/**
 * @AfterReturning
 * 该方法拦截器封装了一个AfterReturningAdvice，和before的一个套路。
 * MethodBeforeAdvice是Advice的另外一个分支的子接口实现，后面再细看，可以直接看成@before注解标识的通知方法。
 */
public class AfterReturningAdviceInterceptor implements MethodInterceptor, AfterAdvice, Serializable {

    private final AfterReturningAdvice advice;

    public AfterReturningAdviceInterceptor(AfterReturningAdvice advice) {
        Assert.notNull(advice, "Advice must not be null");
        this.advice = advice;
    }

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        Object retVal = mi.proceed();
        this.advice.afterReturning(retVal, mi.getMethod(), mi.getArguments(), mi.getThis());
        return retVal;
    }
}


/**
 * @AfterThrowing
 */
@SuppressWarnings("serial")
public class AspectJAfterThrowingAdvice extends AbstractAspectJAdvice
        implements MethodInterceptor, AfterAdvice, Serializable {

    public AspectJAfterThrowingAdvice(
            Method aspectJBeforeAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {
        super(aspectJBeforeAdviceMethod, pointcut, aif);
    }

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        try {
            return mi.proceed();
        }
        catch (Throwable ex) {
            if (shouldInvokeOnThrowing(ex)) {
                //捕获到异常的时候才调用Advice方法
                invokeAdviceMethod(getJoinPointMatch(), null, ex);
            }
            throw ex;
        }
    }

    private boolean shouldInvokeOnThrowing(Throwable ex) {
        return getDiscoveredThrowingType().isAssignableFrom(ex.getClass());
    }
}

/**
 * @Around
 */
public class AspectJAroundAdvice extends AbstractAspectJAdvice implements MethodInterceptor, Serializable {

    public AspectJAroundAdvice(
            Method aspectJAroundAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {
        super(aspectJAroundAdviceMethod, pointcut, aif);
    }

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        if (!(mi instanceof ProxyMethodInvocation)) {
            throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
        }
        //around的具体实行逻辑后面我们再分析
        ProxyMethodInvocation pmi = (ProxyMethodInvocation) mi;
        ProceedingJoinPoint pjp = lazyGetProceedingJoinPoint(pmi);
        JoinPointMatch jpm = getJoinPointMatch(pmi);
        return invokeAdviceMethod(pjp, jpm, null, null);
    }

    protected ProceedingJoinPoint lazyGetProceedingJoinPoint(ProxyMethodInvocation rmi) {
        return new MethodInvocationProceedingJoinPoint(rmi);
    }
}
```

###方法拦截器链和目标方法通过Invocation的调用时序分析
上一篇分析了Invocation的proceed方法里根据拦截器链索引再出发拦截器的调用，这一篇里看到了各种拦截器的invoke方法实现。  
在这里捋一下调用时序。  
先再展示一下Invocation的proceed方法代码：  
```java
public class ReflectiveMethodInvocation implements ProxyMethodInvocation, Cloneable {

    public Object proceed() throws Throwable {
        //	We start with an index of -1 and increment early.
        //拦截器执行完了，就执行被增强的方法了
        if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
            return invokeJoinpoint();
        }
        //自增索引获取下一个拦截器链中的拦截器
        Object interceptorOrInterceptionAdvice =
                this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
        //将InterceptorAndDynamicMethodMatcher类型的去掉了，逻辑是一样的，多了不方便看
        //是一个拦截器（MethodInterceptor）,直接调用执行
        return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
    }
}
```
比如说Invocation里的拦截器链interceptorsAndDynamicMethodMatchers这个集合里有4个方法拦截器（@AfterThrowing->@AfterReturning->@After->@before）。
其实默认会在拦截器链首部增加一个固定的拦截器ExposeInvocationInterceptor，这个拦截器是用来保存当前的Invocation的，这里忽略不看。  
拦截器链在放到Invocation之前是经过排序的，这个点后面源码梳理的过程中看，排序的顺序是：@AfterThrowing->@AfterReturning->@After->@before  
currentInterceptorIndex缩写为：index   
interceptorsAndDynamicMethodMatchers缩写为：list   
Invocation缩写为：mi  
执行步骤如下：  
1. invoke mi.proceed(). index=-1, list.size-1=3, list.get(++index) 获取到了afterThrowing拦截器，执行拦截器afterThrowing的invoke方法。
2. invoke afterThrowing.invoke(). 该方法中先调用了mi.proceed()，如果捕获到异常再调用了afterThrowing.invokeAdvice()。
3. invoke mi.proceed(). index=0， list.size-1=3, list.get(++index) 获取到了afterReturning拦截器，执行afterReturning拦截器的invoke方法。
4. invoke afterReturning.invoke(). 该方法中先执行了mi.proceed()，再调用了afterReturning.invokeAdvice()方法。
5. invoke mi.proceed(). index=1， list.size-1=3, list.get(++index) 获取到了after拦截器，执行拦截器after的invoke方法。
6. invoke after.invoke(). 该方法中先调用了mi.proceed()，再调用了after.invokeAdvice()。
7. invoke mi.proceed(). index=2， list.size-1=3, list.get(++index)获取到了before拦截器，执行before拦截器的invoke方法。
8. invoke before.invoke(). 放方法中先执行了before.invokeAdvice()，再调用mi.proceed().
9. invoke before.invokeAdvice(). 具体的before方法被执行了。
10. invoke mi.proceed(). index=3，list.size-1=3, if条件成立，执行invokeJoinpoint()方法， 目标方法被执行。
11. 回退到步骤6执行after.invokeAdvice(). 具体的after方法被执行了。
12. 回退到步骤4执行afterReturning.invokeAdvice(). 具体的afterReturning方法被执行了。
13. 回退到步骤2，根据是否捕获到异常，决定afterThrowing.invokeAdvice（）方法是否需要执行。
14. 执行完毕。

