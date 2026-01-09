package top.licodetech.market.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author LiPC
 * @description
 * @create 2026-01-09 15:13
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum ActivityStatusEnumVO {
    CREATE(0, "创建"),
    EFFECTIVE(1, "生效"),
    OVERDUE(2, "过期"),
    ABANDONED(3, "废弃"),
    ;

    private Integer code;
    private String info;

    public static ActivityStatusEnumVO valueOf(Integer code) {
        return switch (code) {
            case 0 -> CREATE;
            case 1 -> EFFECTIVE;
            case 2 -> OVERDUE;
            case 3 -> ABANDONED;
            default -> throw new RuntimeException("err code not exist!");
        };
    }


}
