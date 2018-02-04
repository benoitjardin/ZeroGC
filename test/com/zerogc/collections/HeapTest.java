package com.zerogc.collections;

import junit.framework.TestCase;

public class HeapTest extends TestCase {
    private IntHeap intHeap = new IntHeap();
    
    public void testInsertRemove() {
        intHeap.insert(6);
        assertEquals(6, intHeap.getKey(intHeap.firstEntry()));
        intHeap.insert(5);
        assertEquals(5, intHeap.getKey(intHeap.firstEntry()));
        intHeap.insert(4);
        assertEquals(4, intHeap.getKey(intHeap.firstEntry()));
        intHeap.insert(7);
        assertEquals(4, intHeap.getKey(intHeap.firstEntry()));
        intHeap.insert(8);
        assertEquals(4, intHeap.getKey(intHeap.firstEntry()));
        intHeap.insert(9);
        assertEquals(4, intHeap.getKey(intHeap.firstEntry()));

        assertEquals(6, intHeap.size());
        
        intHeap.removeEntry(intHeap.firstEntry());
        assertEquals(5, intHeap.getKey(intHeap.firstEntry()));
        intHeap.removeEntry(intHeap.firstEntry());
        assertEquals(6, intHeap.getKey(intHeap.firstEntry()));
        intHeap.removeEntry(intHeap.firstEntry());
        assertEquals(7, intHeap.getKey(intHeap.firstEntry()));
        intHeap.removeEntry(intHeap.firstEntry());
        assertEquals(8, intHeap.getKey(intHeap.firstEntry()));
        intHeap.removeEntry(intHeap.firstEntry());
        assertEquals(9, intHeap.getKey(intHeap.firstEntry()));
        intHeap.removeEntry(intHeap.firstEntry());
        assertEquals(0, intHeap.size());
    }
    
    public void testInsertRemoveMulti() {
        intHeap.insert(6);
        assertEquals(6, intHeap.getKey(intHeap.firstEntry()));
        intHeap.insert(5);
        assertEquals(5, intHeap.getKey(intHeap.firstEntry()));
        intHeap.insert(4);
        assertEquals(4, intHeap.getKey(intHeap.firstEntry()));
        intHeap.insert(7);
        assertEquals(4, intHeap.getKey(intHeap.firstEntry()));
        intHeap.insert(8);
        assertEquals(4, intHeap.getKey(intHeap.firstEntry()));
        intHeap.insert(9);
        assertEquals(4, intHeap.getKey(intHeap.firstEntry()));

        assertEquals(6, intHeap.size());

        intHeap.insert(7);
        assertEquals(4, intHeap.getKey(intHeap.firstEntry()));
        intHeap.insert(7);
        assertEquals(4, intHeap.getKey(intHeap.firstEntry()));
        intHeap.insert(7);
        assertEquals(4, intHeap.getKey(intHeap.firstEntry()));
        intHeap.insert(7);
        assertEquals(4, intHeap.getKey(intHeap.firstEntry()));

        assertEquals(10, intHeap.size());

        intHeap.removeEntry(intHeap.firstEntry());
        assertEquals(5, intHeap.getKey(intHeap.firstEntry()));
        intHeap.removeEntry(intHeap.firstEntry());
        assertEquals(6, intHeap.getKey(intHeap.firstEntry()));
        intHeap.removeEntry(intHeap.firstEntry());
        assertEquals(7, intHeap.getKey(intHeap.firstEntry()));
        intHeap.removeEntry(intHeap.firstEntry());
        
        assertEquals(7, intHeap.getKey(intHeap.firstEntry()));
        intHeap.removeEntry(intHeap.firstEntry());
        assertEquals(7, intHeap.getKey(intHeap.firstEntry()));
        intHeap.removeEntry(intHeap.firstEntry());
        assertEquals(7, intHeap.getKey(intHeap.firstEntry()));
        intHeap.removeEntry(intHeap.firstEntry());
        assertEquals(7, intHeap.getKey(intHeap.firstEntry()));
        intHeap.removeEntry(intHeap.firstEntry());
        
        
        assertEquals(8, intHeap.getKey(intHeap.firstEntry()));
        intHeap.removeEntry(intHeap.firstEntry());
        assertEquals(9, intHeap.getKey(intHeap.firstEntry()));
        intHeap.removeEntry(intHeap.firstEntry());
        assertEquals(0, intHeap.size());
    }
    
/*
    public void testFind() {
        intHeap.clear();
        
        for (int i=0; i<15; i++) {
            intHeap.insert(10*i);
        }
        
        assertEquals(intHeap.find(5), -1);
        assertEquals(intHeap.find(80), 8);
        assertEquals(intHeap.find(50), 5);
        assertEquals(intHeap.find(150), -1);
        assertEquals(intHeap.find(-50), -1);
    }
*/
}
