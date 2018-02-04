package com.zerogc.logging;

import java.io.IOException;
import java.io.OutputStream;

import com.zerogc.core.ByteStringBuilder;

public abstract class BaseLogWriter implements LogWriter {
    protected OutputStream stream;
    protected boolean immediateFlush = false;
    protected ByteStringBuilder sb = new ByteStringBuilder(4096);

    @Override
    public void close() throws IOException {
        stream.close();
    }

    @Override
    public ByteStringBuilder getSB() {
        return this.sb;
    }

    @Override
    public void write(ByteStringBuilder sb) {
        assert(sb == this.sb);
        try {
            stream.write(sb.getBuffer(), 0, sb.getLength());
            if (immediateFlush) {
                flush();
            }
        } catch (IOException e) {
            System.err.println("Caught Exception: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        sb.setLength(0);
    }

    @Override
    public void flush() throws IOException {
        stream.flush();
    }

}
