package top.licodetech.market.domain.activity.service.trial;

import top.licodetech.market.domain.activity.adapter.repository.IActivityRespository;
import top.licodetech.market.types.design.framwork.tree.AbstractMultiThreadStrategyRouter;
import top.licodetech.market.types.design.framwork.tree.AbstractStrategyRouter;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class AbstractGroupBuyMarketSupport<MarketProductEntity, DynamicContext, TrialBalanceEntity> extends AbstractMultiThreadStrategyRouter<MarketProductEntity, DynamicContext, TrialBalanceEntity> {

    protected long timeout = 500;

    @Resource
    protected IActivityRespository activityRespository;

    @Override
    protected void mutiThread(MarketProductEntity requestParameter, DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {

    }
}
