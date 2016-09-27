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
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.zerogc.util.EventLoop;
import com.zerogc.util.EventLoop.EventLoopListener;
import com.zerogc.util.EventLoop.EventLoopSelectionKey;
import com.zerogc.util.EventLoop.TimerListener;
import com.zerogc.util.Latencies;
import com.zerogc.util.Level;
import com.zerogc.util.Logger;
import com.zerogc.util.Native;
import com.zerogc.util.PollEventLoop;

/**
 * @author Benoit Jardin
 * 
 * COMPILE="-XX:+CITime -XX:+PrintCompilation"
 * java -Djava.library.path=$DEVELOP/zerogc/native -cp $DEVELOP/zerogc/dist/ZeroGC-0.0.0.0.jar com.zerogc.util.Pong -l 36000
 * java -Djava.library.path=$DEVELOP/zerogc/native -cp $DEVELOP/zerogc/dist/ZeroGC-0.0.0.0.jar com.zerogc.util.Pong -d localhost:36000
 */

public class Pong implements EventLoopListener, TimerListener
{
    private static final Logger log = new Logger(Pong.class.getSimpleName());

    private static final int DATA_SIZE = 50;
    
    private EventLoop eventLoop;
    
    private InetSocketAddress sourceInetSocketAddress = null;
    private InetSocketAddress destInetSocketAddress = null;
    private EventLoopSelectionKey destSelectionKey = null;
    private EventLoopSelectionKey sourceSelectionKey = null;
    private SelectableChannel sourceChannel = null;
    private SelectableChannel destChannel = null;
    private SocketAddress sockAddr;
    private int networkInterface = 0;
    
    private final Latencies latencies = new Latencies();

    private boolean fromUdp = false;
    private boolean toUdp = false;
    private long last = 0;
    private int timerDelay = 0;
    private int tokenCount = 1;
    private byte tokens[] = new byte[1024];

    private final ByteBuffer sourceReadBuffer;
    private final ByteBuffer sourceWriteBuffer;
    private final ByteBuffer destReadBuffer;
    private final ByteBuffer destWriteBuffer;
    
    private int tokenTimer;
    private int statTimer = -1;
    private int count;
    private int prevCount;
    
    public Pong(EventLoop eventLoop) {
        log.log(Level.INFO, log.getSB().append("Creating ").append(this.getClass().getSimpleName()));
        this.eventLoop = eventLoop;
        this.sourceReadBuffer = ByteBuffer.allocateDirect(32*1024);
        this.sourceWriteBuffer = ByteBuffer.allocateDirect(32*1024);
        this.destReadBuffer = ByteBuffer.allocateDirect(32*1024);
        this.destWriteBuffer = ByteBuffer.allocateDirect(32*1024);
    }

    private void configureChannel(SocketChannel socketChannel) throws IOException {
        socketChannel.configureBlocking(false);
        socketChannel.socket().setReceiveBufferSize(1024*1024);
        socketChannel.socket().setSendBufferSize(1024*1024);
        socketChannel.socket().setTcpNoDelay(true);
        socketChannel.socket().setSoLinger(true, 0); 
    }
    
