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


/**
 * @author Benoit Jardin
 * Encode latencies as a FIXFAST sequence of mandatory signed integers
 */
public class Latencies {
    private static final int BUFFER_SIZE = 16*1024;
    private static final int DELTA_SIZE = 1000;
    
    // Keep a copy of the encoded latencies array for speedup
    // Without optimization decode + encode 4.4M/sec latencies/sec (decode 7 deltas, encode 8 deltas)
    // With optimization decode + encode 8.8M/sec latencies/sec (decode 7 deltas, encode 8 deltas)
    private byte[] buffer = new byte[BUFFER_SIZE];
    private int len = 0;
    
    private long[] delta = new long[DELTA_SIZE]; 
    private int deltaCount = 0;
    private long deltaSum = 0;

    public byte[] getBuffer() {
        return buffer;
    }
    
    public int getLenth() {
        return this.len;
    }
    public void setLenth(int length) {
        this.len = length;
    }    
    public long[] getDelta() {
        return this.delta;
    }

    public int getDeltaCount() {
        return this.deltaCount;
    }

    public long getDeltaSum() {
        return this.deltaSum;
    }

    public void clear() {
        this.deltaSum = 0;
        this.deltaCount = 0;
        this.len = 0;
    }

    public void init(int pos, int deltaCount, long deltaSum) {
        this.len = pos;
        this.deltaCount = deltaCount;
        this.deltaSum = deltaSum;
    }
    
    public void add(long timestamp) {
        long value = timestamp - deltaSum;
        this.deltaSum = timestamp;
        this.delta[deltaCount++] = value;
        this.len = encode(value, this.len);
    }

    // Add timestamp without changing the state
    public int addScratch(long timestamp) {
        long value = timestamp - deltaSum;
        return encode(value, this.len);
    }

    public final int decode(byte[] buffer, int offset, int len) {
        this.len = offset+len;
        if (buffer != this.buffer) {
            System.arraycopy(buffer, offset, this.buffer, 0, len);
        }
        
        this.deltaCount = 0;
        this.deltaSum = 0;
        while (offset < this.len) {
            boolean stop = false;
            long value = 0;
            if (((buffer[offset] & 0x40) != 0)) {
                value = -1;
            }
            int size = 0;
            while (offset < this.len) {
                byte b = buffer[offset++];
                size++;
                value <<= 7;
                value |= (b & 0x7f);
                stop = ((b & 0x80) != 0);
                if (stop) {
                    break;
                }
            }

            if (!stop /*|| size > (Long.SIZE+6)/7 */) {
                throw new RuntimeException("Missing stop bit while decoding Integer.");
            }
            this.deltaSum += value;
            this.delta[deltaCount++] = value;
        }
        return offset;
    }

