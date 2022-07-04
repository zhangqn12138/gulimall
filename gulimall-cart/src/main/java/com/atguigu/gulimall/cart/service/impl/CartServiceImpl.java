package com.atguigu.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.cart.feign.ProductFeignService;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.CartItemVo;
import com.atguigu.gulimall.cart.vo.CartVo;
import com.atguigu.gulimall.cart.vo.SkuInfoVo;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import jdk.nashorn.internal.ir.CatchNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author QingnanZhang
 * @creat 2022-06-29 17:22
 **/
@Slf4j
@Service
public class CartServiceImpl implements CartService {

    private final String CART_PREFIX = "gulimall:cart:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    @Override
    public CartItemVo addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String result = (String) cartOps.get(skuId.toString());
        if(StringUtils.isEmpty(result)){
            //购物车无此商品，添加新商品
            CartItemVo cartItemVo = new CartItemVo();
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                //1.远程查询需要添加的商品信息
                R skuInfo = productFeignService.getSkuInfo(skuId);
                SkuInfoVo data = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });
                cartItemVo.setCheck(true);
                cartItemVo.setCount(num);
                cartItemVo.setImage(data.getSkuDefaultImg());
                cartItemVo.setTitle(data.getSkuTitle());
                cartItemVo.setSkuId(skuId);
                cartItemVo.setPrice(data.getPrice());
            },executor);
            //2.远程调用sku的组合信息
            CompletableFuture<Void> getSkuSaleAttrValuesTask = CompletableFuture.runAsync(() -> {
                List<String> skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
                cartItemVo.setSkuAttrValues(skuSaleAttrValues);
            }, executor);
            //3.保存数据并返回
            CompletableFuture.allOf(getSkuInfoTask, getSkuSaleAttrValuesTask).get();
            String jsonString = JSON.toJSONString(cartItemVo);
            cartOps.put(skuId.toString(), jsonString);
            return cartItemVo;
        }else{
            //购物车有此商品，修改商品数目
            CartItemVo cartItemVo = JSON.parseObject(result, CartItemVo.class);
            cartItemVo.setCount(cartItemVo.getCount() + num);
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItemVo));
            return cartItemVo;
        }

    }

    @Override
    public CartItemVo getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String str = (String) cartOps.get(skuId.toString());
        CartItemVo item = JSON.parseObject(str, CartItemVo.class);
        return item;
    }

    @Override
    public CartVo getCart() throws ExecutionException, InterruptedException {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        CartVo cartVo = new CartVo();
        if(userInfoTo.getUserId() != null){
            //登录状态
            //1.如果临时购物车的数据未进行合并，则合并临时购物车到用户购物车，并清空临时购物车
            //1.1获取临时购物车数据
            String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItemVo> tempCartItems = getCartItems(tempCartKey);
            if(tempCartItems != null){
                //1.2临时购物车有数据则合并
                for (CartItemVo tempCartItem : tempCartItems) {
                    addToCart(tempCartItem.getSkuId(), tempCartItem.getCount());
                }
                //1.3清除临时购物车
                clearCart(tempCartKey);
            }
            //1.4获取最终的购物车数据
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            List<CartItemVo> cartItems = getCartItems(cartKey);
            cartVo.setItems(cartItems);
            return cartVo;
        }else{
            //未登录状态
            //获取临时购物车及其所有数据
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItemVo> cartItems = getCartItems(cartKey);
            cartVo.setItems(cartItems);
            return cartVo;
        }
    }

    /**
     * 获取到要操作的购物车（登陆购物车还是临时购物车？那个用户或者临时用户的购物车）
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps(){
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        if(userInfoTo.getUserId() != null){
            //登陆了
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        }else{
            //未登录
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }
        BoundHashOperations<String, Object, Object> operations = stringRedisTemplate.boundHashOps(cartKey);
        return operations;
    }

    private List<CartItemVo> getCartItems(String cartKey){
        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();//获取所有该购物车的商品（内部map的值）
        if(values != null && values.size() > 0) {
            List<CartItemVo> collect = values.stream().map((obj) -> {
                String str = (String) obj;
                CartItemVo cartItemVo = JSON.parseObject(str, CartItemVo.class);
                return cartItemVo;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    @Override
    public void clearCart(String cartKey){
        stringRedisTemplate.delete(cartKey);
    }

    @Override
    public void checkItem(Long skuId, Integer check) {
        CartItemVo cartItem = getCartItem(skuId);
        cartItem.setCheck(check == 1? true: false);
        String jsonString = JSON.toJSONString(cartItem);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), jsonString);
    }

    @Override
    public void changeItemCount(Long skuId, Integer num) {
        CartItemVo cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        String jsonString = JSON.toJSONString(cartItem);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), jsonString);
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

}
