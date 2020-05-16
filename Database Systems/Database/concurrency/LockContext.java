package edu.berkeley.cs186.database.concurrency;
// If you see this line, you have successfully pulled the latest changes from the skeleton for proj4!
import edu.berkeley.cs186.database.TransactionContext;
import edu.berkeley.cs186.database.common.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LockContext wraps around LockManager to provide the hierarchical structure
 * of multigranularity locking. Calls to acquire/release/etc. locks should
 * be mostly done through a LockContext, which provides access to locking
 * methods at a certain point in the hierarchy (database, table X, etc.)
 */
public class LockContext {
    // You should not remove any of these fields. You may add additional fields/methods as you see fit.

    // The underlying lock manager.
    protected final LockManager lockman;
    // The parent LockContext object, or null if this LockContext is at the top of the hierarchy.
    protected final LockContext parent;
    // The name of the resource this LockContext represents.
    protected ResourceName name;
    // Whether this LockContext is readonly. If a LockContext is readonly, acquire/release/promote/escalate should
    // throw an UnsupportedOperationException.
    protected boolean readonly;


    // TODO(proj4_part3): implement
    // AutoEscalate is only true for table with enableAutoEscalate() call
    public boolean AutoEscalate ;


    // A mapping between transaction numbers, and the number of locks on children of this LockContext
    // that the transaction holds.
    protected final Map<Long, Integer> numChildLocks;
    // The number of children that this LockContext has, if it differs from the number of times
    // LockContext#childContext was called with unique parameters: for a table, we do not
    // explicitly create a LockContext for every page (we create them as needed), but
    // the capacity should be the number of pages in the table, so we use this
    // field to override the return value for capacity().
    protected int capacity;

    // You should not modify or use this directly.
    protected final Map<Long, LockContext> children;

    // Whether or not any new child LockContexts should be marked readonly.
    protected boolean childLocksDisabled;

    public LockContext(LockManager lockman, LockContext parent, Pair<String, Long> name) {
        this(lockman, parent, name, false);
    }

    protected LockContext(LockManager lockman, LockContext parent, Pair<String, Long> name,
                          boolean readonly) {
        this.lockman = lockman;
        this.parent = parent;
        if (parent == null) {
            this.name = new ResourceName(name);
        } else {
            this.name = new ResourceName(parent.getResourceName(), name);
        }
        this.readonly = readonly;
        this.numChildLocks = new ConcurrentHashMap<>();
        this.capacity = -1;
        this.children = new ConcurrentHashMap<>();
        this.childLocksDisabled = readonly;
    }

    /**
     * Gets a lock context corresponding to NAME from a lock manager.
     */
    public static LockContext fromResourceName(LockManager lockman, ResourceName name) {
        Iterator<Pair<String, Long>> names = name.getNames().iterator();
        LockContext ctx;
        Pair<String, Long> n1 = names.next();
        ctx = lockman.context(n1.getFirst(), n1.getSecond());
        while (names.hasNext()) {
            Pair<String, Long> p = names.next();
            ctx = ctx.childContext(p.getFirst(), p.getSecond());
        }
        return ctx;
    }

    /**
     * Get the name of the resource that this lock context pertains to.
     */
    public ResourceName getResourceName() {
        return name;
    }

    /**
     * Acquire a LOCKTYPE lock, for transaction TRANSACTION.
     *
     * Note: you *must* make any necessary updates to numChildLocks, or
     * else calls to LockContext#saturation will not work properly.
     *
     * @throws InvalidLockException if the request is invalid
     * @throws DuplicateLockRequestException if a lock is already held by TRANSACTION
     * @throws UnsupportedOperationException if context is readonly
     */
    public void acquire(TransactionContext transaction, LockType lockType)
    throws InvalidLockException, DuplicateLockRequestException {
        // TODO(proj4_part2): implement

        //UnsupportedOperationException
        if (this.readonly){
            throw new UnsupportedOperationException("LockContext.acquire");
        }

        //InvalidLockException: Use canBeParentLock
        LockContext parent = this.parentContext();
        while(parent != null){
            if (!LockType.canBeParentLock(parent.lockman.getLockType(transaction,parent.getResourceName()),lockType)){
                throw new InvalidLockException("LockContext.acquire");
            }
            parent = parent.parentContext();
        }

        //DuplicateLockRequestException: Handled by LockManager.acquire()
        lockman.acquire(transaction,this.getResourceName(),lockType);

        //Update numChildLocks
        if (parentContext() != null){
            this.parentContext().numChildLocks.put(transaction.getTransNum(),
                1+this.parentContext().numChildLocks.getOrDefault(transaction.getTransNum(),0));
        }


    }

