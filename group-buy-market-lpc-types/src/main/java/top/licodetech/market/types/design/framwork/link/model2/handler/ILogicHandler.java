package top.licodetech.market.types.design.framwork.link.model2.handler;

/**
 * @author LiPC
 * @description
 * @create 2025-12-28 21:15
 */
public interface ILogicHandler<T, D, R> {

    default R next(T requestParameter, D dynamicContext) {
        return null;
    }

    R apply(T requestParameter, D dynamicContext) throws Exception;

}
