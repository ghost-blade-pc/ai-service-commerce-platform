package top.licodetech.market.domain.trade.model.aggregate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.licodetech.market.domain.trade.model.entity.PayActivityEntity;
import top.licodetech.market.domain.trade.model.entity.PayDiscountEntity;
import top.licodetech.market.domain.trade.model.entity.UserEntity;

/**
 * @author LiPC
 * @description
 * @create 2025-12-25 20:08
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyOrderAggregate {

    /** 用户实体对象 */
    private UserEntity userEntity;
    /** 支付活动实体对象 */
    private PayActivityEntity payActivityEntity;
    /** 支付优惠实体对象 */
    private PayDiscountEntity payDiscountEntity;

}
