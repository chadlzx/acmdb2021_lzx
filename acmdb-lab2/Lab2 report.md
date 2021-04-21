# Lab2 report

## EX1

```
填写BufferPool.java中的Lab2内容。
public void insertTuple(TransactionId tid, int tableId, Tuple t)
        throws DbException, IOException, TransactionAbortedException
向某个表中插入一个tuple
直接获得该表的ID对应的dbFile，然后直接插入后，注意标注脏页。

void deleteTuple(TransactionId tid, Tuple t)
        throws DbException, IOException, TransactionAbortedException

删除某个tuple
由于没有给出tuple对应的表，因此需要先查询tuple自带的信息获得表。
接下来的步骤和insertTuple一致。

void flushPage(PageId pid) throws IOException
刷新某个特定的Page
满足条件直接写入disk即可

void evictPage() throws DbException
策略是直接删除bufferPool中的第一页，删除之前flush一下。

```

## EX2

```
填写BTreeFile.fileLeafPage() in BtreeFile.java
BTreeLeafPage findLeafPage(TransactionId tid, HashMap<PageId, Page> dirtypages, BTreePageId pid, Permissions perm,
			Field f) 
					throws DbException, TransactionAbortedException
使用B+树的常规搜索方法即可。


```

## EX3

```
BTreeInternalPage splitInternalPage(TransactionId tid, HashMap<PageId, Page> dirtypages, 
			BTreeInternalPage page, Field field) 
					throws DbException, IOException, TransactionAbortedException 
分离内部节点。
使用B+树常规分离方式即可。
注意更新各种pointers。
BTreeLeafPage splitLeafPage(TransactionId tid, HashMap<PageId, Page> dirtypages, BTreeLeafPage page, Field field) 
			throws DbException, IOException, TransactionAbortedException 
同上，但不需要更新BTreeEntry了。
```



## EX4

```
void stealFromLeafPage(BTreeLeafPage page, BTreeLeafPage sibling,
			BTreeInternalPage parent, BTreeEntry entry, boolean isRightSibling) throws DbException 
从左方的叶子节点匀一些Tuple过来。
相对简单，pointer方面只需要更新父亲节点的Entry的Child即可。

void stealFromLeftInternalPage(TransactionId tid, HashMap<PageId, Page> dirtypages, 
			BTreeInternalPage page, BTreeInternalPage leftSibling, BTreeInternalPage parent,
			BTreeEntry parentEntry) throws DbException, IOException, TransactionAbortedException 
void stealFromRightInternalPage(TransactionId tid, HashMap<PageId, Page> dirtypages, 
			BTreeInternalPage page, BTreeInternalPage rightSibling, BTreeInternalPage parent,
			BTreeEntry parentEntry) throws DbException, IOException, TransactionAbortedException 
和匀叶子结点相比麻烦一些。
涉及到自身entry的移动和entry的parent修改。


```



## EX5

```
mergeLeafPages(TransactionId tid, HashMap<PageId, Page> dirtypages, 
			BTreeLeafPage leftPage, BTreeLeafPage rightPage, BTreeInternalPage parent, BTreeEntry parentEntry) 
					throws DbException, IOException, TransactionAbortedException
正常的LeafPage的合成。

mergeInternalPages(TransactionId tid, HashMap<PageId, Page> dirtypages, 
			BTreeInternalPage leftPage, BTreeInternalPage rightPage, BTreeInternalPage parent, BTreeEntry parentEntry) 
					throws DbException, IOException, TransactionAbortedException 
正常的内部节点的合成。

```

