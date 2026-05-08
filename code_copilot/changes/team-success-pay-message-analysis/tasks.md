# 任务拆分 — 拼团成团与支付成功消息时序分析

- [x] 检查支付商城 `OrderPaySuccessListener`、`TeamSuccessTopicListener`、订单状态更新和 MQ 发布逻辑。
- [x] 检查拼团侧 `topic.team_success` 生产逻辑、消息体和运行日志。
- [x] 对照支付商城运行日志，确认三笔 `OrderPaySuccessListener` 消息来自同一条整团 `team_success`。
- [x] 判断 `PAY_SUCCESS -> MARKET -> DEAL_DONE` 状态链路的业务合理性和当前实现风险。
- [x] 沉淀分析结论到 `code_copilot`。

## 不做

- [ ] 不修改业务代码。
- [ ] 不调整 MQ topic 命名。
- [ ] 不新增状态流转幂等 SQL。
