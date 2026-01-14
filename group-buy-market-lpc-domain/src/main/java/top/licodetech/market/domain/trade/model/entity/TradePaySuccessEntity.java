package top.licodetech.market.domain.trade.model.entity;

import lombok.*;

import java.util.Date;

/**
 * @author LiPC
 * @description
 * @create 2026-01-13 16:35
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradePaySuccessEntity {

    /** 渠道 */
    private String source;
    /** 来源 */
    private String channel;
    /** 用户ID */
    private String userId;
    /** 外部交易单号 */
    private String outTradeNo;

}
