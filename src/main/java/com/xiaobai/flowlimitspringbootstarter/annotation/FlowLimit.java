package com.xiaobai.flowlimitspringbootstarter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 流控注解
 *
 * @author yin_zhj
 * @date 2020/6/12
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FlowLimit {
    /**
     * 生成令牌时间间隔
     * @return
     */
    long interval() default 10000;

    /**
     * 最大令牌数
     * @return
     */
    long maxTokenNums() default 500;

    /**
     * 流控返回信息
     * @return
     */
    String message() default "Flow limit,please try later";
}
