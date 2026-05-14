# 订阅权益履约与退款补偿

> 来源：`ai-service-subscription-platform` change  
> 范围：`s-pay-mall-ddd`  
> 最后更新：2026-05-12

## 1. 权益开通幂等模式

```
支付成功/成团消息 → OrderPaySuccessListener → GoodsService.changeOrderDealDone
  → SubscriptionService.fulfillOrder(orderId)
    → saveFulfillmentTask (INSERT IGNORE)
    → processFulfillmentTask
      → 已 SUCCESS? → 幂等返回 ✅
      → lockFulfillmentTask (UPDATE status='PROCESSING' WHERE status IN ('PENDING','RETRY'))
      → openEntitlement (INSERT IGNORE, uq_order_package 唯一索引)
      → changeOrderDealDone (UPDATE pay_order SET status='DEAL_DONE')
      → markFulfillmentTaskSuccess
```

关键设计：

- **`subscription_entitlement`** 表：`UNIQUE KEY uq_order_package (order_id, service_package_id)`，`INSERT IGNORE` 保证重复消费不重复开通。
- **`subscription_fulfillment_task`** 表：同样 `uq_order_package` 唯一索引；`lockTask` 用条件 UPDATE（`status IN ('PENDING','RETRY') AND next_retry_time <= now()`）防止并发重复处理。
- **顺序**：lock → open → done → success。任一步骤失败，事务回滚后 lock 被撤销，下次 job/重试重新争抢。

## 2. 履约任务重试与自动退款

```
processFulfillmentTask 失败
  → 查询当前 retry_count
  → shouldAutoRefund?
      retryCount + 1 >= MAX_RETRY_COUNT (3)  → 自动退款
      OR createTime + 5s >= now               → 自动退款
  → 是 → markFulfillmentTaskFailed，返回 autoRefund=true
  → 否 → markFulfillmentTaskRetry (retry_count+1, next_retry_time=now+1s)

SubscriptionFulfillmentTaskJob (fixedDelay=1000ms)
  → queryPendingFulfillmentTaskList
  → processFulfillmentTask
  → autoRefund? → orderService.refundOrder(userId, orderId)
```

关键设计：

- 自动退款复用既有 `OrderService#refundOrder`，不新增独立的资金退款状态机。
- `lockTask` SQL 包含超时兜底：`OR (status='PROCESSING' AND update_time < now() - 5s)`，防止任务僵死。
- Job 每分钟扫描一次，每个任务独立 try-catch，一个失败不影响其他。

## 3. 按比例退款计算

```java
// SubscriptionService.calculateRefundAmount
BigDecimal payAmount = orderEntity.getPayAmount() ?: orderEntity.getTotalAmount();
SubscriptionEntitlementEntity entitlement = queryEntitlementByOrderId(orderId);
if (entitlement == null) return payAmount; // 未开通权益，全额退款

int remainingQuota = entitlement.getRemainingQuota();
if (remainingQuota <= 0) return BigDecimal.ZERO; // 额度已耗尽

BigDecimal refundAmount = payAmount
    .multiply(BigDecimal.valueOf(remainingQuota))
    .divide(BigDecimal.valueOf(entitlement.getTotalQuota()), 2, RoundingMode.HALF_UP);

if (refundAmount > 0 && refundAmount < 0.01) return new BigDecimal("0.01");
return refundAmount;
```

规则：

- 退款金额 = 实付金额 × (剩余额度 / 总额度)
- 保留 2 位小数，四舍五入（`RoundingMode.HALF_UP`）
- 最低退款金额 0.01 元
- 已消耗额度从商城侧 `subscription_entitlement` 表读取，不依赖外部 AI Agent 项目
- 退款成功后调用 `revokeEntitlement`（UPDATE `remaining_quota=0, status='REVOKED'` WHERE `status='ACTIVE'`）

## 4. 相关文件

| 层 | 文件 |
|----|------|
| domain 端口 | `ISubscriptionService`、`ISubscriptionRepository` |
| domain 实体 | `SubscriptionEntitlementEntity`、`SubscriptionFulfillmentTaskEntity`、`FulfillmentResultEntity` |
| domain 值对象 | `EntitlementStatusVO`(ACTIVE/REVOKED)、`FulfillmentTaskStatusVO`(PENDING/PROCESSING/RETRY/SUCCESS/FAILED) |
| infrastructure | `SubscriptionRepository`、`ISubscriptionEntitlementDao`、`ISubscriptionFulfillmentTaskDao` |
| mapper | `subscription_entitlement_mapper.xml`、`subscription_fulfillment_task_mapper.xml` |
| trigger | `OrderPaySuccessListener`、`SubscriptionFulfillmentTaskJob` |
| SQL | `subscription_entitlement` 表、`subscription_fulfillment_task` 表 |
