package top.licodetech.market.domain.trade.adapter.port;

import top.licodetech.market.domain.trade.model.entity.NotifyTaskEntity;

/**
 * @author LiPC
 * @description
 * @create 2026-03-04 17:47
 */
public interface ITradePort {

    String groupBuyNotify(NotifyTaskEntity notifyTask) throws Exception;

}
