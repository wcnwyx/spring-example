##本文描述了bean获取的精简流程，删除掉了部分的细节代码。

###步骤一：
从BeanFactory的getBean()方法入手，该方法直接调用了doGetBean()方法。  
doGetBean方法主要分为一下三步逻辑：  
1：转换beanName，处理FactoryBean类型的beanName  
2：从缓存中查找singletonBean  
3：如果从缓存中找到了，直接调用getObjectForBeanInstance()方法返回。    
4：如果没有查到，进行创建bean  
&emsp;4.1：如果scope是singleton类型，再次通过getSingleton(String beanName, ObjectFactory<?> singletonFactory)方法查找，因为内部逻辑查找不到会创建，所以这个getSingleton方法肯定会返回一个结果，然后再调用直接调用getObjectForBeanInstance()方法返回。  
&emsp;4.2：如果scope是prototype类型，直接create一个新对象，然后调用getObjectForBeanInstance()方法返回。


```
public abstract class AbstractBeanFactory

@Override
public Object getBean(String name) throws BeansException {
    return doGetBean(name, null, null, false);
}

protected <T> T doGetBean(
        final String name, final Class<T> requiredType, final Object[] args, boolean typeCheckOnly)
        throws BeansException {

    //处理FactoryBean类型的beanName，如果是通过加上前缀&beanName获取的FactoryBean本身,这个方法会将前缀&符号去掉
    final String beanName = transformedBeanName(name);
    Object bean;

    //从缓存中来查找是否保存有实例化好的singletonBean，
    Object sharedInstance = getSingleton(beanName);
    if (sharedInstance != null && args == null) {
        //解决FactoryBean类型的真实对象获取
        bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
    }else {
        //缓存中没有bean实例，走创建流程，创建流程在另外一篇里有介绍，这里就直接把大部分代码去掉了，省的看着乱
        
        //上一步从缓存中获取不到，再通过这个getSingleton方法获取，先讲个大概逻辑，后续再详细看。该方法内部会再次从缓存中查找，找不到再通过参数ObjectFactory的getObject()方法调用createBean()和doCreateBean()来创建
        //所以这个getSingleton()方法肯定是有返回的，后续也没有判断是否为空的代码，ObjectFactory可以先忽略不看。
        if (mbd.isSingleton()) {
            sharedInstance = getSingleton(beanName, new ObjectFactory<Object>() {});
            //解决FactoryBean类型的真实对象获取
            bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
        }
        else if (mbd.isPrototype()) {
            //prototype类型的上一步getSingleton()是肯定获取不到的，所以每一次获取都会再次create
            prototypeInstance = createBean(beanName, mbd, args);
            
            //解决FactoryBean类型的真实对象获取
            bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
        }
        else {
            //非singleton和prototype类型的，先忽略    
        }
    }

    return (T) bean;
}
```
###步骤二：
不管是缓存中已经有了的bean，还是要根据scope去创建bean，最后都有了一个bean对象，有了bean对象都要再次调用getObjectForBeanInstance()方法，下面我们看下这个方法

```
public abstract class AbstractBeanFactory


/**
 * 该方法主要是解决FactoryBean的处理
 */
protected Object getObjectForBeanInstance(
        Object beanInstance, String name, String beanName, RootBeanDefinition mbd) {

    //现在已经有了beanInstance, 这个beanInstance有可能是一个普通的bean，也有可能是一个FactoryBean
    //如果beanInstance不是一个FactoryBean，就直接返回该beanInstance
    //isFactoryDereference()这个方法判断是否通过加了前缀&符号来获取FactoryBean本身，如果有前缀&符号，也直接返回该beanInstance
    if (!(beanInstance instanceof FactoryBean) || BeanFactoryUtils.isFactoryDereference(name)) {
        return beanInstance;
    }

    Object object = null;
    if (mbd == null) {
        //从缓存中获取FactoryBean创建出来的对象，如果有，就返回使用了
        //该缓存是放在FactoryBeanRegistrySupport类的factoryBeanObjectCache这个map结构中
        object = getCachedObjectForFactoryBean(beanName);
    }
    if (object == null) {
        //如果缓存中没有，就调用该方法进行创建，创建完后再放到缓存中去，创建的流程这里就先不看
        object = getObjectFromFactoryBean(factory, beanName, !synthetic);
    }
    return object;
}
```

