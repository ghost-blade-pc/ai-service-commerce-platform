package top.licodetech.market.domain.trade.adapter.repository;

import top.licodetech.market.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import top.licodetech.market.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import top.licodetech.market.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import top.licodetech.market.domain.trade.model.entity.GroupBuyActivityEntity;
import top.licodetech.market.domain.trade.model.entity.GroupBuyTeamEntity;
import top.licodetech.market.domain.trade.model.entity.MarketPayOrderEntity;
import top.licodetech.market.domain.trade.model.entity.NotifyTaskEntity;
import top.licodetech.market.domain.trade.model.valobj.GroupBuyProgressVO;

import java.util.List;

/**
 * @author LiPC
 * @description 交易仓储服务接口
 * @create 2025-12-25 20:07
 */
public interface ITradeRepository {

    MarketPayOrderEntity queryMarketPayOrderEntityByOutTradeNo(String userId, String outTradeNo);

    MarketPayOrderEntity lockMarketPayOrder(GroupBuyOrderAggregate groupBuyOrderAggregate);

    GroupBuyProgressVO queryGroupBuyProgress(String teamId);

    GroupBuyActivityEntity queryGroupBuyActivityEntityByActivityId(Long activityId);

    Integer queryOrderCountByActivityId(Long activityId, String userId);

    NotifyTaskEntity settlementMarketPayOrder(GroupBuyTeamSettlementAggregate groupBuyTeamSettlementAggregate);

    GroupBuyTeamEntity queryGroupTeamByTeamId(String teamId);

    boolean isSCBlackIntercept(String source, String channel);

    List<NotifyTaskEntity> queryUnExecutedNotifyTaskList();

    List<NotifyTaskEntity> queryUnExecutedNotifyTaskList(String teamId);

    int updateNotifyTaskStatusSuccess(NotifyTaskEntity notifyTask);

    int updateNotifyTaskStatusError(NotifyTaskEntity notifyTask);

    int updateNotifyTaskStatusRetry(NotifyTaskEntity notifyTask);

    boolean occupyTeamStock(String teamStockKey, String recoveryTeamStockKey, Integer target, Integer validTime);

    void recoveryTeamStock(String recoveryTeamStockKey, Integer validTime);

    NotifyTaskEntity unpaid2Refund(GroupBuyRefundAggregate groupBuyRefundAggregate);

    NotifyTaskEntity paid2Refund(GroupBuyRefundAggregate groupBuyRefundAggregate);

    NotifyTaskEntity paidTeam2Refund(GroupBuyRefundAggregate groupBuyRefundAggregate);

    void refund2AddRecovery(String recoveryTeamStockKey, String orderId);

}
