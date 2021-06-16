package simpledb;

import java.util.*;
import java.util.concurrent.*;// process safe hashmap

public class LockManager {
    private ConcurrentHashMap<PageId, Object> pageId2Locks;// PageID2lock
    
	//if shared lock to page, use pageId2SharedLockDeque, or use pageId2TransId
	private ConcurrentHashMap<PageId, TransactionId> pageId2TransId;//pageID2transID
    private ConcurrentHashMap<PageId, ConcurrentLinkedDeque<TransactionId>> pageId2SharedLockDeque;//pageID2sharedLockDeque
    
	private ConcurrentHashMap<TransactionId, ConcurrentLinkedDeque<PageId>> transId2HoldLocks;//transID2HoldLocks
    private ConcurrentHashMap<TransactionId, ConcurrentLinkedDeque<PageId>> transId2HoldXLocks;//transID2XLocks
	
    private ConcurrentHashMap<TransactionId, ConcurrentLinkedDeque<TransactionId>> transId2DependTransIdDeque;//transID2depend

    private LockManager() {
        pageId2Locks= new ConcurrentHashMap<>();
        pageId2SharedLockDeque= new ConcurrentHashMap<>();
        pageId2TransId= new ConcurrentHashMap<>();
        transId2HoldLocks= new ConcurrentHashMap<>();
        transId2HoldXLocks= new ConcurrentHashMap<>();
        transId2DependTransIdDeque= new ConcurrentHashMap<>();
    }

	
    public static LockManager GetLockManager() {
        return new LockManager();
    }

	
    private boolean hasLock(TransactionId tid, PageId pid, boolean isReadOnly) {
        
		if(isReadOnly  && pageId2SharedLockDeque.containsKey(pid) && pageId2SharedLockDeque.get(pid).contains(tid))
			return true;
		if(!isReadOnly && pageId2TransId.containsKey(pid) && tid.equals(pageId2TransId.get(pid)))
			return true;
		
		return false;
    }

    
    public boolean acquireLock(TransactionId tid, PageId pid, Permissions perm) throws TransactionAbortedException {
        if (perm == Permissions.READ_ONLY) {
            if (hasLock(tid, pid, true)) return true;
            acquireSLock(tid, pid);
        } else if (perm == Permissions.READ_WRITE) {
            if (hasLock(tid, pid, false)) return true;
            acquireXLock(tid, pid);
        }
        updateTransactionLocks(tid, pid);
        return true;
    }
	
	
	private Object getLock(PageId pid) {
        pageId2Locks.putIfAbsent(pid, new Object());
        return pageId2Locks.get(pid);
    }
	
	
    private void acquireSLock(TransactionId tid, PageId pid) throws TransactionAbortedException {
        Object lock = getLock(pid);

        while (true) {
            synchronized(lock) {
                TransactionId holder=pageId2TransId.get(pid);

                if (holder==null || holder.equals(tid) ) { //只要没有人独占或独占的是自己就可以使用
                    removeDependency(tid);
                    addSTransaction(pid, tid);
                    return;
                }
				
                ArrayList<TransactionId> holders = new ArrayList<>();
                holders.add(holder);
                updateDependency(tid, holders);
            }
        }
    }

    private void acquireXLock(TransactionId tid, PageId pid) throws TransactionAbortedException {
        Object lock = getLock(pid);

        while (true) {
            synchronized (lock) {
                ArrayList<TransactionId> holders = new ArrayList<>();
                
				if (pageId2TransId.containsKey(pid)) 
                    holders.add(pageId2TransId.get(pid));
				
                if (pageId2SharedLockDeque.containsKey(pid))
                    holders.addAll(pageId2SharedLockDeque.get(pid));

                if (holders.size() == 0 || (holders.size() == 1 && holders.get(0).equals(tid))) {
                    removeDependency(tid);
                    addXTransaction(pid, tid);
                    return;
                }
                updateDependency(tid, holders);
            }
        }
    }

    private void addSTransaction(PageId pid, TransactionId tid) {
        pageId2SharedLockDeque.putIfAbsent(pid, new ConcurrentLinkedDeque<>());
        pageId2SharedLockDeque.get(pid).add(tid);
    }


    private void addXTransaction(PageId pid, TransactionId tid) {
        pageId2TransId.put(pid, tid);
        transId2HoldXLocks.putIfAbsent(tid, new ConcurrentLinkedDeque<>());
        transId2HoldXLocks.get(tid).add(pid);
    }


    private void removeDependency(TransactionId tid) {
        synchronized (transId2DependTransIdDeque) {
            transId2DependTransIdDeque.remove(tid);
            for (TransactionId curtid : transId2DependTransIdDeque.keySet()) 
                transId2DependTransIdDeque.get(curtid).remove(tid);
            //if start release, we can release all lock directly.
        }
    }

    private void updateDependency(TransactionId acquirer, ArrayList<TransactionId> holders)
            throws TransactionAbortedException {
        transId2DependTransIdDeque.putIfAbsent(acquirer, new ConcurrentLinkedDeque<>());
        boolean Flag = false;
        ConcurrentLinkedDeque<TransactionId> childs = transId2DependTransIdDeque.get(acquirer);
        for (TransactionId holder : holders) {
            if (!childs.contains(holder) && !holder.equals(acquirer)) {
                Flag = true;
                transId2DependTransIdDeque.get(acquirer).add(holder);
            }
        }
        if (Flag) checkDeadLock(acquirer, new HashSet<>());
    }


    private void checkDeadLock(TransactionId now, HashSet<TransactionId> visited) throws TransactionAbortedException {
		
        if (!transId2DependTransIdDeque.containsKey(now)) return;
        for (TransactionId child : transId2DependTransIdDeque.get(now)) {
            if (visited.contains(child))
                throw new TransactionAbortedException();
            visited.add(child);
            checkDeadLock(child, visited);
            visited.remove(child);
        }
    }

    private void updateTransactionLocks(TransactionId tid, PageId pid) {
        transId2HoldLocks.putIfAbsent(tid, new ConcurrentLinkedDeque<>());
        transId2HoldLocks.get(tid).add(pid);
    }


    public void releasePage(TransactionId tid, PageId pid) {
        if (holdsLock(tid, pid)) {
            Object lock = getLock(pid);
            synchronized (lock) {
                if (pageId2SharedLockDeque.containsKey(pid)) 
                    pageId2SharedLockDeque.get(pid).remove(tid);
                if (pageId2TransId.containsKey(pid) && pageId2TransId.get(pid).equals(tid)) 
                    pageId2TransId.remove(pid);
                if (transId2HoldLocks.containsKey(tid)) 
                    transId2HoldLocks.get(tid).remove(pid);
                if (transId2HoldXLocks.containsKey(tid)) 
                    transId2HoldXLocks.get(tid).remove(pid);
            }
        }
    }

    public void releasePages(TransactionId tid) {
        if (transId2HoldLocks.containsKey(tid)) {
            for (PageId pid : transId2HoldLocks.get(tid))
                releasePage(tid, pid);
        }
        transId2HoldXLocks.remove(tid);
    }

    public boolean holdsLock(TransactionId tid, PageId pid) {
        return hasLock(tid, pid, true) || hasLock(tid, pid, false);
    }

    public ConcurrentHashMap<TransactionId, ConcurrentLinkedDeque<PageId>> getTransactionDirtiedPages() {
        return transId2HoldXLocks;
    }
}

