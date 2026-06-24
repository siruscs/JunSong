package com.junsong.common.sensitive.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.junsong.common.sensitive.config.SensitiveJsonSerializer;
import com.junsong.common.sensitive.enums.DesensitizedType;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * 数据脱敏注解
 *
 * @author junsong
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveJsonSerializer.class)
public @interface Sensitive
{
    DesensitizedType desensitizedType();
}