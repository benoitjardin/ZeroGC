/*
 * PLEASE DO NOT EDIT!
 * 
 * This code has been automatically generated.
 * Generator: com.zerogc.messages.GeneratorMessages
 * Schema: tools/resources/TestMessages.xsd
 */


package com.zerogc.messages;


import java.nio.ByteBuffer;
import com.zerogc.util.ByteSlice;
import com.zerogc.util.ByteStringBuilder;
import com.zerogc.util.ByteUtils;

public class TestEnterOrderRequest {
    public static class Lengths {
        public static final short type = 1;
        public static final short token = 4;
        public static final short bankInternalReference = 16;
        public static final short side = 1;
        public static final short quantity = 4;
        public static final short instrument = 4;
        public static final short price = 8;
        public static final short tif = 4;
    }

    public static class Offsets {
        public static final short type = 0;
        public static final short token = 1;
        public static final short bankInternalReference = 5;
        public static final short side = 21;
        public static final short quantity = 22;
        public static final short instrument = 26;
        public static final short price = 30;
        public static final short tif = 38;
    }
    public static final short endOfAttributes = 42;

    private ByteBuffer buffer;
    private int baseOffset;
    private final ByteSlice byteSlice = new ByteSlice();

    public TestEnterOrderRequest() {
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
        this.baseOffset = buffer.position();
        buffer.position(this.baseOffset + endOfAttributes);
    }

    public ByteBuffer getBuffer() {
        return this.buffer;
    }

    public int getBaseOffset() {
        return this.baseOffset;
    }

    public void create(byte type, int token, byte[] bankInternalReference, byte side, int quantity, int instrument, long price, int tif) {
        this.buffer.put(this.baseOffset + Offsets.type, type);
        this.buffer.putInt(this.baseOffset + Offsets.token, token);
        ByteUtils.copySpacePadded(bankInternalReference, 0, bankInternalReference.length, this.buffer, this.baseOffset + Offsets.bankInternalReference, Lengths.bankInternalReference);
        this.buffer.put(this.baseOffset + Offsets.side, side);
        this.buffer.putInt(this.baseOffset + Offsets.quantity, quantity);
        this.buffer.putInt(this.baseOffset + Offsets.instrument, instrument);
        this.buffer.putLong(this.baseOffset + Offsets.price, price);
        this.buffer.putInt(this.baseOffset + Offsets.tif, tif);
    }

    public void setType(byte type) {
        this.buffer.put(this.baseOffset + Offsets.type, type);
    }
    public byte getType() {
        return this.buffer.get(this.baseOffset + Offsets.type);
    }

    public void setToken(int token) {
        this.buffer.putInt(this.baseOffset + Offsets.token, token);
    }
    public int getToken() {
        return this.buffer.getInt(this.baseOffset + Offsets.token);
    }

    public int getBankInternalReferenceOffset() {
        return this.baseOffset + Offsets.bankInternalReference;
    }
    public int getBankInternalReferenceLength() {
        return Lengths.bankInternalReference;
    }
    public void setBankInternalReference(ByteSlice byteSlice) {
        ByteUtils.copySpacePadded(byteSlice.getBuffer(), byteSlice.getOffset(), byteSlice.getLength(), this.buffer, this.baseOffset + Offsets.bankInternalReference, Lengths.bankInternalReference);
    }
    public void setBankInternalReference(byte[] buffer) {
        ByteUtils.copySpacePadded(buffer, 0, buffer.length, this.buffer, this.baseOffset + Offsets.bankInternalReference, Lengths.bankInternalReference);
    }
    public int getBankInternalReference(byte[] buffer) {
        return ByteUtils.copySpaceTrimmed(this.buffer, this.baseOffset + Offsets.bankInternalReference, Lengths.bankInternalReference, buffer, 0);
    }
    public void setBankInternalReference(byte[] buffer, int offset, int length) {
        ByteUtils.copySpacePadded(buffer, offset, length, this.buffer, this.baseOffset + Offsets.bankInternalReference, Lengths.bankInternalReference);
    }
    public int getBankInternalReference(byte[] buffer, int offset) {
        return ByteUtils.copySpaceTrimmed(this.buffer, this.baseOffset + Offsets.bankInternalReference, Lengths.bankInternalReference, buffer, offset);
    }

    public void setSide(byte side) {
        this.buffer.put(this.baseOffset + Offsets.side, side);
    }
    public byte getSide() {
        return this.buffer.get(this.baseOffset + Offsets.side);
    }

    public void setQuantity(int quantity) {
        this.buffer.putInt(this.baseOffset + Offsets.quantity, quantity);
    }
    public int getQuantity() {
        return this.buffer.getInt(this.baseOffset + Offsets.quantity);
    }

    public void setInstrument(int instrument) {
        this.buffer.putInt(this.baseOffset + Offsets.instrument, instrument);
    }
    public int getInstrument() {
        return this.buffer.getInt(this.baseOffset + Offsets.instrument);
    }

    public void setPrice(long price) {
        this.buffer.putLong(this.baseOffset + Offsets.price, price);
    }
    public long getPrice() {
        return this.buffer.getLong(this.baseOffset + Offsets.price);
    }

    public void setTif(int tif) {
        this.buffer.putInt(this.baseOffset + Offsets.tif, tif);
    }
    public int getTif() {
        return this.buffer.getInt(this.baseOffset + Offsets.tif);
    }

    public ByteStringBuilder toString(ByteStringBuilder sb) {
        sb.append("EnterOrderRequest");
        sb.append("[type=").append(getType()).append(']');
        sb.append("[token=").append(getToken()).append(']');
        sb.append("[bankInternalReference=").append(this.buffer, this.baseOffset + Offsets.bankInternalReference, Lengths.bankInternalReference).append(']');
        sb.append("[side=").append(getSide()).append(']');
        sb.append("[quantity=").append(getQuantity()).append(']');
        sb.append("[instrument=").append(getInstrument()).append(']');
        sb.append("[price=").append(getPrice()).append(']');
        sb.append("[tif=").append(getTif()).append(']');
        return sb;
    }

    public String toString() {
        return toString(new ByteStringBuilder()).toString();
    }
}
