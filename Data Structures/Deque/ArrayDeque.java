public class ArrayDeque<T> implements Deque<T> {

    private int size;
    private int nextFirst;
    private int nextLast;
    private T[] items;


    /**
     * Creates an empty array
     */
    public ArrayDeque() {

        this.items = (T[]) new Object[8];
        this.size = 0;
        nextFirst = 0;
        nextLast = 1;
    }

    /**
     * Makes sure that the index of an array is in range
     */
    private int wrap(int p) {
        return Math.floorMod(p, items.length);
    }

    private int capacity() {
        return items.length;
    }

    /**
     * Expands or reduces the length of the array
     */
    private void resize() {
        if (size == items.length) {

            T[] result = (T[]) new Object[size * 2];
            for (int i = 0; i < items.length; i++) {
                result[i] = items[wrap(nextFirst + 1 + i)];
            }
            items = result;
            nextFirst = items.length - 1;
            nextLast = size;


        } else if (items.length >= 16 && ((double) size / items.length) < 0.25) {
            T[] result = (T[]) new Object[items.length / 2];
            for (int i = 0; wrap(i + nextFirst + 1) != nextLast; i++) {
                result[i] = items[wrap(i + nextFirst + 1)];
            }
            items = result;
            nextFirst = items.length - 1;
            nextLast = size;

        }
    }

    @Override
    /** Adds an item to the beginning of an array*/
    public void addFirst(T item) {

        this.items[nextFirst] = item;
        nextFirst = Math.floorMod(nextFirst - 1, items.length);
        size++;
        resize();
    }

    /** Checks if an array is empty*/
//    public boolean isEmpty(){
//        return size == 0;
//    }

    @Override
    /** Adds an item to the end of an array*/
    public void addLast(T item) {

        this.items[nextLast] = item;
        this.nextLast = Math.floorMod((nextLast + 1), items.length);
        size++;
        resize();
    }

    /**
     * Gets the size of an array
     */
    @Override
    public int size() {
        return size;
    }

    @Override
    /** Prints the array */
    public void printDeque() {
        String result = "";
        for (int i = 0; wrap(i + nextFirst + 1) != nextLast; i++) {
            result = result + items[wrap(i + nextFirst + 1)] + " ";
        }
        System.out.println(result.substring(0, result.length() - 1));

    }

    @Override
    /** Removes an item from the beginning of an array */
    public T removeFirst() {
        if (isEmpty()) {
            return null;
        } else {
            T result = items[wrap(nextFirst + 1)];
            items[wrap(nextFirst + 1)] = items[nextFirst];
            nextFirst = wrap(nextFirst + 1);
            size--;
            resize();
            if (isEmpty()) {
                nextFirst = 0;
                nextLast = 1;
            }
            return result;
        }
    }

    @Override
    /** Removes an item from the end of an array*/
    public T removeLast() {
        if (isEmpty()) {
            return null;
        } else {
            T result = items[wrap(nextLast - 1)];
            items[wrap(nextLast - 1)] = items[nextLast];
            nextLast = wrap(nextLast - 1);
            size--;
            resize();
            if (isEmpty()) {
                nextFirst = 0;
                nextLast = 1;
            }
            return result;


        }
    }

    @Override
    /** Gets the item of an array at a certain index */
    public T get(int index) {
        if (index < 0 || index > items.length - 1) {
            throw new IllegalArgumentException("index out of range");
        }
        if (nextFirst > nextLast) {
            if (wrap(index + 1 + nextFirst) >= nextLast && wrap(index + 1 + nextFirst)
                    <= nextFirst) {
                return null;
            }
        }
        if (nextFirst < nextLast) {
            if (wrap(index + 1 + nextFirst) >= nextLast || wrap(index + 1 + nextFirst)
                    <= nextFirst) {
                return null;
            }
        }
        return items[wrap(index + 1 + nextFirst)];
    }


}

