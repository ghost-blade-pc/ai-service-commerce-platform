package top.licodetech.market.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.licodetech.market.domain.trade.model.valobj.TradeOrderStatusEnumVO;

import java.math.BigDecimal;

/**
 * @author LiPC
 * @description 拼团，预购订单营销实体对象
 * @create 2025-12-25 17:11
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketPayOrderEntity {

    /** 拼单组队ID */
    private String teamId;
    /** 活动ID */
    private String orderId;
    /** 折扣金额 */
    private BigDecimal deductionPrice;
    /** 交易订单状态枚举 */
    private TradeOrderStatusEnumVO tradeOrderStatusEnumVO;

}
