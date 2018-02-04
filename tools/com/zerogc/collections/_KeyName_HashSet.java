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

public class _KeyName_HashSet {
    public static final int INITIAL_CAPACITY = 16;
    public static final int MAXIMUM_CAPACITY = 1 << 30;
    public static final float LOAD_FACTOR = 0.75f;
    public static final float GROWTH_FACTOR = 2.0f;

    protected final Logger log;

    private float growthFactor;
    private float loadFactor;
    private int size = 0;
    private int freeEntry = -1;
    private int highMark = 0;

    private int[] next;
    private int[] bucket;
//#if "_KeyType_" != "ByteSlice"
    private _KeyType_[] key;
//#else
    public static final int INITIAL_KEY_BUFFER_CAPACITY = 1024;
    private byte[] keyBuffer;
    private int keyBufferPosition = 0;
    private int[] keyOffset;
    private int[] keyLength;
//#endif
    protected Comparator._KeyName_Comparator comparator = new Comparator._KeyName_Comparator(); 

    public _KeyName_HashSet() {
        this(_KeyName_HashSet.class.getSimpleName(), INITIAL_CAPACITY, GROWTH_FACTOR, LOAD_FACTOR);
    }
    public _KeyName_HashSet(String name) {
        this(name, INITIAL_CAPACITY, GROWTH_FACTOR, LOAD_FACTOR);
    }

    public _KeyName_HashSet(String name, int initialCapacity) {
        this(name, initialCapacity, GROWTH_FACTOR, LOAD_FACTOR);
    }

    public _KeyName_HashSet(String name, int initialCapacity, float growthFactor) {
        this(name, initialCapacity, growthFactor, LOAD_FACTOR);
    }

    public _KeyName_HashSet(String name, int initialCapacity, float growthFactor, float loadFactor) {
        this.log = LogManager.getLogger(name);

        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY) {
            initialCapacity = MAXIMUM_CAPACITY;
        }
        if (growthFactor <= 0 || Float.isNaN(growthFactor)) {
            throw new IllegalArgumentException("Illegal GrowthFactor: " + growthFactor);
        }
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                                               loadFactor);
        this.growthFactor = growthFactor;
        this.loadFactor = loadFactor;
        grow(0, initialCapacity);
//#if "_KeyType_" == "ByteSlice"
        growKeyBuffer(0, INITIAL_KEY_BUFFER_CAPACITY);
