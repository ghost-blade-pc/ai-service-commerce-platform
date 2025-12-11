package top.licodetech.market.domain.activity.service.trial.thread;

import top.licodetech.market.domain.activity.adapter.repository.IActivityRepository;
import top.licodetech.market.domain.activity.model.valobj.SkuVO;

import java.util.concurrent.Callable;

public class QuerySkuVOFromDBThreadTask implements Callable<SkuVO> {

    private final String goodsId;

    private final IActivityRepository activityRespository;

    public QuerySkuVOFromDBThreadTask(String goodsId, IActivityRepository activityRespository) {
        this.goodsId = goodsId;
        this.activityRespository = activityRespository;
    }

    @Override
    public SkuVO call() throws Exception {
        return activityRespository.querySkuByGoodsId(goodsId);
    }
}