    public void open() {
        try {
            if (destInetSocketAddress != null) {
                if (toUdp) {
                    DatagramChannel datagramChannel = DatagramChannel.open();
                    this.destChannel = datagramChannel;
                    datagramChannel.configureBlocking(false);
                    datagramChannel.socket().setReceiveBufferSize(1024*1024);
                    this.destSelectionKey = this.eventLoop.register(datagramChannel, SelectionKey.OP_READ, this);
                } else {
                    SocketChannel socketChannel = SocketChannel.open();
                    //this.destChannel = socketChannel;
                    configureChannel(socketChannel);                    
                    if (socketChannel.connect(this.destInetSocketAddress)) {
                        onConnect(socketChannel);
                    } else {
                        this.destSelectionKey = this.eventLoop.register(socketChannel, SelectionKey.OP_CONNECT, this);
                    }
                }
            }
            if (sourceInetSocketAddress != null) {
                if (fromUdp) {
                    DatagramChannel datagramChannel = DatagramChannel.open();
                    this.sourceChannel = datagramChannel;
                    datagramChannel.configureBlocking(false);
                    datagramChannel.socket().setReuseAddress(true);
                    datagramChannel.socket().bind(sourceInetSocketAddress);
                    if (sourceInetSocketAddress.getAddress().isMulticastAddress()) {
                        log.log(Level.INFO, log.getSB().append("join multicast group: ").append(this.sourceInetSocketAddress.getAddress().hashCode()));
                    	if (Native.joinMcastGroup(datagramChannel, sourceInetSocketAddress.getAddress().hashCode(), networkInterface) == -1) {
                            log.log(Level.ERROR, log.getSB().append("Could not join multicast group: ").append(this.sourceInetSocketAddress));
                        }  
                    }
                    datagramChannel.socket().setReceiveBufferSize(1024*1024);
                    this.sourceSelectionKey = this.eventLoop.register(datagramChannel, SelectionKey.OP_READ, this);
                } else {
                    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                    serverSocketChannel.configureBlocking(false);
                    serverSocketChannel.socket().setReuseAddress(true);
                    serverSocketChannel.socket().bind(this.sourceInetSocketAddress);
                    this.eventLoop.register(serverSocketChannel, SelectionKey.OP_ACCEPT, this);                
                }
            } else {
                for (int i=0; i<tokenCount; i++) {
                    int entry = this.eventLoop.addTimer(this.eventLoop.currentMillis()+100+i, this);
                    tokens[entry] = (byte)i;
                }
            }
        } catch (IOException e) {
            log.log(Level.INFO, log.getSB().append("Could not listen for connection on ").append(this.sourceInetSocketAddress.getAddress().getHostAddress()).append(":").append(this.sourceInetSocketAddress.getPort()));
        }     
    }
    
    public void close() {
        System.exit(0);
    }

    // --------------------- IOHandler ---------------------
    
