package com.zerogc.tools;

import java.nio.ByteBuffer;

import com.zerogc.util.ByteSlice;

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
