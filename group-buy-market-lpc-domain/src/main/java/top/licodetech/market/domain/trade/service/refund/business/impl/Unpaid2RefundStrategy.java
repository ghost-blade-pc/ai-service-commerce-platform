package top.licodetech.market.domain.trade.service.refund.business.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.licodetech.market.domain.trade.adapter.repository.ITradeRepository;
import top.licodetech.market.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import top.licodetech.market.domain.trade.model.entity.TradeRefundOrderEntity;
import top.licodetech.market.domain.trade.service.refund.business.IRefundOrderStrategy;

import javax.annotation.Resource;

/**
 * @author LiPC
 * @description 未支付，未成团；发起退单（未支付），锁单量-1、组队订单状态更新
 * @create 2026-04-22 15:40
 */
@Slf4j
@Service("unpaid2RefundStrategy")
public class Unpaid2RefundStrategy implements IRefundOrderStrategy {

    @Resource
    private ITradeRepository repository;

    @Override
    public void refundOrder(TradeRefundOrderEntity tradeRefundOrderEntity) {
        log.info("退单；未支付，未成团 userId:{} teamId:{} orderId:{}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getTeamId(), tradeRefundOrderEntity.getOrderId());
        repository.unpaid2Refund(GroupBuyRefundAggregate.buildUnpaid2RefundAggregate(tradeRefundOrderEntity, -1));
    }
}
