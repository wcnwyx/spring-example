#本文描述了基于&lt;context:component-scan&gt; 标签来进行扫描解析BeanDefinition的过程，删除掉了很多的细节部分，先对整体的流程有一个大概的认知，后续再细究。

##步骤一：找到标签对应的NameSpaceHandler和BeanDefinitionParser
代码展示从BeanDefinitionParserDelegate的parseCustomElement开始，可以连接上 上一篇基于xml的BeanDefinition解析。  
非默认namespace的标签，采用的是NamespaceHandler来进行处理，各个NamespaceHandler里注册这各种标签对应的BeanDefinitionParser解析类。  
```
public class BeanDefinitionParserDelegate

public BeanDefinition parseCustomElement(Element ele, BeanDefinition containingBd) {
    String namespaceUri = getNamespaceURI(ele);
    NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
    if (handler == null) {
        error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", ele);
        return null;
    }
    return handler.parse(ele, new ParserContext(this.readerContext, this, containingBd));
}
```

展示ContextNamespaceHandler中各种标签和BeanDefinitionParser的对应关系  
component-scan标签对应的就是ComponentScanBeanDefinitionParser解析器
```
public class ContextNamespaceHandler extends NamespaceHandlerSupport {
	@Override
	public void init() {
		registerBeanDefinitionParser("property-placeholder", new PropertyPlaceholderBeanDefinitionParser());
		registerBeanDefinitionParser("property-override", new PropertyOverrideBeanDefinitionParser());
		registerBeanDefinitionParser("annotation-config", new AnnotationConfigBeanDefinitionParser());
		registerBeanDefinitionParser("component-scan", new ComponentScanBeanDefinitionParser());
		registerBeanDefinitionParser("load-time-weaver", new LoadTimeWeaverBeanDefinitionParser());
		registerBeanDefinitionParser("spring-configured", new SpringConfiguredBeanDefinitionParser());
		registerBeanDefinitionParser("mbean-export", new MBeanExportBeanDefinitionParser());
		registerBeanDefinitionParser("mbean-server", new MBeanServerBeanDefinitionParser());
	}

}
```

##步骤二：根据component-scan标签属性来创建并配置ClassPathBeanDefinitionScanner
BeanDefinitionScanner是用来扫描base-package路径下的类并解析成BeanDefinition的

```
public class ComponentScanBeanDefinitionParser implements BeanDefinitionParser

public BeanDefinition parse(Element element, ParserContext parserContext) {
    //获取base-package属性
    String basePackage = element.getAttribute(BASE_PACKAGE_ATTRIBUTE);
    basePackage = parserContext.getReaderContext().getEnvironment().resolvePlaceholders(basePackage);
    //按照分隔符分割处理
    String[] basePackages = StringUtils.tokenizeToStringArray(basePackage,
            ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);

    //创建ClassPathBeanDefinitionScanner并进行配置
    //AnnotationConfigApplicationContext的初始化也是通过该扫描器来进行处理的
    ClassPathBeanDefinitionScanner scanner = configureScanner(parserContext, element);
    //根据配置的base-package包路径进行扫描
    Set<BeanDefinitionHolder> beanDefinitions = scanner.doScan(basePackages);
    //
    registerComponents(parserContext.getReaderContext(), beanDefinitions, element);
    return null;
}


protected ClassPathBeanDefinitionScanner configureScanner(ParserContext parserContext, Element element) {
    //处理use-default-filters配置属性，默认是true
    boolean useDefaultFilters = true;
    if (element.hasAttribute(USE_DEFAULT_FILTERS_ATTRIBUTE)) {
        useDefaultFilters = Boolean.valueOf(element.getAttribute(USE_DEFAULT_FILTERS_ATTRIBUTE));
    }

    // Delegate bean definition registration to scanner class.
    ClassPathBeanDefinitionScanner scanner = createScanner(parserContext.getReaderContext(), useDefaultFilters);
    
    //给扫描器设置一个默认的BeanDefinition对象，用于给解析的BeanDefinition赋值时，没有制定配置的情况下采用默认值
    scanner.setBeanDefinitionDefaults(parserContext.getDelegate().getBeanDefinitionDefaults());
    
    //解析default-autowire-candidates属性
    scanner.setAutowireCandidatePatterns(parserContext.getDelegate().getAutowireCandidatePatterns());

    if (element.hasAttribute(RESOURCE_PATTERN_ATTRIBUTE)) {
        scanner.setResourcePattern(element.getAttribute(RESOURCE_PATTERN_ATTRIBUTE));
    }

    //解析处理name-generator属性，设置beanName生成器。
    //默认使用的是AnnotationBeanNameGenerator，生成规则是首字母小写
    parseBeanNameGenerator(element, scanner);

    //解析处理scope-resolver和scoped-proxy属性
    //默认使用的是AnnotationScopeMetadataResolver
    parseScope(element, scanner);

    //解析include-filter和exclude-filter属性
    parseTypeFilters(element, scanner, parserContext);

    return scanner;
}

protected ClassPathBeanDefinitionScanner createScanner(XmlReaderContext readerContext, boolean useDefaultFilters) {
    //new 一个ClassPathBeanDefinitionScanner扫描器使用
    return new ClassPathBeanDefinitionScanner(readerContext.getRegistry(), useDefaultFilters,
            readerContext.getEnvironment(), readerContext.getResourceLoader());
}
```

