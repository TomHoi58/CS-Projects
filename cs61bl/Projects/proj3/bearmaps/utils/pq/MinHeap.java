package bearmaps.utils.pq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;

/* A MinHeap class of Comparable elements backed by an ArrayList. */
public class MinHeap<E extends Comparable<E>> {

    /* An ArrayList that stores the elements in this MinHeap. */
    private ArrayList<E> contents;
    private int size;
    private HashMap<E, Integer> map;




    /* Initializes an empty MinHeap. */
    public MinHeap() {
        contents = new ArrayList<>();
        contents.add(null);
        this.map = new HashMap<>();
    }

    /* Returns the element at index INDEX, and null if it is out of bounds. */
    private E getElement(int index) {
        if (index >= contents.size()) {
            return null;
        } else {
            return contents.get(index);
        }
    }

    /* Sets the element at index INDEX to ELEMENT. If the ArrayList is not big
       enough, add elements until it is the right size. */
    private void setElement(int index, E element) {
        while (index >= contents.size()) {
            contents.add(null);
        }
        contents.set(index, element);
    }

    /* Swaps the elements at the two indices. */
    private void swap(int index1, int index2) {
        E element1 = getElement(index1);
        E element2 = getElement(index2);
        setElement(index2, element1);
        setElement(index1, element2);
        map.replace(element1, index2);
        map.replace(element2, index1);
    }

    /* Prints out the underlying heap sideways. Use for debugging. */
    @Override
    public String toString() {
        return toStringHelper(1, "");
    }

    /* Recursive helper method for toString. */
    private String toStringHelper(int index, String soFar) {
        if (getElement(index) == null) {
            return "";
        } else {
            String toReturn = "";
            int rightChild = getRightOf(index);
            toReturn += toStringHelper(rightChild, "        " + soFar);
            if (getElement(rightChild) != null) {
                toReturn += soFar + "    /";
            }
            toReturn += "\n" + soFar + getElement(index) + "\n";
            int leftChild = getLeftOf(index);
            if (getElement(leftChild) != null) {
                toReturn += soFar + "    \\";
            }
            toReturn += toStringHelper(leftChild, "        " + soFar);
            return toReturn;
        }
    }

    /* Returns the index of the left child of the element at index INDEX. */
    private int getLeftOf(int index) {
        return 2 * index;
    }

    /* Returns the index of the right child of the element at index INDEX. */
    private int getRightOf(int index) {
        return 2 * index + 1;
    }

    /* Returns the index of the parent of the element at index INDEX. */
    private int getParentOf(int index) {
        return index / 2;
    }

    /* Returns the index of the smaller element. At least one index has a
       non-null element. If the elements are equal, return either index. */
    private int min(int index1, int index2) {
        if (getElement(index1) == null) {
            return index2;
        }
        if (getElement(index2) == null) {
            return index1;
        }
        if (getElement(index1).compareTo(getElement(index2)) >= 0) {
            return index2;
        } else {
            return index1;
        }
    }

    /* Returns but does not remove the smallest element in the MinHeap. */
    public E findMin() {
        return getElement(1);
    }

    /* Bubbles up the element currently at index INDEX. */
    private void bubbleUp(int index) {
        swap(index, getParentOf(index));
    }

    /* Bubbles down the element currently at index INDEX. */
    private void bubbleDown(int index) {
        swap(index, min(getLeftOf(index), getRightOf(index)));
    }

    /* Returns the number of elements in the MinHeap. */
    public int size() {
        return size;
    }

    /* Inserts ELEMENT into the MinHeap. If ELEMENT is already in the MinHeap,
       throw an IllegalArgumentException.*/
    public void insert(E element) {
        if (contains(element)) {
            throw new IllegalArgumentException();
        } else {
            contents.add(element);
            map.put(element, contents.indexOf(element));
            size++;
            int index = contents.indexOf(element);
            while (index != 1 && getElement(index).compareTo(getElement(getParentOf(index))) <= 0) {
                bubbleUp(index);
                index = index / 2;
            }
        }
    }

    /* Returns and removes the smallest element in the MinHeap. */
    public E removeMin() {
        map.remove(getElement(1));
        swap(1, size());
        E result = contents.remove(size());
        size--;
        int index = 1;
        while (getLeftOf(index) <= size() && getElement(index).compareTo
            (getElement(min(getLeftOf(index), getRightOf(index)))) >= 0) {
            int temp = min(getLeftOf(index), getRightOf(index));
            bubbleDown(index);
            index = temp;
        }

        return result;
    }

    /* Replaces and updates the position of ELEMENT inside the MinHeap, which
       may have been mutated since the initial insert. If a copy of ELEMENT does
       not exist in the MinHeap, throw a NoSuchElementException. Item equality
       should be checked using .equals(), not ==. */
    public void update(E element) {
        if (!contains(element)) {
            throw new NoSuchElementException();
        }
        int index = map.get(element);
        contents.remove(index);
        contents.add(index, element);
        while (index != 1 && getElement(index).compareTo
            (getElement(getParentOf(index))) <= 0) {
            bubbleUp(index);
            index = index / 2;
        }

        while (getLeftOf(index) <= size() && getElement(index).compareTo
            (getElement(min(getLeftOf(index), getRightOf(index)))) >= 0) {
            int temp = min(getLeftOf(index), getRightOf(index));
            bubbleDown(index);
            index = temp;
        }

    }

    /* Returns true if ELEMENT is contained in the MinHeap. Item equality should
       be checked using .equals(), not ==. */
    public boolean contains(E element) {
        return map.containsKey(element);
    }
}
