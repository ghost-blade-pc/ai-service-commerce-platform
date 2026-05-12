package top.licodetech.mall.domain.subscription.model.entity;

import lombok.Builder;
import lombok.Data;
import top.licodetech.mall.domain.subscription.model.valobj.EntitlementStatusVO;

import java.util.Date;

@Data
@Builder
public class SubscriptionEntitlementEntity {

    private Long id;

    private String orderId;

    private String userId;

    private String servicePackageId;

    private Integer totalQuota;

    private Integer usedQuota;

    private Integer remainingQuota;

    private EntitlementStatusVO status;

    private Date createTime;

    private Date updateTime;

}
