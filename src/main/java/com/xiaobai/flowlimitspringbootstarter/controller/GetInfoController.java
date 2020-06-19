package com.xiaobai.flowlimitspringbootstarter.controller;

import com.xiaobai.flowlimitspringbootstarter.aspect.FlowLimitAspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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

    @RequestMapping("/flowLimitInfo")
    public String getInfo() {
        return flowLimitAspect.getInfo();
    }

    @RequestMapping("/serviceInfo")
    public String getServiceInfo(@RequestBody Map<String, String> map) {
        return flowLimitAspect.getServiceInfo(map.get("service"));
    }
}
