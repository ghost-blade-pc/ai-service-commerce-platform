package top.licodetech.mall.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionFulfillmentTask {

    private Long id;

    private String orderId;

    private String userId;

    private String servicePackageId;

    private Integer totalQuota;

    private String status;

    private Integer retryCount;

    private String failReason;

    private Date nextRetryTime;

    private Date createTime;

    private Date updateTime;

}
