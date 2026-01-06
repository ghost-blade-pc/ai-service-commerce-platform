package top.licodetech.market.test.types.rule01.logic;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.licodetech.market.test.types.rule01.factory.Rule01TradeRuleFactory;
import top.licodetech.market.types.design.framwork.link.model1.AbstractLogicLink;

/**
 * @author LiPC
 * @description
 * @create 2025-12-29 10:57
 */
@Slf4j
@Service
public class RuleLogic101 extends AbstractLogicLink<String, Rule01TradeRuleFactory.DynamicContext, String> {
    @Override
    public String apply(String requestParameter, Rule01TradeRuleFactory.DynamicContext dynamicContext) throws Exception {
        log.info("link model01 RuleLogic101");
        return next(requestParameter, dynamicContext);
    }
}
