Spring里的Aware接口是一个super interface，没有定义具体的接口方法，该接口在spring容器初始化的过程中以callback方式来进行调用处理。

实际应用中通常会实现其子接口来被容器回调，通过子接口方法设置相应的资源对象（ApplicationContext、ApplicationEventPublisher、ResourceLoader、BeanFactory等）

比较熟悉的子接口有以下这些：

1：接口 ApplicationContextAware：

我们实际应用过程中，任何一个bean如果想获取到其运行所在的ApplicationContext，就可以实现该接口，spring会在初始化的过程通过调用其接口的方法来传递ApplicationContext对象。

其接口方法的定义为：
`void setApplicationContext(ApplicationContext applicationContext) throws BeansException;`

2：接口 MessageSourceAware：  
实现该接口可以获取MessageSource对象

3：接口 BeanFactoryAware：  
实现该接口可以获取BeanFactory对象

4：接口 ResourceLoaderAware：  
实现该接口可以获取ResourceLoader对象

5：接口 ApplicationEventPublisherAware：  
实现该接口可以获取ApplicationEventPublisher对象

6：接口 EnvironmentAware：  
实现该接口可以获取Environment对象

Aware还有一些其它的子接口，就不一一列举了，实现对应的接口都可以用来获取相应的资源对象。