package com.atguigu.common.to.mq;

import lombok.Data;

/*
@author zqn
@create 2022-07-09 18:04
*/
@Data
public class StockLockedTo {

    private Long id;//库存工作单id

    private StockDeatilTo detail;//工作单详情
}
