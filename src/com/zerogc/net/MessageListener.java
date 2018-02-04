package com.zerogc.net;

import java.nio.ByteBuffer;

public interface MessageListener {
    void onMessage(MessageSource source, ByteBuffer buffer);
}
