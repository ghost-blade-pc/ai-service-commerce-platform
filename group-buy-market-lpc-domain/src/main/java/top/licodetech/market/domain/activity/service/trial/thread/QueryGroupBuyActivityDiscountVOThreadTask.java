package top.licodetech.market.domain.activity.service.trial.thread;

import top.licodetech.market.domain.activity.adapter.repository.IActivityRepository;
import top.licodetech.market.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import top.licodetech.market.domain.activity.model.valobj.SCSkuActivityVO;

import java.util.concurrent.Callable;

public class QueryGroupBuyActivityDiscountVOThreadTask implements Callable<GroupBuyActivityDiscountVO> {

    private final String source;

    private final String channel;

    private final String goodsId;

    private final IActivityRepository repository;

    public QueryGroupBuyActivityDiscountVOThreadTask(String source, String channel, String goodsId, IActivityRepository repository) {
        this.source = source;
        this.channel = channel;
        this.goodsId = goodsId;
        this.repository = repository;
    }

    @Override
    public GroupBuyActivityDiscountVO call() throws Exception {
        // 查询渠道商品活动配置关联配置
        SCSkuActivityVO scSkuActivityVO = repository.querySCSkuActivityBySCGoodsId(source, channel, goodsId);
        if (null == scSkuActivityVO) {
            return null;
        }
        return repository.queryGroupBuyActivityDiscountVO(scSkuActivityVO.getActivityId());
    }

}