//#endif
    }

    public void setComparator(Comparator._KeyName_Comparator comparator) {
        if (!this.isEmpty()) {
            throw new IllegalStateException("Collection not empty!");
        }
        this.comparator = comparator;
    }

    public int highMark() {
        return this.highMark;
    }

    public int capacity() {
        return this.bucket.length;
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    protected void grow(int capacity, int newCapacity) {
        log.log(Level.WARN, log.getSB().append("Resizing to ").append(newCapacity));

        int[] oldNext = this.next;
        int[] oldBucket = this.bucket;
//#if "_KeyType_" != "ByteSlice"
        _KeyType_[] oldKey = this.key;
//#else
        int[] oldKeyOffset = this.keyOffset;
        int[] oldKeyLength = this.keyLength;
//#endif
        int adjustedCapacity = 1;
        while (adjustedCapacity < newCapacity) {
            adjustedCapacity <<= 1;
        }
        int threshold = (int)(adjustedCapacity * loadFactor);
        this.next = new int[threshold];
        this.bucket = new int[(int)(adjustedCapacity)];
//#if "_KeyType_" != "ByteSlice"
        this.key = new _KeyType_[threshold];
//#else
        this.keyOffset = new int[threshold];
        this.keyLength = new int[threshold];
//#endif

        if (capacity > 0) {
//#if "_KeyType_" != "ByteSlice"
            System.arraycopy(oldKey, 0, this.key, 0, oldKey.length);
//#else
            System.arraycopy(oldKeyOffset, 0, this.keyOffset, 0, oldKeyOffset.length);
            System.arraycopy(oldKeyLength, 0, this.keyLength, 0, oldKeyLength.length);
//#endif
        }

        for (int i=0; i < bucket.length; i++) {
            bucket[i] = -1;
        }

        if (capacity > 0) {
            // Transfer entries to the resized hashtable
            for (int i=0; i < oldBucket.length; i++) {
                for (int entry = oldBucket[i]; entry != -1; entry = oldNext[entry]) {
                    int hash;
//#if "_KeyType_" != "ByteSlice"
                    hash = comparator.hashCode(this.key[entry]);
//#else
                    hash = comparator.hashCode(this.keyBuffer, this.keyOffset[entry],this.keyLength[entry]);                    
//#endif
                    int bucket = bucketFor(hash);
                    this.next[entry] = this.bucket[bucket];
                       this.bucket[bucket] = entry;
                }
            }
        }
    }

    public void clear() {
        for (int i=0; i < bucket.length; i++) {
            bucket[i] = -1;
        }
        this.next[this.next.length-1] = -1;
        this.freeEntry = -1;
        this.highMark = 0;
        this.size = 0;
//#if "_KeyType_" == "ByteSlice"
        this.keyBufferPosition = 0;
//#endif
    }

//#if "_KeyType_" != "ByteSlice"
    public _KeyType_ getKey(int entry) {
        return this.key[entry];
    }
//#else
    public ByteSlice getKey(int entry, ByteSlice key) {
        return key.set(this.keyBuffer, this.keyOffset[entry], this.keyLength[entry]);
    }
//#endif

    private int newEntry() {
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

        this.size++;
        return entry;
    }

    private int bucketFor(int h) {
        //int bucket = hash & 0x7FFFFFFF % this.bucket.length;
        // Modulo operation is quite expensive
        // With a bucket size at a power of 2 this is 20% faster

        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        h ^= (h >>> 20) ^ (h >>> 12);
        h ^= (h >>> 7) ^ (h >>> 4);
        int bucket = h & (this.bucket.length -1);
        return bucket;
    }

    public int find(_KeyType_ key) {
        int hash = comparator.hashCode(key);
        int bucket = bucketFor(hash);
        int entry = this.bucket[bucket];
        for (; entry != -1; entry = this.next[entry]) {
//#if "_KeyType_" != "ByteSlice"
            if (comparator.equals(key, this.key[entry]))
//#else
            if (comparator.equals(key, this.keyBuffer, this.keyOffset[entry], this.keyLength[entry]))
//#endif
            {
                break;
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
        int hash = comparator.hashCode(key);
        int bucket = bucketFor(hash);
        int entry = this.bucket[bucket];
        for (; entry != -1; entry = next[entry]) {
//#if "_KeyType_" != "ByteSlice"
            if (comparator.equals(key, this.key[entry]))
//#else
            if (comparator.equals(key, this.keyBuffer, this.keyOffset[entry], this.keyLength[entry]))
//#endif
            {
                return entry;
            }
        }

        if (freeEntry == -1) {
            entry = newEntry();
            bucket = bucketFor(hash);
        } else {
            entry = newEntry();
        }
//#if "_KeyType_" != "ByteSlice"
        this.key[entry] = key;
//#else
        // Grow keyBuffer if necessary
        if (this.keyBufferPosition + key.getLength() > this.keyBuffer.length) {
            int newCapacity = (int)(this.keyBuffer.length * growthFactor);
        }
        this.keyOffset[entry] = this.keyBufferPosition;
        this.keyBufferPosition += key.getLength();
        this.keyLength[entry] = key.getLength();
//#endif
        next[entry] = this.bucket[bucket];
          this.bucket[bucket] = entry;
        return entry;
    }

//#if "_KeyType_" == "ByteSlice"
    void growKeyBuffer(int capacity, int newCapacity) {
        byte[] oldKeyArray = this.keyBuffer;
        this.keyBuffer = new byte[newCapacity];
        if (capacity > 0) {
            System.arraycopy(oldKeyArray, 0, this.keyBuffer, 0, capacity);
        }
    }
//#endif

    /**
     * Remove a key from the collection.
     * If the collection contains the key, the existing entry is returned.
     * @param key the key to remove.
     * @return the entry of the key that was removed or {@code -1} if key is not found.
     */
    public int remove(_KeyType_ key) {
        int hash = comparator.hashCode(key);
        int bucket = bucketFor(hash);
        int entry = this.bucket[bucket];
        int prevEntry = -1;
        for (; entry != -1; prevEntry = entry, entry = next[entry]) {
//#if "_KeyType_" != "ByteSlice"
            if (comparator.equals(key, this.key[entry]))
//#else
            if (comparator.equals(key, this.keyBuffer, this.keyOffset[entry], this.keyLength[entry]))
//#endif
            {
                if (prevEntry == -1) {
                    this.bucket[bucket] = next[entry];
                } else {
                    next[prevEntry] = next[entry];
                }
                next[entry] = freeEntry;
                freeEntry = entry;
                size--;
                break;
            }
        }
        return entry;
    }

    public EntryIterator entryIterator(EntryIterator entryIterator) {
        entryIterator.init(this);
        return entryIterator;
    }

    public static class EntryIterator {
        private _KeyName_HashSet hashTable;
        private int bucket;
        private int entry;
        private int prevEntry;

        private int nextBucket;
        private int nextEntry;
        private int nextPrevEntry;

        public EntryIterator() {
        }

        public void init(_KeyName_HashSet hashTable) {
            this.hashTable = hashTable;
            bucket = -1;
            entry = -1;
            prevEntry = -1;

            nextBucket = -1;
            nextEntry = -1;
            nextPrevEntry = -1;
            while (nextEntry == -1 && ++nextBucket < hashTable.bucket.length) {
                nextEntry = hashTable.bucket[nextBucket];
            }
        }

        public boolean hasNext() {
            return nextEntry != -1;
        }

        public int nextEntry() {
            bucket = nextBucket;
            entry = nextEntry;
            prevEntry = nextPrevEntry;
            nextPrevEntry = nextEntry;
            nextEntry = hashTable.next[nextEntry];
            while (nextEntry == -1 && ++nextBucket < hashTable.bucket.length) {
                nextEntry = hashTable.bucket[nextBucket];
                nextPrevEntry = -1;
            }
            return entry;
        }

        public void remove() {
            if (prevEntry == -1) {
                hashTable.bucket[bucket] = hashTable.next[entry];
            } else {
                hashTable.next[prevEntry] = hashTable.next[entry];
            }
            hashTable.next[entry] = hashTable.freeEntry;
            hashTable.freeEntry = entry;
            hashTable.size--;

            bucket = -1;
            entry = -1;
            prevEntry = -1;
        }
    }
}
