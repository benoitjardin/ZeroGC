package com.zerogc.tools;

import java.nio.ByteBuffer;

import com.zerogc.collections.LongObjectHeap;


/**
 * @author Benoit Jardin
 * 
 * Reorder multiple sources based on the captureTime
 *  
 */
public class PcapScheduler implements MessageListener {
    private LongObjectHeap events = new LongObjectHeap();

    private MessageListener messageListener;
    
    public PcapScheduler(MessageListener messageListener) {
    	this.messageListener = messageListener;
    }
    
    @Override
    public void onMessage(MessageSource source, ByteBuffer buffer) {
    	events.insert(source.getCaptureTime(), source);
    }

    public void run() {
        while (this.events.size() > 0) {
        	int entry = this.events.firstEntry();
        	MessageSource source = (MessageSource)this.events.getValue(entry);
        	// Deliver the message
        	messageListener.onMessage(source, source.getBufferIn());
        	// Update to next message from that source
        	this.events.removeEntry(entry);
        	source.dispatch();
        }
    }
}
