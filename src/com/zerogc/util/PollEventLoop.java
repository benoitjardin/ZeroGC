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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe.SinkChannel;
import java.nio.channels.Pipe.SourceChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.zerogc.collections.IntRbTree;
import com.zerogc.collections.LongHeap;
import com.zerogc.collections.LongObjectHeap;

/**
 * GC free event loop based on the System V UNIX poll system call on Unix
 * and select on Windows.
 * @author Benoit Jardin
 */
public class PollEventLoop implements EventLoop {
    private Logger log = new Logger(PollEventLoop.class.getSimpleName());

    private static final int CLOCK_GRANULARITY = 10;

	private final ByteBuffer fdsByteBuffer;
	private boolean open = false;
	
    private LongObjectHeap timers = new LongObjectHeap();
    private long currentMicros;
    private long currentMillis;

    private SelectionKeys selectionKeys = new SelectionKeys(SelectionKeys.class.getSimpleName(), Native.MAX_SELECTABLE_FDS);
    EventLoopListener selectEventHandler = null;
    
    private class PollSelectionKey implements EventLoopSelectionKey {
        public static final int OFFSET_ADDED = -1;
        public static final int OFFSET_REMOVED = -2;
        
    	SelectableChannel selectableChannel;
        int fd;
        int interestOps;
        short eventType;
        EventLoopListener eventHandler;
        int offset;
        
        @Override
        public int interestOps() {
        	return this.interestOps;
        }

        @Override
        public void interestOps(int ops) {
        	this.interestOps = ops;
        	this.eventType = 0;
        	if ((ops & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0) {
        		this.eventType |= Native.POLLIN;
        	}
        	if ((ops & (SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT)) != 0) {
        		// Windows reports async connection errors in select exceptfds
        		//this.eventType |= (Native.POLLOUT | Native.POLLPRI);
        		this.eventType |= Native.POLLOUT;
        	}
        }
        
        @Override
        public void cancel() {
            // Mark the selection key for later removal
            this.offset = PollSelectionKey.OFFSET_REMOVED;
            this.interestOps = 0;
        }
        
    };

    // Map between file descriptor to selection key
    class SelectionKeys extends IntRbTree {
    	private PollSelectionKey[] value;

        public SelectionKeys() {
            super(SelectionKeys.class.getSimpleName(), INITIAL_CAPACITY, GROWTH_FACTOR);
        }

        public SelectionKeys(String name) {
            super(name, INITIAL_CAPACITY, GROWTH_FACTOR);
        }
        
        public SelectionKeys(String name, int initialCapacity) {
            super(name, initialCapacity, GROWTH_FACTOR);
        }

        public SelectionKeys(String name, int initialCapacity, float growthFactor) {
            super(name, initialCapacity, growthFactor);
        }
        
        @Override
        protected void grow(int capacity, int newCapacity) {
            super.grow(capacity, newCapacity);

            PollSelectionKey[] newValue = new PollSelectionKey[newCapacity];
            if (capacity > 0) {
                System.arraycopy(this.value, 0, newValue, 0, capacity);
            }
            for (int i=capacity; i<newCapacity; i++) {
            	newValue[i] = new PollSelectionKey();
            }
            this.value = newValue;
        }
        
        public PollSelectionKey getValue(int entry) {
            return this.value[entry];
        }
      
        public int put(int key) {
        	int entry = insert(key);
        	return entry;        	
        }
    }
 
    // Multimap between timer expiry and EventHandler
    static class TimerEvents extends LongHeap {
    	private EventLoopListener[] value;

        @Override
        protected void grow(int capacity, int newCapacity) {
            super.grow(capacity, newCapacity);

            EventLoopListener[] newValue = new EventLoopListener[newCapacity];
            if (capacity > 0) {
                System.arraycopy(this.value, 0, newValue, 0, capacity);
            }
            this.value = newValue;
        }
        
        public EventLoopListener getValue(int entry) {
            return this.value[entry];
        }
        
        public int put(long key, EventLoopListener eventHandler) {
        	int entry = super.insert(key);
        	this.value[entry] = eventHandler; 
        	return entry;
        }
    }
    
    public PollEventLoop() {
        currentMicros = Native.currentTimeMicros();
        currentMillis = currentMicros/1000;
    	fdsByteBuffer = ByteBuffer.allocateDirect(Native.MAX_SELECTABLE_FDS * Native.POLL_SIZE);
    	fdsByteBuffer.order(ByteOrder.nativeOrder());
    }
    // --------------------- Basic event loop ---------------------

