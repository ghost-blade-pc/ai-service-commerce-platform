# DDD 交易营销平台 — 面试复习文稿

> 基于 `s-pay-mall-ddd`（支付商城）与 `group-buy-market`（拼团营销）两个子项目的完整技术分析。
> 建议准备三个版本：30 秒一句话、3 分钟项目介绍、逐章深挖。

---

## 版本一：30 秒项目介绍

> 这个项目是一个基于 DDD 架构的交易营销平台，由支付商城和拼团营销两个独立服务组成。商城负责下单、支付、模拟发货/业务交付、退款，营销负责拼团锁单、成团结算、退单处理。两个服务之间通过 Retrofit 做同步调用、RabbitMQ 做异步解耦，结合本地消息表、定时补偿、状态机和多层幂等控制来保证跨系统最终一致性。我重点负责订单支付、拼团结算、退单补偿、MQ 消息链路和 DDD 分层设计。

---

## 版本二：3 分钟项目介绍

> 项目按业务边界拆为支付商城和拼团营销两个子系统。商城侧负责订单创建、支付宝支付、支付回调、模拟发货/业务交付和退款；营销侧负责拼团活动、锁单、结算、成团通知和退单规则。
>
> 正向流程：用户在商城下单时如果参与拼团，商城会同步调用营销服务完成锁单获取拼团优惠；支付成功后商城处理支付宝回调，同时通知营销侧结算订单；当拼团人数达到目标，营销侧通过 RabbitMQ 通知商城批量完成模拟发货/业务交付。
>
> 逆向流程：用户退单时，营销侧根据拼团状态和订单支付状态组合选择不同退单策略（未支付/已支付未成团/已支付已成团），之后通过 MQ 通知商城执行支付宝退款。
>
> 技术上采用 DDD 六边形架构，核心业务规则集中在 domain 层，MyBatis、Redis、RabbitMQ、Retrofit 等基础设施能力主要通过 infrastructure 适配。当前实现里 domain 仍有少量 Spring 注解和支付客户端侵入，这是后续可以继续收敛的工程优化点。核心业务中用责任链编排锁单/结算/退单校验规则，用策略模式处理不同退款状态，用状态机控制订单状态流转。
>
> 最大的难点是跨服务一致性——涉及商城订单、营销拼团单、支付宝支付单三方状态一致。方案是同步接口 + MQ 异步事件 + 本地消息表 + 定时补偿任务，同时用唯一索引、CAS 更新、Redis 分布式锁保证幂等和并发安全。

---

## 版本三：逐章深挖（面试追问用）

---

### 1. 项目背景与业务目标

**解决什么问题？**

一个同时支持普通商品购买和拼团营销的电商平台。用户可以选择直接购买商品，也可以参与拼团获得优惠。核心挑战在于：订单、支付、拼团三个领域的状态需要跨两个独立服务保持一致。

**为什么拆成两个系统？**

- **支付商城（s-pay-mall-ddd）**：关注订单生命周期、支付闭环、模拟发货/业务交付、退款。这是交易的核心链路，变化相对稳定。
- **拼团营销（group-buy-market）**：关注拼团活动规则、优惠试算、锁单、成团判定、退单回滚。营销规则变化频繁（不同活动、不同折扣、不同人数要求），独立部署不影响商城稳定性。
- **拆分后**：营销能力可以被其他业务线复用，商城不需要理解拼团规则细节。

**拆分边界**：
- 商城不知道拼团的"人数要求""折扣算法""成团逻辑"
- 营销不知道商城的"支付宝对接""交付逻辑"
- 两者通过约定好的 API 契约和 MQ 消息格式通信

**我负责的模块**：订单创建与支付流程、拼团锁单/结算/退单规则、MQ 消息链路、退款补偿任务、DDD 分层设计。

---

### 2. 系统整体架构

```
┌─────────────────────────────────────────────────────┐
│                   用户/前端                         │
└──────────┬──────────────────────┬───────────────────┘
           │                      │
           ▼                      ▼
   ┌───────────────┐     ┌────────────────┐
   │ s-pay-mall-ddd│     │group-buy-market│
   │  (支付商城)   │◄───►│  (拼团营销)    │
   │  port: 8080   │     │  port: 8091    │
   └───────┬───────┘     └───────┬────────┘
           │                     │
           │    ┌──────────────┐ │
           └───►│   RabbitMQ   │◄┘
                │  (5672)      │
                └──────────────┘
           │                     │
           ▼                     ▼
      ┌─────────┐          ┌─────────┐
      │  MySQL  │          │  MySQL  │
      │ (13306) │          │ (13306) │
      └─────────┘          └─────────┘
           │                     │
           ▼                     ▼
      ┌─────────┐          ┌─────────┐
      │  Redis  │          │  Redis  │
      │ (16379) │          │ (16379) │
      └─────────┘          └─────────┘
```

**两个服务各自的 DDD 模块结构**（完全一致）：

