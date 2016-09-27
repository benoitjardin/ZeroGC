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
package com.zerogc.util;

public class CircularMessageQueue {

    volatile int offsetTail = 0;  // Read&Write by reader, read by writer
    int offsetHead = 0;           // Read&Write by writer
    
    long seqnoTail = 0;           // Read&Write by reader only
    volatile long seqnoHead = 0;  // Read&Write by reader, read by writer

    private final int capacity;
    private static byte[] queue;

    private static class Lengths {
    	static int length = 2;
    }
    
    public CircularMessageQueue(int capacity) {
    	this.capacity = capacity;
    	queue = new byte[capacity];
    }
    
    public int getCapacity() {
		return capacity;
	}

    public boolean isEmpty() {
		return seqnoTail == seqnoHead;
	}

    /**
     * @return the seqno of the sent message
     */
    public long send(byte[] buffer, int offset, int length) {
        int offsetWrite = offsetHead;
        int offsetRead = offsetTail;
    	int nextOffset = offsetWrite + Lengths.length + ((length + 1) & 0xFFFFFFFE);
    	
    	if (nextOffset < capacity) {
    		// Message doesn't wrap around buffer's end
        	// ...T======H..
        	// ===H.......T===
			if (offsetRead > offsetWrite && nextOffset > offsetRead) {
			    return -1;
			}
			ByteUtils.putShortLE(queue, offsetWrite, (short)length);
			System.arraycopy(buffer, offset, queue, offsetWrite, length);
    	} else {
    		// Message wraps around buffer's end
        	// ...T=========H.
        	// =========H...T=
    		nextOffset &= (capacity-1);
    		if (offsetRead != offsetWrite) {
				if (offsetRead > offsetWrite || nextOffset > offsetRead) {
				    return -1;
				}
    		}
    		offsetWrite += ByteUtils.putShortLE(queue, offsetWrite, (short)length);
			int len1 = capacity - offsetWrite;
			if (len1 != 0) {
				System.arraycopy(buffer, offset, queue, offsetWrite, len1);
			}
			if (len1 != length) {
				System.arraycopy(buffer, offset + len1, queue, 0, length - len1);
			}
    	}
		offsetHead = nextOffset;
		return seqnoHead++;
    }
    
    public int receive(byte[] buffer, int offset) {
        if (isEmpty()) {
            return -1;
        }
        int offsetRead = offsetTail;

		int msgLength = ByteUtils.getShortLE(queue, offsetRead);
		int msgSize =  Lengths.length + ((msgLength + 1) & 0xFFFFFFFE);
		int nextOffset = (offsetRead + msgSize);
    	if (nextOffset < capacity) {
    		System.arraycopy(queue, offsetRead + Lengths.length, buffer, offset, msgLength);
    	} else {
    		nextOffset &= (capacity-1);
    		offsetRead += Lengths.length;
    		int len1 = capacity - offsetRead;
    		if (len1 != 0) {
    			System.arraycopy(queue, offsetRead, buffer, offset, len1);
    		}
    		if (len1 != msgLength) {
    			System.arraycopy(queue, 0, buffer, offset+len1, msgLength - len1);
    		}
    	}
		offsetTail = nextOffset;
		seqnoTail++;
    	return msgLength;
    }
}
