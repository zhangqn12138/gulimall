package com.atguigu.gulimall.ware.dao;

import com.atguigu.gulimall.ware.entity.PurchaseEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采购信息
 * 
 * @author zhangqingnan
 * @email zhangqingnan@gmail.com
 * @date 2022-04-06 10:32:39
 */
@Mapper
public interface PurchaseDao extends BaseMapper<PurchaseEntity> {
	
}
