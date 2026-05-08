package top.licodetech.mall.trigger.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.licodetech.mall.domain.order.model.entity.RefundTaskEntity;
import top.licodetech.mall.domain.order.service.IOrderService;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
public class RefundTaskJob {

    private static final int PAGE_SIZE = 20;

    @Resource
    private IOrderService orderService;

    @Scheduled(cron = "0 0/1 * * * ?")
    public void exec() {
        try {
            log.info("任务；处理待补偿退款任务");
            List<RefundTaskEntity> refundTaskEntities = orderService.queryPendingRefundTaskList(PAGE_SIZE);
            if (null == refundTaskEntities || refundTaskEntities.isEmpty()) {
                return;
            }

            for (RefundTaskEntity refundTaskEntity : refundTaskEntities) {
                String orderId = refundTaskEntity.getOrderId();
                try {
                    boolean success = orderService.processRefundTask(refundTaskEntity);
                    log.info("退款任务补偿处理 orderId:{} success:{}", orderId, success);
                } catch (Exception e) {
                    log.error("退款任务补偿处理失败 orderId:{}", orderId, e);
                }
            }
        } catch (Exception e) {
            log.error("处理待补偿退款任务失败", e);
        }
    }

}
