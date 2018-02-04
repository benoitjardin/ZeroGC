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

import com.zerogc.core.ByteStringBuilder;
import com.zerogc.logging.Level;
import com.zerogc.logging.LogManager;
import com.zerogc.logging.Logger;

/**
 * @author Benoit Jardin
 * Textbook implementation of RedBlack Tree.
 * See: "Introduction to Algorithms" by Cormen, Leiserson, Rivest and Stein
 * 
 * This implementation allows to share storage across multiple trees.
 */

public class _KeyName_TreeSet {
    protected static final byte RED = 0;
    protected static final byte BLACK = 1;

    protected final Logger log;

    protected Store store;

    private int size = 0;
    private int root = -1;
    private Comparator._KeyName_Comparator comparator = new Comparator._KeyName_Comparator(); 

    public static class Store {
        public static final int INITIAL_CAPACITY = 16;
        public static final float GROWTH_FACTOR = 2.0f;

        protected final Logger log;

        private float growthFactor = GROWTH_FACTOR;

        private int freeEntry = -1;
        private int highMark = 0;
        private int size = 0;

        protected int[] left;
        protected int[] right;
        protected int[] parent;
        protected byte[] color;
        protected _KeyType_[] key;

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

        public int highMark() {
            return this.highMark;
        }

        public int capacity() {
            return this.parent.length;
        }

        public int size() {
            return this.size;
        }

