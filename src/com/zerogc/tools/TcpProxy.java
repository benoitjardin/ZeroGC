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
package com.zerogc.tools;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.zerogc.core.EPollEventLoop;
import com.zerogc.core.EventLoop;
import com.zerogc.core.PollEventLoop;
import com.zerogc.core.SelectorEventLoop;
import com.zerogc.core.EventLoop.EventLoopListener;
import com.zerogc.core.EventLoop.EventLoopSelectionKey;
import com.zerogc.core.EventLoop.TimerListener;
import com.zerogc.logging.Level;
import com.zerogc.logging.LogManager;
import com.zerogc.logging.Logger;

/*
 DEVELOP=/home/macgarden/develop
 HEAPTRACKER="-agentpath:$DEVELOP/zerogc/native/libheapTracker.so -Xbootclasspath/a:$DEVELOP/zerogc/native/heapTracker.jar"
 DEBUG=" -Xdebug -agentlib:jdwp=transport=dt_socket,server=y,address=36000,suspend=y"

 java -Djava.library.path=$DEVELOP/zerogc/native -cp $DEVELOP/zerogc/dist/ZeroGC-0.0.0.0.jar com.zerogc.util.TcpProxy -l 36000 -d localhost:36001
 java -Xbootclasspath/a:$DEVELOP/zerogc/dist/ZeroGC-0.0.0.0.jar com.zerogc.util.TcpProxy -l 36000 -d localhost:36001
*/

public class TcpProxy implements EventLoopListener, TimerListener {
    private static final Logger log = LogManager.getLogger(TcpProxy.class.getSimpleName());

    private EventLoop eventLoop;
    private ServerSocketChannel serverSocketChannel = null;

    private InetSocketAddress sourceInetSocketAddress = null;
    private SocketChannel sourceSocketChannel = null;
    private EventLoopSelectionKey sourceSelectionKey = null;

    private InetSocketAddress destInetSocketAddress = null;
    private SocketChannel destSocketChannel = null;
    private EventLoopSelectionKey destSelectionKey = null;

    private final ByteBuffer sourceBuffer;
    private final ByteBuffer destBuffer;

    public TcpProxy(EventLoop eventLoop) {
        log.log(Level.INFO, log.getSB().append("Creating ").append(this.getClass().getSimpleName()));
        this.eventLoop = eventLoop;
        this.sourceBuffer = ByteBuffer.allocateDirect(32*1024);
        this.destBuffer = ByteBuffer.allocateDirect(32*1024);
    }

    public TcpProxy(TcpProxy tcpProxy) {
        this(tcpProxy.eventLoop);
        this.destInetSocketAddress = tcpProxy.destInetSocketAddress;
    }

    public void initialize(Logger log, String[] args) throws Exception {
        boolean error = false;

        for (int i=0; i < args.length && args[i].startsWith("-");) {
            String arg = args[i++];

            if (arg.equals("-l")) {
                if (i < args.length && !args[i].startsWith("-")) {
                    String host = args[i++];
                    int pos = host.indexOf(":");
                    if (pos != -1) {
                        int port = Integer.parseInt(host.substring(pos+1));
                        host = host.substring(0, pos);
                        this.sourceInetSocketAddress = new InetSocketAddress(host, port);
                    } else {
                        int port = Integer.parseInt(host);
                        this.sourceInetSocketAddress = new InetSocketAddress(port);
                    }
                    log.log(Level.INFO, log.getSB().append("Listen for connections on ").append(this.sourceInetSocketAddress));
                } else {
                    log.log(Level.ERROR, " -l requires a TCP port");
                    error = true;
                    break;
                }
            } else if (arg.equals("-d")) {
                if (i < args.length && !args[i].startsWith("-")) {
                    String host = args[i++];
                    int port = -1;
                    int pos = host.indexOf(":");
                    if (pos != -1) {
                        port = Integer.parseInt(host.substring(pos+1));
                        host = host.substring(0, pos);
                        this.destInetSocketAddress = new InetSocketAddress(host, port);
                        log.log(Level.INFO, log.getSB().append("Forward connection to ").append(this.destInetSocketAddress));
                    } else {
                        log.log(Level.ERROR, "Unexpected destination format <address:port>");                        
                    }
                } else {
                    log.log(Level.ERROR, "-d requires a list of destination");
                    error = true;
                    break;
                }
            } else {
                log.log(Level.ERROR, "Unknown parmeter: " + arg);
                error = true;
            }
        }

        if (error || args.length == 0 || this.destInetSocketAddress == null || this.sourceInetSocketAddress == null) {
            log.log(Level.ERROR, log.getSB().append("usage: ").append(this.getClass().getSimpleName()).append(" -l <local_port> -d <address:port>"));
            log.log(Level.ERROR, " -l <localport>: Listen for incoming connections on <local_port>");
            log.log(Level.ERROR, " -d <address:port>: destinations to connect to");
            System.exit(1);
        }
   }

