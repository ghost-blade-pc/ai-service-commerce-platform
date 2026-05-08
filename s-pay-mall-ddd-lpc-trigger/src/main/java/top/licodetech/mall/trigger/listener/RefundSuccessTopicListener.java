package top.licodetech.mall.trigger.listener;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import top.licodetech.mall.domain.order.service.IOrderService;

import javax.annotation.Resource;

@Slf4j
@Component
public class RefundSuccessTopicListener {

    @Resource
    private IOrderService orderService;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "${spring.rabbitmq.config.consumer.topic_team_refund.queue}"),
                    exchange = @Exchange(value = "${spring.rabbitmq.config.consumer.topic_team_refund.exchange}", type = ExchangeTypes.TOPIC),
                    key = "${spring.rabbitmq.config.consumer.topic_team_refund.routing_key}"
            )
    )
    public void listener(String message) {
        try {
            log.info("收到拼团退单成功消息 {}", message);
            TeamRefundSuccessMessage refundSuccessMessage = JSON.parseObject(message, TeamRefundSuccessMessage.class);
            String orderId = StringUtils.isNotBlank(refundSuccessMessage.getOutTradeNo())
                    ? refundSuccessMessage.getOutTradeNo()
                    : refundSuccessMessage.getOrderId();
            if (StringUtils.isBlank(orderId)) {
                log.warn("拼团退单成功消息缺少订单号 message:{}", message);
                return;
            }

            boolean success = orderService.receiveRefundSuccessMessage(orderId, message);
            if (!success) {
                log.warn("拼团退单成功消息已落入本地退款任务，等待补偿重试 message:{}", message);
            }
        } catch (JSONException e) {
            log.warn("拼团退单成功消息格式错误，按永久异常确认消息 message:{}", message, e);
        } catch (Exception e) {
            log.error("处理拼团退单成功消息失败，消息未可靠落库将触发MQ重试 {}", message, e);
            throw e;
        }
    }

    @Data
    private static class TeamRefundSuccessMessage {
        private String type;
        private String userId;
        private String teamId;
        private Long activityId;
        private String orderId;
        private String outTradeNo;
    }

}
