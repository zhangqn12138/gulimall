package com.atguigu.gulimall.order.dao;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author zhangqingnan
 * @email zhangqingnan@gmail.com
 * @date 2022-02-26 18:04:41
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
