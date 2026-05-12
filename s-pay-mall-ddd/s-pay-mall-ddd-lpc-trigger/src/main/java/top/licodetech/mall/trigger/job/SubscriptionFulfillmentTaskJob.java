package top.licodetech.mall.trigger.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.licodetech.mall.domain.order.service.IOrderService;
import top.licodetech.mall.domain.subscription.model.entity.FulfillmentResultEntity;
import top.licodetech.mall.domain.subscription.model.entity.SubscriptionFulfillmentTaskEntity;
import top.licodetech.mall.domain.subscription.service.ISubscriptionService;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
public class SubscriptionFulfillmentTaskJob {

    private static final int PAGE_SIZE = 20;

    @Resource
    private ISubscriptionService subscriptionService;

    @Resource
    private IOrderService orderService;

    @Scheduled(fixedDelay = 1000L)
    public void exec() {
        try {
            List<SubscriptionFulfillmentTaskEntity> taskEntities = subscriptionService.queryPendingFulfillmentTaskList(PAGE_SIZE);
            if (null == taskEntities || taskEntities.isEmpty()) {
                return;
            }

            for (SubscriptionFulfillmentTaskEntity taskEntity : taskEntities) {
                String orderId = taskEntity.getOrderId();
                try {
                    FulfillmentResultEntity resultEntity = subscriptionService.processFulfillmentTask(taskEntity);
                    if (resultEntity.isAutoRefund()) {
                        orderService.refundOrder(resultEntity.getUserId(), resultEntity.getOrderId());
                    }
                    log.info("AI服务套餐履约任务补偿 orderId:{} success:{} autoRefund:{}", orderId, resultEntity.isSuccess(), resultEntity.isAutoRefund());
                } catch (Exception e) {
                    log.error("AI服务套餐履约任务补偿失败 orderId:{}", orderId, e);
                }
            }
        } catch (Exception e) {
            log.error("处理AI服务套餐履约任务失败", e);
        }
    }
}
