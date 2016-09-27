#include <jni.h>

#if defined(WIN32)
#include <ws2tcpip.h>
#define EINTR WSAEINTR
#else // defined(WIN32)
#include <sys/time.h>
#include <time.h>
#include <errno.h>

#include <poll.h>
#include <net/if.h>
#include <netinet/in.h>
#endif // defined(WIN32)

#if defined(__linux__)
#include <sys/epoll.h>
#endif // defined(__linux__)

#include <string.h>

static jfieldID serverSocketChannel_fdVal;  /* for jint 'fdVal' in sun.nio.ch.ServerSocketChannelImpl */
static jfieldID socketChannel_fdVal;        /* for jint 'fdVal' in sun.nio.ch.SocketChannelImpl */
static jfieldID datagramChannel_fdVal;      /* for jint 'fdVal' in sun.nio.ch.DatagramChannelImpl */
static jmethodID sourceChannel_getFDVal;    /* for jint 'getFDVal' in sun.nio.ch.SourceChannelImpl */
static jmethodID sinkChannel_getFDVal;      /* for jint 'getFDVal' in sun.nio.ch.SinkChannelImpl */

JNIEXPORT void JNICALL
Java_com_zerogc_util_Native_initIDs(JNIEnv *env, jclass clazz)
{
    clazz = (*env)->FindClass(env, "sun/nio/ch/ServerSocketChannelImpl");
    serverSocketChannel_fdVal = (*env)->GetFieldID(env, clazz, "fdVal", "I");
    clazz = (*env)->FindClass(env, "sun/nio/ch/SocketChannelImpl");
    socketChannel_fdVal = (*env)->GetFieldID(env, clazz, "fdVal", "I");
    clazz = (*env)->FindClass(env, "sun/nio/ch/DatagramChannelImpl");
    datagramChannel_fdVal = (*env)->GetFieldID(env, clazz, "fdVal", "I");
    clazz = (*env)->FindClass(env, "sun/nio/ch/SourceChannelImpl");
    // Windows implementation require using a getter method instead of accessing the field.
    sourceChannel_getFDVal = (*env)->GetMethodID(env, clazz, "getFDVal", "()I");
    clazz = (*env)->FindClass(env, "sun/nio/ch/SinkChannelImpl");
    sinkChannel_getFDVal = (*env)->GetMethodID(env, clazz, "getFDVal", "()I");
 
}

JNIEXPORT jint JNICALL
Java_com_zerogc_util_Native_getFdVal_1ServerSocketChannel(JNIEnv *env, jclass clazz,
                                                          jobject ssco)
{
    return (*env)->GetIntField(env, ssco, serverSocketChannel_fdVal);
}

JNIEXPORT jint JNICALL
Java_com_zerogc_util_Native_getFdVal_1SocketChannel(JNIEnv *env, jclass clazz,
                                                    jobject sco)
{
    return (*env)->GetIntField(env, sco, socketChannel_fdVal);
}

JNIEXPORT jint JNICALL
Java_com_zerogc_util_Native_getFdVal_1DatagramChannel(JNIEnv *env, jclass clazz,
                                                      jobject dco)
{
    return (*env)->GetIntField(env, dco, datagramChannel_fdVal);
}

JNIEXPORT jint JNICALL
Java_com_zerogc_util_Native_getFdVal_1SourceChannel(JNIEnv *env, jclass clazz,
                                                      jobject sco)
{
    return (*env)->CallIntMethod(env, sco, sourceChannel_getFDVal);
}

JNIEXPORT jint JNICALL
Java_com_zerogc_util_Native_getFdVal_1SinkChannel(JNIEnv *env, jclass clazz,
                                                      jobject sco)
{
    return (*env)->CallIntMethod(env, sco, sinkChannel_getFDVal);
}

JNIEXPORT jlong JNICALL
Java_com_zerogc_util_Native_currentTimeMicros(JNIEnv *env, jclass clazz,
                                              jobject sco)
{
    struct timeval tv;
    gettimeofday(&tv, 0);
    return tv.tv_sec * 1000000LL + tv.tv_usec;
}

JNIEXPORT jint JNICALL
Java_com_zerogc_util_Native_errno(JNIEnv *env, jclass clazz)
{
    return errno;
}

/*************************************************************/


JNIEXPORT jint JNICALL
Java_com_zerogc_util_Native_setMcastTtl(JNIEnv *env, jclass clazz,
                                        jobject dco, jbyte ttl)
{
    int sock = (*env)->GetIntField(env, dco, datagramChannel_fdVal);
    return setsockopt(sock, IPPROTO_IP, IP_MULTICAST_TTL, (void *)&ttl, sizeof(ttl));
}

JNIEXPORT jint JNICALL
Java_com_zerogc_util_Native_setMcastLoop(JNIEnv *env, jclass clazz,
                                         jobject dco, jbyte loop)
{
    int sock = (*env)->GetIntField(env, dco, datagramChannel_fdVal);
    return setsockopt(sock, IPPROTO_IP, IP_MULTICAST_LOOP, (void *)&loop, sizeof(loop));
}

