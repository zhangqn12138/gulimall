package com.atguigu.gulimall.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/*
@author zqn
@create 2022-07-09 15:07
*/
@Configuration
public class MyMQConfig {

    /**
     * 容器中的组件都会自动在RabbitMQ中创建
     * 要确保RabbitMQ中没有这些同名组件，因为不会覆盖
     * @return
     */
    @Bean
    public Queue orderDelayQueue(){
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", "oder-event-exchange");
        arguments.put("x-dead-letter-routing-key", "oder.release.order");
        arguments.put("x-message-ttl", 60000);
        Queue queue = new Queue("order.delay.queue", true, false, false, arguments);
        return queue;
    }

    @Bean
    public Queue orderReleaseOrderQueue(){
        Queue queue = new Queue("order.release.order.queue", true, false, false);
        return queue;
    }

    @Bean
    public Exchange orderEventChange(){
        TopicExchange topicExchange = new TopicExchange("order-event-exchange", true, false);
        return topicExchange;
    }

    @Bean
    public Binding orderCreateOrderBinding(){
        Binding binding = new Binding("order.delay.queue"
                , Binding.DestinationType.QUEUE
                , "order-event-exchange"
                , "order.create.order"
                , null);
        return binding;
    }

    @Bean
    public Binding orderReleaseOrderBinding(){
        Binding binding = new Binding("order.release.order.queue"
                , Binding.DestinationType.QUEUE
                , "order-event-exchange"
                , "order.release.order"
                , null);
        return binding;
    }


}
