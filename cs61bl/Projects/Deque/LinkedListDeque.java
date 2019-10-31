public class LinkedListDeque<T> implements Deque<T> {

    private class LLDNode {
        private T item;
        private LLDNode next;
        private LLDNode prev;

        private LLDNode(T item) {
            this.item = item;


        }
    }
    private int size;
    private LLDNode sentinel;

    public LinkedListDeque() {

        sentinel = new LLDNode(null);
        sentinel.next = sentinel;
        sentinel.prev = sentinel;
        this.size = 0;
    }

    @Override
    public void addFirst(T item) {
        LLDNode result = new LLDNode(item);
        LLDNode original = sentinel.next;
        sentinel.next = result;
        sentinel.next.next = original;
        sentinel.next.prev = sentinel;
        original.prev = sentinel.next;
        size++;
    }

    @Override
    public void addLast(T item) {
        LLDNode result = new LLDNode(item);
        LLDNode original = sentinel.prev;
        sentinel.prev = result;
        sentinel.prev.prev = original;
        sentinel.prev.next = sentinel;
        original.next = sentinel.prev;
        size++;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void printDeque() {
        LLDNode p = sentinel;
        while (p.next != sentinel) {
            System.out.print(p.next.item + " ");
            p = p.next;
        }
        System.out.println();
    }

    @Override

    public T removeFirst() {
        if (sentinel.next == sentinel) {
            return null;
        }
        LLDNode original = sentinel.next;
        sentinel.next = sentinel.next.next;
        sentinel.next.prev = sentinel;
        size--;
        return original.item;

    }

//    public boolean isEmpty() {
//        return size() == 0;
//    }

    @Override

    public T removeLast() {
        if (sentinel.prev == sentinel) {
            return null;
        }
        LLDNode original = sentinel.prev;
        sentinel.prev = sentinel.prev.prev;
        sentinel.prev.next = sentinel;
        size--;
        return original.item;

    }

    @Override

    public T get(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        LLDNode p = sentinel.next;
        while (index > 0) {
            p = p.next;
            index--;
        }
        return p.item;
    }

    public T getRecursive(int index) {
        return getRecursiveHelper(index, sentinel.next);
    }

    private T getRecursiveHelper(int index, LLDNode L) {
        if (index < 0 || index >= size) {
            return null;
        } else if (index == 0) {
            return L.item;
        } else {
            return getRecursiveHelper(index - 1, L.next);
        }
    }
}
