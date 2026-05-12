package top.licodetech.mall.domain.subscription.adapter.repository;

import top.licodetech.mall.domain.subscription.model.entity.SubscriptionEntitlementEntity;
import top.licodetech.mall.domain.subscription.model.entity.SubscriptionFulfillmentTaskEntity;

import java.util.List;

public interface ISubscriptionRepository {

    void saveFulfillmentTask(SubscriptionFulfillmentTaskEntity taskEntity);

    int lockFulfillmentTask(String orderId, String servicePackageId);

    void markFulfillmentTaskSuccess(String orderId, String servicePackageId);

    void markFulfillmentTaskRetry(String orderId, String servicePackageId, String failReason);

    void markFulfillmentTaskFailed(String orderId, String servicePackageId, String failReason);

    List<SubscriptionFulfillmentTaskEntity> queryPendingFulfillmentTaskList(Integer pageSize);

    SubscriptionFulfillmentTaskEntity queryFulfillmentTask(String orderId, String servicePackageId);

    int openEntitlement(SubscriptionEntitlementEntity entitlementEntity);

    SubscriptionEntitlementEntity queryEntitlementByOrderId(String orderId);

    void revokeEntitlement(String orderId);

}
