package com.atgiugu.gulimall.auth.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author QingnanZhang
 * @creat 2022-06-21 10:30
 **/
@FeignClient("gulimall-third-party")
public interface ThirdPartFeignService {

    @RequestMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone, @RequestParam("code") String code);
}