    private void configureTcpChannel(SocketChannel socketChannel) throws IOException {
        socketChannel.socket().setReceiveBufferSize(1024*1024);
        socketChannel.socket().setSendBufferSize(1024*1024);
        socketChannel.socket().setTcpNoDelay(true);
        socketChannel.socket().setSoLinger(true, 0); 
    }

    public void open() {
        try {
            this.serverSocketChannel = ServerSocketChannel.open();
            this.serverSocketChannel.configureBlocking(false);
            this.serverSocketChannel.socket().setReuseAddress(true);

            this.serverSocketChannel.socket().bind(this.sourceInetSocketAddress);
            this.eventLoop.register(this.serverSocketChannel, SelectionKey.OP_ACCEPT, this);
        } catch (IOException e) {
            log.log(Level.INFO, log.getSB().append("Could not listen for connection on ").append(this.sourceInetSocketAddress.getAddress().getHostAddress()).append(":").append(this.sourceInetSocketAddress.getPort()));
        }
    }

    public void close() {
        if (this.sourceSelectionKey != null) {
            this.sourceSelectionKey.cancel();
            this.sourceSelectionKey = null;
        }
        if (this.sourceSocketChannel.isOpen()) {
            log.log(Level.INFO, "Close connection to " + this.sourceInetSocketAddress);
            try {
                this.sourceSocketChannel.close();
            } catch (IOException e) {
                log.log(Level.INFO, "IOException", e);
            }
        }
        if (this.destSelectionKey != null) {
            this.destSelectionKey.cancel();
            this.destSelectionKey = null;
        }
        if (this.destSocketChannel.isOpen()) {
            log.log(Level.INFO, "Close connection to " + this.destInetSocketAddress);
            try {
                this.destSocketChannel.close();
            } catch (IOException e) {
                log.log(Level.INFO, "IOException", e);
            }
        }
    }

    // --------------------- IOHandler ---------------------

    public void onAccept(SelectableChannel selectableChannel) {
        log.log(Level.INFO, "onAccept()");
        TcpProxy tcpProxy = new TcpProxy(this);
        tcpProxy.forward(selectableChannel);
    }

    public void forward(SelectableChannel selectableChannel) {
        try {
            this.sourceBuffer.clear();
            this.destBuffer.clear();

            this.serverSocketChannel = (ServerSocketChannel)selectableChannel;
            this.sourceSocketChannel = serverSocketChannel.accept();
            this.sourceSocketChannel.configureBlocking(false);
            configureTcpChannel(this.sourceSocketChannel);

            log.log(Level.INFO, log.getSB().append("Open connection to ").append(this.destInetSocketAddress));

            this.destSocketChannel = SocketChannel.open();
            this.destSocketChannel.configureBlocking(false);
            configureTcpChannel(this.destSocketChannel);
            if (this.destSocketChannel.connect(this.destInetSocketAddress)) {
                onConnect(this.destSocketChannel);
            } else {
                this.destSelectionKey = this.eventLoop.register(this.destSocketChannel, SelectionKey.OP_CONNECT, this);
            }
        } catch (Throwable t) {
            log.log(Level.ERROR, log.getSB().append("Failed to accept connetion on ").append(this.sourceInetSocketAddress), t);
            close();
        }
    }

    public void onConnect(SelectableChannel selectableChannel) {
        log.log(Level.INFO, "onConnect()");
        try {
            if (this.destSelectionKey == null) {
                this.destSelectionKey = this.eventLoop.register(this.destSocketChannel, SelectionKey.OP_READ, this);
            } else {
                if (this.destSocketChannel.finishConnect()) {
                    log.log(Level.INFO, log.getSB().append("Established connection to ").append(this.destInetSocketAddress));
                } else {
                    log.log(Level.INFO, "onConnect() failed: ");
                }
                int ops = (this.destSelectionKey.interestOps() & ~SelectionKey.OP_CONNECT) | SelectionKey.OP_READ;
                this.destSelectionKey.interestOps(ops);
            }
            this.sourceSelectionKey = this.eventLoop.register(this.sourceSocketChannel, SelectionKey.OP_READ, this);
        } catch (Throwable t) {
            log.log(Level.ERROR, log.getSB().append("Failed to connect to: ").append(this.destInetSocketAddress), t);
            close();
        }
    }

