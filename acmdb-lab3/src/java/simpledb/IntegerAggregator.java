package simpledb;
import java.util.*;
import java.lang.reflect.Array;
/**
 * Knows how to compute some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;
	
	private final int gbfield;
    private final Type gbfieldtype;
    private final int afield;
    private final Op what;
    
    private final TreeMap<Object, ArrayList<Integer> > aggregator;//to aggregate
	
    /**
     * Aggregate constructor
     * 
     * @param gbfield
     *            the 0-based index of the group-by field in the tuple, or
     *            NO_GROUPING if there is no grouping
     * @param gbfieldtype
     *            the type of the group by field (e.g., Type.INT_TYPE), or null
     *            if there is no grouping
     * @param afield
     *            the 0-based index of the aggregate field in the tuple
     * @param what
     *            the aggregation operator
     */

    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;
        this.what = what;
        this.aggregator = new TreeMap<>();
        if(gbfield==NO_GROUPING) 
            aggregator.put(0, new ArrayList<>());
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor
     * 
     * @param tup
     *            the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here
        if (this.gbfield == Aggregator.NO_GROUPING) {
            aggregator.get(0).add(((IntField) tup.getField(afield)).getValue());
        } else {
            Object gbKey = (gbfieldtype == Type.INT_TYPE) ?((IntField) tup.getField(gbfield)).getValue():((StringField) tup.getField(gbfield)).getValue();
            Integer aggrVal=((IntField)tup.getField(afield)).getValue();
            if(!aggregator.containsKey(gbKey)) 
                aggregator.put(gbKey, new ArrayList<>());
            aggregator.get(gbKey).add(aggrVal);
        }
    }

    /**
     * Create a DbIterator over group aggregate results.
     * 
     * @return a DbIterator whose tuples are the pair (groupVal, aggregateVal)
     *         if using group, or a single (aggregateVal) if no grouping. The
     *         aggregateVal is determined by the type of aggregate specified in
     *         the constructor.
     */
    public DbIterator iterator() {
        // some code goes here
        return new AggrDbIterator();   
    }
    private class AggrDbIterator implements DbIterator {
    	
        private ArrayList<Tuple> tupleResult;
        private Iterator<Tuple> it;
		
        public int cal(ArrayList<Integer> l) {
            int ans=0;
            switch (what) {
                case MIN:
                    ans=l.get(0);
                    for (int v : l) {
                        if (ans>v) 
                            ans=v;
                    }
                    break;
                case MAX:
                    ans=l.get(0);
                    for (int v : l) {
                        if (ans < v) 
                            ans=v;
                    }
                    break;
                case SUM:
                    ans = 0;
                    for (int v : l) 
                        ans+=v;
                    break;
                case AVG:
                    ans = 0;
                    for (int v : l) 
                        ans += v;
                    ans = ans/l.size();
                    break;
                case COUNT:
                    ans = l.size();
                    break;
            }
            return ans;
        }

        public AggrDbIterator() {
            tupleResult=new ArrayList<Tuple>();
            if (gbfield==Aggregator.NO_GROUPING) {
                Tuple t=new Tuple(getTupleDesc());
                t.setField(0, new IntField(this.cal(aggregator.get(0))));
                tupleResult.add(t);
            } else {
                for (Map.Entry<Object, ArrayList<Integer> > e : aggregator.entrySet()) {
                    Tuple t=new Tuple(getTupleDesc());
                    Field groupVal = (gbfieldtype == Type.INT_TYPE) ?new IntField((int) e.getKey()) :new StringField((String) e.getKey(), ((String) e.getKey()).length());
                    t.setField(0, groupVal);
                    t.setField(1, new IntField(this.cal(e.getValue())));
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
