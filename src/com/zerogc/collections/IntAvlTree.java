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

import com.zerogc.core.ByteSlice;
import com.zerogc.logging.Level;
import com.zerogc.logging.LogManager;
import com.zerogc.logging.Logger;

/**
 * @author Benoit Jardin
 */

public class IntAvlTree {
    public static final int INITIAL_CAPACITY = 16;
    public static final float GROWTH_FACTOR = 2.0f;

    protected final Logger log;

    private float growthFactor = GROWTH_FACTOR;
    private int size = 0;

    protected int root = -1;
    private int freeEntry = -1;
    private int highMark = 0;

    protected int[] left;
    protected int[] right;
    protected int[] parent;
    //protected short[] height;
    protected byte[] balance;
    protected int[] key;
    protected Comparator.IntComparator comparator = new Comparator.IntComparator(); 

    public IntAvlTree() {
        this(IntAvlTree.class.getSimpleName(), INITIAL_CAPACITY, GROWTH_FACTOR);
    }
    public IntAvlTree(String name) {
        this(name, INITIAL_CAPACITY, GROWTH_FACTOR);
    }

    public IntAvlTree(String name, int initialCapacity) {
        this(name, initialCapacity, GROWTH_FACTOR);
    }

    public IntAvlTree(String name, int initialCapacity, float growthFactor) {
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
        //short[] newHeight = new short[newCapacity];
        byte[] newBalance = new byte[newCapacity];
        int[] newKey = new int[newCapacity];

        if (capacity > 0) {
            System.arraycopy(left, 0, newLeft, 0, capacity);
            System.arraycopy(right, 0, newRight, 0, capacity);
            System.arraycopy(parent, 0, newParent, 0, capacity);
            //System.arraycopy(height, 0, newHeight, 0, capacity);
            System.arraycopy(balance, 0, newBalance, 0, capacity);
            System.arraycopy(key, 0, newKey, 0, capacity);
        }

        left = newLeft;
        right = newRight;
        parent = newParent;
        //height = newHeight;
        balance = newBalance;
        key = newKey;
    }

    public void clear() {
        this.freeEntry = -1;
        this.highMark = 0;
        this.root = -1;
        this.size=0;
    }

    private int newEntry() {
        int entry = this.freeEntry;
        if (entry != -1) {
            this.freeEntry = this.parent[entry];
        } else {
            int capacity = this.parent.length;
            if (highMark >= capacity) {
                // Grow the arrays
                int newCapacity = (int) (capacity * this.growthFactor);
                grow(capacity, newCapacity);
            }
            entry = highMark++;
        }

        left[entry] = -1;
        right[entry] = -1;
        //height[entry] = 0;
        balance[entry] = 0;

        this.size++;
        return entry;
    }

    public int firstEntry() {
        int entry = root;
        if (entry != -1) {
            while (left[entry] != -1) {
                entry = left[entry];
            }
        }
        return entry;
    }

    public int prevEntry(int entry) {
        if (entry != -1) {
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
        }
        return entry;
    }

    public int lastEntry() {
        int entry = root;
        if (entry != -1) {
            while (right[entry] != -1) {
                entry = right[entry];
            }
        }
        return entry;
    }

    public int nextEntry(int entry) {
        if (entry != -1) {
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
        }
        return entry;
    }

