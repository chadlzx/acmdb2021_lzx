package simpledb;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TableStats represents statistics (e.g., histograms) about base tables in a
 * query. 
 * 
 * This class is not needed in implementing lab1, lab2 and lab3.
 */
public class TableStats {

    private static final ConcurrentHashMap<String, TableStats> statsMap = new ConcurrentHashMap<String, TableStats>();

    static final int IOCOSTPERPAGE = 1000;

    public static TableStats getTableStats(String tablename) {
        return statsMap.get(tablename);
    }

    public static void setTableStats(String tablename, TableStats stats) {
        statsMap.put(tablename, stats);
    }
    
    public static void setStatsMap(HashMap<String,TableStats> s)
    {
        try {
            java.lang.reflect.Field statsMapF = TableStats.class.getDeclaredField("statsMap");
            statsMapF.setAccessible(true);
            statsMapF.set(null, s);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    public static Map<String, TableStats> getStatsMap() {
        return statsMap;
    }

    public static void computeStatistics() {
        Iterator<Integer> tableIt = Database.getCatalog().tableIdIterator();

        System.out.println("Computing table stats.");
        while (tableIt.hasNext()) {
            int tableid = tableIt.next();
            TableStats s = new TableStats(tableid, IOCOSTPERPAGE);
            setTableStats(Database.getCatalog().getTableName(tableid), s);
        }
        System.out.println("Done.");
    }

    /**
     * Number of bins for the histogram. Feel free to increase this value over
     * 100, though our tests assume that you have at least 100 bins in your
     * histograms.
     */
    static final int NUM_HIST_BINS = 100;
	
	
	private HeapFile table;
	private TupleDesc td;
	
    private int ntups;
    private int ioCostPerPage;
	
	private HashMap<String,Integer> name2min,name2max;
	private HashMap<String, Object> name2hist;//map the fieldName to hist
	
	
	
    /**
     * Create a new TableStats object, that keeps track of statistics on each
     * column of a table
     * 
     * @param tableid
     *            The table over which to compute statistics
     * @param ioCostPerPage
     *            The cost per page of IO. This doesn't differentiate between
     *            sequential-scan IO and disk seeks.
     */
    public TableStats(int tableid, int ioCostPerPage) {
        // For this function, you'll have to get the
        // DbFile for the table in question,
        // then scan through its tuples and calculate
        // the values that you need.
        // You should try to do this reasonably efficiently, but you don't
        // necessarily have to (for example) do everything
        // in a single scan of the table.
        // some code goes here
        this.ioCostPerPage = ioCostPerPage;
        table=(HeapFile) Database.getCatalog().getDatabaseFile(tableid);
        ntups=0;
        name2min=new HashMap<>();
        name2max=new HashMap<>();
        name2hist=new HashMap<>();
        td=table.getTupleDesc();
        
        Transaction t = new Transaction();//get a new transaction to get DbFileIterator
        DbFileIterator iter = table.iterator(t.getId());
        
        try{
        iter.open();
        while (iter.hasNext()) {//get int-type Filed max&min value
            ntups++;
            Tuple tuple=iter.next();
            for (int i=0;i<td.numFields();i++) 
                if ( td.getFieldType(i) == Type.INT_TYPE) {
                    String name=td.getFieldName(i);
                    Integer value=((IntField)tuple.getField(i)).getValue();
                    if (name2min.containsKey(name)) {
                        int minn=name2min.get(name),maxx=name2max.get(name);
                        if (value<minn)	name2min.put(name, value);
                        if (value>maxx) name2max.put(name, value);
                    } else {
                        name2min.put(name, value);
                    	name2max.put(name, value);
                    }
                }
        }
        for (Map.Entry<String,Integer> entry : name2min.entrySet()) {
            int minn=entry.getValue();
            int maxx=name2max.get(entry.getKey());
            IntHistogram histogram = new IntHistogram(NUM_HIST_BINS, minn, maxx);
            name2hist.put(entry.getKey(), histogram);
        }
        iter.rewind();

        while (iter.hasNext()) {
            Tuple tuple = iter.next();
            for (int i=0; i<td.numFields(); i++) {
                String fieldName = td.getFieldName(i);
                if (td.getFieldType(i)==Type.INT_TYPE){
                    int value = ((IntField) tuple.getField(i)).getValue();
                    IntHistogram intHistogram=(IntHistogram) name2hist.get(fieldName);
                    intHistogram.addValue(value);
                    name2hist.put(fieldName,intHistogram);
                } else {
                    String value = ((StringField) tuple.getField(i)).getValue();
                    if (name2hist.containsKey(fieldName)) {
                        StringHistogram stringHistogram = (StringHistogram) name2hist.get(fieldName);
                        stringHistogram.addValue(value);
                        name2hist.put(fieldName, stringHistogram);
                    } else {
                        StringHistogram stringHistogram=new StringHistogram(NUM_HIST_BINS);
                        stringHistogram.addValue(value);
                        name2hist.put(fieldName, stringHistogram);
                    }
                }
            }
        }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Estimates the cost of sequentially scanning the file, given that the cost
     * to read a page is costPerPageIO. You can assume that there are no seeks
     * and that no pages are in the buffer pool.
     * 
     * Also, assume that your hard drive can only read entire pages at once, so
     * if the last page of the table only has one tuple on it, it's just as
     * expensive to read as a full page. (Most real hard drives can't
     * efficiently address regions smaller than a page at a time.)
     * 
     * @return The estimated cost of scanning the table.
     */
    public double estimateScanCost() {
        // some code goes here
        return table.numPages()*ioCostPerPage;
    }

    /**
     * This method returns the number of tuples in the relation, given that a
     * predicate with selectivity selectivityFactor is applied.
     * 
     * @param selectivityFactor
     *            The selectivity of any predicates over the table
     * @return The estimated cardinality of the scan with the specified
     *         selectivityFactor
     */
    public int estimateTableCardinality(double selectivityFactor) {
        // some code goes here
        return (int)Math.ceil(totalTuples()*selectivityFactor);
    }

    /**
     * The average selectivity of the field under op.
     * @param field
     *        the index of the field
     * @param op
     *        the operator in the predicate
     * The semantic of the method is that, given the table, and then given a
     * tuple, of which we do not know the value of the field, return the
     * expected selectivity. You may estimate this value from the histograms.
     * */
    public double avgSelectivity(int field, Predicate.Op op) {
        // some code goes here
        return 1.0;
    }

    /**
     * Estimate the selectivity of predicate <tt>field op constant</tt> on the
     * table.
     * 
     * @param field
     *            The field over which the predicate ranges
     * @param op
     *            The logical operation in the predicate
     * @param constant
     *            The value against which the field is compared
     * @return The estimated selectivity (fraction of tuples that satisfy) the
     *         predicate
     */
    public double estimateSelectivity(int field, Predicate.Op op, Field constant) {
        // some code goes here
        String fieldName = td.getFieldName(field);
        if (constant.getType()==Type.INT_TYPE) {
            int value = ((IntField)constant).getValue();
            IntHistogram histogram = (IntHistogram) name2hist.get(fieldName);
            return histogram.estimateSelectivity(op, value);
        } else {
            String value = ((StringField)constant).getValue();
            StringHistogram histogram = (StringHistogram)name2hist.get(fieldName);
            return histogram.estimateSelectivity(op, value);
        }
    }

    /**
     * return the total number of tuples in this table
     * */
    public int totalTuples() {
        // some code goes here
        return ntups;
    }

}
