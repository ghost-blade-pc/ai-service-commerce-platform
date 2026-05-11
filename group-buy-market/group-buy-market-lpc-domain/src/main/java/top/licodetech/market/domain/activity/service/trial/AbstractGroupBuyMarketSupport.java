package top.licodetech.market.domain.activity.service.trial;

import cn.bugstack.wrench.design.framework.tree.AbstractMultiThreadStrategyRouter;
import top.licodetech.market.domain.activity.adapter.repository.IActivityRepository;
import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class AbstractGroupBuyMarketSupport<MarketProductEntity, DynamicContext, TrialBalanceEntity> extends AbstractMultiThreadStrategyRouter<MarketProductEntity, DynamicContext, TrialBalanceEntity> {

    protected long timeout = 5000;

    @Resource
    protected IActivityRepository activityRepository;

    @Override
    protected void multiThread(MarketProductEntity requestParameter, DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {

    }
}
