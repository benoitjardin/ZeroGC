package com.zerogc.logging;

import java.io.IOException;

import com.zerogc.core.ByteStringBuilder;

public class AsyncFileLogWriter extends FileLogWriter implements Runnable {

    private final ByteStringBuilder[] flipflop;
    private int index = 0;
    private ByteStringBuilder writerSb = null;

    public AsyncFileLogWriter(String name) {
        super(name);
        flipflop = new ByteStringBuilder[2];
        flipflop[0] = this.sb;
        flipflop[1] = new ByteStringBuilder(this.sb.getCapacity());

        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void run() {
        while (true) {
            if (writerSb != null) {
                try {
                    stream.write(writerSb.getBuffer(), 0, writerSb.getLength());
                    if (immediateFlush) {
                        flush();
                    }
                } catch (IOException e) {
                    System.err.println("Caught Exception: " + e.getMessage());
                    e.printStackTrace(System.err);
                }
                writerSb.setLength(0);
                writerSb = null;
            }
            Thread.yield();
        }
    }

    @Override
    public void write(ByteStringBuilder sb) {
        assert(sb == this.sb);
        if (writerSb == null) {
            writerSb = flipflop[index];
            assert(writerSb == this.sb);
            index ^= 1;
            this.sb = flipflop[index];
            assert(writerSb != this.sb);
        }
    }
}