    @Override
    public void onAccept(SelectableChannel selectableChannel) {
        log.log(Level.INFO, "onAccept()");

        try {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel)selectableChannel;
            SocketChannel socketChannel = serverSocketChannel.accept();
            this.sourceChannel = socketChannel;
            configureChannel(socketChannel);
            this.sourceSelectionKey = this.eventLoop.register(socketChannel, SelectionKey.OP_READ, this);
        } catch (Throwable t) {
            log.log(Level.ERROR, log.getSB().append("Failed to accept connetion on ").append(this.sourceInetSocketAddress), t);
            close();
        }
    }
    
    @Override
    public void onConnect(SelectableChannel selectableChannel) {
        log.log(Level.INFO, "onConnect()");
        try {
            this.destChannel = selectableChannel;
            SocketChannel socketChannel = (SocketChannel)selectableChannel;
            if (this.destSelectionKey == null) {
                this.destSelectionKey = this.eventLoop.register(socketChannel, SelectionKey.OP_READ, this);
            } else {
                if (socketChannel.finishConnect()) {
                    log.log(Level.INFO, log.getSB().append("Established connection to ").append(this.destInetSocketAddress));
                } else {
                    log.log(Level.INFO, "onConnect() failed: ");
                }
                int ops = (this.destSelectionKey.interestOps() & ~SelectionKey.OP_CONNECT) | SelectionKey.OP_READ;
                this.destSelectionKey.interestOps(ops);
            }
        } catch (Throwable t) {
            log.log(Level.ERROR, log.getSB().append("Failed to connect to: ").append(this.destInetSocketAddress), t);
            close();
        }
    }

    @Override
    public void onRead(SelectableChannel selectableChannel) {
        log.log(Level.DEBUG, "onRead()");
        try {
            SelectableChannel readChannel;
            SelectableChannel writeChannel;
            ByteBuffer readBuffer;
            ByteBuffer writeBuffer;
            if (selectableChannel == this.sourceChannel) {
                readChannel = this.sourceChannel;
                readBuffer = this.sourceReadBuffer;
                writeChannel = this.destChannel != null ? this.destChannel : this.sourceChannel;
                writeBuffer = this.destWriteBuffer;
            } else {
                readChannel = this.destChannel;
                readBuffer = this.destReadBuffer;
                writeChannel = this.sourceChannel;
                writeBuffer = this.sourceWriteBuffer;
            }
            SocketAddress sockAddr = null;
            int len = 0;
            if (selectableChannel instanceof SocketChannel) {
                len = ((SocketChannel)selectableChannel).read(readBuffer);
            } else {
                sockAddr = ((DatagramChannel)selectableChannel).receive(readBuffer);
                if (readChannel == this.sourceChannel) {
                    this.sockAddr = sockAddr;
                }
            }
            readBuffer.flip();
            while (readBuffer.remaining() >= 2) {
                int limit = readBuffer.limit();
                int position = readBuffer.position();

                int messageLength = readBuffer.getShort(position);

                if (readBuffer.remaining() < messageLength) {
                    break;
                }

                byte[] buffer = latencies.getBuffer(); 
                for (int i=0; i<messageLength; i++) {
                    buffer[i] = readBuffer.get(position + 2 + i);                
                }
                long deliveryTime = Native.currentTimeMicros();
                latencies.decode(latencies.getBuffer(), DATA_SIZE, messageLength-DATA_SIZE);
                latencies.add(deliveryTime);

                // Set new buffer start to next message.
                readBuffer.position(position + 2 + messageLength);

                // Restore real limit.
                readBuffer.limit(limit);
                
                if (writeChannel != null) {
                    writeBuffer.putShort((short)latencies.getLenth());
                    writeBuffer.put(latencies.getBuffer(), 0, latencies.getLenth());
                    writeBuffer.flip();
                    
                    if (writeChannel instanceof SocketChannel) {
                        len = ((SocketChannel)writeChannel).write(writeBuffer);
                    } else {
                        SocketAddress destSocketAddress = this.destInetSocketAddress;
                        if (readChannel == writeChannel) {
                            destSocketAddress = sockAddr;
                        } else if (writeChannel == this.sourceChannel){
                            destSocketAddress = this.sockAddr;
                        }
                        len = ((DatagramChannel)writeChannel).send(writeBuffer, destSocketAddress);
                    }
                    writeBuffer.compact();
                } else {
                    if (latencies.getDeltaSum() - last > 1000000) {
                        long[] delta = latencies.getDelta();
                        long half = delta[1]+delta[2];
                        long total = latencies.getDeltaSum()-latencies.getDelta()[0];
                        log.log(Level.INFO, latencies.toString(log.getSB()).append(" = ").append(half).append(';').append(total));
                        last = latencies.getDeltaSum();
                    }
                    
                    //log.log(Level.INFO, log.getSB().append("Message"));
                    int entry = this.eventLoop.addTimer(this.eventLoop.currentMillis() + timerDelay, this);
                    tokens[entry] = buffer[0];
                }
            }
            readBuffer.compact();

        } catch (IOException e) {
            log.log(Level.INFO, log.getSB().append("onRead caught: "), e);
            close();
        }
    }
    
    @Override
    public void onWrite(SelectableChannel socketChannel) {
        log.log(Level.DEBUG, "onWrite()");
    }

    private void sendToken(byte token) {
        try {
            latencies.clear();
            byte[] buffer=latencies.getBuffer();
            for (int i=0; i<DATA_SIZE; i++) {
                buffer[i]=token;
            }
            latencies.setLenth(DATA_SIZE);
            latencies.add(Native.currentTimeMicros());
            destWriteBuffer.putShort((short)latencies.getLenth());
            destWriteBuffer.put(latencies.getBuffer(), 0, latencies.getLenth());
            destWriteBuffer.flip();
            
            if (destChannel != null) {
                if (destChannel instanceof SocketChannel) {
                    ((SocketChannel)destChannel).write(destWriteBuffer);
                } else {
                    SocketAddress destSocketAddress = this.destInetSocketAddress;
                    ((DatagramChannel)destChannel).send(destWriteBuffer, destSocketAddress);
                }
            }
            destWriteBuffer.compact();
        } catch (IOException e) {
            log.log(Level.INFO, log.getSB().append("onTimer caught: "), e);
            close();
        }
    }

    @Override
    public void onSelect() {
        count++;
    }

    @Override
    public void onTimer(int entry, long expiry) {
        log.log(Level.DEBUG, "Timer expired");
        if (entry == statTimer) {
            statTimer = this.eventLoop.addTimer(this.eventLoop.currentMillis() + 1000, this);
            int iterations = count - prevCount;
            prevCount = count;
            log.log(Level.INFO, log.getSB().append("Iterations: ").append(iterations));
        } else {
            sendToken(tokens[entry]);
        }
    }
    
    @Override
    public void onClose(SelectableChannel selectableChannel) {
        log.log(Level.INFO, "onClose");
        close();
    }

    
    public void initialize(Logger log, String[] args) throws Exception {
        boolean error = false;
        
        for (int i=0; i < args.length && args[i].startsWith("-");) {
            String arg = args[i++];
            
            if (arg.equals("-l") || arg.equals("-lu")) {
                fromUdp = arg.equals("-lu");
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
                    log.log(Level.ERROR, " -l requires a port");
                    error = true;
                    break;
                }
            } else if (arg.equals("-d") || arg.equals("-du")) {
                toUdp = arg.equals("-du");
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
            } else if (arg.equals("-s")) {
                if (i < args.length) { 
                    timerDelay = Integer.parseInt(args[i++]);
                    log.log(Level.INFO, log.getSB().append("Delay token by ").append(timerDelay).append(" ms"));
                } else {
                    log.log(Level.ERROR, "-s requires a timer delay");
                    error = true;
                    break;
                }
            } else if (arg.equals("-t")) {
                if (i < args.length) { 
                    tokenCount = Integer.parseInt(args[i++]);
                    log.log(Level.INFO, log.getSB().append("Using ").append(tokens.length).append(" simultaneous tokens"));
                } else {
                    log.log(Level.ERROR, "-t requires number of tokens");
                    error = true;
                    break;
                }
            } else if (arg.equals("-v")) {
                eventLoop.registerSelect(this);
                statTimer = this.eventLoop.addTimer(this.eventLoop.currentMillis() + 1000, this);
            } else {
                log.log(Level.ERROR, "Unknown parmeter: " + arg);
                error = true;
            }
        }
        
        if (error || args.length == 0 || (this.destInetSocketAddress == null && this.sourceInetSocketAddress == null)) {
            log.log(Level.ERROR, log.getSB().append("usage: ").append(this.getClass().getSimpleName()).append(" -l <local_port> -d <address:port>"));
            log.log(Level.ERROR, " -l[u] <localport>: Listen for incoming connections on <local_port> UDP");
            log.log(Level.ERROR, " -d[u] <address:port>: Destinations to connect to UDP");
            log.log(Level.ERROR, " -s <delay>: Timer delay");
            log.log(Level.ERROR, " -t <tokens>: Number of tokens in flight");
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        log.setLevel(Level.INFO);
        
        try {
            //EventLoop eventLoop = new SelectorEventLoop();
            //EventLoop eventLoop = new EPollEventLoop();
            EventLoop eventLoop = new PollEventLoop();
            
            Pong pong = new Pong(eventLoop);
            pong.initialize(log, args);
            eventLoop.open();
            pong.open();
            eventLoop.run();
        } catch (Exception e) {
            log.log(Level.ERROR, "Error running application: ", e);
        }
    }
}
