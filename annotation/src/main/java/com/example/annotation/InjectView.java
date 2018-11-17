package com.example.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created at 2018/11/15 下午7:14.
 *
 * @author yixu.wang
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD})
public @interface InjectView {
    int value();
}
