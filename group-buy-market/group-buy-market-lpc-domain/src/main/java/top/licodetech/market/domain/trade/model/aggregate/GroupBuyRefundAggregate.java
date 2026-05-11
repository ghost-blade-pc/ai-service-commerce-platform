package top.licodetech.market.domain.trade.model.aggregate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.licodetech.market.domain.trade.model.entity.TradeRefundOrderEntity;
import top.licodetech.market.domain.trade.model.valobj.GroupBuyProgressVO;
import top.licodetech.market.types.enums.GroupBuyOrderEnumVO;

/**
 * @author LiPC
 * @description 拼团退单聚合
 * @create 2026-04-22 15:23
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyRefundAggregate {

    /**
     * 交易退单
     */
    private TradeRefundOrderEntity tradeRefundOrderEntity;

    /**
     * 退单进度
     */
    private GroupBuyProgressVO groupBuyProgressVO;

    /**
     * 拼团枚举
     */
    private GroupBuyOrderEnumVO groupBuyOrderEnumVO;

    public static GroupBuyRefundAggregate buildUnpaid2RefundAggregate(TradeRefundOrderEntity tradeRefundOrderEntity, Integer lockCount) {
        GroupBuyRefundAggregate groupBuyRefundAggregate = new GroupBuyRefundAggregate();
        groupBuyRefundAggregate.setTradeRefundOrderEntity(tradeRefundOrderEntity);
        groupBuyRefundAggregate.setGroupBuyProgressVO(
                GroupBuyProgressVO.builder()
                        .lockCount(lockCount)
                        .build()
        );
        return groupBuyRefundAggregate;
    }

    public static GroupBuyRefundAggregate buildPaid2RefundAggregate(TradeRefundOrderEntity tradeRefundOrderEntity,
                                                                    Integer lockCount,
                                                                    Integer completeCount) {
        GroupBuyRefundAggregate groupBuyRefundAggregate = new GroupBuyRefundAggregate();
        groupBuyRefundAggregate.setTradeRefundOrderEntity(tradeRefundOrderEntity);
        groupBuyRefundAggregate.setGroupBuyProgressVO(
                GroupBuyProgressVO.builder()
                        .lockCount(lockCount)
                        .completeCount(completeCount)
                        .build());
        return groupBuyRefundAggregate;
    }

    public static GroupBuyRefundAggregate buildPaidTeam2RefundAggregate(TradeRefundOrderEntity tradeRefundOrderEntity,
                                                                        Integer lockCount,
                                                                        Integer completeCount,
                                                                        GroupBuyOrderEnumVO groupBuyOrderEnumVO) {
        GroupBuyRefundAggregate groupBuyRefundAggregate = new GroupBuyRefundAggregate();
        groupBuyRefundAggregate.setTradeRefundOrderEntity(tradeRefundOrderEntity);
        groupBuyRefundAggregate.setGroupBuyProgressVO(
                GroupBuyProgressVO.builder()
                        .lockCount(lockCount)
                        .completeCount(completeCount)
                        .build());
        groupBuyRefundAggregate.setGroupBuyOrderEnumVO(groupBuyOrderEnumVO);

        return groupBuyRefundAggregate;
    }

}
