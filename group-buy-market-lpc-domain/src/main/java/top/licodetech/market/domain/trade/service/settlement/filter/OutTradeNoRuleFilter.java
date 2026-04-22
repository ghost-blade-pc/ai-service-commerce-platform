package top.licodetech.market.domain.trade.service.settlement.filter;

import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.licodetech.market.domain.trade.adapter.repository.ITradeRepository;
import top.licodetech.market.domain.trade.model.entity.MarketPayOrderEntity;
import top.licodetech.market.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import top.licodetech.market.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import top.licodetech.market.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import top.licodetech.market.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import top.licodetech.market.types.enums.ResponseCode;
import top.licodetech.market.types.exception.AppException;

import javax.annotation.Resource;

/**
 * @author LiPC
 * @description
 * @create 2026-01-15 19:38
 */
@Slf4j
@Service
public class OutTradeNoRuleFilter implements ILogicHandler<TradeSettlementRuleCommandEntity, TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> {

    @Resource
    private ITradeRepository repository;

    @Override
    public TradeSettlementRuleFilterBackEntity apply(TradeSettlementRuleCommandEntity requestParameter, TradeSettlementRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("结算规则过滤-外部单号校验{} outTradeNo:{}", requestParameter.getUserId(), requestParameter.getOutTradeNo());

        // 查询拼团信息
        MarketPayOrderEntity marketPayOrderEntity = repository.queryMarketPayOrderEntityByOutTradeNo(requestParameter.getUserId(), requestParameter.getOutTradeNo());

        if (null == marketPayOrderEntity || TradeOrderStatusEnumVO.CLOSE.equals(marketPayOrderEntity.getTradeOrderStatusEnumVO())) {
            log.error("不存在的外部交易单号或用户已退单，不需要做支付订单结算:{} outTradeNo:{}", requestParameter.getUserId(), requestParameter.getOutTradeNo());
            throw new AppException(ResponseCode.E0104);
        }

        dynamicContext.setMarketPayOrderEntity(marketPayOrderEntity);

        return next(requestParameter, dynamicContext);
    }
}
