package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

/**
 * @author QingnanZhang
 * @creat 2022-04-06 11:56
 **/
@Data
public class MergeVo {
    private Long purchaseId;//整单id
    private List<Long> items;//合并项id
}