    /**
     * Release TRANSACTION's lock on NAME.
     *
     * Note: you *must* make any necessary updates to numChildLocks, or
     * else calls to LockContext#saturation will not work properly.
     *
     * @throws NoLockHeldException if no lock on NAME is held by TRANSACTION
     * @throws InvalidLockException if the lock cannot be released (because doing so would
     *  violate multigranularity locking constraints)
     * @throws UnsupportedOperationException if context is readonly
     */
    public void release(TransactionContext transaction)
    throws NoLockHeldException, InvalidLockException {
        // TODO(proj4_part2): implement

        //UnsupportedOperationException
        if (this.readonly){
            throw new UnsupportedOperationException("LockContext.release");
        }

        //InvalidLockException: IS, IX, SIX should only be released if all of its children have NL locks
        if (lockman.getLockType(transaction,this.getResourceName()).equals(LockType.IS)||
            lockman.getLockType(transaction,this.getResourceName()).equals(LockType.IX)||
            lockman.getLockType(transaction,this.getResourceName()).equals(LockType.SIX)){
            if (this.numChildLocks.getOrDefault(transaction.getTransNum(),0) != 0){
                throw new InvalidLockException("LockContext.release");
            }
        }

        //DuplicateLockRequestException: Handled by LockManager.release()
        lockman.release(transaction,getResourceName());

        //Update to numChildLocks
        if (parentContext() != null){
            if(this.parentContext().numChildLocks.getOrDefault(transaction.getTransNum(),0) > 0){
                this.parentContext().numChildLocks.put(transaction.getTransNum(),
                    this.parentContext().numChildLocks.getOrDefault(transaction.getTransNum(),0)-1);
            }
        }
    }

    /**
     * Promote TRANSACTION's lock to NEWLOCKTYPE. For promotion to SIX from IS/IX/S, all S,
     * IS, and SIX locks on descendants must be simultaneously released. The helper function sisDescendants
     * may be helpful here.
     *
     * Note: you *must* make any necessary updates to numChildLocks, or
     * else calls to LockContext#saturation will not work properly.
     *
     * @throws DuplicateLockRequestException if TRANSACTION already has a NEWLOCKTYPE lock
     * @throws NoLockHeldException if TRANSACTION has no lock
     * @throws InvalidLockException if the requested lock type is not a promotion or promoting
     * would cause the lock manager to enter an invalid state (e.g. IS(parent), X(child)). A promotion
     * from lock type A to lock type B is valid if B is substitutable
     * for A and B is not equal to A, or if B is SIX and A is IS/IX/S, and invalid otherwise. hasSIXAncestor may
     * be helpful here.
     * @throws UnsupportedOperationException if context is readonly
     */
    public void promote(TransactionContext transaction, LockType newLockType)
    throws DuplicateLockRequestException, NoLockHeldException, InvalidLockException {
        // TODO(proj4_part2): implement

        //UnsupportedOperationException
        if (this.readonly){
            throw new UnsupportedOperationException("LockContext.promote");
        }

        //DuplicateLockRequestException
        if (lockman.getLockType(transaction,this.getResourceName()).equals(newLockType)){
            throw new DuplicateLockRequestException("LockContext.promote");
        }

        //NoLockHeldException
        if (this.lockman.getLockType(transaction,getResourceName()).equals(LockType.NL)){
            throw new NoLockHeldException("LockContext.promote");
        }


        //InvalidLockException: if ancestor has SIX and it's promoting to SIX
        if (newLockType.equals(LockType.SIX) && this.hasSIXAncestor(transaction)){
            throw new InvalidLockException("LockContext.promote");

        }
        //promotion to SIX from IS/IX/S,acquireAndRelease(), release all locks in the list of sisDescendants. Update numChildLocks
        if (newLockType.equals(LockType.SIX)){
            if (lockman.getLockType(transaction,getResourceName()).equals(LockType.IS)||
                lockman.getLockType(transaction,getResourceName()).equals(LockType.IX)||
                lockman.getLockType(transaction,getResourceName()).equals(LockType.S)){
                List<ResourceName> releaseList = this.sisDescendants(transaction);
                releaseList.add(0,this.getResourceName());
                lockman.acquireAndRelease(transaction,this.getResourceName(),LockType.SIX,releaseList);
                for (ResourceName n : releaseList){
                    LockContext p = fromResourceName(this.lockman,n).parentContext();
                    if (p.numChildLocks.getOrDefault(transaction.getTransNum(),0) > 0){
                        p.numChildLocks.put(transaction.getTransNum(),p.numChildLocks.getOrDefault(transaction.getTransNum(),0)-1);
                    }
                }

            }
        }else {
            //InvalidLockException: LockManager.promote()
            lockman.promote(transaction,getResourceName(),newLockType);
        }


    }

