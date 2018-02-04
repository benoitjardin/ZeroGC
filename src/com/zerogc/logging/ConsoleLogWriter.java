package com.zerogc.logging;

public class ConsoleLogWriter extends BaseLogWriter {

    @Override
    public void open() {
        stream = System.err;
    }

}
