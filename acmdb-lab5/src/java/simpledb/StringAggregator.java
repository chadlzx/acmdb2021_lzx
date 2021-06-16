package simpledb;
import java.util.*;

/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;
    
	private final int gbfield;
    private final Type gbfieldtype;
    private final int afield;
    private final Op what;
    
    private TreeMap<Object, Integer > aggr;
    /**
     * Aggregate constructor
     * @param gbfield the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield the 0-based index of the aggregate field in the tuple
     * @param what aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */
	
    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        if (what != Op.COUNT) 
            throw new IllegalArgumentException("StringAggregator only support COUNT!");
        
        this.what = what;
        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;
        this.aggr=null;
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here
        if(aggr==null){
        	aggr=new TreeMap<>();
        	if (this.gbfield==Aggregator.NO_GROUPING) 
        	    aggr.put(0, 0);
        }
        
        if (gbfield == Aggregator.NO_GROUPING) {
            aggr.put(0, aggr.get(0)+1);
        } else {
            Object gbKey = gbfieldtype == Type.INT_TYPE ?((IntField) tup.getField(gbfield)).getValue() :((StringField) tup.getField(gbfield)).getValue();
            if(aggr.containsKey(gbKey)) 
                  aggr.put(gbKey, aggr.get(gbKey)+1);
            else  aggr.put(gbKey, 1);
        }
    }

    /**
     * Create a DbIterator over group aggregate results.
     *
     * @return a DbIterator whose tuples are the pair (groupVal,
     *   aggregateVal) if using group, or a single (aggregateVal) if no
     *   grouping. The aggregateVal is determined by the type of
     *   aggregate specified in the constructor.
     */
    public DbIterator iterator() {
        // some code goes here
        return new AggrDbIterator();
    }
    private class AggrDbIterator implements DbIterator {
    	
        private ArrayList<Tuple> tupleResult;
        private Iterator<Tuple> it;
        
        public AggrDbIterator() {
            tupleResult=new ArrayList<Tuple>();
            if (gbfield==Aggregator.NO_GROUPING) {
                Tuple t=new Tuple(getTupleDesc());
                t.setField(0, new IntField(aggr.get(0)));
                tupleResult.add(t);
            } else {
                for (Map.Entry<Object, Integer > e : aggr.entrySet()) {
                    Tuple t=new Tuple(getTupleDesc());
                    Field groupVal = (gbfieldtype == Type.INT_TYPE) ?new IntField((int) e.getKey()) :new StringField((String) e.getKey(), ((String) e.getKey()).length());
                    t.setField(0, groupVal);
                    t.setField(1, new IntField(e.getValue()));
                    tupleResult.add(t);
                }
            }
        }

        @Override
        public void open() throws DbException, TransactionAbortedException {
        	
            it=tupleResult.iterator();
        }
        @Override
        public boolean hasNext() throws DbException, TransactionAbortedException {
            if (it == null)
                throw new IllegalStateException("IntegerAggregator not open");
            return it.hasNext();
        }

        
        @Override
        public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
            if (it == null)
                throw new IllegalStateException("IntegerAggregator not open");
            return it.next();
        }

        
        @Override
        public void rewind() throws DbException, TransactionAbortedException {
            if (it == null)
                throw new IllegalStateException("IntegerAggregator not open");
            it = tupleResult.iterator();
        }

        @Override
        public TupleDesc getTupleDesc() {
            if (gbfield == Aggregator.NO_GROUPING)return new TupleDesc(new Type[]{Type.INT_TYPE});
            else return new TupleDesc(new Type[]{gbfieldtype, Type.INT_TYPE});
        }
        
        @Override
        public void close() {
            it = null;
        }
    }
    
    

}
