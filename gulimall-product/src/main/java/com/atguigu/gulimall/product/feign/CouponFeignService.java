package com.atguigu.gulimall.product.feign;

import com.atguigu.common.to.SkuReductionTo;
import com.atguigu.common.to.SpuBoundsTo;
import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author QingnanZhang
 * @creat 2022-04-05 18:17
 **/
@FeignClient("gulimall-coupon")
public interface CouponFeignService {

    /**
     * 1.当我们在feign的接口中的定义了CouponFeignService.saveSpuBounds(SpuBoundsTo)方法
     *      ① SpringCloud利用@RequestBody注解将这个对象转为json
     *      ② 在注册中心中找到远程服务gulimall-coupon，给服务的/coupon/spubounds/save发送请求，将上一步转换出来的json放在请求体
     *        位置，发送请求
     *      ③ 被调用的远程服务收到请求并接收到了请求体里的json数据，SpuBoundsController.save(@RequestBody SpuBoundsEntity spuBounds)
     *        将请求体的json转为SpuBoundsEntity
     *      注解：只要SpuBoundsTo和SpuBoundsEntity里面的属性名存在一部分一一对应就可以进行以上的转换
     *      总结：只要json数据模型是兼容的，双方服务无需使用同一个TO
     * @param spuBoundsTo
     * @return
     */
    @PostMapping("/coupon/spubounds/save")
    R saveSpuBounds(@RequestBody SpuBoundsTo spuBoundsTo);

    @PostMapping("/coupon/skufullreduction/saveinfo")
    R saveSkuReduction(@RequestBody SkuReductionTo skuReductionTo);
}
