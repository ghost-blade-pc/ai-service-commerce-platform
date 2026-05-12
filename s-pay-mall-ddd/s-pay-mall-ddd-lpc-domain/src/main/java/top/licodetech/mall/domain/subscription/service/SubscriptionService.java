package top.licodetech.mall.domain.subscription.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.licodetech.mall.domain.order.adapter.repository.IOrderRepository;
import top.licodetech.mall.domain.order.model.entity.OrderEntity;
import top.licodetech.mall.domain.subscription.adapter.repository.ISubscriptionRepository;
import top.licodetech.mall.domain.subscription.model.entity.FulfillmentResultEntity;
import top.licodetech.mall.domain.subscription.model.entity.SubscriptionEntitlementEntity;
import top.licodetech.mall.domain.subscription.model.entity.SubscriptionFulfillmentTaskEntity;
import top.licodetech.mall.domain.subscription.model.valobj.EntitlementStatusVO;
import top.licodetech.mall.domain.subscription.model.valobj.FulfillmentTaskStatusVO;
import top.licodetech.mall.types.common.Constants;
import top.licodetech.mall.types.exception.AppException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import javax.annotation.Resource;

@Slf4j
@Service
public class SubscriptionService implements ISubscriptionService {

    private static final int DEFAULT_FULFILLMENT_TASK_PAGE_SIZE = 20;
    private static final int MAX_FULFILLMENT_TASK_PAGE_SIZE = 100;
    private static final int MAX_RETRY_COUNT = 3;
    private static final long AUTO_REFUND_SECONDS = 5L;
    private static final BigDecimal MIN_REFUND_AMOUNT = new BigDecimal("0.01");

    @Lazy
    @Resource
    private IOrderRepository orderRepository;

    @Resource
    private ISubscriptionRepository subscriptionRepository;

    @Override
    public FulfillmentResultEntity fulfillOrder(String orderId) {
        OrderEntity orderEntity = orderRepository.queryOrderByOrderId(orderId);
        if (null == orderEntity) {
            throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "订单不存在");
        }

