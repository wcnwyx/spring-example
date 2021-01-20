本文描述了bean定义初始化的精简流程，删除掉了部分的细节部分，先对整体的流程有一个大概的认知，后续再细究。    
本文以xml配置的文件梳理流程，就是用ClassPathXmlApplicationContext和FileSystemXmlApplicationContext传入xml文件路径的初始化方法。  
整体思路分为了三大步骤：xml文件的读取转换、解析xml标签属性封装成一个BeanDefinition对象、将BeanDefinition注册到Registry中。  

##步骤一：xml文件的读取、转换
首先还是从AbstractApplicationContext的refresh方法开始展示调用链，整体步骤梳理下来就是以下2小步：  
1.1：将每一个xml路径解析成Resource对象。在AbstractBeanDefinitionReader中，通过ResourceLoader加载xml文件转换成Resource对象。  
1.2：将Resource对象再解析成Document文档对象以备后续使用。通过XmlBeanDefinitionReader先将Resource对象解析成InputSource，再解析成Document对象。

###步骤1.1：将每一个xml路径解析成Resource对象
```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader
        implements ConfigurableApplicationContext, DisposableBean {
    @Override
    public void refresh() throws BeansException, IllegalStateException {
        synchronized (this.startupShutdownMonitor) {

            //通知子类刷新内部的BeanFactory并返回，通过参数可以看出内部用的是ConfigurableListableBeanFactory
            ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

        }
    }

    //通知子类刷新内部的beanFactory
    protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
        //该方法为抽象方法，各个子类实现自己的逻辑
        refreshBeanFactory();

        //该方法为抽象方法，各个子类实现自己的逻辑
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        if (logger.isDebugEnabled()) {
            logger.debug("Bean factory for " + getDisplayName() + ": " + beanFactory);
        }
        return beanFactory;
    }
}
```

```java
public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {
    @Override
    protected final void refreshBeanFactory() throws BeansException {
        //如果已有beanFactory，进行销毁关闭
        if (hasBeanFactory()) {
            destroyBeans();
            closeBeanFactory();
        }
        try {
            //创建一个新的BeanFactory，类型为 DefaultListableBeanFactory
            DefaultListableBeanFactory beanFactory = createBeanFactory();
            beanFactory.setSerializationId(getId());
            customizeBeanFactory(beanFactory);

            //加载BeanDefinitions
            loadBeanDefinitions(beanFactory);

            synchronized (this.beanFactoryMonitor) {
                this.beanFactory = beanFactory;
            }
        }
        catch (IOException ex) {
            throw new ApplicationContextException("I/O error parsing bean definition source for " + getDisplayName(), ex);
        }
    }

    @Override
    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
        // 创建一个BeanDefinitionReader来读取解析BeanDefinition
        XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

        //使用BeanDefinitionReader来加载BeanDefinition
        loadBeanDefinitions(beanDefinitionReader);
    }

    protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
        Resource[] configResources = getConfigResources();
        if (configResources != null) {
            //根据所有的Resource来加载BeanDefinition
            //常用的 ClassPathXmlApplicationContext和FileSystemXmlApplicationContext的getConfigResources()方法返回的都是空，
            //都是要根据路径去把配置文件解析成Resource对象
            reader.loadBeanDefinitions(configResources);
        }
        String[] configLocations = getConfigLocations();
        if (configLocations != null) {
            //根据配置的路径来加载BeanDefinition
            reader.loadBeanDefinitions(configLocations);
        }
    }
}
```

```java
public abstract class AbstractBeanDefinitionReader implements EnvironmentCapable, BeanDefinitionReader {
    @Override
    public int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException {
        Assert.notNull(locations, "Location array must not be null");
        int counter = 0;
        for (String location : locations) {
            //循环调用每个资源路径进行处理
            counter += loadBeanDefinitions(location);
        }
        return counter;
    }


    @Override
    public int loadBeanDefinitions(String location) throws BeanDefinitionStoreException {
        return loadBeanDefinitions(location, null);
    }


    public int loadBeanDefinitions(String location, Set<Resource> actualResources) throws BeanDefinitionStoreException {
        ResourceLoader resourceLoader = getResourceLoader();

        if (resourceLoader instanceof ResourcePatternResolver) {
            // Resource pattern matching available.
            try {
                Resource[] resources = ((ResourcePatternResolver) resourceLoader).getResources(location);
                //根据资源路径解析成Resource后继续调用
                int loadCount = loadBeanDefinitions(resources);
                return loadCount;
            }
        }else {
            // Can only load single resources by absolute URL.
            Resource resource = resourceLoader.getResource(location);
            //根据资源路径解析成Resource后继续调用
            int loadCount = loadBeanDefinitions(resource);
            return loadCount;
        }
    }

    @Override
    public int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException {
        Assert.notNull(resources, "Resource array must not be null");
        int counter = 0;
        for (Resource resource : resources) {
            //循环处理每一个Resource对象
            counter += loadBeanDefinitions(resource);
        }
        return counter;
    }
}
```

