package top.licodetech.market.domain.trade.service.lock.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.licodetech.market.domain.trade.adapter.repository.ITradeRepository;
import top.licodetech.market.domain.trade.model.entity.GroupBuyActivityEntity;
import top.licodetech.market.domain.trade.model.entity.TradeLockRuleCommandEntity;
import top.licodetech.market.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import top.licodetech.market.domain.trade.service.lock.factory.TradeRuleFilterFactory;
import top.licodetech.market.types.design.framwork.link.model2.handler.ILogicHandler;
import top.licodetech.market.types.enums.ActivityStatusEnumVO;
import top.licodetech.market.types.enums.ResponseCode;
import top.licodetech.market.types.exception.AppException;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author LiPC
 * @description
 * @create 2026-01-09 15:41
 */
@Slf4j
@Service
public class ActivityUsabilityRuleFilter implements ILogicHandler<TradeLockRuleCommandEntity, TradeRuleFilterFactory.DynamicContext, TradeLockRuleFilterBackEntity> {

    @Resource
    private ITradeRepository repository;

    @Override
    public TradeLockRuleFilterBackEntity apply(TradeLockRuleCommandEntity requestParameter, TradeRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("交易规则过滤-活动的可用性校验{} activityId:{}", requestParameter.getUserId(), requestParameter.getActivityId());

        // 查询拼团活动
        GroupBuyActivityEntity groupBuyActivity = repository.queryGroupBuyActivityEntityByActivityId(requestParameter.getActivityId());

        // 校验：活动状态 - 可以抛业务异常code，或者把code写入到动态上下文dynamicContext中，最后获取。
        if (!ActivityStatusEnumVO.EFFECTIVE.equals(groupBuyActivity.getStatus())) {
            log.info("活动的可用性校验，非生效状态 activityId:{}", requestParameter.getActivityId());
            throw new AppException(ResponseCode.E0101);
        }

        // 校验：活动时间
        Date currentTime = new Date();
        if (currentTime.before(groupBuyActivity.getStartTime()) || currentTime.after(groupBuyActivity.getEndTime())) {
            log.info("活动的可用性校验，非可参与时间范围 activityId:{}", requestParameter.getActivityId());
            throw new AppException(ResponseCode.E0102);
        }

        // 写入动态上下文
        dynamicContext.setGroupBuyActivity(groupBuyActivity);

        // 走到下一个责任链节点
        return next(requestParameter, dynamicContext);
    }
}
