# 任务拆分 — 需求名称

> 推荐顺序：契约/API → 领域模型与端口 → 基础设施适配 → 领域服务编排 → 触发入口 → 配置与验证  
> 每个任务应是小范围、可审查、可验证的原子变更。

## 前置条件

- [ ] 已读取根级 `code_copilot/rules/*.md`
- [ ] 已读取目标子项目 `AGENTS.md`
- [ ] 涉及 `s-pay-mall-ddd/` 时已读取其子项目级 `code_copilot/`
- [ ] 已确认影响项目和 DDD 层级
- [ ] 已确认是否涉及资金、订单状态、退款、拼团、MQ、外部接口、数据库
- [ ] 已检查 Git 分支和未提交变更

## Task 1: 任务名

- **目标**：
- **影响项目**：group-buy-market / s-pay-mall-ddd / cross-project
- **DDD 层级**：api / domain / infrastructure / trigger / app / types
- **涉及文件**：
  - `path/to/File.java`：新增/修改，说明原因
- **关键签名**：
  ```java
  // TODO: 填写方法签名
  ```
- **依赖**：无 / Task X
- **风险标记**：资金 / 状态 / MQ / 外部接口 / 数据库 / 安全 / 无
- **验收标准**：
- **验证命令**：
- **状态**：pending

## 变更摘要

- **总文件数**：
- **Spec-Plan 偏差记录**：
- **验证结果**：
- **遗留问题**：
