package com.atgiugu.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.atgiugu.gulimall.auth.feign.MemberFeignService;
import com.atgiugu.gulimall.auth.feign.ThirdPartFeignService;
import com.atgiugu.gulimall.auth.vo.UserLoginVo;
import com.atgiugu.gulimall.auth.vo.UserRegistVo;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author QingnanZhang
 * @creat 2022-06-16 10:46
 **/
@Controller
public class LoginController {

    /**
     * 有很多的业务需求都仅仅是只需要通过路径映射到指定的静态页面，如果每一个都需要利用以下方式则需要书写大量的空方法，为此，我们选择
     * 直接配置以一个SpringMVC的viewcontroller，来完成映射过程
     */
    /**
    @GetMapping("/login.html")
    public String loginPage(){
        return "login";
    }

    @GetMapping("/reg.html")
    public String regPage(){
        return "reg";
    }
    */

    @Autowired
    private ThirdPartFeignService thirdPartFeignService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private MemberFeignService memberFeignService;

    @ResponseBody
    @RequestMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone){
        //TODO 验证码接口防盗刷
        //1.验证码的再次校验和时效 ==> key：sms:code:phone    value：code_currentTime
        String redisCode = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if(!StringUtils.isEmpty(redisCode)){
            long time = Long.parseLong(redisCode.split("_")[1]);
            if(System.currentTimeMillis() - time < 60000){
                //60s内不可再发
                return R.error(BizCodeEnume.SMS_CODE_EXCEPION.getCode(), BizCodeEnume.SMS_CODE_EXCEPION.getMsg());
            }
        }
        String code = UUID.randomUUID().toString().substring(0, 5);
        String str = code + "_" + System.currentTimeMillis();
        stringRedisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, str, 10, TimeUnit.MINUTES);
        thirdPartFeignService.sendCode(phone, code);
        return R.ok();
    }

    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result, RedirectAttributes redirectAttributes){
        if(result.hasErrors()){
            //手机所有的错误信息
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            //检验出错，重定向到注册页
            //不能用forward:/reg.html ==> 用户注册发给/regist一个POST请求，转发会把该请求原封不动的转给配置的视图控制器，而试图控制器只能处理GET请求，因此会报异常
            //不能用/reg ==> 转发的路径不变，算作一次请求，所以在刷新注册页面时，会造成表单的重复提交（上面的方式也会有此问题）
            //用redirect:http://auth.gulimall.com/regist.html ==> 此时不能用Model进行数据共享，此时要用RedirectAttributes进行数据的共享，此时，实际上是把数据存放在服务器端的HttpSession中，此时请求中会携带一个Cookie
            //TODO 分布式情况下，使用HttpSession共享数据会存在问题，需要解决
            redirectAttributes.addFlashAttribute("errors", errors);//只要取出数据，HttpSession里面的数据就会被删除
            return "redirect:http://auth.gulimall.com/regist.html";

        }
        //调用远程服务进行真正的注册
        //1.校验验证码
        String code = vo.getCode();
        String verify = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if(!StringUtils.isEmpty(verify)){
            if(code.equals(verify.split("_")[0])){
                //删除验证码：令牌机制
                stringRedisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
                //验证码通过，调用远程服务，进行真正的注册
                R regist = memberFeignService.regist(vo);
                if(regist.getCode() == 0){
                    //成功
                    return "redirect:http://auth.gulimall.com/login.html";
                }else{
                    //失败
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg",regist.getData("msg", new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors", errors);
                    return "redirect:http://auth.gulimall.com/regist.html";
                }
            }else{
                //验证码错误
                Map<String, String> errors = new HashMap<>();
                errors.put(code, "验证码错误");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/regist.html";
            }

        }else{
            //验证码过期
            Map<String, String> errors = new HashMap<>();
            errors.put(code, "验证码已过期");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/regist.html";
        }
    }

    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session){//页面传来的时key-value，不是json，所以不可以使用@RequstBody
        //远程登陆
        R login = memberFeignService.login(vo);
        if(login.getCode() == 0){
            //成功
            MemberRespVo data = login.getData("data", new TypeReference<MemberRespVo>() {
            });
            session.setAttribute(AuthServerConstant.LOGIN_USER, data);
            return "redirect:http://gulimall.com";
        }else {
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", login.getData("msg", new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }

    @GetMapping("login.html")
    public String loginPage(HttpSession session){
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if(attribute == null){
            //未登录
            return "login";
        }else{
            return "redirect:http://gulimall.com/";
        }
    }
}
