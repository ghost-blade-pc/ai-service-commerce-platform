package top.licodetech.mall.infrastructure.adapter.repository;

import org.springframework.stereotype.Repository;
import top.licodetech.mall.domain.subscription.adapter.repository.ISubscriptionRepository;
import top.licodetech.mall.domain.subscription.model.entity.SubscriptionEntitlementEntity;
import top.licodetech.mall.domain.subscription.model.entity.SubscriptionFulfillmentTaskEntity;
import top.licodetech.mall.domain.subscription.model.valobj.EntitlementStatusVO;
import top.licodetech.mall.domain.subscription.model.valobj.FulfillmentTaskStatusVO;
import top.licodetech.mall.infrastructure.dao.ISubscriptionEntitlementDao;
import top.licodetech.mall.infrastructure.dao.ISubscriptionFulfillmentTaskDao;
import top.licodetech.mall.infrastructure.dao.po.SubscriptionEntitlement;
import top.licodetech.mall.infrastructure.dao.po.SubscriptionFulfillmentTask;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

@Repository
public class SubscriptionRepository implements ISubscriptionRepository {

    @Resource
    private ISubscriptionEntitlementDao entitlementDao;

    @Resource
    private ISubscriptionFulfillmentTaskDao fulfillmentTaskDao;

    @Override
    public void saveFulfillmentTask(SubscriptionFulfillmentTaskEntity taskEntity) {
        fulfillmentTaskDao.insertIgnore(buildTaskPO(taskEntity));
    }

    @Override
    public int lockFulfillmentTask(String orderId, String servicePackageId) {
        return fulfillmentTaskDao.lockTask(orderId, servicePackageId);
    }

    @Override
    public void markFulfillmentTaskSuccess(String orderId, String servicePackageId) {
        fulfillmentTaskDao.markSuccess(orderId, servicePackageId);
    }

    @Override
    public void markFulfillmentTaskRetry(String orderId, String servicePackageId, String failReason) {
        fulfillmentTaskDao.markRetry(orderId, servicePackageId, limitFailReason(failReason));
    }

    @Override
    public void markFulfillmentTaskFailed(String orderId, String servicePackageId, String failReason) {
        fulfillmentTaskDao.markFailed(orderId, servicePackageId, limitFailReason(failReason));
    }

    @Override
    public List<SubscriptionFulfillmentTaskEntity> queryPendingFulfillmentTaskList(Integer pageSize) {
        List<SubscriptionFulfillmentTask> taskList = fulfillmentTaskDao.queryPendingTaskList(pageSize);
        if (null == taskList || taskList.isEmpty()) {
            return Collections.emptyList();
        }
        return taskList.stream().map(this::buildTaskEntity).toList();
    }

    @Override
    public SubscriptionFulfillmentTaskEntity queryFulfillmentTask(String orderId, String servicePackageId) {
        return buildTaskEntity(fulfillmentTaskDao.queryTask(orderId, servicePackageId));
    }

    @Override
    public int openEntitlement(SubscriptionEntitlementEntity entitlementEntity) {
        return entitlementDao.insertIgnore(buildEntitlementPO(entitlementEntity));
    }

    @Override
    public SubscriptionEntitlementEntity queryEntitlementByOrderId(String orderId) {
        return buildEntitlementEntity(entitlementDao.queryByOrderId(orderId));
    }

    @Override
    public void revokeEntitlement(String orderId) {
        entitlementDao.revokeByOrderId(orderId);
    }

    private SubscriptionEntitlement buildEntitlementPO(SubscriptionEntitlementEntity entity) {
        return SubscriptionEntitlement.builder()
                .orderId(entity.getOrderId())
                .userId(entity.getUserId())
                .servicePackageId(entity.getServicePackageId())
                .totalQuota(entity.getTotalQuota())
                .usedQuota(entity.getUsedQuota())
                .remainingQuota(entity.getRemainingQuota())
                .status(null == entity.getStatus() ? null : entity.getStatus().getCode())
                .build();
    }

    private SubscriptionEntitlementEntity buildEntitlementEntity(SubscriptionEntitlement po) {
        if (null == po) {
            return null;
        }
        return SubscriptionEntitlementEntity.builder()
                .id(po.getId())
                .orderId(po.getOrderId())
                .userId(po.getUserId())
                .servicePackageId(po.getServicePackageId())
                .totalQuota(po.getTotalQuota())
                .usedQuota(po.getUsedQuota())
                .remainingQuota(po.getRemainingQuota())
                .status(EntitlementStatusVO.valueOfCode(po.getStatus()))
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .build();
    }

    private SubscriptionFulfillmentTask buildTaskPO(SubscriptionFulfillmentTaskEntity entity) {
        return SubscriptionFulfillmentTask.builder()
                .orderId(entity.getOrderId())
                .userId(entity.getUserId())
                .servicePackageId(entity.getServicePackageId())
                .totalQuota(entity.getTotalQuota())
                .status(null == entity.getStatus() ? null : entity.getStatus().getCode())
                .retryCount(entity.getRetryCount())
                .failReason(entity.getFailReason())
                .nextRetryTime(entity.getNextRetryTime())
                .build();
    }

    private SubscriptionFulfillmentTaskEntity buildTaskEntity(SubscriptionFulfillmentTask po) {
        if (null == po) {
            return null;
        }
        return SubscriptionFulfillmentTaskEntity.builder()
                .id(po.getId())
                .orderId(po.getOrderId())
                .userId(po.getUserId())
                .servicePackageId(po.getServicePackageId())
                .totalQuota(po.getTotalQuota())
                .status(FulfillmentTaskStatusVO.valueOfCode(po.getStatus()))
                .retryCount(po.getRetryCount())
                .failReason(po.getFailReason())
                .nextRetryTime(po.getNextRetryTime())
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .build();
    }

    private String limitFailReason(String failReason) {
        if (null == failReason) {
            return null;
        }
        return failReason.length() <= 512 ? failReason : failReason.substring(0, 512);
    }
}
