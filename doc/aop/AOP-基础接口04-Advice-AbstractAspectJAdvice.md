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
        //将ProxyMethodInvocation封装到一个MethodInvocationProceedingJoinPoint中
        ProceedingJoinPoint pjp = lazyGetProceedingJoinPoint(pmi);
        JoinPointMatch jpm = getJoinPointMatch(pmi);
        //调用父类方法执行@Around标注的通知方法
        return invokeAdviceMethod(pjp, jpm, null, null);
    }

    protected ProceedingJoinPoint lazyGetProceedingJoinPoint(ProxyMethodInvocation rmi) {
        return new MethodInvocationProceedingJoinPoint(rmi);
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

    //获取当前的JoinPoint
    protected JoinPoint getJoinPoint() {
        return currentJoinPoint();
    }

    //获取当前的JoinPoint
    public static JoinPoint currentJoinPoint() {
        //这里就可以看到ExposeInvocationInterceptor的作用了
        MethodInvocation mi = ExposeInvocationInterceptor.currentInvocation();
        if (!(mi instanceof ProxyMethodInvocation)) {
            throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
        }
        ProxyMethodInvocation pmi = (ProxyMethodInvocation) mi;
        JoinPoint jp = (JoinPoint) pmi.getUserAttribute(JOIN_POINT_KEY);
        if (jp == null) {
            //这里也用了一个MethodInvocationProceedingJoinPoint，是不是和AspectJAroundAdvice里的一样呢。
            //这里返回的是jp这个变量，是一个ProceedingJoinPoint，这个JoinPoint不是Invocation的父接口JoinPoint
            jp = new MethodInvocationProceedingJoinPoint(pmi);
            pmi.setUserAttribute(JOIN_POINT_KEY, jp);
        }
        return jp;
    }

    //绑定参数
    protected Object[] argBinding(JoinPoint jp, JoinPointMatch jpMatch, Object returnValue, Throwable ex) {
        calculateArgumentBindings();

        // AMC start
        Object[] adviceInvocationArgs = new Object[this.parameterTypes.length];
        int numBound = 0;

        if (this.joinPointArgumentIndex != -1) {
            //这里就可以看出再Advice方法中写的JoinPoint参数，其实是上面那个方法封装的MethodInvocationProceedingJoinPoint
            adviceInvocationArgs[this.joinPointArgumentIndex] = jp;
            numBound++;
        }
        else if (this.joinPointStaticPartArgumentIndex != -1) {
            adviceInvocationArgs[this.joinPointStaticPartArgumentIndex] = jp.getStaticPart();
            numBound++;
        }

        //省略了后面的代码

        return adviceInvocationArgs;
    }

    //计算参数绑定，处理下JoinPoint的参数
    public final synchronized void calculateArgumentBindings() {
        // The simple case... nothing to bind.
        if (this.argumentsIntrospected || this.parameterTypes.length == 0) {
            return;
        }

        int numUnboundArgs = this.parameterTypes.length;
        Class<?>[] parameterTypes = this.aspectJAdviceMethod.getParameterTypes();
        //这里就可以看出来为什么自定义的Advice方法，如果有JoinPoint参数为什么必须定义在第一个位置了
        if (maybeBindJoinPoint(parameterTypes[0]) || maybeBindProceedingJoinPoint(parameterTypes[0]) ||
                maybeBindJoinPointStaticPart(parameterTypes[0])) {
            //如果解析到了JoinPoint，未绑定的数量减1
            numUnboundArgs--;
        }

        if (numUnboundArgs > 0) {
            //如果还有未绑定的参数，继续绑定，这里就不看了
            bindArgumentsByName(numUnboundArgs);
        }

        this.argumentsIntrospected = true;
    }

    //判断是否绑定了JoinPoint
    private boolean maybeBindJoinPoint(Class<?> candidateParameterType) {
        if (JoinPoint.class == candidateParameterType) {
            //这里就可以看出来为什么自定义的Advice方法，如果有JoinPoint参数为什么必须定义在第一个位置了
            this.joinPointArgumentIndex = 0;
            return true;
        }
        else {
            return false;
        }
    }

    //判断是否绑定了ProceedingJoinPoint（@Around里我们用的就是这个类型的JoinPoint）
    private boolean maybeBindProceedingJoinPoint(Class<?> candidateParameterType) {
        if (ProceedingJoinPoint.class == candidateParameterType) {
            if (!supportsProceedingJoinPoint()) {
                throw new IllegalArgumentException("ProceedingJoinPoint is only supported for around advice");
            }
            //这里就可以看出来为什么自定义的Advice方法，如果有JoinPoint参数为什么必须定义在第一个位置了
            this.joinPointArgumentIndex = 0;
            return true;
        }
        else {
            return false;
        }
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

###AspectJAroundAdvice的执行和MethodInvocationProceedingJoinPoint
AspectJAroundAdvice上面就有源码，这里就不列了，这里看一下MethodInvocationProceedingJoinPoint，主要看下proceed方法。   
再看下以下为什么要invocableClone克隆一个新的MethodInvocation。  
```java
public class MethodInvocationProceedingJoinPoint implements ProceedingJoinPoint, JoinPoint.StaticPart {
    
    //通过构造方法将MethodInvocation保存
    public MethodInvocationProceedingJoinPoint(ProxyMethodInvocation methodInvocation) {
        Assert.notNull(methodInvocation, "MethodInvocation must not be null");
        this.methodInvocation = methodInvocation;
    }
    
    @Override
    public Object proceed() throws Throwable {
        //克隆一个后再执行？为什么呢？
        return this.methodInvocation.invocableClone().proceed();
    }

    @Override
    public Object proceed(Object[] arguments) throws Throwable {
        //新的参数会替换掉老的
        this.methodInvocation.setArguments(arguments);
        //克隆一个后再执行？为什么呢？
        return this.methodInvocation.invocableClone(arguments).proceed();
    }
}

@Component
@Aspect
public class DemoAspect {
    //自定义一个round方法，目标方法叫div，两个int相除，可以把除数设置成0让抛出ArithmeticException
    @Around(value = "execution(public int com.wcnwyx.spring.aop.example.DemoBean.div(..))")
    public Object demoAround(ProceedingJoinPoint joinPoint){
        Object result = null;
        try {
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            System.out.println("demoAround 执行异常"+throwable);
            if(throwable instanceof ArithmeticException){
                //如果抛出了算数异常，我们把除数设置成0再执行
                Object[] args = joinPoint.getArgs();
                args[1] = 1;
                result = joinPoint.proceed(args);
            }
        }finally {
            return result;
        }
    }
}

@Component
public class DemoBean{
  public int div(int a, int b){
   System.out.println("do div.");
   return a/b;
  }
}
```
上面写的Around通知方法是可以正确执行的，ProceedingJoinPoint参数暴露的proceed方法可以多次调用，因为每次proceed调用的都是克隆出来的MethodInvocation。  
其实克隆一个新的也就意味着备份了老的，每次克隆都是从备份的那个老的MethodInvocation重新开始执行。  
这就是为什么MethodInvocationProceedingJoinPoint每次proceed都要克隆一个对象来执行了。  
我们看下MethodInvocation的invocableClone方法注释：  

> This implementation returns a shallow copy of this invocation object,
 including an independent copy of the original arguments array.
 We want a shallow copy in this case: We want to use the same interceptor
 chain and other object references, but we want an independent value for the
 current interceptor index.

但是注意，如果和其他类型的Advice一起使用，比如说@Before一起使用，Around第一次执行了JoinPoint.proceed()之后，到抛出异常之前，  
@Before就已经执行了，捕获到异常再次调用JoinPoint.proceed()，@Before还会再次被执行，注意逻辑能不能重复执行    

如果按照上面的逻辑实际跑下来，在@Before方法里打印下JoinPoint里的div方法参数，比如说我们第一次传入的是1，0两个整数，   
捕获异常后虽然虽然第二次将除数0替换成1再去执行了，但是@Before里那到的方法参数还是1，0这两个，为什么呢？
看一下MethodInvocationProceedingJoinPoint获取参数的方法就知道了    
```java
public class MethodInvocationProceedingJoinPoint implements ProceedingJoinPoint, JoinPoint.StaticPart {
     public Object[] getArgs() {
      if (this.args == null) {
       this.args = this.methodInvocation.getArguments().clone();
      }
      return this.args;
     }

    @Override
    public Object proceed(Object[] arguments) throws Throwable {
     //新的参数会替换掉老的
     this.methodInvocation.setArguments(arguments);
     //克隆一个后再执行？为什么呢？
     return this.methodInvocation.invocableClone(arguments).proceed();
    }
}
```
getArgs()第一次执行，会将arguments数组克隆一份（注意是浅克隆）缓存起来，虽然说proceed(Object[] arguments)的时候将老的methodInvocation的arg也修改了，  
但是MethodInvocationProceedingJoinPoint已经将args缓存起来了，再次获取还是第一次缓存的arg数组两个int 1和0.  


那在@AfterThrowing捕获以下异常，再次调用proceed执行一次是否可以呢？  
不可以，看下AspectJAfterThrowingAdvice的invoke方法，方法返回值都没有办法返回出去的。  
