package top.licodetech.market.test.types.rule02.factory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import top.licodetech.market.test.types.rule02.logic.RuleLogic201;
import top.licodetech.market.test.types.rule02.logic.RuleLogic202;
import top.licodetech.market.test.types.rule02.logic.XxxResponse;
import top.licodetech.market.types.design.framwork.link.model2.LinkArmory;
import top.licodetech.market.types.design.framwork.link.model2.chain.BusinessLinkedList;

/**
 * @author LiPC
 * @description
 * @create 2025-12-29 11:40
 */
@Service
public class Rule02TradeRuleFactory {

    @Bean("demo01")
    public BusinessLinkedList<String, DynamicContext, XxxResponse> demo01(RuleLogic201 ruleLogic201, RuleLogic202 ruleLogic202) {
        LinkArmory<String, DynamicContext, XxxResponse> linkArmory = new LinkArmory<>("demo01", ruleLogic201, ruleLogic202);
        return linkArmory.getLogicLink();
    }

    @Bean("demo02")
    public BusinessLinkedList<String, DynamicContext, XxxResponse> demo01(RuleLogic202 ruleLogic202) {
        LinkArmory<String, DynamicContext, XxxResponse> linkArmory = new LinkArmory<>("demo02", ruleLogic202);
        return linkArmory.getLogicLink();
    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {
        private String age;
    }

}
