package enigma;

import static enigma.EnigmaException.*;

/** Class that represents a rotating rotor in the enigma machine.
 *  @author
 */
public class MovingRotor extends Rotor {
    /** A rotor named NAME whose permutation in its default setting is
     *  PERM, and whose notches are at the positions indicated in NOTCHES.
     *  The Rotor is initially in its 0 setting (first character of its
     *  alphabet).
     */
    private String _notches;

    public MovingRotor(String name, Permutation perm, String notches) {
        super(name, perm);
        /* Step 1: Save notches to instance variable _notches. */
        _notches = notches;
    }

    @Override
    public boolean rotates() {
        return true;
    }

    @Override
    public boolean atNotch() {
        /* Step 2a: Get our current setting.
                    HINT: This Class inherits all information present in the Rotor class, meaning
                    that any method which exists in Rotor can be used in MovingRotor. */
        int currentSetting = super.setting();

        /* Step 2b: Check if current setting is inside _notches */
        String letter = String.valueOf(super.alphabet().toChar(currentSetting));
        return _notches.contains(letter);
    }

    @Override
    public void advance() {
        /* NOTE: All this function should do, is increment current setting by 1. Logic for
                 when we should advance is handled in Machine.java. */

        /* Step 3a: Follow NOTE.
                   HINT: This Class inherits all information present in the Rotor class, meaning
                   that any method which exists in Rotor can be used in MovingRotor. */
        int wrappedcs = super.wrap(super.setting()+1);


        /* Step 3b: Don't forget to wrap before you set. */
        super.set(wrappedcs);
    }

    // To run this through command line, from the proj0 directory, run the following:
    // javac enigma/Rotor.java enigma/MovingRotor.java enigma/Permutation.java enigma/Alphabet.java enigma/CharacterRange.java enigma/EnigmaException.java
    // java enigma/MovingRotor
    public static void main(String[] args) {
        Permutation perm = new Permutation("(AB) (CDEFGHIJKLMNOPQRSTUVWXYZ)", new CharacterRange('A', 'Z'));
        MovingRotor rotor = new MovingRotor("forward one", perm, "B");

        System.out.println(rotor.name().equals("forward one"));
        System.out.println(rotor.alphabet() == perm.alphabet());
        System.out.println(rotor.permutation() == perm);
        System.out.println(rotor.rotates() == true);
        System.out.println(rotor.reflecting() == false);

        System.out.println(rotor.size() == 26);
        rotor.set(1);
        System.out.println(rotor.setting() == 1);
        System.out.println(rotor.atNotch() == true);
        rotor.set('A');
        System.out.println(rotor.setting() == 0);
        System.out.println(rotor.atNotch() == false);
        System.out.println(rotor.convertForward(0) == 1);
        System.out.println(rotor.convertBackward(1) == 0);
        rotor.advance();
        System.out.println(rotor.setting() == 1);
        System.out.println(rotor.atNotch() == true);
        System.out.println(rotor.convertForward(0) == 25);
        System.out.println(rotor.convertBackward(25) == 0);
    }

}