###步骤1.2：将Resource对象再解析成Document文档对象以备后续使用
```java
public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {
    @Override
    public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
        return loadBeanDefinitions(new EncodedResource(resource));
    }


    public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
        try {
            InputStream inputStream = encodedResource.getResource().getInputStream();
            try {
                InputSource inputSource = new InputSource(inputStream);
                if (encodedResource.getEncoding() != null) {
                    inputSource.setEncoding(encodedResource.getEncoding());
                }
                //转换成InputSource继续加载
                return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
            }
            finally {
                inputStream.close();
            }
        }
    }


    protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource)
            throws BeanDefinitionStoreException {
        try {
            //将InputSource转换成Document再进行BeanDefinition的解析和注册
            Document doc = doLoadDocument(inputSource, resource);
            return registerBeanDefinitions(doc, resource);
        }
    }

    public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStoreException {
        //创建一个BeanDefinitionDocumentReader接口对象继续解析注册BeanDefinition，实际的类为：DefaultBeanDefinitionDocumentReader
        BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
        int countBefore = getRegistry().getBeanDefinitionCount();
        documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
        return getRegistry().getBeanDefinitionCount() - countBefore;
    }
}
```


##步骤二：解析xml标签属性封装成一个BeanDefinition对象
xml document 的处理分了两种处理方式，默认namespace（即http://www.springframework.org/schema/beans）的和非默认的。  
默认namespace的通过DefaultBeanDefinitionDocumentReader解析&lt;import&gt;、&lt;alias&gt;、&lt;beans&gt;标签，通过BeanDefinitionParserDelegate解析&lt;bean&gt;标签。  
非默认的是通过调用各个NamespaceHandler的parse方法进行解析。  
分为3小步来进行分析：  
2.1：处理profile逻辑。  
2.2：DefaultBeanDefinitionDocumentReader解析默认namespace的&lt;import&gt;、&lt;alias&gt;、&lt;beans&gt;标签。  
2.3：BeanDefinitionParserDelegate解析默认namespace的&lt;bean&gt;标签。  


###步骤2.1：处理profile逻辑
```java
public class DefaultBeanDefinitionDocumentReader implements BeanDefinitionDocumentReader {
    @Override
    public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
        this.readerContext = readerContext;
        logger.debug("Loading bean definitions");
        Element root = doc.getDocumentElement();
        //读取Document的element继续执行加载注册BeanDefinition
        doRegisterBeanDefinitions(root);
    }

    protected void doRegisterBeanDefinitions(Element root) {
        //创建一个BeanDefinition的解析器代理
        BeanDefinitionParserDelegate parent = this.delegate;
        this.delegate = createDelegate(getReaderContext(), root, parent);

        //处理profile逻辑，不是当前的profile就返回了
        if (this.delegate.isDefaultNamespace(root)) {
            String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
            if (StringUtils.hasText(profileSpec)) {
                String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
                        profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
                if (!getReaderContext().getEnvironment().acceptsProfiles(specifiedProfiles)) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Skipped XML bean definition file due to specified profiles [" + profileSpec +
                                "] not matching: " + getReaderContext().getResource());
                    }
                    return;
                }
            }
        }

        //解析BeanDefinition
        parseBeanDefinitions(root, this.delegate);

        this.delegate = parent;
    }
}
```

