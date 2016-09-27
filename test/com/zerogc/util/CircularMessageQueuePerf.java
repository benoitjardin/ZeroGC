package com.zerogc.util;


/**
 * @author Benoit Jardin
 */

// java -cp $DEVELOP/zerogc/dist/ZeroGC-0.0.0.0.jar com.zerogc.util.CircularMessageQueuePerf

public class CircularMessageQueuePerf {
    static final Logger log = new Logger("ArrayPerf");
    private static int count = 0;
    private static int sum = 0;
    
    
    public static class Test implements Runnable {
    	private CircularMessageQueue from;
    	private CircularMessageQueue to;
    	private byte[] buffer = new byte[1024];

    	public Test(CircularMessageQueue from, CircularMessageQueue to) {
			this.from = from;
			this.to = to;
		}
    	
    	@Override
    	public void run() {
    	    long seqno = 0;
    		while (true) {
    			int len = from.receive(buffer, 0);
				if (len == -1) {
	    			Thread.yield();
				    continue;
				}
                to.send(buffer, 0, len);
    			count++;
    			sum += seqno;
    		}
    	}
    }
    
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

    	CircularMessageQueue cmq1 = new CircularMessageQueue(1024*1024);
    	CircularMessageQueue cmq2 = new CircularMessageQueue(1024*1024);
    	byte[] buffer = new byte[8];
    	for (int i=0; i<100; i++) {
    		cmq1.send(buffer, 0, buffer.length);
    	}

    	new Thread(new Test(cmq1, cmq2)).start();
    	new Test(cmq2, cmq1).run();
    }
}
