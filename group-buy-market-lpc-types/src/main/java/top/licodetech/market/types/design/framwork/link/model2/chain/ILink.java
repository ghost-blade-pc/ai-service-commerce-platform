package top.licodetech.market.types.design.framwork.link.model2.chain;

/**
 * @author LiPC
 * @description
 * @create 2025-12-28 19:22
 */
public interface ILink<E> {

    boolean add(E e);

    boolean addFirst(E e);

    boolean addLast(E e);

    boolean remove(Object o);

    E get(int index);

    void printLinkList();

}