###步骤2.2：DefaultBeanDefinitionDocumentReader解析默认namespace的&lt;import&gt;、&lt;alias&gt;、&lt;beans&gt;标签
```java
public class DefaultBeanDefinitionDocumentReader implements BeanDefinitionDocumentReader {
    protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
        //判断是否默认的Namespace,既（http://www.springframework.org/schema/beans）
        if (delegate.isDefaultNamespace(root)) {
            NodeList nl = root.getChildNodes();
            //循环每一个Node进行处理
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                if (node instanceof Element) {
                    Element ele = (Element) node;
                    if (delegate.isDefaultNamespace(ele)) {
                        //如果是默认的执行该方法
                        parseDefaultElement(ele, delegate);
                    }
                    else {
                        //非默认的调用代理的parseCustomElement方法解析处理，最后调用的是NamespaceHandler来处理，先不看这个逻辑
                        delegate.parseCustomElement(ele);
                    }
                }
            }
        }
        else {
            //非默认的调用代理的parseCustomElement方法解析处理，最后调用的是NamespaceHandler来处理，后续篇章里再介绍
            delegate.parseCustomElement(root);
        }
    }

    private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
        if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
            //处理<import>标签逻辑，就是解析出来import的资源路径，再次调用一个加载一个加载
            importBeanDefinitionResource(ele);
        }
        else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
            //处理<alias>标签逻辑，解析好别名注册到BeanDefinitionRegistry中 
            processAliasRegistration(ele);
        }
        else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
            //处理<bean>标签
            processBeanDefinition(ele, delegate);
        }
        else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
            //处理<beans>标签，从新递归调用doRegisterBeanDefinitions方法
            doRegisterBeanDefinitions(ele);
        }
    }

    protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
        //BeanDefinitionHolder是把BeanDefinition进一步封装，封装了beanName属性和aliases属性
        BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
        if (bdHolder != null) {
            bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
            try {
                // 将BeanDefinitionHolder注册到BeanDefinitionRegistry中
                BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
            }
            catch (BeanDefinitionStoreException ex) {
                getReaderContext().error("Failed to register bean definition with name '" +
                        bdHolder.getBeanName() + "'", ele, ex);
            }
            // 发送注册事件
            getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
        }
    }
}

```

###步骤2.3：BeanDefinitionParserDelegate解析默认namespace的&lt;bean&gt;标签
```java
public class BeanDefinitionParserDelegate {
    public BeanDefinitionHolder parseBeanDefinitionElement(Element ele) {
        return parseBeanDefinitionElement(ele, null);
    }

    public BeanDefinitionHolder parseBeanDefinitionElement(Element ele, BeanDefinition containingBean) {
        //解析id属性
        String id = ele.getAttribute(ID_ATTRIBUTE);
        //解析name属性
        String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);

        //多个name的话处理aliases的逻辑
        List<String> aliases = new ArrayList<String>();
        if (StringUtils.hasLength(nameAttr)) {
            String[] nameArr = StringUtils.tokenizeToStringArray(nameAttr, MULTI_VALUE_ATTRIBUTE_DELIMITERS);
            aliases.addAll(Arrays.asList(nameArr));
        }

        String beanName = id;
        if (!StringUtils.hasText(beanName) && !aliases.isEmpty()) {
            beanName = aliases.remove(0);
        }

        if (containingBean == null) {
            //校验beanName和aliases是否唯一，不唯一的话抛出异常结束
            checkNameUniqueness(beanName, aliases, ele);
        }

        //进一步解析<bean>标签的其它属性（class、parent、scope、abstract、lazy-init、autowire、depends-on、
        //autowire-candidate、primary、init-method、destroy-method、factory-method、factory-bean）
        //进一步解析<bean>子标签<description>、<meta>、<lookup-method>、<replaced-method>、<constructor-arg>、<property>、<qualifier>
        //再详细的解析就不列了，都是解析文档属性，然后赋值给BeanDefinition对象
        AbstractBeanDefinition beanDefinition = parseBeanDefinitionElement(ele, beanName, containingBean);
        if (beanDefinition != null) {
            if (!StringUtils.hasText(beanName)) {
                //如果没有beanName,根据相关逻辑创建一个beanName
            }
            String[] aliasesArray = StringUtils.toStringArray(aliases);

            //这里就可以看出BeanDefinitionHolder封装了什么东西了
            return new BeanDefinitionHolder(beanDefinition, beanName, aliasesArray);
        }

        return null;
    }

    //如果还有非默认Namespace的标签，再次调用NamespaceHandler进行装饰处理
    public BeanDefinitionHolder decorateBeanDefinitionIfRequired(Element ele, BeanDefinitionHolder originalDef) {
        return decorateBeanDefinitionIfRequired(ele, originalDef, null);
    }

    public BeanDefinitionHolder decorateBeanDefinitionIfRequired(
            Element ele, BeanDefinitionHolder originalDef, BeanDefinition containingBd) {

        BeanDefinitionHolder finalDefinition = originalDef;

        // Decorate based on custom attributes first.
        NamedNodeMap attributes = ele.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node node = attributes.item(i);
            finalDefinition = decorateIfRequired(node, finalDefinition, containingBd);
        }

        // Decorate based on custom nested elements.
        NodeList children = ele.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                finalDefinition = decorateIfRequired(node, finalDefinition, containingBd);
            }
        }
        return finalDefinition;
    }
}

```


