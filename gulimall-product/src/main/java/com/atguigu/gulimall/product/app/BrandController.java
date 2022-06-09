package com.atguigu.gulimall.product.app;

import java.util.Arrays;
import java.util.Map;

import com.atguigu.common.valid.AddGroup;
import com.atguigu.common.valid.UpdateGroup;
import com.atguigu.common.valid.UpdateStatusGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;


/**
 * 品牌
 *
 * @author zhangqingnan
 * @email zhangqingnan@gmail.com
 * @date 2022-02-26 15:48:45
 *
 * JSR303：一种java定义的校验规则
 *  ① 给Bean添加校验注解javax.validation.constraints，并定义自己的message
 *  ② 开启校验功能@Valid：开启后，若校验错误则有默认提示
 *  ③ 给校验的bean后紧跟一个BindingResult（必须紧跟），就可以获取到校验的结果
 *  ④ 分组校验（多场景的复杂校验）
 *      1>给属性的校验注解中添加分组属性groups，groups属性的值是一个数组，且数组内的内容是接口，它表明什么组别需要进行校验
 *      2>在controller中，用注解@Validated({组别})来替换@Valid，指定该方法的校验是那个组别的
 *      3>没有指定分组的校验注解，在分组校验（@Validated注解中指明了组别）的情况下不起作用，只有在不分组（@Validated注解中未指明组别）的情况下才有作用
 *  ⑤ 自定义校验：
 *      1>编写一个自定义的校验注解
 *      2>编写一个自定义的校验器
 *      3>将自定义的校验注解和自定义的校验器关联起来
 * 统一的异常处理
 * @ControllerAdvice
 *  ① 编写异常处理类，使用@ControllerAdvice
 *  ② 使用@ExceptionHandle，标注可以处理的异常
 */
@RestController
@RequestMapping("product/brand")
public class BrandController {
    @Autowired
    private BrandService brandService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:brand:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = brandService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{brandId}")
    //@RequiresPermissions("product:brand:info")
    public R info(@PathVariable("brandId") Long brandId){
		BrandEntity brand = brandService.getById(brandId);

        return R.ok().put("brand", brand);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:brand:save")
    //添加注解开启校验功能
    public R save(@Validated({AddGroup.class}) @RequestBody BrandEntity brand/*, BindingResult result*/){
        //给校验的Bean后，紧跟一个BindResult，就可以获取到校验的结果。拿到校验的结果，就可以自定义的封装。
//        if(result.hasErrors()){//是否校验出错？
//            Map<String, String> map = new HashMap<>();
//            //1.获取错误的校验结果
//            result.getFieldErrors().forEach((item) -> {
//                //获取错误信息（我们自己在品牌类注解中定义的）
//                String message = item.getDefaultMessage();
//                //获取发生错误的字段
//                String field = item.getField();
//                map.put(field, message);
//            });
//            return R.error(400, "提交的数据不合法").put("data", map);
//        }else{
//            brandService.save(brand);
//        }

        //如果检测到异常，但是未处理，默认就会抛出
        brandService.save(brand);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:brand:update")
    public R update(@Validated({UpdateGroup.class})@RequestBody BrandEntity brand){
        //必须进行冗余更新来保证数据的一致性
        brandService.updateDetail(brand);

        return R.ok();
    }

    /**
     * 修改状态
     */
    @RequestMapping("/update/status")
    //@RequiresPermissions("product:brand:update")
    public R updateStatus(@Validated({UpdateStatusGroup.class})@RequestBody BrandEntity brand){
        brandService.updateById(brand);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:brand:delete")
    public R delete(@RequestBody Long[] brandIds){
		brandService.removeByIds(Arrays.asList(brandIds));

        return R.ok();
    }

}
