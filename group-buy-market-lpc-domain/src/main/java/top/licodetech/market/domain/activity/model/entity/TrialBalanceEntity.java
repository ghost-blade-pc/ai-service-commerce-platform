package top.licodetech.market.domain.activity.model.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrialBalanceEntity {

    private String goodsId;
    private String goodsName;
    private BigDecimal originalPrice;
    private BigDecimal deductionPrice;
    private Integer targetCount;
    private Date startTime;
    private Date endTime;
    private Boolean isVisible;
    private Boolean isEnable;

}
