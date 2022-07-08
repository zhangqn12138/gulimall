package com.atguigu.gulimall.ware.vo;
/*
@author zqn
@create 2022-07-07 17:51
*/

import lombok.Data;

@Data
public class LockStockResult {
    private Long skuId;
    private Integer num;
    private Boolean locked;
}
