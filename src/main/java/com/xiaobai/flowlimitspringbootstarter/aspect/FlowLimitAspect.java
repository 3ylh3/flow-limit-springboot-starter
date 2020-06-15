package com.xiaobai.flowlimitspringbootstarter.aspect;

import com.xiaobai.flowlimitspringbootstarter.annotation.FlowLimit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
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
    private String distributed;

    private Map<String, Long> millisMap = new ConcurrentHashMap<>();
    private Map<String, Long> tokenMap = new ConcurrentHashMap<>();
    private static final String MILLIS = "millis";
    private static final String TOKEN = "token";
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public FlowLimitAspect(String distributed) {
        this.distributed = distributed;
    }

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
            Long tokenNums = null;
            Long millis = null;
            if(distributed.equals("false")) {
                tokenNums = tokenMap.get(path);
                millis = millisMap.get(path);
            } else {
                String tokenStr = (String)redisTemplate.opsForHash().get(path, TOKEN);
                if(null != tokenStr) {
                    tokenNums = Long.valueOf(tokenStr);
                }
                String millisStr = (String)redisTemplate.opsForHash().get(path, MILLIS);
                if(null != millisStr) {
                    millis = Long.valueOf(millisStr);
                }
            }
            if(null == tokenNums) {
                tokenNums = maxTokenNums;
            }
            if (null != millis) {
                long nums = (curMillis - millis) / interval;
                tokenNums += nums;
                if(tokenNums > maxTokenNums) {
                    tokenNums = maxTokenNums;
                }
            }
            if(distributed.equals("false")) {
                millisMap.put(path, curMillis);
            } else {
                redisTemplate.opsForHash().put(path, MILLIS, String.valueOf(curMillis));
            }
            if (tokenNums > 0) {
                tokenNums--;
                if(distributed.equals("false")) {
                    tokenMap.put(path, tokenNums);
                } else {
                    redisTemplate.opsForHash().put(path, TOKEN, String.valueOf(tokenNums));
                }
                return (String) pjp.proceed();
            } else {
                return flowLimit.message();
            }
        } catch (Throwable t) {
            return t.toString();
        }
    }
}
