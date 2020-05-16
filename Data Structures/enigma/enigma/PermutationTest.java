package enigma;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.Timeout;

import static org.junit.Assert.*;

import static enigma.TestUtils.*;

/** The suite of all JUnit tests for the Permutation class.
 *  @author Jackson Leisure
 */
public class PermutationTest {

    /** Testing time limit. */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(5);

    /* ***** TESTING UTILITIES ***** */

    private Permutation perm;
    private String alpha = UPPER_STRING;

    /** Check that perm has an alphabet whose size is that of
     *  FROMALPHA and TOALPHA and that maps each character of
     *  FROMALPHA to the corresponding character of TOALPHA, and
     *  vice-versa. TESTID is used in error messages. */
    private void checkPerm(String testId,
                           String fromAlpha, String toAlpha) {
        int N = fromAlpha.length();
        assertEquals(testId + " (wrong length)", N, perm.size());
        for (int i = 0; i < N; i += 1) {
            char c = fromAlpha.charAt(i), e = toAlpha.charAt(i);
            assertEquals(msg(testId, "wrong translation of '%c'", c),
                         e, perm.permute(c));
            assertEquals(msg(testId, "wrong inverse of '%c'", e),
                         c, perm.invert(e));
            int ci = alpha.indexOf(c), ei = alpha.indexOf(e);
            assertEquals(msg(testId, "wrong translation of %d", ci),
                         ei, perm.permute(ci));
            assertEquals(msg(testId, "wrong inverse of %d", ei),
                         ci, perm.invert(ei));
        }
    }

    /* ***** TESTS ***** */

    @Test
    public void checkIdTransform() {
        perm = new Permutation("", UPPER);
        checkPerm("identity", UPPER_STRING, UPPER_STRING);
    }

    @Test
    public void checkForwardTransform() {
        perm = new Permutation("(ABCDEFGHIJKLMNOPQRSTUVWXYZ)", UPPER);
        checkPerm("forward", UPPER_STRING, UPPER_STRING.substring(1) + "A");
    }

    @Test
    public void checkBackwardTransform() {
        perm = new Permutation("(ZYXWVUTSRQPONMLKJIHGFEDCBA)", UPPER);
        checkPerm("backward", UPPER_STRING, "Z" + UPPER_STRING.substring(0, 25));
    }

    @Test
    public void checkNavalPermutation() {
        perm = new Permutation(NAVALA.get("I"), UPPER);
        checkPerm("naval A: I", UPPER_STRING, NAVALA_MAP.get("I"));
    }

    @Test
    public void navalA() {
        for (String key : NAVALA_MAP.keySet()) {
            perm = new Permutation(NAVALA.get(key), UPPER);
            checkPerm("naval A: " + key, UPPER_STRING, NAVALA_MAP.get(key));
        }
    }

    @Test
    public void navalB() {
        for (String key : NAVALB_MAP.keySet()) {
            perm = new Permutation(NAVALB.get(key), UPPER);
            checkPerm("naval B: " + key, UPPER_STRING, NAVALB_MAP.get(key));
        }
    }

    @Test
    public void navalZ() {
        for (String key : NAVALZ_MAP.keySet()) {
            perm = new Permutation(NAVALZ.get(key), UPPER);
            checkPerm("naval Z: " + key, UPPER_STRING, NAVALZ_MAP.get(key));
        }
    }

}
