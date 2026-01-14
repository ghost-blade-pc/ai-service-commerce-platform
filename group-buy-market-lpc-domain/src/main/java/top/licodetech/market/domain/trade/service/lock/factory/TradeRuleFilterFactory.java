package top.licodetech.market.domain.trade.service.lock.factory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import top.licodetech.market.domain.trade.model.entity.GroupBuyActivityEntity;
import top.licodetech.market.domain.trade.model.entity.TradeRuleCommandEntity;
import top.licodetech.market.domain.trade.model.entity.TradeRuleFilterBackEntity;
import top.licodetech.market.domain.trade.service.lock.filter.ActivityUsabilityRuleFilter;
import top.licodetech.market.domain.trade.service.lock.filter.UserTakeLimitRuleFilter;
import top.licodetech.market.types.design.framwork.link.model2.LinkArmory;
import top.licodetech.market.types.design.framwork.link.model2.chain.BusinessLinkedList;

/**
 * @author LiPC
 * @description
 * @create 2026-01-09 15:38
 */
@Slf4j
@Service
public class TradeRuleFilterFactory {

    @Bean("tradeRuleFilter")
    public BusinessLinkedList<TradeRuleCommandEntity, TradeRuleFilterFactory.DynamicContext, TradeRuleFilterBackEntity> tradeRuleFilter(ActivityUsabilityRuleFilter activityUsabilityRuleFilter, UserTakeLimitRuleFilter userTakeLimitRuleFilter) {

        // 组装链
        LinkArmory<TradeRuleCommandEntity, DynamicContext, TradeRuleFilterBackEntity> linkArmory =
                new LinkArmory<>("交易规则过滤链", activityUsabilityRuleFilter, userTakeLimitRuleFilter);

        // 链对象
        return linkArmory.getLogicLink();
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {

        private GroupBuyActivityEntity groupBuyActivity;

    }

}
