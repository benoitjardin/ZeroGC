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
package com.zerogc.core;

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
import com.zerogc.collections.LongObjectHeap;
import com.zerogc.logging.Level;
import com.zerogc.logging.LogManager;
import com.zerogc.logging.Logger;

/**
 * Event loop based on the BSD UNIX select system call.
 * It is not compatible with the Windows implementation of select (use PollEventLoop instead).
 * @author Benoit Jardin
 */
public class SelectEventLoop implements EventLoop {
    static Logger log = LogManager.getLogger(SelectEventLoop.class.getSimpleName());

    private static final int CLOCK_GRANULARITY = 10;

    private final ByteBuffer readfdsByteBuffer;
    private final ByteBuffer writefdsByteBuffer;
    private final ByteBuffer exceptfdsByteBuffer;
    private boolean open = false;

    private LongObjectHeap timers = new LongObjectHeap();
    private long currentMicros;
    private long currentMillis;

    private SelectionKeys selectionKeys = new SelectionKeys(SelectionKeys.class.getSimpleName(), Native.MAX_SELECTABLE_FDS);
    EventLoopListener selectEventHandler = null;

    // Helpers ala POSIX
    private static int __NFDBITS = 8;
    private static int __FDELT(int fd) {
        return fd/__NFDBITS;
    }
    private static byte __FDMASK(int fd) {
        return (byte)(1 << (fd%__NFDBITS));
    }
    public static void FD_ZERO(ByteBuffer buffer) {
        buffer.clear();
    }
    public static void FD_CLR(int fd, ByteBuffer buffer) {
        int elt = __FDELT(fd);
        byte mask = (byte)(buffer.get(elt) & ~__FDMASK(fd));
        buffer.put(elt, mask);
    }
    public static void FD_SET(int fd, ByteBuffer buffer) {
        int elt = __FDELT(fd);
        byte mask = (byte)(buffer.get(elt) | __FDMASK(fd));
        buffer.put(elt, mask);
    }
    public static boolean FD_ISSET(int fd, ByteBuffer buffer) {
        int elt = __FDELT(fd);
        byte mask = (byte)(buffer.get(elt) & __FDMASK(fd));
        return mask != 0;
    }



    private class SelectSelectionKey implements EventLoopSelectionKey {
        SelectableChannel selectableChannel;
        int fd;
        int interestOps;
        EventLoopListener eventHandler;

        @Override
        public int interestOps() {
            return this.interestOps;
        }

        @Override
        public void interestOps(int ops) {
            this.interestOps = ops;
        }

        @Override
        public void cancel() {
            // Mark the selection key for later removal
            this.interestOps = 0;
        }
    };

    // Map between file descriptor to selection key
    class SelectionKeys extends IntRbTree {
        private SelectSelectionKey[] value;

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

            SelectSelectionKey[] newValue = new SelectSelectionKey[newCapacity];
            if (capacity > 0) {
                System.arraycopy(this.value, 0, newValue, 0, capacity);
            }
            for (int i=capacity; i<newCapacity; i++) {
                newValue[i] = new SelectSelectionKey();
            }
            this.value = newValue;
        }

        public SelectSelectionKey getValue(int entry) {
            return this.value[entry];
        }

