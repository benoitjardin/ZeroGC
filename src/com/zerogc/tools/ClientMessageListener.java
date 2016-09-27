package com.zerogc.tools;

import java.nio.ByteBuffer;

public interface ClientMessageListener {
    void onClientMessage(ByteBuffer buffer);
}