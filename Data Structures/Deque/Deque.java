public interface Deque<T> {

    void addFirst(T item);

    void addLast(T item);

    default boolean isEmpty() {
        return size() == 0;
    }

    int size();

    T removeFirst();

    T removeLast();

    void printDeque();

    T get(int index);
}
