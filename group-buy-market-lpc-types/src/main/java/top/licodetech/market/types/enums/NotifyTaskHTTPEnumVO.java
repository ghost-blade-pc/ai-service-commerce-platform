package top.licodetech.market.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author LiPC
 * @description
 * @create 2026-03-04 17:54
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum NotifyTaskHTTPEnumVO {

    SUCCESS("success", "成功"),
    ERROR("error", "失败"),
    NULL(null, "空执行"),
    ;

    private String code;
    private String info;

}
