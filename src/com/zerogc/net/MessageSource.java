package com.zerogc.net;

import java.nio.ByteBuffer;

import com.zerogc.core.ByteSlice;

public interface MessageSource {
    ByteSlice getName(ByteSlice slice);
    byte[] getSrcAddrBytes();
    int getSrcPort();
    byte[] getDstAddrBytes();
    int getDstPort();
    long getCaptureTime();
    ByteBuffer getBufferIn();
    void dispatch();
}
