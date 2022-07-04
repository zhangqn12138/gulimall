package com.atguigu.gulimall.cart.interceptor;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.constant.CartConstant;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * @author QingnanZhang
 * @creat 2022-06-29 21:13
 **/
public class CartInterceptor implements HandlerInterceptor {

    //利用ThreadLocal做数据共享
    public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();

    /**
     * 在目标方法执行之前进行拦截，用于判断用户的登陆状态，并进行相关操作
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserInfoTo userInfoTo = new UserInfoTo();
        //判断用户是否登录
        HttpSession session = request.getSession();
        MemberRespVo memberRespVo = (MemberRespVo) session.getAttribute(AuthServerConstant.LOGIN_USER);
        if(memberRespVo != null){
            //用户登录了
            userInfoTo.setUserId(memberRespVo.getId());
        }
        //判断是否是第一次访问
        Cookie[] cookies = request.getCookies();
        if(cookies != null && cookies.length > 0){
            for (Cookie cookie : cookies) {
                String name = cookie.getName();
                if(name.equals(CartConstant.TEMP_USER_COOKIE_NAME)){
                    //不是第一次登陆
                    userInfoTo.setUserKey(cookie.getValue());
                }
            }
        }
        if(StringUtils.isEmpty(userInfoTo.getUserKey())){
            //第一次登录，分配一个临时用户
            String uuid = UUID.randomUUID().toString();
            userInfoTo.setUserKey(uuid);
            userInfoTo.setTempUser(true);
        }

        //封装共享信息
        threadLocal.set(userInfoTo);

        return true;
    }

    /**
     * 在目标方法执行之后进行拦截，让浏览器保存cookie，cookie中用于存放临时用户信息
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        UserInfoTo userInfoTo = threadLocal.get();
        if(!userInfoTo.getTempUser()){
            //如果不存在临时用户信息，才放Cookie
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            cookie.setDomain("gulimall.com");
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIMEOUT);
            response.addCookie(cookie);
        }
    }
}
