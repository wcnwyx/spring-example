基于 AnnotationConfigApplicationContext 的beanDefinition是通过该postProcessor进行处理的。  
```java
public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor,
        PriorityOrdered, ResourceLoaderAware, BeanClassLoaderAware, EnvironmentAware {

    /**
     * Derive further bean definitions from the configuration classes in the registry.
     * 从注册表registry中的configuration类中派生出BeanDefinition
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        int registryId = System.identityHashCode(registry);
        if (this.registriesPostProcessed.contains(registryId)) {
            throw new IllegalStateException(
                    "postProcessBeanDefinitionRegistry already called on this post-processor against " + registry);
        }
        if (this.factoriesPostProcessed.contains(registryId)) {
            throw new IllegalStateException(
                    "postProcessBeanFactory already called on this post-processor against " + registry);
        }
        this.registriesPostProcessed.add(registryId);

        processConfigBeanDefinitions(registry);
    }

    /**
     * Build and validate a configuration model based on the registry of
     * {@link Configuration} classes.
     * 基于注册表registry中的Configuration类 来构建和验证一个configuration模型
     * 在new AnnotationConfigApplicationContext的时候，通过构造函数的参数传入annotationClass，首先会调用register方法解析并注册该annotationClass
     */
    public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
        //校验通过是Configuration的类存在该集合中
        List<BeanDefinitionHolder> configCandidates = new ArrayList<BeanDefinitionHolder>();
        
        //获取目前已有的所有BeanDefinition 的name
        String[] candidateNames = registry.getBeanDefinitionNames();

        for (String beanName : candidateNames) {
            BeanDefinition beanDef = registry.getBeanDefinition(beanName);
            if (ConfigurationClassUtils.isFullConfigurationClass(beanDef) ||
                    ConfigurationClassUtils.isLiteConfigurationClass(beanDef)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Bean definition has already been processed as a configuration class: " + beanDef);
                }
            }
            else if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, this.metadataReaderFactory)) {
                //判断是否是一个Configuration类，如果的话还会加上一个order属性
                //判断逻辑1：isFullConfigurationCandidate，是否有@Configuration注解
                //判断逻辑2：isLiteConfigurationCandidate，是否有@Import、@ImportSource、@ComponentScan、@Component或者是有@bean注解的方法
                configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));
            }
        }

        //没有Configuration就返回了
        if (configCandidates.isEmpty()) {
            return;
        }

        //按照order排序
        Collections.sort(configCandidates, new Comparator<BeanDefinitionHolder>() {
            @Override
            public int compare(BeanDefinitionHolder bd1, BeanDefinitionHolder bd2) {
                int i1 = ConfigurationClassUtils.getOrder(bd1.getBeanDefinition());
                int i2 = ConfigurationClassUtils.getOrder(bd2.getBeanDefinition());
                return (i1 < i2) ? -1 : (i1 > i2) ? 1 : 0;
            }
        });

        //设置BeanNameGenerator用于生成beanName
        SingletonBeanRegistry sbr = null;
        if (registry instanceof SingletonBeanRegistry) {
            sbr = (SingletonBeanRegistry) registry;
            if (!this.localBeanNameGeneratorSet && sbr.containsSingleton(CONFIGURATION_BEAN_NAME_GENERATOR)) {
                BeanNameGenerator generator = (BeanNameGenerator) sbr.getSingleton(CONFIGURATION_BEAN_NAME_GENERATOR);
                this.componentScanBeanNameGenerator = generator;
                this.importBeanNameGenerator = generator;
            }
        }

        //创建一个Configuration类的解析器，所有的解析逻辑都在此类中
        ConfigurationClassParser parser = new ConfigurationClassParser(
                this.metadataReaderFactory, this.problemReporter, this.environment,
                this.resourceLoader, this.componentScanBeanNameGenerator, registry);

        Set<BeanDefinitionHolder> candidates = new LinkedHashSet<BeanDefinitionHolder>(configCandidates);
        Set<ConfigurationClass> alreadyParsed = new HashSet<ConfigurationClass>(configCandidates.size());
        do {
            //解析所有的Configuration类
            parser.parse(candidates);
            parser.validate();

            //本轮解析成功的ConfigurationClass
            Set<ConfigurationClass> configClasses = new LinkedHashSet<ConfigurationClass>(parser.getConfigurationClasses());
            //将已经解析过的移除掉
            configClasses.removeAll(alreadyParsed);

            // Read the model and create bean definitions based on its content
            if (this.reader == null) {
                this.reader = new ConfigurationClassBeanDefinitionReader(
                        registry, this.sourceExtractor, this.resourceLoader, this.environment,
                        this.importBeanNameGenerator, parser.getImportRegistry());
            }
            this.reader.loadBeanDefinitions(configClasses);
            alreadyParsed.addAll(configClasses);

            candidates.clear();
            
            //registry中注册的BeanDefinition比方法刚开始时获取的数量多，说明上面的操作比如说@Import、@ComponentScan又注册进来了新的BeanDefinition
            if (registry.getBeanDefinitionCount() > candidateNames.length) {
                //当前注册表registry中有的所有BeanDefinition名字集合
                String[] newCandidateNames = registry.getBeanDefinitionNames();
                //方法一开始就从注册表中获取到的BeanDefinition的名字集合备份
                Set<String> oldCandidateNames = new HashSet<String>(Arrays.asList(candidateNames));
                //本次Configuration类的解析器解析出来的class名称
                Set<String> alreadyParsedClasses = new HashSet<String>();
                for (ConfigurationClass configurationClass : alreadyParsed) {
                    alreadyParsedClasses.add(configurationClass.getMetadata().getClassName());
                }
                for (String candidateName : newCandidateNames) {
                    if (!oldCandidateNames.contains(candidateName)) {
                        BeanDefinition bd = registry.getBeanDefinition(candidateName);
                        if (ConfigurationClassUtils.checkConfigurationClassCandidate(bd, this.metadataReaderFactory) &&
                                !alreadyParsedClasses.contains(bd.getBeanClassName())) {
                            //该beanName在目前注册表中有，并且备份的时候里面没有，本轮解析的结果里也没有，并且也是一个Configuration类，加到集合中重新走do-while循环处理
                            candidates.add(new BeanDefinitionHolder(bd, candidateName));
                        }
                    }
                }
                candidateNames = newCandidateNames;
            }
        }
        while (!candidates.isEmpty());

        // Register the ImportRegistry as a bean in order to support ImportAware @Configuration classes
        //注册一个ImportRegistry
        if (sbr != null) {
            if (!sbr.containsSingleton(IMPORT_REGISTRY_BEAN_NAME)) {
                sbr.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.getImportRegistry());
            }
        }

        if (this.metadataReaderFactory instanceof CachingMetadataReaderFactory) {
            ((CachingMetadataReaderFactory) this.metadataReaderFactory).clearCache();
        }
    }
}
    
```

