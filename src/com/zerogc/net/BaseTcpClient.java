package com.zerogc.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.zerogc.core.ByteSlice;
import com.zerogc.core.EventLoop;
import com.zerogc.core.EventLoop.EventLoopListener;
import com.zerogc.core.EventLoop.EventLoopSelectionKey;
import com.zerogc.core.EventLoop.TimerListener;
import com.zerogc.logging.Level;
import com.zerogc.logging.LogManager;
import com.zerogc.logging.Logger;

public class BaseTcpClient implements EventLoopListener, TimerListener, MessageSource  {
    protected final Logger log;

    protected final EventLoop eventLoop;

    private InetSocketAddress[] destInetSocketAddress = null;
    private int destIndex = 0;

    private SocketChannel socketChannel = null;
    private EventLoopSelectionKey selectionKey = null;

    private MessageListener messageListener;
    private ConnectionListener connectionListener;

    private int connectTimeout = 5000;
    private int connectTimer = -1;
    private int reconnectTimeout = 15000;
    private int reconnectTimer = -1;

    private long sendTime;

    private byte[] name;
    private long captureTime;
    private byte[] srcAddrBytes;
    private int srcPort;
    private byte[] dstAddrBytes;
    private int dstPort;
    private final ByteBuffer bufferIn;
    private final ByteBuffer bufferOut;

    public BaseTcpClient(EventLoop eventLoop, byte[] name) {
        this.log = LogManager.getLogger(name);
        this.eventLoop = eventLoop;
        this.name = name;
        this.bufferIn = ByteBuffer.allocate(32*1024);
        this.bufferOut = ByteBuffer.allocate(32*1024);
    }

    // --------------------- Configuration ---------------------

