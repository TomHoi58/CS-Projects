package enigma;

import static enigma.EnigmaException.*;

/** Superclass that represents a rotor in the enigma machine.
 *  @author
 */
public class Rotor {

    /** My name. */
    private final String _name;

    /** The permutation implemented by this rotor in its 0 position. */
    private Permutation _permutation;

    private int currentSetting;
    

    /** A rotor named NAME whose permutation is given by PERM. */
    public Rotor(String name, Permutation perm) {
        _name = name;
        _permutation = perm;
        /* Step 1: initialize current setting to be 0. */
        currentSetting = 0;
    }

    /** Return my name. */
    public String name() {
        return _name;
    }

    /** Return my alphabet. */
    public Alphabet alphabet() {
        return _permutation.alphabet();
    }

    /** Return my permutation. */
    public Permutation permutation() {
        return _permutation;
    }

    /** Return the size of my alphabet. */
    public int size() {
        return _permutation.size();
    }

    /** Return true if and only if I have a ratchet and can move. */
    public boolean rotates() {
        return false;
    }

    /** Return true if and only if I reflect. */
    public boolean reflecting() {
        return false;
    }

    /** COPY OF WRAP FROM PERMUTATION.java
        Return the value of P modulo the size of this permutation. */
    final int wrap(int p) {
        int r = p % size();
        if (r < 0) {
            r += size();
        }
        return r;
    }

    /** Return my current setting. */
    public int setting() {
        /* Step 2: see method comment. */
        return  currentSetting;
    }

    /** Set setting() to POSN.  */
    public void set(int posn) {
        /* Step 3: see method comment. */
        currentSetting = posn;
    }

    /** Set setting() to character CPOSN. */
    public void set(char cposn) {
        /* Step 4: see method comment.
           HINT: there is a alphabet() method. */
        set(alphabet().toInt(cposn));
    }

    /** Return the conversion of P (an integer in the range 0..size()-1)
     *  according to my permutation. */
    public int convertForward(int p) {
        /* Step 5: a) current setting + input character. then wrap.
                   b) permute the result
                   c) result - current setting. then wrap.*/
        int contact = wrap(currentSetting+ p);
        int result = permutation().permute(contact);
        int last = wrap(result-currentSetting);
        return last;
    }

    /** Return the conversion of C (an integer in the range 0..size()-1)
     *  according to the inverse of my permutation. */
    public int convertBackward(int c) {
        /* Step 6: a) current setting + input character. then wrap.
                   b) invert the result
                   c) result - current setting. then wrap.*/
        int contact = wrap(currentSetting+ c);
        int result = permutation().invert(contact);
        int last = wrap(result-currentSetting);
        return last;
    }

    /** Returns true if and only if I am positioned to allow the rotor
     * to my left to advance. */
    public boolean atNotch() {
        return false;
    }

    /** Advance me one position, if possible. By default, does nothing. */
    public void advance() {
    }

    @Override
    public String toString() {
        return "Rotor " + _name;
    }
}
