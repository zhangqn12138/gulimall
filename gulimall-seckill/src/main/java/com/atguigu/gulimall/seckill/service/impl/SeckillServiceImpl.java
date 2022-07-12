package com.atguigu.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.seckill.feign.CouponFeignService;
import com.atguigu.gulimall.seckill.feign.ProductFeignService;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.atguigu.gulimall.seckill.to.SecKillSkuRedisTo;
import com.atguigu.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.atguigu.gulimall.seckill.vo.SkuInfoVo;
import com.sun.org.apache.bcel.internal.generic.NEW;
import org.redisson.Redisson;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author QingnanZhang
 * @creat 2022-07-12 17:10
 **/
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private RedissonClient redissonClient;

    private final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";

    private final String SKUKILL_CACHE_PREFIX = "seckill:stock:";

    private final String SKU_STOCK_SEMAPHORE = "seckill:skus:";

    @Override
    public void uploadSeckillSkuLatest3Days() {
        //1.调用营销服务扫描最近三天的秒杀活动及其商品
        R r = couponFeignService.getLates3DaySession();
        if(r.getCode() == 0){
            //上架商品
            List<SeckillSessionsWithSkus> sessions = r.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            //缓存到redis
            //缓存活动信息
            saveSessionInfos(sessions);
            //缓存活动的关联商品信息
            saveSessionSkuInfos(sessions);
        }
    }

    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions){
        sessions.stream().forEach((session -> {
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();
            String key = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;
            Boolean hasKey = stringRedisTemplate.hasKey(key);
            if(!hasKey) {//如果已经有了这个场次的信息，就不用再上架了
                List<String> collect = session.getRelationSkus().stream().map((item) -> {
                    String sessionAndSkuId = session.getId().toString() + "_" + item.getSkuId().toString();
                    return sessionAndSkuId;
                }).collect(Collectors.toList());
                stringRedisTemplate.opsForList().leftPushAll(key, collect);
            }
        }));
    }

    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions){
        sessions.stream().forEach((session) -> {
            BoundHashOperations<String, Object, Object> ops = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
            session.getRelationSkus().stream().forEach((item) -> {
                if(!ops.hasKey(session.getId().toString() + "_" + item.getSkuId().toString())){
                    SecKillSkuRedisTo redisTo = new SecKillSkuRedisTo();
                    //缓存商品信息
                    //1.商品服务中商品的详细信息
                    R r = productFeignService.getSkuInfo(item.getSkuId());
                    if(r.getCode() == 0){
                        SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfo(skuInfo);
                    }

                    //2.sms_seckill_sku_relation表中的商品秒杀信息
                    BeanUtils.copyProperties(item, redisTo);

                    //3.开始时间和结束时间
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());

                    //4.设置随机码
                    String token = UUID.randomUUID().toString().replace("-", "");
                    redisTo.setRandomCode(token);

                    //信号量（使用商品可以秒杀的数量）；用于限流
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    semaphore.trySetPermits(item.getSeckillCount());

                    String jsonString = JSON.toJSONString(redisTo);
                    ops.put(session.getId().toString() + "_" + item.getSkuId().toString(), jsonString);

                }

            });
        });
    }
}