| 模块 | 职责 | 依赖方向 |
|------|------|---------|
| `api` | 对外接口契约、DTO、统一 Response | 最上层，被 trigger 实现 |
| `app` | 启动类、Spring 配置、MyBatis XML、测试 | 组装一切 |
| `domain` | 领域服务、聚合、实体、值对象、端口接口 | 主要承载业务规则，原则上避免基础设施细节 |
| `trigger` | HTTP Controller、MQ Listener、定时 Job | 依赖 domain + infrastructure |
| `infrastructure` | DAO/PO、Repository 实现、Gateway、MQ、Redis | 实现 domain 的端口接口 |
| `types` | 常量、枚举、异常、通用工具 | 被所有模块依赖 |

**同步通信**：Retrofit2 REST 调用
- 锁单：商城 → 营销，需要立即返回拼团优惠金额
- 结算：商城 → 营销，支付成功后推进拼团状态
- 退单：商城 → 营销，用户发起退款

**异步通信**：RabbitMQ Topic 模式
- `topic.order_pay_success`：支付成功事件（商城内部消费，触发模拟发货/业务交付）
- `topic.team_success`：成团事件（营销 → 商城，触发批量结算）
- `topic.team_refund`：退单事件（营销 → 商城，触发支付宝退款）

---

### 3. 核心业务流程

#### 3.1 正向流程：下单到成团交付

```
用户下单
  → AliPayController.create_pay_order
  → OrderService.createOrder (Template Method)
     → 1. 查询是否存在未支付订单（幂等复用）
     → 2. 查询商品信息（ProductPort → ProductRPC）
     → 3. 构建 CreateOrderAggregate，落库 pay_order（status=CREATE）
     → 4. 如果是拼团订单：
        → ProductPort.lockMarketPayOrder
        → Retrofit → 营销 MarketTradeController.lock_market_pay_order
        → 营销侧责任链：活动校验 → 用户限制 → 库存占用
        → 返回优惠金额（原价、抵扣、实付）
     → 5. 创建支付宝支付单（AlipayTradePagePayRequest）
     → 6. 更新 pay_order（status=PAY_WAIT, pay_url, 营销金额）

支付宝支付
  → 用户扫码支付

支付回调 / 补偿查询
  → AliPayController.payNotify（RSA256 验签）
  → 或 NoPayNotifyOrderJob 主动查询支付宝
  → OrderService.changeOrderPaySuccess(orderId, payTime)
     → 如果是普通订单：
        → pay_order → PAY_SUCCESS
        → 发布 topic.order_pay_success → OrderPaySuccessListener → DEAL_DONE
     → 如果是拼团订单：
        → pay_order → PAY_SUCCESS
        → ProductPort.settlementMarketPayOrder → 营销结算
        → 营销侧责任链：结算规则过滤 → 更新 group_buy_order_list → complete_count +1
        → 若达到 target_count：group_buy_order → 完成, notify_task 写入
        → TradeTaskService.execNotifyJob → EventPublisher 发布 topic.team_success

成团通知
  → 商城 TeamSuccessTopicListener 收到 topic.team_success
  → OrderService.changeOrderMarketSettlement(outTradeNoList)
  → 批量更新 pay_order → MARKET
  → 发布 topic.order_pay_success → OrderPaySuccessListener → DEAL_DONE
```

**关键一致性点**：
- 商城 `pay_order.order_id` ↔ 营销 `group_buy_order_list.out_trade_no` 是跨项目主关联键
- 支付回调可能丢失 → `NoPayNotifyOrderJob` 每分钟兜底查询
- 营销结算可能失败 → 支付宝回调重试 + Job 补偿间接兜底

#### 3.2 逆向流程：退单到支付宝退款

```
用户退单
  → AliPayController.refund_order
  → OrderService.refundOrder(userId, orderId)
     → 校验：订单存在、归属正确、状态可退
     → 如果是拼团订单：
        → ProductPort.refundMarketPayOrder → 营销退单
        → 营销侧责任链：数据查询 → 唯一性校验 → 退单执行
        → RefundTypeEnumVO.matches(拼团状态, 订单状态) 选择策略：
           ├─ UNPAID_UNLOCK：未支付未成团 → 释放锁单
           ├─ PAID_UNFORMED：已支付未成团 → 释放锁单 + 需退款
           └─ PAID_FORMED：已支付已成团 → 调整统计 + 需退款
        → notify_task 写入 → EventPublisher 发布 topic.team_refund
        → pay_order → REFUNDING

退单消息消费
  → 商城 RefundSuccessTopicListener 收到 topic.team_refund
  → 解析 type/outTradeNo → OrderService.receiveRefundSuccessMessage
  → pay_refund_task insertIgnore（幂等）
  → processRefundTask
     → lockRefundTask（CAS 抢占）
     → 根据 RefundTypeVO 判断是否需要支付渠道退款
        → unpaid_unlock：不需要，直接 REFUNDED
        → paid_unformed / paid_formed：需要 → RefundPort.refund
     → 成功 → markRefundTaskSuccess
     → 可重试失败 → markRefundTaskRetry（递增退避 1~10 分钟）
     → 永久失败 → markRefundTaskFailed
     → pay_order → REFUNDED

退款补偿
  → RefundTaskJob 每分钟扫描 PENDING/RETRY 或超时 PROCESSING 任务
  → OrderService.processRefundTask 重试
```

