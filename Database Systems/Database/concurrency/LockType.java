package edu.berkeley.cs186.database.concurrency;

// If you see this line, you have successfully pulled the latest changes from the skeleton for proj4!

import java.util.Arrays;
import java.util.HashMap;

public enum LockType {
    S,   // shared
    X,   // exclusive
    IS,  // intention shared
    IX,  // intention exclusive
    SIX, // shared intention exclusive
    NL;  // no lock held

    /**
     * This method checks whether lock types A and B are compatible with
     * each other. If a transaction can hold lock type A on a resource
     * at the same time another transaction holds lock type B on the same
     * resource, the lock types are compatible.
     */
    public static boolean compatible(LockType a, LockType b) {
        if (a == null || b == null) {
            throw new NullPointerException("null lock type");
        }
        // TODO(proj4_part1): implement


        //Build a mapping between Lock type to integer 0-5
        HashMap<LockType,Integer> map = new HashMap<>();
        map.put(NL,0);
        map.put(IS,1);
        map.put(IX,2);
        map.put(SIX,3);
        map.put(S,4);
        map.put(X,5);

        //Build a matrix that shows the compatibility between one another
        boolean[][] matrix = new boolean[6][6];

        Arrays.fill(matrix[0],true);

        Arrays.fill(matrix[1],true);
        matrix[1][5] = false;

        Arrays.fill(matrix[2],0,3,true);
        Arrays.fill(matrix[2],3,6,false);

        Arrays.fill(matrix[3],0,2,true);
        Arrays.fill(matrix[3],2,6,false);

        Arrays.fill(matrix[4],0,2,true);
        Arrays.fill(matrix[4],2,6,false);
        matrix[4][4] = true;

        Arrays.fill(matrix[5],false);
        matrix[5][0] = true;

        //Check the matrix to see if the two locks are compatible
        return matrix[map.get(a)][map.get(b)];
    }

    /**
     * This method returns the lock on the parent resource
     * that should be requested for a lock of type A to be granted.
     */
    public static LockType parentLock(LockType a) {
        if (a == null) {
            throw new NullPointerException("null lock type");
        }
        switch (a) {
        case S: return IS;
        case X: return IX;
        case IS: return IS;
        case IX: return IX;
        case SIX: return IX;
        case NL: return NL;
        default: throw new UnsupportedOperationException("bad lock type");
        }
    }

    /**
     * This method returns if parentLockType has permissions to grant a childLockType
     * on a child.
     */
    public static boolean canBeParentLock(LockType parentLockType, LockType childLockType) {
        if (parentLockType == null || childLockType == null) {
            throw new NullPointerException("null lock type");
        }
        // TODO(proj4_part1): implement

        //Build a mapping between Lock type to integer 0-5
        HashMap<LockType,Integer> map = new HashMap<>();
        map.put(NL,0);
        map.put(IS,1);
        map.put(IX,2);
        map.put(SIX,3);
        map.put(S,4);
        map.put(X,5);

        //Build a matrix that shows the canBeParentLock relationships
        boolean[][] matrix = new boolean[6][6];

        Arrays.fill(matrix[0],false);
        matrix[0][0] = true;

        Arrays.fill(matrix[1],false);
        matrix[1][0] = true;
        matrix[1][1] = true;
        matrix[1][4] = true;

        Arrays.fill(matrix[2],true);

        Arrays.fill(matrix[3],false);
        matrix[3][0] = true;
        matrix[3][2] = true;
        matrix[3][5] = true;

        Arrays.fill(matrix[4],false);
        matrix[4][0] = true;

        Arrays.fill(matrix[5],false);
        matrix[5][0] = true;


        //Check the matrix to see if it can be parent
        return matrix[map.get(parentLockType)][map.get(childLockType)];
    }

    /**
     * This method returns whether a lock can be used for a situation
     * requiring another lock (e.g. an S lock can be substituted with
     * an X lock, because an X lock allows the transaction to do everything
     * the S lock allowed it to do).
     */
    public static boolean substitutable(LockType substitute, LockType required) {
        if (required == null || substitute == null) {
            throw new NullPointerException("null lock type");
        }
        // TODO(proj4_part1): implement

        //Build a mapping between Lock type to integer 0-5
        HashMap<LockType,Integer> map = new HashMap<>();
        map.put(NL,0);
        map.put(IS,1);
        map.put(IX,2);
        map.put(SIX,3);
        map.put(S,4);
        map.put(X,5);

        //Build a matrix that shows the substitutability
        boolean[][] matrix = new boolean[6][6];

        Arrays.fill(matrix[0],false);
        matrix[0][0] = true;

        Arrays.fill(matrix[1],false);
        matrix[1][0] = true;
        matrix[1][1] = true;

        Arrays.fill(matrix[2],0,3,true);
        Arrays.fill(matrix[2],3,6,false);

        Arrays.fill(matrix[3],false);
        matrix[3][0] = true;
        matrix[3][3] = true;
        matrix[3][4] = true;

        Arrays.fill(matrix[4],false);
        matrix[4][0] = true;
        matrix[4][4] = true;

        Arrays.fill(matrix[5],false);
        matrix[5][0] = true;
        matrix[5][4] = true;
        matrix[5][5] = true;


        //Check the matrix to see if it can be parent
        return matrix[map.get(substitute)][map.get(required)];

    }

    @Override
    public String toString() {
        switch (this) {
        case S: return "S";
        case X: return "X";
        case IS: return "IS";
        case IX: return "IX";
        case SIX: return "SIX";
        case NL: return "NL";
        default: throw new UnsupportedOperationException("bad lock type");
        }
    }
}

