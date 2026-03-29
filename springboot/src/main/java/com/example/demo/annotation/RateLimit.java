package com.example.demo.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    int maxRequests() default 10;  // 最大请求数
    int timeWindow() default 60;   // 时间窗口（秒）
}
