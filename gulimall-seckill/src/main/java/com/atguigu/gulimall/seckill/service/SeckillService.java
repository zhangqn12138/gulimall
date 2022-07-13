package com.atguigu.gulimall.seckill.service;

import com.atguigu.gulimall.seckill.to.SecKillSkuRedisTo;

import java.util.List;

/**
 * @author QingnanZhang
 * @creat 2022-07-12 17:09
 **/
public interface SeckillService {

    void uploadSeckillSkuLatest3Days();

    List<SecKillSkuRedisTo> getCurrentSeckillSkus();

    SecKillSkuRedisTo getSkukillInfo(Long skuId);

    String kill(String killId, String key, Integer num);
}
