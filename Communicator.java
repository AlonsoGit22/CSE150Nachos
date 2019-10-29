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

    //private int speaker; //used to tell if there is a speaker
    private int listener; //used to tell if there is a listener
    //private boolean sent; //used to confirm speaker message has been sent
    private boolean speakerReady;
    //private boolean listenerReady;
    private boolean received; //used to confirm listener has been received

    private int message; //the message



    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        lock = new Lock();  
        speakerSend = new Condition(lock);
        listenerReceive = new Condition(lock);
        //speaker = 0;
        listener = 0;

        speakerReady = false;
        //listenerReady = false;
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
        while(speakerReady){ //first speaker skips this
            speakerSend.sleep(); //puts next speaker in send queue if there is a speaker ready already
        }
        speakerReady = true; //marks the speaker as true, makes the while loop active
        message = word; //transfer the word
        
        //skip while loop if listener is 0, or message is not received
        while(listener == 1 && !received){ //if there are listeners waiting, then wake the receive queue to send message
            listenerReceive.wake(); //wake up listeners in receive queue
            speakerSend.sleep(); //put speaker in send queue
            received = true;
        }
        /*
        while(listener == 0 && !received){ //if there are no listeners waiting, then wake the receive queue to send message
            listenerReceive.wake(); //wake up listeners just incase
            speakerSend.sleep(); //goes to sleep after attempting to wake listeners
        }
        */

        //listener--;
        
        listenerReceive.wake(); //wakes next listener
        //speaker--;
        speakerSend.wake(); //wakes next speaker
        received = false;//sets receive as false after sending the other messages
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
        listener++; //listener at 1

        while(!speakerReady){ //while speakerReady = false
            listenerReceive.sleep();//puts listener in receive queue
        }
        
        listenerReceive.sleep();//sleeps current receive
        //speakerSend.wake();

        listener--;//listener back to 0

        //listenerWaiting = false;
        //listenerReceive.wake(); 
        
        lock.release();
        return message;
    }
}
