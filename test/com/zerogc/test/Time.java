package com.zerogc.test;

import com.zerogc.core.Native;
import com.zerogc.logging.Level;
import com.zerogc.logging.LogManager;
import com.zerogc.logging.Logger;

// java -Djava.library.path=${DEVELOP}/zerogc/native -cp ${DEVELOP}/zerogc/dist/ZeroGC-0.0.0.0.jar com.zerogc.test.Time

public class Time {
    static final Logger log = LogManager.getLogger("Time");
    private static int count = 0;
    private static long sum = 0;
    
    public static void main(String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                int prevCount = count;
                while (true) {
                    try {
                        Thread.sleep(1000);
                        long end = System.currentTimeMillis();
                        int iterations = count - prevCount;
                        prevCount = count;
                        log.log(Level.INFO, log.getSB().append("Iterations: ").append(iterations).append(" in ").append(end-start).append("ms, ")
                                .append(iterations*1000.0/(end-start)).append(" iterartions/sec")
                                .append(", sum: ").append(sum));
                        start = end;
                    } catch (InterruptedException e) {
                    }
                }
            }
        }).start();
        log.log(Level.INFO, log.getSB().append("Start the performance test loop"));

        while (true) {
            long micros = Native.currentTimeMicros();
            ++count;
        }
    }
}
