理解下AspectInstanceFactory这个接口，对于梳理AOP的源码逻辑是有帮助的。先看一下接口定义：  
```java
/**
 * Interface implemented to provide an instance of an AspectJ aspect.
 * Decouples from Spring's bean factory.
 * 从spring的bean工厂解耦，来创建一个Aspect实例对象
 *
 * <p>Extends the {@link org.springframework.core.Ordered} interface
 * to express an order value for the underlying aspect in a chain.
 * 继承了Ordered接口，可以在有多个AspectInstanceFactory的情况下来排序，决定先用哪个factory来生成Aspect
 */
public interface AspectInstanceFactory extends Ordered {

	/**
	 * Create an instance of this factory's aspect.
     * 创建一个该factory的Aspect实例对象
	 */
	Object getAspectInstance();

	/**
	 * Expose the aspect class loader that this factory uses.
     * 暴露该factory用来加载aspect的ClassLoader
	 */
	ClassLoader getAspectClassLoader();

}
```

从接口的定义可以看到接口的主要作用是用来生成Aspect实例的。  
再实际运行过程中，我们在Aspect中定义的BeforeAdvice、AfterAdvice方法都会被通过反射的方式调用执行：method.invoke(Object obj, Object... args)  
invoke方法需要传入Aspect的实例对象，这个工厂就是用来生成（获取）Aspect的实例对象。

那AspectInstanceFactory可以通过哪些方式来创建Aspect实例呢？  
1. 每次new一个使用，可以的，SimpleAspectInstanceFactory这个实现类就是这么做的。
2. 只new出来一个，然后内部缓存起来后续使用，SingletonAspectInstanceFactory这个实现类就是这么做的。
3. Aspect被看作一个普通的bean加载到IOC容器中，我们直接通过beanFactory.getBean就能获取到了，至于是singleton还是prototype，ioc来控制。
   SimpleBeanFactoryAwareAspectInstanceFactory这个实现类就是这么做的。

下面我们简单看下这三种实现类的代码：  
SimpleAspectInstanceFactory：每次new一个Aspect实例使用  
```java
/**
 * Implementation of {@link AspectInstanceFactory} that creates a new instance
 * of the specified aspect class for every {@link #getAspectInstance()} call.
 */
public class SimpleAspectInstanceFactory implements AspectInstanceFactory {

	private final Class<?> aspectClass;

	public SimpleAspectInstanceFactory(Class<?> aspectClass) {
		Assert.notNull(aspectClass, "Aspect class must not be null");
		this.aspectClass = aspectClass;
	}

	@Override
	public final Object getAspectInstance() {
		try {
            //每次直接newInstance创建一个新的实例返回，aspectClass通过构造方法出入
			return this.aspectClass.newInstance();
		}
		catch (InstantiationException ex) {
			throw new AopConfigException(
					"Unable to instantiate aspect class: " + this.aspectClass.getName(), ex);
		}
		catch (IllegalAccessException ex) {
			throw new AopConfigException(
					"Could not access aspect constructor: " + this.aspectClass.getName(), ex);
		}
	}
}
```

SingletonAspectInstanceFactory：缓存一个实例对象，后续一直使用这一个  
```java
/**
 * Implementation of {@link AspectInstanceFactory} that is backed by a
 * specified singleton object, returning the same instance for every
 * {@link #getAspectInstance()} call.
 *
 */
@SuppressWarnings("serial")
public class SingletonAspectInstanceFactory implements AspectInstanceFactory, Serializable {

	private final Object aspectInstance;


	/**
	 * Create a new SingletonAspectInstanceFactory for the given aspect instance.
	 * @param aspectInstance the singleton aspect instance
	 */
	public SingletonAspectInstanceFactory(Object aspectInstance) {
		Assert.notNull(aspectInstance, "Aspect instance must not be null");
		this.aspectInstance = aspectInstance;
	}


	@Override
	public final Object getAspectInstance() {
	    //每次都使用本地缓存的aspectInstance对象返回出去，aspectInstance对象通过构造方法传入保存
		return this.aspectInstance;
	}

}
```

SimpleBeanFactoryAwareAspectInstanceFactory: 每次从beanFactory中获取返回。
```java
/**
 * Implementation of {@link AspectInstanceFactory} that locates the aspect from the
 * {@link org.springframework.beans.factory.BeanFactory} using a configured bean name.
 */
public class SimpleBeanFactoryAwareAspectInstanceFactory implements AspectInstanceFactory, BeanFactoryAware {

	private String aspectBeanName;

	private BeanFactory beanFactory;


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		Assert.notNull(this.aspectBeanName, "'aspectBeanName' is required");
	}

	@Override
	public Object getAspectInstance() {
	    //从beanFactory中获取，beanFactory通过实现BeanFactoryAware接口来获取
		return this.beanFactory.getBean(this.aspectBeanName);
	}

}
```