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

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe.SinkChannel;
import java.nio.channels.Pipe.SourceChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import com.zerogc.collections.LongHeap;
import com.zerogc.collections.LongObjectHeap;
import com.zerogc.logging.Level;
import com.zerogc.logging.LogManager;
import com.zerogc.logging.Logger;

/**
 * Portable event loop based on the Java NIO Selector.
 * Beware that the NIO selector generates GC.
 * @author Benoit Jardin
 *
 */
public class SelectorEventLoop implements EventLoop {
    static final Logger log = LogManager.getLogger(SelectorEventLoop.class.getSimpleName());

    private static final int CLOCK_GRANULARITY = 10;

    private Selector selector;
    private LongObjectHeap timers = new LongObjectHeap();
    private long now = System.currentTimeMillis();
    EventLoopListener selectEventHandler = null;

    static class TimerEvents extends LongHeap {
        private long[] key;
        private EventLoopListener[] value;

        @Override
        protected void grow(int capacity, int newCapacity) {
            super.grow(capacity, newCapacity);

            EventLoopListener[] newValue = new EventLoopListener[newCapacity];
            long[] newKey = new long[newCapacity];
            if (capacity > 0) {
                System.arraycopy(this.value, 0, newValue, 0, capacity);
                System.arraycopy(this.key, 0, newKey, 0, capacity);
            }
            this.key = newKey;
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

    private static class SelectorSelectionKey implements EventLoopSelectionKey {
        private SelectionKey selectionKey;
        private EventLoopListener eventLoopHandler;

        public SelectorSelectionKey(SelectionKey selectionKey, EventLoopListener eventLoopHandler) {
            this.selectionKey = selectionKey;
            this.eventLoopHandler = eventLoopHandler;
        }

        @Override
        public void cancel() {
            this.selectionKey.cancel();
        }
        @Override
        public int interestOps() {
            return this.selectionKey.interestOps();
        }

        @Override
        public void interestOps(int ops) {
            this.selectionKey.interestOps(ops);
        }

    }
    // --------------------- Basic event loop ---------------------

    @Override
    public long currentMicros() {
        return now*1000;
    }

    @Override
    public long currentMillis() {
        return now;
    }

    @Override
    public void open() {
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            log.log(Level.INFO, "open caught: ", e);
        }
    }

    @Override
    public void close() {
        try {
            this.selector.close();
        } catch (IOException e) {
            log.log(Level.INFO, "close caught: ", e);
        }
    }

    @Override
    public EventLoopSelectionKey register(ServerSocketChannel serverSocketChannel, int ops, EventLoopListener eventHandler) {
        return registerImpl(serverSocketChannel, ops, eventHandler);
    }

    @Override
    public EventLoopSelectionKey register(SocketChannel socketChannel, int ops, EventLoopListener eventHandler) {
        return registerImpl(socketChannel, ops, eventHandler);
    }

    @Override
    public EventLoopSelectionKey register(DatagramChannel datagramChannel, int ops, EventLoopListener eventHandler) {
        return registerImpl(datagramChannel, ops, eventHandler);
    }

    @Override
    public EventLoopSelectionKey register(SourceChannel sourceChannel, int ops, EventLoopListener eventhandler) {
        return registerImpl(sourceChannel, ops, eventhandler);
    }

    @Override
    public EventLoopSelectionKey register(SinkChannel sinkChannel, int ops, EventLoopListener eventhandler) {
        return registerImpl(sinkChannel, ops, eventhandler);
    }

    private EventLoopSelectionKey registerImpl(SelectableChannel selectableChannel, int ops, EventLoopListener eventHandler) {
        SelectorSelectionKey selectorSelectionKey = null;
        try {
            SelectionKey selectionKey = selectableChannel.register(this.selector, ops);
            selectorSelectionKey = new SelectorSelectionKey(selectableChannel.register(this.selector, ops), eventHandler);
            selectionKey.attach(selectorSelectionKey);
        } catch (ClosedChannelException e) {
            log.log(Level.INFO, "register: ", e);
        }
        return selectorSelectionKey;
    }

    @Override
    public EventLoopListener registerSelect(EventLoopListener eventhandler)
    {
        EventLoopListener oldEventhandler = selectEventHandler;
        selectEventHandler = eventhandler;
        return oldEventhandler;
    }

    @Override
    public int addTimer(long when, TimerListener timerHandler) {
        return this.timers.insert(when, timerHandler);
    }

    @Override
    public void cancelTimer(int entry) {
        log.log(Level.DEBUG, "cancelTimer");
        this.timers.removeEntry(entry);
    }


    @Override
    public void run() {
        log.log(Level.INFO, "Starting EventLoop");

        while (true) {
            try {
                long nextTimer = Long.MIN_VALUE;
                while (!this.timers.isEmpty()) {
                    int entry = this.timers.firstEntry();
                    nextTimer = this.timers.getKey(entry);
                    if (nextTimer - now < CLOCK_GRANULARITY) {
                        // Callbacks may change the timer map, remove timer entry beforehand
                        TimerListener timerHandler = (TimerListener)this.timers.getValue(entry);
                        this.timers.removeEntry(entry);
                        timerHandler.onTimer(entry, now);
                        nextTimer = Long.MIN_VALUE;
                    } else {
                        break;
                    }
                }
                // Callbacks may have been running for some non neglectable time, update current time before calculating timeout
                now = System.currentTimeMillis();
                long timeout = nextTimer - now;
                if (nextTimer == Long.MIN_VALUE) {
                    timeout = 0;
                } else if (timeout < CLOCK_GRANULARITY) {
                    continue;
                }

                if (selectEventHandler != null) {
                    selectEventHandler.onSelect();
                }
                if (!this.selector.isOpen()) {
                    break;
                }

                int keys = this.selector.select(timeout);
                now = System.currentTimeMillis();
                if (keys > 0) {
                    Set<SelectionKey> keySet = this.selector.selectedKeys();
                    Iterator<SelectionKey> it = keySet.iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        SelectorSelectionKey selectorSelectionKey = (SelectorSelectionKey)key.attachment();

                        it.remove();
                        if (key.isValid() && key.isReadable()) {
                            selectorSelectionKey.eventLoopHandler.onRead(key.channel());
                        }
                        if (key.isValid() && key.isWritable()) {
                            selectorSelectionKey.eventLoopHandler.onWrite(key.channel());
                        }
                        if (key.isValid() && key.isConnectable()) {
                            selectorSelectionKey.eventLoopHandler.onConnect(key.channel());
                        }
                        if (key.isValid() && key.isAcceptable()) {
                            selectorSelectionKey.eventLoopHandler.onAccept(key.channel());
                        }
                    }
                }
            } catch (Throwable e) {
                log.log(Level.INFO, "EventLoop caught: ", e);
            }
        }
        log.log(Level.INFO, "Exiting EventLoop");
    }
}
