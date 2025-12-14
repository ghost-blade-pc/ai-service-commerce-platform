package top.licodetech.market.infrastructure.adapter.repository;

import org.springframework.stereotype.Repository;
import top.licodetech.market.domain.activity.adapter.repository.IActivityRepository;
import top.licodetech.market.domain.activity.model.valobj.DiscountTypeEnum;
import top.licodetech.market.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import top.licodetech.market.domain.activity.model.valobj.SCSkuActivityVO;
import top.licodetech.market.domain.activity.model.valobj.SkuVO;
import top.licodetech.market.infrastructure.dao.IGroupBuyActivityDao;
import top.licodetech.market.infrastructure.dao.IGroupBuyDiscountDao;
import top.licodetech.market.infrastructure.dao.ISCSkuActivityDao;
import top.licodetech.market.infrastructure.dao.ISkuDao;
import top.licodetech.market.infrastructure.dao.po.GroupBuyActivity;
import top.licodetech.market.infrastructure.dao.po.GroupBuyDiscount;
import top.licodetech.market.infrastructure.dao.po.SCSkuActivity;
import top.licodetech.market.infrastructure.dao.po.Sku;

import javax.annotation.Resource;

@Repository
public class ActivityRepository implements IActivityRepository {

    @Resource
    private IGroupBuyDiscountDao groupBuyDiscountDao;
    @Resource
    private IGroupBuyActivityDao groupBuyActivityDao;

    @Resource
    private ISCSkuActivityDao scSkuActivityDao;

    @Resource
    private ISkuDao skuDao;

    @Override
    public SkuVO querySkuByGoodsId(String goodsId) {
        Sku sku = skuDao.querySkuByGoodsId(goodsId);
        if (null == sku) {
            return null;
        }
        return SkuVO.builder()
                .goodsId(sku.getGoodsId())
                .goodsName(sku.getGoodsName())
                .originalPrice(sku.getOriginalPrice())
                .build();
    }

    @Override
    public SCSkuActivityVO querySCSkuActivityBySCGoodsId(String source, String channel, String goodsId) {
        SCSkuActivity scSkuActivityReq = new SCSkuActivity();
        scSkuActivityReq.setSource(source);
        scSkuActivityReq.setChannel(channel);
        scSkuActivityReq.setGoodsId(goodsId);

        SCSkuActivity scSkuActivityRes = scSkuActivityDao.querySCSkuActivityBySCGoodsId(scSkuActivityReq);
        if (null == scSkuActivityRes) {
            return null;
        }

        return SCSkuActivityVO.builder()
                .source(scSkuActivityRes.getSource())
                .channel(scSkuActivityRes.getChannel())
                .activityId(scSkuActivityRes.getActivityId())
                .goodsId(scSkuActivityRes.getGoodsId())
                .build();
    }

    @Override
    public GroupBuyActivityDiscountVO queryGroupBuyActivityDiscountVO(Long activityId) {
        // 根据SC渠道值查询配置中最新的1个有效的活动
//        GroupBuyActivity groupBuyActivityReq = new GroupBuyActivity();
//        groupBuyActivityReq.setActivityId(activityId);
        GroupBuyActivity groupBuyActivityRes = groupBuyActivityDao.queryValidGroupBuyActivityId(activityId);
        if (null == groupBuyActivityRes) {
            return null;
        }

        String discountId = groupBuyActivityRes.getDiscountId();

        GroupBuyDiscount groupBuyDiscountRes = groupBuyDiscountDao.queryGroupBuyActivityDiscountByDiscountId(discountId);
        if (null == groupBuyDiscountRes) {
            return null;
        }

        GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountName(groupBuyDiscountRes.getDiscountName())
                .discountDesc(groupBuyDiscountRes.getDiscountDesc())
                .discountType(DiscountTypeEnum.get(groupBuyDiscountRes.getDiscountType()))
                .marketExpr(groupBuyDiscountRes.getMarketExpr())
                .marketPlan(groupBuyDiscountRes.getMarketPlan())
                .tagId(groupBuyDiscountRes.getTagId())
                .build();

        return GroupBuyActivityDiscountVO.builder()
                .activityId(groupBuyActivityRes.getActivityId())
                .activityName(groupBuyActivityRes.getActivityName())
                .groupBuyDiscount(groupBuyDiscount)
                .groupType(groupBuyActivityRes.getGroupType())
                .takeLimitCount(groupBuyActivityRes.getTakeLimitCount())
                .target(groupBuyActivityRes.getTarget())
                .validTime(groupBuyActivityRes.getValidTime())
                .status(groupBuyActivityRes.getStatus())
                .startTime(groupBuyActivityRes.getStartTime())
                .endTime(groupBuyActivityRes.getEndTime())
                .tagId(groupBuyActivityRes.getTagId())
                .tagScope(groupBuyActivityRes.getTagScope())
                .build();
    }
}