##步骤三：将解析好的BeanDefinition注册到Registry中  
实际的注册逻辑在DefaultListableBeanFactory类中，存在在一个Map数据中  

```java
public class BeanDefinitionReaderUtils {
    public static void registerBeanDefinition(
            BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry)
            throws BeanDefinitionStoreException {

        //注册BeanDefinition到BeanDefinitionRegistry，最终放到beanDefinitionMap中保存
        String beanName = definitionHolder.getBeanName();
        registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());

        // Register aliases for bean name, if any.
        String[] aliases = definitionHolder.getAliases();
        if (aliases != null) {
            for (String alias : aliases) {
                //注册alias
                registry.registerAlias(beanName, alias);
            }
        }
    }
}

```

```java
//DefaultListableBeanFactory是实现了BeanDefinitionRegistry接口的，所以提供了注册的方法
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
        implements ConfigurableListableBeanFactory, BeanDefinitionRegistry, Serializable {
    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
            throws BeanDefinitionStoreException {

        if (beanDefinition instanceof AbstractBeanDefinition) {
            try {
                //BeanDefinition进行校验
                ((AbstractBeanDefinition) beanDefinition).validate();
            }
            catch (BeanDefinitionValidationException ex) {
                throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
                        "Validation of bean definition failed", ex);
            }
        }

        BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);
        if (existingDefinition != null) {
            //如果说这个beanName已经有BeanDefinition，而且不允许覆盖，直接抛出异常
            if (!isAllowBeanDefinitionOverriding()) {
                throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
                        "Cannot register bean definition [" + beanDefinition + "] for bean '" + beanName +
                                "': There is already [" + existingDefinition + "] bound.");
            }
            else if (existingDefinition.getRole() < beanDefinition.getRole()) {
                //如果后注册的BeanDefinition等级比已有的低（比如说后注册的是用户角色，已有的是系统级别的，就会打印警告日志）
                if (logger.isWarnEnabled()) {
                    logger.warn("Overriding user-defined bean definition for bean '" + beanName +
                            "' with a framework-generated bean definition: replacing [" +
                            existingDefinition + "] with [" + beanDefinition + "]");
                }
            }
            else if (!beanDefinition.equals(existingDefinition)) {
                //新注册的BeanDefinition和已有的不相等
                if (logger.isInfoEnabled()) {
                    logger.info("Overriding bean definition for bean '" + beanName +
                            "' with a different definition: replacing [" + existingDefinition +
                            "] with [" + beanDefinition + "]");
                }
            }
            else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Overriding bean definition for bean '" + beanName +
                            "' with an equivalent definition: replacing [" + existingDefinition +
                            "] with [" + beanDefinition + "]");
                }
            }
            //直接覆盖老的BeanDefinition
            this.beanDefinitionMap.put(beanName, beanDefinition);
        }
        else {
            if (hasBeanCreationStarted()) {
                // Cannot modify startup-time collection elements anymore (for stable iteration)
                //当前已经不是启动注册阶段了，有的bean已经被创建实例化了，进行特殊的处理
                synchronized (this.beanDefinitionMap) {
                    this.beanDefinitionMap.put(beanName, beanDefinition);
                    List<String> updatedDefinitions = new ArrayList<String>(this.beanDefinitionNames.size() + 1);
                    updatedDefinitions.addAll(this.beanDefinitionNames);
                    updatedDefinitions.add(beanName);
                    this.beanDefinitionNames = updatedDefinitions;
                    if (this.manualSingletonNames.contains(beanName)) {
                        Set<String> updatedSingletons = new LinkedHashSet<String>(this.manualSingletonNames);
                        updatedSingletons.remove(beanName);
                        this.manualSingletonNames = updatedSingletons;
                    }
                }
            }
            else {
                // Still in startup registration phase
                //仍处于启动注册阶段，直接处理
                this.beanDefinitionMap.put(beanName, beanDefinition);
                this.beanDefinitionNames.add(beanName);
                this.manualSingletonNames.remove(beanName);
            }
            this.frozenBeanDefinitionNames = null;
        }

        if (existingDefinition != null || containsSingleton(beanName)) {
            //已有该beanName，把相关缓存数据进行重设处理
            resetBeanDefinition(beanName);
        }
    }
}
```