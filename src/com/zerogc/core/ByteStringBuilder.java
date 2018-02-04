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
package com.zerogc.core;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;

public class ByteStringBuilder {
    private byte buffer[];
    private int length;

    static int MAX_SIZE_SHORT = 6;
    static int MAX_SIZE_INT = 11;
    static int MAX_SIZE_LONG = 20;
    static int MAX_SIZE_DOUBLE = 40;
    static int MAX_SIZE_INET4 = 15;
    static int MAX_SIZE_DATE = 23; // "YYYY-MM-DD HH:mm:ss.ccc"
    
    private Calendar calendar = Calendar.getInstance();

    public ByteStringBuilder() {
    }

    public ByteStringBuilder(int capacity) {
        buffer = new byte[capacity];
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int getLength() {
        return length;
    }
    public void setLength(int length) {
        this.length = length;
    }

    public int getCapacity() {
        return buffer.length;
    }

    public void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity > buffer.length) {
            expandCapacity(minimumCapacity);
        }
    }

    void expandCapacity(int minimumCapacity) {
        int newCapacity = (buffer.length + 1) * 2;
        if (newCapacity < 0) {
            newCapacity = Integer.MAX_VALUE;
        } else if (minimumCapacity > newCapacity) {
            newCapacity = minimumCapacity;
        }
        buffer = Arrays.copyOf(buffer, newCapacity);
    }

    public ByteStringBuilder append(Object obj) {
        return append(String.valueOf(obj));
    }

    public ByteStringBuilder append(String str) {
        int len = str.length();
        ensureCapacity(length+len);
        length += ByteUtils.putString(buffer, length, str);
        return this;
    }

    public ByteStringBuilder append(ByteStringBuilder sb) {
        int len = sb.getLength();
        ensureCapacity(length+len);
        System.arraycopy(sb.buffer, 0, buffer, length, len);
        length += len;
        return this;
    }

    public ByteStringBuilder append(ToByteString toByteString) {
        return toByteString.toByteString(this);
    }

    public ByteStringBuilder append(byte str[]) {
        int len = str.length;
        ensureCapacity(length+len);
        System.arraycopy(str, 0, buffer, length, len);
        length += len;
        return this;
    }

    public ByteStringBuilder append(byte str[], int offset, int len) {
        ensureCapacity(length+len);
        System.arraycopy(str, offset, buffer, length, len);
        length += len;
        return this;
    }

    public ByteStringBuilder append(char str[]) {
        int len = str.length;
        ensureCapacity(length+len);
        System.arraycopy(str, 0, buffer, length, len);
        length += len;
        return this;
    }

    public ByteStringBuilder append(char str[], int offset, int len) {
        ensureCapacity(length+len);
        System.arraycopy(str, offset, buffer, length, len);
        length += len;
        return this;
    }

    public ByteStringBuilder append(ByteBuffer buffer) {
        int len = buffer.remaining();
        ensureCapacity(length+len);
        int offset = buffer.position();
        for (int i=offset; i < offset+len; i++) {
            this.buffer[length++] = buffer.get(i);
        }
        return this;
    }

    public ByteStringBuilder append(ByteBuffer buffer, int offset, int len) {
        ensureCapacity(length+len);
        for (int i=offset; i < offset+len; i++) {
            this.buffer[length++] = buffer.get(i);
        }
        return this;
    }

    public ByteStringBuilder append(boolean b) {
        if (b) {
            ensureCapacity(length + 4);
            buffer[length++] = 't';
            buffer[length++] = 'r';
            buffer[length++] = 'u';
            buffer[length++] = 'e';
        } else {
            ensureCapacity(length + 5);
            buffer[length++] = 'f';
            buffer[length++] = 'a';
            buffer[length++] = 'l';
            buffer[length++] = 's';
            buffer[length++] = 'e';
        }
        return this;
    }

    public ByteStringBuilder append(byte b) {
        ensureCapacity(length + 1);
        buffer[length++] = b;
        return this;
    }
    public ByteStringBuilder append(char c) {
        ensureCapacity(length + 1);
        buffer[length++] = (byte)c;
        return this;
    }

    public ByteStringBuilder append(short s) {
        ensureCapacity(length + MAX_SIZE_SHORT);
        length += ByteUtils.putInt(buffer, length, s);
        return this;
    }

    public ByteStringBuilder append(int i) {
        ensureCapacity(length + MAX_SIZE_INT);
        if (i == Integer.MIN_VALUE) {
            length += ByteUtils.putString(buffer, length, "-2147483648");
        } else {
            length += ByteUtils.putInt(buffer, length, i);
        }
        return this;
    }

    public ByteStringBuilder append(long l) {
        ensureCapacity(length + MAX_SIZE_LONG);
        if (l == Long.MIN_VALUE) {
            length = ByteUtils.putString(buffer, length, "-9223372036854775808");
        } else {
            length += ByteUtils.putLong(buffer, length, l);
        }
        return this;
    }

    public ByteStringBuilder append(float f) {
        ensureCapacity(length + MAX_SIZE_DOUBLE);
        length += ByteUtils.putDouble(buffer, length, f);
        return this;
    }

    public ByteStringBuilder append(double d) {
        ensureCapacity(length + MAX_SIZE_DOUBLE);
        length += ByteUtils.putDouble(buffer, length, d);
        return this;
    }

    public ByteStringBuilder appendInet4Address(byte[] addrBytes) {
        ensureCapacity(length + MAX_SIZE_INET4);
        length += ByteUtils.putInt(buffer, length, addrBytes[0] & 0xFF);
        buffer[length++] = '.';
        length += ByteUtils.putInt(buffer, length, addrBytes[1] & 0xFF);
        buffer[length++] = '.';
        length += ByteUtils.putInt(buffer, length, addrBytes[2] & 0xFF);
        buffer[length++] = '.';
        length += ByteUtils.putInt(buffer, length, addrBytes[3] & 0xFF);
        return this;
    }

    public ByteStringBuilder appendInet4Address(int address) {
        ensureCapacity(length + MAX_SIZE_INET4);
        length += ByteUtils.putInt(buffer, length, (address >>> 24) & 0xFF);
        buffer[length++] = '.';
        length += ByteUtils.putInt(buffer, length, (address >>> 16) & 0xFF);
        buffer[length++] = '.';
        length += ByteUtils.putInt(buffer, length, (address >>> 8) & 0xFF);
        buffer[length++] = '.';
        length += ByteUtils.putInt(buffer, length, (address & 0xFF));
        return this;
    }

    public ByteStringBuilder append(InetSocketAddress inetSocketAddress) {
        ensureCapacity(length + MAX_SIZE_INET4 + 6);
        //if (inetSocketAddress.isUnresolved()) {
        int address = inetSocketAddress.getAddress().hashCode();
        appendInet4Address(address);
        //} else {
        // This might block
        //  length += ByteUtils.putString(buffer, length, inetSocketAddress.getHostName());
        //}
        buffer[length++] = ':';
        length += ByteUtils.putInt(buffer, length, inetSocketAddress.getPort());
        return this;
    }

    public ByteStringBuilder appendTimestampFormatted(long timestamp) {
        // Format: "YYYY-MM-DD HH:mm:ss.ccc"
        ensureCapacity(length + 23);

        calendar.setTimeInMillis(timestamp);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH)+1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int millisecond = calendar.get(Calendar.MILLISECOND);

        buffer[length++] = (byte)('0' + year/1000);
        buffer[length++] = (byte)('0' + (year%1000)/100);
        buffer[length++] = (byte)('0' + (year%100)/10);
        buffer[length++] = (byte)('0' + year%10);
        buffer[length++] = (byte)('-');
        buffer[length++] = (byte)('0' + month/10);
        buffer[length++] = (byte)('0' + month%10);
        buffer[length++] = (byte)('-');
        buffer[length++] = (byte)('0' + day/10);
        buffer[length++] = (byte)('0' + day%10);
        buffer[length++] = (byte)(' ');
        buffer[length++] = (byte)('0' + hour/10);
        buffer[length++] = (byte)('0' + hour%10);
        buffer[length++] = (byte)(':');
        buffer[length++] = (byte)('0' + minute/10);
        buffer[length++] = (byte)('0' + minute%10);
        buffer[length++] = (byte)(':');
        buffer[length++] = (byte)('0' + second/10);
        buffer[length++] = (byte)('0' + second%10);
        buffer[length++] = (byte)('.');
        buffer[length++] = (byte)('0' + millisecond/100);
        buffer[length++] = (byte)('0' + (millisecond%100)/10);
        buffer[length++] = (byte)('0' + millisecond%10);
        return this;
    }

    public ByteStringBuilder appendTimestamp(long timestamp) {
        // Format: "YYYYMMDDHHmmssccc"
        ensureCapacity(length + 17);

        calendar.setTimeInMillis(timestamp);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH)+1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int millisecond = calendar.get(Calendar.MILLISECOND);

        buffer[length++] = (byte)('0' + year/1000);
        buffer[length++] = (byte)('0' + (year%1000)/100);
        buffer[length++] = (byte)('0' + (year%100)/10);
        buffer[length++] = (byte)('0' + year%10);
        buffer[length++] = (byte)('0' + month/10);
        buffer[length++] = (byte)('0' + month%10);
        buffer[length++] = (byte)('0' + day/10);
        buffer[length++] = (byte)('0' + day%10);
        buffer[length++] = (byte)('0' + hour/10);
        buffer[length++] = (byte)('0' + hour%10);
        buffer[length++] = (byte)('0' + minute/10);
        buffer[length++] = (byte)('0' + minute%10);
        buffer[length++] = (byte)('0' + second/10);
        buffer[length++] = (byte)('0' + second%10);
        buffer[length++] = (byte)('0' + millisecond/100);
        buffer[length++] = (byte)('0' + (millisecond%100)/10);
        buffer[length++] = (byte)('0' + millisecond%10);
        return this;
    }

    public ByteStringBuilder appendTime(long timestamp) {
        // Format: "HH:mm:ss.ccc"
        ensureCapacity(length + 12);

        calendar.setTimeInMillis(timestamp);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int millisecond = calendar.get(Calendar.MILLISECOND);

        buffer[length++] = (byte)('0' + hour/10);
        buffer[length++] = (byte)('0' + hour%10);
        buffer[length++] = ':';
        buffer[length++] = (byte)('0' + minute/10);
        buffer[length++] = (byte)('0' + minute%10);
        buffer[length++] = ':';
        buffer[length++] = (byte)('0' + second/10);
        buffer[length++] = (byte)('0' + second%10);
        buffer[length++] = '.';
        buffer[length++] = (byte)('0' + millisecond/100);
        buffer[length++] = (byte)('0' + (millisecond%100)/10);
        buffer[length++] = (byte)('0' + millisecond%10);
        return this;
    }

    static final byte[] hex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    private final int BYTES_PER_LINE = 16;
    private final int BYTES_PER_BLOCK = 4;
    private final int TEXT_OFFSET = BYTES_PER_LINE*2 + BYTES_PER_LINE/BYTES_PER_BLOCK;
    // 0123: 0011223344 55667788 99ABBCC DDEEFF 0123456789012345
    //       <---------- TEXT_OFFSET ---------->
    private final int MAX_LINE_LENGTH = 6 + TEXT_OFFSET + BYTES_PER_LINE + 1;

    public ByteStringBuilder appendHexDump(byte[] buffer, int offset, int len) {
        int textOffset = length;
        int i=0;
        ensureCapacity(length + MAX_LINE_LENGTH*((len+MAX_LINE_LENGTH-1)/MAX_LINE_LENGTH));
        for (; i < len; ++i, ++offset) {
            if (i%BYTES_PER_LINE == 0) {
                length = textOffset;
                this.buffer[length++] = '\n';
                this.buffer[length++] = hex[((i >> 12) & 0xF)];
                this.buffer[length++] = hex[((i >> 8) & 0xF)];
                this.buffer[length++] = hex[((i >> 4)  & 0xF)];
                this.buffer[length++] = hex[((i)       & 0xF)];
                this.buffer[length++] = ':';
                this.buffer[length++] = ' ';                
                textOffset = length + TEXT_OFFSET;
            }
            byte value = buffer[offset];
            this.buffer[length++] = hex[((value >> 4)  & 0xF)];
            this.buffer[length++] = hex[((value)       & 0xF)];
            if (value > 31 && value < 127) {
                this.buffer[textOffset++] = value;
            } else {
                this.buffer[textOffset++] = '.';
            }
            if (i%BYTES_PER_BLOCK == BYTES_PER_BLOCK-1) {
                this.buffer[length++] = ' ';                
            }
        }
        // Fill missing hex with blanks
        for (; i%BYTES_PER_LINE > 0; i++) {
            this.buffer[length++] = ' ';
            this.buffer[length++] = ' ';
            if (i%BYTES_PER_BLOCK == BYTES_PER_BLOCK-1) {
                this.buffer[length++] = ' ';                
            }            
        }
        length = textOffset;
        return this;
    }

    public ByteStringBuilder appendHexDump(ByteBuffer buffer) {
        int textOffset = length;
        int i=0;
        int offset = buffer.position();
        int len = buffer.remaining();
        ensureCapacity(length + MAX_LINE_LENGTH*((len+MAX_LINE_LENGTH-1)/MAX_LINE_LENGTH));
        for (; i < len; ++i, ++offset) {
            if (i%BYTES_PER_LINE == 0) {
                length = textOffset;
                this.buffer[length++] = '\n';
                this.buffer[length++] = hex[((i >> 12) & 0xF)];
                this.buffer[length++] = hex[((i >> 8) & 0xF)];
                this.buffer[length++] = hex[((i >> 4)  & 0xF)];
                this.buffer[length++] = hex[((i)       & 0xF)];
                this.buffer[length++] = ':';
                this.buffer[length++] = ' ';                
                textOffset = length + TEXT_OFFSET;
            }
            byte value = buffer.get(offset);
            this.buffer[length++] = hex[((value >> 4)  & 0xF)];
            this.buffer[length++] = hex[((value)       & 0xF)];
            if (value > 31 && value < 127) {
                this.buffer[textOffset++] = value;
            } else {
                this.buffer[textOffset++] = '.';
            }
            if (i%BYTES_PER_BLOCK == BYTES_PER_BLOCK-1) {
                this.buffer[length++] = ' ';                
            }
        }
        // Fill missing hex with blanks
        for (; i%BYTES_PER_LINE > 0; i++) {
            this.buffer[length++] = ' ';
            this.buffer[length++] = ' ';
            if (i%BYTES_PER_BLOCK == BYTES_PER_BLOCK-1) {
                this.buffer[length++] = ' ';                
            }            
        }
        length = textOffset;
        return this;
    }

}