    public int find(int key) {
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

    // Returns the entry of the greatest element in this set less than or equal to the given element, or -1 if there is no such element.
    public int floor(int key) {
        int entry = root;
        if (entry != -1) {
            while (true) {
                int cmp = comparator.compare(key, this.key[entry]);
                if (cmp < (cmp^cmp)) { // Faster than if (cmp < 0) with Sun jdk1.6
                    int left = this.left[entry];
                    if (left == -1) {
                        break;
                    }
                    entry = left;
                } else if (cmp != 0) {
                    int right = this.right[entry];
                    if (right == -1) {
                        entry = prevEntry(entry);
                        break;
                    }
                    entry = right;
                } else {
                    break;
                }
            }
        }
        return entry;
    }

    // Returns the entry of the least element in this set greater than or equal to the given element, or -1 if there is no such element.
    public int ceiling(int key) {
        int entry = root;
        if (entry != -1) {
            while (true) {
                int cmp = comparator.compare(key, this.key[entry]);
                if (cmp < (cmp^cmp)) { // Faster than if (cmp < 0) with Sun jdk1.6
                    int left = this.left[entry];
                    if (left == -1) {
                        entry = nextEntry(entry);
                        break;
                    }
                    entry = left;
                } else if (cmp != 0) {
                    int right = this.right[entry];
                    if (right == -1) {
                        break;
                    }
                    entry = right;
                } else {
                    break;
                }
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
    public int insert(int key) {
        int parent = -1;
        int entry = root;
        int cmp = 0;

        while (entry != -1) {
            assert left[entry] == -1 && right[entry] == -1 && balance[entry] >= 0 ||
                left[entry] == -1 && balance[entry] >= 0 ||
                right[entry] == -1 && balance[entry] <= 0 || left[entry] != -1 && right[entry] != -1;

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
        } else {
            if (cmp < (cmp^cmp)) { // Faster than if (cmp < 0) with Sun jdk1.6
                left[parent] = entry;
            } else {
                right[parent] = entry;
            }
            rebalanceInsert(parent, entry);
        }
        return entry;
    }

    // Same as insert except that it inserts a new node even if the key is already present
    public int insertMulti(int key) {
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
        } else {
            if (cmp < (cmp^cmp)) { // Faster than if (cmp < 0) with Sun jdk1.6
                left[parent] = entry;
            } else {
                right[parent] = entry;
            }
            rebalanceInsert(parent, entry);
        }
        return entry;
    }

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
        if (spliceEntry != entry) {
            // Replace entry to remove in the tree with spliceEntry
            this.balance[spliceEntry] = this.balance[entry];
            //this.height[spliceEntry] = this.height[entry];

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

        if (spliceParent != -1) {
            rebalanceRemove(spliceParent, spliceChild);
        }

        parent[entry] = this.freeEntry;
        this.freeEntry = entry;
        this.size--;

        return next;
    }

    /*
    private int adjustHeight(int entry) {
        short heightRight = right[entry] != -1 ? height[right[entry]] : -1;
        short heightLeft = left[entry] != -1 ? height[left[entry]] : -1;
        return height[entry] = (short)((heightRight > heightLeft ? heightRight : heightLeft) + 1); 
    }

    private void checkBalance(int entry) {
        short heightRight = right[entry] != -1 ? height[right[entry]] : -1;
        short heightLeft = left[entry] != -1 ? height[left[entry]] : -1;
        assert balance[entry] == heightRight - heightLeft;
    }
    */

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

        //balance[x] = (byte)-(--balance[y]);
        balance[x] = (byte)(balance[x] -1 -(balance[y] > 0 ? balance[y] : 0));
        balance[y] = (byte)(balance[y] -1 +(balance[x] < 0 ? balance[x] : 0));
        //adjustHeight(x);
        //adjustHeight(y);
        //checkBalance(x);
        //checkBalance(y);
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

        //balance[x] = (byte)-(++balance[y]);
        balance[x] = (byte)(balance[x] +1 -(balance[y] < 0 ? balance[y] : 0));
        balance[y] = (byte)(balance[y] +1 +(balance[x] > 0 ? balance[x] : 0));
        //adjustHeight(x);
        //adjustHeight(y);
        //checkBalance(x);
        //checkBalance(y);
        return y;
    }

    private void rebalanceInsert(int entry, int child) {
        while (entry != -1) {
            if (left[entry] == child) {
                balance[entry]--;
            } else {
                balance[entry]++;
            }
            //short oldHeight = height[entry];
            //adjustHeight(entry);
            //checkBalance(entry);
            boolean heightUnchanged = balance[entry] == 0;
            if (balance[entry] < -1) {
                if (balance[left[entry]] > 0) {
                    rotateLeft(left[entry]);
                    entry = rotateRight(entry);
                    heightUnchanged = true;
                } else {
                    heightUnchanged = balance[left[entry]] != 0;
                    entry = rotateRight(entry);
                }
            } else if (balance[entry] > 1) {
                if (balance[right[entry]] < 0) {
                    rotateRight(right[entry]);
                    entry = rotateLeft(entry);
                    heightUnchanged = true;
                } else { 
                    heightUnchanged = balance[right[entry]] != 0;
                    entry = rotateLeft(entry);
                }
            }
            if (heightUnchanged) {
                // Subtree was rebalanced
                //assert height[entry] == oldHeight;
                break;
            }
            //assert height[entry] != oldHeight;
            child = entry;
            entry = parent[entry];
        }
    }

    private void rebalanceRemove(int entry, int child) {
        while (entry != -1) {
            if (child == -1 && left[entry] == -1 && right[entry] == -1) {
                balance[entry] = 0;
            } else if (left[entry] == child) {
                balance[entry]++;
            } else {
                balance[entry]--;
            }
            //short oldHeight = height[entry];
            //adjustHeight(entry);
            //checkBalance(entry);
            boolean heightChanged = balance[entry] == 0;
            if (balance[entry] < -1) {
                if (balance[left[entry]] > 0) {
                    rotateLeft(left[entry]);
                    entry = rotateRight(entry);
                    heightChanged = true;
                } else {
                    heightChanged = balance[left[entry]] != 0;
                    entry = rotateRight(entry);
                }
            } else if (balance[entry] > 1) {
                if (balance[right[entry]] < 0) {
                    rotateRight(right[entry]);
                    entry = rotateLeft(entry);
                    heightChanged = true;
                } else { 
                    heightChanged = balance[right[entry]] != 0;
                    entry = rotateLeft(entry);
                }
            }
            if (!heightChanged) {
                //assert height[entry] == oldHeight;
                break;
            }
            //assert height[entry] != oldHeight;
            child = entry;
            entry = parent[entry];
        }
    }

    public EntryIterator entryIterator(EntryIterator entryIterator) {
        entryIterator.init(this);
        return entryIterator;
    }

    public static class EntryIterator {
        private IntAvlTree avlTree;
        private int entry;
        private int nextEntry;

        public EntryIterator() {
        }

        public void init(IntAvlTree avlTree) {
            this.avlTree = avlTree;
            entry = -1;
            nextEntry = avlTree.firstEntry();
        }

        public boolean hasNext() {
            return nextEntry != -1;
        }

        public int nextEntry() {
            entry = nextEntry;
            nextEntry = avlTree.nextEntry(nextEntry);
            return entry;
        }

        public void remove() {
            avlTree.removeEntry(entry);
            entry = -1;
        }
    }
}