```java
class ConfigurationClassParser {
    
    //解析Configuration类
    public void parse(Set<BeanDefinitionHolder> configCandidates) {
        this.deferredImportSelectors = new LinkedList<DeferredImportSelectorHolder>();

        for (BeanDefinitionHolder holder : configCandidates) {
            BeanDefinition bd = holder.getBeanDefinition();
            try {
                if (bd instanceof AnnotatedBeanDefinition) {
                    parse(((AnnotatedBeanDefinition) bd).getMetadata(), holder.getBeanName());
                }
                else if (bd instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) bd).hasBeanClass()) {
                    parse(((AbstractBeanDefinition) bd).getBeanClass(), holder.getBeanName());
                }
                else {
                    parse(bd.getBeanClassName(), holder.getBeanName());
                }
            }
            catch (BeanDefinitionStoreException ex) {
                throw ex;
            }
            catch (Throwable ex) {
                throw new BeanDefinitionStoreException(
                        "Failed to parse configuration class [" + bd.getBeanClassName() + "]", ex);
            }
        }

        processDeferredImportSelectors();
    }

    protected final void parse(AnnotationMetadata metadata, String beanName) throws IOException {
        processConfigurationClass(new ConfigurationClass(metadata, beanName));
    }

    protected void processConfigurationClass(ConfigurationClass configClass) throws IOException {
        //处理@Condition注解条件，是否满足条件
        if (this.conditionEvaluator.shouldSkip(configClass.getMetadata(), ConfigurationPhase.PARSE_CONFIGURATION)) {
            return;
        }

        ConfigurationClass existingClass = this.configurationClasses.get(configClass);
        
        //该ConfigurationClass已经被处理过了
        if (existingClass != null) {
            if (configClass.isImported()) {
                if (existingClass.isImported()) {
                    existingClass.mergeImportedBy(configClass);
                }
                // Otherwise ignore new imported config class; existing non-imported class overrides it.
                return;
            }
            else {
                // Explicit bean definition found, probably replacing an import.
                // Let's remove the old one and go with the new one.
                this.configurationClasses.remove(configClass);
                for (Iterator<ConfigurationClass> it = this.knownSuperclasses.values().iterator(); it.hasNext();) {
                    if (configClass.equals(it.next())) {
                        it.remove();
                    }
                }
            }
        }

        // Recursively process the configuration class and its superclass hierarchy.
        SourceClass sourceClass = asSourceClass(configClass);
        do {
            sourceClass = doProcessConfigurationClass(configClass, sourceClass);
        }
        while (sourceClass != null);

        this.configurationClasses.put(configClass, configClass);
    }


    /**
     * Apply processing and build a complete {@link ConfigurationClass} by reading the
     * annotations, members and methods from the source class. This method can be called
     * multiple times as relevant sources are discovered.
     * @param configClass the configuration class being build
     * @param sourceClass a source class
     * @return the superclass, or {@code null} if none found or previously processed
     */
    protected final SourceClass doProcessConfigurationClass(ConfigurationClass configClass, SourceClass sourceClass)
            throws IOException {

        // Recursively process any member (nested) classes first
        processMemberClasses(configClass, sourceClass);

        //处理@PropertySource注解逻辑
        for (AnnotationAttributes propertySource : AnnotationConfigUtils.attributesForRepeatable(
                sourceClass.getMetadata(), PropertySources.class,
                org.springframework.context.annotation.PropertySource.class)) {
            if (this.environment instanceof ConfigurableEnvironment) {
                processPropertySource(propertySource);
            }
            else {
                logger.warn("Ignoring @PropertySource annotation on [" + sourceClass.getMetadata().getClassName() +
                        "]. Reason: Environment must implement ConfigurableEnvironment");
            }
        }

        //处理@ComponentScan注解逻辑
        Set<AnnotationAttributes> componentScans = AnnotationConfigUtils.attributesForRepeatable(
                sourceClass.getMetadata(), ComponentScans.class, ComponentScan.class);
        if (!componentScans.isEmpty() &&
                !this.conditionEvaluator.shouldSkip(sourceClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN)) {
            for (AnnotationAttributes componentScan : componentScans) {
                // The config class is annotated with @ComponentScan -> perform the scan immediately
                Set<BeanDefinitionHolder> scannedBeanDefinitions =
                        this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());
                // Check the set of scanned definitions for any further config classes and parse recursively if needed
                for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
                    BeanDefinition bdCand = holder.getBeanDefinition().getOriginatingBeanDefinition();
                    if (bdCand == null) {
                        bdCand = holder.getBeanDefinition();
                    }
                    if (ConfigurationClassUtils.checkConfigurationClassCandidate(bdCand, this.metadataReaderFactory)) {
                        parse(bdCand.getBeanClassName(), holder.getBeanName());
                    }
                }
            }
        }

        //处理@Import注解逻辑
        processImports(configClass, sourceClass, getImports(sourceClass), true);

        //处理@ImportResource注解逻辑
        if (sourceClass.getMetadata().isAnnotated(ImportResource.class.getName())) {
            AnnotationAttributes importResource =
                    AnnotationConfigUtils.attributesFor(sourceClass.getMetadata(), ImportResource.class);
            String[] resources = importResource.getStringArray("locations");
            Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");
            for (String resource : resources) {
                String resolvedResource = this.environment.resolveRequiredPlaceholders(resource);
                configClass.addImportedResource(resolvedResource, readerClass);
            }
        }

        //处理@Bean注解标注的method逻辑
        Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(sourceClass);
        for (MethodMetadata methodMetadata : beanMethods) {
            configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
        }

        // Process default methods on interfaces
        processInterfaces(configClass, sourceClass);

        //处理父类的逻辑
        if (sourceClass.getMetadata().hasSuperClass()) {
            String superclass = sourceClass.getMetadata().getSuperClassName();
            if (!superclass.startsWith("java") && !this.knownSuperclasses.containsKey(superclass)) {
                this.knownSuperclasses.put(superclass, configClass);
                // Superclass found, return its annotation metadata and recurse
                return sourceClass.getSuperClass();
            }
        }

        // No superclass -> processing is complete
        return null;
    }
}
```