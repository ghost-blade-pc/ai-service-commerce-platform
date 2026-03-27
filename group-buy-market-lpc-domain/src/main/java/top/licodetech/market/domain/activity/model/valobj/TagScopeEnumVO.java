package top.licodetech.market.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author LiPC
 * @description 活动人群标签作用域范围枚举
 * @create 2026-03-27 21:25
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum TagScopeEnumVO {

    VISIBLE(true,false,"是否可看见拼团"),
    ENABLE(true, false,"是否可参与拼团"),
    ;

    private Boolean allow;
    private Boolean refuse;
    private String desc;

}
