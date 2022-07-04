package com.atguigu.gulimall.cart.vo;

import lombok.Data;
import lombok.ToString;

/**
 * @author QingnanZhang
 * @creat 2022-06-29 21:19
 **/
@ToString
@Data
public class UserInfoTo {
    private Long userId;
    private String userKey;
    //用于判断Cookie是否已经存在临时用户
    private Boolean tempUser = false;
}
