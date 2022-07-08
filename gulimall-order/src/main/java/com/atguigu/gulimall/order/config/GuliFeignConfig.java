package com.atguigu.gulimall.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author QingnanZhang
 * @creat 2022-07-06 17:31
 **/
@Configuration
public class GuliFeignConfig {
    /**
     * 解决OpenFeign远程调用丢失请求头信息的问题
     * @return
     */
    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor(){
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                //1.RequstContextHolder拿到刚进来的请求
                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if(requestAttributes != null){
                    HttpServletRequest request = requestAttributes.getRequest();//老请求
                    if(request != null){
                        //2.同步请求头的Cookie数据
                        String cookie = request.getHeader("Cookie");
                        //3.给新请求同步老请求的Cookie数据
                        requestTemplate.header("Cookie", cookie);
                    }
                }
            }
        };
    }
}