JNIEXPORT jint JNICALL
Java_com_zerogc_util_Native_setMcastIf(JNIEnv *env, jclass clazz,
                                       jobject dco, jint ifaddr)
{
    int sock = (*env)->GetIntField(env, dco, datagramChannel_fdVal);
    struct in_addr sin_addr;
    sin_addr.s_addr = htonl(ifaddr);

    // Set IP multicast interface for sending
    return setsockopt(sock, IPPROTO_IP, IP_MULTICAST_IF, (void *)&sin_addr, sizeof(sin_addr));
}

JNIEXPORT jint JNICALL
Java_com_zerogc_util_Native_joinMcastGroup(JNIEnv *env, jclass clazz,
                                           jobject dco, jint mcastaddr, jint ifaddr)
{
    int sock = (*env)->GetIntField(env, dco, datagramChannel_fdVal);

    // This is the call that join the IPMC group
#ifdef IGMPV3
    struct ip_mreq_source mreqsrc;
    memset(&mreqsrc, 0, sizeof(mreqsrc));
    mreqsrc.imr_multiaddr.s_addr = htonl(mcastaddr);
    mreqsrc.imr_interface.s_addr = htonl(ifaddr);
    mreqsrc.imr_sourceaddr.s_addr = htonl(INADDR_ANY);
    return setsockopt(sock, IPPROTO_IP, IP_ADD_SOURCE_MEMBERSHIP, (char *)&mreqsrc, sizeof(mreqsrc));
#else // IGMPV3
    struct ip_mreq mreq;
    memset(&mreq, 0, sizeof(mreq));
    mreq.imr_multiaddr.s_addr = htonl(mcastaddr);
    mreq.imr_interface.s_addr = htonl(ifaddr);
    return setsockopt(sock, IPPROTO_IP, IP_ADD_MEMBERSHIP, (char *)&mreq, sizeof(mreq));
#endif // IGMPV3
}

JNIEXPORT jint JNICALL
Java_com_zerogc_util_Native_leaveMcastGroup(JNIEnv *env, jclass clazz,
                                           jobject dco, jint mcastaddr, jint ifaddr)
{
    int sock = (*env)->GetIntField(env, dco, datagramChannel_fdVal);

    // This is the call that join the IPMC group
#ifdef IGMPV3
    struct ip_mreq_source mreqsrc;
    memset(&mreqsrc, 0, sizeof(mreqsrc));
    mreqsrc.imr_multiaddr.s_addr = htonl(mcastaddr);
    mreqsrc.imr_interface.s_addr = htonl(ifaddr);
    mreqsrc.imr_sourceaddr.s_addr = htonl(INADDR_ANY);
    return setsockopt(sock, IPPROTO_IP, IP_DROP_SOURCE_MEMBERSHIP, (char *)&mreqsrc, sizeof(mreqsrc));
#else // IGMPV3
    struct ip_mreq mreq;
    memset(&mreq, 0, sizeof(mreq));
    mreq.imr_multiaddr.s_addr = htonl(mcastaddr);
    mreq.imr_interface.s_addr = htonl(ifaddr);
    return setsockopt(sock, IPPROTO_IP, IP_DROP_MEMBERSHIP, (char *)&mreq, sizeof(mreq));
#endif // IGMPV3
}

/*************************************************************/

JNIEXPORT jint JNICALL
Java_com_zerogc_util_Native_select(JNIEnv *env, jclass clazz,
                                 jint nfds, jlong readfdsAddress, jlong writefdsAddress, jlong exceptfdsAddress, jlong timeout)
{
    struct timeval tv, *ptv;
    if (timeout == ~0) {
        ptv = 0;
    } else {
        ptv = &tv;
        tv.tv_sec = (long)(timeout/1000);
        tv.tv_usec = (long)((timeout%1000)*1000);
    }
    int status = select(nfds, (fd_set *)readfdsAddress, (fd_set *)writefdsAddress, (fd_set *)exceptfdsAddress, ptv);
    if (status == -1 && errno == EINTR) {
        status = 0;
    }
    return status;
}

/*************************************************************/

