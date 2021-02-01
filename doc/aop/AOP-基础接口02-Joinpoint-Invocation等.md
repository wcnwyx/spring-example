从顶层接口往下看，依次是Joinpoint->Invocation->MethodInvocation->ProxyMethodInvocation，最后通过具体的实现类ReflectiveMethodInvocation来理解所有的接口及方法。
ReflectiveMethodInvocation 这个类是在执行代理方法时new出来的，通过这个类把所有的advice和目标方法串联起来执行的。  

###Joinpoit 接口
```java
/**
 * This interface represents a generic runtime joinpoint (in the AOP
 * terminology).
 *
 * <p>A runtime joinpoint is an <i>event</i> that occurs on a static
 * joinpoint (i.e. a location in a the program). For instance, an
 * invocation is the runtime joinpoint on a method (static joinpoint).
 * The static part of a given joinpoint can be generically retrieved
 * using the {@link #getStaticPart()} method.
 *
 * <p>In the context of an interception framework, a runtime joinpoint
 * is then the reification of an access to an accessible object (a
 * method, a constructor, a field), i.e. the static part of the
 * joinpoint. It is passed to the interceptors that are installed on
 * the static joinpoint.
 *
 * 这个接口表示一个通用的runtime joinpoint（在AOP的术语中）
 * 一个runtime joinpoint 就是发生在一个static joinpoint上的事件，static joinpoint可以理解为程序上的一个位置。
 * 比如说，一个invocation(调用，这个接口是joinpoint的子接口) 就是一个runtime joinpoint，它的static joinpoint是一个method方法。
 * 可以根据给定的Joinpoint的getStaticPart()方法获取到它的static part（比如说一个invocation调用getStaticPart其实就是那到被增强的Method）；
 * 
 */
public interface Joinpoint {

	/**
	 * Proceed to the next interceptor in the chain.
	 * <p>The implementation and the semantics of this method depends
	 * on the actual joinpoint type (see the children interfaces).
	 * @return see the children interfaces' proceed definition
     * 继续执行链中的下一个连接器
     * 这个方法的具体实现和语义取决于实际的joinpoint类型（查看其子接口）
     * 返回类型取决于子接口的定义
	 */
	Object proceed() throws Throwable;

	/**
	 * Return the object that holds the current joinpoint's static part.
	 * <p>For instance, the target object for an invocation.
	 * @return the object (can be null if the accessible object is static)
     * 返回持有这个joinpoint的static part。
     * 不好理解，先有个概念，通过其具体的实现类看下来就好理解了，可以理解为获取被增强方法所在的类
	 */
	Object getThis();

	/**
	 * Return the static part of this joinpoint.
	 * <p>The static part is an accessible object on which a chain of
	 * interceptors are installed.
     * 返回 joinpoint的 static part。
     * 这个static part是一个AccessibleObject，该AccessibleObject是被安装了一个拦截器链的。
     * AccessibleObject是java reflect包里的，它是Method、Field、Constructor的父类。
     * 这个方法可以理解为获取被增强的方法
	 */
	AccessibleObject getStaticPart();

}
```

###Invocation 接口
```java
/**
 * This interface represents an invocation in the program.
 * <p>An invocation is a joinpoint and can be intercepted by an
 * interceptor.
 * 这个接口代表程序中的一个调用（它的子接口有方法调用和构造方法调用）
 * 一个Invocation是一个joinpoint（通过集成关系也可以看出来），Invocation可以被拦截求拦截
 */
public interface Invocation extends Joinpoint {

	/**
	 * Get the arguments as an array object.
	 * It is possible to change element values within this
	 * array to change the arguments.
	 * @return the argument of the invocation
     * 获取参数数组，比如说一个方法调用，是会有入参的，这个方法就是获取调用方法是传入的参数，以数组方式返回
	 */
	Object[] getArguments();

}
```

###MethodInvocation 接口
```java
/**
 * Description of an invocation to a method, given to an interceptor
 * upon method-call.
 * <p>A method invocation is a joinpoint and can be intercepted by a
 * method interceptor.
 *
 * 该接口是Invocation的子接口，表示一个方法的调用
 * 一个方法调用也是一个joinpoint，可以被方法拦截器拦截。方法拦截器（MethodInterceptor后续也会梳理）
 */
public interface MethodInvocation extends Invocation {

	/**
	 * Get the method being called.
	 * <p>This method is a frienly implementation of the
	 * @return the method being called
     * 获取被调用的方法，就是返回被增强的方法
	 */
	Method getMethod();

}
```

