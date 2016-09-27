package com.zerogc.test;

import java.nio.ByteBuffer;

import com.zerogc.util.Level;
import com.zerogc.util.Logger;

//javah -jni -classpath $DEVELOP/zerogc/dist/ZeroGC-0.0.0.0.jar com.zerogc.test.Jni
//DEVELOP=/home/macgarden/develop
//java -Djava.library.path=$DEVELOP/zerogc/native -cp $DEVELOP/zerogc/dist/ZeroGC-0.0.0.0.jar com.zerogc.test.Jni

public class JniPerf {
	static final Logger log = new Logger(JniPerf.class.getSimpleName());
	private static int count = 0;
	private static int sum = 0;

	// Check the fastest way to transfer data to JNI
    public static native int callDirectByteBuffer(ByteBuffer byteBuffer);
    public static native int callByteArray(byte[] buffer);
    //public static native int callAddress(long address, int len);

    //protected static final Unsafe unsafe;

    static {
		System.loadLibrary("zerogc");

		try {
		   //Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		   //field.setAccessible(true);
		   //unsafe = (sun.misc.Unsafe) field.get(null);
		} catch (Exception e) {
		   throw new AssertionError(e);
		}
		
    }
    
    private static ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
	private static ByteBuffer directByteBuffer = ByteBuffer.allocateDirect(1024);
	private static byte[] byteArray = new byte[1024];
    //private static final long address = unsafe.allocateMemory(1024);

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
            directByteBuffer.clear();

            //for (int i=0; i<200; i++) {
                //byteBuffer.putInt(0);         
                //directByteBuffer.putInt(0);
                //ByteUtils.putInt(byteArray, 0, 0);
                //unsafe.putInt(address, 0);
                directByteBuffer.putInt(0);
            //}
            
            //callByteArray(byteBuffer.array());        //   974 577
			callDirectByteBuffer(directByteBuffer);   // 6 346 438 | 19 808 679  
            //callByteArray(byteArray);                 // 1 586 006
			//callAddress(address, 1024);               // 9 553 518
		    //callAddress(((sun.nio.ch.DirectBuffer)directByteBuffer).address() + directByteBuffer.position(), directByteBuffer.remaining());
		                                                // 7 960 394 | 52 460 839
            
            
			                                            //  200 ints |      1 int
            //callByteArray(byteBuffer.array());        //   974 577 | 10 028 663
            //callDirectByteBuffer(directByteBuffer);   // 6 765 650 | 19 968 086
            //callByteArray(byteArray);                 // 1 586 006 |  9 676 943
            //callAddress(address, 1024);               // 9 553 518 | 65 401 184
            //callAddress(directByteBuffer.address())   // 7 931 181 | 52 460 839
			++count;
		}
	}
}
