package com.atgiugu.gulimall.auth.feign;

import com.atgiugu.gulimall.auth.vo.SocialUser;
import com.atgiugu.gulimall.auth.vo.UserLoginVo;
import com.atgiugu.gulimall.auth.vo.UserRegistVo;
import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author QingnanZhang
 * @creat 2022-06-22 10:26
 **/
@FeignClient("gulimall-member")
public interface MemberFeignService {
    @RequestMapping("/member/member/regist")
    R regist(@RequestBody UserRegistVo vo);

    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo vo);

    @PostMapping("/member/member/oauth/login")
    R oauthlogin(@RequestBody SocialUser socialUser) throws Exception;
}
