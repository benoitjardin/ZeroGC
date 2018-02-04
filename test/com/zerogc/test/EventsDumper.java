package com.zerogc.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import com.zerogc.core.ByteStringBuilder;
import com.zerogc.logging.Level;
import com.zerogc.logging.LogManager;
import com.zerogc.logging.Logger;

//java -cp /sbcexp/cache/home/jardinbe/zerogc/dist/ZeroGC-0.0.0.0.jar com.zerogc.test.EventsDumper

/**
 * @author Benoit Jardin
 */

public class EventsDumper {
    static final Logger log = LogManager.getLogger("EventsDumper");
    
    static final String[] hex = {
    "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "0A", "0B", "0C", "0D", "0E", "0F",
    "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "1A", "1B", "1C", "1D", "1E", "1F",
    "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "2A", "2B", "2C", "2D", "2E", "2F",
    "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "3A", "3B", "3C", "3D", "3E", "3F",
    "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "4A", "4B", "4C", "4D", "4E", "4F",
    "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "5A", "5B", "5C", "5D", "5E", "5F",
    "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "6A", "6B", "6C", "6D", "6E", "6F",
    "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "7A", "7B", "7C", "7D", "7E", "7F",
    "80", "81", "82", "83", "84", "85", "86", "87", "88", "89", "8A", "8B", "8C", "8D", "8E", "8F",
    "90", "91", "92", "93", "94", "95", "96", "97", "98", "99", "9A", "9B", "9C", "9D", "9E", "9F",
    "A0", "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "AA", "AB", "AC", "AD", "AE", "AF",
    "B0", "B1", "B2", "B3", "B4", "B5", "B6", "B7", "B8", "B9", "BA", "BB", "BC", "BD", "BE", "BF",
    "C0", "C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9", "CA", "CB", "CC", "CD", "CE", "CF",
    "D0", "D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8", "D9", "DA", "DB", "DC", "DD", "DE", "DF",
    "E0", "E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8", "E9", "EA", "EB", "EC", "ED", "EE", "EF",
    "F0", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "FA", "FB", "FC", "FD", "FE", "FF" };
    

    public static void main(String[] args) {
        try {
            ByteBuffer datByteBuffer = ByteBuffer.allocateDirect(512*1024);
            ByteBuffer idxByteBuffer = ByteBuffer.allocateDirect(512*1024);
            datByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            idxByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            FileChannel datChannel = new FileInputStream("events.dat").getChannel();
            FileChannel idxChannel = new FileInputStream("events.idx").getChannel();

            long sequence = 0;
            long idxOffset = 0;
            long datOffset = 0;
            
            while (true) {
                int readLen = idxChannel.read(idxByteBuffer);
                if (readLen == -1 && idxByteBuffer.position() == 0) {
                    log.log(Level.INFO, "Reached end of idx file");
                    break;
                }
                readLen = datChannel.read(datByteBuffer);
                if (readLen == -1 && datByteBuffer.position() == 0) {
                    log.log(Level.INFO, "Reached end of dat file");
                    break;
                }
                idxByteBuffer.flip();
                datByteBuffer.flip();
                while (true) {
                    int position = datByteBuffer.position();
                    int limit = datByteBuffer.limit();
                    if (limit - position > 2) {
                        int len = datByteBuffer.getShort(position);
                        if (limit - position > 2 + len) {
                            // TODO: do something usefull with the message
                            
                            //log.log(Level.INFO, log.getSB().append("Message len: ").append(len));
                            ByteStringBuilder sb = log.getSB().append(sequence).append(": ");
                            for (int pos=position+2; pos < position + 2 + len; pos++) {
                                sb.append(EventsDumper.hex[(int)datByteBuffer.get(pos) & 0xFF]).append(' ');
                            }
                            //log.log(Level.INFO, sb);
                            System.out.println(sb.toString());
                            
                            datByteBuffer.position(position + 2 + len);
                            
                            if (idxByteBuffer.remaining() >= 4) {
                                idxOffset = idxByteBuffer.getLong();
                            } else {
                                idxOffset = -1;
                            }
                            if (idxOffset != datOffset) {
                                log.log(Level.ERROR, log.getSB().append("Inconsistent index for message: ").append(sequence)
                                        .append(" datOffset: ").append(datOffset)
                                        .append(" idxOffset: ").append(idxOffset));
                            }
                            datOffset += 2 + len;
                            sequence++;
                        } else {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                datByteBuffer.compact();
                idxByteBuffer.compact();
            }
            datByteBuffer.compact();
        } catch (IOException e) {
            log.log(Level.ERROR, "main caught: ", e);
        }
    }
}
