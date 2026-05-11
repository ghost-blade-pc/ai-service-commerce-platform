package top.licodetech.market.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author LiPC
 * @description
 * @create 2026-04-22 15:11
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeRefundOrderEntity {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 拼单组队ID
     */
    private String teamId;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 预购订单ID
     */
    private String orderId;

    /**
     * 外部交易单号
     */
    private String outTradeNo;

}
