# 需求名称
> status: propose | apply | review | done
> created: YYYY-MM-DD
> scope: group-buy-market | s-pay-mall-ddd | cross-project
> complexity: 简单 | 中等 | 复杂

## 1. 背景与目标

说明为什么做、解决什么问题、完成后如何验证。

## 2. 业务边界

- 影响项目：
- 所属上下文：订单 / 支付 / 退款 / 拼团营销 / 登录 / 配置 / 其他
- 调用方向：本项目对外提供 / 本项目调用外部服务 / MQ 消费 / MQ 生产 / Job 补偿
- 是否跨项目：
- 是否涉及资金、订单状态、拼团状态：

## 3. 代码现状（Research Findings）
> 每个结论必须有路径 + 类名/方法名/配置键/依赖出处。

### 3.1 相关入口

- HTTP:
- MQ:
- Job:
- Domain Service:
- Repository/Port/Gateway:
- Config:

### 3.2 现有实现

### 3.3 发现与风险

## 4. DDD 模块影响

| 项目 | 模块 | 是否影响 | 文件/类 | 说明 |
| --- | --- | --- | --- | --- |
| group-buy-market / s-pay-mall-ddd | api | | | |
| group-buy-market / s-pay-mall-ddd | trigger | | | |
| group-buy-market / s-pay-mall-ddd | domain | | | |
| group-buy-market / s-pay-mall-ddd | infrastructure | | | |
| group-buy-market / s-pay-mall-ddd | app | | | |
| group-buy-market / s-pay-mall-ddd | types | | | |

## 5. 功能点

- [ ] 功能 1：

## 6. 业务规则

- 状态流转：
- 金额/优惠/退款：
- 拼团营销：
- 幂等：
- 异常与补偿：

## 7. 数据变更

| 操作 | 项目 | 表名 | 字段/索引 | 说明 |
| --- | --- | --- | --- | --- |

- 是否需要改 MyBatis mapper：
- 是否需要改初始化 SQL：

## 8. 接口与消息变更

### 8.1 REST API

| 方向 | 服务 | Path | Method | Request DTO | Response DTO | 鉴权/签名 |
| --- | --- | --- | --- | --- | --- | --- |

### 8.2 MQ 契约

| 方向 | Exchange | Routing Key | Queue | Message | 幂等键 |
| --- | --- | --- | --- | --- | --- |

## 9. 测试策略

- 测试范围：
- 优先测试类型：
- 需要独立 `test-spec.md`：是 / 否
- 推荐验证命令：

## 10. 待澄清

- [ ] TODO:

## 11. 技术决策

| 决策点 | 选择 | 备选 | 理由 |
| --- | --- | --- | --- |

## 12. 风险与人工确认

- 资金风险：
- 状态流转风险：
- MQ/外部接口风险：
- 数据风险：
- 安全风险：

## 13. 确认记录

- 确认时间：
- 确认人：
- 确认范围：
