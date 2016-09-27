/*
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
package com.zerogc.util;

import java.nio.ByteBuffer;

public class ByteUtils {
	final static int DOUBLE_DECIMALS = 8;

	final static byte[] DIGITS = {
        '0' , '1' , '2' , '3' , '4' , '5' ,
        '6' , '7' , '8' , '9' , 'a' , 'b' ,
        'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
        'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
        'o' , 'p' , 'q' , 'r' , 's' , 't' ,
        'u' , 'v' , 'w' , 'x' , 'y' , 'z'
    };
    
    final static byte [] DIGIT_TENS = {
        '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
        '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
        '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
        '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
        '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
        '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
        '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
        '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
        '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
        '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
        } ;

    final static byte [] DIGIT_ONES = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        } ;


    final static int [] INT_SIZE = {
    	9,
    	99,
    	999,
    	9999,
    	99999,
    	999999,
    	9999999,
    	99999999,
    	999999999,
    	Integer.MAX_VALUE };

    final static long [] LONG_SIZE = {
    	9L,
    	99L,
    	999L,
    	9999L,
    	99999L,
    	999999L,
    	9999999L,
        99999999L,
        999999999L,
        9999999999L,
        99999999999L,
        999999999999L,
        9999999999999L,
        99999999999999L,
        999999999999999L,
        9999999999999999L,
        99999999999999999L,
        999999999999999999L,
        Long.MAX_VALUE };

    final static long [] POW10 = {
    	1L,
    	10L,
    	100L,
    	1000L,
    	10000L,
    	100000L,
    	1000000L,
    	10000000L,
    	100000000L,
    	1000000000L,
    	10000000000L,
    	100000000000L,
    	1000000000000L,
    	10000000000000L,
    	100000000000000L,
    	1000000000000000L,
    	10000000000000000L,
    	100000000000000000L,
    	1000000000000000000L,
    };
    
    final static double [] NEGPOW10 = {
    	1,
    	1e-1,
    	1e-2,
    	1e-3,
    	1e-4,
    	1e-5,
    	1e-6,
    	1e-7,
    	1e-8,
    	1e-9,
    	1e-10,
    	1e-11,
    	1e-12,
    	1e-13,
    	1e-14,
    	1e-15,
    	1e-16,
    	1e-17,
    	1e-18,
    	1e-19,
    	1e-20,    	
    };

    public static byte toLower(byte c) {
    	return (c < 'A' || c > 'Z') ? c : (byte)(c + ('a' - 'A'));
    }

    public static byte toUpper(byte c) {
    	return (c < 'a' || c > 'z') ? c : (byte)(c - ('a' - 'A'));
    }

    public static int putString(ByteBuffer buffer, String str) {
    	int i=0;
    	for (; i < str.length(); i++) {
    		buffer.put((byte)str.charAt(i));
    	}
    	return i;
    }

    public static int putString(byte[] buffer, int offset, String str) {
    	int i=0;
    	for (; i < str.length(); i++) {
    		buffer[offset++] = (byte)str.charAt(i);
    	}
    	return i;
    }

    public static int putBuffer(byte[] dest, int destOffset, int destLength, ByteBuffer buffer, int offset, int len) {
    	for (int i=offset; i < offset+len; i++) {
    		dest[destOffset++] = buffer.get(i);
    	}
    	return len;
    }
    
	public static int hashCode(byte[] buffer, int offset, int length) {
        int result = 1;
        for (int i=offset,end=offset+length; i < end; i++) {
            result = 31 * result + buffer[i];
        }
        return result;
	}

    public static int compareTo(byte[] left, byte[] right) {
        return compareTo(left, 0, left.length, right, 0, right.length);
    }

    public static int compareTo(byte[] left,
            byte[] right, int rightOffset, int rightLength) {
        return compareTo(left, 0, left.length, right, rightOffset, rightLength);
    }
    
    public static int compareTo(byte[] left, int leftOffset, int leftLength,
            byte[] right, int rightOffset, int rightLength) {
        while (leftLength > 0 && rightLength> 0) {
            byte rightByte = right[rightOffset++];
            byte leftByte = left[leftOffset++];
            if (leftByte != rightByte) {
                return leftByte < rightByte ? -1 : 1;
            }
            leftLength--;
            rightLength--;
        }
        if (leftLength == rightLength) {
            return 0;
        } else {
            return leftLength < rightLength ? -1 : 1;
        }
    }
    
    public static int compareTo(String left, ByteBuffer right, int rightOffset, int rightLength) {
    	int leftOffset = 0;
    	int leftLength = left.length();
        while (leftLength > 0 && rightLength> 0) {
            byte rightByte = right.get(rightOffset++);
            byte leftByte = (byte)left.charAt(leftOffset++);
            if (leftByte != rightByte) {
                return leftByte < rightByte ? -1 : 1;
            }
            leftLength--;
            rightLength--;
        }
        if (leftLength == rightLength) {
            return 0;
        } else {
            return leftLength < rightLength ? -1 : 1;
        }
    }

    public static int compareTo(String left, ByteBuffer right) {
    	return compareTo(left, right, right.position(), right.remaining());
    }
    
    /** @return the number of bytes copied */
    public static int copy(byte[] src, int srcPos, ByteBuffer dest, int destPos, int length) {
    	for (int i=destPos,j=srcPos,destEnd=destPos+length; i<destEnd; i++, j++) {
    		dest.put(i, src[j]);
    	}
        return length;
    }

    /** @return the number of bytes copied */
    public static int copySpacePadded(byte[] src, int srcPos, int srcLength,
    		byte[] dest, int destPos, int destLength) {
        if (srcLength > destLength) {
        	System.arraycopy(src, srcPos, dest, destPos, destLength);
        } else {
        	System.arraycopy(src, srcPos, dest, destPos, srcLength);
        	for (int i=destPos+srcLength; i<destPos+destLength; i++) {
        		dest[i] = (byte)' ';
        	}
        }
        return destLength;
    }

    /** @return the number of bytes copied */
    public static int copySpacePadded(byte[] src, int srcPos, int srcLength,
    		ByteBuffer dest, int destPos, int destLength) {
        if (srcLength > destLength) {
        	for (int i=destPos,j=srcPos,destEnd=destPos+destLength; i<destEnd; i++, j++) {
        		dest.put(i, src[j]);
        	}
        } else {
        	for (int i=destPos,j=srcPos,srcEnd=srcPos+srcLength; j<srcEnd; i++, j++) {
        		dest.put(i, src[j]);
        	}
        	for (int i=destPos+srcLength,destEnd=destPos+destLength; i<destEnd; i++) {
        		dest.put(i, (byte)' ');
        	}
        }
        return destLength;
    }

    public static int indexOf(byte[] buffer, int offset, int length, byte c) {
    	for (int i=offset,endPos=i+length; i < endPos; i++) {
    		if (buffer[i] == c) {
    			return i;
    		}
    	}
    	return -1;
    }

    public static int indexOf(ByteBuffer buffer, int offset, int length, byte c) {
    	for (int i=offset,endPos=i+length; i < endPos; i++) {
    		if (buffer.get(i) == c) {
    			return i;
    		}
    	}
    	return -1;
    }

    public static int lastIndexOf(byte[] buffer, int offset, int length, byte c) {
    	for (int endPos=offset,i = endPos+length; i > endPos;) {
    		if (buffer[--i] == c) {
    			return i-offset;
    		}
    	}
    	return -1;
    }

    public static int lastIndexOf(ByteBuffer buffer, int offset, int length, byte c) {
    	for (int endPos=offset,i=endPos+length; i > endPos;) {
    		if (buffer.get(--i) == c) {
    			return i-offset;
    		}
    	}
    	return -1;
    }

    public static int getTrimmedLength(byte[] buffer, int offset, int length, byte trim) {
    	for (int i = offset + length; i > offset;) {
    		if (buffer[--i] != trim) {
    			return i-offset+1;
    		}
    	}
    	return 0;
    }

    public static int getTrimmedLength(ByteBuffer buffer, int offset, int length, byte trim) {
    	for (int endPos=offset,i=endPos+length; i > endPos;) {
    		if (buffer.get(--i) != trim) {
    			return i-endPos+1;
    		}
    	}
    	return 0;
    }

    public static int getSpaceTrimmedLength(byte[] buffer, int offset, int length) {
    	return getTrimmedLength(buffer, offset, length, (byte)' ');
    }

    public static int getSpaceTrimmedLength(ByteBuffer buffer, int offset, int length) {
    	return getTrimmedLength(buffer, offset, length, (byte)' ');
    }

    /** @return the number of bytes copied */
    public static int copyTrimmed(byte[] src, int srcPos, int srcLength,
    		byte[] dest, int destPos, byte trim) {
    	int trimmedLength = getTrimmedLength(src, srcPos, srcLength, trim);
       	System.arraycopy(src, srcPos, dest, destPos, trimmedLength);
        return trimmedLength;
    }

    /** @return the number of bytes copied */
    public static int copyTrimmed(ByteBuffer src, int srcPos, int srcLength,
    		byte[] dest, int destPos, byte trim) {
    	int trimmedLength = getTrimmedLength(src, srcPos, srcLength, trim);
    	for (int i=destPos,j=srcPos,endPos=destPos+trimmedLength; i<trimmedLength; i++,j++) {
    		dest[i] = src.get(j);
    	}
        return trimmedLength;
    }

    /** @return the number of bytes copied */
    public static int copySpaceTrimmed(byte[] src, int srcPos, int srcLength,
    		byte[] dest, int destPos) {
    	return copyTrimmed(src, srcPos, srcLength, dest, destPos, (byte)' ');
    }
    
    /** @return the number of bytes copied */
    public static int copySpaceTrimmed(ByteBuffer src, int srcPos, int srcLength,
    		byte[] dest, int destPos) {
    	return copyTrimmed(src, srcPos, srcLength, dest, destPos, (byte)' ');
    }

    /** @return the number of bytes copied */
    public static int fill(byte[] buffer, int offset, int length, byte b) {
    	for (int i=offset,end=offset+length; i<end; i++) {
    		buffer[i] = b;
    	}
    	return length;
    }
    
    // Requires positive x
    static int stringSize(int x) {
    	for (int i=0; ; i++)
    		if (x <= INT_SIZE[i])
    			return i+1;
    }
    
    static int numDigits(long x) {
    	for (int i=0; ; i++)
    		if (x <= LONG_SIZE[i])
    			return i+1;
    }
    
    public static int parseInt(byte[] buffer, int offset, int length)
    {
    	int end = offset+length;
    	boolean negative = false;
    	int result = 0;
    	// Process sign and skip leading blanks and 0
    	while (offset < end) {
    		byte c = buffer[offset];
    		switch (c) {
    		case '+':
    		case ' ':
    		case '0':
    			offset++;
    			continue;
    		case '-':
    			offset++;
    			negative = true;
    			break;
    		}
			break;
    	}
    	while (offset < end) {
    		byte c = buffer[offset++];
    		result = result*10 + c - '0';
    	}
    	if (negative) {
    		result = -result;
    	}
    	return result;
    }

    public static int parseInt(ByteBuffer buffer, int offset, int length)
    {
    	int end = offset + length;
    	boolean negative = false;
    	int result = 0;
    	// Process sign and skip leading blanks and 0
    	while (offset < end) {
    		byte c = buffer.get(offset);
    		switch (c) {
    		case '+':
    		case ' ':
    		case '0':
    			offset++;
    			continue;
    		case '-':
    			offset++;
    			negative = true;
    			break;
    		}
			break;
    	}
    	while (offset < end) {
    		byte c = buffer.get(offset++);
    		result = result*10 + c - '0';
    	}
    	if (negative) {
    		result = -result;
    	}
    	return result;
    }

    public static int parseDoubleAsInt(byte[] buffer, int offset, int length, int impliedDecimals)
    {
    	int end = offset+length;
    	boolean negative = false;
    	int result = 0;
    	int factor = (int)POW10[impliedDecimals];
    	// Process sign and skip leading blanks and 0
    	while (offset < end) {
    		byte c = buffer[offset];
    		switch (c) {
    		case '+':
    		case ' ':
    		case '0':
    			offset++;
    			continue;
    		case '-':
    			offset++;
    			negative = true;
    			break;
    		}
			break;
    	}
    	while (offset < end) {
    		byte c = buffer[offset++];
    		switch (c) {
    		case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
    			result = result*10 + c - '0';
    			break;
    		case '.':
    			int decimals = end-offset;
    			if (decimals > impliedDecimals) {
    				end -= (decimals - impliedDecimals);
    				factor = 1;
    			} else {
    				factor = (int)POW10[impliedDecimals-decimals];
    			}
    			break;
    		}
    	}
    	if (negative) {
    		result = -result;
    	}
    	return result*factor;
    }

    public static int parseDoubleAsInt(ByteBuffer buffer, int offset, int length, int impliedDecimals)
    {
    	int end = offset+length;
    	boolean negative = false;
    	int result = 0;
    	int factor = (int)POW10[impliedDecimals];
    	// Process sign and skip leading blanks and 0
    	while (offset < end) {
    		byte c = buffer.get(offset);
    		switch (c) {
    		case '+':
    		case ' ':
    		case '0':
    			offset++;
    			continue;
    		case '-':
    			offset++;
    			negative = true;
    			break;
    		}
			break;
    	}
    	while (offset < end) {
    		byte c = buffer.get(offset++);
    		switch (c) {
    		case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
    			result = result*10 + c - '0';
    			break;
    		case '.':
    			int decimals = end-offset;
    			if (decimals > impliedDecimals) {
    				end -= (decimals - impliedDecimals);
    				factor = 1;
    			} else {
    				factor = (int)POW10[impliedDecimals-decimals];
    			}
    			break;
    		}
    	}
    	if (negative) {
    		result = -result;
    	}
    	return result*factor;
    }

    public static long parseLong(byte[] buffer, int offset, int length)
    {
    	int end = offset+length;
    	boolean negative = false;
    	long result = 0;
    	// Process sign and skip leading blanks and 0
    	while (offset < end) {
    		byte c = buffer[offset];
    		switch (c) {
    		case '+':
    		case ' ':
    		case '0':
    			offset++;
    			continue;
    		case '-':
    			offset++;
    			negative = true;
    			break;
    		}
			break;
    	}
    	while (offset < end) {
    		byte c = buffer[offset++];
    		result = result*10 + c - '0';
    	}
    	if (negative) {
    		result = -result;
    	}
    	return result;
    }

    public static long parseLong(ByteBuffer buffer, int offset, int length)
    {
    	int end = offset + length;
    	boolean negative = false;
    	long result = 0;
    	// Process sign and skip leading blanks and 0
    	while (offset < end) {
    		byte c = buffer.get(offset);
    		switch (c) {
    		case '+':
    		case ' ':
    		case '0':
    			offset++;
    			continue;
    		case '-':
    			offset++;
    			negative = true;
    			break;
    		}
			break;
    	}
    	while (offset < end) {
    		byte c = buffer.get(offset++);
    		result = result*10 + c - '0';
    	}
    	if (negative) {
    		result = -result;
    	}
    	return result;
    }

    public static long parseDoubleAsLong(byte[] buffer, int offset, int length, int impliedDecimals)
    {
    	int end = offset+length;
    	boolean negative = false;
    	long result = 0;
    	long factor = POW10[impliedDecimals];
    	// Process sign and skip leading blanks and 0
    	while (offset < end) {
    		byte c = buffer[offset];
    		switch (c) {
    		case '+':
    		case ' ':
    		case '0':
    			offset++;
    			continue;
    		case '-':
    			offset++;
    			negative = true;
    			break;
    		}
			break;
    	}
    	while (offset < end) {
    		byte c = buffer[offset++];
    		switch (c) {
    		case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
    			result = result*10 + c - '0';
    			break;
    		case '.':
    			int decimals = end-offset;
    			if (decimals > impliedDecimals) {
    				end -= (decimals - impliedDecimals);
    				factor = 1;
    			} else {
    				factor = POW10[impliedDecimals-decimals];
    			}
    			break;
    		}
    	}
    	if (negative) {
    		result = -result;
    	}
    	return result*factor;
    }

    public static long parseDoubleAsLong(ByteBuffer buffer, int offset, int length, int impliedDecimals)
    {
    	int end = offset+length;
    	boolean negative = false;
    	long result = 0;
    	long factor = POW10[impliedDecimals];
    	// Process sign and skip leading blanks and 0
    	while (offset < end) {
    		byte c = buffer.get(offset);
    		switch (c) {
    		case '+':
    		case ' ':
    		case '0':
    			offset++;
    			continue;
    		case '-':
    			offset++;
    			negative = true;
    			break;
    		}
			break;
    	}
    	while (offset < end) {
    		byte c = buffer.get(offset++);
    		switch (c) {
    		case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
    			result = result*10 + c - '0';
    			break;
    		case '.':
    			int decimals = end-offset;
    			if (decimals > impliedDecimals) {
    				end -= (decimals - impliedDecimals);
    				factor = 1;
    			} else {
    				factor = POW10[impliedDecimals-decimals];
    			}
    			break;
    		}
    	}
    	if (negative) {
    		result = -result;
    	}
    	return result*factor;
    }

    private static int putPosInt(byte[] buffer, int offset, int value, int numDigits) {
        int charPos = offset + numDigits;
        int q, r;

        // Generate two digits per iteration
        while (value >= 65536) {
            q = value / 100;
        // really: r = i - (q * 100);
            r = value - ((q << 6) + (q << 5) + (q << 2));
            value = q;
            buffer [--charPos] = DIGIT_ONES[r];
            buffer [--charPos] = DIGIT_TENS[r];
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i <= 65536, i);
        for (;;) {
            q = (value * 52429) >>> (16+3);
            r = value - ((q << 3) + (q << 1));  // r = i-(q*10) ...
            buffer [--charPos] = DIGITS [r];
            value = q;
            if (value == 0) break;
        }
        return numDigits;
    }
    
    public static int putInt(byte[] buffer, int offset, int value) {
		int pos = offset;
        if (value < 0) {
        	buffer[offset++] = '-';
            value = -value;
        }
        offset += putPosInt(buffer, offset, value, stringSize(value));
        return offset - pos;
    }

    public static int putIntZeroPadded(byte[] buffer, int offset, int value, int width) {
    	int pos = offset;
        if (value < 0) {
        	buffer[offset++] = '-';
            value = -value;
            width--;
        }
        int nd = numDigits(value);
    	for (int i=nd; i < width; i++) {
        	buffer[offset++] = '0';
    	}
    	offset += putPosInt(buffer, offset, value, nd);
    	return offset - pos;
    }

    public static int putIntRightPadded(byte[] buffer, int offset, int value, int width) {
    	int pos = offset;
        if (value < 0) {
        	buffer[offset++] = '-';    		
            value = -value;
            width--;
        }
        int nd = numDigits(value);
    	offset += putPosInt(buffer, offset, value, nd);
    	for (int i=nd; i < width; i++) {
        	buffer[offset++] = ' ';
    	}
    	return offset - pos;
    }
    
    public static int putIntLeftPadded(byte[] buffer, int offset, int value, int width) {
    	int pos = offset;
    	boolean negative = false;
        if (value < 0) {
        	negative = true;
            value = -value;
            width--;
        }
        int nd = numDigits(value);
    	for (int i=nd; i < width; i++) {
        	buffer[offset++] = ' ';
    	}
    	if (negative) {
        	buffer[offset++] = '-';    		
    	}
    	offset += putPosInt(buffer, offset, value, nd);
    	return offset - pos;
    }

    private static int putPosLong(byte[] buffer, int offset, long value, int numDigits) {
        int charPos = offset + numDigits;
		long q;
        int r;

        // Get 2 digits/iteration using longs until quotient fits into an int
        while (value > Integer.MAX_VALUE) {
            q = value / 100;
            // really: r = i - (q * 100);
            r = (int)(value - ((q << 6) + (q << 5) + (q << 2)));
            value = q;
            buffer[--charPos] = DIGIT_ONES[r];
            buffer[--charPos] = DIGIT_TENS[r];
        }

        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int)value;
        while (i2 >= 65536) {
            q2 = i2 / 100;
            // really: r = i2 - (q * 100);
            r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
            i2 = q2;
            buffer[--charPos] = DIGIT_ONES[r];
            buffer[--charPos] = DIGIT_TENS[r];
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i2 <= 65536, i2);
        for (;;) {
            q2 = (i2 * 52429) >>> (16+3);
            r = i2 - ((q2 << 3) + (q2 << 1));  // r = i2-(q2*10) ...
            buffer[--charPos] = DIGITS[r];
            i2 = q2;
            if (i2 == 0) break;
        }
        return numDigits;
    }
    
    private static int putPosLongTrimmed(byte[] buffer, int offset, long value, int numDigits, int trimZeros) {
        int charPos = offset + numDigits;
		long q;
        int r;

        // Get 2 digits/iteration using longs until quotient fits into an int
        while (value > Integer.MAX_VALUE) {
            q = value / 100;
            // really: r = i - (q * 100);
            r = (int)(value - ((q << 6) + (q << 5) + (q << 2)));
            value = q;
            byte b = DIGIT_ONES[r];
            if (trimZeros > 0) {
            	if (b == '0') {
	            	--charPos;
	            	--trimZeros;
	            	--numDigits;
            	} else {
            		trimZeros = 0;
                	buffer[--charPos] = b;
            	}
            } else {
            	buffer[--charPos] = b;
            }
            b = DIGIT_TENS[r];
            if (trimZeros > 0) {
            	if (b == '0') {
	            	--charPos;
	            	--trimZeros;
	            	--numDigits;
            	} else {
            		trimZeros = 0;
                	buffer[--charPos] = b;
            	}
            } else {
            	buffer[--charPos] = b;
            }
        }

        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int)value;
        while (i2 >= 65536) {
            q2 = i2 / 100;
            // really: r = i2 - (q * 100);
            r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
            i2 = q2;
            byte b = DIGIT_ONES[r];
            if (trimZeros > 0) {
            	if (b == '0') {
	            	--charPos;
	            	--trimZeros;
	            	--numDigits;
            	} else {
            		trimZeros = 0;
                	buffer[--charPos] = b;
            	}
            } else {
            	buffer[--charPos] = b;
            }
            b = DIGIT_TENS[r];
            if (trimZeros > 0) {
            	if (b == '0') {
	            	--charPos;
	            	--trimZeros;
	            	--numDigits;
            	} else {
            		trimZeros = 0;
                	buffer[--charPos] = b;
            	}
            } else {
            	buffer[--charPos] = b;
            }
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i2 <= 65536, i2);
        for (;;) {
            q2 = (i2 * 52429) >>> (16+3);
            r = i2 - ((q2 << 3) + (q2 << 1));  // r = i2-(q2*10) ...
            byte b = DIGITS[r];
            if (trimZeros > 0) {
            	if (b == '0') {
	            	--charPos;
	            	--trimZeros;
	            	--numDigits;
            	} else {
            		trimZeros = 0;
                	buffer[--charPos] = b;
            	}
            } else {
            	buffer[--charPos] = b;
            }
            i2 = q2;
            if (i2 == 0) break;
        }
        return numDigits;
    }
    
    public static int putLong(byte[] buffer, int offset, long value) {
		int pos = offset;
        if (value < 0) {
        	buffer[offset++] = '-';
            value = -value;
        }
        offset += putPosLong(buffer, offset, value, numDigits(value));
        return offset - pos;
    }

    public static int putLongZeroPadded(byte[] buffer, int offset, long value, int width) {
    	int pos = offset;
        if (value < 0) {
        	buffer[offset++] = '-';
            value = -value;
            width--;
        }
        int nd = numDigits(value);
    	for (int i=nd; i < width; i++) {
        	buffer[offset++] = '0';
    	}
    	offset += putPosLong(buffer, offset, value, nd);
    	return offset - pos;
    }

    public static int putLongRightPadded(byte[] buffer, int offset, long value, int width) {
    	int pos = offset;
        if (value < 0) {
        	buffer[offset++] = '-';    		
            value = -value;
            width--;
        }
        int nd = numDigits(value);
    	offset += putPosLong(buffer, offset, value, nd);
    	for (int i=nd; i < width; i++) {
        	buffer[offset++] = ' ';
    	}
    	return offset - pos;
    }
    
    public static int putLongLeftPadded(byte[] buffer, int offset, long value, int width) {
    	int pos = offset;
    	boolean negative = false;
        if (value < 0) {
        	negative = true;
            value = -value;
            width--;
        }
        int nd = numDigits(value);
    	for (int i=nd; i < width; i++) {
        	buffer[offset++] = ' ';
    	}
    	if (negative) {
        	buffer[offset++] = '-';    		
    	}
    	offset += putPosLong(buffer, offset, value, nd);
    	return offset - pos;
    }

    public static int putDouble(byte[] buffer, int offset, double d) {
    	return putDouble(buffer, offset, d, DOUBLE_DECIMALS);
    }

    public static int putDouble(byte[] buffer, int offset, double d, int decimals) {
    	long pi = (long)(d + NEGPOW10[decimals] * 0.5);
    	long pd = (long)((d-pi)*POW10[decimals] + 0.5);
    	int pos = offset;
    	if (pi < 0) {
    		buffer[offset++] = '-';
    		pi = -pi;
    		pd = -pd;
    	}
    	offset += putLong(buffer, offset, pi);
    	if (pd > 0) {
	    	buffer[offset++] = '.';
	    	int nd = numDigits(pd);
	    	for (int i=nd; i < decimals; i++) {
	        	buffer[offset++] = '0';
	    	}
	    	offset += putPosLongTrimmed(buffer, offset, pd, nd, nd);
    	}
    	return offset - pos;
    }
    
    public static int putShortBE(byte[] buffer, int pos, short value)
    {
        buffer[pos++] = (byte)((value >> 8) & 0xff);
        buffer[pos++] = (byte)((value >> 0) & 0xff);
        return 2;
    }

    public static int putShortLE(byte[] buffer, int pos, short value)
    {
        buffer[pos++] = (byte)((value >> 0) & 0xff);
        buffer[pos++] = (byte)((value >> 8) & 0xff);
        return 2;
    }
    
    public static short getShortLE(byte[] buffer, int pos)
    {
        return (short)((buffer[pos++]&0xff) | ((buffer[pos++]&0xff) << 8));
    }


    public static short getShortBE(ByteBuffer buf, int offset)
    {
        return (short)(((buf.get(offset) & 0xff) << 8) | ((buf.get(offset+1) & 0xff)));
    }
    
    public static short getShortLE(ByteBuffer buf, int offset)
    {
        return (short)(((buf.get(offset) & 0xff)) | ((buf.get(offset+1) & 0xff) << 8));
    }

    public static int putIntBE(byte[] buffer, int pos, int value)
    {
        buffer[pos++] = (byte)((value >> 24) & 0xff);
        buffer[pos++] = (byte)((value >> 16) & 0xff);
        buffer[pos++] = (byte)((value >> 8) & 0xff);
        buffer[pos++] = (byte)((value >> 0) & 0xff);
        return 4;
    }

    public static int putIntLE(byte[] buffer, int pos, int value)
    {
        buffer[pos++] = (byte)((value >> 0) & 0xff);
        buffer[pos++] = (byte)((value >> 8) & 0xff);
        buffer[pos++] = (byte)((value >> 16) & 0xff);
        buffer[pos++] = (byte)((value >> 24) & 0xff);
        return 4;
    }

    public static int getIntLE(byte[] buffer, int pos)
    {
        return (((int)(buffer[pos++])&0xff)) |
        	(((int)(buffer[pos++])&0xff) << 8) |
        	(((int)(buffer[pos++])&0xff) << 16) |
        	(((int)(buffer[pos++])&0xff) << 24);
    }
    
    public static int putLongBE(byte[] buffer, int pos, long value)
    {
        buffer[pos++] = (byte)((value >> 56) & 0xff);
        buffer[pos++] = (byte)((value >> 48) & 0xff);
        buffer[pos++] = (byte)((value >> 40) & 0xff);
        buffer[pos++] = (byte)((value >> 32) & 0xff);
        buffer[pos++] = (byte)((value >> 24) & 0xff);
        buffer[pos++] = (byte)((value >> 16) & 0xff);
        buffer[pos++] = (byte)((value >> 8) & 0xff);
        buffer[pos++] = (byte)((value >> 0) & 0xff);
        return 8;
    }

    public static int putLongLE(byte[] buffer, int pos, long value)
    {
        buffer[pos++] = (byte)((value >> 0) & 0xff);
        buffer[pos++] = (byte)((value >> 8) & 0xff);
        buffer[pos++] = (byte)((value >> 16) & 0xff);
        buffer[pos++] = (byte)((value >> 24) & 0xff);
        buffer[pos++] = (byte)((value >> 32) & 0xff);
        buffer[pos++] = (byte)((value >> 40) & 0xff);
        buffer[pos++] = (byte)((value >> 48) & 0xff);
        buffer[pos++] = (byte)((value >> 56) & 0xff);
        return 8;
    }

}
