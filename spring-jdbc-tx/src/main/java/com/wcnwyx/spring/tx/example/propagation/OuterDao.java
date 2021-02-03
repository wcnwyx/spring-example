package com.wcnwyx.spring.tx.example.propagation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Propagation.REQUIRED : inner发现已有事务，和outer使用同一个事务，inner报错，两个方法都回滚。
 *
 */
@Repository
public class OuterDao {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private InnerDao innerDao;

    @Transactional()
    public void outer(){
        jdbcTemplate.execute("insert into user(name,sex) values('outer',1)");
//        try{
            innerDao.inner();
//        }catch (Exception e){
//
//        }
        int a = 1/0;
    }


}
