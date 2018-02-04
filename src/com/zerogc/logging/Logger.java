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
package com.zerogc.logging;

import java.util.Calendar;

import com.zerogc.core.ByteStringBuilder;

/**
 * @author Benoit Jardin
 */

public class Logger {
    private final static byte[] LEVEL = new byte[]{ 'O', 'F', 'E', 'W', 'I', 'D', 'T', 'A' };

    //private final int HEADER_FIXED_LENGTH = 28; // Format: "yyyyMMdd HH:mm:ss.ccc: L Name - "
    private final int HEADER_FIXED_LENGTH = 19; // Format: "HH:mm:ss.ccc: L Name - "
    private final int HEADER_LENGTH;

    private byte[] name;
    private int level = Level.INFO;
    private LogWriter logWriter;
    private int headerPos = 0;

    private Calendar calendar = Calendar.getInstance();

    public Logger(byte[] name, LogWriter logWriter) {
        this.name = name;
        this.logWriter = logWriter;
        HEADER_LENGTH = HEADER_FIXED_LENGTH + this.name.length;
    }

    public byte[] getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }
    public void setLevel(int level) {
        this.level = level;
    }

    public ByteStringBuilder getSB() {
        ByteStringBuilder sb = logWriter.getSB();
        headerPos = sb.getLength();
        sb.ensureCapacity(sb.getLength() + HEADER_LENGTH);
        // Reserve space for header
        sb.setLength(sb.getLength() + HEADER_LENGTH);
        return sb;
    }

    public boolean isEnabledFor(int level) {
        return level <= this.level;
    }

    public void addHeader(ByteStringBuilder sb) {
        calendar.setTimeInMillis(System.currentTimeMillis());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int millisecond = calendar.get(Calendar.MILLISECOND);

        // Format: "HH:mm:ss.ccc: "
        int offset = headerPos;
        byte[] buffer = sb.getBuffer();
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
        buffer[offset++] = ':';
        buffer[offset++] = ' ';
        buffer[offset++] = LEVEL[level];
        buffer[offset++] = ' ';
        for (int i=0; i <this.name.length; i++){
            buffer[offset++] = name[i];
        }
        buffer[offset++] = ' ';
        buffer[offset++] = '-';
        buffer[offset++] = ' ';
    }

    public void trace(String message) { log(Level.TRACE, message); }
    public void debug(String message) { log(Level.DEBUG, message); }
    public void info(String message) { log(Level.INFO, message); }
    public void warn(String message) { log(Level.WARN, message); }
    public void error(String message) { log(Level.ERROR, message); }
    public void fatal(String message) { log(Level.FATAL, message); }

    public void trace(ByteStringBuilder sb) { log(Level.TRACE, sb); }
    public void debug(ByteStringBuilder sb) { log(Level.DEBUG, sb); }
    public void info(ByteStringBuilder sb) { log(Level.INFO, sb); }
    public void warn(ByteStringBuilder sb) { log(Level.WARN, sb); }
    public void error(ByteStringBuilder sb) { log(Level.ERROR, sb); }
    public void fatal(ByteStringBuilder sb) { log(Level.FATAL, sb); }

    public void log(int level, String message) {
        ByteStringBuilder sb = getSB();
        if (level <= this.level) {
            addHeader(sb);
            sb.append(message);
            sb.append('\n');
            logWriter.write(sb);
        } else {
            sb.setLength(headerPos);
        }
    }

    public void log(int level, String message, Throwable t) {
        ByteStringBuilder sb = getSB();
        if (level <= this.level) {
            addHeader(sb);
            sb.append(message);
            sb.append('\n');
            sb.append(t.getMessage());
            sb.append(t.getStackTrace()); // TODO: remove allocation
            logWriter.write(sb);
        } else {
            sb.setLength(headerPos);
        }
    }

    public void log(int level, ByteStringBuilder sb) {
        if (level <= this.level) {
            addHeader(sb);
            sb.append('\n');
            logWriter.write(sb);
        } else {
            sb.setLength(headerPos);
        }
    }

    public void log(int level, ByteStringBuilder sb, Throwable t) {
        if (level <= this.level) {
            addHeader(sb);
            sb.append('\n');
            sb.append(t.getMessage());
            sb.append(t.getStackTrace()); // TODO: remove allocation
            logWriter.write(sb);
        } else {
            sb.setLength(headerPos);
        }
    }
}
