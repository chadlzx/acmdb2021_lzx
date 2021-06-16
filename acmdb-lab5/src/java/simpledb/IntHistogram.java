package simpledb;

/** A class to represent a fixed-width histogram over a single integer-based field.
 */
public class IntHistogram {




	private int min,max,buckets,ntups,width;
	private int histogram[];
	private int range;
    /**
     * Create a new IntHistogram.
     * 
     * This IntHistogram should maintain a histogram of integer values that it receives.
     * It should split the histogram into "buckets" buckets.
     * 
     * The values that are being histogrammed will be provided one-at-a-time through the "addValue()" function.
     * 
     * Your implementation should use space and have execution time that are both
     * constant with respect to the number of values being histogrammed.  For example, you shouldn't 
     * simply store every value that you see in a sorted list.
     * 
     * @param buckets The number of buckets to split the input value into.
     * @param min The minimum integer value that will ever be passed to this class for histogramming
     * @param max The maximum integer value that will ever be passed to this class for histogramming
     */
    public IntHistogram(int buckets, int min, int max) {
    	// some code goes here
    	this.min=min;
    	this.max=max;
    	this.buckets=buckets;
    	this.width=(int)Math.ceil(((double)(max-min+1.0))/buckets);
    	this.histogram=new int[buckets];
    	for(int i=0;i<buckets;i++)
    		this.histogram[i]=0;
    	this.ntups=0;
    }


	//get the buckets position
	private int indexOf(int x){
		return (x-min)/width;
	}
	
    /**
     * Add a value to the set of values that you are keeping a histogram of.
     * @param v Value to add to the histogram
     */
    public void addValue(int v) {
    	// some code goes here
    	histogram[indexOf(v)]+=1;
    	ntups+=1;
    }

    /**
     * Estimate the selectivity of a particular predicate and operand on this table.
     * 
     * For example, if "op" is "GREATER_THAN" and "v" is 5, 
     * return your estimate of the fraction of elements that are greater than 5.
     * 
     * @param op Operator
     * @param v Value
     * @return Predicted selectivity of this particular operator and value
     */
    public double estimateSelectivity(Predicate.Op op, int v) {

    	// some code goes here
    	int bucketIndex=indexOf(v),height=0;
    	int leftOfBucket=min+width*bucketIndex;
    	int rightOfBucket=leftOfBucket+width-1;
    	
    	switch (op) {
            case EQUALS:
                if(v<min||v>max)return 0.0;
                else {
                    height=histogram[bucketIndex];
                    return (height*1.0/width)/ntups;
                }
            case GREATER_THAN:
                if (v<min) 
                    return 1.0;
                if (v>max)
                    return 0.0;
                height=histogram[bucketIndex];
                double sum1=((rightOfBucket-v)*1.0)/width*height;
                double sum2=0;
                for (int i=bucketIndex+1;i<buckets;i++) 
                    sum2+=histogram[i];
                return (sum1+sum2)/ntups;
            case LESS_THAN:
                if(v<min)
                    return 0.0;
                if(v>max) 
                    return 1.0;
                height=histogram[bucketIndex];
                double sum3=((v-leftOfBucket)*1.0/width)*height;
                double sum4=0;
                for (int i=bucketIndex-1;i>=0;i--) 
                    sum4+= histogram[i];
                return (sum3+sum4)/ntups;
            case LESS_THAN_OR_EQ:
                return estimateSelectivity(Predicate.Op.LESS_THAN, v)+estimateSelectivity(Predicate.Op.EQUALS,v);
            case GREATER_THAN_OR_EQ:
                return estimateSelectivity(Predicate.Op.GREATER_THAN,v)+estimateSelectivity(Predicate.Op.EQUALS,v);
            case LIKE:
                return avgSelectivity();
            case NOT_EQUALS:
                return 1-estimateSelectivity(Predicate.Op.EQUALS,v);
            default:
                throw new RuntimeException("No this operation!");
        }
    	
    	
        
    }
    
    /**
     * @return
     *     the average selectivity of this histogram.
     *     
     *     This is not an indispensable method to implement the basic
     *     join optimization. It may be needed if you want to
     *     implement a more efficient optimization
     * */
    public double avgSelectivity()
    {
        // some code goes here
  		//As we all know, we have not to implement it! 
        return 1.0;
    }
    
    /**
     * @return A string describing this histogram, for debugging purposes
     */
    public String toString() {
        // some code goes here
        return null;
    }
}
