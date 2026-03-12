package top.licodetech.market.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author LiPC
 * @description 结算应答对象
 * @create 2026-03-09 15:36
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SettlementMarketPayOrderResponseDTO {

    /** 用户ID */
    private String userId;
    /** 拼单组队ID */
    private String teamId;
    /** 活动ID */
    private Long activityId;
    /** 外部交易单号 */
    private String outTradeNo;

}
