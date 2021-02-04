spring事务的传播属性（propagation）主要用于多个方法调用链中，如果多个方法都有@Transactional注解，那这几个方法的事务是独立的还是共享的。  
先列一下所有的传播属性，然后通过两个方法outer和inner来测试一下，outer方法内部调用inner方法。    
1. REQUIRED：默认级别，如果outer没有事务，inner会创建一个新的事务来执行；如果outer有事务，inner不会再新建事务，和outer共用。  
2. REQUIRES_NEW：不管outer有没有事务，都会新建一个自己的事务。outer有事务，inner也会新建一个自己的事务。 
3. SUPPORTS：inner在执行时，如果outer声明了事务，则和outer公用一个事务，如果outer没有事务，自己也不用事务。
4. NOT_SUPPORTED: 表明该方法不支持事务，即使outer有事务，inner也不支持事务，inner执行时会将outer的事务挂起。
5. MANDATORY: 强制性使用事务，如果outer未声明事务，inner声明了MANDATORY，则会抛出异常；如果outer声明了事务，inner和outer公用一个事务。
6. NEVER：和NOT_SUPPORTED很像，都是强制不在事务中执行，但是NEVER不是将已有的事务挂起，而是直接抛出异常。
7. NESTED：和REQUIRED类似，但是只支持JDBC，不支持JTA或者Hibernate。

##REQUIRED测试
###测试1：  
outer不定义事务，inner定义REQUIRED事务，并在inner里抛出异常。  
代码如下：  
```
    public void outer() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('outer')");
        innerDao.inner();
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void inner() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('inner')");
        throw new RuntimeException();
    }
```
运行结果：outer提交入库，inner数据回滚。 说明inner自己创建了一个新的事务，因为异常所以回滚了数据。  

###测试2  
outer定义事务，inner采用REQUIRED事务，并在inner里抛出异常。  
测试代码如下：  
```
    @Transactional
    public void outer() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('outer')");
        innerDao.inner();
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void inner() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('inner')");
        throw new RuntimeException();
    }
```
测试结果：两条数据都被回滚，因为inner和outer使用的是用一个事务。  

疑问1：  
如果按照Spring事务的TransactionInterceptor里的逻辑，方法调用完就会根据是否有异常来执行commit或者rollback。  
inner里抛出异常，inner会执行一次rollback，但是异常传递到outer方法里，outer也会抛出了异常，  
outer又会在做一次rollback，两次rollback不会报错吗？是不会报错的。  
原因：  
因为两个方法用的是同一个事务，inner方法持有的TransactionStatus里newTransaction=false，表示不是自己新创建的事务，  
那么在执行回滚的操作时，不是做的真实的回滚，只是设置了一个RollbackOnly状态，表示该事务只能回滚，不能提交。  

根据这个结论，我们再改一下测试代码进行测试。
###测试3
在outer里将inner的调用try-catch住，不让outer抛出异常，这样子outer就会执行commit，看看什么效果。    
代码如下：  
```
    @Transactional
    public void outer() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('outer')");
        try{
            innerDao.inner();
        }catch (Exception e){

        }
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void inner() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('inner')");
        throw new RuntimeException();
    }
```
运行结果：两条数据都被回滚了，并抛出异常UnexpectedRollbackException。  
下面是异常栈信息：  
```
Exception in thread "main" org.springframework.transaction.UnexpectedRollbackException: Transaction rolled back because it has been marked as rollback-only
	at org.springframework.transaction.support.AbstractPlatformTransactionManager.commit(AbstractPlatformTransactionManager.java:728)
	at org.springframework.transaction.interceptor.TransactionAspectSupport.commitTransactionAfterReturning(TransactionAspectSupport.java:521)
	at org.springframework.transaction.interceptor.TransactionAspectSupport.invokeWithinTransaction(TransactionAspectSupport.java:293)
	at org.springframework.transaction.interceptor.TransactionInterceptor.invoke(TransactionInterceptor.java:96)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:179)
	at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:671)
	at com.wcnwyx.spring.tx.example.propagation.OuterDao$$EnhancerBySpringCGLIB$$de6a7ea9.outer(<generated>)
	at com.wcnwyx.spring.tx.example.propagation.Test.main(Test.java:9)
```
可以看出outer的确去执行commit了，但是commit异常，还是执行了rollback，因为transaction已经被标注为rollback-only，只能回滚，不能提交。  


疑问2：  
为什么inner不能执行真实的回滚呢？因为inner内部并不知道该事务是否进行完了，如果inner调用完outer里还有其它的逻辑要走呢？事务rollback之后该事务的状态就是已完成了，后续不能再进行其它操作了。  


疑问3：  
根据上面的逻辑，那如果inner里没有抛出异常，outer里抛出异常，是不是inner方法的commit应该也是一个假动作呢？最终结果会被outer的回滚全部回滚吗？  
根据这个想法，再改下测试代码测试。
###测试4
```
    @Transactional()
    public void outer() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('outer')");
        innerDao.inner();
        throw new RuntimeException();
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void inner() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('inner')");
    }
```
运行结果：两条数据都被回滚了，说明inner的commit没有起作用。  
原因和上面说到的一样，因为inner方法持有的TransactionStatus里newTransaction=false，表示这个事务不是自己创建的，自己不负责结束该事务。  
commit的步骤里真实的doCommit方法会被跳过，不执行。  


