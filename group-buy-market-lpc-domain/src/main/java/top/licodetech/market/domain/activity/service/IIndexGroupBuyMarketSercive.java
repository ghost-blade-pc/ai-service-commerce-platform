package top.licodetech.market.domain.activity.service;

import top.licodetech.market.domain.activity.model.entity.MarketProductEntity;
import top.licodetech.market.domain.activity.model.entity.TrialBalanceEntity;

public interface IIndexGroupBuyMarketSercive {

    TrialBalanceEntity indexMarketTrial(MarketProductEntity marketProductEntity) throws Exception;

}
