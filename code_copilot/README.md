# code_copilot — s-pay-mall-ddd 渐进式 SpecAI 编程框架

本目录是当前支付商城 DDD 项目的 AI 协作工作台。它不是通用提示词集合，而是围绕本项目的模块分层、支付/订单/拼团营销交互、资金状态变更风险和渐进式 Spec 流程定制的上下文资产。

## 目录说明

```text
code_copilot/
  agents/              AI 角色提示词
  rules/               项目长期规则，优先级高于一次性需求
  knowledge/           可复用业务知识索引
  changes/templates/   需求 Spec、任务、日志、测试模板
  changes/<name>/      单个需求或 bug 的 Spec、任务、日志、测试计划
```

## 使用方式

每次让 Codex 改代码前，先让它读取：

```text
code_copilot/agents/copilot-prompt.md
code_copilot/rules/*.md
code_copilot/knowledge/index.md
```

然后按需求创建：

```text
code_copilot/changes/<change-name>/spec.md
code_copilot/changes/<change-name>/tasks.md
code_copilot/changes/<change-name>/log.md
```

推荐提问方式：

```text
请按 code_copilot 的规则处理这个需求：
<你的需求描述>

要求：
1. 先做 Research，引用真实文件路径和类/方法
2. 先生成 spec 和 tasks，不要直接改代码
3. 涉及支付、退款、订单状态、拼团营销交互时标记高风险
```

## 当前项目架构关键词

- Maven 多模块：`api`、`app`、`domain`、`trigger`、`infrastructure`、`types`
- 入口协议：Spring MVC Controller、RabbitMQ Listener、Job
- 领域核心：订单、支付、退款、拼团营销锁单/结算、登录
- 外部交互：支付宝、微信、拼团营销服务、RabbitMQ、MySQL
- HTTP 客户端：Retrofit2，优先沿用 `infrastructure/gateway` 模式

## 当前 Change 状态

| Change | 状态 | 说明 |
|--------|------|------|
| `refund-market-mq-integration` | done | 退单退款服务对接，包含模拟退款、拼团退款 MQ、幂等和运行期时序修复 |
| `payment-callback-realtime` | propose | 后续 bug：支付成功不能总等待 `NoPayNotifyOrderJob` 兜底 |
| `group-buy-join-discount` | propose | 后续 bug：参团时疑似没有享受拼团优惠 |

## 基本原则

- 先 Spec，后代码；先小范围 Research，后设计方案。
- 领域规则进入 `domain`，外部系统适配进入 `infrastructure`。
- `trigger` 只做协议接入、参数转换、响应封装，不承载核心业务规则。
- 涉及资金、退款、订单状态、支付回调、拼团结算的变更必须人工复核。
- 一个已完成 change 不继续塞入新需求；运行期发现的新问题应拆成新的 `changes/<bug-name>`，避免任务边界失控。
