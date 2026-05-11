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
public class RefundTask {

    private Long id;

    private String orderId;

    private String refundType;

    private String message;

    private String status;

    private Integer retryCount;

    private String errorInfo;

    private Date nextRetryTime;

    private Date createTime;

    private Date updateTime;

}