        public int put(int key) {
            int entry = insert(key);
            return entry;
        }
    }


    public SelectEventLoop() {
        currentMicros = Native.currentTimeMicros();
        currentMillis = currentMicros/1000;
        readfdsByteBuffer = ByteBuffer.allocateDirect(Native.MAX_SELECTABLE_FDS/Byte.SIZE);
        readfdsByteBuffer.order(ByteOrder.nativeOrder());
        writefdsByteBuffer = ByteBuffer.allocateDirect(Native.MAX_SELECTABLE_FDS/Byte.SIZE);
        writefdsByteBuffer.order(ByteOrder.nativeOrder());
        exceptfdsByteBuffer = ByteBuffer.allocateDirect(Native.MAX_SELECTABLE_FDS/Byte.SIZE);
        exceptfdsByteBuffer.order(ByteOrder.nativeOrder());
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
    public EventLoopSelectionKey register(SourceChannel sourceChannel, int ops, EventLoopListener eventhandler) {
        int fd = Native.getFdVal_SourceChannel(sourceChannel);
        return registerImpl(sourceChannel, fd, ops, eventhandler);
    }

    @Override
    public EventLoopSelectionKey register(SinkChannel sinkChannel, int ops, EventLoopListener eventhandler) {
        int fd = Native.getFdVal_SinkChannel(sinkChannel);
        return registerImpl(sinkChannel, fd, ops, eventhandler);
    }

    @Override
    public EventLoopSelectionKey register(DatagramChannel datagramChannel, int ops, EventLoopListener eventHandler) {
        int fd = Native.getFdVal_DatagramChannel(datagramChannel);
        return registerImpl(datagramChannel, fd, ops, eventHandler);
    }

    private EventLoopSelectionKey registerImpl(SelectableChannel selectableChannel, int fd, int ops, EventLoopListener eventHandler) {
        int entry = this.selectionKeys.put(fd);

        log.log(Level.DEBUG, log.getSB().append("Register fd: ").append(fd).append(" at entry: ").append(entry).append(" for ops: ").append(ops));

        SelectSelectionKey selectionKey = this.selectionKeys.getValue(entry);
        selectionKey.fd = fd;
        selectionKey.selectableChannel = selectableChannel;
        selectionKey.eventHandler = eventHandler;
        selectionKey.interestOps(ops);
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

                int _nfds = 0;
                this.readfdsByteBuffer.clear();
                this.writefdsByteBuffer.clear();
                this.exceptfdsByteBuffer.clear();
                for (int entry = this.selectionKeys.firstEntry(), i = 0; entry != -1; i++) {
                    SelectSelectionKey selectionKey = this.selectionKeys.getValue(entry);
                    if (selectionKey.interestOps != 0) {
                        log.log(Level.DEBUG, log.getSB().append("Select register fd: ").append(selectionKey.fd).append(" at entry: ").append(entry).append(" for event: ").append(selectionKey.interestOps));
                        switch (selectionKey.interestOps) {
                        case SelectionKey.OP_READ:
                        case SelectionKey.OP_ACCEPT:
                            FD_SET(selectionKey.fd, readfdsByteBuffer);
                            break;
                        case SelectionKey.OP_CONNECT:
                        case SelectionKey.OP_WRITE:
                            FD_SET(selectionKey.fd, writefdsByteBuffer);
                            break;
                        }
                        if (selectionKey.fd >= _nfds) {
                            _nfds = selectionKey.fd + 1;
                        }
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
                    timeout = Long.MIN_VALUE;
                } else if (timeout < CLOCK_GRANULARITY) {
                    timeout = 0;
                }

                if (selectEventHandler != null) {
                    selectEventHandler.onSelect();
                }
                if (!open) {
                    break;
                }

                log.log(Level.DEBUG, log.getSB().append("Select ").append(this.selectionKeys.size()).append(" files with timeout: ").append(timeout));
                int nfds = Native.select(_nfds, ((sun.nio.ch.DirectBuffer)readfdsByteBuffer).address(), ((sun.nio.ch.DirectBuffer)writefdsByteBuffer).address(), ((sun.nio.ch.DirectBuffer)exceptfdsByteBuffer).address(), timeout);
                log.log(Level.DEBUG, log.getSB().append("Select returned with nfds: ").append(nfds));

                currentMicros = Native.currentTimeMicros();
                currentMillis = currentMicros/1000;
                if (nfds == 0) {
                    // A timer expired
                    continue;
                } else if (nfds == -1) {
                    log.log(Level.ERROR, log.getSB().append("EventLoop select failed, errno: ").append(Native.errno()));
                    break;
                }
                for (int i=0, entry = this.selectionKeys.firstEntry(); entry != -1; i++, entry = this.selectionKeys.nextEntry(entry)) {
                    SelectSelectionKey selectionKey = this.selectionKeys.getValue(entry);
                    if (selectionKey.interestOps == 0) {
                        continue;
                    }
                    switch (selectionKey.interestOps) {
                      case SelectionKey.OP_READ:
                        if (FD_ISSET(selectionKey.fd, readfdsByteBuffer)) {
                            selectionKey.eventHandler.onRead(selectionKey.selectableChannel);
                        }
                        break;
                      case SelectionKey.OP_ACCEPT:
                        if (FD_ISSET(selectionKey.fd, readfdsByteBuffer)) {
                            selectionKey.eventHandler.onAccept(selectionKey.selectableChannel);
                        }
                        break;
                      case SelectionKey.OP_CONNECT:
                        if (FD_ISSET(selectionKey.fd, writefdsByteBuffer)) {
                            selectionKey.eventHandler.onConnect(selectionKey.selectableChannel);
                        }
                        break;
                      case SelectionKey.OP_WRITE:
                        if (FD_ISSET(selectionKey.fd, writefdsByteBuffer)) {
                            selectionKey.eventHandler.onWrite(selectionKey.selectableChannel);
                        }
                        break;
                    }
                }
            } catch (Throwable e) {
                log.log(Level.INFO, "EventLoop caught: ", e);
            }
        }
        log.log(Level.INFO, "Exiting EventLoop");
    }
}
