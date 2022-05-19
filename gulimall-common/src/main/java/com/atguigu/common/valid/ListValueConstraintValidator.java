package com.atguigu.common.valid;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;

/**
 * @author QingnanZhang
 * @creat 2022-03-17 17:41
 **/
//必须继承ConstraintValidator接口，该接口有两个泛型，前者指定注解，后者指定要校验的数据的类型
public class ListValueConstraintValidator implements ConstraintValidator<ListValue, Integer> {

    private Set<Integer> set = new HashSet<>();

    //初始化方法，constraintAnnotation接收了注解中的详细信息
    @Override
    public void initialize(ListValue constraintAnnotation) {
        int[] vals = constraintAnnotation.vals();
        for (int val : vals) {//最好进行非空判断，防止遍历没数据
            set.add(val);
        }
    }

    //判断是否校验成功

    /**
     *
     * @param integer 被注解的属性获得的值
     * @param constraintValidatorContext 上下文环境信息
     * @return
     */
    @Override
    public boolean isValid(Integer integer, ConstraintValidatorContext constraintValidatorContext) {
        return set.contains(integer);
    }
}