---

### 4. DDD 分层设计

**核心原则**：domain 层应聚焦领域模型、领域服务和端口接口，不直接依赖 Controller、DAO、Mapper、RabbitMQ Listener、Retrofit 网关等基础设施细节。

**当前实现说明**：两个子项目已经按 `api/app/domain/trigger/infrastructure/types` 拆分职责，但还不是严格意义上的 Pure Java domain。比如 `OrderService` 使用了 `@Service`、`@Transactional`、`@Value` 和 `AlipayClient`，部分领域工厂使用 `@Bean` 组装责任链。面试时可以把它讲成“DDD 分层落地 + 后续可继续做依赖收敛”，不要说成完全零框架依赖。

#### 4.1 各层职责

| 层 | 放什么 | 不放什么 |
|----|--------|---------|
| **api** | `IPayService`、`IMarketTradeService` 接口，`*RequestDTO`、`*ResponseDTO`、`Response<T>` | 业务逻辑 |
| **domain** | `IOrderService`、`OrderService`，聚合/实体/值对象，`IProductPort`、`IOrderRepository` 接口 | Controller、DAO、MQ 注解、Retrofit 网关 |
| **trigger** | `AliPayController`、`MarketTradeController`、`*Listener`、`*Job` | 核心业务规则——只做协议转换和编排 |
| **infrastructure** | `ProductPort`、`OrderRepository` 实现，`I*Dao`、`*PO`、`EventPublisher`、`IGroupBuyMarketService`(Retrofit) | 领域规则 |
| **types** | `Constants`、`ResponseCode`、`AppException` | 业务含义 |

#### 4.2 为什么 domain 不依赖 infrastructure？

通过**依赖倒置**：
- domain 定义接口 `IOrderRepository`、`IProductPort`
- infrastructure 实现这些接口
- Spring DI 在运行时注入实现

好处：
1. 领域规则可以优先做单元测试，降低对完整 Spring 容器和外部依赖的要求
2. 替换基础设施（如换 MQ、换数据库）不影响领域逻辑
3. 领域规则集中、可读性高——打开 domain 包就能理解全部业务

#### 4.3 聚合设计（面试重点准备）

**CreateOrderAggregate**（商城下单聚合）：
```java
// s-pay-mall-ddd-lpc-domain/.../model/aggregate/CreateOrderAggregate.java
public class CreateOrderAggregate {
    private String userId;           // 用户标识
    private ProductEntity productEntity;  // 商品实体
    private OrderEntity orderEntity;      // 订单实体（含订单号、状态、金额）
}
```
- 聚合了"创建一个订单"所需的全部信息
- 静态工厂方法 `buildOrderEntity()` 封装订单号生成和初始状态
- 一次 Repository 调用完成整个聚合的持久化

**GroupBuyOrderAggregate**（拼团锁单聚合）：
- 聚合了用户信息、活动信息、优惠信息、参与次数
- 锁单操作对这个聚合整体负责——要么全部成功，要么回滚库存

#### 4.4 实体 vs 值对象

| | 实体 (Entity) | 值对象 (Value Object) |
|---|---|---|
| **特征** | 有唯一标识，有生命周期 | 无标识，由属性值定义相等性 |
| **例子** | `OrderEntity`（有 orderId）、`ProductEntity` | `OrderStatusVO`、`MarketTypeVO`、`RefundTypeVO` |
| **状态** | 可变 | 不可变（枚举） |

**值对象的业务价值**：`OrderStatusVO` 不只是状态字符串，它封装了状态判断逻辑：
```java
// s-pay-mall-ddd-lpc-domain/.../valobj/OrderStatusVO.java
public enum OrderStatusVO {
    CREATE, PAY_WAIT, PAY_SUCCESS, DEAL_DONE, CLOSE, MARKET, REFUNDING, REFUNDED;

    public static boolean canRefund(String code) { ... }   // 哪些状态可以退款
    public static boolean isRefunding(String code) { ... }  // 是否退单中
    public static boolean isRefunded(String code) { ... }   // 是否已退单
}
```

**`RefundTypeEnumVO` 是最能体现 DDD 价值的值对象**：
```java
// group-buy-market-lpc-domain/.../valobj/RefundTypeEnumVO.java
public enum RefundTypeEnumVO {
    UNPAID_UNLOCK("unpaid_unlock", "unpaid2RefundStrategy", "未支付,未成团") {
        public boolean matches(GroupBuyOrderEnumVO g, TradeOrderStatusEnumVO t) {
            return GroupBuyOrderEnumVO.PROGRESS.equals(g)
                && TradeOrderStatusEnumVO.CREATE.equals(t);
        }
    },
    PAID_UNFORMED("paid_unformed", "paid2RefundStrategy", "已支付，未成团") { ... },
    PAID_FORMED("paid_formed", "paidTeam2RefundStrategy", "已支付，已成团") { ... };

    public abstract boolean matches(GroupBuyOrderEnumVO, TradeOrderStatusEnumVO);
}
```
- 每个枚举值自带**匹配规则**（双状态组合判断）
- 自带**策略 Bean 名称**（用于 Spring 注入）
- 把"哪种情况走哪个退单策略"的规则**编码在值对象中**，而不是散落在 Service 的 if-else 里

