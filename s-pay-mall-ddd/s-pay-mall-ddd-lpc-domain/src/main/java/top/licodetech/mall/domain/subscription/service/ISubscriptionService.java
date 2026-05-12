package top.licodetech.mall.domain.subscription.service;

import top.licodetech.mall.domain.order.model.entity.OrderEntity;
import top.licodetech.mall.domain.subscription.model.entity.FulfillmentResultEntity;
import top.licodetech.mall.domain.subscription.model.entity.SubscriptionFulfillmentTaskEntity;

import java.math.BigDecimal;
import java.util.List;

public interface ISubscriptionService {

    FulfillmentResultEntity fulfillOrder(String orderId);

    FulfillmentResultEntity processFulfillmentTask(SubscriptionFulfillmentTaskEntity taskEntity);

    List<SubscriptionFulfillmentTaskEntity> queryPendingFulfillmentTaskList(Integer pageSize);

    BigDecimal calculateRefundAmount(OrderEntity orderEntity);

    void revokeEntitlement(String orderId);

}
