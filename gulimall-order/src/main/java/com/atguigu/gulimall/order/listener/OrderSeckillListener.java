package com.atguigu.gulimall.order.listener;

import com.atguigu.common.to.SeckillOrderTo;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author QingnanZhang
 * @creat 2022-07-13 14:51
 **/
@RabbitListener(queues = "order.seckill.order.queue")
@Component
public class OrderSeckillListener {
    @Autowired
    private OrderService orderService;


    @RabbitHandler
    public void listener(SeckillOrderTo orderTo, Message message, Channel channel) throws IOException {
        orderService.createSeckillOrder(orderTo);
    }
}
