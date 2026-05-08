package top.licodetech.mall.domain.order.adapter.repository;

import java.util.List;

public interface IRefundTaskRepository {

    void saveRefundTask(String orderId, String message);

    int lockRefundTask(String orderId);

    void markRefundTaskSuccess(String orderId);

    void markRefundTaskRetry(String orderId, String errorInfo);

    void markRefundTaskFailed(String orderId, String errorInfo);

    List<String> queryPendingRefundTaskOrderList(Integer pageSize);

}
