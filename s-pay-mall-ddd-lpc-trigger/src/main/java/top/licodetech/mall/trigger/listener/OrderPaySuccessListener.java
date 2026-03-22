package top.licodetech.mall.trigger.listener;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.licodetech.mall.domain.goods.adapter.repository.IGoodsRepository;
import top.licodetech.mall.domain.goods.service.IGoodsService;
import top.licodetech.mall.domain.order.adapter.event.PaySuccessMessageEvent;

import javax.annotation.Resource;

@Slf4j
@Component
public class OrderPaySuccessListener {

    @Resource
    private IGoodsService goodsService;

    @Subscribe
    public void handleEvent(String paySuccessMessageJson) {
        log.info("收到支付成功消息 {}", paySuccessMessageJson);

        PaySuccessMessageEvent.PaySuccessMessage paySuccessMessage = JSON.parseObject(paySuccessMessageJson, PaySuccessMessageEvent.PaySuccessMessage.class);

        log.info("模拟发货（如；发货、充值、开户员、返利），单号:{}", paySuccessMessage.getTradeNo());

        // 变更订单状态 - 发货完成&结算
        goodsService.changeOrderDealDone(paySuccessMessage.getTradeNo());
    }

}
