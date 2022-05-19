package com.atguigu.gulimall.product.vo;

import lombok.Data;

/**
 * @author QingnanZhang
 * @creat 2022-04-01 17:02
 **/
@Data
public class AttrRespVo extends AttrVo {
   private String catelogName;
   private String groupName;

   private Long[] catelogPath;

}
