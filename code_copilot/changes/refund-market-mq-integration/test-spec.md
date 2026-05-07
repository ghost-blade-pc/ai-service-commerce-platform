# 单测 Spec — 退单退款服务对接
> status: done
> created: 2026-05-07

## 0. 测试原则与选择策略

- **DDD 优先级**：本期优先覆盖 `domain` 的退单申请、退款成功确认、状态幂等规则。
- **测试比例**：以“单元测试 + Mock”为主；MyBatis SQL、RabbitMQ、真实数据库读写后续用少量 SpringBoot 集成测试补充。
- **本期约束**：退款暂时模拟成功，不接真实 `AlipayTradeRefundRequest`；因此不做支付宝沙箱退款集成测试。

## 1. 当前项目测试框架

当前项目已有两类测试：

- **JUnit4 + Mockito 单元测试**：参考 `s-pay-mall-ddd-lpc-app/src/test/java/top/licodetech/mall/test/domain/OrderServiceRefundTest.java`
- **JUnit4 + SpringBootTest 集成测试**：参考 `s-pay-mall-ddd-lpc-app/src/test/java/top/licodetech/mall/test/domain/OrderServiceTest.java`

| 项目 | 值 |
|------|-----|
| JUnit 版本 | JUnit4，使用 `org.junit.Test`、`org.junit.Assert` |
| Mock 框架 | Mockito，使用 `mock`、`when`、`verify` |
| Spring 测试 | `@RunWith(SpringRunner.class)` + `@SpringBootTest` |
| Maven 注意事项 | `s-pay-mall-ddd-lpc-app/pom.xml` 默认 `skipTests=true`，本期已改为可被 `-DskipTests=false` 覆盖 |

## 2. 测试类型选择

| 场景 | 推荐测试类型 | 本期处理 |
|------|--------------|----------|
| 测试订单状态流转 | 单元测试 + Mock | 已覆盖 `PAY_WAIT -> REFUNDING -> REFUNDED`、拼团订单 `PAY_SUCCESS -> REFUNDING` |
| 测试退款规则 | 单元测试 + Mock | 已覆盖非本人订单、非法状态、幂等状态 |
| 测试拼团锁单失败后的业务处理 | 单元测试 + Mock | 本期覆盖拼团退单成功；失败分支后续可补充 |
| 测试支付回调验签后的状态变化 | 单元测试 + Mock，必要时补集成测试 | 非本需求范围 |
| 测试 MyBatis mapper SQL 是否正确 | SpringBoot 集成测试 | 本期编译校验；真实 SQL 需测试库后补 |
| 测试 Spring Bean 是否能启动 | SpringBoot 集成测试 | 本期通过 Maven reactor 编译校验 |
| 测试真实数据库读写 | SpringBoot 集成测试 | 非本期 |
| 测试完整下单链路 | SpringBoot 集成测试 | 后续联调 RabbitMQ + MySQL + 拼团服务时补 |

### 2.1 分层推荐

| 分层 | 推荐方式 | 本期对象 |
|------|----------|----------|
| `domain` | 优先单元测试 + Mock | `OrderService#refundOrder`、`OrderService#changeOrderRefundSuccess`、`OrderStatusVO` |
| `infrastructure` | 需要时用 SpringBoot 集成测试 | `ProductPort#refundMarketPayOrder`、`OrderRepository`、`pay_order_mapper.xml` |
| `trigger/controller` | SpringBootTest 或 MockMvc | `AliPayController#refundOrder`、`RefundSuccessTopicListener#listener` |
| 完整业务链路 | 少量 SpringBoot 集成测试兜底 | 支付商城退单申请 -> 拼团退单 -> MQ -> 支付商城退款确认 |

## 3. 覆盖范围

### P0 — 核心业务逻辑

