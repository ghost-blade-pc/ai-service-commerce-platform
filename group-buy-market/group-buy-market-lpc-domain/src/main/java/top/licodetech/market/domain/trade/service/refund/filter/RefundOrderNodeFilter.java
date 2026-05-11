package top.licodetech.market.domain.trade.service.refund.filter;

import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.licodetech.market.domain.trade.model.entity.*;
import top.licodetech.market.domain.trade.model.valobj.RefundTypeEnumVO;
import top.licodetech.market.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import top.licodetech.market.domain.trade.service.refund.business.IRefundOrderStrategy;
import top.licodetech.market.domain.trade.service.refund.factory.TradeRefundRuleFilterFactory;
import top.licodetech.market.types.enums.GroupBuyOrderEnumVO;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author LiPC
 * @description
 * @create 2026-04-25 16:59
 */
@Slf4j
@Service
public class RefundOrderNodeFilter implements ILogicHandler<TradeRefundCommandEntity, TradeRefundRuleFilterFactory.DynamicContext, TradeRefundBehaviorEntity> {

    @Resource
    private Map<String, IRefundOrderStrategy> refundOrderStrategyMap;

    @Override
    public TradeRefundBehaviorEntity apply(TradeRefundCommandEntity tradeRefundCommandEntity, TradeRefundRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        GroupBuyTeamEntity groupBuyTeamEntity = dynamicContext.getGroupBuyTeamEntity();
        GroupBuyOrderEnumVO groupBuyOrderEnumVO = groupBuyTeamEntity.getStatus();
        MarketPayOrderEntity marketPayOrderEntity = dynamicContext.getMarketPayOrderEntity();
        TradeOrderStatusEnumVO tradeOrderStatusEnumVO = marketPayOrderEntity.getTradeOrderStatusEnumVO();
        // 3. 状态类型判断 - 使用策略模式获取退款类型
        RefundTypeEnumVO refundType = RefundTypeEnumVO.getRefundStrategy(groupBuyOrderEnumVO, tradeOrderStatusEnumVO);
        IRefundOrderStrategy refundOrderStrategy = refundOrderStrategyMap.get(refundType.getStrategy());
        refundOrderStrategy.refundOrder(TradeRefundOrderEntity.builder()
                .userId(tradeRefundCommandEntity.getUserId())
                .orderId(marketPayOrderEntity.getOrderId())
                .activityId(groupBuyTeamEntity.getActivityId())
                .teamId(marketPayOrderEntity.getTeamId())
                .outTradeNo(tradeRefundCommandEntity.getOutTradeNo())
                .build());

        return TradeRefundBehaviorEntity.builder()
                .userId(tradeRefundCommandEntity.getUserId())
                .orderId(marketPayOrderEntity.getOrderId())
                .teamId(marketPayOrderEntity.getTeamId())
                .tradeRefundBehaviorEnum(TradeRefundBehaviorEntity.TradeRefundBehaviorEnum.SUCCESS)
                .build();
    }
}
