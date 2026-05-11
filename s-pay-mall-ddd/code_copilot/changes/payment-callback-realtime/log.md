# 变更日志 — 支付回调实时化

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
|------|------|------|------|
| 2026-05-07 | Propose | 从退款联调中拆出支付回调延迟问题 | 用户反馈不能总等定时任务 |
| 2026-05-07 | Research | 定位异步回调和兜底 Job | `AliPayController#payNotify`、`NoPayNotifyOrderJob#exec`、`OrderService#changeOrderPaySuccess` |
| 2026-05-11 | Research | 复核本地 Docker 运行态日志，确认一次支付成功最终由 Job 推进 | 订单 `621247345364` 在 `20:06:59` 创建，`20:08:00` 由 `NoPayNotifyOrderJob` 查单成功触发 `OrderPaySuccessListener`，未见 `payNotify` 异步回调日志 |
| 2026-05-11 | Propose | 细化第二层主动查单方案，等待用户确认后执行 | 建议新增 `query_pay_result`，并将 `create_pay_order` 响应调整为 `orderId + payUrl` |

## 根因记录

- 支付宝异步通知不是实时保证；沙箱环境和本地 FRP 链路都会增加不确定性。
- 当前主路径只有两个状态推进入口：`AliPayController#payNotify` 异步回调、`NoPayNotifyOrderJob#exec()` 分钟级兜底。
- `queryNoPayNotifyOrder` 只查询下单超过 1 分钟仍为 `PAY_WAIT` 的订单，所以异步通知未到时，用户必然感知到至少分钟级延迟。
- `return_url` 只负责浏览器回跳，不会自动推进支付成功状态。
- 更实时的方案是新增“前端支付完成后主动查单/确认”入口，并统一复用 `OrderService#changeOrderPaySuccess`。
- 当前 `create_pay_order` 返回 `Response<String>`，data 是支付宝表单 HTML；领域层 `PayOrderEntity` 已经有 `orderId`。为了让前端可靠短轮询，推荐改为 `Response<CreatePayResponseDTO(orderId, payUrl)>`，而不是从表单 HTML 解析 `biz_content.out_trade_no`。

## 约束

- 当前只建 Spec，不修改业务代码。
- 涉及支付成功状态，后续 apply 前必须人工确认方案。
- 第一阶段不新增数据库字段，不保存支付宝 `trade_no`。
