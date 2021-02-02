###TransactionAttributeSource接口
这个接口就定义了一个方法，获取给定Method的事务属性。  

```java
/**
 * Strategy interface used by {@link TransactionInterceptor} for metadata retrieval.
 *
 * <p>Implementations know how to source transaction attributes, whether from configuration,
 * metadata attributes at source level (such as Java 5 annotations), or anywhere else.
 * 在TransactionInterceptor作为一个策略接口使用
 * 实现类知道具体如何从配置里或者注解里获取事务属性（TransactionAttribute）
 * TransactionAttribute可以理解为@Transaction注解里配置的属性（比如：rollbackFor、transactionManager等）
 * 
 */
public interface TransactionAttributeSource {

	/**
	 * Return the transaction attribute for the given method,
	 * or {@code null} if the method is non-transactional.
     * 获取给定Method的事务属性（TransactionAttribute），
	 */
	TransactionAttribute getTransactionAttribute(Method method, Class<?> targetClass);

}
```

###AbstractFallbackTransactionAttributeSource实现类
```java
/**
 * Abstract implementation of {@link TransactionAttributeSource} that caches
 * attributes for methods and implements a fallback policy: 1. specific target
 * method; 2. target class; 3. declaring method; 4. declaring class/interface.
 * 
 * TransactionAttributeSource的抽象实现，可以缓存method的事务属性并实现后备策略。
 * 什么后备策略呢?这里只是点了4个点，在这个computeTransactionAttribute方法里就提现出来了。
 *
 * <p>Defaults to using the target class's transaction attribute if none is
 * associated with the target method. Any transaction attribute associated with
 * the target method completely overrides a class transaction attribute.
 * If none found on the target class, the interface that the invoked method
 * has been called through (in case of a JDK proxy) will be checked.
 * 
 * 如果目标方法没有事务属性，默认使用目标类的事务属性。
 * 与目标方法相关联的事务属性会完全覆盖类的事务属性。（就是说如果类上有@Transactional，方法上也有，会使用方法上定义的覆盖类上定义的属性）
 * 
 *
 * <p>This implementation caches attributes by method after they are first used.
 * If it is ever desirable to allow dynamic changing of transaction attributes
 * (which is very unlikely), caching could be made configurable. Caching is
 * desirable because of the cost of evaluating rollback rules.
 *
 */
public abstract class AbstractFallbackTransactionAttributeSource implements TransactionAttributeSource {

	/**
	 * 定义一个没有任何属性的TransactionAttribute，用于没有任何事务属性的方法使用
	 */
	@SuppressWarnings("serial")
	private static final TransactionAttribute NULL_TRANSACTION_ATTRIBUTE = new DefaultTransactionAttribute() {
		@Override
		public String toString() {
			return "null";
		}
	};


	/**
     * 使用Map结构缓存解析好的TransactionAttribute,可以是一个特殊的类MethodClassKey
	 */
	private final Map<Object, TransactionAttribute> attributeCache =
			new ConcurrentHashMap<Object, TransactionAttribute>(1024);


	/**
	 * Determine the transaction attribute for this method invocation.
	 * <p>Defaults to the class's transaction attribute if no method attribute is found.
     * 获取此Method的事务属性，如果方法没有，默认使用Class的
	 */
	@Override
	public TransactionAttribute getTransactionAttribute(Method method, Class<?> targetClass) {
		if (method.getDeclaringClass() == Object.class) {
			return null;
		}

		// 首先查看是否有缓存，有的话就直接返回了
		Object cacheKey = getCacheKey(method, targetClass);
		TransactionAttribute cached = this.attributeCache.get(cacheKey);
		if (cached != null) {
			if (cached == NULL_TRANSACTION_ATTRIBUTE) {
				return null;
			}
			else {
				return cached;
			}
		}
		else {
			//缓存中没有记录的话，需要执行获取属性逻辑
			TransactionAttribute txAttr = computeTransactionAttribute(method, targetClass);
			//获取到后放到缓存中
			if (txAttr == null) {
				this.attributeCache.put(cacheKey, NULL_TRANSACTION_ATTRIBUTE);
			}
			else {
				String methodIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
				if (txAttr instanceof DefaultTransactionAttribute) {
					((DefaultTransactionAttribute) txAttr).setDescriptor(methodIdentification);
				}
				this.attributeCache.put(cacheKey, txAttr);
			}
			return txAttr;
		}
	}

	/**
     * 构建一个缓存Map中使用的key
	 */
	protected Object getCacheKey(Method method, Class<?> targetClass) {
		return new MethodClassKey(method, targetClass);
	}

	/**
     * 获取method的事务属性，此方法不会直接将结果进行缓存
	 */
	protected TransactionAttribute computeTransactionAttribute(Method method, Class<?> targetClass) {
		//如果设置的只有public 方法支持事务，但是该方法不是public权限，直接返回null
		if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
			return null;
		}

		// Ignore CGLIB subclasses - introspect the actual user class.
		Class<?> userClass = ClassUtils.getUserClass(targetClass);
		// The method may be on an interface, but we need attributes from the target class.
		// If the target class is null, the method will be unchanged.
		Method specificMethod = ClassUtils.getMostSpecificMethod(method, userClass);
		// If we are dealing with method with generic parameters, find the original method.
		specificMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);

		//首先从specificMethod找事务属性
		TransactionAttribute txAttr = findTransactionAttribute(specificMethod);
		if (txAttr != null) {
			return txAttr;
		}

		//如果没找到，再从specificMethod.getDeclaringClass()找
		txAttr = findTransactionAttribute(specificMethod.getDeclaringClass());
		if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
			return txAttr;
		}

		if (specificMethod != method) {
			// Fallback is to look at the original method.
			txAttr = findTransactionAttribute(method);
			if (txAttr != null) {
				return txAttr;
			}
			// Last fallback is the class of the original method.
			txAttr = findTransactionAttribute(method.getDeclaringClass());
			if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
				return txAttr;
			}
		}

		return null;
	}


	/**
     * 抽象方法，子类实现，找给定类的事务属性
	 */
	protected abstract TransactionAttribute findTransactionAttribute(Class<?> clazz);

	/**
     * 抽象方法，子类实现，找给定方法的事务属性
	 */
	protected abstract TransactionAttribute findTransactionAttribute(Method method);

	/**
	 * 是否是有public权限的方法才有事务的逻辑
	 */
	protected boolean allowPublicMethodsOnly() {
		return false;
	}

}
```

