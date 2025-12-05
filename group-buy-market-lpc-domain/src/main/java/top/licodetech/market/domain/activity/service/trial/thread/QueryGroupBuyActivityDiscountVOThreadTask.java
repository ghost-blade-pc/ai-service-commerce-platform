package top.licodetech.market.domain.activity.service.trial.thread;

import top.licodetech.market.domain.activity.adapter.repository.IActivityRespository;
import top.licodetech.market.domain.activity.model.valobj.GroupBuyActivityDiscountVO;

import java.util.concurrent.Callable;

public class QueryGroupBuyActivityDiscountVOThreadTask implements Callable<GroupBuyActivityDiscountVO> {

    private final String source;

    private final String channel;

    private final IActivityRespository respository;

    public QueryGroupBuyActivityDiscountVOThreadTask(String source, String channel, IActivityRespository respository) {
        this.source = source;
        this.channel = channel;
        this.respository = respository;
    }

    @Override
    public GroupBuyActivityDiscountVO call() throws Exception {
        return respository.queryGroupBuyActivityDiscountVO(source, channel);
    }

}