        String servicePackageId = StringUtils.defaultIfBlank(orderEntity.getServicePackageId(), orderEntity.getProductId());
        SubscriptionFulfillmentTaskEntity taskEntity = SubscriptionFulfillmentTaskEntity.builder()
                .orderId(orderId)
                .userId(orderEntity.getUserId())
                .servicePackageId(servicePackageId)
                .totalQuota(orderEntity.getTotalQuota())
                .status(FulfillmentTaskStatusVO.PENDING)
                .retryCount(0)
                .build();
        subscriptionRepository.saveFulfillmentTask(taskEntity);
        return processFulfillmentTask(taskEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FulfillmentResultEntity processFulfillmentTask(SubscriptionFulfillmentTaskEntity taskEntity) {
        if (null == taskEntity || StringUtils.isBlank(taskEntity.getOrderId()) || StringUtils.isBlank(taskEntity.getServicePackageId())) {
            throw new AppException(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode(), "履约任务参数不能为空");
        }

        String orderId = taskEntity.getOrderId();
        String servicePackageId = taskEntity.getServicePackageId();
        SubscriptionFulfillmentTaskEntity latestTask = subscriptionRepository.queryFulfillmentTask(orderId, servicePackageId);
        if (null != latestTask && FulfillmentTaskStatusVO.SUCCESS.equals(latestTask.getStatus())) {
            return FulfillmentResultEntity.builder()
                    .orderId(orderId)
                    .userId(latestTask.getUserId())
                    .success(true)
                    .build();
        }

        int lockCount = subscriptionRepository.lockFulfillmentTask(orderId, servicePackageId);
        if (1 != lockCount) {
            return FulfillmentResultEntity.builder()
                    .orderId(orderId)
                    .userId(null == latestTask ? taskEntity.getUserId() : latestTask.getUserId())
                    .success(false)
                    .build();
        }

        try {
            OrderEntity orderEntity = orderRepository.queryOrderByOrderId(orderId);
            if (null == orderEntity) {
                throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "订单不存在");
            }
            Integer totalQuota = orderEntity.getTotalQuota();
            if (null == totalQuota || totalQuota <= 0) {
                throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "服务套餐额度不能为空");
            }

            subscriptionRepository.openEntitlement(SubscriptionEntitlementEntity.builder()
                    .orderId(orderId)
                    .userId(orderEntity.getUserId())
                    .servicePackageId(servicePackageId)
                    .totalQuota(totalQuota)
                    .usedQuota(0)
                    .remainingQuota(totalQuota)
                    .status(EntitlementStatusVO.ACTIVE)
                    .build());
            orderRepository.changeOrderDealDone(orderId);
            subscriptionRepository.markFulfillmentTaskSuccess(orderId, servicePackageId);
            log.info("AI服务套餐额度履约成功 orderId:{} servicePackageId:{} totalQuota:{}", orderId, servicePackageId, totalQuota);
            return FulfillmentResultEntity.builder()
                    .orderId(orderId)
                    .userId(orderEntity.getUserId())
                    .success(true)
                    .build();
        } catch (Exception e) {
            SubscriptionFulfillmentTaskEntity failedTask = subscriptionRepository.queryFulfillmentTask(orderId, servicePackageId);
            if (shouldAutoRefund(failedTask)) {
                subscriptionRepository.markFulfillmentTaskFailed(orderId, servicePackageId, e.getMessage());
                log.warn("AI服务套餐额度履约超过重试阈值，进入自动退款 orderId:{} servicePackageId:{}", orderId, servicePackageId, e);
                return FulfillmentResultEntity.builder()
                        .orderId(orderId)
                        .userId(null == failedTask ? taskEntity.getUserId() : failedTask.getUserId())
                        .success(false)
                        .autoRefund(true)
                        .build();
            }

            subscriptionRepository.markFulfillmentTaskRetry(orderId, servicePackageId, e.getMessage());
            log.warn("AI服务套餐额度履约失败，等待重试 orderId:{} servicePackageId:{}", orderId, servicePackageId, e);
            return FulfillmentResultEntity.builder()
                    .orderId(orderId)
                    .userId(null == failedTask ? taskEntity.getUserId() : failedTask.getUserId())
                    .success(false)
                    .build();
        }
    }

    @Override
    public List<SubscriptionFulfillmentTaskEntity> queryPendingFulfillmentTaskList(Integer pageSize) {
        int limit = null == pageSize ? DEFAULT_FULFILLMENT_TASK_PAGE_SIZE : pageSize;
        if (limit <= 0) {
            throw new AppException(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode(), "pageSize必须大于0");
        }
        return subscriptionRepository.queryPendingFulfillmentTaskList(Math.min(limit, MAX_FULFILLMENT_TASK_PAGE_SIZE));
    }

    @Override
    public BigDecimal calculateRefundAmount(OrderEntity orderEntity) {
        BigDecimal payAmount = null != orderEntity.getPayAmount() ? orderEntity.getPayAmount() : orderEntity.getTotalAmount();
        if (null == payAmount) {
            throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "退款金额不能为空");
        }

        SubscriptionEntitlementEntity entitlementEntity = subscriptionRepository.queryEntitlementByOrderId(orderEntity.getOrderId());
        if (null == entitlementEntity || null == entitlementEntity.getTotalQuota() || entitlementEntity.getTotalQuota() <= 0) {
            return payAmount.setScale(2, RoundingMode.HALF_UP);
        }

        int remainingQuota = null == entitlementEntity.getRemainingQuota() ? 0 : entitlementEntity.getRemainingQuota();
        if (remainingQuota <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal refundAmount = payAmount
                .multiply(BigDecimal.valueOf(remainingQuota))
                .divide(BigDecimal.valueOf(entitlementEntity.getTotalQuota()), 2, RoundingMode.HALF_UP);
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0 && refundAmount.compareTo(MIN_REFUND_AMOUNT) < 0) {
            return MIN_REFUND_AMOUNT;
        }
        return refundAmount;
    }

    @Override
    public void revokeEntitlement(String orderId) {
        subscriptionRepository.revokeEntitlement(orderId);
    }

    private boolean shouldAutoRefund(SubscriptionFulfillmentTaskEntity taskEntity) {
        if (null == taskEntity) {
            return false;
        }
        int retryCount = null == taskEntity.getRetryCount() ? 0 : taskEntity.getRetryCount();
        if (retryCount + 1 >= MAX_RETRY_COUNT) {
            return true;
        }
        return null != taskEntity.getCreateTime()
                && System.currentTimeMillis() - taskEntity.getCreateTime().getTime() >= AUTO_REFUND_SECONDS * 1000;
    }
}
