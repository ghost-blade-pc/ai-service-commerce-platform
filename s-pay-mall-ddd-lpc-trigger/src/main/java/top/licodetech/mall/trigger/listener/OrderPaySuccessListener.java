package top.licodetech.mall.trigger.listener;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import top.licodetech.mall.domain.goods.service.IGoodsService;
import top.licodetech.mall.domain.order.adapter.event.PaySuccessMessageEvent;

import javax.annotation.Resource;

@Slf4j
@Component
public class OrderPaySuccessListener {

    @Resource
    private IGoodsService goodsService;

//    @Subscribe
    @RabbitListener(queues = "${spring.rabbitmq.config.consumer.topic_order_pay_success.queue}")
    public void listener(String paySuccessMessageJson) {
        try {
            log.info("收到支付成功消息 {}", paySuccessMessageJson);

            PaySuccessMessageEvent.PaySuccessMessage paySuccessMessage = JSON.parseObject(paySuccessMessageJson, PaySuccessMessageEvent.PaySuccessMessage.class);

            log.info("模拟发货（如；发货、充值、开户员、返利），单号:{}", paySuccessMessage.getTradeNo());

            // 变更订单状态 - 发货完成&结算
            goodsService.changeOrderDealDone(paySuccessMessage.getTradeNo());

            // 可以打开测试，MQ 消费失败，会抛异常，之后重试消费。这个也是最终执行的重要手段。
            // throw new RuntimeException("重试消费");
        } catch (Exception e) {
            log.error("收到支付成功消息失败 {}", paySuccessMessageJson,e);
            throw e;
        }
    }

}
