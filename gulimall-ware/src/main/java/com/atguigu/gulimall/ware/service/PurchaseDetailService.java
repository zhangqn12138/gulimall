package com.atguigu.gulimall.ware.service;

import com.atguigu.gulimall.ware.entity.PurchaseDetailEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;

import java.util.List;
import java.util.Map;

/**
 * 
 *
 * @author zhangqingnan
 * @email zhangqingnan@gmail.com
 * @date 2022-04-06 10:32:39
 */
public interface PurchaseDetailService extends IService<PurchaseDetailEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<PurchaseDetailEntity> listDeatilByPurchaseId(Long id);
}

