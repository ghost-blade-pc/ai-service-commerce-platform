package top.licodetech.market.domain.trade.service.refund.business.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.licodetech.market.domain.trade.adapter.repository.ITradeRepository;
import top.licodetech.market.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import top.licodetech.market.domain.trade.model.entity.NotifyTaskEntity;
import top.licodetech.market.domain.trade.model.entity.TradeRefundOrderEntity;
import top.licodetech.market.domain.trade.service.ITradeTaskService;
import top.licodetech.market.domain.trade.service.refund.business.IRefundOrderStrategy;
import top.licodetech.market.types.exception.AppException;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author LiPC
 * @description 发起退单（已支付、未成团），锁单量-1、完成量-1、组队订单状态更新、发送退单消息（MQ）
 * @create 2026-04-22 15:44
 */
@Slf4j
@Service("paid2RefundStrategy")
public class Paid2RefundStrategy implements IRefundOrderStrategy {

    @Resource
    private ITradeRepository repository;

    @Resource
    private ITradeTaskService tradeTaskService;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public void refundOrder(TradeRefundOrderEntity tradeRefundOrderEntity) {
        log.info("退单：已支付，未成团 userId:{} teamId:{} orderId:{}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getTeamId(),tradeRefundOrderEntity.getOrderId());

        NotifyTaskEntity notifyTaskEntity = repository.paid2Refund(GroupBuyRefundAggregate.buildPaid2RefundAggregate(tradeRefundOrderEntity, -1, -1));
        if(null != notifyTaskEntity){
            threadPoolExecutor.execute(() -> {
                Map<String, Integer> notifyResultMap = null;
                try {
                    notifyResultMap = tradeTaskService.execNotifyJob(notifyTaskEntity);
                    log.info("回调通知交易退单 result:{}", JSON.toJSONString(notifyResultMap));
                } catch (Exception e) {
                    log.error("回调通知交易退单失败 result:{}", JSON.toJSONString(notifyResultMap), e);
                    throw new AppException(e.getMessage());
                }
            });
        }
    }
}