###步骤三
上一步为止，最上层的获取逻辑就完成了  
下一步我们分析下第一步从缓存中获取singleton的getSingleton(String beanName)方法和创建singleton类型的bean时候调用的getSingleton(String beanName, ObjectFactory<?> singletonFactory) 方法。  
这一步涉及到三个缓存的概念，分别是：singletonObjects、singletonFactories和earlySingletonObjects，这三个缓存map的使用是为了解决循环应用，这里不详细讲，后续篇章再介绍。

```
public class DefaultSingletonBeanRegistry

/** Cache of singleton objects: bean name --> bean instance */
//缓存着完全初始化好的singleton bean
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>(256);

/** Cache of singleton factories: bean name --> ObjectFactory */
//缓存这doCreateBean时创建的ObjectFactory
private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<String, ObjectFactory<?>>(16);

/** Cache of early singleton objects: bean name --> bean instance */
//缓存着singletonFactories这个Map中的ObjectFactory生成的singleton bean
private final Map<String, Object> earlySingletonObjects = new HashMap<String, Object>(16);
    
@Override
public Object getSingleton(String beanName) {
    //allowEarlyReference 参数写死的是true
    return getSingleton(beanName, true);
}

//该方法中设计到从三个map中依次获取，顺序为singletonObjects->earlySingletonObjects->singletonFactory
//从获取的顺序，可以暂时猜测到这三个map 放数据的顺序应该是：
//singletonFactory->earlySingletonObjects->singletonObjects
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    //先从singletonObjects中获取，spring容器正常加载完成后，都是可以获取到的
    Object singletonObject = this.singletonObjects.get(beanName);
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        //如果singletonObjects这个map中没有，并且该bean正在创建中
        synchronized (this.singletonObjects) {
            //再次从earlySingletonObjects这个map中获取
            singletonObject = this.earlySingletonObjects.get(beanName);
            if (singletonObject == null && allowEarlyReference) {
                //再次从singletonFactories这个map中获取，这个map中方的是ObjectFactory，可以通过getObject方法获得bean
                ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                if (singletonFactory != null) {
                    singletonObject = singletonFactory.getObject();
                    //获得后加到earlySingletonObjects中，并从singletonFactories移除
                    this.earlySingletonObjects.put(beanName, singletonObject);
                    this.singletonFactories.remove(beanName);
                }
            }
        }
    }
    return (singletonObject != NULL_OBJECT ? singletonObject : null);
}

//创建singleton bean时调用的getSingleton方法，
public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
    synchronized (this.singletonObjects) {
        Object singletonObject = this.singletonObjects.get(beanName);
        if (singletonObject == null) {
            //再次从singletonObjects中获取，如果没有，就create，通过singletonFactory.getObject()方法创建，创建流程还是doCreateBean()逻辑。
            boolean newSingleton = false;
            try {
                //
                singletonObject = singletonFactory.getObject();
                newSingleton = true;
            }
            if (newSingleton) {
                //新创建好了会加到缓存中
                addSingleton(beanName, singletonObject);
            }
        }
        return (singletonObject != NULL_OBJECT ? singletonObject : null);
    }
}

//singletonFactory.getObject()调用doCreateBean()创建好的bean，会添加到singletonObjects中，并且从singletonFactories、earlySingletonObjects移除
protected void addSingleton(String beanName, Object singletonObject) {
    synchronized (this.singletonObjects) {
        this.singletonObjects.put(beanName, (singletonObject != null ? singletonObject : NULL_OBJECT));
        this.singletonFactories.remove(beanName);
        this.earlySingletonObjects.remove(beanName);
        this.registeredSingletons.add(beanName);
    }
}
```
