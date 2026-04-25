package top.licodetech.market.domain.trade.service.refund.factory;

import cn.bugstack.wrench.design.framework.link.model2.LinkArmory;
import cn.bugstack.wrench.design.framework.link.model2.chain.BusinessLinkedList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import top.licodetech.market.domain.trade.model.entity.GroupBuyTeamEntity;
import top.licodetech.market.domain.trade.model.entity.MarketPayOrderEntity;
import top.licodetech.market.domain.trade.model.entity.TradeRefundBehaviorEntity;
import top.licodetech.market.domain.trade.model.entity.TradeRefundCommandEntity;
import top.licodetech.market.domain.trade.service.refund.filter.DataNodeFilter;
import top.licodetech.market.domain.trade.service.refund.filter.RefundOrderNodeFilter;
import top.licodetech.market.domain.trade.service.refund.filter.UniqueRefundNodeFilter;

/**
 * @author LiPC
 * @description
 * @create 2026-04-25 15:39
 */
@Slf4j
@Service
public class TradeRefundRuleFilterFactory {

    @Bean("tradeRefundRuleFilter")
    public BusinessLinkedList<TradeRefundCommandEntity, TradeRefundRuleFilterFactory.DynamicContext, TradeRefundBehaviorEntity> tradeRefundRuleFiler(
            DataNodeFilter dataNodeFilter,
            UniqueRefundNodeFilter uniqueRefundNodeFilter,
            RefundOrderNodeFilter refundOrderNodeFilter
    ) {
        // 组装链
        LinkArmory<TradeRefundCommandEntity, TradeRefundRuleFilterFactory.DynamicContext, TradeRefundBehaviorEntity> linkArmory =
                new LinkArmory<>("退单规则过滤链",
                        dataNodeFilter,
                        uniqueRefundNodeFilter,
                        refundOrderNodeFilter);

        // 链对象
        return linkArmory.getLogicLink();
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {

        private MarketPayOrderEntity marketPayOrderEntity;

        private GroupBuyTeamEntity groupBuyTeamEntity;

    }
}
