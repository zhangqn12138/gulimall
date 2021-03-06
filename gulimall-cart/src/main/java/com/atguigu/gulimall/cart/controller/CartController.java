package com.atguigu.gulimall.cart.controller;

import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.CartItemVo;
import com.atguigu.gulimall.cart.vo.CartVo;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author QingnanZhang
 * @creat 2022-06-29 17:24
 **/
@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("/currentUserCartItems")
    @ResponseBody
    public List<CartItemVo> getCurrentUserCartItems(){
        return cartService.getUserCartItems();
    }

    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId){
        cartService.deleteItem(skuId);
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num){
        cartService.changeItemCount(skuId, num);
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId, @RequestParam("check") Integer check){
        cartService.checkItem(skuId, check);
        return "redirect:http://cart.gulimall.com/cart.html";
    }


    @GetMapping("/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {
        CartVo cartvo = cartService.getCart();
        model.addAttribute("cart", cartvo);
        return "cartList";
    }

    /**
     * ????????????????????????
     * ra.addFlashAttribute ==> ???????????????session???????????????????????????????????????????????????
     * ra.addAttribute ==> ??????????????????url??????
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num, RedirectAttributes ra) throws ExecutionException, InterruptedException {
        CartItemVo cartItemVo = cartService.addToCart(skuId, num);
        //?????????????????????????????????url?????????
        ra.addAttribute("skuId",skuId);
        return "redirect:http://cart.gulimall.com/addToCartSuccess.html";
    }

    /**
     * ???????????????????????????????????????
     * @param skuId
     * @param model
     * @return
     */
    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccess(@RequestParam("skuId") Long skuId, Model model){
        CartItemVo item = cartService.getCartItem(skuId);
        model.addAttribute("item", item);
        return "success";
    }
}
