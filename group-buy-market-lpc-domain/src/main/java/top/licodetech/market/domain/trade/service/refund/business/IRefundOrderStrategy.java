package top.licodetech.market.domain.trade.service.refund.business;

import top.licodetech.market.domain.trade.model.entity.TradeRefundOrderEntity;
import top.licodetech.market.domain.trade.model.valobj.TeamRefundSuccess;

/**
 * @author LiPC
 * @description
 * @create 2026-04-22 15:07
 */
public interface IRefundOrderStrategy {

    void refundOrder(TradeRefundOrderEntity tradeRefundOrderEntity) throws Exception;

    void reverseStock(TeamRefundSuccess teamRefundSuccess) throws Exception;

}