JNIEXPORT jint JNICALL
Java_com_zerogc_util_Native_poll(JNIEnv *env, jclass clazz,
                                 jlong fdsAddress, jint nfds, jint timeout)
{
#if !defined(WIN32)
    int status = poll((struct pollfd *)fdsAddress, nfds, timeout);
    if (status == -1 && errno == EINTR) {
        status = 0;
    }
    return status;
#else // !WIN32
// Windows select is implemented differently than on Unix.
// It is based on arrays of fd instead of bitmasks.

/* Event types that can be polled for.  These bits may be set in `events'
   to indicate the interesting event types; they will appear in `revents'
   to indicate the status of the file descriptor.  */
#define POLLIN      0x001   /* There is data to read.  */
#define POLLPRI     0x002   /* There is urgent data to read.  */
#define POLLOUT     0x004   /* Writing now will not block.  */
/* Event types always implicitly polled for.  These bits need not be set in
   `events', but they will appear in `revents' to indicate the status of
   the file descriptor.  */
#define POLLERR     0x008   /* Error condition.  */
#define POLLHUP	    0x010   /* Hung up.  */
#define POLLNVAL    0x020   /* Invalid polling request.  */

    struct pollfd {
        int   fd;         /* file descriptor */
        short events;     /* requested events */
        short revents;    /* returned events */
    };
    struct pollfd *fds = (struct pollfd *)fdsAddress;

    FD_SET readfds, writefds, exceptfds;
    int read_count = 0, write_count = 0, except_count = 0;

    struct timeval tv, *ptv;
    if (timeout < 0) {
        ptv = 0;
    } else {
        ptv = &tv;
        tv.tv_sec = timeout/1000;
        tv.tv_usec = (timeout%1000)*1000;
    }

    /* Set FD_SET structures required for select */
    for (int i = 0; i < nfds; i++) {
        if (fds[i].events & POLLIN) {
            readfds.fd_array[read_count] = fds[i].fd;
            read_count++;
        }
        if (fds[i].events & POLLOUT) {
            writefds.fd_array[write_count] = fds[i].fd;
            write_count++;
        }
        // Async connection failure are reported as exception on Windows
        exceptfds.fd_array[except_count] = fds[i].fd;
        except_count++;
    }

    readfds.fd_count = read_count;
    writefds.fd_count = write_count;
    exceptfds.fd_count = except_count;

    int status = select(nfds, &readfds, &writefds, &exceptfds, ptv);
    if (status == -1 && errno == EINTR) {
        status = 0;
    }

    if (status > 0) {
        status = 0;
        /* Set FD_SET structures required for select */
        for (int i=0; i < nfds; i++) {
            int mask = 0;
            for (int j = 0; j < readfds.fd_count; j++) {
                if (readfds.fd_array[j] == fds[i].fd) {
                    mask |= POLLIN;
                }
            }
            for (int j = 0; j < writefds.fd_count; j++) {
                if (writefds.fd_array[j] == fds[i].fd) {
                    mask |= POLLOUT;
                }
            }
            for (int j = 0; j < exceptfds.fd_count; j++) {
                if (exceptfds.fd_array[j] == fds[i].fd) {
                    //mask |= POLLPRI;
                    mask |= POLLERR;
                }
            }
            if ((fds[i].revents = ((fds[i].events|POLLERR|POLLHUP|POLLNVAL) & mask))) {
                status++;
            }
        }
    }

    return status;
#endif
}

/*************************************************************/

JNIEXPORT jint JNICALL
Java_com_zerogc_util_Native_epoll_1create(JNIEnv *env, jclass clazz,
                                          jint size)
{
#if defined(__linux__)

    int status = epoll_create(size);
    return status;
#else
    return -1;
#endif // defined(__linux__)
}

JNIEXPORT jint JNICALL
Java_com_zerogc_util_Native_epoll_1ctl(JNIEnv *env, jclass clazz,
                                       jint epfd, jint opcode, jint fd, jint events)
{
#if defined(__linux__)
    struct epoll_event event;
    int res;

    event.events = events;
    event.data.fd = fd;

    int status = epoll_ctl(epfd, (int)opcode, (int)fd, &event);
    return status;
#else
    return -1;
#endif // defined(__linux__)
}

JNIEXPORT jint JNICALL
Java_com_zerogc_util_Native_epoll_1wait(JNIEnv *env, jclass clazz,
                                        jint epfd, jlong eventsAddress, jint maxevents, jint timeout)
{
#if defined(__linux__)
    int status = epoll_wait(epfd, (struct epoll_event *)eventsAddress, maxevents, timeout);
    if (status == -1 && errno == EINTR) {
        status = 0;
    }
    return status;
#else
    return -1;
#endif // defined(__linux__)
}

JNIEXPORT jint JNICALL
Java_com_zerogc_util_Native_getOffset_1epoll_1event_1epoll_1data_1fd(JNIEnv *env, jclass clazz)
{
#if defined(__linux__)
    struct epoll_event event;
    return ((char *)&(event.data.fd) - (char *)&event);
#else
    return -1;
#endif // defined(__linux__)
}

/*************************************************************/

JNIEXPORT jint JNICALL
Java_com_zerogc_test_JniPerf_callDirectByteBuffer(JNIEnv *env, jclass clazz,
                                                  jobject byteBuffer)
{
    struct pollfd *address = NULL;
    if (byteBuffer) {
        address = ((*env)->GetDirectBufferAddress(env, byteBuffer));
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_zerogc_test_JniPerf_callByteArray(JNIEnv *env, jclass clazz,
                                           jbyteArray byteArray)
{
    jboolean iscopy = JNI_FALSE;
    //jbyte *pByteArray = (*env)->GetByteArrayElements(env, byteArray, &iscopy);
    //(*env)->ReleaseByteArrayElements(env, byteArray, pByteArray, 0);
    void *address = (*env)->GetPrimitiveArrayCritical(env, byteArray, &iscopy);
    (*env)->ReleasePrimitiveArrayCritical(env, byteArray, address, 0);
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_zerogc_test_JniPerf_callAddress(JNIEnv *env, jclass clazz,
                                         jlong address, jint len)
{
    return 0;
}

/*************************************************************/
