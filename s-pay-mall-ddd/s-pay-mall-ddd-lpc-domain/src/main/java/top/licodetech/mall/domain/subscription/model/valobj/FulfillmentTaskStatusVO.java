package top.licodetech.mall.domain.subscription.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FulfillmentTaskStatusVO {

    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    RETRY("RETRY", "待重试"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    ;

    private final String code;

    private final String desc;

    public static FulfillmentTaskStatusVO valueOfCode(String code) {
        for (FulfillmentTaskStatusVO value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