###ProxyMethodInvocation 接口
```java
/**
 * Extension of the AOP Alliance {@link org.aopalliance.intercept.MethodInvocation}
 * interface, allowing access to the proxy that the method invocation was made through.
 *
 * <p>Useful to be able to substitute return values with the proxy,
 * if necessary, for example if the invocation target returned itself.
 *
 */
public interface ProxyMethodInvocation extends MethodInvocation {

	/**
	 * Return the proxy that this method invocation was made through.
	 * @return the original proxy object
     * 返回这个方法调用所通过的原始代理对象。
     * 比如说我们调用一个被增强的方法，就是返回这个方法所在的类的代理类
	 */
	Object getProxy();

	/**
	 * Create a clone of this object. If cloning is done before {@code proceed()}
	 * is invoked on this object, {@code proceed()} can be invoked once per clone
	 * to invoke the joinpoint (and the rest of the advice chain) more than once.
     * 克隆
	 */
	MethodInvocation invocableClone();

	/**
	 * Create a clone of this object. If cloning is done before {@code proceed()}
	 * is invoked on this object, {@code proceed()} can be invoked once per clone
	 * to invoke the joinpoint (and the rest of the advice chain) more than once.
	 * @param arguments the arguments that the cloned invocation is supposed to use,
	 * overriding the original arguments
     * 克隆（覆盖原始的参数）   
	 */
	MethodInvocation invocableClone(Object... arguments);

	/**
	 * Set the arguments to be used on subsequent invocations in the any advice
	 * in this chain.
	 * @param arguments the argument array
     * 设置方法调用的参数   
	 */
	void setArguments(Object... arguments);

	/**
	 * Add the specified user attribute with the given value to this invocation.
	 * <p>Such attributes are not used within the AOP framework itself. They are
	 * just kept as part of the invocation object, for use in special interceptors.
	 * @param key the name of the attribute
	 * @param value the value of the attribute, or {@code null} to reset it
     * 设置一个用户自定义的属性，AOP框架自身是不使用的。
	 */
	void setUserAttribute(String key, Object value);

	/**
	 * Return the value of the specified user attribute.
	 * @param key the name of the attribute
	 * @return the value of the attribute, or {@code null} if not set
     * 获取一个用户自定义的属性值
	 */
	Object getUserAttribute(String key);

}
```

