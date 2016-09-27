package com.zerogc.test;

import java.util.Random;
import java.util.TreeMap;

import com.zerogc.collections.LongAvlTree;
import com.zerogc.collections.LongLongTreeMap;
import com.zerogc.collections.LongRbTree;
import com.zerogc.util.Level;
import com.zerogc.util.Logger;

/**
 * @author Benoit Jardin
 */

// java -Djava.library.path=${DEVELOP}/zerogc/native -cp ${DEVELOP}/zerogc/dist/ZeroGC-0.0.0.0.jar com.zerogc.test.MapPerf
public class MapPerf {
    static final Logger log = new Logger(MapPerf.class.getSimpleName());
    static final int SIZE = 10000;
    
    private static int count = 0;
	private static long sum = 0;

    // Multimap between timer expiry and EventHandler
    static class LongLongRbMap extends LongRbTree {
    	private long[] value;

        @Override
        protected void grow(int capacity, int newCapacity) {
            super.grow(capacity, newCapacity);

            long[] newValue = new long[newCapacity];
            if (capacity > 0) {
                System.arraycopy(this.value, 0, newValue, 0, capacity);
            }
            this.value = newValue;
        }
        
        public final long getValue(int entry) {
            return this.value[entry];
        }

        public final long get(long key) {
            return this.value[find(key)];
        }
        
        public final int insert(long key, long value) {
        	int entry = super.insert(key);
        	this.value[entry] = value; 
        	return entry;
        }
        
        public final void remove(long key) {
            remove(find(key));
        }
    }

    static class LongLongAvlMap extends LongAvlTree {
        private long[] value;

        @Override
        protected void grow(int capacity, int newCapacity) {
            super.grow(capacity, newCapacity);

            long[] newValue = new long[newCapacity];
            if (capacity > 0) {
                System.arraycopy(this.value, 0, newValue, 0, capacity);
            }
            this.value = newValue;
        }
        
        public final long getValue(int entry) {
            return this.value[entry];
        }
        
        public final int insert(long key, long value) {
            int entry = super.insert(key);
            this.value[entry] = value; 
            return entry;
        }
    }
    
    static int m_w = 36;    /* must not be zero */
    static int m_z = 23;    /* must not be zero */
    private static int nextRadomInt() {
        m_z = 36969 * (m_z & 65535) + (m_z >> 16);
        m_w = 18000 * (m_w & 65535) + (m_w >> 16);
        return (m_z << 16) + m_w;  /* 32-bit result */
    }

    public static void main(String[] args) {
    	
        new Thread(new Runnable() {
        	@Override
            public void run() {
            	long start = System.currentTimeMillis();
            	int prevCount = count;
                while (true) {
                    try {
                        Thread.sleep(1000);
                        long end = System.currentTimeMillis();
                        int iterations = count - prevCount;
                        prevCount = count;
                        log.log(Level.INFO, log.getSB().append("Iterations: ").append(iterations).append(" in ").append(end-start).append("ms, ")
                        		.append(iterations*1000.0/(end-start)).append(" iterartions/sec")
                        		.append(", sum: ").append(sum));
                        start = end;
                    } catch (InterruptedException e) {
                    }
                }
            }
        }).start();
        log.log(Level.INFO, log.getSB().append("Start the performance test loop"));

        int x = 0x80000000;
        x = x>>31;
        log.log(Level.INFO, log.getSB().append(x));

        long[] values = new long[SIZE];
        Random rnd = new Random(System.nanoTime());
        for (int i = 0; i < values.length; ++i){
            values [i] = nextRadomInt()%SIZE;
        }

        LongLongAvlMap longLongAvlMap = new LongLongAvlMap();
        LongLongRbMap longLongRbMap = new LongLongRbMap();
        //LongHashTable longRbTree = new LongHashTable();
        TreeMap<Long, Long> longTreeMap = new TreeMap<Long, Long>();
        LongLongTreeMap longLongTreeMap = new LongLongTreeMap();
        //LongLongTreeMap longLongTreeMap = new LongLongTreeMap();
        
        for (int i = 0; i < values.length; ++i) {
            longLongRbMap.insert(values[i], values[i]);
            longLongTreeMap.insert(values[i], values[i]);
            longLongAvlMap.insert(values[i], values[i]);
            longTreeMap.put(values[i], values[i]);
            //longLongTreeMap.put(values[i], values[i]);
        }

        while (true) {
            //longLongRbMap.clear();
            for (int i = 0; i < SIZE; ++i) {
                //sum += longLongTreeMap.getEntry(values[i]);
                //sum += longLongAvlMap.find(values[i]);
                //sum += longLongRbMap.find(values[i]);
                sum += longLongTreeMap.find(values[i]);
                //log.log(Level.INFO, log.getSB().append(values[i]).append(" = ").append(longLongRbMap.get(values[i])));
                //longRbTree.remove(entry);
            	//longRbTree.remove(values[i]);
                //sum += longTreeMap.get(values[i]);
                //longLongRbMap.insert(values[i], values[i]);
                //longTreeMap.put(values[i], values[i]);
            }
/*
            for (int i = 0; i < SIZE; ++i) {
                //longLongRbMap.remove(values[i]);
                //longTreeMap.remove(values[i]);
            }
*/
            ++count;
        }

/*
        while (true) {
            for (int i = 0; i < values.length; ++i) {
                longTreeSet.add(values[i]);
            }
            for (int i = 0; i < values.length; ++i) {
                longTreeSet.remove(values[i]);
            }
            ++count;
        }
        */
/*
        while (true) {
            for (int i = 0; i < values.length; ++i) {
                objectRbTree.insert(values[i]);
            }
            for (int i = 0; i < values.length; ++i) {
                int entry = objectRbTree.find(values[i]);
                objectRbTree.remove(entry);

            }
            ++count;
        }
*/
    }
}
