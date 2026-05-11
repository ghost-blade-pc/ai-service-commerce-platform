# 支付回调实时化
> status: propose
> created: 2026-05-07
> complexity: 🟡中等

## 1. 背景与目标

当前支付宝异步通知到达存在延迟，支付商城主要依赖 `NoPayNotifyOrderJob` 每分钟查询兜底，用户支付完成后订单和拼团结算可能不能及时推进。

目标是让支付成功后的订单状态尽快变成 `PAY_SUCCESS/MARKET`，不能总是等待定时任务。可选方向包括：支付返回页主动查询、前端轮询查单、后端提供支付结果查询接口、或在支付成功页面触发支付宝主动查单。

### 1.1 业务边界

- 所属上下文：支付 / 订单 / 拼团营销结算
- 调用方向：支付宝异步回调 / 支付宝主动查单 / 本服务对外提供查询接口 / Job 补偿
- 是否涉及资金或订单状态：是

## 2. 代码现状（Research Findings）

- HTTP 支付回调入口：`s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/AliPayController.java` 的 `payNotify(HttpServletRequest)` 只处理支付宝异步 POST 通知，验签通过后调用 `orderService.changeOrderPaySuccess(tradeNo, payTime)`。
- 支付成功领域编排：`s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/OrderService.java` 的 `changeOrderPaySuccess(String orderId, Date payTime)` 对拼团订单先 `changeMarketOrderPaySuccess`，再调用 `port.settlementMarketPayOrder`。
- 兜底任务：`s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/job/NoPayNotifyOrderJob.java` 的 `exec()` 每分钟查询 `orderService.queryNoPayNotifyOrder()`，再调用支付宝 `AlipayTradeQueryRequest`，成功时调用 `changeOrderPaySuccess`。
- 当前风险：如果支付宝异步通知慢，订单需要等 Job；如果 `changeOrderPaySuccess` 里的拼团结算失败，需要依赖下一次异步通知或 Job 重试。

## 3. 功能点

- [ ] 功能 1：新增或复用支付结果主动查询能力，用户支付完成后可主动推进订单状态。
- [ ] 功能 2：保持 `NoPayNotifyOrderJob` 作为兜底补偿，而不是主路径。
- [ ] 功能 3：拼团订单支付成功后仍必须触发 `settlementMarketPayOrder`，不能只改支付状态。
- [ ] 功能 4：支付成功处理必须幂等，重复回调、重复查单不能重复破坏状态。

## 4. 业务规则

- 只有支付宝查询/回调明确 `TRADE_SUCCESS` 才允许推进支付成功。
- 支付成功处理统一收敛到 `OrderService#changeOrderPaySuccess`。
- 拼团订单不能绕过营销结算。
- 定时任务保留为补偿手段。

## 5. 数据变更

- 暂不确定是否新增字段。
- 如果需要记录支付宝 `trade_no`、最后查单时间、支付确认来源，应单独评估 mapper 和初始化 SQL。

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 待定 | `/api/v1/alipay/*` | GET/POST | 可能新增支付结果查询/主动确认接口 |

## 7. 风险与关注点

- ⚠️ 资金风险：不能伪造支付成功，必须依赖支付宝签名回调或主动查单结果。
- ⚠️ 状态风险：重复回调、Job、主动查询可能并发推进同一订单。
- ⚠️ 外部服务风险：支付宝查单失败不能误判为支付失败。

## 8. 测试策略

- `domain`：单元测试 + Mock，覆盖 `changeOrderPaySuccess` 幂等和拼团结算失败重试语义。
- `trigger`：MockMvc 或 Controller 单测，覆盖主动查询接口入参和响应。
- `infrastructure`：支付宝查单适配建议封装成端口后 Mock。

## 9. 待澄清

- [ ] 前端支付完成后是否能拿到 `orderId` 并调用支付结果查询接口？
- [ ] 是否允许新增入站接口，例如 `query_pay_result`？
- [ ] 是否需要保存支付宝 `trade_no`？
- [ ] 拼团结算失败时是否需要新增独立补偿 Job，而不是复用未支付通知查询 Job？

## 10. 技术决策

| 决策点 | 建议 | 理由 |
|--------|------|------|
| 支付成功入口 | 统一调用 `OrderService#changeOrderPaySuccess` | 避免回调、查单、Job 三套状态逻辑 |
| 定时任务定位 | 保留兜底 | 支付宝异步通知和主动查单都可能失败 |
| 支付宝查单 | 通过端口封装 | 避免 Controller/Job 直接散落 SDK 调用 |