---

### 5. 核心设计模式

#### 5.1 责任链模式（面试主讲）

**用在哪**：拼团锁单、拼团结算、拼团退单的**规则校验编排**。

**为什么用**：这些业务场景需要按顺序经过多个校验规则，而且规则可能增减。责任链让每个规则独立、可复用、可插拔。

**实现方式**：基于 `xfg-wrench` 框架的 `BusinessLinkedList` + `LinkArmory`。

**锁单责任链**（TradeLockRuleFilterFactory）：

```
TradeLockRuleCommandEntity
  → ActivityUsabilityRuleFilter    // 活动是否可用（时间、状态）
  → UserTakeLimitRuleFilter        // 用户参与次数限制
  → TeamStockOccupyRuleFilter      // 拼团库存占用
  → TradeLockRuleFilterBackEntity  // 返回过滤结果
```

**结算责任链**（TradeSettlementRuleFilterFactory）：

```
TradeSettlementRuleCommandEntity
  → SCRuleFilter          // 渠道规则
  → OutTradeNoRuleFilter  // 订单号查重
  → SettableRuleFilter    // 是否可结算
  → EndRuleFilter         // 结算截止规则
  → TradeSettlementRuleFilterBackEntity
```

**退单责任链**（TradeRefundRuleFilterFactory）：

```
TradeRefundCommandEntity
  → DataNodeFilter          // 查询订单和拼团数据
  → UniqueRefundNodeFilter  // 退单唯一性校验
  → RefundOrderNodeFilter   // 执行退单（委托给策略模式）
  → TradeRefundBehaviorEntity
```

**工厂组装方式**：
```java
// TradeRefundRuleFilterFactory.java
@Bean("tradeRefundRuleFilter")
public BusinessLinkedList<...> tradeRefundRuleFilter(
        DataNodeFilter dataNodeFilter,
        UniqueRefundNodeFilter uniqueRefundNodeFilter,
        RefundOrderNodeFilter refundOrderNodeFilter) {
    LinkArmory<...> linkArmory = new LinkArmory<>("退单规则过滤链",
            dataNodeFilter, uniqueRefundNodeFilter, refundOrderNodeFilter);
    return linkArmory.getLogicLink();
}
```

**面试讲法**：
> 锁单、结算、退单各自需要经过多个校验步骤，而且后续可能增加新规则（比如风控规则、限购规则）。如果用 if-else，每个 Service 方法会越来越长。用责任链后，新增规则只需要写一个新的 Filter 类，然后在 Factory 的 `@Bean` 方法里加一行，不碰原有代码。每个 Filter 只做一件事，可以独立测试。

#### 5.2 策略模式

**用在哪**：不同退单场景下的处理策略。

**为什么用**：退单有三种情况——未支付未成团、已支付未成团、已支付已成团。它们的处理逻辑完全不同（是否调支付宝退款、是否恢复拼团库存、是否调整成团统计）。如果用 if-else，逻辑会交织在一起。

**实现**：
```java
// IRefundOrderStrategy.java
public interface IRefundOrderStrategy {
    void refundOrder(TradeRefundOrderEntity entity);      // 退单操作
    void reverseStock(TeamRefundSuccess teamRefundSuccess); // 库存恢复
}

// Unpaid2RefundStrategy  — 未支付：释放锁单，不调支付宝退款
// Paid2RefundStrategy    — 已支付未成团：释放锁单 + 需支付宝退款
// PaidTeam2RefundStrategy — 已支付已成团：调整统计 + 需支付宝退款
```

策略选择通过枚举 + Spring Bean 名称：
```java
// RefundTypeEnumVO.getRefundStrategy() 根据双状态匹配出对应枚举
// 枚举.code → Spring @Service("beanName") → Map<String, IRefundOrderStrategy>
```

**面试讲法**：
> 退单有三种情况，处理逻辑完全不同。我用策略模式来解决——定义一个 `IRefundOrderStrategy` 接口，三种情况各一个实现。选策略的逻辑放在 `RefundTypeEnumVO` 枚举的 `matches()` 方法里，根据拼团状态和订单状态双条件匹配。新增退单类型只需加一个枚举值和一个策略实现类，不碰已有代码。

#### 5.3 模板方法模式

**用在哪**：订单创建流程。

**为什么用**：下单流程骨架固定（查重 → 查商品 → 落库 → 锁单 → 创建支付），但具体步骤由子类实现，方便测试替换。

```java
// AbstractOrderService.java
public PayOrderEntity createOrder(ShopCartEntity shopCartEntity) {
    // 1. 查询未支付订单（幂等复用）
    // 2. 查询商品信息
    // 3. 构建聚合，落库
    this.doSaveOrder(orderAggregate);               // → 子类实现
    // 4. 如果是拼团 → 营销锁单
    this.lockMarketPayOrder(...);                    // → 子类实现
    // 5. 创建支付宝支付单
    this.doPrepayOrder(...);                         // → 子类实现
    // 6. 返回支付 URL
}

// OrderService.java — 生产实现
// 测试时可创建 TestOrderService 替换部分步骤
```

