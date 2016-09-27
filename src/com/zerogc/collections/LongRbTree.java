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

import com.zerogc.util.ByteStringBuilder;
import com.zerogc.util.Level;
import com.zerogc.util.Logger;

/**
 * @author Benoit Jardin
 * Textbook implementation of RedBlack Tree.
 * See: "Introduction to Algorithms" by Cormen, Leiserson, Rivest and Stein
 */

public class LongRbTree {
	public static final int INITIAL_CAPACITY = 16;
	public static final float GROWTH_FACTOR = 2.0f;

    protected static final byte RED = 0;
    protected static final byte BLACK = 1;

	protected final Logger log;
	
    private float growthFactor = GROWTH_FACTOR;
    private int size = 0;
    
    protected int root = -1;
    private int freeEntry = -1;
    private int highMark = 0;

    protected int[] left;
    protected int[] right;
    protected int[] parent;
    protected byte[] color;
    protected long[] key;
    protected Comparator.LongComparator comparator = new Comparator.LongComparator(); 
    
    public LongRbTree() {
    	this(LongRbTree.class.getSimpleName(), INITIAL_CAPACITY, GROWTH_FACTOR);
    }
    
    public LongRbTree(String name) {
        this(name, INITIAL_CAPACITY, GROWTH_FACTOR);
    }

    public LongRbTree(String name, int initialCapacity) {
    	this(name, initialCapacity, GROWTH_FACTOR);
    }

    public LongRbTree(String name, int initialCapacity, float growthFactor) {
    	this.log = new Logger(name);
    	 
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        }
        if (growthFactor <= 0 || Float.isNaN(growthFactor)) {
            throw new IllegalArgumentException("Illegal GrowthFactor: " + growthFactor);
        }
        if (initialCapacity == 0) {
            initialCapacity = 1;
        }
        this.growthFactor = growthFactor;
        
