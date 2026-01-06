package top.licodetech.market.test.types.rule02.logic;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.licodetech.market.test.types.rule02.factory.Rule02TradeRuleFactory;
import top.licodetech.market.types.design.framwork.link.model2.handler.ILogicHandler;

/**
 * @author LiPC
 * @description
 * @create 2025-12-29 11:42
 */
@Slf4j
@Service
public class RuleLogic202 implements ILogicHandler<String, Rule02TradeRuleFactory.DynamicContext, XxxResponse> {
    @Override
    public XxxResponse apply(String requestParameter, Rule02TradeRuleFactory.DynamicContext dynamicContext) throws Exception {
        log.info("link model02 RuleLogic202");

        return new XxxResponse("hi 小傅哥！");
    }
}
