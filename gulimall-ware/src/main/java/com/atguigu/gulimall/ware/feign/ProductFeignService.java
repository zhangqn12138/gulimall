package com.atguigu.gulimall.ware.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author QingnanZhang
 * @creat 2022-04-06 16:55
 **/
@FeignClient("gulimall-product")
public interface ProductFeignService {

    /**
     * 两种方式可以进行远程调用
     *  1、通过网关进行远程调用
     *      ① @FeignClient("gulimall-gateway")：给gulimall-gateway所在机器发请求
     *      ② /api/product/skuinfo/info/{skuId}
     *
     *  2、后端服务之间远程调用
     *      ① @FeignClient("gulimall-product")：直接让后台指定服务处理/给gulimall-product所在机器发请求
     *      ② /product/skuinfo/info/{skuId}
     * @param skuId
     * @return
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    public R info(@PathVariable("skuId") Long skuId);
}
