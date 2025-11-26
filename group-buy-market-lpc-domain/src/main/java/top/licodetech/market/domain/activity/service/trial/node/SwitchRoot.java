package top.licodetech.market.domain.activity.service.trial.node;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.licodetech.market.domain.activity.model.entity.MarketProductEntity;
import top.licodetech.market.domain.activity.model.entity.TrialBalanceEntity;
import top.licodetech.market.domain.activity.service.trial.AbstractGroupBuyMarketSupport;
import top.licodetech.market.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import top.licodetech.market.types.design.framwork.tree.StrategyHandler;

@Slf4j
@Service
public class SwitchRoot extends AbstractGroupBuyMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {
    @Override
    public TrialBalanceEntity apply(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return null;
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) {
        return null;
    }
}
