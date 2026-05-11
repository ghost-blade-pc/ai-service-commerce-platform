# 任务拆分 — 支付回调实时化
> status: propose

## 前置条件

- [ ] 用户确认是否允许新增支付结果查询接口。
- [ ] 用户确认是否保存支付宝 `trade_no`。
- [ ] Research 支付创建返回页、前端调用方式和当前订单查询接口。

## Task 1: Research 支付成功链路

- **目标**: 明确支付宝异步回调、return_url、定时任务和前端查询当前如何协作。
- **涉及文件**:
  - `s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/AliPayController.java`
  - `s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/job/NoPayNotifyOrderJob.java`
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/OrderService.java`
- **完成**: pending

## Task 2: 设计支付主动确认端口

- **目标**: 将支付宝查单能力从 Job 中抽出为 infrastructure 端口。
- **完成**: pending

## Task 3: 新增支付结果查询/确认入口

- **目标**: 用户支付完成后可通过接口主动触发查单并推进状态。
- **完成**: pending

## Task 4: 补充幂等和测试

- **目标**: 覆盖异步回调、主动查单、Job 重复执行的状态幂等。
- **完成**: pending