    public void onRead(SelectableChannel socketChannel) {
        log.log(Level.DEBUG, "onRead()");
        try {
            SocketChannel readSocketChannel;
            EventLoopSelectionKey readSelectionKey;
            EventLoopSelectionKey writeSelectionKey;
            ByteBuffer buffer;
            if (socketChannel == this.sourceSocketChannel) {
                readSocketChannel = this.sourceSocketChannel;
                readSelectionKey = this.sourceSelectionKey;
                writeSelectionKey = this.destSelectionKey;
                buffer = this.sourceBuffer;
            } else {
                readSocketChannel = this.destSocketChannel;
                readSelectionKey = this.destSelectionKey;
                writeSelectionKey = this.sourceSelectionKey;
                buffer = this.destBuffer;
            }
            int len = readSocketChannel.read(buffer);
            if (len < 0) {
                log.log(Level.ERROR, log.getSB().append("Detected failure of session"));
                close();
                return;
            } else {
                log.log(Level.DEBUG, log.getSB().append("Read ").append(len).append(" bytes"));
            }
            updateInterestOps(readSelectionKey, writeSelectionKey, buffer);
        } catch (Throwable e) {
            log.log(Level.ERROR, log.getSB().append("onRead() caught: "), e);
            close();
        }
    }

    private void updateInterestOps(EventLoopSelectionKey readSelectionKey, EventLoopSelectionKey writeSelectionKey, ByteBuffer buffer) {
        if (buffer.hasRemaining()) {
            readSelectionKey.interestOps(readSelectionKey.interestOps() | SelectionKey.OP_READ);
        } else {
            readSelectionKey.interestOps(readSelectionKey.interestOps() & ~SelectionKey.OP_READ);
        }
        if (buffer.position() > 0) {
            writeSelectionKey.interestOps(writeSelectionKey.interestOps() | SelectionKey.OP_WRITE);
        } else {
            writeSelectionKey.interestOps(writeSelectionKey.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    public void onWrite(SelectableChannel socketChannel) {
        log.log(Level.DEBUG, "onWrite()");
        try {
            SocketChannel writeSocketChannel;
            EventLoopSelectionKey readSelectionKey;
            EventLoopSelectionKey writeSelectionKey;
            ByteBuffer buffer;
            if (socketChannel == this.sourceSocketChannel) {
                readSelectionKey = this.destSelectionKey;
                writeSocketChannel = this.sourceSocketChannel;
                writeSelectionKey = this.sourceSelectionKey;
                buffer = this.destBuffer;
            } else {
                readSelectionKey = this.sourceSelectionKey;
                writeSocketChannel = this.destSocketChannel;
                writeSelectionKey = this.destSelectionKey;
                buffer = this.sourceBuffer;
            }
            buffer.flip();
            int len = writeSocketChannel.write(buffer);
            if (len < 0) {
                log.log(Level.ERROR, log.getSB().append("Detected failure of session"));
                close();
                return;
            } else {
                log.log(Level.DEBUG, log.getSB().append("Written ").append(len).append(" bytes"));
            }
            buffer.compact();
            updateInterestOps(readSelectionKey, writeSelectionKey, buffer);
        } catch (Throwable t) {
            log.log(Level.INFO, "onWrite() caught: ", t);
            close();
        }
    }

    @Override
    public void onTimer(int entry, long expiry) {
        log.log(Level.ERROR, "Timer expired");
    }

    @Override
    public void onSelect() {
    }

    public void onClose(SelectableChannel selectableChannel) {
        log.log(Level.INFO, "onClose");
        close();
    }

    public static void main(String[] args) {
        log.setLevel(Level.INFO);

        try {
            EventLoop eventLoop = new EPollEventLoop();

            TcpProxy tcpProxy = new TcpProxy(eventLoop);
            tcpProxy.initialize(log, args);
            eventLoop.open();
            tcpProxy.open();
            eventLoop.run();
        } catch (Exception e) {
            log.log(Level.ERROR, "Error running application: ", e);
        }
    }
}
