package com.zerogc.net;

import java.nio.ByteBuffer;

public interface ClientMessageListener {
    void onClientMessage(ByteBuffer buffer);
}