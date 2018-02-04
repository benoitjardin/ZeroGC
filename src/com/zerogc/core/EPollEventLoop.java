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
import com.zerogc.collections.LongHeap;
import com.zerogc.collections.LongObjectHeap;
import com.zerogc.logging.Level;
import com.zerogc.logging.LogManager;
import com.zerogc.logging.Logger;

/**
 * GC free event loop based on the Linux epoll system call.
 * It is not compatible with Windows (use PollEventLoop instead).
 * @author Benoit Jardin
 */
public class EPollEventLoop implements EventLoop {
    final Logger log = LogManager.getLogger(EPollEventLoop.class.getSimpleName());

    private static final int CLOCK_GRANULARITY = 10;

    private final int epfd;
    private final ByteBuffer eventsByteBuffer;
    private boolean open = false;

    private LongObjectHeap timers = new LongObjectHeap();
    private long currentMicros;
    private long currentMillis;

    private SelectionKeys selectionKeys = new SelectionKeys(SelectionKeys.class.getSimpleName(), Native.MAX_SELECTABLE_FDS);
    EventLoopListener selectEventHandler = null;

    private class EPollSelectionKey implements EventLoopSelectionKey {
        SelectableChannel selectableChannel;
        int fd;
        int interestOps;
        short eventType;
        EventLoopListener eventHandler;

        @Override
        public int interestOps() {
            return this.interestOps;
        }
        private void interestOpsInt(int ops) {
            this.interestOps = ops;
            this.eventType = 0;
            if ((ops & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0) {
                this.eventType |= Native.EPOLLIN;
            }
            if ((ops & (SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT)) != 0) {
                this.eventType |= Native.EPOLLOUT;
            }
        }
        @Override
        public void interestOps(int ops) {
            interestOpsInt(ops);
            Native.epoll_ctl(epfd, Native.EPOLL_CTL_MOD, fd, this.eventType);
        }

        @Override
        public void cancel() {
            // Mark the selection key for later removal
            this.interestOps = 0;
            this.eventType = 0;
            this.eventHandler = null;
            if (Native.epoll_ctl(epfd, Native.EPOLL_CTL_DEL, fd, this.eventType) != 0) {
                 log.log(Level.ERROR, log.getSB().append("epoll_ctl failed"));
            }
        }

    };

    // Map between file descriptor to selection key
    class SelectionKeys extends IntRbTree {
        private EPollSelectionKey[] value;

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

            EPollSelectionKey[] newValue = new EPollSelectionKey[newCapacity];
            if (capacity > 0) {
                System.arraycopy(this.value, 0, newValue, 0, capacity);
            }
            for (int i=capacity; i<newCapacity; i++) {
                newValue[i] = new EPollSelectionKey();
            }
            this.value = newValue;
        }

        public EPollSelectionKey getValue(int entry) {
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

    public EPollEventLoop() {
        currentMicros = Native.currentTimeMicros();
        currentMillis = currentMicros/1000;
        eventsByteBuffer = ByteBuffer.allocateDirect(Native.MAX_SELECTABLE_FDS * Native.EPOLLEVENT_SIZE);
        eventsByteBuffer.order(ByteOrder.nativeOrder());
        epfd = Native.epoll_create(256);
        if (epfd < 0) {
            log.log(Level.ERROR, log.getSB().append("epoll_create failed"));
        }
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

        EPollSelectionKey selectionKey = this.selectionKeys.getValue(entry);
        selectionKey.fd = fd;
        selectionKey.selectableChannel = selectableChannel;
        selectionKey.eventHandler = eventHandler;
        selectionKey.interestOpsInt(ops);
        if (Native.epoll_ctl(epfd, Native.EPOLL_CTL_ADD, fd, selectionKey.eventType) != 0) {
            log.log(Level.ERROR, log.getSB().append("epoll_ctl failed"));
        }
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
    public int addTimer(long when, TimerListener timerHandler) {
        log.log(Level.DEBUG, "addTimer");
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

                for (int entry = this.selectionKeys.firstEntry(), i = 0; entry != -1; i++) {
                    EPollSelectionKey selectionKey = this.selectionKeys.getValue(entry);
                    if (selectionKey.eventHandler == null) {
                        log.log(Level.DEBUG, log.getSB().append("Remove selectionKey for fd: ").append(selectionKey.fd).append(" at entry: ").append(entry));
                        entry = selectionKeys.removeEntry(entry);
                    } else {
                        entry = selectionKeys.nextEntry(entry);
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

                if (selectEventHandler != null) {
                    selectEventHandler.onSelect();
                }
                if (!open) {
                    break;
                }

                log.log(Level.DEBUG, log.getSB().append("EPoll ").append(this.selectionKeys.size()).append(" files with timeout: ").append(timeout));
                this.eventsByteBuffer.clear();
                int nfds = Native.epoll_wait(epfd, ((sun.nio.ch.DirectBuffer)eventsByteBuffer).address(), this.selectionKeys.size(), intTimeout);
                log.log(Level.DEBUG, log.getSB().append("EPoll returned with nfds: ").append(nfds));

                currentMicros = Native.currentTimeMicros();
                currentMillis = currentMicros/1000;
                if (nfds == 0) {
                    // A timer expired
                    continue;
                } else if (nfds == -1) {
                    log.log(Level.ERROR, log.getSB().append("EventLoop epoll_wait failed, errno: ").append(Native.errno()));
                    break;
                }
                for (int i=0; i < nfds; i++) {
                    int offset = i*Native.EPOLLEVENT_SIZE;
                    int fd = eventsByteBuffer.getInt(offset + Native.EPOLLEVENT_FD_OFFSET);
                    int events = eventsByteBuffer.getInt(offset + Native.EPOLLEVENT_EVENTS_OFFSET);
                    int entry = this.selectionKeys.find(fd);
                    EPollSelectionKey selectionKey = this.selectionKeys.getValue(entry);
                    log.log(Level.DEBUG, log.getSB().append("events: ").append(events).append(" available for fd: ").append(fd).append(" at entry: ").append(entry).append(" interestOps: ").append(selectionKey.interestOps));
                    if (selectionKey.eventHandler == null) {
                        continue;
                    }
                    if ((events & Native.EPOLLIN) != 0) {
                        log.log(Level.DEBUG, log.getSB().append("POLLIN available for fd: ").append(fd).append(" at entry: ").append(entry).append(" interestOps: ").append(selectionKey.interestOps));
                        if ((selectionKey.interestOps & SelectionKey.OP_READ) != 0) {
                            selectionKey.eventHandler.onRead(selectionKey.selectableChannel);
                        } else if ((selectionKey.interestOps & SelectionKey.OP_ACCEPT) != 0) {
                            selectionKey.eventHandler.onAccept(selectionKey.selectableChannel);
                        }
                    }
                    if ((events & Native.EPOLLOUT) != 0) {
                        log.log(Level.DEBUG, log.getSB().append("POLLOUT available for fd: ").append(fd).append(" at entry: ").append(entry).append(" interestOps: ").append(selectionKey.interestOps));
                        if ((selectionKey.interestOps & SelectionKey.OP_WRITE) != 0) {
                            selectionKey.eventHandler.onWrite(selectionKey.selectableChannel);
                        } else if ((selectionKey.interestOps & SelectionKey.OP_CONNECT) != 0) {
                            selectionKey.eventHandler.onConnect(selectionKey.selectableChannel);
                        }
                    }
                    // Socket is in an error state
                    if ((events & ~(Native.EPOLLIN | Native.EPOLLOUT)) != 0 ) {
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
