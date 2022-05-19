package com.atguigu.common.valid;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author QingnanZhang
 * @creat 2022-03-17 17:32
 **/
//在pom文件中导入javax.validation-api
@Documented
@Constraint(validatedBy = { ListValueConstraintValidator.class })//指定该注解使用哪个校验器进行校验，如果此处不指定，则在初始化时指定；一个注解可以指定多个校验器，可以根据被标注注解的属性的类型属性自动进行适配
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
public @interface ListValue {
    String message() default "{com.atguigu.common.valid.ListValue.message}";//当校验出错后，默认在哪里取错误信息（默认在ValidationMessages.properties中获取）

    Class<?>[] groups() default { };//默认支持分组功能

    Class<? extends Payload>[] payload() default { };//自定义负载信息

    int[] vals() default { };//自定义该注解的属性
}
