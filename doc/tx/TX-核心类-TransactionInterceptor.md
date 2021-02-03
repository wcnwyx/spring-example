###TransactionInterceptor
spring的事务也是基于AOP的基础上完成的，TransactionInterceptor通过名字就可以看出是一个拦截器，具体的事务逻辑在其父类里。  

```java
/**
 * AOP Alliance MethodInterceptor for declarative transaction
 * management using the common Spring transaction infrastructure
 * ({@link org.springframework.transaction.PlatformTransactionManager}).
 *
 * <p>Derives from the {@link TransactionAspectSupport} class which
 * contains the integration with Spring's underlying transaction API.
 * TransactionInterceptor simply calls the relevant superclass methods
 * such as {@link #invokeWithinTransaction} in the correct order.
 *
 * <p>TransactionInterceptors are thread-safe.
 *
 * 实现了AOP里的MethodInterceptor接口，所以是一个方法拦截器。
 * 使用了spring的基础事务管理类 PlatformTransactionManager。
 * 从TransactionAspectSupport派生出来的子类，TransactionAspectSupport里包含了spring的底层事务api。
 * TransactionInterceptors是线程安全的。
 */
@SuppressWarnings("serial")
public class TransactionInterceptor extends TransactionAspectSupport implements MethodInterceptor, Serializable {

	public TransactionInterceptor() {
	}

	public TransactionInterceptor(PlatformTransactionManager ptm, Properties attributes) {
		setTransactionManager(ptm);
		setTransactionAttributes(attributes);
	}

	public TransactionInterceptor(PlatformTransactionManager ptm, TransactionAttributeSource tas) {
		setTransactionManager(ptm);
		setTransactionAttributeSource(tas);
	}


	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		//从MethodInvocation中获取目标类
		Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null);

		// 调用父类TransactionAspectSupport的invokeWithinTransaction方法来采取事务执行
		return invokeWithinTransaction(invocation.getMethod(), targetClass, new InvocationCallback() {
			@Override
			public Object proceedWithInvocation() throws Throwable {
				return invocation.proceed();
			}
		});
	}


    //序列化接口支持
	private void writeObject(ObjectOutputStream oos) throws IOException {
		//忽略
	}

    //序列化接口支持
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		//忽略
	}

}
```

###TransactionAspectSupport

