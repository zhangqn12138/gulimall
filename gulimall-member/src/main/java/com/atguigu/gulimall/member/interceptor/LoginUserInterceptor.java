package com.atguigu.gulimall.member.interceptor;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.vo.MemberRespVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author QingnanZhang
 * @creat 2022-07-06 15:34
 **/
@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    @Autowired
    public static ThreadLocal<MemberRespVo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //让支付服务不用登陆
        String uri = request.getRequestURI();
        boolean match = new AntPathMatcher().match("/member/**", uri);
        if(match){
            return true;
        }

        HttpSession session = request.getSession();
        MemberRespVo user = (MemberRespVo) session.getAttribute(AuthServerConstant.LOGIN_USER);
        if(user != null){
            //登录了
            loginUser.set(user);
            return true;
        }else {
            //未登录，去登录
            session.setAttribute("msg", "请先进行登录");
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
    }
}