#### 5.4 工厂模式

用 `@Bean` 方法 + `LinkArmory` 组装责任链，将链条的构建逻辑集中管理，避免在业务代码中散落 `new` 和组装。

#### 5.5 建造者模式

项目中大量使用 Lombok `@Builder` 构建聚合、实体和 DTO，提升对象创建的可读性。需要注意，`@Builder` 本身不保证不可变性，实体对象仍然可以是可变的。

#### 5.6 观察者/事件模式

`EventPublisher` 封装 RabbitMQ 消息发布（强制持久化 `MessageDeliveryMode.PERSISTENT`）。商城侧当前主要由 Repository/基础设施适配层触发消息发布，领域服务通过 Repository/Port 间接触发，避免 Controller 或业务入口直接拼装 MQ 发送逻辑。

---

### 6. 订单与拼团状态机

#### 6.1 商城订单状态机（pay_order.status）

```
CREATE ──► PAY_WAIT ──► PAY_SUCCESS ──► MARKET ──► DEAL_DONE
  │                        │               │
  │                        │               │
  └──── CLOSE ◄────────────┘               │
                                           │
  REFUNDING ◄──────────────────────────────┘
     │
     └──► REFUNDED
```

| 状态 | 含义 | 触发条件 |
|------|------|---------|
| CREATE | 订单创建，尚未创建支付单 | 用户提交订单 |
| PAY_WAIT | 等待支付 | 支付宝支付单创建成功 |
| PAY_SUCCESS | 支付成功 | 支付宝回调/主动查询 |
| MARKET | 拼团营销结算完成 | 成团 MQ 消息到达 |
| DEAL_DONE | 模拟发货/业务交付完成 | 支付成功→交付监听器 |
| CLOSE | 超时关单 | 定时任务扫描超时订单 |
| REFUNDING | 退单中 | 用户发起退款 |
| REFUNDED | 已退单 | 退款完成 |

**状态前置条件**（编码在 `OrderStatusVO` 中）：
```java
canRefund(code):    PAY_WAIT / PAY_SUCCESS / MARKET / DEAL_DONE → true
isRefunding(code):  REFUNDING → true
isRefunded(code):   REFUNDED → true
```

#### 6.2 拼团订单状态机（group_buy_order.status）

```
0 (拼单中) ──► 1 (完成)
   │
   ├──► 2 (失败)
   │
   └──► 3 (完成但含退单)
```

#### 6.3 拼团订单明细状态机（group_buy_order_list.status）

```
0 (初始锁定) ──► 1 (消费完成)
    │
    └──► 2 (用户退单)
```

**面试讲法**：
> 状态机不只是文档上的状态枚举，而是编码在值对象里的业务规则。支付回调、MQ 消费、退款请求都可能重复到达，我通过状态前置条件拦截非法流转——比如已退款的订单再次收到退款消息不会重复退款，未支付订单不会去调支付宝退款接口。

---

### 7. 分布式一致性与补偿机制

**核心问题**：商城订单、营销拼团单、支付宝支付单分属三个系统，如何保证最终一致？

**总体方案**：不用强一致性分布式事务，而是**最终一致性 = 同步推进 + 异步事件 + 本地消息表 + 定时补偿**。

#### 7.1 同步调用（强依赖场景）

需要立即返回结果的操作用 Retrofit 同步 REST：
- **锁单**：用户下单时需要立即知道拼团优惠金额，同步调用
- **结算**：支付成功后需要推进营销状态，同步调用（失败时抛异常，依赖支付宝回调重试）
- **退单**：用户发起退款时同步调营销退单

#### 7.2 异步消息（解耦场景）

非强实时的后续动作用 RabbitMQ：
- 成团通知 → 商城模拟发货/业务交付
- 退单通知 → 商城退款

#### 7.3 本地消息表

**营销侧 notify_task 表**：

```sql
-- 关键字段
uuid            -- 业务幂等标识：teamId_notifyCategory_orderId
team_id         -- 拼团队伍ID
notify_type     -- HTTP / MQ
notify_mq       -- MQ路由键
parameter_json  -- 消息体 JSON
notify_status   -- 0待发送 / 1成功 / 2重试 / 3失败
notify_count    -- 重试次数
```

流程：
1. 业务操作（结算/退单）完成后，写入 notify_task
2. `TradeTaskService.execNotifyJob` 异步发送 MQ/HTTP
3. 成功 → 标记成功；失败 → 重试（超过 4 次标记失败）

**商城侧 pay_refund_task 表**：

```sql
-- 关键字段
order_id              -- 订单号，唯一索引 uq_order_id
status                -- PENDING / PROCESSING / SUCCESS / FAILED / RETRY
retry_count           -- 重试次数
next_retry_time       -- 下次重试时间
error_info            -- 错误信息
message               -- 原始 MQ 消息（用于解析退款类型）
```

