package top.licodetech.market.test.types.rule01.factory;

import cn.bugstack.wrench.design.framework.link.model1.ILogicLink;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import top.licodetech.market.test.types.rule01.logic.RuleLogic101;
import top.licodetech.market.test.types.rule01.logic.RuleLogic102;

import javax.annotation.Resource;

/**
 * @author LiPC
 * @description
 * @create 2025-12-29 10:58
 */
@Service
public class Rule01TradeRuleFactory {

    @Resource
    private RuleLogic101 ruleLogic101;

    @Resource
    private RuleLogic102 ruleLogic102;

    public ILogicLink<String, DynamicContext, String> openLogicLink() {
        ruleLogic101.appendNext(ruleLogic102);
        return ruleLogic101;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {
        private String age;
    }

}
