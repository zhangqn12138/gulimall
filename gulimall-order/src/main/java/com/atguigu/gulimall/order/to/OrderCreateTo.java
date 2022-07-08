package com.atguigu.gulimall.order.to;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/*
@author zqn
@create 2022-07-07 15:06
*/
@Data
public class OrderCreateTo {

    private OrderEntity order;

    private List<OrderItemEntity> orderItems;

    private BigDecimal payPrice;//订单计算的应付价格

    private BigDecimal fare;//运费
}
