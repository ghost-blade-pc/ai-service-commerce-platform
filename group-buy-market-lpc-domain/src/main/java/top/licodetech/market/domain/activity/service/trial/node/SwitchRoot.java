package top.licodetech.market.domain.activity.service.trial.node;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.licodetech.market.domain.activity.model.entity.MarketProductEntity;
import top.licodetech.market.domain.activity.model.entity.TrialBalanceEntity;
import top.licodetech.market.domain.activity.service.trial.AbstractGroupBuyMarketSupport;
import top.licodetech.market.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import top.licodetech.market.types.design.framwork.tree.StrategyHandler;

import javax.annotation.Resource;

/**
 * @author LiPC
 */
@Slf4j
@Service
public class SwitchRoot extends AbstractGroupBuyMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    @Resource
    private TagNode tagNode;

    @Override
    public TrialBalanceEntity doApply(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) {
        return tagNode;
    }
}
