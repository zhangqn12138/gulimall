package com.atguigu.gulimall.ware;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;

//数据库有关配置都配到了com.atguigu.gulimall.ware.config.MyBatisConfig中
//@EnableTransactionManagement
//@MapperScan("com.atguigu.gulimall.ware.dao")
@EnableRabbit
@EnableFeignClients(basePackages = "com.atguigu.gulimall.ware.feign")
@SpringBootApplication
@EnableDiscoveryClient
public class GulimallWareApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallWareApplication.class, args);
    }

}
