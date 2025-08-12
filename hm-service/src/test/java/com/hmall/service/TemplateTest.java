package com.hmall.service;

import com.hmall.HMallApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest(classes = HMallApplication.class)
public class TemplateTest {
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

//    @Autowired
//    private RedisTemplate redisTemplate;

    @Test
    public void testString() {
        System.out.println(111);
        // 写入一条String数据
        redisTemplate.opsForValue().set("name", "虎哥17");
        // 获取string数据
        Object name = redisTemplate.opsForValue().get("name");
        System.out.println("name = " + name);
    }
}