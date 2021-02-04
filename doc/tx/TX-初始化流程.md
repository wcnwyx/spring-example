##@EnableTransactionManagement
通过该注解来开启事务。看一下该注解的源码：    
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(TransactionManagementConfigurationSelector.class)
public @interface EnableTransactionManagement {

	/**
	 * Indicate whether subclass-based (CGLIB) proxies are to be created ({@code true}) as
	 * opposed to standard Java interface-based proxies ({@code false}). The default is
	 * {@code false}. <strong>Applicable only if {@link #mode()} is set to
	 * {@link AdviceMode#PROXY}</strong>.
	 * <p>Note that setting this attribute to {@code true} will affect <em>all</em>
	 * Spring-managed beans requiring proxying, not just those marked with
	 * {@code @Transactional}. For example, other beans marked with Spring's
	 * {@code @Async} annotation will be upgraded to subclass proxying at the same
	 * time. This approach has no negative impact in practice unless one is explicitly
	 * expecting one type of proxy vs another, e.g. in tests.
	 * 表明是否使用CGLIB创建代理还是使用java基于接口的代理。
	 * 默认是false。前提是mode设置为AdviceMode.PROXY.
	 * 注意，设置该属性为true，将影响所有spring管理的bean创建代理，不仅仅是被标注@Transaction的bean。
	 * 例如，其他的bean标注了spring的@Async注解，子类也会升级为代理。
	 */
	boolean proxyTargetClass() default false;

	/**
	 * Indicate how transactional advice should be applied.
	 * <p><b>The default is {@link AdviceMode#PROXY}.</b>
	 * Please note that proxy mode allows for interception of calls through the proxy
	 * only. Local calls within the same class cannot get intercepted that way; an
	 * {@link Transactional} annotation on such a method within a local call will be
	 * ignored since Spring's interceptor does not even kick in for such a runtime
	 * scenario. For a more advanced mode of interception, consider switching this to
	 * {@link AdviceMode#ASPECTJ}.
	 * 
	 * 表明那种事务切面将被应用。
	 * 默认是AdviceMode.PROXY.
	 * 请注意，PROXY模式只允许通过拦截器调用。
	 * 同一类内部的本地调用无法以这种方式被拦截。
	 * 在本地调用中，此类方法上的@Transactional注释将被忽略，因为在这种运行时场景中，Spring的拦截器甚至不会启动。
	 * 要获取一个更高级模式的拦截，请考虑将其换为AdviceMode.ASPECTJ。
	 */
	AdviceMode mode() default AdviceMode.PROXY;

	/**
	 * Indicate the ordering of the execution of the transaction advisor
	 * when multiple advices are applied at a specific joinpoint.
	 * <p>The default is {@link Ordered#LOWEST_PRECEDENCE}.
	 * 
	 * 表明在一个特殊的joinpoint上如果有多个Advice时的顺序
	 */
	int order() default Ordered.LOWEST_PRECEDENCE;

}
```

通过源码可以看出通过@Import注解有导入了TransactionManagementConfigurationSelector，再看下这个类和其父类的源码。  
```java
public class TransactionManagementConfigurationSelector extends AdviceModeImportSelector<EnableTransactionManagement> {

	 /**
	 * 根据给定的AdviceMode来确定需要导入的类。
	 */
	@Override
	protected String[] selectImports(AdviceMode adviceMode) {
		switch (adviceMode) {
			case PROXY:
				return new String[] {AutoProxyRegistrar.class.getName(),
						ProxyTransactionManagementConfiguration.class.getName()};
			case ASPECTJ:
				return new String[] {
						TransactionManagementConfigUtils.TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME};
			default:
				return null;
		}
	}
}


/**
 * Convenient base class for {@link ImportSelector} implementations that select imports
 * based on an {@link AdviceMode} value from an annotation (such as the {@code @Enable*}
 * annotations).
 *
 */
public abstract class AdviceModeImportSelector<A extends Annotation> implements ImportSelector {

	/**
	 * 默认advice mode属性的参数名
	 */
	public static final String DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME = "mode";


	 /**
	 * 从注解的泛型A里，获取AdviceMode属性的参数名称，默认是{@value #DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME}，
	 * 但是子类如果要自定义可以重写该方法。
	 */
	protected String getAdviceModeAttributeName() {
		return DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME;
	}

	 /**
	 * 根据注解类型的泛型A获取AdviceMode，然后调用子类的方法获取需要import的类。
	 */
	@Override
	public final String[] selectImports(AnnotationMetadata importingClassMetadata) {
		Class<?> annType = GenericTypeResolver.resolveTypeArgument(getClass(), AdviceModeImportSelector.class);
		AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(importingClassMetadata, annType);
		if (attributes == null) {
			throw new IllegalArgumentException(String.format(
					"@%s is not present on importing class '%s' as expected",
					annType.getSimpleName(), importingClassMetadata.getClassName()));
		}

		AdviceMode adviceMode = attributes.getEnum(getAdviceModeAttributeName());
		String[] imports = selectImports(adviceMode);
		if (imports == null) {
			throw new IllegalArgumentException("Unknown AdviceMode: " + adviceMode);
		}
		return imports;
	}

