package top.licodetech.mall.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.licodetech.mall.infrastructure.dao.po.SubscriptionFulfillmentTask;

import java.util.List;

@Mapper
public interface ISubscriptionFulfillmentTaskDao {

    void insertIgnore(SubscriptionFulfillmentTask task);

    int lockTask(@Param("orderId") String orderId, @Param("servicePackageId") String servicePackageId);

    void markSuccess(@Param("orderId") String orderId, @Param("servicePackageId") String servicePackageId);

    void markRetry(@Param("orderId") String orderId, @Param("servicePackageId") String servicePackageId, @Param("failReason") String failReason);

    void markFailed(@Param("orderId") String orderId, @Param("servicePackageId") String servicePackageId, @Param("failReason") String failReason);

    SubscriptionFulfillmentTask queryTask(@Param("orderId") String orderId, @Param("servicePackageId") String servicePackageId);

    List<SubscriptionFulfillmentTask> queryPendingTaskList(@Param("pageSize") Integer pageSize);

}
