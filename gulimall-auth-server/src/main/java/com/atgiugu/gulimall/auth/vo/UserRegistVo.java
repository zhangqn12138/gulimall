package com.atgiugu.gulimall.auth.vo;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;


@Data
public class UserRegistVo {

    @NotEmpty(message = "用户名不能为空")
    @Min(value = 6, message = "用户名最少6位")
    @Max(value = 18,message = "用户名最长18位")
    private String userName;

    @NotEmpty(message = "密码必须填写")
    @Min(value = 6, message = "密码最少6位")
    @Max(value = 18,message = "密码最长18位")
    private String password;

    @NotEmpty(message = "手机号不能为空")
    @Pattern(regexp = "^[1]([3-9])[0-9]{9}$", message = "手机号格式不正确")
    private String phone;

    @NotEmpty(message = "验证码不能为空")
    private String code;

}
