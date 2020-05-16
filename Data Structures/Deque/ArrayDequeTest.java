import org.junit.Test;
import static org.junit.Assert.*;

public class ArrayDequeTest {

    @Test
    public void addFirst() {
        ArrayDeque test1 = new ArrayDeque();
        test1.addFirst(1);
        test1.addFirst(2);
        test1.addFirst(3);
        test1.addFirst(4);
        test1.addFirst(5);
        test1.addFirst(6);
        test1.addFirst(7);
        test1.addFirst(8);
        test1.addFirst(9);
        System.out.println(test1.size());
        test1.printDeque();
    }

    @Test
    public void addLast() {
        ArrayDeque test1 = new ArrayDeque();
        test1.addLast(1);
        test1.addLast(2);
        test1.addLast(3);
        test1.addLast(4);
        test1.addLast(5);
        test1.addLast(6);
        test1.addLast(7);
        test1.addLast(8);
        test1.addLast(9);
        System.out.println(test1.size());
        test1.printDeque();
    }

    @Test
    public void resize() {
        ArrayDeque test1 = new ArrayDeque();
        test1.addLast(1);
        test1.addLast(2);
        test1.addLast(3);
        test1.addLast(4);
        test1.addLast(5);
        test1.addLast(6);
        test1.addLast(7);
        test1.addLast(8);
        test1.addLast(9);
        assertEquals(9, test1.size());


        test1.printDeque();
        test1.removeLast();
        test1.removeLast();
        test1.removeLast();
        test1.removeLast();
        test1.removeLast();

        test1.removeFirst();

    }

    @Test
    public void removeTest() {
        ArrayDeque test1 = new ArrayDeque();
        test1.addLast(1);
        test1.addLast(2);
        test1.addLast(3);
        test1.addLast(4);
        test1.addLast(5);
        test1.addLast(6);
        test1.addLast(7);
        test1.addLast(8);
        test1.addLast(9);
        assertEquals(9, test1.size());


        test1.printDeque();
        test1.removeLast();
        test1.removeLast();
        test1.removeLast();
        test1.removeLast();
        test1.removeLast();
        test1.removeFirst();
        test1.printDeque();
        test1.removeFirst();
        test1.removeFirst();
        test1.removeFirst();
        System.out.println(test1.removeFirst());
    }

    @Test
    public void arrayDequeTest() {
        ArrayDeque test1 = new ArrayDeque();
        test1.addLast(1);
        test1.addLast(2);
        test1.addLast(3);
        test1.addLast(4);
        test1.addLast(5);
        test1.addLast(6);
        test1.addLast(7);
        test1.addLast(8);
        test1.addLast(9);
        System.out.println(test1.size() == 9);
        test1.printDeque();
        test1.removeLast();
        test1.removeLast();
        test1.removeLast();
        test1.removeLast();
        test1.removeLast();
        test1.removeFirst();
        System.out.println(test1.size() == 3);
        test1.printDeque();
    }


    @Test
    public void getTest() {
        ArrayDeque test1 = new ArrayDeque();
        test1.addLast(1);
        test1.addLast(2);
        test1.addLast(3);
        test1.addLast(4);
        test1.addLast(5);
        test1.addLast(6);
        test1.addLast(7);
        test1.addLast(8);
        test1.addLast(9);
        test1.addFirst(10);
        test1.removeLast();
        System.out.println(test1.get(0));
        System.out.println(test1.get(8));
        test1.printDeque();
    }

}
