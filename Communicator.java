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

	private Lock lock; //the lock for the communicators
    private Condition speakerQueue; //queue for speakers
    private Condition listenerQueue; //queue for listeners

    private boolean speakerWaiting; //used to tell if there is a speaker waiting
    private boolean listenerWaiting; //used to tell if there is a listener waiting
    
    private Condition speakerSend; //used to send message
    private Condition listenerReceive; //used to receive message

    private boolean sent; //used to confirm speaker message has been sent
    private boolean received; //used to confirm listener has been received

    private int message; //the message
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    lock = new Lock();
    speakerQueue = new Condition(lock);
    listenerQueue = new Condition(lock);
    speakerWaiting = false;
    listenerWaiting = false;
    delivered = false;
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
    	while(speakerWaiting){
    		speakerQueue.sleep(); //puts next speaker in queue
    	}

    	speakerWaiting = true;
    	message  = word;

    	while(!listenerWaiting || !delivered || !sent){ //if there are no listeners waiting, then wake the receive queue to send message
    		listenerReceive.wake();
    		speakerSend.sleep();
    	}
    	
    	sent = true;
    	speakerWaiting = false;
    	speakerQueue.wake(); //wakes next speaker
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
   		//listenerReady.wake();
   		while(listenerWaiting){
   			listenerQueue.sleep(); //puts next listener in queue
   		}

   		listenerWaiting = true;

   		while(!speakerWaiting || !sent){ //puts listener to sleep if there are no speakers ready or the message has been delivered
   			listenerReceive.sleep();
   		}
   		speakerSend.wake();
   		
   		delivered = true;
   		listenerWaiting = false;

   		listenerQueue.wake(); 
   		lock.release();
   		return message;

    }
    
}