关键 Mapper 操作：
```xml
<!-- insertIgnore：依赖 uq_order_id 防重复插入 -->
<insert id="saveRefundTask">insert ignore into pay_refund_task ...</insert>

<!-- lockRefundTask：CAS 抢占，PENDING/RETRY 或超时 PROCESSING → PROCESSING -->
<update id="lockRefundTask">
    update pay_refund_task set status='PROCESSING'
    where order_id=? and (status in ('PENDING','RETRY')
       or (status='PROCESSING' and next_retry_time < now()))
</update>

<!-- markRefundTaskRetry：递增退避 1~10 分钟 -->
<update id="markRefundTaskRetry">
    ... next_retry_time = date_add(now(),
        interval least(retry_count + 1, 10) minute)
</update>
```

#### 7.4 定时补偿任务

| 任务 | 所属项目 | 触发 | 作用 |
|------|---------|------|------|
| `NoPayNotifyOrderJob` | 商城 | 每分钟 | 查询支付宝未回调的支付订单，主动补单 |
| `TimeoutCloseOrderJob` | 商城 | 每 30 分钟 | 关闭超时未支付订单 |
| `RefundTaskJob` | 商城 | 每分钟 | 扫描待处理退款任务，执行支付宝退款 |
| `GroupBuyNotifyJob` | 营销 | 每天 | 回调通知任务补偿 |
| `TimeoutRefundJob` | 营销 | 每分钟 | 超时未支付拼团订单自动退单 |

营销侧两个 Job 使用 Redisson 分布式锁防多实例并发：
```java
// 分布式锁 key
group_buy_market_notify_job_exec
group_buy_market_timeout_refund_job_exec
```

商城侧当前没有给这几个定时 Job 统一加 Redisson 锁，其中 `RefundTaskJob` 主要依赖 `pay_refund_task.lockRefundTask` 的 CAS 抢占避免同一退款任务被多个实例同时处理。

---

### 8. 并发安全与幂等（面试重点）

**核心问题**：MQ 重复消费、支付回调重复到达、定时任务多实例并发、用户重复点击退款。

**五层幂等防护**：

#### 第一层：数据库唯一索引

| 表 | 唯一索引 | 作用 |
|----|---------|------|
| `pay_order` | `uq_order_id(order_id)` | 同一订单号只能有一条记录 |
| `pay_refund_task` | `uq_order_id(order_id)` | 同一订单的退款任务只插一条（`insertIgnore`） |
| `group_buy_order` | `uq_team_id(team_id)` | 队伍唯一 |
| `group_buy_order_list` | `uq_order_id(order_id)` | 营销订单唯一 |

#### 第二层：状态机前置条件

```java
// 退款前检查
if (OrderStatusVO.isRefunded(code)) return orderEntity;     // 已退款直接返回
if (!OrderStatusVO.canRefund(code)) throw AppException(...);  // 不可退款抛异常
```

#### 第三层：CAS 更新

```java
// 退款状态更新
int updateCount = repository.changeOrderRefunding(userId, orderId);
if (1 != updateCount) {
    // 更新失败 → 重新查询 → 判断是否已被其他线程处理
    return queryRefundingOrRefundedOrder(orderId, errorInfo);
}
```

`changeOrderRefunding` 的 SQL 带状态条件，重复执行只影响 0 行。

#### 第四层：Redis 分布式锁

```java
// 拼团库存恢复防重复
String lockKey = "refund_lock_" + orderId;
repository.refund2AddRecovery(recoveryTeamStockKey, orderId); // 内部使用 Redis 锁
```

#### 第五层：本地消息表 + 重试退避

- `pay_refund_task.insertIgnore` → 同一订单不会重复创建任务
- `lockRefundTask` CAS 抢占 → 同一任务不会被多个线程同时处理
- 递增退避 `least(retry_count + 1, 10)` 分钟 → 避免失败任务频繁重试

**面试讲法**：
> 幂等不是靠一个方案搞定的，而是多层防护。唯一索引兜底数据库层，状态机兜底业务层，CAS 兜底并发，分布式锁兜底跨实例，消息表兜底跨服务。每层解决不同层面的重复问题。

---

### 9. 支付宝支付与退款

#### 9.1 支付下单

```java
// OrderService.doPrepayOrder
AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
bizContent.put("out_trade_no", orderId);       // 商户订单号
bizContent.put("total_amount", payAmount);      // 金额（拼团用优惠后金额）
bizContent.put("subject", productName);
bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
String form = alipayClient.pageExecute(request).getBody(); // 返回 HTML 表单
```

#### 9.2 回调验签

```java
// AliPayController.payNotify
// 1. 只处理 TRADE_SUCCESS
if (!"TRADE_SUCCESS".equals(trade_status)) return "false";

// 2. RSA256 验签
boolean ok = AlipaySignature.rsa256CheckContent(content, sign, alipayPublicKey, "UTF-8");
if (!ok) return "false";

// 3. 推进订单状态
orderService.changeOrderPaySuccess(tradeNo, payTime);
```

#### 9.3 双向补偿

```
异步回调（主路径）  +  主动查询（兜底）
AliPayController     NoPayNotifyOrderJob (每分钟)
```

