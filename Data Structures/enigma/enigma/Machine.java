package enigma;

import static enigma.EnigmaException.*;

/** Class that represents a complete enigma machine.
 *  @author
 */
public class Machine {

    /** Common alphabet of my rotors. */
    private final Alphabet _alphabet;
    private int _numRotors;
    private int _pawls;
    private Rotor[] _allRotors;
    private Permutation _plugboard;

    private Rotor[] myRotors; // An array to store rotors belonging to this Machine

    /** A new Enigma machine with alphabet ALPHA, 1 < NUMROTORS rotor slots,
     *  and 0 <= PAWLS < NUMROTORS pawls. ALLROTORS contains all the
     *  available rotors. */
    public Machine(Alphabet alpha, int numRotors, int pawls,
            Rotor[] allRotors) {
        _alphabet = alpha;
        _numRotors = numRotors;
        _pawls = pawls;
        _allRotors = allRotors;

        /* Step 1a: Define instance variables and initialize all of them.
                   HINT: you probably want to save all fields passed into
                   this constructor */
        myRotors = new Rotor[_numRotors];

        /* Step 1b: initialize myRotors to hold the correct number of rotors. */
    }

    /** Return the number of rotor slots I have. */
    public int numRotors() {
        /* Step 2: Did you save this number from before? */
        return _numRotors;
    }

    /** Return the number pawls (and thus rotating rotors) I have. */
    public int numPawls() {
        /* Step 3: Did you save this number from before? */
        return _pawls;
    }

    /** Set my rotor slots to the rotors named ROTORS from my set of
     *  available rotors (ROTORS[0] names the reflector).
     *  Initially, all rotors are set at their 0 setting. */
    public void insertRotors(String[] rotors) {
        /* Step 4: For each rotor name in rotors, find the Rotor in allRotors
                   whose name is equal to the rotor name and insert it into
                   myRotors. */
        for (int i = 0;i<rotors.length;i++){
            String wantedrotor = rotors[i].toUpperCase();
            for (int j = 0; j< _allRotors.length; j++){
                String matchrotor = _allRotors[j].name().toUpperCase();
                if (matchrotor.equals(wantedrotor)) {
                    myRotors[i]= _allRotors[j];
                }
            }
        }

        /* NOTE: Use String.toUpperCase() on both of the names before
                 you compare if they are equal to each other.

           NOTE: To check equality between Strings, you must use
                 .equals() between the two. "==" will not work. */
    }



    /** Set my rotors according to SETTING, which must be a string of
     *  numRotors()-1 upper-case letters. The first letter refers to the
     *  leftmost rotor setting (not counting the reflector).  */
    public void setRotors(String setting) {
        /* Step 5: iterate from myRotors[1] to myRotors[end], use setting
                   to determine what position they should be at. */
        setting = setting.toUpperCase();
        Rotor[] standbyrotors = new Rotor[setting.length()+1];
        char[] letterarray = setting.toCharArray();
        for (int i = 0; i< letterarray.length;i++){
            int wantedsetting = _alphabet.toInt(letterarray[i]);
            for (int j = 1; j<myRotors.length;j++){
                if (myRotors[j].setting() == wantedsetting) {
                    standbyrotors[i+1] = myRotors[j];

                }
            }
        }
        for (int i = 1; i<standbyrotors.length;i++){
            myRotors[i] = standbyrotors[i];
        }
    }

    /** Set the plugboard to PLUGBOARD. */
    public void setPlugboard(Permutation plugboard) {
        /* Step 6: create a _plugboard instance variable and save plugboard there */
        _plugboard = plugboard;
    }

    /** Returns the result of converting the input character C (as an
     *  index in the range 0..alphabet size - 1), after first advancing
     *  the machine. */
    public int convert(int c) {
        /* Step 7a: always advance() first */
        advance();
        /* Step 7b: permute the character with the plugboard. */
        int pluggedletter = _plugboard.permute(c);
        /* Step 7c: from myRotors[end] to myRotors[1] convertForward */
        for (int i = myRotors.length-1;i>0;i--){
            pluggedletter = myRotors[i].convertForward(pluggedletter);
        }
        /* Step 7d: convertForward on the reflector. Remember the reflector
                    is myRotors[0]. */
        pluggedletter = myRotors[0].convertForward(pluggedletter);
        /* Step 7e: from myRotors[1] to myRotors[end] convertBackward */
        for (int i = 1;i<myRotors.length;i++){
            pluggedletter = myRotors[i].convertBackward(pluggedletter);
        }
        /* Step 7f: invert character with the plugboard. */
        int result = _plugboard.invert(pluggedletter);

        return result;
    }

    /** Helper method for convert() which rotates the necessary Rotors. */


