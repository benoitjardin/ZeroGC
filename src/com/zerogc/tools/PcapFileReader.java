package com.zerogc.tools;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import com.zerogc.util.ByteSlice;
import com.zerogc.util.ByteUtils;
import com.zerogc.util.Level;
import com.zerogc.util.Logger;


public class PcapFileReader implements MessageSource {
    private Logger log;

    private static final int HEADER_LEN = 16;

    private MessageListener networkMessageListener;
    private FileChannel fileChannel;

    private ByteBuffer inBuffer = ByteBuffer.allocate(32*1024).order(ByteOrder.LITTLE_ENDIAN);

    private byte[] name;
    private byte[] srcAddrBytes = new byte[4];
    private byte[] dstAddrBytes = new byte[4];
    private long captureTime;
    private int srcPort;
    private int dstPort;
    
	public ByteSlice getName(ByteSlice slice) {
		return slice.set(name, 0, name.length);
	}
	public long getCaptureTime() {
		return this.captureTime;
	}
	public byte[] getSrcAddrBytes() {
		return srcAddrBytes;
	}
	public int getSrcPort() {
		return srcPort;
	}
	public byte[] getDstAddrBytes() {
		return dstAddrBytes;
	}
	public int getDstPort() {
		return dstPort;
	}
	public ByteBuffer getBufferIn() {
		return inBuffer;
	}

    public PcapFileReader(Logger log, String name, MessageListener networkMessageListener) throws IOException {
        this.log = log;
        this.name = name.getBytes();
        this.networkMessageListener = networkMessageListener;

        FileInputStream fileInputStream = new FileInputStream(name);
        this.fileChannel = fileInputStream.getChannel();
        
        // Initial load of the inBuffer
        int len = this.fileChannel.read(this.inBuffer);
        this.inBuffer.flip();
        // Skip pcap header
        this.inBuffer.position(24);
    }

    // Dispatch 1 message and resplendish inBuffer when exhausted 
    public void dispatch() {
        boolean exhausted = true;
        while (exhausted) {
            if (this.inBuffer.remaining() >= HEADER_LEN) {
            	int mark = this.inBuffer.position();
                int tv_sec = this.inBuffer.getInt();
                int tv_usec = this.inBuffer.getInt();
                this.captureTime = tv_sec*1000L + tv_usec/1000;
                int caplen = this.inBuffer.getInt();
                int len = this.inBuffer.getInt();
                if (this.inBuffer.remaining() < len) {
                    this.inBuffer.position(mark);
                } else {
                	int limit = this.inBuffer.limit();
                	this.inBuffer.limit(this.inBuffer.position()+len);
                	// Ethernet Header + IP Header until Source IP (30 bytes)
                	this.inBuffer.position(this.inBuffer.position()+30);
                	
                	this.inBuffer.get(this.srcAddrBytes);
                	this.inBuffer.get(this.dstAddrBytes);
                	this.srcPort = ByteUtils.getShortBE(this.inBuffer, this.inBuffer.position()) & 0xFFFF;
                	this.inBuffer.position(this.inBuffer.position()+2);
                	this.dstPort = ByteUtils.getShortBE(this.inBuffer, this.inBuffer.position()) & 0xFFFF;
                	this.inBuffer.position(this.inBuffer.position()+2);
                	int udpLen = ByteUtils.getShortBE(this.inBuffer, this.inBuffer.position()) & 0xFFFF;
                	this.inBuffer.position(this.inBuffer.position()+4);
                	
                    this.networkMessageListener.onMessage(this, this.inBuffer);

                    this.inBuffer.position(this.inBuffer.limit());
                	this.inBuffer.limit(limit);
                    exhausted = false;
                }
            }
            if (exhausted) {
                this.inBuffer.compact();
                try {
                    int len = this.fileChannel.read(this.inBuffer);
                    this.inBuffer.flip();
                    if (len == -1) {
                        this.networkMessageListener.onMessage(this, null);                    
                        break;
                    }
                } catch (IOException e) {
                    this.log.log(Level.ERROR, this.log.getSB().append("Exception: "), e);
                }
            }
        }
    }
}