##REQUIRES_NEW
REQUIRES_NEW表示各自创建各自的事务，即使outer方法已经有了事务，但是inner还是会创建自己的事务。  
###测试1：
outer声明事务，inner声明REQUIRES_NEW事务，并在inner内部抛出异常。  
代码如下：  
```
    @Transactional()
    public void outer() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('outer')");
        innerDao.inner();
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void inner() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('inner')");
        throw new RuntimeException();
    }
```
运行结果：两条数据都被回滚了，不是说单独的事务互不影响吗？为什么outer没有插入进去呢？  
因为inner抛出了异常，但是传递到outer方法后，outer方法没有处理，也直接抛出了异常，  
导致两个事务都捕获到了异常，所以两条数据都回滚了。  

再修改下测试代码，在outer方法中将inner的异常捕获住，不让outer再抛出异常。  
代码如下：  
```
    @Transactional()
    public void outer() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('outer')");
        try{
            innerDao.inner();
        }catch (Exception e){
            System.out.println("inner异常了，但是我要继续正常执行");
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void inner() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('inner')");
        throw new RuntimeException();
    }
```
运行结果：outer正常commit了，inner被回滚，符合预期效果。

##SUPPORTS
表示执行时如果有事务，就共用已有的事务，如果没有事务，那自己也不用事务。
###测试1：
outer没有事务，inner为SUPPORTS事务，inner内部抛出异常  
代码如下：  
```
    public void outer() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('outer')");
        innerDao.inner();
    }
    
    @Transactional(propagation = Propagation.SUPPORTS)
    public void inner() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('inner')");
        throw new RuntimeException();
    }
```
运行结果：两条数据都入库了，没有回滚。  
说明inner方法SUPPORTS类型的事务，因为outer没有事务，所以inner也不带事务执行。  

###测试2：
outer声明事务，inner声明SUPPORTS事务，在inner调用后抛出异常
代码如下：  
```
    @Transactional
    public void outer() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('outer')");
        innerDao.inner();
        throw new RuntimeException();
    }
    
    @Transactional(propagation = Propagation.SUPPORTS)
    public void inner() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('inner')");
    }
```
运行结果：两条数据都回滚了，说明inner也是带事务执行了，并且和outer使用的是同一个事务。  

##NOT_SUPPORTED
表明该方法不支持事务，即使outer有事务，inner也不支持事务。
###测试1：
outer声明事务，inner声明为NOT_SUPPORTED并且抛出异常  
代码如下：
```
    @Transactional
    public void outer() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('outer')");
        innerDao.inner();
    }
    
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void inner() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('inner')");
        throw new RuntimeException();
    }
```
运行结果：outer回滚了，inner成功入库。和预期结果一致。

疑问？  
为什么要这个呢？直接inner方法不标注@Transactional注解不就行了吗？  
不行，如果不标注的话，inner方法执行过程中使用的就是outer的事务。
###测试2：
outer声明事务,并且在inner调用后抛出异常，inner不加@Transactional注解  
代码如下：
```
    @Transactional
    public void outer() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('outer')");
        innerDao.inner();
        throw new RuntimeException();
    }
    
    public void inner() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('inner')");
    }
```
运行结果：两条数据都被回滚了，说明inner即使没有被声明@Transactional，但是也拥有了事务，并且和outer是同一个。  
NOT_SUPPORTED明确是强制该方法不用事务的，如果有事务，就会将当前事务挂起。

##MANDATORY
强制性使用事务，如果没有事务就抛出异常。
###测试1：
outer没有事务，inner声明为MANDATORY类型事务    
代码如下：  
```
    public void outer() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('outer')");
        innerDao.inner();
    }
    
    @Transactional(propagation = Propagation.MANDATORY)
    public void inner() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('inner')");
    }
```
测试结果：outer入库成功，因为outer没有事务并且入库操作本来就在inner方法调用之前，
inner入库失败，抛出异常`IllegalTransactionStateException: No existing transaction found for transaction marked with propagation 'mandatory'`  
测试结果符合预期，inner标注了强制性的事务，但是outer却没有事务，就会抛出异常。  

###测试2：
outer标注事务并在inner调用后抛出异常，inner使用MANDATORY强制性事务，验证使用的是否是同一个事务。  
代码如下：  
```
    @Transactional
    public void outer() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('outer')");
        innerDao.inner();
        throw new RuntimeException();
    }
    
    @Transactional(propagation = Propagation.MANDATORY)
    public void inner() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('inner')");
    }
```
运行结果：两条数据都回滚了，说明MANDATORY是共用了已有的事务。  

##NEVER
NEVER和NOT_SUPPORTED很像，都是强制不在事务中执行，但是NEVER不是将已有的事务挂起，而是直接抛出异常。
测试：outer标注事务，inner标注NEVER事务  
代码如下：  
```
    @Transactional
    public void outer() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('outer')");
        innerDao.inner();
    }
    
    @Transactional(propagation = Propagation.NEVER)
    public void inner() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('inner')");
    }
```
运行结果：两条数据都被回滚了，并且抛出异常`IllegalTransactionStateException: Existing transaction found for transaction marked with propagation 'never'`  
inner执行时因为标注的是NEVER并且outer带有事务，所以抛出异常。  
outer因为未捕获异常，直接往上继续抛，导致outer事务回滚。  

##NESTED
功能和REQUIRED类似，但是只支持JDBC，不支持JTA或者Hibernate