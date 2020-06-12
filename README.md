# flow-limit-springboot-starter
基于令牌桶算法的简单流量控制组件，控制粒度到接口方法级别。
# 使用
引入jar包：
`xml
<dependency>
   <groupId>com.xiaobai</groupId>
   <artifactId>flow-limit-springboot-starter</artifactId>
   <version>1.0.0</version>
</dependency>
`
在需要流控的方法上添加@FlowLimit注解（该方法需为controller中使用@RequestMapping注解的方法）:
```java
@RequestMapping("/test")
@FlowLimit(interval = 1000, maxTokenNums = 200, message = "message")
public String test() {
    return "success";
}
```
注解中interval参数为生成令牌的时间间隔(毫秒)，默认为1000ms；maxTokenNums为最大令牌数量，默认为500；message为触发流控后的返回信息，默认为"Flow limit,please try later"。
# 下载