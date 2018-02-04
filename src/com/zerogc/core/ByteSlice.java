package com.zerogc.core;

public class ByteSlice implements ToByteString, Comparable<ByteSlice> {
    private byte[] buffer;
    private int offset;
    private int length;

    public ByteSlice() {
    }

    public ByteSlice(byte[] buffer) {
        this.buffer = buffer;
        this.offset = 0;
        this.length = buffer.length;
    }

    public ByteSlice(byte[] buffer, int offset, int length) {
        this.buffer = buffer;
        this.offset = offset;
        this.length = length;
    }

    public ByteSlice set(byte[] buffer, int offset, int length) {
        this.buffer = buffer;
        this.offset = offset;
        this.length = length;
        return this;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    @Override
    public int hashCode() {
        return ByteUtils.hashCode(buffer, offset, length);
    }

    @Override
    public int compareTo(ByteSlice rhs) {
        return ByteUtils.compareTo(buffer, offset, length, rhs.buffer, rhs.offset, rhs.length);
    }

    @Override
    public ByteStringBuilder toByteString(ByteStringBuilder sb) {
        return sb.append(buffer, offset, length);
    }

    @Override
    public String toString() {
        return new String(buffer, offset, length);
    }
}
