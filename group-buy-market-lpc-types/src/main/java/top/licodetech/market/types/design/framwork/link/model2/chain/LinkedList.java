package top.licodetech.market.types.design.framwork.link.model2.chain;

import lombok.Getter;

/**
 * @author LiPC
 * @description
 * @create 2025-12-28 19:31
 */
public class LinkedList<E> implements ILink<E>{

    /**
     * 责任链名称
     */
    @Getter
    private final String name;

    transient int size = 0;

    transient Node<E> first;

    transient Node<E> last;

    public LinkedList(String name) {
        this.name = name;
    }

    /**
     * 前插
     * @param e
     */
    void linkFirst(E e) {
        final Node<E> f = first;
        final Node<E> newNode = new Node<>(null, e, f);
        first = newNode;
        if (null == f) {
            last = newNode;
        } else {
            f.prev = newNode;
        }
        size++;
    }

    /**
     * 尾插
     * @param e
     */
    void linkLast(E e) {
        final Node<E> l = last;
        final Node<E> newNode = new Node<>(l,e,null);
        last = newNode;
        if (null == l) {
            first = newNode;
        } else {
            l.next = newNode;
        }
        size++;
    }

    E unlink(Node<E> x) {
        final E element = x.item;
        final Node<E> next = x.next;
        final Node<E> prev = x.prev;

        if (null == prev) {
            first = next;
        } else {
            prev.next = next;
            x.prev = null;
        }

        if (null == next) {
            last = next;
        } else {
            next.prev = prev;
            x.next = null;
        }
        x.item = null;
        size--;
        return element;
    }

    Node<E> node(int index) {
        if (index < (size >> 1)) {
            Node<E> x = first;
            for (int i = 0; i < index; i++) {
                x = x.next;
            }
            return x;
        } else {
            Node<E> x = last;
            for (int i = size - 1; i > index; i--) {
                x = x.prev;
            }
            return x;
        }
    }

    @Override
    public boolean add(E e) {
        linkLast(e);
        return true;
    }

    @Override
    public boolean addFirst(E e) {
        linkFirst(e);
        return true;
    }

    @Override
    public boolean addLast(E e) {
        linkLast(e);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (null == o) {
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null) {
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public E get(int index) {
        return node(index).item;
    }

    @Override
    public void printLinkList() {
        if (this.size == 0) {
            System.out.println("链表为空");
        } else {
            Node<E> temp = first;
            System.out.print("目前的列表，头节点：" + first.item + " 尾节点：" + last.item + " 整体：");
            while(temp != null) {
                System.out.print(temp.item + "，");
                temp = temp.next;
            }
            System.out.println();
        }
    }

    protected static class Node<E> {

        E item;
        Node<E> next;
        Node<E> prev;

        public Node(Node<E> prev, E element, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }

    }

}
