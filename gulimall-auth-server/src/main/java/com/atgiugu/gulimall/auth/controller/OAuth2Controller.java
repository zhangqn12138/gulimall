package com.atgiugu.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atgiugu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.common.vo.MemberRespVo;
import com.atgiugu.gulimall.auth.vo.SocialUser;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * @author QingnanZhang
 * @creat 2022-06-23 13:42
 **/
@Slf4j
@Controller
public class OAuth2Controller {

    private MemberFeignService memberFeignService;

    /**
     * 社交登录成功回调
     * @param code
     * @return
     * @throws Exception
     */
    @RequestMapping("/oauth2.0/weibo/success")
    public String weibo(@RequestParam String code, HttpSession session) throws Exception {
        //1.根据code换取accessToken、uid等信息
        Map<String, String> map = new HashMap<>();
        //TODO 网页的client_id和这里的client_id、这里的client_secret都未写
        map.put("client_id","");
        map.put("client_secret", "");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://gulimall.com/oauth2.0/weibo/success");
        map.put("code", code);
        HttpResponse response = HttpUtils.doPost("api.weibo.com", "/oauth2/access_token", "post", null, null, map);
        //2.进行登录
        //2.1 处理换取结果
        if(response.getStatusLine().getStatusCode() == 200){
            //换取成功
            //① 获取换取结果
            String json = EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);
            //② 判断当前用户是否第一次登录
            // 是，则进行注册（生成一条属于该社交用户的会员信息账号）
            // 不是，则进行更新
            R oauthlogin = memberFeignService.oauthlogin(socialUser);
            if(oauthlogin.getCode() == 0){
                //成功
                MemberRespVo data = oauthlogin.getData("data", new TypeReference<MemberRespVo>() {
                });
                log.info("登陆成功，用户信息为：" + data.toString());
                //两个问题：① 分布式情况下，session的存储问题 ② 子域session的共享问题 ③ 优化：使用JSON序列化的方式来序列化对象数据到redis中
                session.setAttribute("loginUser", data);
                return "redirect:http://gulimall.com";
            }else{
                //失败
                return "redirect:http://auth.gulimall.com/login.html";
            }
        }else{
            //换取失败则返回注册页
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }
}
