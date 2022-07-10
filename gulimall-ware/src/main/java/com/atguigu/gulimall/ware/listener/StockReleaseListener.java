package com.atguigu.gulimall.ware.listener;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.OrderTo;
import com.atguigu.common.to.mq.StockDeatilTo;
import com.atguigu.common.to.mq.StockLockedTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.dao.WareSkuDao;
import com.atguigu.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.atguigu.gulimall.ware.entity.WareOrderTaskEntity;
import com.atguigu.gulimall.ware.feign.OrderFeignService;
import com.atguigu.gulimall.ware.service.WareOrderTaskDetailService;
import com.atguigu.gulimall.ware.service.WareOrderTaskService;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.gulimall.ware.vo.OrderVo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

import java.io.IOException;

/*
@author zqn
@create 2022-07-09 18:15
*/

@RabbitListener(queues = "stock.release.stock.queue")
@Service
public class StockReleaseListener {

    @Autowired
    private WareSkuService wareSkuService;

    /**
     * 注意：如果库存解锁失败，应该重试，因此不能使用RabbitMQ的自动确认，要改为手动
     * @param to
     * @param message
     * @param channel
     * @throws IOException
     */
    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo to,Message message , Channel channel) throws IOException {
        try {
            //库存锁定失败，此时无需解锁，因为都回滚了
            //订单是正常的
            //订单状态不是已锁定
            //订单不存在，或者订单存在但状态为已取消，此时需要解锁库存
            wareSkuService.unlockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e){
            //远程查询失败，没能成功进行后续操作，会抛出异常，需要将消息放回队列，等待下一次消费
            //解锁库存失败，会抛出异常，需要将消息放回队列，等待下一次消费
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }

    @RabbitHandler
    public void handlerOrderCloseRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {
        try{
            wareSkuService.unlockStock(orderTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
