package com.atguigu.gulimall.ware.feign;
/*
@author zqn
@create 2022-07-09 18:37
*/

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("gulimall-order")
public interface OrderFeignService {

    @GetMapping("/order/order/status/{orderSn}")
    R getOrder(@PathVariable("orderSn") String orderSn);
}
