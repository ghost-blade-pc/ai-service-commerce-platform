package top.licodetech.market.domain.trade.adapter.repository;

import top.licodetech.market.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import top.licodetech.market.domain.trade.model.entity.GroupBuyActivityEntity;
import top.licodetech.market.domain.trade.model.entity.MarketPayOrderEntity;
import top.licodetech.market.domain.trade.model.valobj.GroupBuyProgressVO;

/**
 * @author LiPC
 * @description
 * @create 2025-12-25 20:07
 */
public interface ITradeRepository {

    MarketPayOrderEntity queryMarketPayOrderEntityByOutTradeNo(String userId, String outTradeNo);

    MarketPayOrderEntity lockMarketPayOrder(GroupBuyOrderAggregate groupBuyOrderAggregate);

    GroupBuyProgressVO queryGroupBuyProgress(String teamId);

    GroupBuyActivityEntity queryGroupBuyActivityEntityByActivityId(Long activityId);

    Integer queryOrderCountByActivityId(Long activityId, String userId);
}
