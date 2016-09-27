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
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

// javah -jni -classpath $DEVELOP/zerogc/dist/ZeroGC-0.0.0.0.jar com.zerogc.util.Native

public class Native {
    public final static int MAX_SELECTABLE_FDS = 1024;

    // Event masks
    public static final short POLLIN       = 0x0001;
    public static final short POLLPRI      = 0x0002;
    public static final short POLLOUT      = 0x0004;
    public static final short POLLERR      = 0x0008;
    public static final short POLLHUP      = 0x0010;
    public static final short POLLNVAL     = 0x0020;
    public static final short POLLREMOVE   = 0x0800;

    // Miscellaneous constants
    public static final short POLL_SIZE          = 8;
    public static final short POLL_FD_OFFSET     = 0;
    public static final short POLL_EVENT_OFFSET  = 4;
    public static final short POLL_REVENT_OFFSET = 6;
    
    // struct pollfd {
    //     int   fd;         /* file descriptor */
    //     short events;     /* requested events */
    //     short revents;    /* returned events */
    // };

    /*************************************************************/
    
    // EPOLL_EVENTS
    static final int EPOLLIN      = 0x001;
    static final int EPOLLPRI     = 0x002;
    static final int EPOLLOUT     = 0x004;
    static final int EPOLLRDNORM  = 0x040;
    static final int EPOLLRDBAND  = 0x080;
    static final int EPOLLWRNORM  = 0x100;
    static final int EPOLLWRBAND  = 0x200;
    static final int EPOLLMSG     = 0x400;
    static final int EPOLLERR     = 0x008;
    static final int EPOLLHUP     = 0x010;
    static final int EPOLLONESHOT = (1 << 30);
    static final int EPOLLET      = (1 << 31);

    // opcodes
    static final int EPOLL_CTL_ADD  = 1; /* Add a file descriptor to the interface.  */
    static final int EPOLL_CTL_DEL  = 2; /* Remove a file descriptor from the interface.  */
    static final int EPOLL_CTL_MOD  = 3; /* Change file descriptor epoll_event structure.  */

    // Miscellaneous constants
    static final int EPOLLEVENT_EVENTS_OFFSET    = 0;
    static final int EPOLLEVENT_FD_OFFSET;
    static final int EPOLLEVENT_SIZE;
    
    //typedef union epoll_data {
    //    void *ptr;
    //    int fd;
    //    __uint32_t u32;
    //    __uint64_t u64;
    //} epoll_data_t;
    //
    //struct epoll_event {
    //    __uint32_t events;      /* Epoll events */
    //    epoll_data_t data;      /* User data variable */
    //};

    static native void initIDs();

    public static native long currentTimeMicros();

    public static native int errno();
    
    public static native int getFdVal_ServerSocketChannel(ServerSocketChannel serverSocketChannel);
    public static native int getFdVal_SocketChannel(SocketChannel socketChannel);
    public static native int getFdVal_DatagramChannel(DatagramChannel datagramChannel);
    public static native int getFdVal_SourceChannel(SourceChannel sourceChannel);
    public static native int getFdVal_SinkChannel(SinkChannel sinkChannel);

    public static native int select(int nfds, long readfdsAddress, long writefdsAddress, long exceptfdsAddress, long timeout);

    public static native int poll(long fdsAddress, int nfds, int timeout);
    
    public static native int epoll_create(int size);
    public static native int epoll_ctl(int epfd, int opcode, int fd, int events);
    public static native int epoll_wait(int epfd, long eventsAddress, int maxevents, int timeout);
    public static native int getOffset_epoll_event_epoll_data_fd();
    
    public static native int setMcastTtl(DatagramChannel datagramChannel, byte ttl);
    public static native int setMcastLoop(DatagramChannel datagramChannel, byte loop);
    public static native int setMcastIf(DatagramChannel datagramChannel, int ifaddr);
    public static native int joinMcastGroup(DatagramChannel datagramChannel, int mcastaddr, int ifaddr);
    public static native int leaveMcastGroup(DatagramChannel datagramChannel, int mcastaddr, int ifaddr);
    
	static {
		System.loadLibrary("zerogc");
		Native.initIDs();
        EPOLLEVENT_FD_OFFSET        = Native.getOffset_epoll_event_epoll_data_fd();
        EPOLLEVENT_SIZE             = EPOLLEVENT_FD_OFFSET + 8;
	}
}
