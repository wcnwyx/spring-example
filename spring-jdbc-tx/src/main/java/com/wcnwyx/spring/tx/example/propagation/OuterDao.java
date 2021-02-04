package com.wcnwyx.spring.tx.example.propagation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class OuterDao {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private InnerDao innerDao;

    public void outer() throws Exception{
        jdbcTemplate.execute("insert into user(name) values('outer')");
        innerDao.inner();
    }


}