        grow(0, initialCapacity);
    }

    public void setComparator(Comparator.LongComparator comparator) {
    	if (!this.isEmpty()) {
    		throw new IllegalStateException("Collection not empty!");
    	}
    	this.comparator = comparator;
    }
    
    public int highMark() {
        return this.highMark;
    }

    public int capacity() {
        return this.parent.length;
    }

    public int size() {
        return this.size;
    }
    
    public boolean isEmpty() {
        return this.size == 0;
    }
    
    protected void grow(int capacity, int newCapacity) {
        log.log(Level.WARN, log.getSB().append("Resizing to ").append(newCapacity));

        int[] newLeft = new int[newCapacity];
        int[] newRight = new int[newCapacity];
        int[] newParent = new int[newCapacity];
        byte[] newColor = new byte[newCapacity];
        long[] newKey = new long[newCapacity];
        
        if (capacity > 0) {
            System.arraycopy(left, 0, newLeft, 0, capacity);
            System.arraycopy(right, 0, newRight, 0, capacity);
            System.arraycopy(parent, 0, newParent, 0, capacity);
            System.arraycopy(color, 0, newColor, 0, capacity);
            System.arraycopy(key, 0, newKey, 0, capacity);
        }
        
        left = newLeft;
        right = newRight;
        parent = newParent;
        color = newColor;
        key = newKey;
    }
    
    public void clear() {
        this.freeEntry = -1;
        this.highMark = 0;
        this.root = -1;
        this.size = 0;
    }
    
    public long getKey(int entry) {
    	return this.key[entry];
    }
    
    private int newEntry() {
        int entry = this.freeEntry;
        if (entry != -1) {
        	this.freeEntry = this.parent[entry];
        } else {
            int capacity = this.parent.length;
        	if (highMark >= capacity) {
	            // Grow the arrays
	            int newCapacity = (int)(capacity * this.growthFactor);
	            grow(capacity, newCapacity);
        	}
	        entry = highMark++;
        }
        
        left[entry] = -1;
        right[entry] = -1;

        this.size++;
        return entry;
    }
    
    /** Returns the first entry in the collection or {@code -1} if it is empty. */
    public int firstEntry() {
        int entry = root;
        if (entry != -1) {
            while (left[entry] != -1) {
                entry = left[entry];
            }
        }
        return entry;
    }
    
    /** Returns the last entry in the collection or {@code -1} if it is empty. */
    public int lastEntry() {
        int entry = root;
        if (entry != -1) {
            while (right[entry] != -1) {
                entry = right[entry];
            }
        }
        return entry;
    }
    
    /** Returns the previous entry in the collection or {@code -1} when the beginning is reached. */
    public int prevEntry(int entry) {
        if (left[entry] != -1) {
            entry = left[entry];
            while (right[entry] != -1) {
                entry = right[entry];
            }
        } else {
            int parent = this.parent[entry];
            while (parent != -1 && entry == left[parent]) {
                entry = parent;
                parent = this.parent[entry];
            }
            entry = parent;
        }
        return entry;
    }

    /** Returns the next entry in the collection or {@code -1} when the end is reached. */
    public int nextEntry(int entry) {
        if (right[entry] != -1) {
            entry = right[entry];
            while (left[entry] != -1) {
                entry = left[entry];
            }
        } else {
            int parent = this.parent[entry];
            while (parent != -1 && entry == right[parent]) {
                entry = parent;
                parent = this.parent[entry];
            }
            entry = parent;
        }
        return entry;
    }

    /**
     * Returns an entry of the key in the collection.
     * @param key the key to find.
     * @return the entry of the key or {@code -1} if not found.
     */
    public int find(long key) {
        int x = root;
        while (x != -1) {
            int cmp = comparator.compare(key, this.key[x]);
            if (cmp < (cmp^cmp)) { // Faster than if (cmp < 0) with Sun jdk1.6
                x = left[x];
            } else if (cmp != 0) {
                x = right[x];
            } else {
                break;
            }
        }
        return x;
    }
    
    /**
     * Returns the first entry of the key in the collection.
     * @param key the key to find.
     * @return the entry of the key or {@code -1} if not found.
     */
    public int findFirst(long key) {
        int x = root;
        int entry = -1;
        while (x != -1) {
            //if (!(comparator.compare(this.key[x], key) < 0)) {
        	if (!comparator.less(this.key[x], key)) {
            	entry = x; // key[x] >= key
                x = left[x];
            } else {
                x = right[x];
            }
        }
        //return (entry == -1) || (comparator.compare(key, this.key[entry]) < 0) ? -1 : entry;
        return (entry == -1) || comparator.less(key, this.key[entry]) ? -1 : entry;
    }
    
    /**
     * Returns the entry of the greatest key in the collection that is less than or equal to the given key, or {@code -1} if there is no such key.
     * @param key the key to find.
     * @return the entry of the key or {@code -1} if there is no such key.
     * */
    public int floor(long key) {
        int x = root;
        int entry = -1;
        while (x != -1) {
            if (comparator.compare(key, this.key[x]) < 0) {
                x = left[x];  // key[x] > key
            } else {
            	entry = x; // key[x] <= key, this is a candidate
                x = right[x];
            }
        }
        return entry;        
    }

    /**
     * Returns the entry of the least element in the collection that is greater than or equal to the given element, or {@code -1} if there is no such key.
     * @param key the key to find.
     * @return the entry of the key or {@code -1} if there is no such key.
     * */
    public int ceiling(long key) {
        int x = root;
        int entry = -1;
        while (x != -1) {
            if (comparator.compare(key, this.key[x]) <= 0) {
            	entry = x; // key[x] >= key, this is a candidate
                x = left[x];  // key[x] < key
            } else {
                x = right[x]; // key[x] < key
            }
        }
        return entry;        
    }
    
    /**
     * Insert a key in the collection.
     * If the collection already contained the key, the existing entry is returned.
     * @param key the key to insert.
     * @return the entry of the key that was inserted.
     */
    public int insert(long key) {
        int parent = -1;
        int entry = root;
        int cmp = 0;
        
        while (entry != -1) {
            parent = entry;
            cmp = comparator.compare(key, this.key[entry]);
            if (cmp < (cmp^cmp)) { // Faster than if (cmp < 0) with Sun jdk1.6
                entry = left[entry];
            } else if (cmp != 0) {
                entry = right[entry];
            } else {
                return entry;
            }
        }
        
        entry = newEntry();
        this.key[entry] = key;
        this.parent[entry] = parent;
        if (parent == -1) {
            root = entry;
            color[root] = BLACK;
        } else {
            if (cmp < (cmp^cmp)) { // Faster than if (cmp < 0) with Sun jdk1.6
                left[parent] = entry;
            } else {
                right[parent] = entry;
            }
            rebalanceRemove(entry);
        }
        return entry;
    }

    /**
     * Insert a key in the collection.
     * If the collection already contained the key, a new entry is created after the last instance of the key.
     * @param key the key to insert.
     * @return the entry of the key that was inserted.
     */
    public int insertMulti(long key) {
        int parent = -1;
        int entry = root;
        int cmp = 0;
        
        while (entry != -1) {
            parent = entry;
            cmp = comparator.compare(key, this.key[entry]);
            if (cmp < (cmp^cmp)) { // Faster than if (cmp < 0) with Sun jdk1.6
                entry = left[entry];
            } else {
                entry = right[entry];
            }
        }
        
        entry = newEntry();
        this.key[entry] = key;
        this.parent[entry] = parent;
        if (parent == -1) {
            root = entry;
            color[root] = BLACK;
        } else {
            if (cmp < (cmp^cmp)) { // Faster than if (cmp < 0) with Sun jdk1.6
                left[parent] = entry;
            } else {
                right[parent] = entry;
            }
            rebalanceRemove(entry);
        }
        return entry;
    }
    
    /** 
     * Remove the specified entry from the collection.
     * @param entry to remove.
     * @return the next entry.
     */
    public int removeEntry(int entry) {
    	int next = nextEntry(entry);
        int spliceEntry = (left[entry] == -1 || right[entry] == -1) ? entry : next;
        // if entry != -1 then spliceEntry != -1
        // If entry has a right child then entry's successor can't have a left child  
        int spliceChild = (left[spliceEntry] != -1) ? left[spliceEntry] : right[spliceEntry];
        int spliceParent = parent[spliceEntry];
        if (spliceChild != -1) {
            parent[spliceChild] = spliceParent;
        }
        if (spliceParent == -1) {
            this.root = spliceChild;
        } else if (spliceEntry == left[spliceParent]) {
            left[spliceParent] = spliceChild;
        } else {
            right[spliceParent] = spliceChild;
        }
        int spliceColor = this.color[spliceEntry];
        if (spliceEntry != entry) {
            // Replace entry to remove in the tree with spliceEntry
            this.color[spliceEntry] = this.color[entry];
            
            int right = this.right[entry];
            this.right[spliceEntry] = right;
            if (right != -1) {
                parent[right] = spliceEntry;
            }
            int left = this.left[entry];
            this.left[spliceEntry] = left;
            if (left != -1) {
                parent[left] = spliceEntry;
            }
            int parent = this.parent[entry];
            this.parent[spliceEntry] = parent;
            if (parent == -1) {
                this.root = spliceEntry;
            } else if (entry == this.left[parent]) {
                this.left[parent] = spliceEntry;
            } else {
                this.right[parent] = spliceEntry;
            }
            
            if (spliceParent == entry) {
            	spliceParent = spliceEntry;
            }
        }
        if (spliceColor == BLACK) {
            rebalanceRemove(spliceParent, spliceChild);
        }

        parent[entry] = this.freeEntry;
        this.freeEntry = entry;
        this.size--;

        return next;
    }
    
	//   x            y
	//  / \          / \
	// a   y   -->  x   c
	//    / \      / \
	//   b   c    a   b
    private int rotateLeft(int x) {
        int y = right[x];
        right[x] = left[y];
        if (left[y] != -1) {
            parent[left[y]] = x;
        }
        parent[y] = parent[x];
        if (parent[x] == -1) {
            this.root = y;
        } else if (x == left[parent[x]]) {
            left[parent[x]] = y;
        } else {
            right[parent[x]] = y;
        }
        left[y] = x;
        parent[x] = y;
        return y;
    }
    
    //     x           y
    //    / \         / \
    //   y   c  -->  a   x
    //  / \             / \
    // a   b           b   c
    private int rotateRight(int x) {
        int y = left[x];
        left[x] = right[y];
        if (right[y] != -1) {
            parent[right[y]] = x;
        }
        parent[y] = parent[x];
        if (parent[x] == -1) {
            this.root = y;
        } else if (x == right[parent[x]]) {
            right[parent[x]] = y;
        } else {
            left[parent[x]] = y;
        }
        right[y] = x;
        parent[x] = y;
        return y;
    }

    private void rebalanceRemove(int entry) {
        color[entry] = RED;
        int parent = this.parent[entry];
        while (entry != root && color[parent] == RED) {
            int grandParent = this.parent[parent];
            if (grandParent == -1) {
            	// Just need to make the parent/root black
            	break;
            } else if (parent == left[grandParent]) {
                int uncle = right[grandParent];
                if (uncle != -1 && color[uncle] == RED) {
                    color[parent] = BLACK;
                    color[uncle] = BLACK;
                    color[grandParent] = RED;
                    entry = grandParent;
                    parent = this.parent[entry];
                } else {
                    if (entry == right[parent]) {
                    	// operation swaps entry and parent while grandParent stays the same  
                        entry = parent;
                        parent = rotateLeft(parent);
                    }
                    color[parent] = BLACK; // Break the loop
                    color[grandParent] = RED;
                    // operation does not change parent
                    rotateRight(grandParent);
                }
            } else {
                int uncle = left[grandParent];
                if (uncle != -1 && color[uncle] == RED) {
                    color[parent] = BLACK;
                    color[uncle] = BLACK;
                    color[grandParent] = RED;
                    entry = grandParent;
                    parent = this.parent[entry];
                } else {
                    if (entry == left[parent]) {
                    	// operation swaps entry and parent while grandParent stays the same  
                        entry = parent;
                        parent = rotateRight(parent);
                    }
                    color[parent] = BLACK; // Break the loop
                    color[grandParent] = RED;
                    // operation does not change parent
                    rotateLeft(grandParent);
                }
            }
        }
        color[root] = BLACK;
    }
    
    private void rebalanceRemove(int entry, int child) {
        // It is possible that child == -1
        // parent == -1 only when child == root
        while (child != this.root && (child == -1 || color[child] == BLACK)) {
            // parent != -1 since child != root
            if (child == left[entry]) {
                int sibling = right[entry];
                if (sibling == -1) {
                    child = entry;
                    entry = this.parent[child];                    
                } else {
                    if (color[sibling] == RED) {
                        color[sibling] = BLACK;  // sibling becomes grandParent after rotation
                        color[entry] = RED;
                        rotateLeft(entry);
                        sibling = right[entry];
                        // sibling != -1
                    }
                    // color[sibling] == BLACK
                    int leftNephew = left[sibling];
                    int rightNephew = right[sibling];
                    if ((leftNephew == -1 || color[leftNephew] == BLACK) &&
                    		(rightNephew == -1 || color[rightNephew] == BLACK)) {
                        color[sibling] = RED;
                        child = entry;
                        entry = this.parent[child];
                    } else {
                        if (rightNephew == -1 || color[rightNephew] == BLACK) {
                            // color[leftNephew] == RED
                            color[leftNephew] = BLACK; // leftNephew becomes sibling after rotation
                            color[sibling] = RED; // sibling becomes rightNephew after rotation
                            rightNephew = sibling;
                            sibling = rotateRight(sibling);
                        }
                        // color[rightNephew] == RED
                        color[sibling] = color[entry];
                        color[entry] = BLACK;
                        color[rightNephew] = BLACK;
                        rotateLeft(entry);
                        child = this.root; // End the loop
                    }
                }
            } else {
                int sibling = left[entry];
                if (sibling == -1) {
                    child = entry;
                    entry = this.parent[child];                    
                } else {
                    if (sibling != -1 && color[sibling] == RED) {
                        color[sibling] = BLACK; // sibling becomes grandParent after rotation
                        color[entry] = RED;
                        rotateRight(entry);
                        sibling = left[entry];
                        // sibling != -1
                    }
                    // color[sibling] == BLACK
                    int leftNephew = left[sibling];
                    int rightNephew = right[sibling];
                    if ((leftNephew == -1 || color[leftNephew] == BLACK) &&
                    		(rightNephew == -1 || color[rightNephew] == BLACK)) {
                        color[sibling] = RED;
                        child = entry;
                        entry = this.parent[child];
                    } else {
                        if (leftNephew == -1 || color[leftNephew] == BLACK) {
                            // color[rightNephew] == RED
                            color[rightNephew] = BLACK;
                            color[sibling] = RED;
                            leftNephew = sibling;
                            sibling = rotateLeft(sibling);
                        }
                        // color[leftNephew] == RED
                        color[sibling] = color[entry];
                        color[entry] = BLACK;
                        color[leftNephew] = BLACK;
                        rotateRight(entry);
                        child = this.root;  // End the loop
                    }
                }
            }
        }
        if (child != -1) {
            color[child] = BLACK;
        }
    }

    public ByteStringBuilder toString(ByteStringBuilder sb) {
    	for (int entry = firstEntry(); entry != -1; entry = nextEntry(entry)) {
    		sb.append("[entry=").append(entry).append("]");
    		sb.append("[key=").append(key[entry]).append("]");
    		sb.append("[parent=").append(parent[entry]).append("]");
    		sb.append("[left=").append(left[entry]).append("]");
    		sb.append("[right=").append(right[entry]).append("]");
    		sb.append("\n");
    	}
    	return sb;
    }

    public EntryIterator entryIterator(EntryIterator entryIterator) {
        entryIterator.init(this);
        return entryIterator;
    }
    
    public static class EntryIterator {
    	private LongRbTree rbTree;
    	private int entry;
    	private int nextEntry;
    	
    	/** Initialize the iterator at the beginning of the collection. */
    	public void init(LongRbTree rbTree) {
    		this.rbTree = rbTree;
    		entry = -1;
    		nextEntry = rbTree.firstEntry();
    	}
    	
		/** Returns {@code true} if the iteration has more elements.
		 * {@link #hasNext} returning true guarantees that {@link #nextEntry} will not return -1.*/
		public boolean hasNext() {
			return nextEntry != -1;
		}

		/** Returns the next entry in the collection or {@code -1} if the iterator has reached the end. */
		public int nextEntry() {
			entry = nextEntry;
    		nextEntry = rbTree.nextEntry(nextEntry);
			return entry;
	    }

		/** Remove from the iteration's current entry from the underlying collection. */
		public void remove() {
			rbTree.removeEntry(entry);
			entry = -1;
	    }
    }
}