        protected void grow(int capacity, int newCapacity) {
            log.log(Level.WARN, log.getSB().append("Resizing to ").append(newCapacity));

            int[] newLeft = new int[newCapacity];
            int[] newRight = new int[newCapacity];
            int[] newParent = new int[newCapacity];
            byte[] newColor = new byte[newCapacity];
            _KeyType_[] newKey = new _KeyType_[newCapacity];

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

        private void clear() {
            this.freeEntry = -1;
            this.highMark = 0;
            this.size = 0;
        }

        protected int newEntry() {
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

        protected void removeEntry(int entry) {
            parent[entry] = this.freeEntry;
            this.freeEntry = entry;
            this.size--;
        }
    }

    public _KeyName_TreeSet() {
        this(_KeyName_TreeSet.class.getSimpleName(), Store.INITIAL_CAPACITY, Store.GROWTH_FACTOR);
    }

    public _KeyName_TreeSet(String name) {
        this(name, Store.INITIAL_CAPACITY, Store.GROWTH_FACTOR);
    }

    public _KeyName_TreeSet(String name, int initialCapacity) {
        this(name, initialCapacity, Store.GROWTH_FACTOR);
    }

    public _KeyName_TreeSet(String name, int initialCapacity, float growthFactor) {
        this(name, new Store(name, initialCapacity, growthFactor));
    }

    public _KeyName_TreeSet(String name, Store store) {
        this.log = LogManager.getLogger(name);
        this.store = store;
    }

    public void setComparator(Comparator._KeyName_Comparator comparator) {
        if (!this.isEmpty()) {
            throw new IllegalStateException("Collection not empty!");
        }
        this.comparator = comparator;
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
        this.root = -1;
        this.size = 0;
    }

    public _KeyType_ getKey(int entry) {
        return this.store.key[entry];
    }

    private int newEntry() {
        this.size++;
        return store.newEntry();
    }

    /** Returns the first entry in the collection or {@code -1} if it is empty. */
    public int firstEntry() {
        int entry = root;
        if (entry != -1) {
            while (store.left[entry] != -1) {
                entry = store.left[entry];
            }
        }
        return entry;
    }

    /** Returns the last entry in the collection or {@code -1} if it is empty. */
    public int lastEntry() {
        int entry = root;
        if (entry != -1) {
            while (store.right[entry] != -1) {
                entry = store.right[entry];
            }
        }
        return entry;
    }

    /** Returns the previous entry in the collection or {@code -1} when the beginning is reached. */
    public int prevEntry(int entry) {
        if (store.left[entry] != -1) {
            entry = store.left[entry];
            while (store.right[entry] != -1) {
                entry = store.right[entry];
            }
        } else {
            int parent = this.store.parent[entry];
            while (parent != -1 && entry == store.left[parent]) {
                entry = parent;
                parent = this.store.parent[entry];
            }
            entry = parent;
        }
        return entry;
    }

    /** Returns the next entry in the collection or {@code -1} when the end is reached. */
    public int nextEntry(int entry) {
        if (store.right[entry] != -1) {
            entry = store.right[entry];
            while (store.left[entry] != -1) {
                entry = store.left[entry];
            }
        } else {
            int parent = this.store.parent[entry];
            while (parent != -1 && entry == store.right[parent]) {
                entry = parent;
                parent = this.store.parent[entry];
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
    public int find(_KeyType_ key) {
        int x = root;
        while (x != -1) {
            int cmp = comparator.compare(key, this.store.key[x]);
            if (cmp < (cmp^cmp)) { // Faster than if (cmp < 0) with Sun jdk1.6
                x = store.left[x];
            } else if (cmp != 0) {
                x = store.right[x];
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
    public int findFirst(_KeyType_ key) {
        int x = root;
        int entry = -1;
        while (x != -1) {
            //if (!(comparator.compare(this.treeStore.key[x], key) < 0)) {
            if (!comparator.less(this.store.key[x], key)) {
                entry = x; // treeStore.key[x] >= key
                x = store.left[x];
            } else {
                x = store.right[x];
            }
        }
        //return (entry == -1) || (comparator.compare(key, this.treeStore.key[entry]) < 0) ? -1 : entry;
        return (entry == -1) || comparator.less(key, this.store.key[entry]) ? -1 : entry;
    }

    /**
     * Returns the entry of the greatest key in the collection that is less than or equal to the given key, or {@code -1} if there is no such key.
     * @param key the key to find.
     * @return the entry of the key or {@code -1} if there is no such key.
     * */
    public int floor(_KeyType_ key) {
        int x = root;
        int entry = -1;
        while (x != -1) {
            if (comparator.compare(key, this.store.key[x]) < 0) {
                x = store.left[x];  // treeStore.key[x] > key
            } else {
                entry = x; // treeStore.key[x] <= key, this is a candidate
                x = store.right[x];
            }
        }
        return entry;
    }

    /**
     * Returns the entry of the least element in the collection that is greater than or equal to the given element, or {@code -1} if there is no such key.
     * @param key the key to find.
     * @return the entry of the key or {@code -1} if there is no such key.
     * */
    public int ceiling(_KeyType_ key) {
        int x = root;
        int entry = -1;
        while (x != -1) {
            if (comparator.compare(key, this.store.key[x]) <= 0) {
                entry = x; // treeStore.key[x] >= key, this is a candidate
                x = store.left[x];  // treeStore.key[x] < key
            } else {
                x = store.right[x]; // treeStore.key[x] < key
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
    public int insert(_KeyType_ key) {
        int parent = -1;
        int entry = root;
        int cmp = 0;

        while (entry != -1) {
            parent = entry;
            cmp = comparator.compare(key, this.store.key[entry]);
            if (cmp < (cmp^cmp)) { // Faster than if (cmp < 0) with Sun jdk1.6
                entry = store.left[entry];
            } else if (cmp != 0) {
                entry = store.right[entry];
            } else {
                return entry;
            }
        }

        entry = newEntry();
        this.store.key[entry] = key;
        this.store.parent[entry] = parent;
        if (parent == -1) {
            root = entry;
            store.color[root] = BLACK;
        } else {
            if (cmp < (cmp^cmp)) { // Faster than if (cmp < 0) with Sun jdk1.6
                store.left[parent] = entry;
            } else {
                store.right[parent] = entry;
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
    public int insertMulti(_KeyType_ key) {
        int parent = -1;
        int entry = root;
        int cmp = 0;

        while (entry != -1) {
            parent = entry;
            cmp = comparator.compare(key, this.store.key[entry]);
            if (cmp < (cmp^cmp)) { // Faster than if (cmp < 0) with Sun jdk1.6
                entry = store.left[entry];
            } else {
                entry = store.right[entry];
            }
        }

        entry = newEntry();
        this.store.key[entry] = key;
        this.store.parent[entry] = parent;
        if (parent == -1) {
            root = entry;
            store.color[root] = BLACK;
        } else {
            if (cmp < (cmp^cmp)) { // Faster than if (cmp < 0) with Sun jdk1.6
                store.left[parent] = entry;
            } else {
                store.right[parent] = entry;
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
        int spliceEntry = (store.left[entry] == -1 || store.right[entry] == -1) ? entry : next;
        // if entry != -1 then spliceEntry != -1
        // If entry has a treeStore.right child then entry's successor can't have a treeStore.left child  
        int spliceChild = (store.left[spliceEntry] != -1) ? store.left[spliceEntry] : store.right[spliceEntry];
        int spliceParent = store.parent[spliceEntry];
        if (spliceChild != -1) {
            store.parent[spliceChild] = spliceParent;
        }
        if (spliceParent == -1) {
            this.root = spliceChild;
        } else if (spliceEntry == store.left[spliceParent]) {
            store.left[spliceParent] = spliceChild;
        } else {
            store.right[spliceParent] = spliceChild;
        }
        int spliceColor = this.store.color[spliceEntry];
        if (spliceEntry != entry) {
            // Replace entry to remove in the tree with spliceEntry
            this.store.color[spliceEntry] = this.store.color[entry];

            int right = this.store.right[entry];
            this.store.right[spliceEntry] = right;
            if (right != -1) {
                store.parent[right] = spliceEntry;
            }
            int left = this.store.left[entry];
            this.store.left[spliceEntry] = left;
            if (left != -1) {
                store.parent[left] = spliceEntry;
            }
            int parent = this.store.parent[entry];
            this.store.parent[spliceEntry] = parent;
            if (parent == -1) {
                this.root = spliceEntry;
            } else if (entry == this.store.left[parent]) {
                this.store.left[parent] = spliceEntry;
            } else {
                this.store.right[parent] = spliceEntry;
            }

            if (spliceParent == entry) {
                spliceParent = spliceEntry;
            }
        }
        if (spliceColor == BLACK) {
            rebalanceRemove(spliceParent, spliceChild);
        }

        store.removeEntry(entry);
        this.size--;

        return next;
    }

    //   x            y
    //  / \          / \
    // a   y   -->  x   c
    //    / \      / \
    //   b   c    a   b
    private int rotateLeft(int x) {
        int y = store.right[x];
        store.right[x] = store.left[y];
        if (store.left[y] != -1) {
            store.parent[store.left[y]] = x;
        }
        store.parent[y] = store.parent[x];
        if (store.parent[x] == -1) {
            this.root = y;
        } else if (x == store.left[store.parent[x]]) {
            store.left[store.parent[x]] = y;
        } else {
            store.right[store.parent[x]] = y;
        }
        store.left[y] = x;
        store.parent[x] = y;
        return y;
    }

    //     x           y
    //    / \         / \
    //   y   c  -->  a   x
    //  / \             / \
    // a   b           b   c
    private int rotateRight(int x) {
        int y = store.left[x];
        store.left[x] = store.right[y];
        if (store.right[y] != -1) {
            store.parent[store.right[y]] = x;
        }
        store.parent[y] = store.parent[x];
        if (store.parent[x] == -1) {
            this.root = y;
        } else if (x == store.right[store.parent[x]]) {
            store.right[store.parent[x]] = y;
        } else {
            store.left[store.parent[x]] = y;
        }
        store.right[y] = x;
        store.parent[x] = y;
        return y;
    }

    private void rebalanceRemove(int entry) {
        store.color[entry] = RED;
        int parent = this.store.parent[entry];
        while (entry != root && store.color[parent] == RED) {
            int grandParent = this.store.parent[parent];
            if (grandParent == -1) {
                // Just need to make the parent/root black
                break;
            } else if (parent == store.left[grandParent]) {
                int uncle = store.right[grandParent];
                if (uncle != -1 && store.color[uncle] == RED) {
                    store.color[parent] = BLACK;
                    store.color[uncle] = BLACK;
                    store.color[grandParent] = RED;
                    entry = grandParent;
                    parent = this.store.parent[entry];
                } else {
                    if (entry == store.right[parent]) {
                        // operation swaps entry and parent while grandParent stays the same  
                        entry = parent;
                        parent = rotateLeft(parent);
                    }
                    store.color[parent] = BLACK; // Break the loop
                    store.color[grandParent] = RED;
                    // operation does not change parent
                    rotateRight(grandParent);
                }
            } else {
                int uncle = store.left[grandParent];
                if (uncle != -1 && store.color[uncle] == RED) {
                    store.color[parent] = BLACK;
                    store.color[uncle] = BLACK;
                    store.color[grandParent] = RED;
                    entry = grandParent;
                    parent = this.store.parent[entry];
                } else {
                    if (entry == store.left[parent]) {
                        // operation swaps entry and parent while grandParent stays the same  
                        entry = parent;
                        parent = rotateRight(parent);
                    }
                    store.color[parent] = BLACK; // Break the loop
                    store.color[grandParent] = RED;
                    // operation does not change parent
                    rotateLeft(grandParent);
                }
            }
        }
        store.color[root] = BLACK;
    }

    private void rebalanceRemove(int entry, int child) {
        // It is possible that child == -1
        // parent == -1 only when child == root
        while (child != this.root && (child == -1 || store.color[child] == BLACK)) {
            // parent != -1 since child != root
            if (child == store.left[entry]) {
                int sibling = store.right[entry];
                if (sibling == -1) {
                    child = entry;
                    entry = this.store.parent[child];
                } else {
                    if (store.color[sibling] == RED) {
                        store.color[sibling] = BLACK;  // sibling becomes grandParent after rotation
                        store.color[entry] = RED;
                        rotateLeft(entry);
                        sibling = store.right[entry];
                        // sibling != -1
                    }
                    // treeStore.color[sibling] == BLACK
                    int leftNephew = store.left[sibling];
                    int rightNephew = store.right[sibling];
                    if ((leftNephew == -1 || store.color[leftNephew] == BLACK) &&
                            (rightNephew == -1 || store.color[rightNephew] == BLACK)) {
                        store.color[sibling] = RED;
                        child = entry;
                        entry = this.store.parent[child];
                    } else {
                        if (rightNephew == -1 || store.color[rightNephew] == BLACK) {
                            // treeStore.color[leftNephew] == RED
                            store.color[leftNephew] = BLACK; // leftNephew becomes sibling after rotation
                            store.color[sibling] = RED; // sibling becomes rightNephew after rotation
                            rightNephew = sibling;
                            sibling = rotateRight(sibling);
                        }
                        // treeStore.color[rightNephew] == RED
                        store.color[sibling] = store.color[entry];
                        store.color[entry] = BLACK;
                        store.color[rightNephew] = BLACK;
                        rotateLeft(entry);
                        child = this.root; // End the loop
                    }
                }
            } else {
                int sibling = store.left[entry];
                if (sibling == -1) {
                    child = entry;
                    entry = this.store.parent[child];
                } else {
                    if (sibling != -1 && store.color[sibling] == RED) {
                        store.color[sibling] = BLACK; // sibling becomes grandParent after rotation
                        store.color[entry] = RED;
                        rotateRight(entry);
                        sibling = store.left[entry];
                        // sibling != -1
                    }
                    // treeStore.color[sibling] == BLACK
                    int leftNephew = store.left[sibling];
                    int rightNephew = store.right[sibling];
                    if ((leftNephew == -1 || store.color[leftNephew] == BLACK) &&
                            (rightNephew == -1 || store.color[rightNephew] == BLACK)) {
                        store.color[sibling] = RED;
                        child = entry;
                        entry = this.store.parent[child];
                    } else {
                        if (leftNephew == -1 || store.color[leftNephew] == BLACK) {
                            // treeStore.color[rightNephew] == RED
                            store.color[rightNephew] = BLACK;
                            store.color[sibling] = RED;
                            leftNephew = sibling;
                            sibling = rotateLeft(sibling);
                        }
                        // treeStore.color[leftNephew] == RED
                        store.color[sibling] = store.color[entry];
                        store.color[entry] = BLACK;
                        store.color[leftNephew] = BLACK;
                        rotateRight(entry);
                        child = this.root;  // End the loop
                    }
                }
            }
        }
        if (child != -1) {
            store.color[child] = BLACK;
        }
    }

    public ByteStringBuilder toString(ByteStringBuilder sb) {
        for (int entry = firstEntry(); entry != -1; entry = nextEntry(entry)) {
            sb.append("[entry=").append(entry).append("]");
            sb.append("[key=").append(store.key[entry]).append("]");
            sb.append("[parent=").append(store.parent[entry]).append("]");
            sb.append("[left=").append(store.left[entry]).append("]");
            sb.append("[right=").append(store.right[entry]).append("]");
            sb.append("\n");
        }
        return sb;
    }

    public EntryIterator entryIterator(EntryIterator entryIterator) {
        entryIterator.init(this);
        return entryIterator;
    }

    public static class EntryIterator {
        private _KeyName_TreeSet rbTree;
        private int entry;
        private int nextEntry;

        /** Initialize the iterator at the beginning of the collection. */
        public void init(_KeyName_TreeSet rbTree) {
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
