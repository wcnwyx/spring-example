package com.wcnwyx.spring.tx.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class DemoDao {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void insert(String name, int sex){
        jdbcTemplate.execute("insert into user(name,sex) values('"+name+"',"+sex+")");
        int a = 1/0;
    }
}
