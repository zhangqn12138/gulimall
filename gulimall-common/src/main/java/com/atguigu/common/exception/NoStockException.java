package com.atguigu.common.exception;
/*
@author zqn
@create 2022-07-07 21:21
*/

public class NoStockException extends RuntimeException {

    private Long skuId;

    public NoStockException(String msg){
        super(msg);
    }

    public NoStockException(Long skuId){
        super("商品[" + skuId + "]没有足够的库存");
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Long getSkuId() {
        return skuId;
    }
}
