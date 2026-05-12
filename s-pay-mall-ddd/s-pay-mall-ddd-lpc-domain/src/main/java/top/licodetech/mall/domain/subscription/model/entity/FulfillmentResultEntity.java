package top.licodetech.mall.domain.subscription.model.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FulfillmentResultEntity {

    private String orderId;

    private String userId;

    private boolean success;

    private boolean autoRefund;

}
