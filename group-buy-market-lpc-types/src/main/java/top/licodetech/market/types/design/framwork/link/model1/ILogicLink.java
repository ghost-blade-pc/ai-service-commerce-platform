package top.licodetech.market.types.design.framwork.link.model1;

/**
 * @author LiPC
 * @description
 * @create 2025-12-28 17:53
 */
public interface ILogicLink<T, D, R> extends ILogicChainArmory<T, D, R> {

    R apply(T requestParameter, D dynamicContext) throws Exception;

}
