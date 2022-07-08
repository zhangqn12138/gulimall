package com.atguigu.gulimall.ware.vo;
/*
@author zqn
@create 2022-07-07 14:02
*/

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FareVo {
    private MemberAddressVo address;
    private BigDecimal fare;

}
