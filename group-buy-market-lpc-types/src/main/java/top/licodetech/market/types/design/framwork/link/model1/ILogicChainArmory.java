package top.licodetech.market.types.design.framwork.link.model1;

/**
 * @author LiPC
 * @description
 * @create 2025-12-28 17:59
 */
public interface ILogicChainArmory<T, D, R> {

    ILogicLink<T, D, R> next();

    ILogicLink<T, D, R> appendNext(ILogicLink<T, D, R> next);

}
