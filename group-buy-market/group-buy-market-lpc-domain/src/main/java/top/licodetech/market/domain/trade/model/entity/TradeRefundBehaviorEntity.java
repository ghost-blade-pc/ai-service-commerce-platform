package top.licodetech.market.domain.trade.model.entity;

import lombok.*;

/**
 * @author LiPC
 * @description 退单行动
 * @create 2026-04-22 14:51
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeRefundBehaviorEntity {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 组队ID
     */
    private String teamId;

    /**
     * 行为枚举
     */
    private TradeRefundBehaviorEnum tradeRefundBehaviorEnum;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public enum TradeRefundBehaviorEnum {

        SUCCESS("success", "成功"),
        REPEAT("repeat", "重复"),
        FAIL("fail", "失败"),

        ;

        private String code;
        private String info;
    }

}
