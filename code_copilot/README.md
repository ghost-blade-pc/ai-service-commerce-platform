# code_copilot — ddd-trade-marketing-platform 工作区级 SpecAI 规范

本目录是 `ddd-trade-marketing-platform` 根工作区的 AI 协作规范。当前工作区聚合两个 Java DDD 项目：

- `group-buy-market/`：拼团营销服务。
- `s-pay-mall-ddd/`：支付商城服务。

根级 `code_copilot/` 用于跨项目需求、联调问题、架构边界和变更管理。若某个子项目已有自己的 `code_copilot/` 或 `AGENTS.md`，子项目规则对该子项目内的代码变更具有更高优先级。

## 目录说明

```text
code_copilot/
  agents/              AI 编码、Spec 审查、质量审查角色提示词
  rules/               工作区长期规则
  knowledge/           可复用知识索引
  changes/templates/   需求 Spec、任务、测试、日志模板
  changes/<change-id>/ 单个变更目录
```

## 使用方式

处理业务代码前，先读取：

```text
code_copilot/README.md
code_copilot/rules/project-context.md
code_copilot/rules/domain-rules.md
code_copilot/rules/coding-style.md
code_copilot/rules/security.md
code_copilot/knowledge/index.md
```

如果变更只涉及 `s-pay-mall-ddd/`，还必须读取 `s-pay-mall-ddd/code_copilot/` 下更具体的规则与变更文档。

推荐提问方式：

```text
请按 ddd-trade-marketing-platform 根级 code_copilot 规则处理这个需求：
<需求描述>

要求：
1. 先做 Research，引用真实路径、类名和方法名
2. 先生成 spec 和 tasks，不要直接改代码
3. 标明影响 group-buy-market、s-pay-mall-ddd，还是跨项目联动
```

## 当前项目事实

- 根目录没有 Maven 聚合 `pom.xml`；两个子项目各自拥有 Maven reactor。
- 两个子项目均采用 Java 17、Spring Boot 2.7.12、Maven 多模块 DDD 分层。
- 常见模块：`api`、`app`、`domain`、`trigger`、`infrastructure`、`types`。
- 主要集成：MyBatis/MySQL、RabbitMQ、Redis/Redisson、Docker Compose 本地环境。
- 高风险变更：拼团锁单/结算、订单支付、退款、MQ 重复消息、外部服务调用、数据库映射与配置。

## 基本原则

- 先 Spec，后代码；先 Research，后设计。
- 每个结论必须有代码或配置出处。
- 跨项目变更必须明确调用方向、接口契约、消息契约、幂等策略和失败补偿。
- 不把一个子项目的实现细节泄漏到另一个子项目的领域层。
- 涉及资金、退款、订单状态、拼团状态、MQ、外部接口、数据库结构的变更必须标记高风险。
