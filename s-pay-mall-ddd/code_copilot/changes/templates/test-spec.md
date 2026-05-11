# 单测 Spec — 需求名称
> status: propose | apply | done
> created: YYYY-MM-DD

## 0. 测试原则与选择策略

- **Red/Green TDD**：测试必须先 Red 再 Green，跳过 Red 的测试无法证明有效
- **First Run the Tests**：开始前先跑已有测试套件，了解框架和基线
- **展示工作**：必须展示 Maven test 实际输出，禁止只写"测试通过"
- **DDD 优先级**：优先覆盖 `domain` 业务规则，其次覆盖 `infrastructure` 映射/网关失败，再覆盖 `trigger` 入参和响应转换
- **测试比例**：以“单元测试 + Mock”为主，以“SpringBoot 集成测试”为辅

## 1. 当前项目测试框架

当前项目已有两类测试：

- **JUnit4 + Mockito 单元测试**：参考 `s-pay-mall-ddd-lpc-app/src/test/java/top/licodetech/mall/test/domain/OrderServiceRefundTest.java`
- **JUnit4 + SpringBootTest 集成测试**：参考 `s-pay-mall-ddd-lpc-app/src/test/java/top/licodetech/mall/test/domain/OrderServiceTest.java`

| 项目 | 值 |
|------|-----|
| JUnit 版本 | JUnit4，使用 `org.junit.Test`、`org.junit.Assert` |
| Mock 框架 | Mockito，使用 `mock`、`when`、`verify` |
| Spring 测试 | `@RunWith(SpringRunner.class)` + `@SpringBootTest` |
| Maven 注意事项 | `s-pay-mall-ddd-lpc-app/pom.xml` 当前配置了 `skipTests=true`，需要按实际命令显式开启测试 |

## 2. 测试类型选择

| 场景 | 推荐测试类型 | 说明 |
|------|--------------|------|
| 测试订单状态流转 | 单元测试 + Mock | 直接构造领域服务，Mock `IOrderRepository`、`IProductPort` 等端口 |
| 测试退款规则 | 单元测试 + Mock | 覆盖订单归属、可退款状态、异常分支、Repository 调用 |
| 测试拼团锁单失败后的业务处理 | 单元测试 + Mock | Mock 拼团营销端口返回失败/异常，验证订单处理策略 |
| 测试支付回调验签后的状态变化 | 单元测试 + Mock，必要时补集成测试 | 业务状态变化用 Mock；验签/参数解析复杂时补充少量集成测试 |
| 测试 MyBatis mapper SQL 是否正确 | SpringBoot 集成测试 | 需要真实或测试数据库，验证 SQL、字段映射、分页/条件 |
| 测试 Spring Bean 是否能启动 | SpringBoot 集成测试 | 验证 Bean 装配、配置绑定、启动上下文 |
| 测试真实数据库读写 | SpringBoot 集成测试 | 验证 DAO、Repository、事务和数据库数据 |
| 测试完整下单链路 | SpringBoot 集成测试 | 少量兜底，避免依赖过多外部服务导致不稳定 |

### 2.1 分层推荐

| 分层 | 推荐方式 | 典型对象 |
|------|----------|----------|
| `domain` | 优先单元测试 + Mock | `OrderService`、`AbstractOrderService`、`OrderStatusVO` |
| `infrastructure` | 需要时用 SpringBoot 集成测试 | `OrderRepository`、`IOrderDao`、MyBatis mapper、外部 gateway 适配 |
| `trigger/controller` | SpringBootTest 或 MockMvc | `AliPayController`、`LoginController`、Listener、Job |
| 完整业务链路 | 少量 SpringBoot 集成测试兜底 | 创建订单、支付回调、拼团结算、退款链路 |

## 3. 覆盖范围

### P0 — 核心业务逻辑（必须覆盖，优先单元测试 + Mock）

#### 类名: XxxDomainService / XxxRepository / XxxPort

| 方法 | 场景 | 输入 | Mock 行为 | 预期结果 |
|------|------|------|-----------|---------|

### P1 — DDD 适配层（按需单元测试或集成测试）

| 对象 | 场景 | 推荐测试类型 | 验证点 |
|------|------|--------------|--------|
| Repository | PO ↔ Entity 映射 | 单元测试 / SpringBoot 集成测试 | 字段转换、空值、状态枚举 |
| MyBatis mapper | SQL 正确性 | SpringBoot 集成测试 | 查询条件、更新行数、分页、字段映射 |
| Port/Gateway | 外部返回 ↔ 领域对象转换 | 单元测试 + Mock | 成功、失败、空响应、异常 |

### P2 — 入口层/协议层（按需 SpringBootTest 或 MockMvc）

| 对象 | 场景 | 推荐测试类型 | 验证点 |
|------|------|--------------|--------|
| Controller | 参数校验和 Response | MockMvc / SpringBootTest | code、info、data、异常响应 |
| MQ Listener | 消息解析和幂等入口 | 单元测试 + Mock / SpringBootTest | 重复消息、异常消息、领域服务调用 |
| Job | 定时补偿逻辑 | 单元测试 + Mock | 查询条件、批处理、失败处理 |

### 不测试（明确列出原因）

- [ ] 不测试项 1：原因

## 4. 执行计划

- [ ] Step 1: 运行已有测试，确认基线
- [ ] Step 2: 根据场景选择测试类型：单元测试 + Mock / SpringBoot 集成测试 / MockMvc
- [ ] Step 3: 生成 P0 领域测试，确认 Red，再实现到 Green
- [ ] Step 4: 按风险补充 P1/P2 测试
- [ ] Step 5: 运行本需求相关测试，记录 Maven 输出
- [ ] Step 6: 如涉及 MyBatis、配置或完整链路，再运行对应集成测试

## 5. 建议命令

> 由于 `s-pay-mall-ddd-lpc-app/pom.xml` 当前配置了 `skipTests=true`，执行测试时建议显式加 `-DskipTests=false`。

```bash
mvn -pl s-pay-mall-ddd-lpc-app -DskipTests=false test
mvn -pl s-pay-mall-ddd-lpc-app -DskipTests=false -Dtest=OrderServiceRefundTest test
mvn -pl s-pay-mall-ddd-lpc-app -DskipTests=false -Dtest=OrderServiceTest test
```

## 6. 本次测试结论

| 测试类 | 测试类型 | 命令 | 结果 | 备注 |
|--------|----------|------|------|------|
| | 单元测试 + Mock / SpringBoot 集成测试 / MockMvc | | | |
