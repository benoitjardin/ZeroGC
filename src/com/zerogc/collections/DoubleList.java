/*
 * Copyright 2016 Benoit Jardin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zerogc.collections;

import com.zerogc.util.Level;
import com.zerogc.util.Logger;

public class DoubleList {
	protected final Logger log;

    protected int first = -1;
    protected int last = -1;

    protected Store store;

    private int size = 0;
    
    public static class Store {
    	public static final int INITIAL_CAPACITY = 16;
    	public static final float GROWTH_FACTOR = 2.0f;

    	protected final Logger log;

        private float growthFactor = GROWTH_FACTOR;

        private int freeEntry = -1;
        private int highMark = 0;
        private int size = 0;

        protected int[] next;
        protected int[] prev;
        protected double[] key;
        
        public Store() {
        	this(Store.class.getSimpleName(), INITIAL_CAPACITY, GROWTH_FACTOR);
        }
        
        public Store(String name) {
            this(name, INITIAL_CAPACITY, GROWTH_FACTOR);
        }

        public Store(String name, int initialCapacity) {
        	this(name, initialCapacity, GROWTH_FACTOR);
        }

        public Store(String name, int initialCapacity, float growthFactor) {
        	this.log = new Logger(name);
        	 
        	if (initialCapacity < 0) {
                throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
            }
            if (growthFactor <= 0 || Float.isNaN(growthFactor)) {
                throw new IllegalArgumentException("Illegal Load: " + growthFactor);
            }
            if (initialCapacity == 0) {
                initialCapacity = 1;
            }
            this.growthFactor = growthFactor;
                
            grow(0, initialCapacity);
        }

        public int highMark() {
            return this.highMark;
        }

        public int capacity() {
            return this.next.length;
        }
        
        public int size() {
        	return this.size;
        }

        protected void grow(int capacity, int newCapacity) {
            log.log(Level.WARN, log.getSB().append("Resizing to ").append(newCapacity));

            int[] newNext = new int[newCapacity];
            int[] newPrev = new int[newCapacity];
            double[] newKey = new double[newCapacity];

            if (capacity > 0) {
                System.arraycopy(next, 0, newNext, 0, capacity);
                System.arraycopy(prev, 0, newPrev, 0, capacity);
                System.arraycopy(key, 0, newKey, 0, capacity);
            }
            
            next = newNext;
            prev = newPrev;
            key = newKey;
        }
        
        public void clear() {
            this.freeEntry = -1;
            this.highMark = 0;
            this.size = 0;
        }
        
        protected int newEntry() {
            int entry = this.freeEntry;
            if (entry != -1) {
            	this.freeEntry = this.next[entry];
            } else {
                int capacity = this.next.length;
            	if (highMark >= capacity) {
    	            // Grow the arrays
    	            int newCapacity = (int) (capacity * this.growthFactor);
    	            grow(capacity, newCapacity);
            	}
    	        entry = highMark++;
            }
            
            this.next[entry] = -1;
            this.prev[entry] = -1;

            this.size++;
            return entry;
        }
        
        protected void removeEntry(int entry) {
        	this.next[entry] = this.freeEntry;
            this.freeEntry = entry;
            this.size--;
        }
    }
    
    public DoubleList() {
        this(DoubleList.class.getSimpleName(), Store.INITIAL_CAPACITY, Store.GROWTH_FACTOR);
    }

    public DoubleList(String name) {
        this(name, Store.INITIAL_CAPACITY, Store.GROWTH_FACTOR);
    }

    public DoubleList(String name, int initialCapacity) {
        this(name, initialCapacity, Store.GROWTH_FACTOR);
    }

    public DoubleList(String name, int initialCapacity, float growthFactor) {
    	this(name, new Store(name, initialCapacity, growthFactor));
    }
    
    public DoubleList(String name, Store store) {
    	this.log = new Logger(name);
    	this.store = store;
    }
    
    public int highMark() {
        return this.store.highMark();
    }

    public int capacity() {
        return this.store.capacity();
    }

    public int size() {
        return this.size;
    }
    
    public boolean isEmpty() {
        return this.size == 0;
    }
    
    public void clear() {
    	for (int entry = firstEntry(); entry != -1; entry = nextEntry(entry)) {
    		store.removeEntry(entry);
    	}
        this.first = -1;
        this.last = -1;
        this.size = 0;
    }
    
    public double getKey(int entry) {
    	return this.store.key[entry];
    }
    
    private int newEntry() {
        this.size++;
        return store.newEntry();
    }

    /** Returns the first entry in the collection or {@code -1} if it is empty. */
    public int firstEntry() {
        return this.first;
    }

    /** Returns the last entry in the collection or {@code -1} if it is empty. */
    public int lastEntry() {
        return this.last;
    }
    
    /** Returns the next entry in the collection or {@code -1} when the end is reached. */
    public int nextEntry(int x) {
    	return this.store.next[x];
    }

    /** Returns the previous entry in the collection or {@code -1} when the beginning is reached. */
    public int prevEntry(int x) {
    	return this.store.prev[x];
    }

    /** 
     * Remove the specified entry from the collection.
     * @param entry to remove.
     * @return the next entry.
     */
    protected final int removeEntry(int entry) {
    	int next = this.store.next[entry];
    	int prev = this.store.prev[entry]; 
    	if (prev == -1) {
    		this.first = next; 
    	} else {
    		this.store.next[prev] = next;
    	}
    	if (next == -1) {
    		this.last = prev;
    	} else {
    		this.store.prev[next] = prev;
    	}
    	store.removeEntry(entry);
        this.size--;
        return next;
    }

    
    public int addFirst(double key) {
    	int entry = newEntry();
    	this.store.prev[entry] = -1;
    	this.store.next[entry] = this.first;
    	if (this.first == -1) {
    		this.last = entry;
    	} else {
    		this.store.prev[this.first] = entry;
    	}
    	this.first = entry;
    	this.store.key[entry] = key;
    	return entry;
    }

    public int addLast(double key) {
    	int entry = newEntry();
    	this.store.next[entry] = -1;
    	this.store.prev[entry] = this.last;
    	if (this.last == -1) {
    		this.first = entry;
    	} else {
    		this.store.next[this.last] = entry;
    	}
    	this.last = entry;
    	this.store.key[entry] = key;
    	return entry;
    }

    public EntryIterator entryIterator(EntryIterator entryIterator) {
        entryIterator.init(this);
        return entryIterator;
    }
    
    public static class EntryIterator {
    	private DoubleList list;
    	private int entry;
    	private int nextEntry;
    	
    	public EntryIterator() {
    	}
    	
    	public void init(DoubleList list) {
    		this.list = list;
    		entry = -1;
    		nextEntry = list.firstEntry();
    	}
    	
		public boolean hasNext() {
			return nextEntry != -1;
		}

		public int nextEntry() {
			entry = nextEntry;
    		nextEntry = list.nextEntry(nextEntry);
			return entry;
	    }

		public void remove() {
			list.removeEntry(entry);
			entry = -1;
	    }
    }
}
