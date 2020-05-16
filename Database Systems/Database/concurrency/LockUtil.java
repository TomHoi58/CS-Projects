package edu.berkeley.cs186.database.concurrency;
// If you see this line, you have successfully pulled the latest changes from the skeleton for proj4!
import edu.berkeley.cs186.database.TransactionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * LockUtil is a declarative layer which simplifies multigranularity lock acquisition
 * for the user (you, in the second half of Part 2). Generally speaking, you should use LockUtil
 * for lock acquisition instead of calling LockContext methods directly.
 */
public class LockUtil {
    /**
     * Ensure that the current transaction can perform actions requiring LOCKTYPE on LOCKCONTEXT.
     *
     * This method should promote/escalate as needed, but should only grant the least
     * permissive set of locks needed.
     *
     * lockType is guaranteed to be one of: S, X, NL.
     *
     * If the current transaction is null (i.e. there is no current transaction), this method should do nothing.
     */
    public static void ensureSufficientLockHeld(LockContext lockContext, LockType lockType) {
        // TODO(proj4_part2): implement

        TransactionContext transaction = TransactionContext.getTransaction(); // current transaction

        //Transaction is null or NL lock, do nothing
        if (transaction == null || lockType.equals(LockType.NL)){
            return;
        }

        //If the parent is a table, need to check for Auto-escalation conditions are met first
        //if so, escalate the parent context
        LockContext pa = lockContext.parentContext();
        if (pa != null && pa.AutoEscalate && pa.capacity()>= 10 && pa.saturation(transaction)>= 0.2){
            pa.escalate(transaction);
        }

        //If the current effective lock has the enough permissions for the requested one, do nothing
        LockManager lockman = lockContext.lockman;
        ResourceName name = lockContext.getResourceName();
        if (LockType.substitutable(lockContext.getEffectiveLockType(transaction),lockType)){
            return;
        }

        //Case of S
        if (lockType.equals(LockType.S)){
            //Ensuring that we have the appropriate locks on ancestors
            List<LockContext> ancestors = new ArrayList<>();
            LockContext parent = lockContext.parentContext();
            while (parent != null){
                ancestors.add(0,parent);
                parent = parent.parentContext();
            }
            //If the ancestor has NL, acquire IS
            for (LockContext LC: ancestors){
                //If the parent is a table, need to check for Auto-escalation conditions are met first
                //if so, escalate the parent context, and we are done with the descendants since we have S/X lock above
                LockContext papa = LC.parentContext();
                if (papa != null && papa.AutoEscalate){
                    if (papa.capacity()>= 10 && papa.saturation(transaction)>= 0.2){
                        papa.escalate(transaction);
                        return;
                    }
                }

                if (lockman.getLockType(transaction,LC.getResourceName()).equals(LockType.NL)){
                    LC.acquire(transaction,LockType.IS);
                }
            }

            //Acquiring the lock on the resource
            if (lockman.getLockType(transaction,name).equals(LockType.NL)){
                //Originally hold NL
                lockContext.acquire(transaction,LockType.S);
            }else if (lockman.getLockType(transaction,name).equals(LockType.IS)){
                //Originally hold IS
                lockContext.escalate(transaction);
            }else if (lockman.getLockType(transaction,name).equals(LockType.IX)){
                //Originally hold IX
                lockContext.promote(transaction,LockType.SIX);
            }else {
                throw new UnsupportedOperationException("Should not be here");
            }
        }

        //Case of X
        if (lockType.equals(LockType.X)){
            //Ensuring that we have the appropriate locks on ancestors
            List<LockContext> ancestors = new ArrayList<>();
            LockContext parent = lockContext.parentContext();
            while (parent != null){
                ancestors.add(0,parent);
                parent = parent.parentContext();
            }
            //If the ancestor has NL, acquire IX
            //If the ancestor has IS, promote IX
            //If the ancestor has S, promote SIX
            for (LockContext LC: ancestors){
                //If the parent is a table, need to check for Auto-escalation conditions are met first
                //if so, escalate the parent context, and we are done with the descendants since we have S/X lock above
                LockContext papa = LC.parentContext();
                if (papa != null && papa.AutoEscalate && papa.capacity()>= 10 && papa.saturation(transaction)>= 0.2){
                    papa.escalate(transaction);
                    return;
                }
                if (lockman.getLockType(transaction,LC.getResourceName()).equals(LockType.NL)){
                    LC.acquire(transaction,LockType.IX);
                }else if (lockman.getLockType(transaction,LC.getResourceName()).equals(LockType.IS)){
                    LC.promote(transaction,LockType.IX);
                }else if (lockman.getLockType(transaction,LC.getResourceName()).equals(LockType.S)){
                    LC.promote(transaction,LockType.SIX);
                }
            }

            //Acquiring the lock on the resource
            if (lockman.getLockType(transaction,name).equals(LockType.NL)){
                //Originally hold NL
                lockContext.acquire(transaction,LockType.X);
            }else {
                //Originally hold IS/IX/SIX/S
                lockContext.escalate(transaction);
                lockContext.promote(transaction,LockType.X);

            }
        }








    }

    // TODO(proj4_part2): add helper methods as you see fit
    public static void ensureIXHeld(LockContext lockContext){
        TransactionContext transaction = TransactionContext.getTransaction(); // current transaction

        //Transaction is null, do nothing
        if (transaction == null){
            return;
        }


        //If the current effective lock has the enough permissions for the requested one, do nothing
        LockManager lockman = lockContext.lockman;
        ResourceName name = lockContext.getResourceName();
        if (LockType.substitutable(lockContext.getEffectiveLockType(transaction),LockType.IX)){
            return;
        }


        //Ensuring that we have the appropriate locks on ancestors
        List<LockContext> ancestors = new ArrayList<>();
        LockContext parent = lockContext.parentContext();
        while (parent != null){
            ancestors.add(0,parent);
            parent = parent.parentContext();
        }
        //If the ancestor has NL, acquire IX
        //If the ancestor has IS, promote IX
        //If the ancestor has S, promote SIX
        for (LockContext LC: ancestors){
            if (lockman.getLockType(transaction,LC.getResourceName()).equals(LockType.NL)){
                LC.acquire(transaction,LockType.IX);
            }else if (lockman.getLockType(transaction,LC.getResourceName()).equals(LockType.IS)){
                LC.promote(transaction,LockType.IX);
            }else if (lockman.getLockType(transaction,LC.getResourceName()).equals(LockType.S)){
                LC.promote(transaction,LockType.SIX);
            }
        }

        //Acquiring the lock on the resource
        if (lockman.getLockType(transaction,name).equals(LockType.NL)){
            //Originally hold NL
            lockContext.acquire(transaction,LockType.IX);
        }else if (lockman.getLockType(transaction,name).equals(LockType.IS)) {
            //Originally hold IS
            lockContext.promote(transaction,LockType.IX);
        }else if (lockman.getLockType(transaction,name).equals(LockType.S)){
            lockContext.promote(transaction,LockType.SIX);
        }

    }

}
