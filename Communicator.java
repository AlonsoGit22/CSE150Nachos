package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    lock = new Lock();
    speakerReady = new Condition(lock);
    listenerReady = new Condition(lock);
    speakerWaiting = false;
    listenerWaiting = false;


    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
    	lock.acquire();
    	while(listenerWaiting = false)
    		listenWaiting.sleep();
    	listenerWaiting = false;
    	message  = word;
    	speakerReady.wake();
    	lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {

   		lock.acquire();
   		listenReady.wake();
   		while(speakerWaiting = false)
   			speakerReady.sleep();
   		speakerWaiting = false;
   		lock.release();
   		return message;

	return 0;
    }
    
    private Lock lock;
    private Condition speakerReady;
    private Condition listenerReady;
    private boolean speakerWaiting;
    private boolean listenerWaiting;

    private int message;
    private int sent;
    private int recieved;

}
