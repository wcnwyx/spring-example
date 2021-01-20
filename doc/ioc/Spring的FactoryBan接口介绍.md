spring的FactoryBean相当于给我们一个接口，让我们自己控制bean的创建。一般用于创建一些创建过程比较复杂的类。  
先看一下FactoryBean这个接口提供的三个方法：
```
public interface FactoryBean<T> {

    //获取一个bean实例
    T getObject() throws Exception;

    //获取创建的bean类型
    Class<?> getObjectType();

    //创建的bean是否是singleton类型
	boolean isSingleton();

}
```

比如说我们定义一个DemoFactoryBean，用于创建Demo类，scope为singleton，代码如下
```
//实现FactoryBean接口
public class DemoFactoryBean implements FactoryBean<Demo> {
    @Override
    public Demo getObject() throws Exception {
        return new Demo();
    }

    @Override
    public Class<?> getObjectType() {
        return Demo.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}

//通过FactoryBean来创建的类
public class Demo {

}

//配置类
@Configuration
public class Config {

    @Bean
    public DemoFactoryBean demoFactoryBean(){
        return new DemoFactoryBean();
    }
}
```

测试代码如下：
```
public static void main(String[] args) {
    //根据Config类来初始化ApplicationContext
    ApplicationContext applicationContext = new AnnotationConfigApplicationContext(Config.class);

    //通过demoFactoryBean这个beanName获取到的是Demo这个类
    Demo demo1 = applicationContext.getBean("demoFactoryBean", Demo.class);
    System.out.println(demo1);

    //再次获取用于判断isSingleton()方法的作用
    Demo demo2 = applicationContext.getBean("demoFactoryBean", Demo.class);
    System.out.println(demo2);

    //通过加前缀&来获取DemoFactoryBean本身
    DemoFactoryBean demoFactoryBean = applicationContext.getBean("&demoFactoryBean", DemoFactoryBean.class);
    System.out.println(demoFactoryBean);
    
    //如果说通过demoFactoryBean再次手动创建出来一个Demo类，肯定和spring容器里获取的不是同一个实例
    Demo demo3 = demoFactoryBean.getObject();
    System.out.println(demo3);
}
```

运行结果为：  
com.study.spring.ioc.factoryBean.Demo@5cc816a9  
com.study.spring.ioc.factoryBean.Demo@5cc816a9  
com.study.spring.ioc.factoryBean.DemoFactoryBean@5447e0d5  
com.study.spring.ioc.factoryBean.Demo@13ee89c0


可以看出我们通过demoFactoryBean这个beanName获取到的是Demo这个实例，因为isSingleton方法返回的是true，所以两次获取到的Demo对象是同一个。

如果想获取DemoFactoryBean本身怎么办？通过加前缀&符号获取。

我们经常用到的SqlSessionFactoryBean就是一个FactoryBean的实现类，用于创建SqlSessionFactory。