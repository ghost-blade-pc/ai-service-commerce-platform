package top.licodetech.mall.infrastructure.adapter.repository;

import org.springframework.stereotype.Repository;
import top.licodetech.mall.domain.order.adapter.repository.IRefundTaskRepository;
import top.licodetech.mall.domain.order.model.entity.RefundTaskEntity;
import top.licodetech.mall.domain.order.model.valobj.RefundTaskStatusVO;
import top.licodetech.mall.domain.order.model.valobj.RefundTypeVO;
import top.licodetech.mall.infrastructure.dao.IRefundTaskDao;
import top.licodetech.mall.infrastructure.dao.po.RefundTask;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

@Repository
public class RefundTaskRepository implements IRefundTaskRepository {

    @Resource
    private IRefundTaskDao refundTaskDao;

    @Override
    public void saveRefundTask(String orderId, String message) {
        saveRefundTask(orderId, RefundTypeVO.PAID_FORMED, message);
    }

    @Override
    public void saveRefundTask(String orderId, RefundTypeVO refundType, String message) {
        RefundTask refundTask = RefundTask.builder()
                .orderId(orderId)
                .refundType(null == refundType ? null : refundType.getCode())
                .message(message)
                .status(RefundTaskStatusVO.PENDING.getCode())
                .retryCount(0)
                .build();
        refundTaskDao.insertIgnore(refundTask);
    }

    @Override
    public int lockRefundTask(String orderId) {
        return refundTaskDao.lockRefundTask(orderId);
    }

    @Override
    public void markRefundTaskSuccess(String orderId) {
        refundTaskDao.markRefundTaskSuccess(orderId);
    }

    @Override
    public void markRefundTaskRetry(String orderId, String errorInfo) {
        refundTaskDao.markRefundTaskRetry(orderId, limitErrorInfo(errorInfo));
    }

    @Override
    public void markRefundTaskFailed(String orderId, String errorInfo) {
        refundTaskDao.markRefundTaskFailed(orderId, limitErrorInfo(errorInfo));
    }

    @Override
    public List<RefundTaskEntity> queryPendingRefundTaskList(Integer pageSize) {
        List<RefundTask> refundTaskList = refundTaskDao.queryPendingRefundTaskList(pageSize);
        if (null == refundTaskList) {
            return Collections.emptyList();
        }
        return refundTaskList.stream()
                .map(this::buildRefundTaskEntity)
                .toList();
    }

    private RefundTaskEntity buildRefundTaskEntity(RefundTask refundTask) {
        if (null == refundTask) {
            return null;
        }
        RefundTaskStatusVO status = null == refundTask.getStatus() ? null : RefundTaskStatusVO.valueOf(refundTask.getStatus());
        return RefundTaskEntity.builder()
                .id(refundTask.getId())
                .orderId(refundTask.getOrderId())
                .refundType(RefundTypeVO.of(refundTask.getRefundType()))
                .message(refundTask.getMessage())
                .status(status)
                .retryCount(refundTask.getRetryCount())
                .errorInfo(refundTask.getErrorInfo())
                .nextRetryTime(refundTask.getNextRetryTime())
                .createTime(refundTask.getCreateTime())
                .updateTime(refundTask.getUpdateTime())
                .build();
    }

    private String limitErrorInfo(String errorInfo) {
        if (null == errorInfo) {
            return null;
        }
        return errorInfo.length() <= 512 ? errorInfo : errorInfo.substring(0, 512);
    }
}