**为什么需要主动查询**：支付宝异步通知可能延迟、丢失或被防火墙拦截。主动查询作为兜底保证支付状态最终推进。

#### 9.4 退款

- 支付渠道退款通过 `RefundPort.refund(orderId, refundAmount)` 调用支付宝退款接口。
- 普通订单退款由商城侧直接申请退款并推进 `REFUNDED`。
- 拼团退单 MQ 到达商城后，通过 `pay_refund_task` 记录和补偿退款任务，`RefundTaskJob` 每分钟扫描可处理任务。

---

### 10. 动态配置中心（DCC）

基于 Redis Pub/Sub 的动态配置推送机制。

**实现原理**：
```
DCCController.update_config(key, value)
  → RTopic.publish(new AttributeVO(key, value))
  → DCCService 中的 @DCCValue 字段热更新
```

**应用场景**：

| 配置项 | 用途 |
|--------|------|
| `downgradeSwitch` | 降级开关：0 关闭 / 1 开启 |
| `cutRange` | 灰度切量：按 userId hash 取模控制流量百分比 |
| `scBlacklist` | 渠道黑名单：拦截指定 source+channel 组合 |
| `cacheSwitch` | 缓存开关：控制是否启用缓存 |

```java
// DCCService.java
@DCCValue("downgradeSwitch:0")
private String downgradeSwitch;  // 运行时通过 Redis 消息热更新

public boolean isCutRange(String userId) {
    int hashCode = Math.abs(userId.hashCode());
    int lastTwoDigits = hashCode % 100;
    return lastTwoDigits <= Integer.parseInt(cutRange); // 灰度切量
}
```

**面试讲法**：
> 不需要重启服务就能调整降级开关、灰度比例、黑名单。线上出了问题可以秒级关掉某个功能或切走流量。技术实现很简单——Redis pub/sub + 注解驱动的字段更新。

---

### 11. 项目亮点总结（面试主动讲的部分）

**按优先级排列**：

1. **跨服务最终一致性方案**：同步 REST + 异步 MQ + 本地消息表 + 定时补偿，四个组件协同工作，不依赖强一致性分布式事务。支付回调丢失有支付宝重试和 `NoPayNotifyOrderJob` 兜底；营销成团/退单通知通过 `notify_task` 做任务化发送；拼团退款落到商城后由 `pay_refund_task` 补偿。当前商城内部支付成功消息、RabbitMQ confirm/return、死信队列和独立结算补偿仍是可继续增强的点。

2. **多层幂等防护**：唯一索引 → 状态机前置 → CAS 更新 → 分布式锁 → 本地消息表。五层防护分别解决不同层面的重复问题。

3. **责任链 + 策略模式协同**：锁单/结算/退单的规则编排用责任链（规则可插拔），退单的具体处理用策略模式（行为可扩展）。不是堆砌设计模式，而是用在最合适的场景。

4. **DDD 落地实践**：六模块六边形架构，domain 层集中承载业务规则，聚合建模业务操作，值对象封装业务规则（`RefundTypeEnumVO.matches()` 双状态匹配），Repository/Port 依赖倒置。当前还有少量 Spring 与支付客户端侵入，可以作为后续依赖收敛方向。

5. **状态机驱动开发**：核心业务流程由状态机建模，退款等关键链路把状态前置条件编码在值对象和 SQL 条件中。成团结算、模拟发货等链路当前还可以继续补强状态条件，避免重复消息造成不必要的重复更新。

6. **SpecAI 工程化开发流程**：开发前先写 Spec（需求分析、任务拆分、测试点），开发后沉淀知识（`platform-knowledge.md`）。代码变更可追溯、可 Review、可归档。

---

### 12. 高频追问准备

#### Q1: 支付回调丢失了怎么办？
> 两个兜底：一是支付宝本身有重试机制（间隔递增），二是商城的 `NoPayNotifyOrderJob` 每分钟查询未支付订单，主动调支付宝查询接口确认支付状态。只要两个兜底有一个生效，订单就能推进。

#### Q2: 如果支付成功后，调营销结算失败了怎么办？
> 当前实现是抛异常让支付宝回调重试。因为支付宝会多次回调，只要有一次结算成功就行。如果回调全部失败，可以补充独立的结算补偿 Job——检查拼团订单支付成功超过 N 分钟仍未结算的，主动调营销结算。这是 `platform-knowledge.md` 中标注的待优化点。

#### Q3: 成团消息重复消费怎么办？
> 当前 `changeOrderMarketSettlement(outTradeNoList)` 会把订单批量更新到 `MARKET`，然后逐笔发布 `topic.order_pay_success` 触发模拟发货/交付。需要注意，现有 SQL 没有显式限制“只有 PAY_SUCCESS 才能更新到 MARKET”，`changeOrderDealDone` 也会直接更新 `DEAL_DONE`，所以不能把它说成已经完全靠状态前置防住重复消息。更稳妥的回答是：当前重复消息通常不会生成新的订单或退款任务，但成团结算和交付链路还可以补状态条件或独立幂等记录，这是我会优先优化的点。

