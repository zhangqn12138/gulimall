package com.atguigu.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.SeckillOrderTo;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.seckill.feign.CouponFeignService;
import com.atguigu.gulimall.seckill.feign.ProductFeignService;
import com.atguigu.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.atguigu.gulimall.seckill.to.SecKillSkuRedisTo;
import com.atguigu.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.atguigu.gulimall.seckill.vo.SkuInfoVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.sun.org.apache.bcel.internal.generic.NEW;
import org.apache.commons.lang.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.xml.crypto.Data;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
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

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";

    private final String SKUKILL_CACHE_PREFIX = "seckill:skus";

    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";

    @Override
    public void uploadSeckillSkuLatest3Days() {
        //1.???????????????????????????????????????????????????????????????
        R r = couponFeignService.getLates3DaySession();
        if(r.getCode() == 0){
            //????????????
            List<SeckillSessionsWithSkus> sessions = r.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            //?????????redis
            //??????????????????
            saveSessionInfos(sessions);
            //?????????????????????????????????
            saveSessionSkuInfos(sessions);
        }
    }

    @Override
    public List<SecKillSkuRedisTo> getCurrentSeckillSkus() {
        //1.?????????????????????????????????
        long currentTime = new Date().getTime();
        Set<String> keys = stringRedisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");
        for (String key : keys) {
            String replace = key.replace(SESSIONS_CACHE_PREFIX, "");
            String[] time = replace.split("_");
            long startTime = Long.parseLong(time[0]);
            long endTime = Long.parseLong(time[1]);
            if(currentTime >= startTime && currentTime <= endTime){
                //2.??????????????????????????????????????????????????????
                List<String> range = stringRedisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                List<String> list = hashOps.multiGet(range);
                if(list != null && list.size() > 0){
                    List<SecKillSkuRedisTo> tos = list.stream().map((item) -> {
                        SecKillSkuRedisTo redisTo = JSON.parseObject(item, SecKillSkuRedisTo.class);
                        return redisTo;
                    }).collect(Collectors.toList());
                    return tos;
                }
                break;
            }
        }

        return null;
    }

    @Override
    public SecKillSkuRedisTo getSkukillInfo(Long skuId) {
        //??????????????????????????????????????????key
        BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if(keys != null && keys.size() > 0){
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                if(Pattern.matches(regx, key)){
                    String json = hashOps.get(key);
                    SecKillSkuRedisTo to = JSON.parseObject(json, SecKillSkuRedisTo.class);
                    long currentTime = new Date().getTime();
                    if(currentTime >= to.getStartTime() && currentTime <= to.getEndTime()){
                    }else{
                        to.setRandomCode(null);
                    }
                    return to;
                }
            }
        }
        return null;
    }

    @Override
    public String kill(String killId, String key, Integer num) {

        MemberRespVo respVo = LoginUserInterceptor.loginUser.get();

        BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

        String json = hashOps.get(killId);

        if(!StringUtils.isEmpty(json)){
            SecKillSkuRedisTo redisTo = JSON.parseObject(json, SecKillSkuRedisTo.class);
            //???????????????
            Long startTime = redisTo.getStartTime();
            Long endTime = redisTo.getEndTime();
            long currentTime = new Date().getTime();
            //1.?????????????????????
            if(currentTime >= startTime && currentTime <= endTime){
                //2.????????????id?????????????????????
                String randomCode = redisTo.getRandomCode();
                String skuIdAndRandomCode = redisTo.getPromotionSessionId() + "_" + redisTo.getSkuId();
                if(randomCode.equals(key) && killId.equals(skuIdAndRandomCode)){
                    //3.??????????????????????????????
                    if(num <= redisTo.getSeckillCount()){
                        //4.??????????????????????????????
                        String redisKey = respVo.getId() + "_" + skuIdAndRandomCode;
                        long ttl = startTime - currentTime;
                        Boolean notBuy = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                        if(notBuy){
                            //?????????????????????
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE);
                            boolean decSuccess = semaphore.tryAcquire(num);
                            //???????????????????????????MQ???????????????
                            if(decSuccess){
                                String orderSn = IdWorker.getTimeId();
                                SeckillOrderTo seckillOrderTo = new SeckillOrderTo();
                                seckillOrderTo.setOrderSn(orderSn);
                                seckillOrderTo.setMemberId(respVo.getId());
                                seckillOrderTo.setNum(num);
                                seckillOrderTo.setPromotionSessionId(redisTo.getPromotionSessionId());
                                seckillOrderTo.setSkuId(redisTo.getSkuId());
                                seckillOrderTo.setSeckillPrice(redisTo.getSeckillPrice());
                                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", seckillOrderTo);
                                return orderSn;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions){
        sessions.stream().forEach((session -> {
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();
            String key = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;
            Boolean hasKey = stringRedisTemplate.hasKey(key);
            if(!hasKey) {//???????????????????????????????????????????????????????????????
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
                    //??????????????????
                    //1.????????????????????????????????????
                    R r = productFeignService.getSkuInfo(item.getSkuId());
                    if(r.getCode() == 0){
                        SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfo(skuInfo);
                    }

                    //2.sms_seckill_sku_relation???????????????????????????
                    BeanUtils.copyProperties(item, redisTo);

                    //3.???????????????????????????
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());

                    //4.???????????????
                    String token = UUID.randomUUID().toString().replace("-", "");
                    redisTo.setRandomCode(token);

                    //???????????????????????????????????????????????????????????????
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    semaphore.trySetPermits(item.getSeckillCount());

                    String jsonString = JSON.toJSONString(redisTo);
                    ops.put(session.getId().toString() + "_" + item.getSkuId().toString(), jsonString);

                }

            });
        });
    }
}
