# Lab1 报告

## Exercise 1

主要任务：完成Tuple.java和TupleDesc.java。

```
TupleDesc.java描述一个Tuple的组织结构。
TDIterm：记录单个field的fieldType和fieldName。
TDitem[] tupleDesc  TDiterm序列。
IDItermIterator实现TDIterm的迭代器。
TupleDesc(Type[] typeAr, String[] fieldAr) 直接填充即可
TupleDesc(Type[] typeAr) 填充名称为空，再引用上述构造方法
... 
未实现hashCode()

```

```
Tuple.java描述一个元组。

TupleDesc td 描述元组结构。
Field[] fvalues 记录每个域对应的值。
RecordId rid 记录RecordID
FieldIterator 实现fvalues的迭代器。
...
未实现toString()

```

## Exercise 2

主要任务：实现catalog.java。

```
Catalog.java主要记录数据库内不同的表和它们的关系。

* class Table:记录一张表，属性有file,name,priKeyField。
id2table,name2id记录id转化table，name转化id的两张映射表。
...

```

## Exercise 3

主要任务：实现BufferPool.java。

```
BufferPool.java主要管理从硬盘到内存的读写（类似cache）。
int numPages: 当前内存池内页的数量。
Map<PageId,Page> pid2page:记录pid 到page的映射。
Page getPage(TransactionId tid, PageId pid, Permissions perm)  获取内存池内的页。


```

## Exercise 4

实现HeapPageId.java，RecordID.java，HeapPage.java。

```
HeapPageID.java
tableID,pgNo分别记录对应的表号和堆页号。
...

HeapPage.java
getNumTuples(),getHeaderSize() 直接使用上方公式
getNumEmptySlots()获得空洞数量
TupleIterator() 该页的Tuple迭代器

RecordID.java记录特定页的特定tuple。
pid,tupleno分别记录页号和tuple号。
int hashCode()当前recordID的hashcode
...
```

## Exercise 5

实现Heapfile.java。

```
Heapfile是dbfile的一种实现。
File f 硬盘上的文件
TupleDesc td tuple格式
int numPages 页数量
int getId() 直接使用f.getAbsoluteFile().hashCode()
Page readPage(PageId pid)  直接使用pid到f文件中找到对应位置读到page中去。
HeapFileIterator 使用dbfile迭代器实现对tuple的访问。
```

## Exercise 6

实现SeqScan.java

```
SeqScan.java是读一张表里的tuple实现。

TransactionId tid; 			tid记录操作的ID
int tableid;				tableID
String tableAlias;			table别名
DbFile dbFile;				表对应的dbfile
DbFileIterator dbIterator;  对应的dbfileiterator

TupleDesc getTupleDesc() 获得TD，但需要对名字加前缀。
无其他注意点！
```

