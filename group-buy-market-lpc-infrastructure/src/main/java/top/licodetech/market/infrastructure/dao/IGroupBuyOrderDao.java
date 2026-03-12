package top.licodetech.market.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import top.licodetech.market.infrastructure.dao.po.GroupBuyOrder;

import java.util.List;
import java.util.Set;

/**
 * @author LiPC
 * @description
 * @create 2025-12-25 16:19
 */
@Mapper
public interface IGroupBuyOrderDao {

    void insert(GroupBuyOrder groupBuyOrder);

    int updateAddLockCount(String teamId);

    int updateSubtractionLockCount(String teamId);

    GroupBuyOrder queryGroupBuyProgress(String teamId);

    GroupBuyOrder queryGroupTeamByTeamId(String teamId);

    int updateAddCompleteCount(String teamId);

    int updateOrderStatus2COMPLETE(String teamId);

    List<GroupBuyOrder> queryGroupBuyProgressByTeamIds(Set<String> teamIds);

    Integer queryAllTeamCount(Set<String> teamIds);

    Integer queryAllTeamCompleteCount(Set<String> teamIds);

    Integer queryAllUserCount(Set<String> teamIds);

}
