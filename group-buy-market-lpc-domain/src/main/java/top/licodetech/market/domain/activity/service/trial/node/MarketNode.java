package top.licodetech.market.domain.activity.service.trial.node;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.licodetech.market.domain.activity.model.entity.MarketProductEntity;
import top.licodetech.market.domain.activity.model.entity.TrialBalanceEntity;
import top.licodetech.market.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import top.licodetech.market.domain.activity.model.valobj.SkuVO;
import top.licodetech.market.domain.activity.service.trial.AbstractGroupBuyMarketSupport;
import top.licodetech.market.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import top.licodetech.market.domain.activity.service.trial.thread.QueryGroupBuyActivityDiscountVOThreadTask;
import top.licodetech.market.domain.activity.service.trial.thread.QuerySkuVOFromDBThreadTask;
import top.licodetech.market.types.design.framwork.tree.StrategyHandler;

import javax.annotation.Resource;
import java.util.concurrent.*;

@Slf4j
@Service
public class MarketNode extends AbstractGroupBuyMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private EndNode endNode;

    @Override
    protected void mutiThread(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        QueryGroupBuyActivityDiscountVOThreadTask queryGroupBuyActivityDiscountVOThreadTask = new QueryGroupBuyActivityDiscountVOThreadTask(requestParameter.getSource(), requestParameter.getChannel(), activityRespository);
        FutureTask<GroupBuyActivityDiscountVO> groupBuyActivityDiscountVOFutureTask = new FutureTask<>(queryGroupBuyActivityDiscountVOThreadTask);
        threadPoolExecutor.execute(groupBuyActivityDiscountVOFutureTask);

        QuerySkuVOFromDBThreadTask querySkuVOFromDBThreadTask = new QuerySkuVOFromDBThreadTask(requestParameter.getGoodsId(), activityRespository);
        FutureTask<SkuVO> skuVOFutureTask = new FutureTask<>(querySkuVOFromDBThreadTask);
        threadPoolExecutor.execute(skuVOFutureTask);

        dynamicContext.setGroupBuyActivityDiscountVO(groupBuyActivityDiscountVOFutureTask.get(timeout, TimeUnit.MINUTES));
        dynamicContext.setSkuVO(skuVOFutureTask.get(timeout, TimeUnit.MINUTES));
    }

    @Override
    public TrialBalanceEntity doApply(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) {
        return endNode;
    }
}
