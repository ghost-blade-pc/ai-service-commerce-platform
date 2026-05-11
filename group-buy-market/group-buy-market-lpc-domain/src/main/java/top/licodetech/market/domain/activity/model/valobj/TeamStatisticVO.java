package top.licodetech.market.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author LiPC
 * @description 队伍统计值对象
 * @create 2026-03-11 17:03
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamStatisticVO {

    // 开团队伍数量
    private Integer allTeamCount;
    // 成团队伍数量
    private Integer allTeamCompleteCount;
    // 参团人数总量 - 一个商品的总参团人数
    private Integer allTeamUserCount;

}
