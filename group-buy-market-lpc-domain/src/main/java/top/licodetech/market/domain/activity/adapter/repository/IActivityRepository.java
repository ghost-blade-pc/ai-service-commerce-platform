package top.licodetech.market.domain.activity.adapter.repository;

import top.licodetech.market.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import top.licodetech.market.domain.activity.model.valobj.SCSkuActivityVO;
import top.licodetech.market.domain.activity.model.valobj.SkuVO;

public interface IActivityRepository {

    SkuVO querySkuByGoodsId(String goodsId);

    SCSkuActivityVO querySCSkuActivityBySCGoodsId(String source, String channel, String goodsId);

    GroupBuyActivityDiscountVO queryGroupBuyActivityDiscountVO(Long activityId);

    boolean isTagCrowdRange(String tagId, String userId);

    boolean downgradeSwitch();

    boolean cutRange(String userId);
}