#### Q4: 退款补偿任务被多个实例同时执行怎么办？
> `lockRefundTask` 用 CAS 更新把可处理任务置为 `PROCESSING`，只有抢占成功的实例会继续执行；没抢到的实例 update 返回 0，直接跳过。商城侧当前 `RefundTaskJob` 没有 Redisson 分布式锁，任务级并发主要靠这条 CAS 更新兜底。营销侧的 `GroupBuyNotifyJob`、`TimeoutRefundJob` 才使用 Redisson 锁防止多实例重复执行。

#### Q5: 为什么要拆成两个项目而不是一个？
> 支付商城和拼团营销的业务变化频率不同——营销活动规则经常变（折扣、人数、有效期），商城交易链路相对稳定。拆开后营销变更不影响商城稳定性。而且营销能力可以单独被其他业务线复用。

#### Q6: DDD 里 domain 层怎么做到不依赖 Spring？
> domain 层先定义接口（`IOrderRepository`、`IProductPort`），infrastructure 层实现这些接口并加 `@Component`，Spring 在运行时通过 DI 把实现注入给 domain 的 Service。当前项目还不是完全无 Spring 依赖：domain service 和部分领域工厂用了 `@Service/@Bean/@Transactional/@Resource`，商城 `OrderService` 还直接使用了 `AlipayClient`。所以准确讲法是：依赖方向已经按端口做了隔离，核心业务规则集中在 domain；后续可以继续把支付客户端、配置注入和事务边界下沉到 application/infrastructure，进一步收敛为更纯的领域层。

#### Q7: 本地消息表和事务消息有什么区别？
> 本项目用的是本地消息表方案：业务操作和消息写入在同一个本地事务中完成，然后异步发送。RocketMQ 的事务消息可以省掉本地消息表，但项目用的是 RabbitMQ，不支持事务消息，所以本地消息表是合理的替代。好处是实现简单、可排查；代价是多一张表和补偿 Job。

#### Q8: Redis 锁怎么保证锁过期时间合理？
> Redisson 的 `RLock` 默认有 watch dog 机制，会自动续期。对于定时 Job，锁的持有时间通常很短（秒级），不需要设置很长的过期时间。如果 Job 执行超时，watch dog 会保证锁不被误释放。

---

### 附录：关键文件索引（备查）

```
## 商城核心文件
s-pay-mall-ddd/s-pay-mall-ddd-lpc-domain/.../order/service/
  ├── IOrderService.java              # 订单服务接口
  ├── AbstractOrderService.java       # 模板方法骨架
  └── OrderService.java              # 订单核心实现（支付/退款/补偿）
s-pay-mall-ddd/s-pay-mall-ddd-lpc-domain/.../order/model/
  ├── aggregate/CreateOrderAggregate.java  # 下单聚合
  ├── entity/OrderEntity.java
  └── valobj/OrderStatusVO.java          # 订单状态机
s-pay-mall-ddd/s-pay-mall-ddd-lpc-infrastructure/.../
  ├── adapter/port/ProductPort.java       # 营销调用适配器
  ├── adapter/repository/OrderRepository.java
  ├── gateway/IGroupBuyMarketService.java  # Retrofit 接口
  └── event/EventPublisher.java
s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/.../
  ├── http/AliPayController.java          # 支付/订单/退款入口
  ├── listener/OrderPaySuccessListener.java
  ├── listener/TeamSuccessTopicListener.java
  ├── listener/RefundSuccessTopicListener.java
  └── job/NoPayNotifyOrderJob.java

## 营销核心文件
group-buy-market/group-buy-market-lpc-domain/.../trade/service/
  ├── lock/TradeLockOrderService.java          # 锁单服务
  ├── lock/filter/ActivityUsabilityRuleFilter.java
  ├── lock/filter/UserTakeLimitRuleFilter.java
  ├── lock/filter/TeamStockOccupyRuleFilter.java
  ├── settlement/TradeSettlementOrderService.java # 结算服务
  ├── refund/TradeRefundOrderService.java        # 退单服务
  ├── refund/business/IRefundOrderStrategy.java  # 退单策略接口
  ├── refund/business/impl/Unpaid2RefundStrategy.java
  ├── refund/business/impl/Paid2RefundStrategy.java
  ├── refund/business/impl/PaidTeam2RefundStrategy.java
  └── task/TradeTaskService.java                # 通知任务服务
group-buy-market/group-buy-market-lpc-domain/.../trade/model/
  └── valobj/RefundTypeEnumVO.java              # 退单类型枚举（策略选择）
group-buy-market/group-buy-market-lpc-trigger/.../
  ├── http/MarketTradeController.java           # 营销交易入口
  ├── listener/RefundSuccessTopicListener.java
  └── job/TimeoutRefundJob.java
group-buy-market/group-buy-market-lpc-infrastructure/.../
  ├── dcc/DCCService.java                      # 动态配置
  └── event/EventPublisher.java
```

---

> 最后更新：2026-05-09
> 建议：面试前通读 3 分钟版本，然后逐章过一遍追问准备。重点背 5 个故事：拆分原因、锁单链路、支付一致性、退单策略、幂等方案。
