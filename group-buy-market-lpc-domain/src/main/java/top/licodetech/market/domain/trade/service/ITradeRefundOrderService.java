package top.licodetech.market.domain.trade.service;

import top.licodetech.market.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import top.licodetech.market.domain.trade.model.entity.TradeRefundBehaviorEntity;
import top.licodetech.market.domain.trade.model.entity.TradeRefundCommandEntity;
import top.licodetech.market.domain.trade.model.valobj.TeamRefundSuccess;

import java.util.List;

/**
 * @author LiPC
 * @description  退单，逆向流程接口
 * @create 2026-04-22 14:41
 */
public interface ITradeRefundOrderService {

    TradeRefundBehaviorEntity refundOrder(TradeRefundCommandEntity tradeRefundCommandEntity) throws Exception;

    /**
     * 退单恢复锁单库存
     * @param teamRefundSuccess 退单消息
     * @throws Exception 异常
     */
    void restoreTeamLockStock(TeamRefundSuccess teamRefundSuccess) throws Exception;


    List<UserGroupBuyOrderDetailEntity> queryTimeoutUnpaidOrderList();
}
