package top.licodetech.mall.domain.subscription.model.entity;

import lombok.Builder;
import lombok.Data;
import top.licodetech.mall.domain.subscription.model.valobj.FulfillmentTaskStatusVO;

import java.util.Date;

@Data
@Builder
public class SubscriptionFulfillmentTaskEntity {

    private Long id;

    private String orderId;

    private String userId;

    private String servicePackageId;

    private Integer totalQuota;

    private FulfillmentTaskStatusVO status;

    private Integer retryCount;

    private String failReason;

    private Date nextRetryTime;

    private Date createTime;

    private Date updateTime;

}
