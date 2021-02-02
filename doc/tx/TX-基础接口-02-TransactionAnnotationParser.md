###TransactionAnnotationParser接口

```java
/**
 * Strategy interface for parsing known transaction annotation types.
 * {@link AnnotationTransactionAttributeSource} delegates to such
 * parsers for supporting specific annotation types such as Spring's own
 * {@link Transactional}, JTA 1.2's {@link javax.transaction.Transactional}
 * or EJB3's {@link javax.ejb.TransactionAttribute}.
 * 
 * 策略接口用来解析事务注解类型。
 * AnnotationTransactionAttributeSource委托一些parsers来解析支持的特殊注解类型，
 * 包括spring自己的@Transactional,JTA 1.2的@Transactional以及ESB3的@TransactionAttribute
 */
public interface TransactionAnnotationParser {

	/**
	 * Parse the transaction attribute for the given method or class,
	 * based on an annotation type understood by this parser.
     * 通过该parser（解析器），解析给定的method和class的基于注解类型的事务属性。
     * AnnotatedElement是jdk自身的一个接口，代表一个可以标注注解的元素（Method和Class都是实现了该接口）
	 */
	TransactionAttribute parseTransactionAnnotation(AnnotatedElement element);

}
```

###SpringTransactionAnnotationParser实现类  
通过上面的接口注释可以知道有三个实现类，spring自身注解的Parser，以及JTA1.2注解的和EJB3注解的parser，之看一个就好。  
```java
/**
 * Strategy implementation for parsing Spring's {@link Transactional} annotation.
 * 解析spring自己的@Transaction注解
 */
@SuppressWarnings("serial")
public class SpringTransactionAnnotationParser implements TransactionAnnotationParser, Serializable {

	@Override
	public TransactionAttribute parseTransactionAnnotation(AnnotatedElement element) {
		AnnotationAttributes attributes = AnnotatedElementUtils.getMergedAnnotationAttributes(
				element, Transactional.class);
		if (attributes != null) {
			return parseTransactionAnnotation(attributes);
		}
		else {
			return null;
		}
	}

	public TransactionAttribute parseTransactionAnnotation(Transactional ann) {
		return parseTransactionAnnotation(AnnotationUtils.getAnnotationAttributes(ann, false, false));
	}

	protected TransactionAttribute parseTransactionAnnotation(AnnotationAttributes attributes) {
		//逐个属性解析，最后封装到了一个RuleBasedTransactionAttribute里返回。
        //每个属性名称其实就是@Transaction注解里的属性
	    RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();

		Propagation propagation = attributes.getEnum("propagation");
		rbta.setPropagationBehavior(propagation.value());
		Isolation isolation = attributes.getEnum("isolation");
		rbta.setIsolationLevel(isolation.value());
		rbta.setTimeout(attributes.getNumber("timeout").intValue());
		rbta.setReadOnly(attributes.getBoolean("readOnly"));
		rbta.setQualifier(attributes.getString("value"));

		//RollbackRuleAttribute就是吧exception的name封装了以下
		List<RollbackRuleAttribute> rollbackRules = new ArrayList<RollbackRuleAttribute>();
		for (Class<?> rbRule : attributes.getClassArray("rollbackFor")) {
			rollbackRules.add(new RollbackRuleAttribute(rbRule));
		}
		for (String rbRule : attributes.getStringArray("rollbackForClassName")) {
			rollbackRules.add(new RollbackRuleAttribute(rbRule));
		}
		for (Class<?> rbRule : attributes.getClassArray("noRollbackFor")) {
			rollbackRules.add(new NoRollbackRuleAttribute(rbRule));
		}
		for (String rbRule : attributes.getStringArray("noRollbackForClassName")) {
			rollbackRules.add(new NoRollbackRuleAttribute(rbRule));
		}
		rbta.setRollbackRules(rollbackRules);

		return rbta;
	}

}
```