    @Override
    public long currentMicros() {
        return this.currentMicros;
    }
    
    @Override
    public long currentMillis() {
        return this.currentMillis;
    }
    
    @Override
    public void open() {
    	open = true;
    }
    
    @Override
    public void close() {
    	open = false;
    }
    
    @Override
    public EventLoopSelectionKey register(ServerSocketChannel serverSocketChannel, int ops, EventLoopListener eventHandler) {
        int fd = Native.getFdVal_ServerSocketChannel(serverSocketChannel);
        return registerImpl(serverSocketChannel, fd, ops, eventHandler);
    }

    @Override
    public EventLoopSelectionKey register(SocketChannel socketChannel, int ops, EventLoopListener eventHandler) {
        int fd = Native.getFdVal_SocketChannel(socketChannel);
        return registerImpl(socketChannel, fd, ops, eventHandler);
    }

    @Override
    public EventLoopSelectionKey register(DatagramChannel datagramChannel, int ops, EventLoopListener eventHandler) {
        int fd = Native.getFdVal_DatagramChannel(datagramChannel);
        return registerImpl(datagramChannel, fd, ops, eventHandler);
    }

	@Override
	public EventLoopSelectionKey register(SourceChannel sourceChannel, int ops, EventLoopListener eventhandler) {
        int fd = Native.getFdVal_SourceChannel(sourceChannel);
		return registerImpl(sourceChannel, fd, ops, eventhandler);
	}

	@Override
	public EventLoopSelectionKey register(SinkChannel sinkChannel, int ops, EventLoopListener eventhandler) {
        int fd = Native.getFdVal_SinkChannel(sinkChannel);
		return registerImpl(sinkChannel, fd, ops, eventhandler);
	}

    private EventLoopSelectionKey registerImpl(SelectableChannel selectableChannel, int fd, int ops, EventLoopListener eventHandler) {
    	int entry = this.selectionKeys.put(fd);
    	
    	log.log(Level.DEBUG, log.getSB().append("Register fd: ").append(fd).append(" at entry: ").append(entry).append(" for ops: ").append(ops));

    	PollSelectionKey selectionKey = this.selectionKeys.getValue(entry);
    	selectionKey.fd = fd;
    	selectionKey.selectableChannel = selectableChannel;
    	selectionKey.eventHandler = eventHandler;
    	selectionKey.interestOps(ops);
    	selectionKey.offset = PollSelectionKey.OFFSET_ADDED;
    	return selectionKey;
    }

    @Override
    public EventLoopListener registerSelect(EventLoopListener eventhandler)
    {
        EventLoopListener oldEventhandler = selectEventHandler;
        selectEventHandler = eventhandler;
        return oldEventhandler;
    }

    @Override
    public int addTimer(long whenMillis, TimerListener timerHandler) {
    	log.log(Level.DEBUG, "addTimer");
    	return this.timers.insert(whenMillis, timerHandler);
    }
    
    

    @Override
    public void cancelTimer(int entry) {
    	log.log(Level.DEBUG, "cancelTimer");
    	this.timers.removeEntry(entry);
    }

