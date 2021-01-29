AspectJAdvisorFactory看名字就知道是用来生成Advisor的工厂。

看接口源码：  
```java
/**
 * Interface for factories that can create Spring AOP Advisors from classes
 * annotated with AspectJ annotation syntax.
 * 可以从使用AspectJ注释语法注释的类创建Spring AOP Advisor的工厂的接口
 */
public interface AspectJAdvisorFactory {

	/**
	 * Determine whether or not the given class is an aspect, as reported
	 * by AspectJ's {@link org.aspectj.lang.reflect.AjTypeSystem}.
     * 确定给定的类是否是一个切面（Aspect）.
     * 静态代理方面的先忽略。
	 */
	boolean isAspect(Class<?> clazz);

	/**
	 * Is the given class a valid AspectJ aspect class?
     * 验证给定的aspect
	 */
	void validate(Class<?> aspectClass) throws AopConfigException;

	/**
	 * Build Spring AOP Advisors for all annotated At-AspectJ methods
	 * on the specified aspect instance.
     * 通过aspectInstanceFactory创建出Aspect实例，再构建Advisor
	 */
	List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory);

	/**
	 * Build a Spring AOP Advisor for the given AspectJ advice method.
     * 通过给定的aspect实例和Advice方法，构建Advisor。
	 */
	Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory,
			int declarationOrder, String aspectName);

	/**
	 * Build a Spring AOP Advice for the given AspectJ advice method.
     * 通过给定的aspect实例和Advice方法，构建Advisor。
	 */
	Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut,
			MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName);

}
```

实现类源码：AbstractAspectJAdvisorFactory
这个抽象类里只实现了部分接口，剩余的在其子类里。  
```java
public abstract class AbstractAspectJAdvisorFactory implements AspectJAdvisorFactory {
    @Override
    public boolean isAspect(Class<?> clazz) {
        //compiledByAjc这个方法先不看，静态代理的东西
        return (hasAspectAnnotation(clazz) && !compiledByAjc(clazz));
    }

    private boolean hasAspectAnnotation(Class<?> clazz) {
        //判断是否有@Aspect注解
        return (AnnotationUtils.findAnnotation(clazz, Aspect.class) != null);
    }

    @Override
    public void validate(Class<?> aspectClass) throws AopConfigException {
        //如果父类也有@Apect注解，但是不是抽象类，抛出异常
        if (aspectClass.getSuperclass().getAnnotation(Aspect.class) != null &&
                !Modifier.isAbstract(aspectClass.getSuperclass().getModifiers())) {
            throw new AopConfigException("[" + aspectClass.getName() + "] cannot extend concrete aspect [" +
                    aspectClass.getSuperclass().getName() + "]");
        }

        AjType<?> ajType = AjTypeSystem.getAjType(aspectClass);
        if (!ajType.isAspect()) {
            throw new NotAnAtAspectException(aspectClass);
        }
        //判断@Aspect注解里的value属性，不支持percflow和percflowbelow
        if (ajType.getPerClause().getKind() == PerClauseKind.PERCFLOW) {
            throw new AopConfigException(aspectClass.getName() + " uses percflow instantiation model: " +
                    "This is not supported in Spring AOP.");
        }
        if (ajType.getPerClause().getKind() == PerClauseKind.PERCFLOWBELOW) {
            throw new AopConfigException(aspectClass.getName() + " uses percflowbelow instantiation model: " +
                    "This is not supported in Spring AOP.");
        }
    }
}
```

