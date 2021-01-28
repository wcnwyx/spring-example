这一篇看一下Advice接口的实现类AbstractAspectJAdvice和其子类。  
@Before、@After、@AfterReturning、@AfterThrowing、@Around 五个注解分别被封装成五个子类。  

上一篇介绍Advice->Interceptor这个分支的接口时，梳理过五个MethodInterceptor的子类：  
1. MethodBeforeAdviceInterceptor，内部封装了一个MethodBeforeAdvice接口，它的一个实现类就是这篇要看的AspectJMethodBeforeAdvice。
2. AspectJAfterAdvice，这个类既实现了MethodInterceptor,也继承了AbstractAspectJAdvice。
3. AfterReturningAdviceInterceptor，内部封装了一个AfterReturningAdvice，它的一个实现类就是这篇要看的AspectJAfterReturningAdvice。
4. AspectJAfterThrowingAdvice，这个类即实现了MethodInterceptor,也继承了AbstractAspectJAdvice。
5. AspectJAroundAdvice，这个类即实现了MethodInterceptor,也继承了AbstractAspectJAdvice。

从上面列出的情况可以看到，五种通知（Advice），其中三个自身就实现了MethodInterceptor接口，另外两个没有实现。  
因为方法调用过程中，通知会被封装成MethodInterceptor拦截器链放到Invocation中调用，  
所以另外两个没有实现MethodInterceptor的Advice被单独封装到各自的Method**AdviceInterceptor中。  

下面看下这五个通知的类源码：  
```java
public class AspectJMethodBeforeAdvice extends AbstractAspectJAdvice implements MethodBeforeAdvice, Serializable {
	
    @Override
	public void before(Method method, Object[] args, Object target) throws Throwable {
	    //通过父类的invokeAdviceMethod方法执行@Before方法的逻辑。
		invokeAdviceMethod(getJoinPointMatch(), null, null);
	}
}


public class AspectJAfterAdvice extends AbstractAspectJAdvice
        implements MethodInterceptor, AfterAdvice, Serializable {
    
    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        try {
            return mi.proceed();
        }
        finally {
            //通过父类的invokeAdviceMethod方法执行@After方法的逻辑。
            invokeAdviceMethod(getJoinPointMatch(), null, null);
        }
    }
}

public class AspectJAfterReturningAdvice extends AbstractAspectJAdvice
        implements AfterReturningAdvice, AfterAdvice, Serializable {

    @Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
        if (shouldInvokeOnReturnValueOf(method, returnValue)) {
            //通过父类的invokeAdviceMethod方法执行@AfterReturning方法的逻辑。
            invokeAdviceMethod(getJoinPointMatch(), returnValue, null);
        }
    }
}


public class AspectJAfterThrowingAdvice extends AbstractAspectJAdvice
        implements MethodInterceptor, AfterAdvice, Serializable {

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        try {
            return mi.proceed();
        }
        catch (Throwable ex) {
            if (shouldInvokeOnThrowing(ex)) {
                //通过父类的invokeAdviceMethod方法执行@AfterThrowing方法的逻辑。
                invokeAdviceMethod(getJoinPointMatch(), null, ex);
            }
            throw ex;
        }
    }
}

public class AspectJAroundAdvice extends AbstractAspectJAdvice implements MethodInterceptor, Serializable {

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        if (!(mi instanceof ProxyMethodInvocation)) {
            throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
        }
        ProxyMethodInvocation pmi = (ProxyMethodInvocation) mi;
        ProceedingJoinPoint pjp = lazyGetProceedingJoinPoint(pmi);
        JoinPointMatch jpm = getJoinPointMatch(pmi);
        //通过父类的invokeAdviceMethod方法执行@Around方法的逻辑。
        return invokeAdviceMethod(pjp, jpm, null, null);
    }
}
```

通过五个通知类可以看到，具体的方法执行逻辑都在父类AbstractAspectJAdvice中，下面我们在看下父类代码：  
```java
/**
 * Base class for AOP Alliance {@link org.aopalliance.aop.Advice} classes
 * wrapping an AspectJ aspect or an AspectJ-annotated advice method.
 *
 */
public abstract class AbstractAspectJAdvice implements Advice, AspectJPrecedenceInformation, Serializable {

    public AbstractAspectJAdvice(
            Method aspectJAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aspectInstanceFactory) {
        Assert.notNull(aspectJAdviceMethod, "Advice method must not be null");
        this.declaringClass = aspectJAdviceMethod.getDeclaringClass();
        this.methodName = aspectJAdviceMethod.getName();
        this.parameterTypes = aspectJAdviceMethod.getParameterTypes();
        this.aspectJAdviceMethod = aspectJAdviceMethod;
        this.pointcut = pointcut;
        this.aspectInstanceFactory = aspectInstanceFactory;
    }
    
    /**
     * Invoke the advice method.
     * 这个方法就是子类调用的来执行通知方法的，我们顺着这个方法往下看
     */
    protected Object invokeAdviceMethod(JoinPointMatch jpMatch, Object returnValue, Throwable ex) throws Throwable {
        //获取参数并继续调用
        return invokeAdviceMethodWithGivenArgs(argBinding(getJoinPoint(), jpMatch, returnValue, ex));
    }

    /**
     * 具体的执行方法
     */
    protected Object invokeAdviceMethodWithGivenArgs(Object[] args) throws Throwable {
        //处理参数
        Object[] actualArgs = args;
        if (this.aspectJAdviceMethod.getParameterTypes().length == 0) {
            actualArgs = null;
        }
        try {
            //设置方法可以访问
            ReflectionUtils.makeAccessible(this.aspectJAdviceMethod);
            //aspectJAdviceMethod就是解析好的通知方法了，通过构造方法传入的。
            //通过反射来调用通知方法，aspectInstanceFactory这个上面几篇已经介绍过，就是来生成Aspect的实例工厂
            return this.aspectJAdviceMethod.invoke(this.aspectInstanceFactory.getAspectInstance(), actualArgs);
        }
        catch (IllegalArgumentException ex) {
            throw new AopInvocationException("Mismatch on arguments to advice method [" +
                    this.aspectJAdviceMethod + "]; pointcut expression [" +
                    this.pointcut.getPointcutExpression() + "]", ex);
        }
        catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }
}
```

这一篇其实主要是理解下Aspect里面定义的Advice和MethodInterceptor的却别，  
虽然MethodInterceptor的顶层接口也是Advice，但是一般说起来Advice还是说的Aspect里具体定义的五中Advice。