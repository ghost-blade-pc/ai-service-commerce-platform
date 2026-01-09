package top.licodetech.market.domain.activity.service.discount;

import lombok.extern.slf4j.Slf4j;
import top.licodetech.market.domain.activity.adapter.repository.IActivityRepository;
import top.licodetech.market.domain.activity.model.valobj.DiscountTypeEnum;
import top.licodetech.market.domain.activity.model.valobj.GroupBuyActivityDiscountVO;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * @author LiPC
 */
@Slf4j
public abstract class AbstractDiscountCalculateService implements IDiscountCalculateService{

    @Resource
    private IActivityRepository repository;

    @Override
    public BigDecimal calculate(String userId, BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {
        // 1. 人群标签过滤
        if (DiscountTypeEnum.TAG.equals(groupBuyDiscount.getDiscountType())){
            boolean isCrowdRange = filterTagId(userId, groupBuyDiscount.getTagId());
            if (!isCrowdRange) {
                log.info("折扣优惠计算拦截，用户不在优惠人群标签范围内 userId:{}", userId);
                return originalPrice;
            }
        }
        // 2.  折扣优惠计算
        return doCalculate(originalPrice, groupBuyDiscount);
    }

    protected BigDecimal minPrice(BigDecimal deductionPrice) {
        // 判断折扣后金额，最低支付1分钱
        if (deductionPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("0.01");
        }
        return deductionPrice;
    }

    protected abstract BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount);

    private boolean filterTagId(String useId, String tagId) {
        return repository.isTagCrowdRange(tagId, useId);
    }



}
