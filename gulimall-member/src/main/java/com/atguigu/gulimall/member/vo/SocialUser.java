package com.atguigu.gulimall.member.vo;

import lombok.Data;

/**
 * @author QingnanZhang
 * @creat 2022-06-23 14:05
 **/
@Data
public class SocialUser {
    private String access_token;
    private String remind_in;
    private long expires_in;
    private String uid;
    private String isRealName;
}
