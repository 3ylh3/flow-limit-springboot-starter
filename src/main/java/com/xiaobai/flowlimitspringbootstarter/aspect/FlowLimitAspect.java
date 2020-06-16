package com.xiaobai.flowlimitspringbootstarter.aspect;

import com.alibaba.fastjson.JSON;
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
import java.util.HashMap;
import java.util.Map;

/**
 * 流控切面类
 *
 * @author yin_zhj
 * @date 2020/6/12
 */
@Aspect
public class FlowLimitAspect {
    private String distributed;

    private Map<String, Long> millisMap = new HashMap<>();
    private Map<String, Long> tokenMap = new HashMap<>();
    private Map<String, Map<String, Long>> infoMap = new HashMap<>();
    private static final String MILLIS = "millis";
    private static final String TOKEN = "token";
    private static final String FALSE = "false";
    private static final String LIMIT = "limit";
    private static final String ACCESS = "access";
    private static final String INFO_KEY = "flow_limit_info";
    private boolean cleanFlag = false;
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
            //获取生成令牌时间间隔和最大令牌数
            long interval = flowLimit.interval();
            long maxTokenNums = flowLimit.maxTokenNums();
            String path = requestMapping.value()[0];
            long curMillis = System.currentTimeMillis();
            Long tokenNums = null;
            Long millis = null;
            synchronized (this) {
                //获取令牌数和上次生成令牌时间戳
                if (FALSE.equals(distributed)) {
                    tokenNums = tokenMap.get(path);
                    millis = millisMap.get(path);
                } else {
                    //项目重新启动后第一次需要清除redis历史数据
                    if(!cleanFlag) {
                        cleanReids();
                    }
                    String tokenStr = (String) redisTemplate.opsForHash().get(path, TOKEN);
                    if (null != tokenStr) {
                        tokenNums = Long.valueOf(tokenStr);
                    }
                    String millisStr = (String) redisTemplate.opsForHash().get(path, MILLIS);
                    if (null != millisStr) {
                        millis = Long.valueOf(millisStr);
                    }
                }
                //根据当前时间和上次生成令牌时间的时间间隔、上次令牌数和生成令牌时间间隔计算当前令牌数
                if (null == tokenNums) {
                    tokenNums = maxTokenNums;
                }
                if (null != millis) {
                    long nums = (curMillis - millis) / interval;
                    tokenNums += nums;
                    if (tokenNums > maxTokenNums) {
                        tokenNums = maxTokenNums;
                    }
                }
                //保存本次生成令牌时间戳
                if (FALSE.equals(distributed)) {
                    millisMap.put(path, curMillis);
                } else {
                    redisTemplate.opsForHash().put(path, MILLIS, String.valueOf(curMillis));
                }
                //令牌数大于0，请求通过
                if (tokenNums > 0) {
                    tokenNums--;
                    if (FALSE.equals(distributed)) {
                        //保存令牌数
                        tokenMap.put(path, tokenNums);
                        //保存通过信息
                        if (infoMap.containsKey(path)) {
                            Map<String, Long> tmp = infoMap.get(path);
                            Long accNums = tmp.get(ACCESS);
                            if(null != accNums) {
                                tmp.put(ACCESS, accNums + 1);
                                infoMap.put(path, tmp);
                            } else {
                                tmp.put(ACCESS, 1L);
                                infoMap.put(path, tmp);
                            }
                        } else {
                            Map<String, Long> tmp = new HashMap<>();
                            tmp.put(ACCESS, 1L);
                            infoMap.put(path, tmp);
                        }
                    } else {
                        //保存令牌数
                        redisTemplate.opsForHash().put(path, TOKEN, String.valueOf(tokenNums));
                        //保存通过信息
                        String infoStr = (String)redisTemplate.opsForHash().get(INFO_KEY, path);
                        if(null != infoStr) {
                            Map<String, String> tmp = JSON.parseObject(infoStr, Map.class);
                            String accNums = tmp.get(ACCESS);
                            if(null != accNums) {
                                tmp.put(ACCESS, String.valueOf(Long.parseLong(accNums) + 1));
                                redisTemplate.opsForHash().put(INFO_KEY, path, JSON.toJSONString(tmp));
                            } else {
                                tmp.put(ACCESS, "1");
                                redisTemplate.opsForHash().put(INFO_KEY, path, JSON.toJSONString(tmp));
                            }
                        } else {
                            Map<String, String> tmp = new HashMap<>();
                            tmp.put(ACCESS, "1");
                            redisTemplate.opsForHash().put(INFO_KEY, path, JSON.toJSONString(tmp));
                        }
                    }
                    return (String) pjp.proceed();
                } else {
                    //请求被限制，保存限制信息
                    if(FALSE.equals(distributed)) {
                        if(infoMap.containsKey(path)) {
                            Map<String, Long> tmp = infoMap.get(path);
                            Long lmtNums = tmp.get(LIMIT);
                            if(null != lmtNums) {
                                tmp.put(LIMIT, lmtNums + 1);
                                infoMap.put(path, tmp);
                            } else {
                                tmp.put(LIMIT, 1L);
                                infoMap.put(path, tmp);
                            }
                        } else {
                            Map<String, Long> tmp = new HashMap<>();
                            tmp.put(LIMIT, 1L);
                            infoMap.put(path, tmp);
                        }
                    } else {
                        String infoStr = (String)redisTemplate.opsForHash().get(INFO_KEY, path);
                        if(null != infoStr) {
                            Map<String, String> tmp = JSON.parseObject(infoStr, Map.class);
                            String accNums = tmp.get(LIMIT);
                            if(null != accNums) {
                                tmp.put(LIMIT, String.valueOf(Long.parseLong(accNums) + 1));
                                redisTemplate.opsForHash().put(INFO_KEY, path, JSON.toJSONString(tmp));
                            } else {
                                tmp.put(LIMIT, "1");
                                redisTemplate.opsForHash().put(INFO_KEY, path, JSON.toJSONString(tmp));
                            }
                        } else {
                            Map<String, String> tmp = new HashMap<>();
                            tmp.put(LIMIT, "1");
                            redisTemplate.opsForHash().put(INFO_KEY, path, JSON.toJSONString(tmp));
                        }
                    }
                    return flowLimit.message();
                }
            }
        } catch (Throwable t) {
            return t.toString();
        }
    }

    public void cleanReids() {
        //清理redis中历史数据
        Map<Object, Object> map = redisTemplate.opsForHash().entries(INFO_KEY);
        //清理令牌和时间数据
        for(Map.Entry<Object, Object> entry : map.entrySet()) {
            redisTemplate.delete(entry.getKey().toString());
        }
        //清理通过/限制信息
        redisTemplate.delete(INFO_KEY);
        cleanFlag = true;
    }

    public String getInfo() {
        if(FALSE.equals(distributed)) {
            return JSON.toJSONString(infoMap);
        } else {
            //项目重启后清楚历史记录
            if(!cleanFlag) {
                cleanReids();
            }
            Map<Object, Object> tmp = redisTemplate.opsForHash().entries(INFO_KEY);
            Map<String, Map<String, Long>> map = new HashMap<>();
            for(Map.Entry<Object, Object> entry : tmp.entrySet()) {
                String key = entry.getKey().toString();
                String value = entry.getValue().toString();
                map.put(key, JSON.parseObject(value, Map.class));
            }
            return JSON.toJSONString(map);
        }
    }
}
