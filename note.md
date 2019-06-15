## 赛题
``` 
https://code.aliyun.com/middlewarerace2019/adaptive-loadbalance?spm=5176.12281978.0.0.562d1556ZdU8ra&accounttraceid=1f4ad975-ff41-4ea9-a17a-0d0d5ce799d6
```

## git

```
https://code.aliyun.com/cirnotxm/adaptive-loadbalance?spm=a2111a.8458726.0.0.6c6c3531E1A8Fv
```

## 常见LB算法简介
```
https://avinetworks.com/docs/16.3/load-balancing-algorithms
```

### 题目要点

**要求的整体能力**

1. Consumer需动态优化分配
2. Provider需自动进行容量评估
3. Provider可拒绝部分请求保证服务不宕机
4. Consumer允许拒绝超过总和的请求量
5. Consumer: 4C4G  Provider: 1C2G 2C4G 3C6G
6. 尽量避免因重试导致流量放大
7. 需考虑多Consumer的情况

**Provider**

使用rpc接口的实际功能是计算hashCode

1. 接口响应时间符合负指数分布，也就是说次数越多耗时越低？
2. 并发程度会变化，即允许同时调用的线程数会变化

**启动顺序**

1. Provider
2. Consumer

**请求流程**

1. Consumer选择Provider
2. Provider处理并返回
3. Consumer返回

**Consumer接口**

1. LoadBalance负载均衡
2. CallbackListener回调监听
3. Filter过滤器

**Provider接口**

1. CallbackService回调推送服务
2. RequestLimiter限流
3. Filter过滤器

**已知条件**

1. 预热30s，请求速率固定，停止5s
2. 评测1m，速率固定
3. 排名规则：1.成功请求数 2.最大tps
4. Provider处理能力会变化

**限制**

1. 不允许修改配置
2. 不允许替换服务
3. 不允许使用Provider信息，可计算
4. 不允许引入外部依赖
5. 不允许修改jvm参数

### 想法

**可以做到的事**

1. 请求计数、完成计数
2. 处理计时
3. Consumer拒绝请求
4. Provider拒绝请求
5. Consumer重试
6. Provider推送信息
7. 猜测Provider处理能力

**细节想法**

实际功能是计算hashCode
>core、memory都不会对计算能力有影响
不同的Provider，对于同一个请求的处理能力相同，响应时间相同
由于模拟了实际情况，Provider处理能力会变化，响应时间会变化

计算成绩按照成功请求数+最大tps计算
>要求我们需要尽量保证请求成功响应，即尽量减少拒绝次数
要求tps尽量高，即尽量达到请求qps，即要求响应时间尽量短
最终目标：在最短的时间内完成请求处理，并尽量减少拒绝次数

实际响应时间=负载均衡计算时间+Provider处理时间
>Provider处理时间不可控，只能缩短负载均衡计算时间
理想情况下，不计算路由，每个请求没有负载均衡计算时间
可以采用乐观锁的思想，即没出错就不加处理

对于同一个Provider，可以统计一定数量内的响应时间平均值
>由于配置对响应时间影响不大，我们可以理解为每个Provider的响应时间基本相同
尽量选择响应时间短的Provider，有助于提高tps

每个Provider的响应时间会变化
>可以根据变化趋势猜测处理能力的变化
可通过Filter拿Queue计算Provider最近n次请求的响应时间平均值，最好放在Provider计算，避免影响Consumer能力
如果一次（或多次）请求的响应时间比平均时间长，那么可以猜测处理能力下降，比对其他Provider的响应时间，之后的请求换Provider
如果响应时间下降，可以猜测处理能力在提高，继续使用这个Provider

最好引入超时时间
>通过超时时间可以在RequestLimiter中及早避免负载过高的问题

评测过程中有个预热阶段
>在预热阶段可以探测每个Provider的最大线程池负载能力，并应用到后续的评测阶段
如果超过了处理能力可以考虑拒绝请求

刚开始时可以有Provider的预热
>初始状态下，直接将一批请求打到同一个Provider中，达到线程池阈值的一定比例后再换，直到Provider已全部预热过

预热阶段使用生产者消费者模式
>生产者发起请求，消费者可以并发消费请求，每个Provider分配一个消费者线程
使用队列将生产速率和消费速率分离

## 初步实现思路

**核心思路：减少负载均衡计算次数，用切铁轨的方式完成负载均衡**

1. 预热阶段轮流将每一个Provider打到响应超时为止，探测最大处理能力
2. 评测阶段首先预热，在达到Provider处理能力的一定比例如50%前都不进行负载均衡计算
3. （使用队列）为每个Provider计算最近n次请求的响应时间平均值
4. 策略开始，在响应时间未（多次）超过平均时间，且未发生超时，且未达到线程池负载阈值的情况下，不进行负载均衡计算，直接复用上一次的Provider
5. 一旦上述条件不满足，触发负载均衡计算，找到最近平均响应时间最短的Provider，使用CAS无锁切换下一次的Provider
6. 重复上述策略，还得分情况如果多次命中原来的Provider该怎么办
7. 其他情况