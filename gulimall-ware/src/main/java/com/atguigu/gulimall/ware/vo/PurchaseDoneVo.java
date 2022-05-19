package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author QingnanZhang
 * @creat 2022-04-06 16:17
 **/
@Data
public class PurchaseDoneVo {
    @NotNull
    private Long id;//采购单id
    private List<PurchaseItemDoneVo> items;//采购项
}
