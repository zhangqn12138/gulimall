package com.atguigu.gulimall.product.exception;

import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author QingnanZhang
 * @creat 2022-03-17 16:17
 **/
@Slf4j
//可以使用SpringMvc所提供的@ControllerAdvice，通过“basePackages”能够说明处理哪些路径下的异常。
//@ControllerAdvice(basePackages = "com.atguigu.gulimall.product.controller")
//@ResponseBody
@RestControllerAdvice(basePackages = "com.atguigu.gulimall.product.controller")//此注解可以替代上面的两个
public class GulimallExceptionControllerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)//表明该方法处理哪些异常
    public R handleValidException(MethodArgumentNotValidException exception){
        BindingResult result = exception.getBindingResult();

        Map<String, String> map = new HashMap<>();
        //1.获取错误的校验结果
        result.getFieldErrors().forEach((item) -> {
            //获取错误信息（我们自己在品牌类注解中定义的）
            String message = item.getDefaultMessage();
            //获取发生错误的字段
            String field = item.getField();
            map.put(field, message);
        });

        log.error("数据校验出现问题{}，异常类型{}", exception.getMessage(), exception.getClass());

//        return R.error(400, "数据校验出现问题").put("data", map);
        return R.error(BizCodeEnume.VAILD_EXCEPTION.getCode(), BizCodeEnume.VAILD_EXCEPTION.getMsg()).put("data", map);
    }

    @ExceptionHandler(value = Throwable.class)//如果不能精确匹配到异常，则找更大范围可匹配的
    public R handleException(Throwable throwable){
        log.error("数据校验出现问题{}，异常类型{}", throwable.getMessage(), throwable.getClass());
//        return R.error(400, "未知异常");
        return R.error(BizCodeEnume.UNKNOW_EXCEPTION.getCode(), BizCodeEnume.UNKNOW_EXCEPTION.getMsg());
    }
}
