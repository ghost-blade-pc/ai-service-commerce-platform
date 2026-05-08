package top.licodetech.mall.infrastructure.adapter.repository;

import org.springframework.stereotype.Repository;
import top.licodetech.mall.domain.order.adapter.repository.IRefundTaskRepository;
import top.licodetech.mall.domain.order.model.valobj.RefundTaskStatusVO;
import top.licodetech.mall.infrastructure.dao.IRefundTaskDao;
import top.licodetech.mall.infrastructure.dao.po.RefundTask;

import javax.annotation.Resource;
import java.util.List;

@Repository
public class RefundTaskRepository implements IRefundTaskRepository {

    @Resource
    private IRefundTaskDao refundTaskDao;

    @Override
    public void saveRefundTask(String orderId, String message) {
        RefundTask refundTask = RefundTask.builder()
                .orderId(orderId)
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
    public List<String> queryPendingRefundTaskOrderList(Integer pageSize) {
        return refundTaskDao.queryPendingRefundTaskOrderList(pageSize);
    }

    private String limitErrorInfo(String errorInfo) {
        if (null == errorInfo) {
            return null;
        }
        return errorInfo.length() <= 512 ? errorInfo : errorInfo.substring(0, 512);
    }
}