###AnnotationTransactionAttributeSource 具体实现类
```java
/**
 * Implementation of the
 * {@link org.springframework.transaction.interceptor.TransactionAttributeSource}
 * interface for working with transaction metadata in JDK 1.5+ annotation format.
 * 
 * <p>This class reads Spring's JDK 1.5+ {@link Transactional} annotation and
 * exposes corresponding transaction attributes to Spring's transaction infrastructure.
 * Also supports JTA 1.2's {@link javax.transaction.Transactional} and EJB3's
 * {@link javax.ejb.TransactionAttribute} annotation (if present).
 * This class may also serve as base class for a custom TransactionAttributeSource,
 * or get customized through {@link TransactionAnnotationParser} strategies.
 * 
 * TransactionAttributeSource接口的一个实现，
 * 用于处理JDK 1.5+注解格式的事务元数据
 * 
 * 此类读取Spring的JDK 1.5+ @Transactional注解，并将相应的事务属性公开给Spring的事务基础结构。
 * 同样支持JTA 1.2 的 javax.transaction.Transactional注解 和 EJB3的javax.ejb.TransactionAttribute注解
 * 此类也可以用作自定义TransactionAttributeSource的基类，或通过TransactionAnnotationParser策略进行自定义
 */
@SuppressWarnings("serial")
public class AnnotationTransactionAttributeSource extends AbstractFallbackTransactionAttributeSource
		implements Serializable {

    //JTA 1.2的事务类是否有
	private static final boolean jta12Present = ClassUtils.isPresent(
			"javax.transaction.Transactional", AnnotationTransactionAttributeSource.class.getClassLoader());

	//EJB3的事务类是否有
	private static final boolean ejb3Present = ClassUtils.isPresent(
			"javax.ejb.TransactionAttribute", AnnotationTransactionAttributeSource.class.getClassLoader());

	private final boolean publicMethodsOnly;

	//事务注解的一组解析器，如果有JTA1.2的和EJB3的，都加进去
	private final Set<TransactionAnnotationParser> annotationParsers;


	public AnnotationTransactionAttributeSource() {
		this(true);
	}

	public AnnotationTransactionAttributeSource(boolean publicMethodsOnly) {
		this.publicMethodsOnly = publicMethodsOnly;
		
		//初始化事务注解解析器
		this.annotationParsers = new LinkedHashSet<TransactionAnnotationParser>(4);
		this.annotationParsers.add(new SpringTransactionAnnotationParser());
		if (jta12Present) {
			this.annotationParsers.add(new JtaTransactionAnnotationParser());
		}
		if (ejb3Present) {
			this.annotationParsers.add(new Ejb3TransactionAnnotationParser());
		}
	}


	//实现父类的抽象方法，根据给定的类查找事务属性
	@Override
	protected TransactionAttribute findTransactionAttribute(Class<?> clazz) {
		return determineTransactionAttribute(clazz);
	}

	//实现父类的抽象方法，根据给定的方法查找事务的属性
	@Override
	protected TransactionAttribute findTransactionAttribute(Method method) {
		return determineTransactionAttribute(method);
	}

	/**
	 * Determine the transaction attribute for the given method or class.
	 * <p>This implementation delegates to configured
	 * {@link TransactionAnnotationParser TransactionAnnotationParsers}
	 * for parsing known annotations into Spring's metadata attribute class.
	 * Returns {@code null} if it's not transactional.
	 * <p>Can be overridden to support custom annotations that carry transaction metadata.
	 * @param element the annotated method or class
	 * @return the configured transaction attribute, or {@code null} if none was found
     * 根据AnnotatedElement参数来确定事务属性。
     * AnnotatedElement是一个接口，Method和Class都实现了的一个接口，表示可以被注解标注的一个元件（element）
     * 具体的查找逻辑通过各种TransactionAnnotationParser再来解析
	 */
	protected TransactionAttribute determineTransactionAttribute(AnnotatedElement element) {
		if (element.getAnnotations().length > 0) {
			for (TransactionAnnotationParser annotationParser : this.annotationParsers) {
				TransactionAttribute attr = annotationParser.parseTransactionAnnotation(element);
				if (attr != null) {
					return attr;
				}
			}
		}
		return null;
	}

	/**
	 * 重写了父类的方法，父类该方法默认返回的false
	 */
	@Override
	protected boolean allowPublicMethodsOnly() {
		return this.publicMethodsOnly;
	}

}
```

总结：  
1. TransactionAttributeSource接口用来获取方法或者类的事务属性（TransactionAttribute）。   
2. TransactionInterceptor接口使用该接口来实现策略模式，用于获取不通类型的事务属性。
3. AnnotationTransactionAttributeSource是基于注解方式的获取事务属性，其也是应用的策略模式，根据不同的注解（spring的@Transactional、JTA 1.2的@Transactional、EJB3的@TransactionAttribute）
采用不通的TransactionAnnotationParser解析器再进一步获取事务属性。