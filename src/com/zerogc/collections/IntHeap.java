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

import com.zerogc.logging.Level;
import com.zerogc.logging.LogManager;
import com.zerogc.logging.Logger;

/**
 * @author Benoit Jardin
 * Textbook implementation of Heap.
 * See: "Introduction to Algorithms" by Cormen, Leiserson, Rivest and Stein
 */

public class IntHeap {
    public static final int INITIAL_CAPACITY = 16;
    public static final float GROWTH_FACTOR = 2.0f;

    protected final Logger log;

    private float growthFactor = GROWTH_FACTOR;
    private int size = 0;

    private int freeEntry = -1;
    private int highMark = 0;

    protected int[] node; // Map heap nodes to entries
    protected int[] entry; // Map entries to heap nodes
    protected int[] key;

    protected Comparator.IntComparator comparator = new Comparator.IntComparator(); 

    public IntHeap() {
        this(IntHeap.class.getSimpleName(), INITIAL_CAPACITY, GROWTH_FACTOR);
    }

    public IntHeap(String name) {
        this(name, INITIAL_CAPACITY, GROWTH_FACTOR);
    }

    public IntHeap(String name, int initialCapacity) {
        this(name, initialCapacity, GROWTH_FACTOR);
    }

    public IntHeap(String name, int initialCapacity, float growthFactor) {
        this.log = LogManager.getLogger(name);

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

    public void setComparator(Comparator.IntComparator comparator) {
            if (!this.isEmpty()) {
                    throw new IllegalStateException("Collection not empty!");
            }
            this.comparator = comparator;
    }

    public int highMark() {
        return this.highMark;
    }

    public int capacity() {
        return this.entry.length;
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    protected void grow(int capacity, int newCapacity) {
        log.log(Level.WARN, log.getSB().append("Resizing to ").append(newCapacity));

        int[] newTreeNodeToEntry = new int[newCapacity];
        int[] newEntryToTreeNode = new int[newCapacity];
        int[] newKey = new int[newCapacity];

        if (capacity > 0) {
            System.arraycopy(this.node, 0, newTreeNodeToEntry, 0, capacity);
            System.arraycopy(this.entry, 0, newEntryToTreeNode, 0, capacity);
            System.arraycopy(this.key, 0, newKey, 0, capacity);
        }

        this.node = newTreeNodeToEntry;
        this.entry = newEntryToTreeNode;
        this.key = newKey;
    }

    public void clear() {
        this.size = 0;
        this.freeEntry = -1;
        this.highMark = 0;
    }

    public int getKey(int entry) {
        return this.key[entry];
    }

    private int newEntry() {
        int entry = this.freeEntry;
        if (entry != -1) {
            this.freeEntry = this.entry[entry];
        } else {
            int capacity = this.entry.length;
            if (highMark >= capacity) {
                // Grow the arrays
                int newCapacity = (int) (capacity * this.growthFactor);
                grow(capacity, newCapacity);
            }
            entry = highMark++;
        }

        this.entry[entry] = this.size;
        this.node[this.size] = entry;

        this.size++;
        return entry;
    }

    /** Returns the first entry in the collection or {@code -1} if it is empty. */
    public int firstEntry() {
        return this.node[0];
    }

    /** Returns the last entry in the collection or {@code -1} if it is empty. */
    public int lastEntry() {
        return (this.size > 0) ? this.node[this.size-1] : -1;
    }

    /** Returns the previous entry in the collection or {@code -1} when the beginning is reached. */
    public int prevEntry(int entry) {
        int node = this.entry[entry];
        return --node >= 0 ? this.node[node] : -1;
    }

    /** Returns the next entry in the collection or {@code -1} when the end is reached. */
    public int nextEntry(int entry) {
        int node = this.entry[entry];
        return ++node < this.size ? this.node[node] : -1;
    }

    /**
     * Returns an entry of the key in the collection.
     * @param key
     * @return
     */
/*
    public int find(int key) {
        int node = 0;
        int entry = -1;
        while (node < size) {
                int nodeEntry = this.node[node];
            int cmp = comparator.compare(key, this.key[nodeEntry]);
            if (cmp > (cmp^cmp)) { // Faster than if (cmp < 0) with Sun jdk1.6
                    // Left child
                    int left = (node << 1) + 1;
                    if (left < size) {
                            node = left;
                            continue;
                    }
            } else if (cmp == 0){
                    entry = nodeEntry;
                    break;
            }
                // Next right sibling or ancestor's right sibling
            // Left nodes are odd, right nodes are even
            while ((node & 0x01) == 0) {
                    node = (node - 1) >> 1;
                    if (node <= 0) {
                            return -1;
                }
            }
            node++;
        }
        return entry;
    }
*/
    /**
     * Insert a key in the collection.
     * If the collection already contained the key, the existing entry is returned.
     * @param key the key to insert.
     * @return the entry of the key that was inserted.
     */
    public int insert(int key) {
        int node = size;
        int entry = newEntry();
        this.key[entry] = key;

        node = bubleUp(node, key);

        this.node[node] = entry;
        this.entry[entry] = node;
        return entry;
    }

    /**
     * Move a node in the heap
     * @param to
     * @param from
     */
    protected void move(int to, int from) {
        int fromEntry = this.node[from];
        this.node[to] = fromEntry;
        this.entry[fromEntry] = to;
    }

    /** 
     * Remove the specified entry from the collection.
     * @param entry to remove.
     * @return the next entry.
     */
    public int removeEntry(int entry) {
        int nextEntry = nextEntry(entry); 
        int node = this.entry[entry];
        int lastEntry = this.node[--size];
        int lastKey = this.key[lastEntry];

        node = bubleDown(node, lastKey);
        this.node[node] = lastEntry;
        this.entry[lastEntry] = node;

        this.node[size] = -1;
        this.entry[entry] = this.freeEntry;
        this.freeEntry = entry;

        return nextEntry;
    }

    /**
     * Find position of key in the heap at or above a given node.
     * @param node the original node.
     * @param key for which we are looking for a node. 
     * @return the node where to store the key.
     */
    private int bubleUp(int node, int key) {
        while (node != 0) {
            int parent = (node -1) >> 1;
            if (comparator.compare(this.key[this.node[parent]], key) <= 0) {
                break;
            }
            move(node, parent);
            node = parent;
        }
        return node;
    }

    /**
     * Find position of key in the heap at or below a given node.
     * @param node the original node.
     * @param key for which we are looking for a node. 
     * @return the node where to store the key.
     */
    private int bubleDown(int node, int key) {
        int child;
        while ((child = (node << 1)+1) < this.size) {
            // Find smallest child
            int childKey = this.key[this.node[child]];
            if (child+1 < this.size) {
                int rightChildKey = this.key[this.node[child+1]];
                if (comparator.compare(childKey, rightChildKey) > 0) {
                    // Adjust smallest child
                    ++child;
                    childKey = rightChildKey;
                }
            }
            if (comparator.compare(key, childKey) <= 0) {
                break;
            }
            move(node, child);
            node = child;
        }
        return node;
    }

    public EntryIterator entryIterator(EntryIterator entryIterator) {
        entryIterator.init(this);
        return entryIterator;
    }

    public static class EntryIterator {
        private IntHeap heap;
        private int node;
        private int nextNode;

        /** Initialize the iterator at the beginning of the collection. */
        public void init(IntHeap heap) {
            this.heap = heap;
            node = -1;
            nextNode = 0;
        }

        /** Returns {@code true} if the iteration has more elements.
         * {@link #hasNext} returning true guarantees that {@link #nextNode} will not return -1.*/
        public boolean hasNext() {
            return nextNode < heap.size();
        }

        /** Returns the next entry in the collection or {@code -1} if the iterator has reached the end. */
        public int nextEntry() {
            node = nextNode++;
            return heap.node[node];
        }

        /** Remove from the iteration's current entry from the underlying collection. */
        public void remove() {
            nextNode = heap.removeEntry(heap.node[node]);
            node = -1;
        }
    }
}
