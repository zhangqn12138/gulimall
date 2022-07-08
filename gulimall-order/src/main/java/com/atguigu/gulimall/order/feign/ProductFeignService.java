package com.atguigu.gulimall.order.feign;
/*
@author zqn
@create 2022-07-07 16:23
*/

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("gulimall-product")
public interface ProductFeignService {

    @PostMapping("/product/spuinfo/skuId/{id}")
    R getSpuInfoBySkuId(@RequestParam("id") Long skuId);
}
