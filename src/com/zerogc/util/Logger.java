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

import java.util.Arrays;
import java.util.Calendar;

/**
 * @author Benoit Jardin
 */

public class Logger {
    private final static byte[] LEVEL = new byte[]{ 'O', 'F', 'E', 'W', 'I', 'D', 'T', 'A' };

    private byte[] name;
    private int level = Level.INFO;
    
    private ByteStringBuilder sb = new ByteStringBuilder(4096);
    private byte[] buffer = new byte[4096];
    private int offset;
    
    private Calendar calendar = Calendar.getInstance();

    public static Logger getLogger(String name) {
    	return new Logger(name);
    }

    public static Logger getLogger(byte[] name) {
    	return new Logger(name);
    }

    public Logger(String name) {
        this.name = name.getBytes();
    }

    public Logger(byte[] name) {
        this.name = name;
    }
    
    public byte[] getName() {
		return name;
	}
	public void setName(byte[] name) {
		this.name = name;
	}

	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
        this.level = level;
    }
    
    public ByteStringBuilder getSB() {
        this.sb.setLength(0);
        return this.sb;
    }
    
    private void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity > buffer.length) {
            expandCapacity(minimumCapacity);
        }
    }

    private void expandCapacity(int minimumCapacity) {
        int newCapacity = (buffer.length + 1) * 2;
            if (newCapacity < 0) {
                newCapacity = Integer.MAX_VALUE;
            } else if (minimumCapacity > newCapacity) {
            newCapacity = minimumCapacity;
        }
        buffer = Arrays.copyOf(buffer, newCapacity);
    }
        
    public boolean isEnabledFor(int level) {
        return level <= this.level;
    }
    
    public void addHeader() {
        // Format: "HH:mm:ss.ccc: "
        ensureCapacity(offset + 14);

        calendar.setTimeInMillis(System.currentTimeMillis());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int millisecond = calendar.get(Calendar.MILLISECOND);
        
        buffer[offset++] = (byte)('0' + hour/10);
        buffer[offset++] = (byte)('0' + hour%10);
        buffer[offset++] = ':';
        buffer[offset++] = (byte)('0' + minute/10);
        buffer[offset++] = (byte)('0' + minute%10);
        buffer[offset++] = ':';
        buffer[offset++] = (byte)('0' + second/10);
        buffer[offset++] = (byte)('0' + second%10);
        buffer[offset++] = '.';
        buffer[offset++] = (byte)('0' + millisecond/100);
        buffer[offset++] = (byte)('0' + (millisecond%100)/10);
        buffer[offset++] = (byte)('0' + millisecond%10);
        buffer[offset++] = ' ';
        buffer[offset++] = LEVEL[level];
        buffer[offset++] = ' ';
        //buffer[offset++] = '[';
        for (int i=0; i <this.name.length; i++){
        	buffer[offset++] = name[i];
        }
        //buffer[offset++] = ']';
        buffer[offset++] = ':';
        buffer[offset++] = ' ';
    }

    public void log(int level, String message) {
        if (level <= this.level) {
            addHeader();
            ensureCapacity(offset + message.length() + 1);
            offset += ByteUtils.putString(buffer, offset, message);
            buffer[offset++] = '\n';
            write();
        }
    }

    public void log(int level, String message, Throwable t) {
        if (level <= this.level) {
            addHeader();
            ensureCapacity(offset + message.length() + 1);
            offset += ByteUtils.putString(buffer, offset, message);
            buffer[offset++] = '\n';
            write();
            t.printStackTrace();
        }
    }
    
    public void log(int level, ByteStringBuilder sb) {
        if (level <= this.level) {
            addHeader();
            ensureCapacity(offset + sb.length() + 1);
            System.arraycopy(sb.buffer(), 0, buffer, offset, sb.length());
            offset += sb.length();
            buffer[offset++] = '\n';
            write();
        }
    }
    
    public void log(int level, ByteStringBuilder sb, Throwable t) {
        if (level <= this.level) {
            addHeader();
            ensureCapacity(offset + sb.length() + 1);
            System.arraycopy(sb.buffer(), 0, buffer, offset, sb.length());
            offset += sb.length();
            buffer[offset++] = '\n';
            write();
            t.printStackTrace();
        }
    }
    
    private void write() {
        System.err.write(buffer, 0, offset);
        offset = 0;
    }

}
