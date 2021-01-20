BeanFactoryPostProcessor是spring的一种后置处理器，在spring初始化的refresh（）方法中被调用，调用时机是在BeanDefinition解析完成后，在Bean实例化之前。  
BeanFactoryPostProcessor的调用过程中，牵扯到它的一个子接口BeanDefinitionRegistryPostProcessor的调用处理，会优先处理子接口BeanDefinitionRegistryPostProcessor。  
处理注解@Configuration的就是BeanDefinitionRegistryPostProcessor的一个实现类（ConfigurationClassPostProcessor）。  

BeanFactoryPostProcessor接口的代码如下所示：  
```java
public interface BeanFactoryPostProcessor {

	//该方法会在BeanDefinition解析之后，在bean实例化之前调用
	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}
```
BeanDefinitionRegistryPostProcessor接口的代码如下所示：
```java
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {

    /**
     * Modify the application context's internal bean definition registry after its
     * standard initialization. All regular bean definitions will have been loaded,
     * but no beans will have been instantiated yet. This allows for adding further
     * bean definitions before the next post-processing phase kicks in.
     * @param registry the bean definition registry used by the application context
     * @throws org.springframework.beans.BeansException in case of errors
     */
    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException;

}
```

调用链从AbstractApplicationContext.refresh（）中调用的invokeBeanFactoryPostProcessors（）方法开始展示梳理。

```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader
	implements ConfigurableApplicationContext, DisposableBean{

    public void refresh() throws BeansException, IllegalStateException {
        //调用执行该Context中定义的所有BeanFactoryPostProcessor（后续单独文章中介绍）
        invokeBeanFactoryPostProcessors(beanFactory);
    }
    
    /**
     * Instantiate and invoke all registered BeanFactoryPostProcessor beans,
     * respecting explicit order if given.
     * <p>Must be called before singleton instantiation.
     * 实例化并调用所有注册了的 BeanFactoryPostProcessor，有给定顺序的话按顺序执行，必须在singleton实例化之前调用
     */
    protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
        //获取到beanFactory中所有的BeanFactoryPostProcessors然后去逐个调用
		PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

        if (beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
            //增加了一个BeanPostProcessor,这个BeanPostProcessor这里先不看
            beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
            beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
        }
    }
}

```

