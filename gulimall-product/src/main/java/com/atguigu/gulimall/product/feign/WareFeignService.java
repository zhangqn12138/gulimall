package com.atguigu.gulimall.product.feign;

import com.atguigu.common.to.SkuHasStockVo;
import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author QingnanZhang
 * @creat 2022-06-04 20:19
 **/
@FeignClient("gulimall-ware")
public interface WareFeignService {

    /**
     * 为了方便的使用返回类型，有三种方法
     * 1.设计R的时候加上泛型
     * 2.直接返回想要的结果
     * 3.自己封装解析后的结果
     * @param skuIds
     * @return
     */
    @PostMapping("/ware/waresku/hasstock")
    public R getSkusHasStock(@RequestBody List<Long> skuIds);

}