    public MessageListener getMessageListener() {
        return messageListener;
    }
    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public ConnectionListener getConnectionListener() {
        return connectionListener;
    }
    public void setConnectionListener(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    public InetSocketAddress[] getDestInetSocketAddress() {
        return destInetSocketAddress;
    }
    public void setDestInetSocketAddress(InetSocketAddress[] destInetSocketAddress) {
        this.destInetSocketAddress = destInetSocketAddress;
    }

    private void configureTcpChannel(SocketChannel socketChannel) throws IOException {
        socketChannel.socket().setReceiveBufferSize(1024*1024);
        socketChannel.socket().setSendBufferSize(1024*1024);
        socketChannel.socket().setTcpNoDelay(true);
        socketChannel.socket().setSoLinger(true, 0);
    }

    public void open() {
        try {
            this.bufferIn.clear();
            this.bufferOut.clear();

            log.log(Level.INFO, log.getSB().append("Open connection to ").append(this.destInetSocketAddress[destIndex]));

            this.socketChannel = SocketChannel.open();
            this.socketChannel.configureBlocking(false);
            configureTcpChannel(this.socketChannel);
            if (this.socketChannel.connect(this.destInetSocketAddress[destIndex])) {
                onConnect(this.socketChannel);
            } else {
                this.selectionKey = this.eventLoop.register(this.socketChannel, SelectionKey.OP_CONNECT, this);
                if (connectTimer != -1) {
                    eventLoop.cancelTimer(connectTimer);
                    connectTimer = -1;
                }
                // Timeout connection establishment
                connectTimer = eventLoop.addTimer(eventLoop.currentMillis() + connectTimeout, this);
            }
        } catch (Throwable t) {
            log.log(Level.ERROR, log.getSB().append("Failed to connet to ").append(this.destInetSocketAddress[destIndex]), t);
            reconnect();
        }
    }

    public boolean isOpen() {
        return this.socketChannel.isOpen();
    }

    public void close() {
        if (this.selectionKey != null) {
            this.selectionKey.cancel();
            this.selectionKey = null;
        }
        if (this.socketChannel.isOpen()) {
            log.log(Level.INFO, "Close connection to " + this.destInetSocketAddress[destIndex]);
            try {
                this.socketChannel.close();
            } catch (IOException e) {
                log.log(Level.INFO, "IOException", e);
            }
            if (connectionListener != null) {
                connectionListener.onClose(socketChannel);
            }
        }

        if (connectTimer != -1) {
            eventLoop.cancelTimer(connectTimer);
            connectTimer = -1;
        }
        if (reconnectTimer != -1) {
            eventLoop.cancelTimer(reconnectTimer);
            reconnectTimer = -1;
        }
    }

    // --------------------- MessageSource ---------------------

    public long getSendTime() {
        return this.sendTime;
    }

    @Override
    public ByteSlice getName(ByteSlice slice) {
        return slice.set(name, 0, name.length);
    }
    @Override
    public long getCaptureTime() {
        return this.captureTime;
    }
    @Override
    public byte[] getSrcAddrBytes() {
        return srcAddrBytes;
    }
    @Override
    public int getSrcPort() {
        return srcPort;
    }
    @Override
    public byte[] getDstAddrBytes() {
        return dstAddrBytes;
    }
    @Override
    public int getDstPort() {
        return dstPort;
    }
    @Override
    public ByteBuffer getBufferIn() {
        return bufferIn;
    }
    public ByteBuffer getBufferOut() {
        return bufferOut;
    }

    // --------------------- IOHandler ---------------------

    @Override
    public void onConnect(SelectableChannel selectableChannel) {
        log.log(Level.INFO, "onConnect()");
        try {
            this.captureTime = eventLoop.currentMillis();
            if (connectTimer != -1) {
                eventLoop.cancelTimer(connectTimer);
                connectTimer = -1;
            }
            if (this.selectionKey == null) {
                this.selectionKey = this.eventLoop.register(this.socketChannel, SelectionKey.OP_READ, this);
            } else {
                if (this.socketChannel.finishConnect()) {
                    log.log(Level.INFO, log.getSB().append("Established connection to ").append(this.destInetSocketAddress[destIndex]));
                } else {
                    log.log(Level.WARN, "finishConnect() failed");
                }
                int ops = (this.selectionKey.interestOps() & ~SelectionKey.OP_CONNECT) | SelectionKey.OP_READ;
                this.selectionKey.interestOps(ops);
            }

            this.srcAddrBytes = this.socketChannel.socket().getLocalAddress().getAddress();
            this.srcPort = this.socketChannel.socket().getLocalPort();
            this.dstAddrBytes = this.socketChannel.socket().getInetAddress().getAddress();
            this.dstPort = this.socketChannel.socket().getPort();

            if (connectionListener != null) {
                connectionListener.onConnect(selectableChannel);
            }
        } catch (Throwable t) {
            log.log(Level.ERROR, log.getSB().append("Failed to connect to: ").append(this.destInetSocketAddress[destIndex]), t);
            reconnect();
        }
    }

    @Override
    public void onRead(SelectableChannel selectableChannel) {
        log.log(Level.DEBUG, "onRead()");
        try {
            int len = socketChannel.read(bufferIn);
            if (len < 0) {
                log.log(Level.ERROR, log.getSB().append("Detected failure of TCP connection"));
                reconnect();
                return;
            } else {
                log.log(Level.DEBUG, log.getSB().append("Read ").append(len).append(" bytes"));
            }
            this.captureTime = eventLoop.currentMillis();
            dispatch();
        } catch (Throwable e) {
            log.log(Level.ERROR, log.getSB().append("onRead() caught: "), e);
            reconnect();
        }
    }

    @Override
    public void onWrite(SelectableChannel selectableChannel) {
        sendBufferOut();
    }

    public int sendBufferOut() {
        int len = -1;
        try {
            bufferOut.flip();
            len = socketChannel.write(bufferOut);
            this.sendTime = eventLoop.currentMillis();
            if (len < 0) {
                log.log(Level.ERROR, log.getSB().append("Detected failure of TCP connection"));
                reconnect();
            } else {
                log.log(Level.DEBUG, log.getSB().append("Sent ").append(len).append(" bytes"));
            }
            bufferOut.compact();
            int ops = this.selectionKey.interestOps();
            if (this.bufferOut.position() > 0) {
                if ((ops & ~SelectionKey.OP_WRITE) == 0) {
                    this.selectionKey.interestOps(ops | SelectionKey.OP_WRITE);
                }
            } else {
                if ((ops & ~SelectionKey.OP_WRITE) != 0) {
                    this.selectionKey.interestOps(ops & ~SelectionKey.OP_WRITE);
                }
            }
        } catch (Throwable e) {
            log.log(Level.ERROR, log.getSB().append("send() caught: "), e);
            reconnect();
        }
        return len;
    }

    @Override
    public void dispatch() {
        bufferIn.flip();
        this.messageListener.onMessage(this, bufferIn);
        if (bufferIn.hasRemaining()) {
            bufferIn.compact();
        } else {
            bufferIn.clear();
        }
    }

    @Override
    public void onSelect() {
    }

    @Override
    public void onAccept(SelectableChannel selectableChannel) {
        log.log(Level.ERROR, "Unexpected Accept");
        if (connectionListener != null) {
            connectionListener.onAccept(selectableChannel);
        }
    }

    @Override
    public void onClose(SelectableChannel selectableChannel) {
        log.log(Level.INFO, "onClose");
        reconnect();
    }

    public void reconnect() {
        close();

        log.log(Level.INFO, log.getSB().append("Attempt to reconnect in ").append(reconnectTimeout).append(" ms"));
        if (reconnectTimer != -1) {
            eventLoop.cancelTimer(reconnectTimer);
            reconnectTimer = -1;
        }
        reconnectTimer = eventLoop.addTimer(eventLoop.currentMillis() + reconnectTimeout, this);
    }

    @Override
    public void onTimer(int entry, long expiry) {
        if (entry == connectTimer) {
            connectTimer = -1;
            log.log(Level.INFO, log.getSB().append("Connection establishment timed out."));
            reconnect();
        } else if (entry == reconnectTimer) {
            reconnectTimer = -1;
            destIndex = (destIndex + 1) % destInetSocketAddress.length;
            open();
        }
    }
}
