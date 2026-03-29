package com.example.demo.aspect;

import com.example.demo.annotation.RateLimit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class RateLimitAspect {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        // 获取请求 IP
        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes()).getRequest();
        String ip = request.getRemoteAddr();
        String method = pjp.getSignature().getName();

        // 构建 Redis key
        String key = "rate_limit:" + ip + ":" + method;

        // 使用 Redis INCR 实现计数
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == 1) {
            // 第一次请求，设置过期时间
            redisTemplate.expire(key, rateLimit.timeWindow(), TimeUnit.SECONDS);
        }

        // 检查是否超过限制
        if (count > rateLimit.maxRequests()) {
            throw new RuntimeException("请求过于频繁，请稍后再试");
        }

        return pjp.proceed();
    }
}
