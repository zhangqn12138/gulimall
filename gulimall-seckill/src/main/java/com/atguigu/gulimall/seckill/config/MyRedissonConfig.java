package com.atguigu.gulimall.seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author QingnanZhang
 * @creat 2022-06-07 10:22
 **/
@Configuration
public class MyRedissonConfig {
    /**
     * 所有对Redisson的使用都是通过RedissonClient对象
     * @return
     */
    @Bean
    public RedissonClient redisson(){
        //1.创建配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://101.43.199.148:6379");
        //2.根据配置创建RedissonClient实例
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }
}

