package top.licodetech.market.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import top.licodetech.market.infrastructure.dao.po.GroupBuyActivity;

import java.util.List;

@Mapper
public interface IGroupBuyActivityDao {

    List<GroupBuyActivity> queryGroupBuyActivityList();

}
