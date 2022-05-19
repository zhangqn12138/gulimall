package com.atguigu.gulimall.ware.vo;

import lombok.Data;

/**
 * @author QingnanZhang
 * @creat 2022-04-06 16:16
 **/
@Data
public class PurchaseItemDoneVo {
    private Long itemId;
    private Integer status;
    private String reason;
}
