package com.atguigu.gulimall.seckill.to;

import com.atguigu.gulimall.seckill.vo.SkuInfoVo;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author QingnanZhang
 * @creat 2022-07-12 18:11
 **/
@Data
public class SecKillSkuRedisTo {

    //sku秒杀信息
    /**
     * 活动id
     */
    private Long promotionId;
    /**
     * 活动场次id
     */
    private Long promotionSessionId;
    /**
     * 商品id
     */
    private Long skuId;
    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;
    /**
     * 秒杀总量
     */
    private Integer seckillCount;
    /**
     * 每人限购数量
     */
    private Integer seckillLimit;
    /**
     * 排序
     */
    private Integer seckillSort;

    //sku详细信息
    private SkuInfoVo skuInfo;

    //开始时间
    private Long startTime;

    //结束时间
    private Long endTime;

    //商品随机码
    private String randomCode;
}