    @Override
    public void run() {
        log.log(Level.INFO, "Starting EventLoop");

        while (open) {
	        try {
                long nextTimer = Long.MIN_VALUE;
                while (!this.timers.isEmpty()) {
                    int entry = this.timers.firstEntry();
                    nextTimer = this.timers.getKey(entry);
                    if (nextTimer - currentMillis < CLOCK_GRANULARITY) {
                        // Callbacks may change the timer map, remove timer entry beforehand
                    	TimerListener timerHandler = (TimerListener)this.timers.getValue(entry);
                        this.timers.removeEntry(entry);
                        timerHandler.onTimer(entry, currentMillis);
                        nextTimer = Long.MIN_VALUE;
                    } else {
                        break;
                    }
                }
                
                this.fdsByteBuffer.clear();
                for (int entry = this.selectionKeys.firstEntry(), i = 0; entry != -1; i++) {
                	PollSelectionKey selectionKey = this.selectionKeys.getValue(entry);
                	if (selectionKey.offset != PollSelectionKey.OFFSET_REMOVED) {
                		log.log(Level.DEBUG, log.getSB().append("Poll register fd: ").append(selectionKey.fd).append(" at entry: ").append(entry).append(" for event: ").append(selectionKey.eventType));
                    	this.fdsByteBuffer.putInt(selectionKey.fd);
                    	this.fdsByteBuffer.putShort(selectionKey.eventType);
                        selectionKey.offset = this.fdsByteBuffer.position();
                    	this.fdsByteBuffer.putShort((short)0);
                    	entry = selectionKeys.nextEntry(entry);
                    } else {
                        log.log(Level.DEBUG, log.getSB().append("Remove selectionKey for fd: ").append(selectionKey.fd).append(" at entry: ").append(entry));
                        entry = selectionKeys.removeEntry(entry);
                    }
                }
                
                // Callbacks may have been running for some non neglectable time, update current time before calculating timeout
                currentMicros = Native.currentTimeMicros();
                currentMillis = currentMicros/1000;
                long timeout = (int)(nextTimer - currentMillis);
                if (nextTimer == Long.MIN_VALUE) {
                    timeout = -1;
                } else if (timeout < CLOCK_GRANULARITY) {
                    timeout = 0;
                }
                int intTimeout = timeout > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)timeout;
                //intTimeout = 0;
                
                if (selectEventHandler != null) {
                    selectEventHandler.onSelect();
                }
                if (!open) {
                	break;
                }
                
                log.log(Level.DEBUG, log.getSB().append("Poll ").append(this.selectionKeys.size()).append(" files with timeout: ").append(timeout));
                int nfds = Native.poll(((sun.nio.ch.DirectBuffer)fdsByteBuffer).address(), this.selectionKeys.size(), intTimeout);
                log.log(Level.DEBUG, log.getSB().append("Poll returned with nfds: ").append(nfds));
                
                currentMicros = Native.currentTimeMicros();
                currentMillis = currentMicros/1000;
                if (nfds == 0) {
                	// A timer expired
                	continue;
                } else if (nfds == -1) {
                    log.log(Level.ERROR, log.getSB().append("EventLoop poll failed, errno: ").append(Native.errno()));
                    break;
                }
                for (int i=0, entry = this.selectionKeys.firstEntry(); entry != -1; i++, entry = this.selectionKeys.nextEntry(entry)) {
                	PollSelectionKey selectionKey = this.selectionKeys.getValue(entry);
                	if (selectionKey.offset < 0) {
                	    continue;
                	}
            		short revent = fdsByteBuffer.getShort(selectionKey.offset);
                    log.log(Level.DEBUG, log.getSB().append("revent: ").append(revent).append(" available for fd: ").append(selectionKey.fd).append(" at entry: ").append(entry).append(" interestOps: ").append(selectionKey.interestOps));
            		if ((revent & Native.POLLIN) != 0) {
                		log.log(Level.DEBUG, log.getSB().append("POLLIN available for fd: ").append(selectionKey.fd).append(" at entry: ").append(entry).append(" interestOps: ").append(selectionKey.interestOps));
            			if ((selectionKey.interestOps & SelectionKey.OP_READ) != 0) {
            			    selectionKey.eventHandler.onRead(selectionKey.selectableChannel);
            			} else if ((selectionKey.interestOps & SelectionKey.OP_ACCEPT) != 0) {
            			    selectionKey.eventHandler.onAccept(selectionKey.selectableChannel);
            			}
            		}
            		if ((revent & Native.POLLOUT) != 0) {
                		log.log(Level.DEBUG, log.getSB().append("POLLOUT available for fd: ").append(selectionKey.fd).append(" at entry: ").append(entry).append(" interestOps: ").append(selectionKey.interestOps));
            			if ((selectionKey.interestOps & SelectionKey.OP_WRITE) != 0) {
            			    selectionKey.eventHandler.onWrite(selectionKey.selectableChannel);
            			} else if ((selectionKey.interestOps & SelectionKey.OP_CONNECT) != 0) {
            			    selectionKey.eventHandler.onConnect(selectionKey.selectableChannel);
            			}
            		}
            		// Socket is in an error state
                    if ((revent & ~(Native.POLLIN | Native.POLLOUT)) != 0 ) {
                    	log.log(Level.ERROR, "Socket is in an error state");
                    	selectionKey.eventHandler.onClose(selectionKey.selectableChannel);
                    }
                }
	        } catch (Throwable e) {
	            log.log(Level.INFO, "EventLoop caught: ", e);
	        }
        }
        log.log(Level.INFO, "Exiting EventLoop");
    }
}
