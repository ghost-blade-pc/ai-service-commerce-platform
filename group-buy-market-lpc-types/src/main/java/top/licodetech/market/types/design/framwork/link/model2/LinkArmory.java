package top.licodetech.market.types.design.framwork.link.model2;

import lombok.Getter;
import top.licodetech.market.types.design.framwork.link.model2.chain.BusinessLinkedList;
import top.licodetech.market.types.design.framwork.link.model2.handler.ILogicHandler;

/**
 * @author LiPC
 * @description
 * @create 2025-12-28 21:18
 */
public class LinkArmory<T, D, R> {

    @Getter
    private final BusinessLinkedList<T, D, R> logicLink;

    @SafeVarargs
    public LinkArmory(String linkName, ILogicHandler<T, D, R>... logicHandlers) {
        logicLink = new BusinessLinkedList<>(linkName);
        for (ILogicHandler<T, D, R> logicHandler : logicHandlers) {
            logicLink.add(logicHandler);
        }
    }

}
