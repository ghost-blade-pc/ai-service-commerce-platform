# 变更日志 — AI 服务订阅与营销平台适配

> 记录决策、偏差、验证证据和可沉淀知识。

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
| --- | --- | --- | --- |
| 2026-05-11 20:44:48 +0800 | propose | 读取根级 `code_copilot/` 规则、知识、模板和 Git 状态 | 当前工作区已有较多未提交改动，本 change 仅新增根级 proposal 文档。 |
| 2026-05-11 20:44:48 +0800 | research | 读取 `s-pay-mall-ddd` 子项目 `AGENTS.md`、`code_copilot/README.md`、规则与知识索引 | 子项目规则优先，涉及商城支付/订单/退款必须标记高风险。 |
| 2026-05-11 20:44:48 +0800 | research | 扫描商城订单、商品、支付成功、成团、退款相关代码 | 发现当前履约为 `GoodsService#changeOrderDealDone` 直接置 `DEAL_DONE`。 |
| 2026-05-11 20:44:48 +0800 | research | 扫描营销侧拼团配置、锁单、结算、退单、SKU、通知任务相关代码与 SQL | 发现营销侧可复用 `goods_id/sku/activity/team/outTradeNo` 链路承载服务包拼团。 |
| 2026-05-11 20:44:48 +0800 | propose | 创建 `ai-service-subscription-platform` proposal | 只新增 `spec.md`、`tasks.md`、`log.md`，不进入实现。 |

## 技术决策

| 决策 | 选择 | 放弃的方案 | 原因 |
| --- | --- | --- | --- |
| 首期 proposal 策略 | 用“兼容字段 + 语义适配 + 履约边界扩展”描述迁移路径 | 直接要求全量重命名 `product/goods` | 当前 API、SQL、Mapper、前端和测试均依赖 `productId/goodsId`，全量重命名风险过高。 |
| 履约边界 | 支付/成团后异步触发权益开通，并用本地幂等记录或任务补偿 | 支付回调事务内同步调用 AI 权益系统 | 外部权益开通失败不能影响支付成功主路径。 |
| 变更目录 | `code_copilot/changes/ai-service-subscription-platform` | 放入某个子项目 change | 需求横跨商城与营销两个项目，根级工作区 change 更合适。 |

## DDD 边界记录

| 变更点 | 项目 | 模块 | 是否越界 | 说明 |
| --- | --- | --- | --- | --- |
| 订阅下单契约 | s-pay-mall-ddd | api/trigger/domain | 否 | API 接收 DTO，trigger 做协议转换，domain 处理订单规则。 |
| 服务套餐查询 | s-pay-mall-ddd | domain/infrastructure | 否 | domain 通过 `IProductPort` 感知业务对象，infrastructure 适配数据源。 |
| 拼团锁单/结算/退单 | cross-project | infrastructure/trigger/domain | 否 | 商城通过 gateway/port 调用营销 HTTP，营销 trigger 调用领域服务。 |
| 权益开通 | s-pay-mall-ddd | trigger/domain/infrastructure | 待确认 | 建议新增或替换当前 `goods` 履约服务，不能把外部 API 调用泄漏进订单领域。 |
| 营销活动数据 | group-buy-market | app/infrastructure/domain | 否 | SQL 初始化和 mapper 承载活动与服务包关联，domain 保持活动规则。 |

## 风险记录

| 风险类型 | 位置 | 风险描述 | 处理方式 |
| --- | --- | --- | --- |
| 资金 | `OrderService#doPrepayOrder`、`refundOrder`、`changeOrderRefundSuccess` | 服务订阅可能涉及额度消耗后退款、部分退款、退订停止服务等规则，不能沿用普通商品全额退款假设。 | 在 `spec.md` 标记待澄清，进入 `/apply` 前确认。 |
| 状态 | `OrderStatusVO`、`pay_order_mapper.xml` | `DEAL_DONE` 当前表示商品发货完成，适配后要表达权益开通完成。 | 首期建议复用状态但改语义；如新增状态需单独确认。 |
| MQ | `OrderPaySuccessListener`、`TeamSuccessTopicListener`、退款 Listener | 重复消息可能导致重复开通权益、重复退款或重复撤销。 | 权益履约必须增加幂等记录或本地任务表。 |
| 外部接口 | `ProductPort`、`IGroupBuyMarketService`、未来权益网关 | 商品/服务包目录和权益开通来源未确认，不能凭空实现外部系统。 | proposal 中保留 TODO，等待用户确认。 |
| 数据库 | `pay_order`、`pay_refund_task`、`sku`、`sc_sku_activity`、可能新增权益表 | 字段重命名或新增表会影响 Mapper、DAO、PO、SQL 和测试。 | 首期不直接重命名既有字段；新增表需单独设计和测试。 |
| 安全 | API Key、token、MCP 访问凭证 | 权益凭证不能进入日志、MQ、测试数据或文档示例。 | `security.md` 约束已写入 proposal 风险与验收。 |

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
| --- | --- | --- | --- |
| AI 服务套餐目录 | 需要可维护的服务套餐数据源 | 当前 `ProductRPC#queryProductByProductId` 硬编码 `MyBatisBook` 和 `100.00` | 标记为 Task 1，待确认数据源。 |
| 权益履约 | 支付或成团后开通 API Key/额度/权限 | 当前 `GoodsService#changeOrderDealDone` 只改订单状态 | 标记为 Task 4，待确认是否新增权益表和任务表。 |
| 营销商品语义 | AI 服务包可参加拼团 | 当前 SQL 初始化数据为图书商品 | 标记为 Task 2，按服务包示例数据适配。 |

## 验证证据

| 命令/方式 | 结果 | 备注 |
| --- | --- | --- |
| `git status --short --branch` | 成功 | 工作区已有未提交改动；本 proposal 避免触碰既有改动文件。 |
| 读取根级 `code_copilot/README.md`、`rules/*.md`、`knowledge/index.md`、`platform-knowledge.md` | 成功 | 确认跨项目规则和高风险边界。 |
| 读取 `s-pay-mall-ddd/code_copilot/README.md`、`rules/*.md`、`knowledge/index.md` | 成功 | 确认商城侧支付/订单/退款规则。 |
| `rg`/`sed` 扫描商城和营销核心代码 | 成功 | 形成 Research Findings。 |
| Maven 测试 | 未执行 | `/propose` 阶段仅新增文档，不改业务代码。 |

## 踩坑记录

| 问题 | 原因 | 解决方案 | 是否沉淀 |
| --- | --- | --- | --- |
| `s-pay-mall-ddd/docs/dev-ops/mysql/sql/s-pay-mall.sql` 未找到 | 当前工作区实际可见的商城 SQL 位于 `group-buy-market/docs/dev-ops/mysql/sql/s-pay-mall.sql` | proposal 中引用 Mapper 与实际扫描到的 SQL 事实，不依赖不存在路径。 | 否 |

## 知识发现

- [ ] 当前工作区里商城 SQL 文件路径与历史知识记录存在差异，进入 `/apply` 前需要再次确认 SQL 权威位置。
- [ ] `ProductRPC` 仍是硬编码商品模拟实现，是 AI 服务套餐适配的最小切入点之一。
- [ ] 权益开通不能只复用 `DEAL_DONE` 状态，需要额外表达“具体权益是否已开通、是否可重试、是否已撤销”。
