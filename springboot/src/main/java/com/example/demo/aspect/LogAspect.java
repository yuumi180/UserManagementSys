package com.example.demo.aspect;

import com.example.demo.annotation.Log;
import com.example.demo.entity.OperationLog;
import com.example.demo.mapper.OperationLogMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Date;

@Aspect
@Component
public class LogAspect {

    @Resource
    private OperationLogMapper operationLogMapper;

    private ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Pointcut("@annotation(com.example.demo.annotation.Log)")
    public void logPointcut() {
    }

    @Before("logPointcut()")
    public void before(JoinPoint joinPoint) {
        startTime.set(System.currentTimeMillis());
    }

    @AfterReturning(pointcut = "logPointcut()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Log log = method.getAnnotation(Log.class);

            OperationLog operationLog = new OperationLog();

            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                operationLog.setUrl(request.getRequestURI());
                operationLog.setMethod(request.getMethod());

                // 获取参数
                String params = java.util.Arrays.toString(joinPoint.getArgs());
                if (params.length() > 1000) {
                    params = params.substring(0, 1000) + "...";
                }
                operationLog.setParams(params);
            }

            operationLog.setOperation(log.value());
            operationLog.setResult(result.toString());
            operationLog.setCreateTime(new Date());

            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
