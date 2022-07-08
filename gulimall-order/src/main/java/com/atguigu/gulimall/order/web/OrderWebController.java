package com.atguigu.gulimall.order.web;

import com.atguigu.common.exception.NoStockException;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

/**
 * @author QingnanZhang
 * @creat 2022-07-06 15:33
 **/
@Controller
public class OrderWebController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = orderService.confirmOrder();
        model.addAttribute("orderConfirmData", confirmVo);
        return "confirm";
    }

    /**
     * 下单功能
     * @return
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes){
        try {
            SubmitOrderResponseVo responseVo = orderService.submitOrder(vo);
            if (responseVo.getCode() == 0) {
                //成功：去支付选择页
                model.addAttribute("submitOrderResp", responseVo);
                return "pay";
            } else {
                //失败：重回订单页重新计算
                String msg = "下单失败";
                switch (responseVo.getCode()) {
                    case 1:
                        msg += "订单信息过期，请刷新再次提交（令牌校验失败）";
                        break;
                    case 2:
                        msg += "订单商品价格发生变化，请确认后再次提交";
                        break;
//                    case 3:
//                        msg += "库存锁定失败，商品库存不足";
//                        break;
                }
                redirectAttributes.addFlashAttribute("msg", msg);
                return "redirect:http://order.gulimall.com/toTrade";
            }
        } catch (Exception e){
            if(e instanceof NoStockException){
                String message = ((NoStockException) e).getMessage();
                redirectAttributes.addFlashAttribute("msg", message);
            }
            return "redirect:http://order.gulimall.com/toTrade";
        }
    }
}
