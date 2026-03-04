package top.licodetech.market.domain.trade.service.settlement.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.licodetech.market.domain.trade.adapter.repository.ITradeRepository;
import top.licodetech.market.domain.trade.model.entity.GroupBuyTeamEntity;
import top.licodetech.market.domain.trade.model.entity.MarketPayOrderEntity;
import top.licodetech.market.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import top.licodetech.market.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import top.licodetech.market.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import top.licodetech.market.types.design.framwork.link.model2.handler.ILogicHandler;

import javax.annotation.Resource;

/**
 * @author LiPC
 * @description
 * @create 2026-01-15 20:08
 */
@Slf4j
@Service
public class EndRuleFilter implements ILogicHandler<TradeSettlementRuleCommandEntity, TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> {

    @Resource
    private ITradeRepository repository;

    @Override
    public TradeSettlementRuleFilterBackEntity apply(TradeSettlementRuleCommandEntity requestParameter, TradeSettlementRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("结算规则过滤-结束节点{} outTradeNo:{}", requestParameter.getUserId(), requestParameter.getOutTradeNo());

        // 获取上下文对象
        GroupBuyTeamEntity groupBuyTeamEntity = dynamicContext.getGroupBuyTeamEntity();

        return TradeSettlementRuleFilterBackEntity.builder()
                .teamId(groupBuyTeamEntity.getTeamId())
                .activityId(groupBuyTeamEntity.getActivityId())
                .targetCount(groupBuyTeamEntity.getTargetCount())
                .completeCount(groupBuyTeamEntity.getCompleteCount())
                .lockCount(groupBuyTeamEntity.getLockCount())
                .status(groupBuyTeamEntity.getStatus())
                .validStartTime(groupBuyTeamEntity.getValidStartTime())
                .validEndTime(groupBuyTeamEntity.getValidEndTime())
                .notifyUrl(groupBuyTeamEntity.getNotifyUrl())
                .build();
    }
}
