package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.order.constant.OrderConstant;
import com.atguigu.gulimall.order.enmue.OrderStatusEnum;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.ProductFeignService;
import com.atguigu.gulimall.order.feign.WareFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.to.OrderCreateTo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> confirmThreadLocal = new ThreadLocal<>();

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private WareFeignService wareFeignService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private OrderItemService orderItemService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();

        //解决OpenFeign异步调用请求头丢失的问题
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            //1.远程查询所有的收货地址列表
            //解决OpenFeign异步调用请求头丢失的问题
            RequestContextHolder.setRequestAttributes(requestAttributes);

            List<MemberAddressVo> address = memberFeignService.getAddress(memberRespVo.getId());
            confirmVo.setMemberAddressVos(address);
        }, executor);

        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            //2.远程查询购物车所有选中的购物项
            //解决OpenFeign异步调用请求头丢失的问题
            RequestContextHolder.setRequestAttributes(requestAttributes);

            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(items);
        }, executor).thenRunAsync(() -> {
            //4.设置商品每一个商品的库存信息
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> skuIds = items.stream().map((item) -> {
                return item.getSkuId();
            }).collect(Collectors.toList());
            R r = wareFeignService.getSkusHasStock(skuIds);
            List<SkuStockVo> data = r.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if(data != null){
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }
        });

        //3.查询用户积分
        Integer integration = memberRespVo.getIntegration();
        confirmVo.setIntegration(integration);

        //5.设置防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        confirmVo.setOrderToken(token);
        stringRedisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()
                , token
                , 30
                , TimeUnit.MINUTES);
        CompletableFuture.allOf(getAddressFuture, cartFuture).get();
        return confirmVo;
    }

    @GlobalTransactional//开启全局事务
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        confirmThreadLocal.set(vo);
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        responseVo.setCode(0);
        //1.验证令牌
        String orderToken = vo.getOrderToken();
        //利用lua脚本实现令牌验证和删除的原子性
        String script = "";//返回0失败，返回1成功
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        Long result = stringRedisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class)
                , Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId())
                , orderToken);
        if(result == 0L){
            //令牌验证失败
            responseVo.setCode(1);
            return responseVo;
        }else{
            //令牌验证成功
            //1.创建订单
            OrderCreateTo order = createOrder();
            //2.验价
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();
            if(Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01){
                //3.价格未变化（验价成功），保存订单
                saveOrder(order);
                //4.锁定库存，有异常就回滚订单数据
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> orderItemVos = order.getOrderItems().stream().map((item) -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    orderItemVo.setTitle(item.getSkuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(orderItemVos);
                R r = wareFeignService.orderLockStock(lockVo);
                    if(r.getCode() == 0){
                    //锁定成功
                    responseVo.setOrder(order.getOrder());
                    return responseVo;
                }else{
                    //锁定失败
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
                }
            }else{
                responseVo.setCode(2);
                return responseVo;
            }
        }
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {

        OrderEntity orderEntity = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return orderEntity;
    }

    /**
     * 保存订单数据
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {
        //保存订单数据
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);
        //保存订单项数据
        List<OrderItemEntity> items = order.getOrderItems();
        orderItemService.saveBatch(items);
    }

    private OrderCreateTo createOrder(){
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        //1.生成订单具体信息
        //生成订单号
        String orderSn = IdWorker.getTimeId();
        //构建订单
        OrderEntity orderEntity = buildOrder(orderSn);
        orderCreateTo.setOrder(orderEntity);

        //2.生成订单项信息
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSn);
        orderCreateTo.setOrderItems(orderItemEntities);

        //3.计算并设置订单价格和积分
        computePriceAndScore(orderEntity, orderItemEntities);


        return orderCreateTo;
    }

    private void computePriceAndScore(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {
        BigDecimal total = new BigDecimal("0.0");
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal intergration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");
        BigDecimal gift = new BigDecimal("0.0");
        BigDecimal growth = new BigDecimal("0.0");

        for (OrderItemEntity orderItemEntity : orderItemEntities) {
            coupon = coupon.add(orderItemEntity.getCouponAmount());
            intergration = intergration.add(orderItemEntity.getIntegrationAmount());
            promotion = promotion.add(orderItemEntity.getPromotionAmount());
            total = total.add(orderItemEntity.getRealAmount());
            gift = gift.add(new BigDecimal(orderItemEntity.getGiftIntegration().toString()));
            growth = growth.add(new BigDecimal(orderItemEntity.getGiftGrowth().toString()));
        }
        orderEntity.setTotalAmount(total);
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(intergration);
        orderEntity.setCouponAmount(coupon);
        orderEntity.setIntegration(gift.intValue());
        orderEntity.setGrowth(growth.intValue());
    }

    /**
     * 生成订单信息
     * @param orderSn
     */
    private OrderEntity buildOrder(String orderSn) {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setMemberId(memberRespVo.getId());
        orderEntity.setOrderSn(orderSn);
        //获取收货地址、运费等信息
        OrderSubmitVo submitVo = confirmThreadLocal.get();
        R fare = wareFeignService.getFare(submitVo.getAddrId());
        FareVo fareResp = fare.getData(new TypeReference<FareVo>() {
        });
        orderEntity.setFreightAmount(fareResp.getFare());
        orderEntity.setReceiverCity(fareResp.getAddress().getCity());
        orderEntity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
        orderEntity.setReceiverName(fareResp.getAddress().getName());
        orderEntity.setReceiverPhone(fareResp.getAddress().getPhone());
        orderEntity.setReceiverPostCode(fareResp.getAddress().getPostCode());
        orderEntity.setReceiverProvince(fareResp.getAddress().getProvince());
        orderEntity.setReceiverRegion(fareResp.getAddress().getRegion());
        //设置订单状态信息等信息
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setAutoConfirmDay(7);
        orderEntity.setDeleteStatus(0);
        return orderEntity;
    }

    /**
     * 构建所有订单项数据
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //最后确定每个购物项的价格
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if(currentUserCartItems != null && currentUserCartItems.size() > 0){
            List<OrderItemEntity> itemEntities = currentUserCartItems.stream().map((cartItem) -> {
                OrderItemEntity orderItemEntity = buildOrderItem(cartItem);
                orderItemEntity.setOrderSn(orderSn);
                return orderItemEntity;
            }).collect(Collectors.toList());
            return itemEntities;
        }
        return null;
    }

    /**
     * 构建单个订单项数据
     * @param cartItem
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        //1.订单信息：订单号
        //2.spu信息
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo data = r.getData(new TypeReference<SpuInfoVo>() {
        });
        orderItemEntity.setSpuId(data.getId());
        orderItemEntity.setSpuBrand(data.getBrandId().toString());
        orderItemEntity.setSpuName(data.getSpuName());
        orderItemEntity.setCategoryId(data.getCatalogId());
        //3.sku信息
        orderItemEntity.setSkuId(cartItem.getSkuId());
        orderItemEntity.setSkuName(cartItem.getTitle());
        orderItemEntity.setSkuPic(cartItem.getImage());
        orderItemEntity.setSkuPrice(cartItem.getPrice());
        orderItemEntity.setSkuQuantity(cartItem.getCount());
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttrValues(), ";");
        orderItemEntity.setSkuAttrsVals(skuAttr);
        //4.优惠信息：无
        //5.积分信息
        orderItemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        orderItemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        //6.价格信息
        orderItemEntity.setPromotionAmount(new BigDecimal("0"));
        orderItemEntity.setCouponAmount(new BigDecimal("0"));
        orderItemEntity.setIntegrationAmount(new BigDecimal("0"));
        BigDecimal orign = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()));
        orign.subtract(orderItemEntity.getCouponAmount())
            .subtract(orderItemEntity.getPromotionAmount())
            .subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(orign);
        return orderItemEntity;
    }

}