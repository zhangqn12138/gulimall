package com.atguigu.gulimall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gulimall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private  String app_id = "2021000121620771";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private  String merchant_private_key = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCl13AWbmZ7uLPb46WYaSjK8r5WTXffXyNUhGndbmqCgZERQUMIR2iJoS8qdXzsxzodg10EREgP+FhWbEQ6T8r0B4zCEZQNBo1CN1D4gLrcgM7OxRkXeXa9awVf81NEbAEve2D2qRFu+H+s0zN3cjtkzfXWPgpfzX4clu36kmmlisABisHfxrI8clK6N6qLw8onBLH9DCncHGPw6dkTMD/Kr8rFIFHMy+6CT/PHirPWssDMUvf6S7xX9BtxQA/1CQN0aI60yxd+z/VCy8EN6B8t0iD1G0X/bPPRb7xVG2F5Dw49Sn3LpUJuv7ve24mrm1vFFcfbNEUb6z0kIBbEaDiZAgMBAAECggEAepFC8hEeBQp5Q2Z4GLB/yZ6mNbqDgb8aqXhDlbfpWBLSWGJXXGMKS82owkSa05Rayz0vHdwpNkRhZV4W/tHCOaDq0vOS7B2PgQTI/yCE+jroncDlzoeizlOuYT/Apqz8I0+YVjSWAeH+2ogtXXY7l0DxFCo+GJgTQKX7tFlJY2J6GeT0GxNKC08EVnYJhKv7HKVlEWStIYjMtsfBkXFru7VHtegwWhw9zfu38R9xKg3IcE4G37AUVLBXSM6aUoJV+77wCg+p22lymGBQEWi/PUKA7lgXDuQZ4nI3TT2FnIhpyVsPM5s/8iGgXig7xwuwJjQIlJYty+9QJL0n1kh6QQKBgQDmRfn4riS3yHfHEtQAxjKPrbzjyY80GRnGaDRMFNW+vIwKErYSCpgHuV4Q3CoMqKA2wjvHdIC4HOc4LNhpcqeSQ7DWtGpceVX788An9c6aNosp52duaQvHEYIdu8koeEKXMCG2JLbneGUgf0JJQ3ReBIAYQiT5RljaPBr/+vKw/QKBgQC4XqeQz1Qxjf9+lKqtSbwapxy137eSTSiNV42c7YJe0k3uxKOtk1RPwsjiHJOc1X71s3YuhrqRiPk/ZRm6aySS4OeFNvzGYRBycFPV4y2/ibFQuv4Kap8dXdUAdwEm38lS6VzTqtmz4BV2L+EqADl5FKQ80YjjbWccFSu7daHWzQKBgQC9Wn+CaZjR86vi7G2esVw0X9Z6rWzvl1BloZyXj25waNTzF43WahW4DEr4rEJ7pFISlUfY7MlIHKRZwi63D0wSb01EUJlBr5jwFPFHKs8Yao+nBp074m+H47LORnPUrod7kV23TJuG3a6yuVLFsg1HnoXC7OlAfgNkeZZuNmpI9QKBgA2ofXNd2JffNBk1fDtgJf43eQoEK7471xA5dzUz5x+NJtbUXGfEbU+HV4hgo5LPwhsDk3K46mNTqVtH6xMDUu9Kl/wnaaxsjmJb7en/bWkuEyOu5pjw8x6iz9+78SrnEywAO682jfAkXpKl02FgKOHOZKhYwXJPrX7IFrhJa5bVAoGBAMDVOcyEtAJtjzr3Vz8YXAjvzT8DizV4vRIfGLaFZ8qfpkVPuPausNIWAD4RsWBc9q87Y3YITAZNaX9Gwlv6jzDcpPliqshuHcEN4s5QoffZLJhIG3DxOqxZlqnVDOH9gTmkIIK/td8IHA+56aVKi1455VQnt/RVMQIJpZHwVrK/";
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private  String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAll20ezdX2gGNDBe38nJYeYrD+uHbS6ZnLzUcnaNcHvNcfTUlkJtXvdQB3mRgpSVT8uBdAAAQ9dg1XTeR+30oRzxl93C1/5dkJueCNEzWUIpGODzk1RGHFs+kp/o1gKc2KbHfb5iA7S01u1v5lw3AVbsWmNM9roaNlVkOE19fqNwaYLESFBKJ6RcukryKw1QQZ/P+RpyglGVpCfh58vEpc0BjlaPh5JHNwiRmcYrcoQY9t8yMoD6gbJSdPoiONEMrgX3QuAr/HkjnMPM96qvxiDi6Uy0BoXC4p23PquNeCHmpoWfSJJYiew9FOltzEqfB1HEiht02Mcu1SHkUX90NRQIDAQAB";
    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private  String notify_url = "https://f3901m1439.zicp.fun/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private  String return_url = "http://member.gulimall.com/memberOrder.html";

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    //收单时间
    private String timeout = "30m";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private  String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"timeout_express\":\""+ timeout +"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
