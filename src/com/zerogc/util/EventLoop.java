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

import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe.SinkChannel;
import java.nio.channels.Pipe.SourceChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * @author Benoit Jardin
 */

public interface EventLoop {
    public interface TimerListener {
        public void onTimer(int entry, long expiry);
    }
    public interface EventLoopListener {
        public void onClose(SelectableChannel selectableChannel);
        public void onAccept(SelectableChannel selectableChannel);
        public void onConnect(SelectableChannel selectableChannel);
        public void onRead(SelectableChannel selectableChannel);
        public void onWrite(SelectableChannel selectableChannel);
        public void onSelect();
    }
    
    public interface EventLoopSelectionKey {
        public int interestOps();
        public void interestOps(int ops);
        public void cancel();
    }
    
    public void open();
    public void close();
    
    public long currentMicros();
    public long currentMillis();

    public EventLoopSelectionKey register(ServerSocketChannel serverSocketChannel, int ops, EventLoopListener eventhandler);
    public EventLoopSelectionKey register(SocketChannel socketChannel, int ops, EventLoopListener eventhandler);
    public EventLoopSelectionKey register(DatagramChannel dataGramChannel, int ops, EventLoopListener eventhandler);
    public EventLoopSelectionKey register(SourceChannel ssourceChannel, int ops, EventLoopListener eventhandler);
    public EventLoopSelectionKey register(SinkChannel sinkChannel, int ops, EventLoopListener eventhandler);

    /** @return the new timer id */
    public int addTimer(long when, TimerListener timerHandler);
    public void cancelTimer(int entry);
    
    public EventLoopListener registerSelect(EventLoopListener eventhandler);
    
    public void run();
    
}
