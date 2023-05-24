package com.xxlgroup.annotation;

import com.xxlgroup.enums.RepeatSubmitEnum;

import java.lang.annotation.*;

/**
 *
 * 防表单重复提交
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RepeatSubmit {

    /**
     * 防重提交，支持两种，一个是方法参数，一个是令牌
     */
    enum Type { PARAM, TOKEN }
    /**
     * 默认防重提交，是方法参数
     * @return
     */
    RepeatSubmitEnum limitType() default RepeatSubmitEnum.PARAM;

    /**
     * 加锁过期时间，默认是3秒
     * @return
     */
    long lockTime() default 3;
}
