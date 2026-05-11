# 拼团成团与支付成功消息时序分析

## 背景

用户观察到支付商城日志中 `TeamSuccessTopicListener` 和 `OrderPaySuccessListener` 近乎同时收到消息，并且最后一次像是同时收到三笔订单的支付成功消息。设计预期理解为每笔订单支付完成后就会收到一条 `OrderPaySuccessListener` 消息，因此需要确认是否存在 MQ 异常、消息堆积、重复投递或业务语义误解。

本次只做分析和文档沉淀，不修改业务代码。

## 结论

这不是 RabbitMQ 把三笔支付成功消息合并，也不是消费端异常堆积释放导致的现象。对拼团订单来说，`OrderPaySuccessListener` 当前实际承载的是“可履约/模拟发货”的后置动作，而不是“支付宝刚支付成功”的第一时间事件。

当前拼团订单链路是：

```text
PAY_WAIT -> PAY_SUCCESS -> MARKET -> DEAL_DONE
```

- `PAY_SUCCESS`: 支付侧确认单笔订单已支付。
- `MARKET`: 拼团侧确认整团成团，支付侧完成营销结算。
- `DEAL_DONE`: 支付商城消费本地 `topic.order_pay_success` 后模拟发货完成。

## 证据

支付商城代码：

- `OrderService#changeOrderPaySuccess`: 拼团订单支付成功后先 `changeMarketOrderPaySuccess(orderId)`，再调用拼团结算端口，不直接发布 `topic.order_pay_success`。
- `TeamSuccessTopicListener#listener`: 消费拼团侧 `topic.team_success`，消息中包含 `outTradeNoList`。
- `OrderRepository#changeOrderMarketSettlement`: 对 `outTradeNoList` 批量更新 `MARKET`，随后逐个发布 `topic.order_pay_success`。
- `OrderPaySuccessListener#listener`: 消费 `topic.order_pay_success` 后调用 `goodsService.changeOrderDealDone`，模拟发货。

拼团侧代码：

- `TradeRepository`: 成团时查询整团完成订单列表，写入 `NotifyTask.parameterJson`，字段为 `teamId` 和 `outTradeNoList`。
- `TradePort#groupBuyNotify`: `notifyType=MQ` 时发布 `topic.team_success`。

运行日志示例：

- `2026-05-08 18:02:00.549`: 拼团侧发送 `topic.team_success`，消息体为 `{"teamId":"27671702","outTradeNoList":["876802918063","945473857007","166441276106"]}`。
- `2026-05-08 18:02:00.584`: 支付侧 `TeamSuccessTopicListener` 收到同一整团消息。
- `2026-05-08 18:02:00.610` 到 `18:02:00.628`: 支付侧 `OrderPaySuccessListener` 连续收到三条 `tradeNo`，分别对应整团三笔订单。

## 风险判断

状态模型本身可以成立：拼团订单必须等成团后才能履约，所以 `PAY_SUCCESS -> MARKET -> DEAL_DONE` 有业务含义。

但当前实现有两个命名和幂等风险：

- `topic.order_pay_success` / `OrderPaySuccessListener` 对拼团订单来说命名偏前置，实际更接近“订单可履约”事件。
- 当前分支 `changeOrderMarketSettlement` 和 `changeOrderDealDone` 的 SQL 前置状态约束较弱，重复消息或乱序消息存在把订单推进到后续状态的风险。
