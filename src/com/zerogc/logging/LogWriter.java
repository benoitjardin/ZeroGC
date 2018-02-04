package com.zerogc.logging;

import java.io.IOException;

import com.zerogc.core.ByteStringBuilder;

public interface LogWriter {
    ByteStringBuilder getSB();
    void open() throws IOException;
    void close() throws IOException;
    void write(ByteStringBuilder sb);
    void flush() throws IOException;
}