```java
public abstract class TransactionAspectSupport implements BeanFactoryAware, InitializingBean {

	/**
     * 将事务的集合数据封装到一个TransactionInfo里，然后保存到ThreadLocal中
	 */
	private static final ThreadLocal<TransactionInfo> transactionInfoHolder =
			new NamedThreadLocal<TransactionInfo>("Current aspect-driven transaction");


	/**
	 * 获取当前线程的TransactionInfo
	 */
	protected static TransactionInfo currentTransactionInfo() throws NoTransactionException {
		return transactionInfoHolder.get();
	}

	/**
     * 获取当前线程的TransactionStatus
	 */
	public static TransactionStatus currentTransactionStatus() throws NoTransactionException {
		TransactionInfo info = currentTransactionInfo();
		if (info == null || info.transactionStatus == null) {
			throw new NoTransactionException("No transaction aspect-managed TransactionStatus in scope");
		}
		return info.transactionStatus;
	}


	private String transactionManagerBeanName;

	private PlatformTransactionManager transactionManager;

	//该接口在基础接口里看过了，用来获取事务属性（TransactionAttribute）
	private TransactionAttributeSource transactionAttributeSource;

	private BeanFactory beanFactory;

	//缓存所有的TransactionManager
	private final ConcurrentMap<Object, PlatformTransactionManager> transactionManagerCache =
			new ConcurrentReferenceHashMap<Object, PlatformTransactionManager>(4);


	/**
     * 实现了BeanFactoryAware接口，BeanFactory会被设置进来，用于从中检索PlatformTransactionManager
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * 实现了InitializingBean接口，用来检测一些必要的属性是否设置
	 */
	@Override
	public void afterPropertiesSet() {
		if (getTransactionManager() == null && this.beanFactory == null) {
			throw new IllegalStateException(
					"Set the 'transactionManager' property or make sure to run within a BeanFactory " +
					"containing a PlatformTransactionManager bean!");
		}
		if (getTransactionAttributeSource() == null) {
			throw new IllegalStateException(
					"Either 'transactionAttributeSource' or 'transactionAttributes' is required: " +
					"If there are no transactional methods, then don't use a transaction aspect.");
		}
	}


	/**
     * 子类通过该方法来实现带事务执行目标方法。
	 */
	protected Object invokeWithinTransaction(Method method, Class<?> targetClass, final InvocationCallback invocation)
			throws Throwable {

		//获取事务属性，如果事务属性为空，表示该方法不需要事务
		final TransactionAttribute txAttr = getTransactionAttributeSource().getTransactionAttribute(method, targetClass);
		//获取TransactionManager
		final PlatformTransactionManager tm = determineTransactionManager(txAttr);
		//给joinpint起一个名字，日志记录用
		final String joinpointIdentification = methodIdentification(method, targetClass, txAttr);

		if (txAttr == null || !(tm instanceof CallbackPreferringPlatformTransactionManager)) {
			//创建一个TransactionInfo，不一定有事务，根据txInfo.hasTransaction()来判断是否有事务
            //没有事务也创建一个TransactionInfo，用于维护正常的TransactionInfo栈结构
			TransactionInfo txInfo = createTransactionIfNecessary(tm, txAttr, joinpointIdentification);

			Object retVal;
			try {
				//执行MethodInvocation链表的下一个节点，最中也会执行到目标方法。AOP中有详细介绍。
				retVal = invocation.proceedWithInvocation();
			}
			catch (Throwable ex) {
				//捕获到异常，如果开启了事务，做rollback回滚操作
				completeTransactionAfterThrowing(txInfo, ex);
				throw ex;
			}
			finally {
			    //当前TransactionInfo出栈
				cleanupTransactionInfo(txInfo);
			}
			//没有异常，如果开启了事务，做commit提交操作
			commitTransactionAfterReturning(txInfo);
			return retVal;
		}
		//CallbackPreferringPlatformTransactionManager类型的逻辑就不看了
	}


	/**
	 * 根据qualifier、transactionManagerBeanName依次查找TransactionManager
     * 如果还未找到，从beanFactory中根据beanType查找
	 */
	protected PlatformTransactionManager determineTransactionManager(TransactionAttribute txAttr) {
		//如何为设置txAttr或者beanFactory，直接返回本类中保存的TransactionManager
		if (txAttr == null || this.beanFactory == null) {
			return getTransactionManager();
		}

		String qualifier = txAttr.getQualifier();
		if (StringUtils.hasText(qualifier)) {
			return determineQualifiedTransactionManager(qualifier);
		}
		else if (StringUtils.hasText(this.transactionManagerBeanName)) {
			return determineQualifiedTransactionManager(this.transactionManagerBeanName);
		}
		else {
			PlatformTransactionManager defaultTransactionManager = getTransactionManager();
			if (defaultTransactionManager == null) {
				defaultTransactionManager = this.transactionManagerCache.get(DEFAULT_TRANSACTION_MANAGER_KEY);
				if (defaultTransactionManager == null) {
					defaultTransactionManager = this.beanFactory.getBean(PlatformTransactionManager.class);
					this.transactionManagerCache.putIfAbsent(
							DEFAULT_TRANSACTION_MANAGER_KEY, defaultTransactionManager);
				}
			}
			return defaultTransactionManager;
		}
	}


	/**
	 * 如果需要的话，创建一个事务
	 */
	@SuppressWarnings("serial")
	protected TransactionInfo createTransactionIfNecessary(
			PlatformTransactionManager tm, TransactionAttribute txAttr, final String joinpointIdentification) {

		//如果没有指定name，封装到DelegatingTransactionAttribute中使用joinpointIdentification代表name
		if (txAttr != null && txAttr.getName() == null) {
			txAttr = new DelegatingTransactionAttribute(txAttr) {
				@Override
				public String getName() {
					return joinpointIdentification;
				}
			};
		}

		TransactionStatus status = null;
		if (txAttr != null) {
			if (tm != null) {
			    //获取当前事务或者开启一个新的事务
				status = tm.getTransaction(txAttr);
			}
		}
		return prepareTransactionInfo(tm, txAttr, joinpointIdentification, status);
	}

	/**
     * 将给定的事务信息封装为一个TransactionInfo对象
     * 即使不开启事务，也会生成一个TransactionInfo，用于保证完整的栈结构
	 */
	protected TransactionInfo prepareTransactionInfo(PlatformTransactionManager tm,
			TransactionAttribute txAttr, String joinpointIdentification, TransactionStatus status) {

		TransactionInfo txInfo = new TransactionInfo(tm, txAttr, joinpointIdentification);
		if (txAttr != null) {
			//开启了事务，将TransactionStatus保存
			txInfo.newTransactionStatus(status);
		}
		else {
            //不开启事务，这里就不会设置TransactionStatus，那么TransactionInfo.hasTransaction() 将会返回false
		}

		//将当前TransactionInfo保存到ThreadLocal，入栈
		txInfo.bindToThread();
		return txInfo;
	}

	/**
	 * 成功执行完成后commit事务
	 */
	protected void commitTransactionAfterReturning(TransactionInfo txInfo) {
		if (txInfo != null && txInfo.hasTransaction()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() + "]");
			}
			txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
		}
	}

	/**
	 * 捕获到异常后，如果是rollbackOn的异常，执行rollback回滚事务，如果不是，commit提交事务
	 */
	protected void completeTransactionAfterThrowing(TransactionInfo txInfo, Throwable ex) {
		if (txInfo != null && txInfo.hasTransaction()) {
			if (txInfo.transactionAttribute.rollbackOn(ex)) {
				try {
					txInfo.getTransactionManager().rollback(txInfo.getTransactionStatus());
				}
                catch (TransactionSystemException ex2) { }
                catch (RuntimeException ex2) { }
                catch (Error err) { }
			}
			else {
				try {
					txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
				}
                catch (TransactionSystemException ex2) { }
                catch (RuntimeException ex2) { }
                catch (Error err) { }
			}
		}
	}

	/**
	 * 将给定的事务信息里备份的事务信息重新放到ThreadLocal中
	 */
	protected void cleanupTransactionInfo(TransactionInfo txInfo) {
		if (txInfo != null) {
			txInfo.restoreThreadLocalStatus();
		}
	}


	/**
	 * 封装了事务相关的信息
	 */
	protected final class TransactionInfo {

		private final PlatformTransactionManager transactionManager;

		private final TransactionAttribute transactionAttribute;

		private final String joinpointIdentification;

		private TransactionStatus transactionStatus;

		//通过保存老的信息，来维持一个栈结构
		private TransactionInfo oldTransactionInfo;

        public void newTransactionStatus(TransactionStatus status) {
            this.transactionStatus = status;
        }

        //是不是开启了事务，是通过transactionStatus来判断的
        public boolean hasTransaction() {
            return (this.transactionStatus != null);
        }

		private void bindToThread() {
			//备份了老的事务信息
            //将当前事务信息暴露再ThreadLocal中
			this.oldTransactionInfo = transactionInfoHolder.get();
			transactionInfoHolder.set(this);
		}

		private void restoreThreadLocalStatus() {
			// 将备份的老的事务信息重新设置回来
			transactionInfoHolder.set(this.oldTransactionInfo);
		}

	}

}

```