    /**
     * Escalate TRANSACTION's lock from descendants of this context to this level, using either
     * an S or X lock. There should be no descendant locks after this
     * call, and every operation valid on descendants of this context before this call
     * must still be valid. You should only make *one* mutating call to the lock manager,
     * and should only request information about TRANSACTION from the lock manager.
     *
     * For example, if a transaction has the following locks:
     *      IX(database) IX(table1) S(table2) S(table1 page3) X(table1 page5)
     * then after table1Context.escalate(transaction) is called, we should have:
     *      IX(database) X(table1) S(table2)
     *
     * You should not make any mutating calls if the locks held by the transaction do not change
     * (such as when you call escalate multiple times in a row).
     *
     * Note: you *must* make any necessary updates to numChildLocks of all relevant contexts, or
     * else calls to LockContext#saturation will not work properly.
     *
     * @throws NoLockHeldException if TRANSACTION has no lock at this level
     * @throws UnsupportedOperationException if context is readonly
     */
    public void escalate(TransactionContext transaction) throws NoLockHeldException {
        // TODO(proj4_part2): implement

        //UnsupportedOperationException
        if (this.readonly){
            throw new UnsupportedOperationException("escalate");
        }

        //NoLockHeldException
        if (this.lockman.getLockType(transaction,getResourceName()).equals(LockType.NL)){
            throw new NoLockHeldException("escalate");
        }

        //Traverse all the descendants' locks hold by the transaction and decide X or S.
        //Set variable seenx, if true, set to x. If false, set to s if current is not ix, six, x. else, set to x
        boolean seenx = this.hasXIXDescendants(transaction);
        List<ResourceName> releaseList = this.clearDescendantsLocks(transaction);
        if (releaseList.isEmpty()){
            if (this.lockman.getLockType(transaction,getResourceName()).equals(LockType.X) ||
                this.lockman.getLockType(transaction,getResourceName()).equals(LockType.S)){
                return;
            }
        }
        releaseList.add(0,this.getResourceName());
        if (seenx){
            lockman.acquireAndRelease(transaction,getResourceName(),LockType.X,releaseList);
        }else{
            if (lockman.getLockType(transaction,getResourceName()).equals(LockType.IX)||
                lockman.getLockType(transaction,getResourceName()).equals(LockType.SIX)||
                lockman.getLockType(transaction,getResourceName()).equals(LockType.X)){
                lockman.acquireAndRelease(transaction,getResourceName(),LockType.X,releaseList);
            }else {
                lockman.acquireAndRelease(transaction,getResourceName(),LockType.S,releaseList);
            }
        }
    }

    /**
     * Helper method to see if the transaction holds a X/IX lock at descendants of this context
     * @param transaction the transaction
     * @return true if holds a X/IX at descendants, false if not
     */
    private boolean hasXIXDescendants(TransactionContext transaction) {
        // TODO(proj4_part2): implement
        boolean seenXorIX = false;
        for (int i = 0; i <capacity(); i++){
            LockContext child = this.childContext((long) i);
            if (child.lockman.getLockType(transaction,child.getResourceName()).equals(LockType.IX) ||
                child.lockman.getLockType(transaction,child.getResourceName()).equals(LockType.X)){
                seenXorIX = true;
                break;
            }else {
                seenXorIX = child.hasXIXDescendants(transaction);
            }
        }
        return seenXorIX;
    }

    /**
     * Helper method to get a list of resourceName of its descendants who have a lock to be released
     * @param transaction the transaction
     * @return true list of ResourceName
     */
    private List<ResourceName> clearDescendantsLocks(TransactionContext transaction) {
        // TODO(proj4_part2): implement
        this.numChildLocks.put(transaction.getTransNum(),0);
        List<ResourceName> releaseList = new ArrayList<>();
        //Iterate through the locks held by transaction and check if it's the current context's child
        //If so, add the resource name and recursive call on child
        for (Lock l : this.lockman.getLocks(transaction)){
            if (l.name.isDescendantOf(this.name) && l.name.parent().equals(this.name)){
                releaseList.add(l.name);
                releaseList.addAll(this.childContext(l.name.getCurrentName().getSecond()).clearDescendantsLocks(transaction));
            }
        }
//        for (int i = 0; i <capacity(); i++){
//            LockContext child = this.childContext((long) i);
//
//            if (!child.lockman.getLockType(transaction,child.getResourceName()).equals(LockType.NL)){
//                releaseList.add(child.getResourceName());
//            }
//            releaseList.addAll(child.clearDescendantsLocks(transaction));
//
//        }

        return releaseList;
    }




