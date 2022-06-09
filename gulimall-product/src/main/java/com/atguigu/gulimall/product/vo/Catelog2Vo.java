package com.atguigu.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sun.plugin.perf.PluginRollup;

import java.util.List;

/**
 * @author QingnanZhang
 * @creat 2022-06-05 10:02
 **/
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Catelog2Vo {//二级分类VO
    private String catalog1Id;//一级父分类id
    private List<Catelog3Vo> catalog3List;//三级子分类数据
    private String id;//当前二级分类id
    private String name;//当前二级分类名称

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class Catelog3Vo {
        private String catalog2Id;//二级父分类id
        private String id;//当前三级分类id
        private String name;//当前三级分类名称
    }
}
