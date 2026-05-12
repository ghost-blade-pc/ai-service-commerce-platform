package top.licodetech.market.domain.activity.model.valobj;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SkuVO {

    /** 商品ID */
    private String goodsId;
    /** 商品名称 */
    private String goodsName;
    /** 大模型调用总额度 */
    private Integer totalQuota;
    /** 原始价格 */
    private BigDecimal originalPrice;

}
