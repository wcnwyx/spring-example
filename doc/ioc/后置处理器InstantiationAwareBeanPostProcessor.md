InstantiationAwareBeanPostProcessor后置处理器继承于BeanPostProcessor，主要是在bean实例化前后调用其接口方法。  

```java
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {
    
    //创建bean实例之前调用
    //在createBean方法中调用，调用位置在doCreateBean()之前
	Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException;

	//在bean实例化之后，populateBean之前
    //在populateBean方法的最开始位置调用，Autowire和applyPropertyValues之前
	boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException;

	//在populateBean方法中调用，Autowire之后，applyPropertyValues之前
	PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException;

}
```

下面看下createBean方法和populateBean方法中调用该接口的位置  

```java
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory
		implements AutowireCapableBeanFactory {

    protected Object createBean(String beanName, RootBeanDefinition mbd, Object[] args) throws BeanCreationException {
        RootBeanDefinition mbdToUse = mbd;


        // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
        //给BeanPostProcessors一个机会去创建并返回一个代理对象（AbstractAutoProxyCreator就是在此方法中创建的代理对象）
        //该方法中调用的是InstantiationAwareBeanPostProcessor的postProcessBeforeInstantiation方法
        Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
        if (bean != null) {
            return bean;
        }

        Object beanInstance = doCreateBean(beanName, mbdToUse, args);
        return beanInstance;
    }


    protected void populateBean(String beanName, RootBeanDefinition mbd, BeanWrapper bw) {
        PropertyValues pvs = mbd.getPropertyValues();

        boolean continueWithPropertyPopulation = true;

        if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                if (bp instanceof InstantiationAwareBeanPostProcessor) {
                    InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                    if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
                        //调用InstantiationAwareBeanPostProcessor的postProcessAfterInstantiation方法
                        continueWithPropertyPopulation = false;
                        break;
                    }
                }
            }
        }

        if (!continueWithPropertyPopulation) {
            return;
        }


        // Add property values based on autowire by name if applicable.
        if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME) {
            autowireByName(beanName, mbd, bw, newPvs);
        }

        // Add property values based on autowire by type if applicable.
        if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
            autowireByType(beanName, mbd, bw, newPvs);
        }


        boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
        boolean needsDepCheck = (mbd.getDependencyCheck() != RootBeanDefinition.DEPENDENCY_CHECK_NONE);

        if (hasInstAwareBpps || needsDepCheck) {
            PropertyDescriptor[] filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
            if (hasInstAwareBpps) {
                for (BeanPostProcessor bp : getBeanPostProcessors()) {
                    if (bp instanceof InstantiationAwareBeanPostProcessor) {
                        InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                        //调用InstantiationAwareBeanPostProcessor的postProcessPropertyValues方法
                        pvs = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
                        if (pvs == null) {
                            return;
                        }
                    }
                }
            }
        }

        applyPropertyValues(beanName, mbd, bw, pvs);
    }
}
```