| 方法 | 场景 | 输入 | Mock 行为 | 预期结果 |
|------|------|------|-----------|---------|
| `OrderService#refundOrder` | 普通未支付订单退单 | `PAY_WAIT` 普通订单 | `changeOrderRefunding` 成功，二次查询为 `REFUNDING`，`changeOrderRefunded` 成功 | 返回 `REFUNDED` |
| `OrderService#refundOrder` | 拼团已支付订单退单 | `PAY_SUCCESS` + `GROUP_BUY_MARKET` | `IProductPort#refundMarketPayOrder` 返回 true，`changeOrderRefunding` 成功 | 返回 `REFUNDING` |
| `OrderService#refundOrder` | 非本人订单 | userId 不匹配 | 不更新仓储 | 抛业务异常 |
| `OrderService#refundOrder` | 非法状态 | `CLOSE` | 不更新仓储 | 抛业务异常 |
| `OrderService#changeOrderRefundSuccess` | MQ 确认退款成功 | `REFUNDING` 订单 | `changeOrderRefunded` 成功 | 返回 `REFUNDED` |
| `OrderService#changeOrderRefundSuccess` | MQ 先于本地 `REFUNDING` 到达 | `PAY_WAIT` 订单 | `changeOrderRefunding` 成功，`changeOrderRefunded` 成功 | 返回 `REFUNDED` |
| `OrderService#changeOrderRefundSuccess` | MQ 快于退单申请事务提交 | 首次查询 `PAY_WAIT`，重查后 `REFUNDING` | `changeOrderRefunding` 返回 0，短暂重查后 `changeOrderRefunded` 成功 | 返回 `REFUNDED`，不抛首次并发误报 |
| `OrderService#changeOrderRefundSuccess` | 重复 MQ | `REFUNDED` 订单 | 不再更新仓储 | 幂等返回 `REFUNDED` |

### P1 — 适配层

| 对象 | 场景 | 推荐测试类型 | 验证点 |
|------|------|--------------|--------|
| `ProductPort#refundMarketPayOrder` | 拼团响应转换 | 单元测试 + Mock Retrofit | `0000 + success/repeat` 成功；失败响应抛异常 |
| `pay_order_mapper.xml` | 状态 SQL | SpringBoot 集成测试 | 只有合法前置状态可进入 `REFUNDING`，只有 `REFUNDING` 可进入 `REFUNDED` |

### P2 — 入口层

| 对象 | 场景 | 推荐测试类型 | 验证点 |
|------|------|--------------|--------|
| `RefundSuccessTopicListener#listener` | 消息解析和领域调用 | 单元测试 + Mock / SpringBootTest | `outTradeNo` 优先于 `orderId`，异常触发 MQ 重试 |
| `AliPayController#refundOrder` | HTTP 响应 | MockMvc / SpringBootTest | 拼团订单申请后响应 `REFUNDING` |

### 不测试

- 本期不测试真实支付宝退款：用户已确认先模拟退款成功。
- 本期不测试真实 RabbitMQ 投递：当前任务重点是支付商城主链路代码落地，联调阶段再补。
- 本期不测试真实数据库读写：需要稳定测试库和初始化数据。

## 4. 执行计划

- [x] Step 1: 运行已有/目标测试，确认 POM 测试开关问题。
- [x] Step 2: 将 app 模块 `skipTests` 改为默认跳过但可用命令行覆盖。
- [x] Step 3: 扩展 `OrderServiceRefundTest` 覆盖 P0 领域状态流转。
- [x] Step 4: 运行支付商城目标单测。
- [x] Step 5: 运行拼团侧 reactor 编译校验。

## 5. 验证命令

```bash
mvn -pl s-pay-mall-ddd-lpc-app -am -DskipTests=false -DfailIfNoTests=false -Dtest=OrderServiceRefundTest test
mvn -pl group-buy-market-lpc-app -am -DskipTests -DfailIfNoTests=false test
```

## 6. 本次测试结论

| 测试类/项目 | 测试类型 | 命令 | 结果 | 备注 |
|-------------|----------|------|------|------|
| `OrderServiceRefundTest` | 单元测试 + Mock | `mvn -pl s-pay-mall-ddd-lpc-app -am -DskipTests=false -DfailIfNoTests=false -Dtest=OrderServiceRefundTest test` | 8 tests, 0 failures, 0 errors, BUILD SUCCESS | 覆盖核心退款状态流转和 MQ 先到收敛场景 |
| `OrderServiceRefundTest` | 单元测试 + Mock | `mvn -pl s-pay-mall-ddd-lpc-app -am -DskipTests=false -DfailIfNoTests=false -Dtest=OrderServiceRefundTest test` | 11 tests, 0 failures, 0 errors, BUILD SUCCESS | 追加覆盖 MQ 快于本地事务提交的短暂重试场景 |
| `group-buy-market-lpc-app` reactor | 编译校验 | `mvn -pl group-buy-market-lpc-app -am -DskipTests -DfailIfNoTests=false test` | BUILD SUCCESS | 测试被项目配置跳过，但主/测试源码均编译通过 |
