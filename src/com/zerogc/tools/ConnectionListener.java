package com.zerogc.tools;

import java.nio.channels.SelectableChannel;

public interface ConnectionListener {
    public void onConnect(SelectableChannel selectableChannel);
    public void onAccept(SelectableChannel selectableChannel);
    public void onClose(SelectableChannel selectableChannel);
}
