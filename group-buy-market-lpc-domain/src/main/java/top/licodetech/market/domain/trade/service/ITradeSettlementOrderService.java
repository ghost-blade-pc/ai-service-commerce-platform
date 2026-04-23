package top.licodetech.market.domain.trade.service;

import top.licodetech.market.domain.trade.model.entity.TradePaySettlementEntity;
import top.licodetech.market.domain.trade.model.entity.TradePaySuccessEntity;

/**
 * @author LiPC
 * @description
 * @create 2026-01-13 16:31
 */
public interface ITradeSettlementOrderService {
    /**
     * 营销结算
     * @param tradePaySuccessEntity 交易支付订单实体对象
     * @return 交易结算订单实体
     */
    TradePaySettlementEntity settlementMarketPayOrder(TradePaySuccessEntity tradePaySuccessEntity) throws Exception;

}