实现类源码：ReflectiveAspectJAdvisorFactory  
```java
@SuppressWarnings("serial")
public class ReflectiveAspectJAdvisorFactory extends AbstractAspectJAdvisorFactory implements Serializable {

    //生成一个排序器
    private static final Comparator<Method> METHOD_COMPARATOR;

    static {
        //排序规则，以前说到的MethodInterceptor，Invocation中的拦截器链是有序的，就是在这里排序的
        CompoundComparator<Method> comparator = new CompoundComparator<Method>();
        comparator.addComparator(new ConvertingComparator<Method, Annotation>(
                new InstanceComparator<Annotation>(
                        Around.class, Before.class, After.class, AfterReturning.class, AfterThrowing.class),
                new Converter<Method, Annotation>() {
                    @Override
                    public Annotation convert(Method method) {
                        AspectJAnnotation<?> annotation =
                                AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(method);
                        return (annotation != null ? annotation.getAnnotation() : null);
                    }
                }));
        comparator.addComparator(new ConvertingComparator<Method, String>(
                new Converter<Method, String>() {
                    @Override
                    public String convert(Method method) {
                        return method.getName();
                    }
                }));
        METHOD_COMPARATOR = comparator;
    }
    
    @Override
    public List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory) {
        
        //获取aspect的class类型名名称，并校验
        Class<?> aspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
        String aspectName = aspectInstanceFactory.getAspectMetadata().getAspectName();
        validate(aspectClass);

        //将aspectInstanceFactory封装到一个LazySingletonAspectInstanceFactoryDecorator工厂中，用于生成懒加载、单实例的aspect对象
        MetadataAwareAspectInstanceFactory lazySingletonAspectInstanceFactory =
                new LazySingletonAspectInstanceFactoryDecorator(aspectInstanceFactory);

        List<Advisor> advisors = new ArrayList<Advisor>();
        //获取aspectClass中定义的所有非@Pointcut方法
        for (Method method : getAdvisorMethods(aspectClass)) {
            //继续构建Advisor
            Advisor advisor = getAdvisor(method, lazySingletonAspectInstanceFactory, advisors.size(), aspectName);
            if (advisor != null) {
                advisors.add(advisor);
            }
        }

        // If it's a per target aspect, emit the dummy instantiating aspect.
        if (!advisors.isEmpty() && lazySingletonAspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
            Advisor instantiationAdvisor = new SyntheticInstantiationAdvisor(lazySingletonAspectInstanceFactory);
            advisors.add(0, instantiationAdvisor);
        }

        //定义的introduction类型的变量，就是@DeclareParents定义的接口变量
        for (Field field : aspectClass.getDeclaredFields()) {
            Advisor advisor = getDeclareParentsAdvisor(field);
            if (advisor != null) {
                advisors.add(advisor);
            }
        }

        return advisors;
    }

    @Override
    public Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory,
                              int declarationOrderInAspect, String aspectName) {
        //校验aspectClass
        validate(aspectInstanceFactory.getAspectMetadata().getAspectClass());

        //构建pointCut，前面说够，PointcutAdvisor类型的，其实都是封装了pointcut和advice
        AspectJExpressionPointcut expressionPointcut = getPointcut(
                candidateAdviceMethod, aspectInstanceFactory.getAspectMetadata().getAspectClass());
        if (expressionPointcut == null) {
            return null;
        }

        //new 出来一个InstantiationModelAwarePointcutAdvisorImpl返回
        return new InstantiationModelAwarePointcutAdvisorImpl(expressionPointcut, candidateAdviceMethod,
                this, aspectInstanceFactory, declarationOrderInAspect, aspectName);
    }

    //获取aspectClass里所有没有@Pointcut注解的方法，排序返回，Invocation中的有序的拦截器链就是在这里排序的
    private List<Method> getAdvisorMethods(Class<?> aspectClass) {
        final List<Method> methods = new ArrayList<Method>();
        ReflectionUtils.doWithMethods(aspectClass, new ReflectionUtils.MethodCallback() {
            @Override
            public void doWith(Method method) throws IllegalArgumentException {
                // Exclude pointcuts
                if (AnnotationUtils.getAnnotation(method, Pointcut.class) == null) {
                    methods.add(method);
                }
            }
        });
        Collections.sort(methods, METHOD_COMPARATOR);
        return methods;
    }

    //找到pointcut配置并且封装返回
    private AspectJExpressionPointcut getPointcut(Method candidateAdviceMethod, Class<?> candidateAspectClass) {
        //通过注解来找到pointcut
        AspectJAnnotation<?> aspectJAnnotation =
                AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
        if (aspectJAnnotation == null) {
            return null;
        }

        AspectJExpressionPointcut ajexp =
                new AspectJExpressionPointcut(candidateAspectClass, new String[0], new Class<?>[0]);
        ajexp.setExpression(aspectJAnnotation.getPointcutExpression());
        ajexp.setBeanFactory(this.beanFactory);
        return ajexp;
    }
}
```