package top.licodetech.mall.infrastructure.gateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoodsMarketResponseDTO {

    private Long activityId;

    private Goods goods;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Goods {

        private String goodsId;

        private String goodsName;

        private Integer totalQuota;

        private BigDecimal originalPrice;

        private BigDecimal deductionPrice;

        private BigDecimal payPrice;

    }
}
