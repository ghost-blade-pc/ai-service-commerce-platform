package top.licodetech.mall.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.licodetech.mall.infrastructure.dao.po.RefundTask;

import java.util.List;

@Mapper
public interface IRefundTaskDao {

    void insertIgnore(RefundTask refundTask);

    int lockRefundTask(@Param("orderId") String orderId);

    void markRefundTaskSuccess(@Param("orderId") String orderId);

    void markRefundTaskRetry(@Param("orderId") String orderId, @Param("errorInfo") String errorInfo);

    void markRefundTaskFailed(@Param("orderId") String orderId, @Param("errorInfo") String errorInfo);

    List<String> queryPendingRefundTaskOrderList(@Param("pageSize") Integer pageSize);

}
