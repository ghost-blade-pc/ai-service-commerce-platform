package top.licodetech.market.types.design.framwork.tree;

/**
 * 策略映射器
 */
public interface StrategyMapper<T, D, R> {

    StrategyHandler<T, D, R> get(T requestParameter, D dynamicContext) throws Exception;

}
