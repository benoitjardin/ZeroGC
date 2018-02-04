/*
 * PLEASE DO NOT EDIT!
 *
 * This code has been automatically generated.
 * Generator: com.zerogc.messages.GeneratorStores
 * Schema: tools/resources/TestStores.xsd
 */


package com.zerogc.stores;

import com.zerogc.core.ByteSlice;
import com.zerogc.core.ByteStringBuilder;
import com.zerogc.core.ByteUtils;
import com.zerogc.logging.Level;
import com.zerogc.logging.LogManager;
import com.zerogc.logging.Logger;

public class TestOrderStore {
    protected final Logger log;

    public static class ByteLengths {
        public static final int side = 1;
    }

    public static class ByteOffsets {
        public static final int side = 0;
        public static final int end = 1;
    }

    public static class IntLengths {
        public static final int instrument = 1;
        public static final int quantity = 1;
    }

    public static class IntOffsets {
        public static final int instrument = 0;
        public static final int quantity = 1;
        public static final int end = 2;
    }

    public static class LongLengths {
        public static final int insertDateTime = 1;
        public static final int orderNumber = 1;
        public static final int Price = 1;
    }

    public static class LongOffsets {
        public static final int insertDateTime = 0;
        public static final int orderNumber = 1;
        public static final int Price = 2;
        public static final int end = 3;
    }

    private byte[] bytes;
    private int[] ints;
    private long[] longs;

    protected int capacity = 0;

    private final ByteSlice emptySlice = new ByteSlice(new byte[0], 0, 0);
    private final ByteSlice slice = new ByteSlice();

    public TestOrderStore(String name, int initialCapacity) {
        log = LogManager.getLogger(name);
        grow(initialCapacity);
    }

    public int getCapacity() {
       return capacity;
    }

    protected void grow(int newCapacity) {
        if (capacity > 0) {
        	log.log(Level.WARN, log.getSB().append("Resizing TestOrderStore from ").append(capacity).append(" to ").append(newCapacity));
        }

        byte[] newBytes = new byte[newCapacity*ByteOffsets.end];
        int[] newInts = new int[newCapacity*IntOffsets.end];
        long[] newLongs = new long[newCapacity*LongOffsets.end];

        if (capacity > 0) {
            System.arraycopy(bytes, 0, newBytes, 0, capacity*ByteOffsets.end);
            System.arraycopy(ints, 0, newInts, 0, capacity*IntOffsets.end);
            System.arraycopy(longs, 0, newLongs, 0, capacity*LongOffsets.end);
        }
        bytes = newBytes;
        ints = newInts;
        longs = newLongs;
        capacity = newCapacity;
    }

    public void setInsertDateTime(int slot, long value) {
        this.longs[slot*LongOffsets.end + LongOffsets.insertDateTime] = value;
    }
    public long getInsertDateTime(int slot) {
        return this.longs[slot*LongOffsets.end + LongOffsets.insertDateTime];
    }
    public void setOrderNumber(int slot, long value) {
        this.longs[slot*LongOffsets.end + LongOffsets.orderNumber] = value;
    }
    public long getOrderNumber(int slot) {
        return this.longs[slot*LongOffsets.end + LongOffsets.orderNumber];
    }
    public void setSide(int slot, byte value) {
        this.bytes[slot*ByteOffsets.end + ByteOffsets.side] = value;
    }
    public byte getSide(int slot) {
        return this.bytes[slot*ByteOffsets.end + ByteOffsets.side];
    }
    public void setInstrument(int slot, int value) {
        this.ints[slot*IntOffsets.end + IntOffsets.instrument] = value;
    }
    public int getInstrument(int slot) {
        return this.ints[slot*IntOffsets.end + IntOffsets.instrument];
    }
    public void setQuantity(int slot, int value) {
        this.ints[slot*IntOffsets.end + IntOffsets.quantity] = value;
    }
    public int getQuantity(int slot) {
        return this.ints[slot*IntOffsets.end + IntOffsets.quantity];
    }
    public void setPrice(int slot, long value) {
        this.longs[slot*LongOffsets.end + LongOffsets.Price] = value;
    }
    public long getPrice(int slot) {
        return this.longs[slot*LongOffsets.end + LongOffsets.Price];
    }

    public void clear(int slot) {
        setInsertDateTime(slot, 0);
        setOrderNumber(slot, 0);
        setSide(slot, (byte)0);
        setInstrument(slot, 0);
        setQuantity(slot, 0);
        setPrice(slot, 0);
    }


    public ByteStringBuilder toString(ByteStringBuilder sb, int slot) {
        sb.append("TestOrderStore");
        sb.append("[insertDateTime=").append(getInsertDateTime(slot)).append(']');
        sb.append("[orderNumber=").append(getOrderNumber(slot)).append(']');
        sb.append("[side=").append(getSide(slot)).append(']');
        sb.append("[instrument=").append(getInstrument(slot)).append(']');
        sb.append("[quantity=").append(getQuantity(slot)).append(']');
        sb.append("[Price=").append(getPrice(slot)).append(']');
        sb.append("\n");
        return sb;
    }
}
