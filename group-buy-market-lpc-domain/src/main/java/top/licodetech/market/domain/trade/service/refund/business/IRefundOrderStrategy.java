package top.licodetech.market.domain.trade.service.refund.business;

import top.licodetech.market.domain.trade.model.entity.TradeRefundOrderEntity;

/**
 * @author LiPC
 * @description
 * @create 2026-04-22 15:07
 */
public interface IRefundOrderStrategy {

    void refundOrder(TradeRefundOrderEntity tradeRefundOrderEntity) throws Exception;

}
