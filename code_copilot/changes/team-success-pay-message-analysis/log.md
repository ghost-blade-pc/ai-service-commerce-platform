# 变更日志 — 拼团成团与支付成功消息时序分析

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
|------|------|------|------|
| 2026-05-08 | Research | 检查支付商城两个 listener、订单服务、仓储和 mapper | 确认拼团订单支付成功不直接发货，成团后再发布本地支付成功消息 |
| 2026-05-08 | Research | 检查拼团侧 `TradeRepository`、`TradePort` 和运行日志 | 确认 `topic.team_success` 一条消息携带整团 `outTradeNoList` |
| 2026-05-08 | Analysis | 对照 `teamId=86190686` 和 `teamId=27671702` 的日志 | 发现最后一笔支付/查单结算触发成团，随后支付侧连续发布三条本地履约消息 |
| 2026-05-08 | Analysis | 判断订单状态链路 | `PAY_SUCCESS -> MARKET -> DEAL_DONE` 对拼团业务可成立，但当前命名和状态前置条件偏弱 |
| 2026-05-08 | Docs | 按当前分支回退情况清理 `code_copilot` | 移除已回退的 3.8 hardening change 文档，新增本分析记录 |

## 关键发现

- 拼团侧成团消息不是单订单粒度，而是整团粒度。
- 支付侧 `TeamSuccessTopicListener` 收到整团消息后会对列表内每笔订单发布一条 `topic.order_pay_success`。
- `OrderPaySuccessListener` 的日志“收到支付成功消息”容易误读；对拼团订单而言，它发生在成团后的履约阶段。
- 支付侧 `NoPayNotifyOrderJob` 会在支付宝异步通知未及时到达时按分钟查单，因此最后一笔订单可能由 Job 推进到支付成功并触发成团。

## 后续建议

- 如后续修改代码，优先考虑把 `topic.order_pay_success` 的语义改名或补充注释为“可履约事件”。
- 给 `MARKET` 和 `DEAL_DONE` 状态更新增加前置状态条件，降低重复消息和乱序消息风险。