	 /**
	 * 子类来根据AdviceMode来确定要导入的类。
	 */
	protected abstract String[] selectImports(AdviceMode adviceMode);

}
```
看下来其实就是ImportSelector来根据条件（AdviceMode）来导入类。
根据PROXY类型看下导入的这两个类：AutoProxyRegistrar、ProxyTransactionManagementConfiguration。  


```java
/**
 * Registers an auto proxy creator against the current {@link BeanDefinitionRegistry}
 * as appropriate based on an {@code @Enable*} annotation having {@code mode} and
 * {@code proxyTargetClass} attributes set to the correct values.
 * 向BeanDefinitionRegistry中注册一个autoProxyCreator，
 * 根据注解@Enable* 里有的mode和proxyTargetClass属性设置正确的值。
 */
public class AutoProxyRegistrar implements ImportBeanDefinitionRegistrar {


	/**
	 * Register, escalate, and configure the standard auto proxy creator (APC) against the
	 * given registry. Works by finding the nearest annotation declared on the importing
	 * {@code @Configuration} class that has both {@code mode} and {@code proxyTargetClass}
	 * attributes. If {@code mode} is set to {@code PROXY}, the APC is registered; if
	 * {@code proxyTargetClass} is set to {@code true}, then the APC is forced to use
	 * subclass (CGLIB) proxying.
	 * <p>Several {@code @Enable*} annotations expose both {@code mode} and
	 * {@code proxyTargetClass} attributes. It is important to note that most of these
	 * capabilities end up sharing a {@linkplain AopConfigUtils#AUTO_PROXY_CREATOR_BEAN_NAME
	 * single APC}. For this reason, this implementation doesn't "care" exactly which
	 * annotation it finds -- as long as it exposes the right {@code mode} and
	 * {@code proxyTargetClass} attributes, the APC can be registered and configured all
	 * the same.
	 * 向BeanDefinitionRegistry中注册、升级、配置一个标准的autoProxyCreator，就是InfrastructureAdvisorAutoProxyCreator
	 */
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		boolean candidateFound = false;
		Set<String> annTypes = importingClassMetadata.getAnnotationTypes();
		for (String annType : annTypes) {
			AnnotationAttributes candidate = AnnotationConfigUtils.attributesFor(importingClassMetadata, annType);
			if (candidate == null) {
				continue;
			}
			Object mode = candidate.get("mode");
			Object proxyTargetClass = candidate.get("proxyTargetClass");
			if (mode != null && proxyTargetClass != null && AdviceMode.class == mode.getClass() &&
					Boolean.class == proxyTargetClass.getClass()) {
				candidateFound = true;
				if (mode == AdviceMode.PROXY) {
                    //注册一个InfrastructureAdvisorAutoProxyCreator
					AopConfigUtils.registerAutoProxyCreatorIfNecessary(registry);
					if ((Boolean) proxyTargetClass) {
						AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
						return;
					}
				}
			}
		}
	}

}

@Configuration
public class ProxyTransactionManagementConfiguration extends AbstractTransactionManagementConfiguration {

    /**
    * 注册一个BeanFactoryTransactionAttributeSourceAdvisor
    * xxxAdvisor就是在AOP里看到的，封装了Advice和Pointcut
    */
	@Bean(name = TransactionManagementConfigUtils.TRANSACTION_ADVISOR_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public BeanFactoryTransactionAttributeSourceAdvisor transactionAdvisor() {
		BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
		advisor.setTransactionAttributeSource(transactionAttributeSource());
		advisor.setAdvice(transactionInterceptor());
		advisor.setOrder(this.enableTx.<Integer>getNumber("order"));
		return advisor;
	}

    //这个Bean基础接口里说过，就是用来解析@Transactional注解里配置的各种属性的作用
	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public TransactionAttributeSource transactionAttributeSource() {
		return new AnnotationTransactionAttributeSource();
	}

    //这个bean也单独说过，就是一个MethodInterceprot，用来拦截标注有@Transactionl注解的方法，使用事务来执行。
	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public TransactionInterceptor transactionInterceptor() {
		TransactionInterceptor interceptor = new TransactionInterceptor();
		interceptor.setTransactionAttributeSource(transactionAttributeSource());
		if (this.txManager != null) {
			interceptor.setTransactionManager(this.txManager);
		}
		return interceptor;
	}

}
```

整体看下来，完整的流程如下：  
1. 使用@EnableTransactionManagement注解来导入TransactionManagementConfigurationSelector。
2. TransactionManagementConfigurationSelector根据不同的AdviceMode来导入AutoProxyRegistrar和ProxyTransactionManagementConfiguration。
3. AutoProxyRegistrar来注册一个autoProxyCreator（InfrastructureAdvisorAutoProxyCreator）。
4. ProxyTransactionManagementConfiguration来注册三个bean，BeanFactoryTransactionAttributeSourceAdvisor、TransactionAttributeSource和TransactionInterceptor。