spring的五中通知（Advice）：beforeAdvice、afterAdvice、afterReturningAdvice、afterThrowingAdvice、aroundAdvice  

```
public class AspectJMethodBeforeAdvice extends AbstractAspectJAdvice implements MethodBeforeAdvice, Serializable
```

```
public class AspectJAfterAdvice extends AbstractAspectJAdvice
		implements MethodInterceptor, AfterAdvice, Serializable
```

```
public class AspectJAfterReturningAdvice extends AbstractAspectJAdvice
		implements AfterReturningAdvice, AfterAdvice, Serializable
```

```
public class AspectJAfterThrowingAdvice extends AbstractAspectJAdvice
		implements MethodInterceptor, AfterAdvice, Serializable
```

```
public class AspectJAroundAdvice extends AbstractAspectJAdvice implements MethodInterceptor, Serializable
```