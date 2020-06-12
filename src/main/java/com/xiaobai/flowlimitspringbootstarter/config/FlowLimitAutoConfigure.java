package com.xiaobai.flowlimitspringbootstarter.config;

import com.xiaobai.flowlimitspringbootstarter.aspect.FlowLimitAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自动装配类
 *
 * @author yin_zhj
 * @date 2020/6/12
 */
@Configuration
public class FlowLimitAutoConfigure {

    @Bean
    @ConditionalOnMissingBean
    public FlowLimitAspect initFlowLimit() {
        return new FlowLimitAspect();
    }
}
