package com.atguigu.gulimall.member.exception;

/**
 * @author QingnanZhang
 * @creat 2022-06-21 16:34
 **/
public class UserNameExistException extends RuntimeException{
    public UserNameExistException(){
        super("用户名已存在");
    }
}
