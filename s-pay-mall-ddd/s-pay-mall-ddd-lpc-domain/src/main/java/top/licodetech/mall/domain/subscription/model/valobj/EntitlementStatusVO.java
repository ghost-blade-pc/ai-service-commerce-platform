package top.licodetech.mall.domain.subscription.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EntitlementStatusVO {

    ACTIVE("ACTIVE", "已开通"),
    REVOKED("REVOKED", "已撤销"),
    ;

    private final String code;

    private final String desc;

    public static EntitlementStatusVO valueOfCode(String code) {
        for (EntitlementStatusVO value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