##步骤三：执行扫描操作，将根据component-scan配置的扫描规则以及class的注解信息，将符合条件的class解析成BeanDefinition并注册
```
public class ClassPathBeanDefinitionScanner extends ClassPathScanningCandidateComponentProvider

protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
    Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<BeanDefinitionHolder>();
    //循环所有的文件目录扫描解析
    for (String basePackage : basePackages) {
        //扫描目录下的候选组件，既带有@Component、@Service等注解的类，解析成BeanDefinition返回
        Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
        for (BeanDefinition candidate : candidates) {
            //设置scope属性
            ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
            candidate.setScope(scopeMetadata.getScopeName());
            
            //生成beanName
            String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
            
            if (candidate instanceof AbstractBeanDefinition) {
                //根据BeanDefinitionDefault设置属性默认值
                //设置BeanDefinition的autowireCandidate属性
                postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);
            }
            
            if (candidate instanceof AnnotatedBeanDefinition) {
                //解析一些通用的注解并赋值给BeanDefinition，有@Lazy、@Primary、@DependsOn、@Role、@Description
                AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
            }
            
            //检测beanName是否已被注册
            if (checkCandidate(beanName, candidate)) {
                BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
                definitionHolder =
                        AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
                beanDefinitions.add(definitionHolder);
                
                //实际注册的逻辑和上一篇讲的基于xml的BeanDefinition解析注册中的注册逻辑就是一样的了
                registerBeanDefinition(definitionHolder, this.registry);
            }
        }
    }
    return beanDefinitions;
}
```

```
该类为ClassPathBeanDefinitionScanner的父类
public class ClassPathScanningCandidateComponentProvider implements EnvironmentCapable, ResourceLoaderAware

//扫描basePackage目录下的候选组件
public Set<BeanDefinition> findCandidateComponents(String basePackage) {
    Set<BeanDefinition> candidates = new LinkedHashSet<BeanDefinition>();
    try {
        //拼接完整的路径
        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                resolveBasePackage(basePackage) + '/' + this.resourcePattern;
        //将路径下的类解析成Resouce对象
        Resource[] resources = this.resourcePatternResolver.getResources(packageSearchPath);
        
        for (Resource resource : resources) {
            if (resource.isReadable()) {
                try {
                    //获取元数据读取器，用来读取类的元数据和注解的元数据信息
                    MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(resource);
                    //判断是否是候选组件类，根据include-filter和exclude-filter属性以及@Conditional来判断是否符合
                    if (isCandidateComponent(metadataReader)) {
                        //这里可以看出，通过扫描出来的BeanDefinition使用的都是ScannedGenericBeanDefinition这个子类
                        ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
                        sbd.setResource(resource);
                        sbd.setSource(resource);
                        
                        //再次判断是否可以成为候选组件，判断是否为抽象类、接口以及LoopUp注解判断
                        if (isCandidateComponent(sbd)) {
                            candidates.add(sbd);
                        }
                    }
                }
            }
        }
    }
    return candidates;
}
```