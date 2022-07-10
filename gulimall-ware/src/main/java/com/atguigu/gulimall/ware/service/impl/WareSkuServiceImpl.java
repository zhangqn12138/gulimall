package com.atguigu.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.OrderTo;
import com.atguigu.common.to.mq.StockDeatilTo;
import com.atguigu.common.to.mq.StockLockedTo;
import com.atguigu.common.utils.R;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.atguigu.gulimall.ware.entity.WareOrderTaskEntity;
import com.atguigu.gulimall.ware.feign.OrderFeignService;
import com.atguigu.gulimall.ware.feign.ProductFeignService;
import com.atguigu.common.to.SkuHasStockVo;
import com.atguigu.gulimall.ware.service.WareOrderTaskDetailService;
import com.atguigu.gulimall.ware.service.WareOrderTaskService;
import com.atguigu.gulimall.ware.vo.OrderItemVo;
import com.atguigu.gulimall.ware.vo.OrderVo;
import com.atguigu.gulimall.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
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

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private WareOrderTaskService orderTaskService;

    @Autowired
    private WareOrderTaskDetailService orderTaskDetailService;

    @Autowired
    private OrderFeignService orderFeignService;

    @Autowired
    private WareSkuDao wareSkuDao;

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
     * 库存什么时候解锁？
     * 1.系统或者用户取消订单
     * 2.业务调用出现问题
     * @param vo
     * @return
     */
    @Transactional(rollbackFor = NoStockException.class)//默认只要是运行时异常都会回滚
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {

        //生成锁定订单信息
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        orderTaskService.save(taskEntity);

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
                    //TODO 记得改变数据库的表 294
                    //生成锁定库存的详情信息
                    WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity(null, skuId, "", num, taskEntity.getId(), wareId, 1);
                    orderTaskDetailService.save(taskDetailEntity);
                    //给RabbitMQ发送消息
                    StockLockedTo stockLockedTo = new StockLockedTo();
                    stockLockedTo.setId(taskEntity.getId());
                    StockDeatilTo stockDeatilTo = new StockDeatilTo();
                    BeanUtils.copyProperties(taskDetailEntity, stockDeatilTo);
                    stockLockedTo.setDetail(stockDeatilTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", stockLockedTo);
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

    @Override
    public void unlockStock(StockLockedTo to) {
        StockDeatilTo detail = to.getDetail();//锁定库存的详细信息
        Long detailId = detail.getId();
        WareOrderTaskDetailEntity detailEntity = orderTaskDetailService.getById(detailId);
        if(detailEntity != null){
            //说明库存锁定成功
            Long id = to.getId();//库存工作单id
            WareOrderTaskEntity taskEntity = orderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();
            R r = orderFeignService.getOrder(orderSn);
            if(r.getCode() == 0){
                //远程查询订单成功
                OrderVo data = r.getData(new TypeReference<OrderVo>() {
                });
                if(detail.getLockStatus() == 1){
                    //如果库存锁定详情的状态是已锁定，才解锁
                    if(data == null || data.getStatus() == 4){
                        //订单真的存在且未被取消，才解锁
                        unLockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
                    }
                }
            }
        }
    }


    private void unLockStock(Long skuId, Long wareId, Integer skuNum, Long detailId) {
        //先解锁
        wareSkuDao.unLockStock(skuId, wareId, skuNum);
        //后改库存锁定详情状态为已解锁
        WareOrderTaskDetailEntity detailEntity = new WareOrderTaskDetailEntity();
        detailEntity.setId(detailId);
        detailEntity.setLockStatus(2);//将状态变为已解锁
        orderTaskDetailService.updateById(detailEntity);
    }

    //防止订单服务在改订单状态时发生卡顿，从而导致解锁库存时由于订单状态未改而无法解锁，此后消息不会传来，将会一直无法解锁
    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        //查询库存的最新状态，避免重复解锁
        WareOrderTaskEntity task = orderTaskService.getOrderTaskByOrderSn(orderTo.getOrderSn());
        Long taskId = task.getId();
        //按照库存工作单，找到未解锁的库存进行解锁
        List<WareOrderTaskDetailEntity> entities = orderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
                .eq("task_id", taskId).eq("lock_status", 1));
        //TODO 此时有可能已经都被解锁了，entities可能为null，此时应该怎么处理？我加了一个非空判断
        if(entities != null){
            for (WareOrderTaskDetailEntity entity : entities) {
                unLockStock(entity.getSkuId(), entity.getWareId(), entity.getSkuNum(), entity.getId());
            }
        }
    }

    @Data
    class SkuWareHasStock{
        private Long skuId;
        private Integer num;
        private List<Long> wareId;
    }

}