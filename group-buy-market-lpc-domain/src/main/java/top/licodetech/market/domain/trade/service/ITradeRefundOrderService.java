package top.licodetech.market.domain.trade.service;

import top.licodetech.market.domain.trade.model.entity.TradeRefundBehaviorEntity;
import top.licodetech.market.domain.trade.model.entity.TradeRefundCommandEntity;

/**
 * @author LiPC
 * @description  退单，逆向流程接口
 * @create 2026-04-22 14:41
 */
public interface ITradeRefundOrderService {

    TradeRefundBehaviorEntity refundOrder(TradeRefundCommandEntity tradeRefundCommandEntity) throws Exception;

}
