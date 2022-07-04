package com.atguigu.gulimall.member.exception;

/**
 * @author QingnanZhang
 * @creat 2022-06-21 16:34
 **/
public class PhoneExistException extends RuntimeException {
    public PhoneExistException(){
        super("手机号已存在");
    }
}
