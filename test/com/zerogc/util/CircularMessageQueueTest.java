package com.zerogc.util;

import junit.framework.TestCase;

public class CircularMessageQueueTest extends TestCase {
	
	private CircularMessageQueue cmq = new CircularMessageQueue(32);
	private byte[] ten = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
	
	public void testWrap() {
		assertEquals(0, cmq.offsetHead);
		assertEquals(0, cmq.offsetTail);
		cmq.send(ten,  0, 10);
		assertEquals(12, cmq.offsetHead);
		assertEquals(0, cmq.offsetTail);
		cmq.send(ten,  0, 10);
		assertEquals(24, cmq.offsetHead);
		assertEquals(0, cmq.offsetTail);
		cmq.send(ten,  0, 10);
		assertEquals(4, cmq.offsetHead);
		assertEquals(12, cmq.offsetTail);
		cmq.send(ten,  0, 10);
		assertEquals(16, cmq.offsetHead);
		assertEquals(24, cmq.offsetTail);
		cmq.send(ten,  0, 10);
		assertEquals(28, cmq.offsetHead);
		assertEquals(4, cmq.offsetTail);
		cmq.send(ten,  0, 10);
		assertEquals(8, cmq.offsetHead);
		assertEquals(16, cmq.offsetTail);
		cmq.send(ten,  0, 10);
		assertEquals(20, cmq.offsetHead);
		assertEquals(28, cmq.offsetTail);
		cmq.send(ten,  0, 10);
		assertEquals(0, cmq.offsetHead);
		assertEquals(8, cmq.offsetTail);
		cmq.send(ten,  0, 10);
		assertEquals(12, cmq.offsetHead);
		assertEquals(20, cmq.offsetTail);
		cmq.send(ten,  0, 10);
		assertEquals(24, cmq.offsetHead);
		assertEquals(0, cmq.offsetTail);
	}
	
	public void testRead() {
		byte[] buffer = new byte[cmq.getCapacity()];
		assertEquals(0, cmq.offsetHead);
		assertEquals(0, cmq.offsetTail);
		cmq.send(ten, 0, 10);
		assertEquals(12, cmq.offsetHead);
		assertEquals(0, cmq.offsetTail);
		cmq.send(ten, 0, 10);
		assertEquals(24, cmq.offsetHead);
		assertEquals(0, cmq.offsetTail);

		long seqno = cmq.seqnoTail;
		int len = cmq.receive(buffer, 0);
		assertEquals(24, cmq.offsetHead);
		assertEquals(12, cmq.offsetTail);
		assertEquals(seqno, 0);
		
		seqno = cmq.seqnoTail;
		len = cmq.receive(buffer, 0);
		assertEquals(24, cmq.offsetHead);
		assertEquals(24, cmq.offsetTail);
		assertEquals(seqno, 1);
	}
}
