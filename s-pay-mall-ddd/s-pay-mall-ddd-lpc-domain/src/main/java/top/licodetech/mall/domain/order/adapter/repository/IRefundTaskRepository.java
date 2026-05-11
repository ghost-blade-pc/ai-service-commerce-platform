package top.licodetech.mall.domain.order.adapter.repository;

import top.licodetech.mall.domain.order.model.entity.RefundTaskEntity;
import top.licodetech.mall.domain.order.model.valobj.RefundTypeVO;

import java.util.List;

public interface IRefundTaskRepository {

    void saveRefundTask(String orderId, String message);

    void saveRefundTask(String orderId, RefundTypeVO refundType, String message);

    int lockRefundTask(String orderId);

    void markRefundTaskSuccess(String orderId);

    void markRefundTaskRetry(String orderId, String errorInfo);

    void markRefundTaskFailed(String orderId, String errorInfo);

    List<RefundTaskEntity> queryPendingRefundTaskList(Integer pageSize);

}
