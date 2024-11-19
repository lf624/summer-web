package com.learn.summer.annotation;

import com.learn.summer.web.utils.WebUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestParam {
    String value();

    String defaultValue() default WebUtils.DEFAULT_PARAM_VALUE;
}
