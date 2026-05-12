package top.licodetech.mall.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.licodetech.mall.infrastructure.dao.po.SubscriptionEntitlement;

@Mapper
public interface ISubscriptionEntitlementDao {

    int insertIgnore(SubscriptionEntitlement entitlement);

    SubscriptionEntitlement queryByOrderId(@Param("orderId") String orderId);

    void revokeByOrderId(@Param("orderId") String orderId);

}
