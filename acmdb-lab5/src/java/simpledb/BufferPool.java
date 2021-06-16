package simpledb;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 * 
 * @Threadsafe, all fields are final
 */
public class BufferPool {
    /** Bytes per page, including header. */
    private static final int PAGE_SIZE = 4096;

    private static int pageSize = PAGE_SIZE;
    
    /** Default number of pages passed to the constructor. This is used by
    other classes. BufferPool should use the numPages argument to the
    constructor instead. */
    public static final int DEFAULT_PAGES = 50;

	private LockManager lockManager;


	private int numPages;
    private Map<PageId,Page> pid2page;
    
    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        this.numPages = numPages;
        pid2page = new HashMap<>();
        lockManager=LockManager.GetLockManager();
    }
    
    public static int getPageSize() {
      return pageSize;
    }
    
    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void setPageSize(int pageSize) {
    	BufferPool.pageSize = pageSize;
    }
    
    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void resetPageSize() {
    	BufferPool.pageSize = PAGE_SIZE;
    }

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, an page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid the ID of the transaction requesting the page
     * @param pid the ID of the requested page
     * @param perm the requested permissions on the page
     */
    public  Page getPage(TransactionId tid, PageId pid, Permissions perm)
        throws TransactionAbortedException, DbException {
        
        
        lockManager.acquireLock(tid, pid, perm);
        
        if(pid2page.containsKey(pid)) {
            return pid2page.get(pid);
        }
        else {
            if(pid2page.size()>=numPages)
                evictPage();
            DbFile dbFile=Database.getCatalog().getDatabaseFile(pid.getTableId());
            Page page=dbFile.readPage(pid);
            pid2page.put(pid, page);
            return page;
        }
    }

    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public  void releasePage(TransactionId tid, PageId pid) {
        // some code goes here
        // not necessary for lab1|lab2
        lockManager.releasePage(tid, pid);
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public void transactionComplete(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
        transactionComplete(tid, true);
    }

    /** Return true if the specified transaction has a lock on the specified page */
    public boolean holdsLock(TransactionId tid, PageId p) {
        // some code goes here
        // not necessary for lab1|lab2
        return lockManager.holdsLock(tid, p);
        
    }

    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */
    public void transactionComplete(TransactionId tid, boolean commit)
        throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
        ConcurrentHashMap<TransactionId, ConcurrentLinkedDeque<PageId>> tid2dirtypageIds=lockManager.getTransactionDirtiedPages();
		
		
        if (tid2dirtypageIds.containsKey(tid)) {
        	
            for (PageId pid : tid2dirtypageIds.get(tid)) {
                if (commit) {
                    flushPage(pid);
                    try { this.getPage(tid, pid, Permissions.READ_WRITE);} 	
                    catch (Exception e){}
                } 
                
                else {
                    int tableId = pid.getTableId();
                    Page page = getDbFileFromDisc(tableId).readPage(pid);
                    pid2page.replace(pid, page);
                    page.markDirty(false, null);
                }
            }
        }
        lockManager.releasePages(tid);
    }
	private DbFile getDbFileFromDisc(int tableId) {
        return Database.getCatalog().getDatabaseFile(tableId);
    }
    
    
    
    /**
     * Add a tuple to the specified table on behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to and any other 
     * pages that are updated (Lock acquisition is not needed for lab2). 
     * May block if the lock(s) cannot be acquired.
     * 
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have 
     * been dirtied to the cache (replacing any existing versions of those pages) so 
     * that future requests see up-to-date pages. 
     *
     * @param tid the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t the tuple to add
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
        throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
        DbFile tableFile=Database.getCatalog().getDatabaseFile(tableId);
        ArrayList<Page> dirtyPages=tableFile.insertTuple(tid,t);
        
        for(Page dirtyPage : dirtyPages) {
            dirtyPage.markDirty(true, tid);
            pid2page.remove(dirtyPage.getId());
            
            if (pid2page.size() >= numPages) {
		        evictPage();
		    }
		    pid2page.put(dirtyPage.getId(), dirtyPage);
        }
        
    }

    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is removed from and any
     * other pages that are updated. May block if the lock(s) cannot be acquired.
     *
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have 
     * been dirtied to the cache (replacing any existing versions of those pages) so 
     * that future requests see up-to-date pages. 
     *
     * @param tid the transaction deleting the tuple.
     * @param t the tuple to delete
     */
    public  void deleteTuple(TransactionId tid, Tuple t)
        throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
        int tableId=t.getRecordId().getPageId().getTableId();
        DbFile tableFile=Database.getCatalog().getDatabaseFile(tableId);
        ArrayList<Page> dirtyPages=tableFile.deleteTuple(tid, t);
        for(Page dirtyPage : dirtyPages) {
            dirtyPage.markDirty(true, tid);
            pid2page.remove(dirtyPage.getId());
            if (pid2page.size() >= numPages) {
		        evictPage();
		    }
            pid2page.put(dirtyPage.getId(), dirtyPage);
        }
    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     *     break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        // some code goes here
        // not necessary for lab1
        for(PageId pid : pid2page.keySet())
            flushPage(pid);

    }

    /** Remove the specific page id from the buffer pool.
        Needed by the recovery manager to ensure that the
        buffer pool doesn't keep a rolled back page in its
        cache.
        
        Also used by B+ tree files to ensure that deleted pages
        are removed from the cache so they can be reused safely
    */
    public synchronized void discardPage(PageId pid) {
        // some code goes here
        // not necessary for lab1
        pid2page.remove(pid);
    }

    /**
     * Flushes a certain page to disk
     * @param pid an ID indicating the page to flush
     */
    private synchronized  void flushPage(PageId pid) throws IOException {
        // some code goes here
        // not necessary for lab1
        if(!pid2page.containsKey(pid))return; 
        Page page = pid2page.get(pid);
        if(page.isDirty() == null)return;
        
        int tableId=page.getId().getTableId();
        DbFile dbFile=Database.getCatalog().getDatabaseFile(tableId);
        dbFile.writePage(page);
        page.markDirty(false, null);
    }

    /** Write all pages of the specified transaction to disk.
     */
    public synchronized  void flushPages(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
        for (PageId pid : pid2page.keySet()) {
            Page page = pid2page.get(pid);

            if (tid.equals(page.isDirty()))
                flushPage(pid);
        }
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     */
    private synchronized  void evictPage() throws DbException {
        // some code goes here
        // not necessary for lab1

        PageId evict_pid = null;
        for (PageId pid : pid2page.keySet()) {
            Page page = pid2page.get(pid);
            if (page != null && page.isDirty() == null) {
                evict_pid = pid;
                break;
            }
        }

        if (evict_pid == null)
            throw new DbException("All pages are dirty, No valid page to evict!");

        try {
            flushPage(evict_pid);
            discardPage(evict_pid);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
    

}