    /**
     * Gets the type of lock that the transaction has at this level, either implicitly
     * (e.g. explicit S lock at higher level implies S lock at this level) or explicitly.
     * Returns NL if there is no explicit nor implicit lock.
     */
    public LockType getEffectiveLockType(TransactionContext transaction) {
        if (transaction == null) {
            return LockType.NL;
        }
        // TODO(proj4_part2): implement

        //If IX, check if hasSIXAncestor(), if so, return SIX. Else, return IX
        if (this.getExplicitLockType(transaction).equals(LockType.IX)){
            if (this.hasSIXAncestor(transaction)){
                return LockType.SIX;
            }else{
                return LockType.IX;
            }
        }
        //if there is explicit and not IX, return it.
        if (!this.getExplicitLockType(transaction).equals(LockType.NL)){
            return this.getExplicitLockType(transaction);
        }

        //if no explicit, traverse up, if see S or six, return S, if see x, return x
        LockContext parent = this.parentContext();
        while (parent != null){
            if (parent.lockman.getLockType(transaction,parent.getResourceName()).equals(LockType.S) ||
                parent.lockman.getLockType(transaction,parent.getResourceName()).equals(LockType.SIX)){
                return LockType.S;
            }else if (parent.lockman.getLockType(transaction,parent.getResourceName()).equals(LockType.X)){
                return LockType.X;
            }else{
                parent = parent.parentContext();
            }
        }
        return LockType.NL;
    }

    /**
     * Helper method to see if the transaction holds a SIX lock at an ancestor of this context
     * @param transaction the transaction
     * @return true if holds a SIX at an ancestor, false if not
     */
    private boolean hasSIXAncestor(TransactionContext transaction) {
        // TODO(proj4_part2): implement
        if (this.parentContext() == null){
            return false;
        }else {
            return this.parentContext().lockman.getLockType(transaction,this.parentContext().getResourceName()).equals(LockType.SIX) ||
                this.parentContext().hasSIXAncestor(transaction);
        }
    }

    /**
     * Helper method to get a list of resourceNames of all locks that are S or IS and are descendants of current context
     * for the given transaction.
     * @param transaction the given transaction
     * @return a list of ResourceNames of descendants which the transaction holds a S or IS lock.
     */
    private List<ResourceName> sisDescendants(TransactionContext transaction) {
        // TODO(proj4_part2): implement
        List<ResourceName> result = new ArrayList<>();
        for (int i = 0; i<this.capacity();i++){
            LockContext child = this.childContext((long)i);
            if (child.lockman.getLockType(transaction,child.getResourceName()).equals(LockType.S) ||
                child.lockman.getLockType(transaction,child.getResourceName()).equals(LockType.IS)){
                result.add(child.getResourceName());
            }
            result.addAll(child.sisDescendants(transaction));
        }
        return result;
    }

    /**
     * Get the type of lock that TRANSACTION holds at this level, or NL if no lock is held at this level.
     */
    public LockType getExplicitLockType(TransactionContext transaction) {
        if (transaction == null) {
            return LockType.NL;
        }
        // TODO(proj4_part2): implement

        //Use getLockType() in LockManager
        return this.lockman.getLockType(transaction,getResourceName());
    }

    /**
     * Disables locking descendants. This causes all new child contexts of this context
     * to be readonly. This is used for indices and temporary tables (where
     * we disallow finer-grain locks), the former due to complexity locking
     * B+ trees, and the latter due to the fact that temporary tables are only
     * accessible to one transaction, so finer-grain locks make no sense.
     */
    public void disableChildLocks() {
        this.childLocksDisabled = true;
    }

    /**
     * Gets the parent context.
     */
    public LockContext parentContext() {
        return parent;
    }

    /**
     * Gets the context for the child with name NAME (with a readable version READABLE).
     */
    public synchronized LockContext childContext(String readable, long name) {
        LockContext temp = new LockContext(lockman, this, new Pair<>(readable, name),
                                           this.childLocksDisabled || this.readonly);
        LockContext child = this.children.putIfAbsent(name, temp);
        if (child == null) {
            child = temp;
        }
        if (child.name.getCurrentName().getFirst() == null && readable != null) {
            child.name = new ResourceName(this.name, new Pair<>(readable, name));
        }
        return child;
    }

    /**
     * Gets the context for the child with name NAME.
     */
    public synchronized LockContext childContext(long name) {
        return childContext(Long.toString(name), name);
    }

    /**
     * Sets the capacity (number of children).
     */
    public synchronized void capacity(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Gets the capacity. Defaults to number of child contexts if never explicitly set.
     */
    public synchronized int capacity() {
        return this.capacity < 0 ? this.children.size() : this.capacity;
    }

    /**
     * Gets the saturation (number of locks held on children / number of children) for
     * a single transaction. Saturation is 0 if number of children is 0.
     */
    public double saturation(TransactionContext transaction) {
        if (transaction == null || capacity() == 0) {
            return 0.0;
        }
        return ((double) numChildLocks.getOrDefault(transaction.getTransNum(), 0)) / capacity();
    }

    @Override
    public String toString() {
        return "LockContext(" + name.toString() + ")";
    }
}

