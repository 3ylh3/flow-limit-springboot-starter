package com.xiaobai.flowlimitspringbootstarter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 配置类
 *
 * @author yin_zhj
 * @date 2020/6/12
 */
@Data
@ConfigurationProperties("flow-limit")
public class FlowLimitProperties {
    /**
     * 分布式标志
     */
    public String distributed;
}
