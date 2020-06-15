package com.xiaobai.flowlimitspringbootstarter.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaobai.flowlimitspringbootstarter.aspect.FlowLimitAspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 自动装配类
 *
 * @author yin_zhj
 * @date 2020/6/12
 */
@Configuration
@EnableConfigurationProperties(FlowLimitProperties.class)
public class FlowLimitAutoConfigure {
    @Autowired
    private FlowLimitProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public FlowLimitAspect initFlowLimit() {
        if(null == properties.getDistributed()) {
            return new FlowLimitAspect("false");
        } else {
            return new FlowLimitAspect(properties.getDistributed());
        }
    }
}
