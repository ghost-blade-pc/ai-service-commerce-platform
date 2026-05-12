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
public class SubscriptionEntitlement {

    private Long id;

    private String orderId;

    private String userId;

    private String servicePackageId;

    private Integer totalQuota;

    private Integer usedQuota;

    private Integer remainingQuota;

    private String status;

    private Date createTime;

    private Date updateTime;

}
