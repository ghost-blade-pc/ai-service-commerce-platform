# 变更日志 — 支付回调实时化

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
|------|------|------|------|
| 2026-05-07 | Propose | 从退款联调中拆出支付回调延迟问题 | 用户反馈不能总等定时任务 |
| 2026-05-07 | Research | 定位异步回调和兜底 Job | `AliPayController#payNotify`、`NoPayNotifyOrderJob#exec`、`OrderService#changeOrderPaySuccess` |

## 约束

- 当前只建 Spec，不修改业务代码。
- 涉及支付成功状态，后续 apply 前必须人工确认方案。
