package top.licodetech.mall.domain.order.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RefundTaskStatusVO {

    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    RETRY("RETRY", "待重试"),
    SUCCESS("SUCCESS", "处理成功"),
    FAILED("FAILED", "处理失败"),
    ;

    private final String code;
    private final String desc;

}
