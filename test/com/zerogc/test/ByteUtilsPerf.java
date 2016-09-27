package com.zerogc.test;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import sun.misc.Unsafe;

import com.zerogc.util.Level;
import com.zerogc.util.Logger;

/**
 * @author Benoit Jardin
 */


//javah -jni -classpath $DEVELOP/zerogc/dist/ZeroGC-0.0.0.0.jar com.zerogc.test.Jni
//DEVELOP=/home/macgarden/develop
//java -Djava.library.path=$DEVELOP/zerogc/native -cp $DEVELOP/zerogc/dist/ZeroGC-0.0.0.0.jar com.zerogc.test.Jni

public class ByteUtilsPerf {

    protected static final Unsafe unsafe;

    static {
        System.loadLibrary("zerogc");

        try {
           Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
           field.setAccessible(true);
           unsafe = (sun.misc.Unsafe) field.get(null);
        } catch (Exception e) {
           throw new AssertionError(e);
        }
        
    }
    
    static final Logger log = new Logger("Jni");
    private static int count = 0;
    private static int sum = 0;

    private static ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
    private static byte[] buffer = new byte[1024];
    private static final long address = unsafe.allocateMemory(1024);

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
            //byteBuffer.clear();
            //byteBuffer.putInt(0); // Iterations: 164 425 424
            //ByteUtils.putIntLE(buffer, 0, 0); // Iterations: 439 522 443
            unsafe.putInt(address, 0); // Iterations: 12 647 628
            ++count;
        }
    }
}