```java
class PostProcessorRegistrationDelegate {
    
    //这个方法代码挺长，其实逻辑很简单，先是处理子接口BeanDefinitionRegistryPostProcessor，再处理父接口BeanFactoryPostProcessor
    public static void invokeBeanFactoryPostProcessors(
            ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

        // 存储所有执行过的BeanFactoryPostProcessors，避免重复执行
        Set<String> processedBeans = new HashSet<String>();

        //判断beanFactory是否是BeanDefinitionRegistry实现，因为调用BeanDefinitionRegistryPostProcessor.postProcessBeanDefinitionRegistry（）的时候，是需要传入BeanDefinitionRegistry的
        if (beanFactory instanceof BeanDefinitionRegistry) {
            //转型后供postProcessBeanDefinitionRegistry方法使用
            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

            //beanFactoryPostProcessors方法中非BeanDefinitionRegistryPostProcessor类型的存在该list中保存，后续再调用
            List<BeanFactoryPostProcessor> regularPostProcessors = new LinkedList<BeanFactoryPostProcessor>();

            //所有处理过的BeanDefinitionRegistryPostProcessor保存在该list中，
            //等待BeanDefinitionRegistryPostProcessor.postProcessBeanDefinitionRegistry接口方法执行过后，
            //再执行父接口BeanFactoryPostProcessor.postProcessBeanFactory()接口方法
            List<BeanDefinitionRegistryPostProcessor> registryProcessors = new LinkedList<BeanDefinitionRegistryPostProcessor>();

            //先处理该context中已有的BeanFactoryPostProcessor，如果是BeanDefinitionRegistryPostProcessor，直接调用处理，如果不是，保存到list中后续再处理
            for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
                if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
                    //直接处理，并保存记录
                    BeanDefinitionRegistryPostProcessor registryProcessor =
                            (BeanDefinitionRegistryPostProcessor) postProcessor;
                    registryProcessor.postProcessBeanDefinitionRegistry(registry);
                    registryProcessors.add(registryProcessor);
                } else {
                    //保存到list中后续再处理
                    regularPostProcessors.add(postProcessor);
                }
            }

            // Do not initialize FactoryBeans here: We need to leave all regular beans
            // uninitialized to let the bean factory post-processors apply to them!
            // Separate between BeanDefinitionRegistryPostProcessors that implement
            // PriorityOrdered, Ordered, and the rest.
            List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<BeanDefinitionRegistryPostProcessor>();

            // First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
            //首先处理实现PriorityOrdered接口的，排序后逐个调用
            String[] postProcessorNames =
                    beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
            for (String ppName : postProcessorNames) {
                if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
                    currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
                    processedBeans.add(ppName);
                }
            }
            sortPostProcessors(currentRegistryProcessors, beanFactory);
            registryProcessors.addAll(currentRegistryProcessors);
            invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
            currentRegistryProcessors.clear();

            // Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
            //再调用实现Ordered接口的，排序后逐个调用
            postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
            for (String ppName : postProcessorNames) {
                if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
                    currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
                    processedBeans.add(ppName);
                }
            }
            sortPostProcessors(currentRegistryProcessors, beanFactory);
            registryProcessors.addAll(currentRegistryProcessors);
            invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
            currentRegistryProcessors.clear();

            // Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
            //最后，调用既未实现PriorityOrdered也未实现Ordered接口的
            //这里为什么使用while循环处理呢？可能是因为调用过BeanDefinitionRegistryPostProcessor.postProcessBeanDefinitionRegistry接口方法后，可能会新注册了一个BeanDefinitionRegistryPostProcessor
            boolean reiterate = true;
            while (reiterate) {
                reiterate = false;
                postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
                for (String ppName : postProcessorNames) {
                    if (!processedBeans.contains(ppName)) {
                        currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
                        processedBeans.add(ppName);
                        reiterate = true;
                    }
                }
                sortPostProcessors(currentRegistryProcessors, beanFactory);
                registryProcessors.addAll(currentRegistryProcessors);
                invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
                currentRegistryProcessors.clear();
            }

            // Now, invoke the postProcessBeanFactory callback of all processors handled so far.
            //执行所有BeanDefinitionRegistryPostProcessor实现类的BeanFactoryPostProcessor.postProcessBeanFactory()方法
            invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);

            //执行所有BeanFactoryPostProcessor.postProcessBeanFactory()方法
            invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
        } else {
            // Invoke factory processors registered with the context instance.
            //调用context中注册过的beanFactoryPostProcessor
            invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
        }

        // Do not initialize FactoryBeans here: We need to leave all regular beans
        // uninitialized to let the bean factory post-processors apply to them!
        String[] postProcessorNames =
                beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

        // Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
        // Ordered, and the rest.
        //将BeanFactoryPostProcessor按照是否实现PriorityOrdered接口和Ordered接口分类保存，以供后面分布执行调用
        List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
        List<String> orderedPostProcessorNames = new ArrayList<String>();
        List<String> nonOrderedPostProcessorNames = new ArrayList<String>();
        for (String ppName : postProcessorNames) {
            if (processedBeans.contains(ppName)) {
                // skip - already processed in first phase above
                //processedBeans包含该name，表示上面的流程已经执行过了该BeanFactoryPostProcessor
            } else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
                priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
            } else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
                orderedPostProcessorNames.add(ppName);
            } else {
                nonOrderedPostProcessorNames.add(ppName);
            }
        }

        // First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
        //首先调用实现了PriorityOrdered接口的BeanFactoryPostProcessor，排序后逐个调用
        sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
        invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

        // Next, invoke the BeanFactoryPostProcessors that implement Ordered.
        //再调用实现了Ordered接口的BeanFactoryPostProcessor，排序后逐个调用
        List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
        for (String postProcessorName : orderedPostProcessorNames) {
            orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
        }
        sortPostProcessors(orderedPostProcessors, beanFactory);
        invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

        // Finally, invoke all other BeanFactoryPostProcessors.
        //最后，调用所有其它的BeanFactoryPostProcessors（未实现PriorityOrdered和Ordered接口的）
        List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
        for (String postProcessorName : nonOrderedPostProcessorNames) {
            nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
        }
        invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

        // Clear cached merged bean definitions since the post-processors might have
        // modified the original metadata, e.g. replacing placeholders in values...
        beanFactory.clearMetadataCache();
    }


    /**
     * 循环调用所有BeanFactoryPostProcessor的postProcessBeanFactory方法
     */
    private static void invokeBeanFactoryPostProcessors(
            Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

        for (BeanFactoryPostProcessor postProcessor : postProcessors) {
            postProcessor.postProcessBeanFactory(beanFactory);
        }
    }
}


```