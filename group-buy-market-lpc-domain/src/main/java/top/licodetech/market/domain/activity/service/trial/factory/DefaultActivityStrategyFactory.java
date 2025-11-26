package top.licodetech.market.domain.activity.service.trial.factory;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import top.licodetech.market.domain.activity.model.entity.MarketProductEntity;
import top.licodetech.market.domain.activity.model.entity.TrialBalanceEntity;
import top.licodetech.market.domain.activity.service.trial.node.RootNode;
import top.licodetech.market.types.design.framwork.tree.StrategyHandler;

@Service
public class DefaultActivityStrategyFactory {

    private final RootNode rootNode;

    public DefaultActivityStrategyFactory(RootNode rootNode) {
        this.rootNode = rootNode;
    }

    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> strategyHandler(){
        return rootNode;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {

    }

}
