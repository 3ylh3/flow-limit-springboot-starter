package com.xiaobai.flowlimitspringbootstarter.controller;

import com.xiaobai.flowlimitspringbootstarter.aspect.FlowLimitAspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 获取通过/拒绝信息controller
 *
 * @author yin_zhj
 * @date 2020/6/15
 */
@RestController
public class GetInfoController {
    @Autowired
    private FlowLimitAspect flowLimitAspect;

    @RequestMapping("/info")
    public String getInfo() {
        return flowLimitAspect.getInfo();
    }
}
