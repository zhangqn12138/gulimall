package com.atguigu.gulimall.ware.service.impl;

import com.atguigu.common.utils.R;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.gulimall.ware.feign.ProductFeignService;
import com.atguigu.common.to.SkuHasStockVo;
import com.atguigu.gulimall.ware.vo.OrderItemVo;
import com.atguigu.gulimall.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.ware.dao.WareSkuDao;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private ProductFeignService productFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if(!StringUtils.isEmpty(skuId)){
            wrapper.eq("sku_id", skuId);
        }

        String wareId = (String) params.get("wareId");
        if(!StringUtils.isEmpty(wareId)){
            wrapper.eq("ware_id", wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("sku_id", skuId).eq("ware_id", wareId);
        List<WareSkuEntity> wareSkuEntities = this.baseMapper.selectList(wrapper);
        if(wareSkuEntities == null || wareSkuEntities.size() == 0){//判断如果没有此库存记录就是新增操作，有才是更新
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStockLocked(0);
            //TODO 远程查询sku_name
            //如果失败，整个事务无需回滚的处理方式
            //1.方法一：自己catch异常
            //TODO 我们还可以使用什么办法让异常出现以后不回滚
            try {
                R info = productFeignService.info(skuId);
                if(info.getCode() == 0){
                    Map<String, Object> skuInfo = (Map<String, Object>) info.get("skuInfo");
                    wareSkuEntity.setSkuName((String) skuInfo.get("skuName"));
                }
            }catch (Exception e){

            }

            this.baseMapper.insert(wareSkuEntity);
        }else{
            this.baseMapper.addStock(skuId, wareId, skuNum);
        }

    }

    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {

        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            //查询当前的skuId的库存量
            Long count = baseMapper.getSkuStock(skuId);
            vo.setSkuId(skuId);
            vo.setHasStock(count == null? false: count > 0);
            return vo;
        }).collect(Collectors.toList());
        return collect;
    }

    /**
     * 为某个订单锁定库存
     * @param vo
     * @return
     */
    @Transactional(rollbackFor = NoStockException.class)//默认只要是运行时异常都会回滚
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        //1.找到每个商品在哪个仓库有库存
        List<OrderItemVo> locks = vo.getLocks();
        List<SkuWareHasStock> wareHasStocks = locks.stream().map((item) -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            List<Long> wareIds = this.baseMapper.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareIds);
            return stock;
        }).collect(Collectors.toList());
        //2.锁定库存
        for (SkuWareHasStock wareHasStock : wareHasStocks) {
            Boolean skuStock = false;
            Long skuId = wareHasStock.getSkuId();
            Integer num = wareHasStock.getNum();
            List<Long> wareIds = wareHasStock.getWareId();
            if(wareIds == null || wareIds.size() == 0){
                //没有仓库有该商品的库存
                throw new NoStockException(skuId);
            }
            for (Long wareId : wareIds) {
                //成功则返回1，否则就是失败
                Long count = this.baseMapper.lockSkuStock(skuId, wareId, num);
                if(count == 1){
                    //锁定成功
                    skuStock = true;
                    break;
                }else{
                    //锁定失败，重试
                }
            }
            if(!skuStock){
                //所有仓库尝试完了都没锁住
                throw new NoStockException(skuId);
            }
        }
        //全部都锁定成功
        return true;
    }

    @Data
    class SkuWareHasStock{
        private Long skuId;
        private Integer num;
        private List<Long> wareId;
    }

}