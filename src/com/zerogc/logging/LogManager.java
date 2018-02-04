package com.zerogc.logging;

import java.io.IOException;
import java.util.HashMap;

import com.zerogc.core.ByteStringBuilder;

public class LogManager {
    // TODO: Use ZeroGC HashMap
    private static HashMap<byte[], Logger> loggers = new HashMap<byte[], Logger>();

    private static LogWriter logWriter;

    private static LogManager instance;
    public static LogManager getInstance() {
        return instance;
    }

    public static LogManager initialize(LogWriter logWriter) {
        return instance = new LogManager(logWriter);
    }

    private LogManager(LogWriter logWriter) {
        this.logWriter = logWriter;
    }

    public LogWriter getLogWriter() {
        return logWriter;
    }

    public void open() throws IOException {
        logWriter.open();
    }

    public void close() throws IOException {
        logWriter.close();
    }

    public static Logger getLogger() {
        String name = Thread.currentThread().getStackTrace()[2].getClassName();
        name =  name.substring(name.lastIndexOf(".")+1);
        return getLogger(name);
    }

    public static Logger getLogger(Class clazz) {
        return getLogger(clazz.getSimpleName());
    }

    public static Logger getLogger(String name) {
        return getLogger(name.getBytes());
    }

    public static Logger getLogger(byte[] name) {
        Logger logger;
        if (loggers.containsKey(name)) {
            logger = loggers.get(name);
        } else {
            logger = new Logger(name, logWriter);
            loggers.put(name, logger);
        }
        return logger;
    }

    static long count = 0;

    public static void main(String[] args) {
        try {
            //LogManager.initialize(new ConsoleLogWriter()).open();
            //LogManager.initialize(new FileLogWriter("logfile")).open();
            LogManager.initialize(new AsyncFileLogWriter("logfile")).open();
            //Logger log = LogManager.getLogger(LogManager.class);
        } catch (IOException e) {
            System.err.println("Caught Exception: " + e.getMessage());
            e.printStackTrace(System.err);
        }

        new Thread(new Runnable() {
            @Override
            public void run(){
                ByteStringBuilder sb = new ByteStringBuilder(1024);
                long sum = 0;
                while (true) {
                    long iterations = count;
                    sum += count;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    iterations = count - iterations;
                    sb.setLength(0);
                    sb.appendTime(System.currentTimeMillis()).append(": iterations: ").append(iterations).append(" latency ").append(1000000.0 / iterations).append(" us ").append(sum).append(System.lineSeparator());
                    System.out.write(sb.getBuffer(), 0, sb.getLength());
                }
            }
        }).start();

        Logger log = LogManager.getLogger();
        while (true) {
            log.info("Hello world");
            count++;
        }

    }
}
