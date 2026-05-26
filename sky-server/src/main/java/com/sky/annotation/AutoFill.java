package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//自定义注解，用于标识方法需要进行字段自动填充
@Target(ElementType.METHOD)//表示注解只能用在方法上
@Retention(RetentionPolicy.RUNTIME)//表示注解在运行时保留
public @interface AutoFill {
    OperationType value();//表示数据库操作类型
}
