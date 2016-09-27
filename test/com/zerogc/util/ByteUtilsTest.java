package com.zerogc.util;

import junit.framework.TestCase;


/**
 * @author Benoit Jardin
 */

public class ByteUtilsTest extends TestCase {
    private byte[] buffer = new byte[1024];
    private StringBuilder sb = new StringBuilder();
    
    public void testputInt() {
        int value = Integer.MAX_VALUE;
        int len = ByteUtils.putInt(buffer, 0, value);
        
        assertEquals(0, ByteUtils.compareTo(Integer.toString(value).getBytes(), buffer, 0 , len));
        
        value = -12345;
        len = ByteUtils.putInt(buffer, 0, value);
        assertEquals(0, ByteUtils.compareTo(Integer.toString(value).getBytes(), buffer, 0 , len));

        len = ByteUtils.putIntLeftPadded(buffer, 0, value, 10);
        assertEquals(0, ByteUtils.compareTo("    -12345".getBytes(), buffer, 0 , len));

        len = ByteUtils.putIntZeroPadded(buffer, 0, value, 10);
        assertEquals(0, ByteUtils.compareTo("-000012345".getBytes(), buffer, 0 , len));

        len = ByteUtils.putIntRightPadded(buffer, 0, value, 10);
        assertEquals(0, ByteUtils.compareTo("-12345    ".getBytes(), buffer, 0 , len));

    }

    public void testputDouble() {
        double value = 12345;
        int len = ByteUtils.putDouble(buffer, 0, value);
        assertEquals(0, ByteUtils.compareTo("12345".getBytes(), buffer, 0 , len));
        
        value = 12345.6789;
        len = ByteUtils.putDouble(buffer, 0, value);
        assertEquals(0, ByteUtils.compareTo("12345.6789".getBytes(), buffer, 0 , len));

        value = 12345.00000001;
        len = ByteUtils.putDouble(buffer, 0, value);
        assertEquals(0, ByteUtils.compareTo("12345.00000001".getBytes(), buffer, 0 , len));


    }
}
