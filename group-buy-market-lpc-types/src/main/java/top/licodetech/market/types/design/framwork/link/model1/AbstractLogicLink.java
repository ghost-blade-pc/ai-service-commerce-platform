package top.licodetech.market.types.design.framwork.link.model1;

/**
 * @author LiPC
 * @description
 * @create 2025-12-28 18:06
 */
public abstract class AbstractLogicLink<T, D, R> implements ILogicLink<T, D, R> {

    private ILogicLink<T, D, R> next;

    @Override
    public ILogicLink<T, D, R> next() {

        return next;

    }

    @Override
    public ILogicLink<T, D, R> appendNext(ILogicLink<T, D, R> next) {
        this.next = next;
        return next;
    }

    protected R next(T requestParameter, D dynamicContext) throws Exception {
        if (null != next) {
            return next.apply(requestParameter, dynamicContext);
        }
        return null;
    }

}
