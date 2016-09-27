package com.zerogc.test;

import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

import com.zerogc.util.Level;
import com.zerogc.util.Logger;

/**
 * @author Benoit Jardin
 */

public class McastDumper {
    static final Logger log = new Logger("McastDumper");

    static final int OFFSET_SESSION = 0;
    static final int OFFSET_FIRST_SEQUENCE = 4;
    static final int OFFSET_NUM_MESSAGES = 12;
    static final int OFFSET_FIRST_MESSAGE = 14;
    
    private static DatagramChannel createMulticastDatagramChannel(InetSocketAddress inetSocketAddress, NetworkInterface networkInterface) throws Exception {
        DatagramChannel datagramChannel = DatagramChannel.open();
        DatagramSocket datagramSocket = datagramChannel.socket();
        datagramSocket.setReuseAddress(true);
        datagramSocket.bind(inetSocketAddress);

        //datagramSocket.getImpl().join(inetSocketAddress.getAddress());
        Method getImpl = DatagramSocket.class.getDeclaredMethod("getImpl");
        getImpl.setAccessible(true);
        DatagramSocketImpl datagramSocketImpl = (DatagramSocketImpl)getImpl.invoke(datagramSocket);

        //datagramSocketImpl.join(inetSocketAddress.getAddress());
        Method joinGroup = DatagramSocketImpl.class.getDeclaredMethod("joinGroup", SocketAddress.class, NetworkInterface.class);
        joinGroup.setAccessible(true);
        joinGroup.invoke(datagramSocketImpl, inetSocketAddress, networkInterface);

        return datagramChannel;
    }

    public static void main(String[] args) {
        try {
            int mcastPort = 36456;
            InetAddress mcastAddrFrom = InetAddress.getByName("239.255.2.34");
            InetAddress mcastAddrTo = InetAddress.getByName("239.255.2.34");
            NetworkInterface networkInterface = NetworkInterface.getByName("eth0");
            
            Selector selector = Selector.open();

            for (byte j=mcastAddrFrom.getAddress()[3]; j <= mcastAddrTo.getAddress()[3]; j++) {
                byte[] bytes = mcastAddrFrom.getAddress();
                bytes[3] = j;
                InetAddress mcastAddress = Inet4Address.getByAddress(bytes);

                InetSocketAddress mcastSocketAddress = new InetSocketAddress(mcastAddress, mcastPort);
            
                DatagramChannel datagramChannel = createMulticastDatagramChannel(mcastSocketAddress, networkInterface);
                datagramChannel.configureBlocking(false);
                
                datagramChannel.register(selector, SelectionKey.OP_READ);
            }
                
            long sequence = 0;
            long nextSequence = 0;
            
            ByteBuffer byteBuffer = ByteBuffer.allocate(8192);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            while (true) {
                int keys = selector.select();
                if (keys > 0) {
                    Set<SelectionKey> keySet = selector.selectedKeys();
                    Iterator<SelectionKey> it = keySet.iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        it.remove();
                        if (key.isReadable()) {
                            DatagramChannel channel = (DatagramChannel)key.channel();
                            byteBuffer.clear();
                            InetSocketAddress fromSocketAddress = (InetSocketAddress)channel.receive(byteBuffer);
                            //System.err.println(fromSocketAddress.getAddress().getHostAddress() + ":" + fromSocketAddress.getPort() + " -> " + channel.socket().getLocalAddress().getHostAddress() + ":" + channel.socket().getLocalPort() + ": " + byteBuffer.position()+ " bytes");
                            
                            byteBuffer.flip();
                            /*
                            StringBuffer sb = log.getSB().append(sequence).append(": ");
                            for (int pos=0; pos < byteBuffer.limit(); pos++) {
                                sb.append(EventsDumper.hex[(int)byteBuffer.get(pos) & 0xFF]).append(' ');
                            }
                            //log.log(Level.INFO, sb);
                            System.out.println(sb.toString());
                            */
                            short count = byteBuffer.getShort(OFFSET_NUM_MESSAGES);
                            sequence = byteBuffer.getLong(OFFSET_FIRST_SEQUENCE);
                            if (count == 0) {
                                // Heartbeat
                                if (sequence != nextSequence-1) {
                                    log.log(Level.ERROR, log.getSB().append("Heartbeat sequence mismatch, expected: ").append(nextSequence-1).append(" received: ").append(sequence));                                     
                                    nextSequence = sequence+1;
                                }
                            } else if (sequence != nextSequence) { 
                                log.log(Level.ERROR, log.getSB().append("Sequence mismatch, expected: ").append(nextSequence).append(" received: ").append(sequence)); 
                            }
                            nextSequence = sequence + count;
                            
                            //log.log(Level.INFO, log.getSB().append("packet sequence: ").append(sequence).append(", count: ").append(count)); 

                            byteBuffer.position(OFFSET_FIRST_MESSAGE);
                            for (int i=0; i < count; i++) {
                                int position = byteBuffer.position();
                                int limit = byteBuffer.limit();
                                int len = byteBuffer.getShort(position);
                                // TODO: do something usefull with the message
                                //log.log(Level.INFO, log.getSB().append("message sequence: ").append(sequence)); 
                                /*
                                
                                //log.log(Level.INFO, log.getSB().append("Message len: ").append(len));
                                StringBuffer sb = log.getSB().append(sequence).append(": ");
                                for (int pos=position+2; pos < position+2+len; pos++) {
                                    sb.append(EventsDumper.hex[(int)byteBuffer.get(pos) & 0xFF]).append(' ');
                                }
                                //log.log(Level.INFO, sb);
                                System.out.println(sb.toString());
*/
                                byteBuffer.position(position + 2 + len);
                                sequence++;
                            }
                            byteBuffer.compact();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }


}
