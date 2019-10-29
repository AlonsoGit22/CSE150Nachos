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
    private Condition speakerSend; //used to send message
    private Condition listenerReceive; //used to receive message
    private int speaker; //used to tell if there is a speaker
    private int listener; //used to tell if there is a listener
    //private boolean sent; //used to confirm speaker message has been sent
    private boolean received; //used to confirm listener has been received

    private int message; //the message



    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        lock = new Lock();  
        speakerSend = new Condition(lock);
        listenerReceive = new Condition(lock);
        speaker = 0;
        listener = 0;
    
        received = false; 
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param   word    the integer to transfer.
     */
    public void speak(int word) {
        lock.acquire();
        while(speaker == 0){
            speakerSend.sleep(); //puts next speaker in queue
        }
        speaker++; //
        message = word;


        while(listener != 0 || !received){ //if there are no listeners waiting, then wake the receive queue to send message
            listenerReceive.wake();
            speakerSend.sleep();
        }
        listener--;
        received = false;
        speakerSend.wake(); //wakes next speaker
        //listenerReceive.wake(); //this might cause the communicator to return the word twice
        
        lock.release(); 
    }


    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return  the integer transferred.
     */    
    public int listen() {

        lock.acquire();
        //listenerReady.wake();
        while(listener == 0){
            listenerReceive.sleep(); //puts next listener in queue
        }
        listener++;

        while(speaker == 0){ //puts listener to sleep if there are no speakers ready or the message has been received
            listenerReceive.sleep();
        }
        //speakerSend.wake();

        received = true;
        //listenerWaiting = false;

        //listenerQueue.wake(); 
        speaker--;
        lock.release();
        return message;
    }
}