###ReflectiveMethodInvocation实现类
通过这个具体的实现类，看下里面对上层接口的实现，会更容易理解上面所列的接口的意义。  
```java
/**
 * Spring's implementation of the AOP Alliance
 * {@link org.aopalliance.intercept.MethodInvocation} interface,
 * implementing the extended
 * {@link org.springframework.aop.ProxyMethodInvocation} interface.
 * 
 * <p>Invokes the target object using reflection. Subclasses can override the
 * {@link #invokeJoinpoint()} method to change this behavior, so this is also
 * a useful base class for more specialized MethodInvocation implementations.
 *
 * <p>It is possible to clone an invocation, to invoke {@link #proceed()}
 * repeatedly (once per clone), using the {@link #invocableClone()} method.
 * It is also possible to attach custom attributes to the invocation,
 * using the {@link #setUserAttribute} / {@link #getUserAttribute} methods.
 *
 * <p><b>NOTE:</b> This class is considered internal and should not be
 * directly accessed. The sole reason for it being public is compatibility
 * with existing framework integrations (e.g. Pitchfork). For any other
 * purposes, use the {@link ProxyMethodInvocation} interface instead.
 *
 * 是ProxyMethodInvocation的一个实现类。
 * 通过反射来执行目标对象，子类可以覆盖invokeJoinpoint()方法来改变这个行为，所以对于更多的有专业用途的MethodInvocation实现类来说，该类是一个有用的基类。
 * 这个类之后，我们在看一个它的子类（CglibMethodInvocation） 是如何覆盖invokeJoinpoint（）方法的。
 * 该类是用在JDK做代理的情况下，其子类CglibMethodInvocation使用在使用cglib做代理的情况下。
 */
public class ReflectiveMethodInvocation implements ProxyMethodInvocation, Cloneable {

	protected final Object proxy;

	protected final Object target;

	protected final Method method;

	protected Object[] arguments;

	private final Class<?> targetClass;

	/**
	 * Lazily initialized map of user-specific attributes for this invocation.
     * 懒初始化的一个map，用于保存用户自定义的属性
	 */
	private Map<String, Object> userAttributes;

	/**
	 * List of MethodInterceptor and InterceptorAndDynamicMethodMatcher
	 * that need dynamic checks.
     * 这个List数据就是所谓的拦截器连，拦截器类型可以是MethodInterceptor或者InterceptorAndDynamicMethodMatcher
     * InterceptorAndDynamicMethodMatcher就是一个MethodInterceptor和MethodMatcher的封装类而已
	 */
	protected final List<?> interceptorsAndDynamicMethodMatchers;

	/**
	 * Index from 0 of the current interceptor we're invoking.
	 * -1 until we invoke: then the current interceptor.
     * 记录当前拦截器链调用索引位置
	 */
	private int currentInterceptorIndex = -1;


	/**
	 * Construct a new ReflectiveMethodInvocation with the given arguments.
	 * @param proxy the proxy object that the invocation was made on
	 * @param target the target object to invoke
	 * @param method the method to invoke
	 * @param arguments the arguments to invoke the method with
	 * @param targetClass the target class, for MethodMatcher invocations
	 * @param interceptorsAndDynamicMethodMatchers interceptors that should be applied,
	 * along with any InterceptorAndDynamicMethodMatchers that need evaluation at runtime.
	 * MethodMatchers included in this struct must already have been found to have matched
	 * as far as was possibly statically. Passing an array might be about 10% faster,
	 * but would complicate the code. And it would work only for static pointcuts.
	 */
	protected ReflectiveMethodInvocation(
			Object proxy, Object target, Method method, Object[] arguments,
			Class<?> targetClass, List<Object> interceptorsAndDynamicMethodMatchers) {

		this.proxy = proxy;
		this.target = target;
		this.targetClass = targetClass;
		this.method = BridgeMethodResolver.findBridgedMethod(method);
		this.arguments = AopProxyUtils.adaptArgumentsIfNecessary(method, arguments);
		this.interceptorsAndDynamicMethodMatchers = interceptorsAndDynamicMethodMatchers;
	}

    //ProxyMethodInvocation接口中定义的方法，再此类中没有体现出来具体怎么使用，就是获取method所在的类的代理对象
	@Override
	public final Object getProxy() {
		return this.proxy;
	}

	//Joinpoint接口中定义的方法，获取被执行的method所在的类，method.invoke(target,args)
	@Override
	public final Object getThis() {
		return this.target;
	}

    //在Joinpoint接口中定义的方法，可以看出在方法调用这种joinpoint中，static part就是一个Method
    @Override
	public final AccessibleObject getStaticPart() {
		return this.method;
	}

	/**
	 * Return the method invoked on the proxied interface.
	 * May or may not correspond with a method invoked on an underlying
	 * implementation of that interface.
     * MethodInvocation接口中定义的方法，获取被增强的方法
	 */
	@Override
	public final Method getMethod() {
		return this.method;
	}

	//Invocation接口中定义的方法，获取方法调用的入参
	@Override
	public final Object[] getArguments() {
		return (this.arguments != null ? this.arguments : new Object[0]);
	}

	//ProxyMethodInvocation接口中定义的方法，设置方法调用的入参
	@Override
	public void setArguments(Object... arguments) {
		this.arguments = arguments;
	}

    /**
     * Joinpoint接口中定义的方法
     * 总体方法意思是获取下一个拦截器，执行，如果没有拦截器了，执行被增强的Method。
     * 这个方法看着好像只是调用了拦截器中的一个拦截器，那拦截器链怎么被逐个处理呢？这个在介绍拦截器（Interceptor）的时候再详细说，
     * 简单点说就是各种拦截器中会在合适的位置再次调用该方法的。
     */
	@Override
	public Object proceed() throws Throwable {
		//	We start with an index of -1 and increment early.
        //拦截器执行完了，就执行被增强的方法了
		if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
			return invokeJoinpoint();
		}

		//自增索引获取下一个拦截器链中的拦截器
		Object interceptorOrInterceptionAdvice =
				this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
		if (interceptorOrInterceptionAdvice instanceof InterceptorAndDynamicMethodMatcher) {
			//如果是InterceptorAndDynamicMethodMatcher，需要根据其内部的MethodMatcher来动态的匹配
			InterceptorAndDynamicMethodMatcher dm =
					(InterceptorAndDynamicMethodMatcher) interceptorOrInterceptionAdvice;
			if (dm.methodMatcher.matches(this.method, this.targetClass, this.arguments)) {
			    //匹配成功，执行拦截器
				return dm.interceptor.invoke(this);
			}
			else {
			    //匹配不成功，递归调用，获取拦截器链的下一个继续执行
				return proceed();
			}
		}
		else {
            //是一个拦截器（MethodInterceptor）,直接调用执行
			return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
		}
	}

	/**
	 * Invoke the joinpoint using reflection.
	 * Subclasses can override this to use custom invocation.
	 * @return the return value of the joinpoint
	 * @throws Throwable if invoking the joinpoint resulted in an exception
     * 该方法不是实现的接口方法，实际内容就是通过反射执行这个所谓的method类型的joinpoint。
     * 实际代码：method.invoke(target,arguments)
	 */
	protected Object invokeJoinpoint() throws Throwable {
		return AopUtils.invokeJoinpointUsingReflection(this.target, this.method, this.arguments);
	}


	/**
	 * This implementation returns a shallow copy of this invocation object,
	 * including an independent copy of the original arguments array.
	 * <p>We want a shallow copy in this case: We want to use the same interceptor
	 * chain and other object references, but we want an independent value for the
	 * current interceptor index.
     * ProxyMethodInvocation接口定义的方法，克隆
	 */
	@Override
	public MethodInvocation invocableClone() {
		Object[] cloneArguments = null;
		if (this.arguments != null) {
			// Build an independent copy of the arguments array.
			cloneArguments = new Object[this.arguments.length];
			System.arraycopy(this.arguments, 0, cloneArguments, 0, this.arguments.length);
		}
		return invocableClone(cloneArguments);
	}

	/**
	 * This implementation returns a shallow copy of this invocation object,
	 * using the given arguments array for the clone.
	 * <p>We want a shallow copy in this case: We want to use the same interceptor
	 * chain and other object references, but we want an independent value for the
	 * current interceptor index.
     * ProxyMethodInvocation接口定义的方法，克隆，并覆盖参数
	 */
	@Override
	public MethodInvocation invocableClone(Object... arguments) {
		// Force initialization of the user attributes Map,
		// for having a shared Map reference in the clone.
		if (this.userAttributes == null) {
			this.userAttributes = new HashMap<String, Object>();
		}

		// Create the MethodInvocation clone.
		try {
			ReflectiveMethodInvocation clone = (ReflectiveMethodInvocation) clone();
			clone.arguments = arguments;
			return clone;
		}
		catch (CloneNotSupportedException ex) {
			throw new IllegalStateException(
					"Should be able to clone object of type [" + getClass() + "]: " + ex);
		}
	}


    //ProxyMethodInvocation接口定义的方法，这只一个用户自定义的属性，就是往map中添加一个记录
    @Override
	public void setUserAttribute(String key, Object value) {
		if (value != null) {
			if (this.userAttributes == null) {
				this.userAttributes = new HashMap<String, Object>();
			}
			this.userAttributes.put(key, value);
		}
		else {
			if (this.userAttributes != null) {
				this.userAttributes.remove(key);
			}
		}
	}

    //ProxyMethodInvocation接口定义的方法，获取用户自定义添加的属性，就是根据key从map中get数据
    @Override
	public Object getUserAttribute(String key) {
		return (this.userAttributes != null ? this.userAttributes.get(key) : null);
	}

	/**
	 * Return user attributes associated with this invocation.
	 * This method provides an invocation-bound alternative to a ThreadLocal.
	 * <p>This map is initialized lazily and is not used in the AOP framework itself.
	 * @return any user attributes associated with this invocation
	 * (never {@code null})
     * 获取用户自定义添加的属性集合
	 */
	public Map<String, Object> getUserAttributes() {
		if (this.userAttributes == null) {
			this.userAttributes = new HashMap<String, Object>();
		}
		return this.userAttributes;
	}

}
```

###CglibMethodInvocation 实现类
```java
/**
 * 该类继承与ReflectiveMethodInvocation，重写了invokeJoinpoint方法
 */
private static class CglibMethodInvocation extends ReflectiveMethodInvocation {

		private final MethodProxy methodProxy;

		private final boolean publicMethod;

		public CglibMethodInvocation(Object proxy, Object target, Method method, Object[] arguments,
				Class<?> targetClass, List<Object> interceptorsAndDynamicMethodMatchers, MethodProxy methodProxy) {

			super(proxy, target, method, arguments, targetClass, interceptorsAndDynamicMethodMatchers);
			this.methodProxy = methodProxy;
			this.publicMethod = Modifier.isPublic(method.getModifiers());
		}

		/**
		 * Gives a marginal performance improvement versus using reflection to
		 * invoke the target when invoking public methods.
		 */
		@Override
		protected Object invokeJoinpoint() throws Throwable {
			if (this.publicMethod) {
			    //如果是public权限的method，通过methodProxy来调用
				return this.methodProxy.invoke(this.target, this.arguments);
			}
			else {
			    //通过父类来调用，调用逻辑就是method.invoke(target,args)
				return super.invokeJoinpoint();
			}
		}
	}
```