package top.licodetech.market.infrastructure.adapter.repository;

import org.redisson.api.RBitSet;
import org.springframework.stereotype.Repository;
import top.licodetech.market.domain.activity.adapter.repository.IActivityRepository;
import top.licodetech.market.domain.activity.model.valobj.DiscountTypeEnum;
import top.licodetech.market.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import top.licodetech.market.domain.activity.model.valobj.SCSkuActivityVO;
import top.licodetech.market.domain.activity.model.valobj.SkuVO;
import top.licodetech.market.infrastructure.dao.*;
import top.licodetech.market.infrastructure.dao.po.GroupBuyActivity;
import top.licodetech.market.infrastructure.dao.po.GroupBuyDiscount;
import top.licodetech.market.infrastructure.dao.po.SCSkuActivity;
import top.licodetech.market.infrastructure.dao.po.Sku;
import top.licodetech.market.infrastructure.dcc.DCCService;
import top.licodetech.market.infrastructure.redis.IRedisService;

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
    private IRedisService redisService;

    @Resource
    private ISkuDao skuDao;

    @Resource
    private DCCService dccService;

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

    @Override
    public boolean isTagCrowdRange(String tagId, String userId) {
        RBitSet bitSet = redisService.getBitSet(tagId);
        if (!bitSet.isExists()) {
            return true;
        }
        // 判断用户是否存在人群中
        return bitSet.get(redisService.getIndexFromUserId(userId));
    }

    @Override
    public boolean downgradeSwitch() {
        return dccService.isDowngradeSwitch();
    }

    @Override
    public boolean cutRange(String userId) {
        return dccService.isCutRange(userId);
    }
}
