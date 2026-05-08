package top.licodetech.mall.domain.order.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
@AllArgsConstructor
public enum RefundTypeVO {

    UNPAID_UNLOCK("unpaid_unlock", false),
    PAID_UNFORMED("paid_unformed", true),
    PAID_FORMED("paid_formed", true);

    private final String code;
    private final boolean needPayRefund;

    public static RefundTypeVO of(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        for (RefundTypeVO refundTypeVO : values()) {
            if (refundTypeVO.getCode().equals(code)) {
                return refundTypeVO;
            }
        }
        return null;
    }

}
