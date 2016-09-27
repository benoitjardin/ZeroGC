package com.zerogc.tools;

import java.nio.ByteBuffer;

public interface MessageListener {
    void onMessage(MessageSource source, ByteBuffer buffer);
}