    /** Returns the encoding/decoding of MSG, updating the state of
     *  the rotors accordingly. */
    private void advance() {
        /* Step 8a: make boolean array, one element per rotor. */
        boolean[] indicator = new boolean[numRotors()];
        /* Step 8b: iterate through every rotor, starting from myRotors[1]. */
        /* Step 8c: if rotor i is at a notch, mark rotor i and i-1 to true
                    in boolean array. */
        for (int i =1; i<myRotors.length;i++){
            if (myRotors[i].atNotch()) {
                indicator[i] = true;
                indicator[i-1] = true;
            }
        }
        /* Step 8d: mark myRotors[end] as true. */
        indicator[numRotors()-1]= true;
        /* Step 8e: rotate all rotors who are marked true in the boolean array. */
        for (int j =0; j< myRotors.length; j++ ){
            if (indicator[j] == true){
                myRotors[j].advance();
            }
        }

    }

    public String convert(String msg) {
        /* Step 9a: Capitalize msg and save it to a variable. */
        String bettername = msg.toUpperCase();
        /* Step 9b: Create an empty string, called output. */
        String output = "";
        /* Step 9c: For each character in the capitalized msg, convert the
                    character using convert(int c) and add it to output.
                    NOTE: convert(int c) takes in an INT, not a CHAR. Use
                          _alphabet methods to translate the character between
                          int's and char's as necessary.*/
        char[] betterarray = bettername.toCharArray();
        for (int i =0 ; i<betterarray.length;i++){
            int index = _alphabet.toInt(betterarray[i]);
            int covertindex = convert(index);
            betterarray[i] = _alphabet.toChar(covertindex);
        }
        /* Step 9d: Return the output. */
        output = new String(betterarray);

        return output;
    }

    // To run this through command line, from the proj0 directory, run the following:
    // javac enigma/Machine.java enigma/Rotor.java enigma/FixedRotor.java enigma/Reflector.java enigma/MovingRotor.java enigma/Permutation.java enigma/Alphabet.java enigma/CharacterRange.java enigma/EnigmaException.java
    // java enigma/Machine
    public static void main(String[] args) {

        CharacterRange upper = new CharacterRange('A', 'Z');
        MovingRotor rotorI = new MovingRotor("I",
                new Permutation("(AELTPHQXRU) (BKNW) (CMOY) (DFG) (IV) (JZ) (S)", upper),
                "Q");
        MovingRotor rotorII = new MovingRotor("II",
                new Permutation("(FIXVYOMW) (CDKLHUP) (ESZ) (BJ) (GR) (NT) (A) (Q)", upper),
                "E");
        MovingRotor rotorIII = new MovingRotor("III",
                new Permutation("(ABDHPEJT) (CFLVMZOYQIRWUKXSG) (N)", upper),
                "V");
        MovingRotor rotorIV = new MovingRotor("IV",
                new Permutation("(AEPLIYWCOXMRFZBSTGJQNH) (DV) (KU)", upper),
                "J");
        MovingRotor rotorV = new MovingRotor("V",
                new Permutation("(AVOLDRWFIUQ)(BZKSMNHYC) (EGTJPX)", upper),
                "Z");
        FixedRotor rotorBeta = new FixedRotor("Beta",
                new Permutation("(ALBEVFCYODJWUGNMQTZSKPR) (HIX)", upper));
        FixedRotor rotorGamma = new FixedRotor("Gamma",
                new Permutation("(AFNIRLBSQWVXGUZDKMTPCOYJHE)", upper));
        Reflector rotorB = new Reflector("B",
                new Permutation("(AE) (BN) (CK) (DQ) (FU) (GY) (HW) (IJ) (LO) (MP) (RX) (SZ) (TV)", upper));
        Reflector rotorC = new Reflector("C",
                new Permutation("(AR) (BD) (CO) (EJ) (FN) (GT) (HK) (IV) (LM) (PW) (QZ) (SX) (UY)", upper));

        Rotor[] allRotors = new Rotor[9];
        allRotors[0] = rotorI;
        allRotors[1] = rotorII;
        allRotors[2] = rotorIII;
        allRotors[3] = rotorIV;
        allRotors[4] = rotorV;
        allRotors[5] = rotorBeta;
        allRotors[6] = rotorGamma;
        allRotors[7] = rotorB;
        allRotors[8] = rotorC;

        Machine machine = new Machine(upper, 5, 3, allRotors);
        machine.insertRotors(new String[]{"B", "BETA", "III", "IV", "I"});
        machine.setRotors("AXLE");
        machine.setPlugboard(new Permutation("(HQ) (EX) (IP) (TR) (BY)", upper));

        System.out.println(machine.numRotors() == 5);
        System.out.println(machine.numPawls() == 3);
        System.out.println(machine.convert(5) == 16);
        System.out.println(machine.convert(17) == 21);
        System.out.println(machine.convert("OMHISSHOULDERHIAWATHA").equals("PQSOKOILPUBKJZPISFXDW"));
        System.out.println(machine.convert("TOOK THE CAMERA OF ROSEWOOD").equals("BHCNSCXNUOAATZXSRCFYDGU"));
        System.out.println(machine.convert("Made of sliding folding rosewood").equals("FLPNXGXIXTYJUJRCAUGEUNCFMKUF"));
    }
}
