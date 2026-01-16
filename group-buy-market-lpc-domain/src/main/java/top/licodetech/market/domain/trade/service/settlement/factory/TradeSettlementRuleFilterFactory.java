package top.licodetech.market.domain.trade.service.settlement.factory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import top.licodetech.market.domain.trade.model.entity.GroupBuyTeamEntity;
import top.licodetech.market.domain.trade.model.entity.MarketPayOrderEntity;
import top.licodetech.market.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import top.licodetech.market.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import top.licodetech.market.domain.trade.service.settlement.filter.EndRuleFilter;
import top.licodetech.market.domain.trade.service.settlement.filter.OutTradeNoRuleFilter;
import top.licodetech.market.domain.trade.service.settlement.filter.SCRuleFilter;
import top.licodetech.market.domain.trade.service.settlement.filter.SettableRuleFilter;
import top.licodetech.market.types.design.framwork.link.model2.LinkArmory;
import top.licodetech.market.types.design.framwork.link.model2.chain.BusinessLinkedList;

/**
 * @author LiPC
 * @description
 * @create 2026-01-15 17:39
 */
@Slf4j
@Service
public class TradeSettlementRuleFilterFactory {

    @Bean("tradeSettlementRuleFilter")
    public BusinessLinkedList<TradeSettlementRuleCommandEntity, TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> tradeSettlementRuleFilter(
            SCRuleFilter scRuleFilter,
            OutTradeNoRuleFilter outTradeNoRuleFilter,
            SettableRuleFilter settableRuleFilter,
            EndRuleFilter endRuleFilter
    ) {
        LinkArmory<TradeSettlementRuleCommandEntity, DynamicContext, TradeSettlementRuleFilterBackEntity> linkArmory = new LinkArmory<>("交易结算规则过滤链", scRuleFilter, outTradeNoRuleFilter, settableRuleFilter, endRuleFilter);

        return linkArmory.getLogicLink();
    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {
        // 订单营销实体对象
        private MarketPayOrderEntity marketPayOrderEntity;
        // 拼团组队实体对象
        private GroupBuyTeamEntity groupBuyTeamEntity;
    }

}
