package enigma;

import static enigma.EnigmaException.*;

/** Represents a permutation of a range of integers starting at 0 corresponding
 *  to the characters of an alphabet.
 *  @author
 */
public class Permutation {

    /** Alphabet of this permutation. */
    private Alphabet _alphabet;
    private String[] _cycles; // A string to store the permutation cycle.


    /** Set this Permutation to that specified by CYCLES, a string in the
     *  form "(cccc) (cc) ..." where the c's are characters in ALPHABET, which
     *  is interpreted as a permutation in cycle notation.  Characters in the
     *  alphabet that are not included in any cycle map to themselves.
     *  Whitespace is ignored. */
    public Permutation(String cycles, Alphabet alphabet) {
        _alphabet = alphabet;
        _cycles = cycles.substring(1,cycles.length()-1).split("\\) \\(");
        /* Step 1: Save cycles to instance variable _cycles. */
    }

    /** Return the value of P modulo the size of this permutation. */
    final int wrap(int p) {
        int r = p % size();
        if (r < 0) {
            r += size();
        }
        return r;
    }

    /** Returns the size of the alphabet I permute. */
    public int size() {
        /* Step 2: Return the size of _alphabet. Look at Alphabet.java
           to find function. */
        return _alphabet.size();
    }

    /** Return the character result of applying this permutation to the index
     * of character P in ALPHABET. */
    public char permute(char p) {
        return _alphabet.toChar(permute(_alphabet.toInt(p)));
    }

    /** Return the index result of applying this permutation to the character
     *  at index P in ALPHABET. */
    public int permute(int p) {

        // NOTE: it might be beneficial to have one permute() method always call the other
        char letter = _alphabet.toChar(wrap(p));
        for (int i = 0;i < _cycles.length;i++){
            String cycle = _cycles[i];
            int len = cycle.length();
            if (cycle.indexOf(letter) == -1) {
                continue;
            } else if (cycle.indexOf(letter) == len - 1) {
                return _alphabet.toInt(cycle.charAt(0));

            } else {
                return _alphabet.toInt(cycle.charAt(cycle.indexOf(letter) + 1));
            }
        }
        return p;  // FIXME - How do we use our instance variables to get the index that P permutes to?
    }
    /** Return the character result of applying the inverse of this permutation
     * to the index of character P in ALPHABET. */
    public char invert(char c) {
        return _alphabet.toChar(invert(_alphabet.toInt(c)));
    }

    /** Return the index result of applying the inverse of this permutation
     *  to the character at index C in ALPHABET. */
    public int invert(int c) {
    	/* Step 6: find a way to use invert(char c) to invert our input int. */
        char letter = _alphabet.toChar(c);
        for (int i = 0;i < _cycles.length;i++){
            String cycle = _cycles[i];
            int len = cycle.length();
            if (cycle.indexOf(letter) == -1) {
                continue;
            } else if (cycle.indexOf(letter) == 0) {
                return _alphabet.toInt(cycle.charAt(len - 1));

            } else {
                return _alphabet.toInt(cycle.charAt(cycle.indexOf(letter) - 1));
            }
        }
        return c;
    }

    /** Return the alphabet used to initialize this Permutation. */
    public Alphabet alphabet() {
        return _alphabet;
    }

    // Some starter code for unit tests. Feel free to change these up!
    // To run this through command line, from the proj0 directory, run the following:
    // javac enigma/Permutation.java enigma/Alphabet.java enigma/CharacterRange.java enigma/EnigmaException.java
    // java enigma/Permutation
    public static void main(String[] args) {
        Permutation perm = new Permutation("(ABCDEFGHIJKLMNOPQRSTUVWXYZ)", new CharacterRange('A', 'Z'));
        System.out.println(perm.size() == 26);
        System.out.println(perm.permute('A') == 'B');
        System.out.println(perm.invert('B') == 'A');
        System.out.println(perm.permute(0) == 1);
        System.out.println(perm.invert(1) == 0);
    }
}