    public final int encode(long value, int pos) {
        if (value >= 0) {
/*
      Test value a max of 4 time as follow:
Level: 1 2 3 4    
       | 0x0000000000000040
       0x0000000000002000
         | | 0x0000000000100000
         | 0x0000000008000000
         | | 0x0000000400000000
         0x0000020000000000
           | 0x0001000000000000
           0x0080000000000000
             0x4000000000000000
*/

            if (value < 0x0000000000002000L) {
                if (value < 0x0000000000000040L) {
                    buffer[pos++] = (byte)(((value & 0x7F) | 0x80));
                } else {
                    buffer[pos++] = (byte)(((value >> 7) & 0x7F));
                    buffer[pos++] = (byte)(((value & 0x7F) | 0x80));
                }
            } else if (value < 0x0000020000000000L) {
                if (value < 0x0000000008000000L) {
                    if (value < 0x0000000000100000L) {
                        buffer[pos++] = (byte)(((value >> 14) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 7) & 0x7F));
                        buffer[pos++] = (byte)(((value & 0x7F) | 0x80));
                    } else {
                        buffer[pos++] = (byte)(((value >> 21) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 14) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 7) & 0x7F));
                        buffer[pos++] = (byte)(((value & 0x7F) | 0x80));
                    }
                } else {
                    if (value < 0x0000000400000000L) {
                        buffer[pos++] = (byte)(((value >> 28) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 21) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 14) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 7) & 0x7F));
                        buffer[pos++] = (byte)(((value & 0x7F) | 0x80));
                    } else {
                        buffer[pos++] = (byte)(((value >> 35) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 28) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 21) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 14) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 7) & 0x7F));
                        buffer[pos++] = (byte)(((value & 0x7F) | 0x80));
                    }
                }
            } else {
                if (value < 0x0080000000000000L) {
                    if (value < 0x0001000000000000L) {
                        buffer[pos++] = (byte)(((value >> 42) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 35) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 28) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 21) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 14) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 7) & 0x7F));
                        buffer[pos++] = (byte)(((value & 0x7F) | 0x80));
                    } else {
                        buffer[pos++] = (byte)(((value >> 49) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 42) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 35) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 28) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 21) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 14) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 7) & 0x7F));
                        buffer[pos++] = (byte)(((value & 0x7F) | 0x80));
                    }
                } else {
                    if (value < 0x4000000000000000L) {
                        buffer[pos++] = (byte)(((value >> 56) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 49) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 42) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 35) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 28) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 21) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 14) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 7) & 0x7F));
                        buffer[pos++] = (byte)(((value & 0x7F) | 0x80));
                    } else {
                        buffer[pos++] = (byte)(((value >> 63) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 56) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 49) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 42) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 35) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 28) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 21) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 14) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 7) & 0x7F));
                        buffer[pos++] = (byte)(((value & 0x7F) | 0x80));
                    }
                }
            }
        } else {
            // Negative case
            if (value >= -0x0000000000002000L) {
                if (value >= -0x0000000000000040L) {
                    buffer[pos++] = (byte)(((value & 0x7F) | 0x80));
                } else {
                    buffer[pos++] = (byte)(((value >> 7) & 0x7F));
                    buffer[pos++] = (byte)(((value & 0x7F) | 0x80));
                }
            } else if (value >= -0x0000020000000000L) {
                if (value >= -0x0000000008000000L) {
                    if (value >= -0x0000000000100000L) {
                        buffer[pos++] = (byte)(((value >> 14) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 7) & 0x7F));
                        buffer[pos++] = (byte)(((value & 0x7F) | 0x80));
                    } else {
                        buffer[pos++] = (byte)(((value >> 21) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 14) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 7) & 0x7F));
                        buffer[pos++] = (byte)(((value & 0x7F) | 0x80));
                    }
                } else {
                    if (value >= -0x0000000400000000L) {
                        buffer[pos++] = (byte)(((value >> 28) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 21) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 14) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 7) & 0x7F));
                        buffer[pos++] = (byte)(((value & 0x7F) | 0x80));
                    } else {
                        buffer[pos++] = (byte)(((value >> 35) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 28) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 21) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 14) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 7) & 0x7F));
                        buffer[pos++] = (byte)(((value & 0x7F) | 0x80));
                    }
                }
            } else {
                if (value >= -0x0080000000000000L) {
                    if (value >= -0x0001000000000000L) {
                        buffer[pos++] = (byte)(((value >> 42) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 35) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 28) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 21) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 14) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 7) & 0x7F));
                        buffer[pos++] = (byte)(((value & 0x7F) | 0x80));
                    } else {
                        buffer[pos++] = (byte)(((value >> 49) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 42) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 35) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 28) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 21) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 14) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 7) & 0x7F));
                        buffer[pos++] = (byte)(((value & 0x7F) | 0x80));
                    }
                } else {
                    if (value >= -0x4000000000000000L) {
                        buffer[pos++] = (byte)(((value >> 56) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 49) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 42) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 35) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 28) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 21) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 14) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 7) & 0x7F));
                        buffer[pos++] = (byte)(((value & 0x7F) | 0x80));
                    } else {
                        buffer[pos++] = (byte)(((value >> 63) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 56) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 49) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 42) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 35) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 28) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 21) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 14) & 0x7F));
                        buffer[pos++] = (byte)(((value >> 7) & 0x7F));
                        buffer[pos++] = (byte)(((value & 0x7F) | 0x80));
                    }
                }
            }
        }
        return pos;
    }

    public ByteStringBuilder toString(ByteStringBuilder sb) {
        for (int i=0; i<deltaCount; ++i) {
            if (i != 0) {
                sb.append(";");
            }
            sb.append(delta[i]);
        }
        return sb;
    }
}

