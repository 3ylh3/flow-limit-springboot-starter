package com.xiaobai.flowlimitspringbootstarter.aspect;

import com.xiaobai.flowlimitspringbootstarter.annotation.FlowLimit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流控切面类
 *
 * @author yin_zhj
 * @date 2020/6/12
 */
@Aspect
public class FlowLimitAspect {
    private Map<String, Long> millisMap = new ConcurrentHashMap<>();
    private Map<String, Long> tokenMap = new ConcurrentHashMap<>();

    @Pointcut("@annotation(com.xiaobai.flowlimitspringbootstarter.annotation.FlowLimit)")
    public void flowLimit() {
    }

    @Around("flowLimit()")
    public String excute(ProceedingJoinPoint pjp) {
        Class<?> clzz = pjp.getTarget().getClass();
        MethodSignature methodSignature = (MethodSignature)pjp.getSignature();
        try {
            Method method = clzz.getDeclaredMethod(methodSignature.getName(), methodSignature.getParameterTypes());
            RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
            if(null == requestMapping) {
                return "Annotation '@RequestMapping' not find";
            }
            FlowLimit flowLimit = method.getAnnotation(FlowLimit.class);
            long interval = flowLimit.interval();
            long maxTokenNums = flowLimit.maxTokenNums();
            String path = requestMapping.value()[0];
            long curMillis = System.currentTimeMillis();
            Long tokenNums = tokenMap.get(path);
            if(null == tokenNums) {
                tokenNums = maxTokenNums;
            }
            Long millis = millisMap.get(path);
            if (null != millis) {
                long nums = (curMillis - millis) / interval;
                tokenNums += nums;
                if(tokenNums > maxTokenNums) {
                    tokenNums = maxTokenNums;
                }
            }
            millisMap.put(path, curMillis);
            if (tokenNums > 0) {
                tokenNums--;
                tokenMap.put(path, tokenNums);
                return (String) pjp.proceed();
            } else {
                return flowLimit.message();
            }
        } catch (Throwable t) {
            return t.toString();
        }
    }
}
