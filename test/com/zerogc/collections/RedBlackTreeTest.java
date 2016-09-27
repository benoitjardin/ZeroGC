package com.zerogc.collections;

import junit.framework.TestCase;

import com.zerogc.collections.IntTreeSet;


/**
 * @author Benoit Jardin
 */

public class RedBlackTreeTest extends TestCase {
	IntTreeSet intRbTree = new IntTreeSet();
	IntTreeSet.EntryIterator entryIterator = new IntTreeSet.EntryIterator();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	@Override
	protected void tearDown() throws Exception {
		// TODO Auto-generated method stub
		super.tearDown();
	}
		
	public void testInsert() {
		for (int i=0; i<10; ++i) {
			intRbTree.insert(9-i);
		}
		assertEquals(10, intRbTree.size());
		
		// Test iterator and sorting
		IntTreeSet.EntryIterator iterator = new IntTreeSet.EntryIterator();
		intRbTree.entryIterator(iterator);
		int prev = -1;
		while (iterator.hasNext()) {
			int entry = iterator.nextEntry();
			assert(intRbTree.getKey(entry) > prev);
			prev = intRbTree.getKey(entry);
		}

		// Test remove
		for (int i=0; i<10; ++i) {
			int entry = intRbTree.find(i);
			assertTrue(entry != -1);
			intRbTree.removeEntry(entry);
		}
		assertEquals(0, intRbTree.size());
		assertEquals(-1, intRbTree.firstEntry());
		assertEquals(-1, intRbTree.lastEntry());
	}

	public void testInsertMulti() {
		for (int i=0; i<10; ++i) {
			intRbTree.insert(i);
		}
		assertEquals(10, intRbTree.size());
		
		intRbTree.insertMulti(5);
		intRbTree.insertMulti(5);
		intRbTree.insertMulti(5);
		assertEquals(13, intRbTree.size());
		
		assertEquals(10, intRbTree.nextEntry(5));
		assertEquals(11, intRbTree.nextEntry(10));
		assertEquals(12, intRbTree.nextEntry(11));
		assertEquals(6, intRbTree.nextEntry(12));
				
		assertEquals(12, intRbTree.prevEntry(6));
		assertEquals(11, intRbTree.prevEntry(12));
		assertEquals(10, intRbTree.prevEntry(11));
		assertEquals(5, intRbTree.prevEntry(10));
		
		for (int i=0; i<10; ++i) {
			int entry = intRbTree.find(i);
			assertTrue(entry != -1);
			intRbTree.removeEntry(entry);
		}
		
		assertEquals(3, intRbTree.size());
		int entry = intRbTree.findFirst(5);
		assertEquals(5, entry);
		assertTrue(entry != -1);
		intRbTree.removeEntry(entry);
		entry = intRbTree.find(5);
		assertTrue(entry != -1);
		intRbTree.removeEntry(entry);
		entry = intRbTree.find(5);
		assertTrue(entry != -1);
		intRbTree.removeEntry(entry);
		
		assertEquals(0, intRbTree.size());
		assertEquals(-1, intRbTree.firstEntry());
		assertEquals(-1, intRbTree.lastEntry());
	}

	public void testIterator() {
		for (int i=0; i<10; ++i) {
			intRbTree.insert(i);
		}
		assertEquals(10, intRbTree.size());

		entryIterator = intRbTree.entryIterator(entryIterator);
		while (entryIterator.hasNext()) {
			int entry = entryIterator.nextEntry();
			assertTrue(entry != -1);
			intRbTree.removeEntry(entry);
		}
		assertEquals(0, intRbTree.size());
		assertEquals(-1, intRbTree.firstEntry());
		assertEquals(-1, intRbTree.lastEntry());
	}
	
	public void testCeilingFloor() {
		for (int i=0; i<10; ++i) {
			intRbTree.insert(i*2);
		}
		assertEquals(10, intRbTree.size());
		assertEquals(2, intRbTree.find(4));
		assertEquals(-1, intRbTree.find(5));
		assertEquals(3, intRbTree.find(6));
		assertEquals(-1, intRbTree.find(7));
		assertEquals(4, intRbTree.find(8));

		assertEquals(2, intRbTree.ceiling(4));
		assertEquals(3, intRbTree.ceiling(5));
		assertEquals(3, intRbTree.ceiling(6));
		
		assertEquals(2, intRbTree.floor(5));
		assertEquals(3, intRbTree.floor(6));
		assertEquals(3, intRbTree.floor(7));
	}
}
