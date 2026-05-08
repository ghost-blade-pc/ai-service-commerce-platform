package top.licodetech.mall.domain.order.model.entity;

import lombok.Builder;
import lombok.Data;
import top.licodetech.mall.domain.order.model.valobj.RefundTaskStatusVO;

import java.util.Date;

@Data
@Builder
public class RefundTaskEntity {

    private Long id;

    private String orderId;

    private String message;

    private RefundTaskStatusVO status;

    private Integer retryCount;

    private String errorInfo;

    private Date nextRetryTime;

    private Date createTime;

    private Date updateTime;

}
