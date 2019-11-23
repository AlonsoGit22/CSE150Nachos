package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.threads.PriorityScheduler.PriorityQueue;
import nachos.threads.PriorityScheduler.ThreadState;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocate a new lottery scheduler.
     */
	public LotteryScheduler() {
	}
	/**
	 * Allocate a new lottery thread queue.
	 *
	 * @param	transferPriority	<tt>true</tt> if this queue should
	 *					transfer tickets from waiting threads
	 *					to the owning thread.
	 * @return	a new lottery thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		// implement me
		return new LotteryQueue(transferPriority);
	}

	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null) {
			thread.schedulingState = new ThreadState(thread);
		}
		
		return (ThreadState) thread.schedulingState;
	}
	
	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());
		Lib.assertTrue(priority >= priorityMinimum && priority <= priorityMaximum);
		getThreadState(thread).setPriority(priority);
	}
	
	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();
		KThread thread = KThread.currentThread();
		int priority = getPriority(thread);
		
		if (priority == priorityMaximum) {
			return false;
		}
		
		setPriority(thread, priority + 1);
		Machine.interrupt().restore(intStatus);
		return true;
	}
	
	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();
		KThread thread = KThread.currentThread();
		int priority = getPriority(thread);
		
		if (priority == priorityMinimum) {
			return false;
		}
		
		setPriority(thread, priority - 1);
		Machine.interrupt().restore(intStatus);
		return true;
	}
	
	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	
	public static final int priorityDefault = 1;
	
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	
	public static final int priorityMinimum = 1;
	
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	
	public static final int priorityMaximum = Integer.MAX_VALUE;
	
	protected class LotteryQueue extends ThreadQueue {
		private java.util.HashMap<ThreadState, Integer> waitQ;
		boolean TPriority;
		private ThreadState acquired;
		private int sum;
		boolean sumCh;
		
		LotteryQueue(boolean transferPriority) {
			waitQ = new java.util.HashMap<ThreadState, Integer>();
			this.TPriority = transferPriority;
			sum = 0;
			sumCh = true;
		}
	
		public void signal(ThreadState ts, int tickets) {
			if (TPriority == true && acquired != null) {
				if (acquired == ts) {
					acquired.setEffectivePriority(tickets - sum);
				} else {
					acquired.effectivePriorityUpdated();
				}
			}
		}
	
		public void updateWaitingThread(ThreadState ts, int tickets) {
			if (waitQ.size() == 0) {
				return;
			}
			
			// update the value of a threadstate on the queue
			waitQ.put(ts, new Integer(Math.min(tickets,  threadTickets(ts))));
			
			// flag the queue that its sum has changed and announce to others
			sumCh = true;
			signal(ts, tickets);
		}
		
		public void dequeueWaitingThread(ThreadState ts) {
			Lib.assertTrue(waitQ.containsKey(ts));
			
			// remove the thread from the lottery queue
			Integer tickets = waitQ.remove(ts);
			
			// flag the queue that its sum has changed
			sumCh = true;
			signal(ts, tickets.intValue());
		}
		// DONE
		
		public void enqueueWaitingThread(ThreadState ts) {
			int tickets = threadTickets(ts);
			Lib.assertTrue(!waitQ.containsKey(ts));
			waitQ.put(ts, new Integer(tickets));
			
			// flag the queue that its sum has changed
			sumCh = true;
			signal(ts, tickets);
		}
		
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			ThreadState ts = getThreadState(thread);
			enqueueWaitingThread(ts);
			ts.waitForAccess(this);
		}
		
		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			ThreadState ts = getThreadState(thread);
			
			// unacquire previous
			if (acquired != null) {
				acquired.unacquire(this);
			}
			
			// if thread on waiting queue, dequeue it
			if (waitQ.containsKey(ts)) {
				dequeueWaitingThread(ts);
			}
			
			this.acquired = ts;
			ts.acquire(this);
		}
		
		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			ThreadState ts = pickNextThread();
			
			if (ts == null) {
				if (acquired != null) {
					acquired.unacquire(this);
				}
				return null;
			}
			
			acquire(ts.thread);
			return ts.thread;
		}
		
		// Hold a lottery when picks a new thread - DONE
		protected ThreadState pickNextThread() {
			int total = 0;
			int num;
			ThreadState result = null;
			Random rand = new Random();
			
			if (waitQ.size() == 0) {
				return null;
			}
			
			num = rand.nextInt(getTicketSum()) + 1;
			Iterator<ThreadState> it = waitQ.keySet().iterator();
			
			while (num > total && it.hasNext()) {
				result = it.next();
				total += waitQ.get(result).intValue();
			}
			return result;
		}
		
		private int getTicketSum(){
			if (sumCh) {
				return updateSum();
			} else {
				return sum;
			}
		}
		
		public int getSum() {
			if(TPriority){
				return getTicketSum();
			} else {
				return 0;
			}
		}
		
		private int updateSum() {
			Lib.assertTrue(sumCh == true);
			int total = 0;
			Map.Entry<ThreadState, Integer> value;
			Iterator<Map.Entry<ThreadState, Integer>> it = waitQ.entrySet().iterator();
			
			while (it.hasNext()) {
				value = it.next();
				total += value.getValue().intValue();
			}
			
			sum = total;
			sumCh = false;
			return sum;
		}
		
		private int threadTickets(ThreadState ts){
			if (TPriority){
				return ts.getEffectivePriority();
			}
			else {
				return ts.getPriority();
			}
		}
		
		public boolean contains(KThread thread) {
			ThreadState ts = getThreadState(thread);
			return waitQ.containsKey(ts);
		}
		
		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			System.out.println("Number of tickets = " + getTicketSum() + "\tNumber of threads on queue = " + size());
			System.out.println(waitQ.toString());
		}
		
		public boolean empty() {
			return waitQ.size() == 0;
		}
		
		public boolean isValid() {
			return !sumCh;
		}
		
		public int size() {
			return waitQ.size();
		}
	}
	
	public class ThreadState extends PriorityScheduler.ThreadState {
		public ThreadState(KThread thread) {
			super(thread);
		}
	
		public void setPriority(int priority) {
			this.priority = priority;
			effectivePriorityUpdated();
		}
		
		public int getEffectivePriority() {
			return effectivePriority;
		}
		
		public void setEffectivePriority(int value) {
			if (value < this.priority) {
				this.effectivePriority = this.priority;
			} else {
				this.effectivePriority = value;
			}
			announcePrioChange();
		}
		
		public int calculateEffectivePriority() {
			int sum = priority;
			int qSum = 0;
			
			Iterator<ThreadQueue> it = myResource.iterator();
			LotteryQueue q;
			
			while (it.hasNext()) {
				q = (LotteryQueue) it.next();
				qSum = q.getSum();
				
				if (Integer.MAX_VALUE - sum < qSum) {
					return Integer.MAX_VALUE;
				}
				sum += qSum;
			}
			return sum;
		}
		
		void effectivePriorityUpdated() {
			int newEffectivePriority = calculateEffectivePriority();
			
			if (newEffectivePriority != effectivePriority) {
				effectivePriority = newEffectivePriority;
				announcePrioChange();
			}
		}
		
		// Mark the waitQ the queue that this thread is waiting on
		public void waitForAccess(ThreadQueue waitQ) {
			myResource.add(waitQ);
		}
		
		// Thread acquires a queue
		public void acquire(ThreadQueue waitQ) {
			myResource.remove(waitQ);
			myResource.add(waitQ);
			effectivePriorityUpdated();
		}
		
		// Unacquire the thread from the queue
		public void unacquire(ThreadQueue noQ) {
			myResource.remove(noQ);
			effectivePriorityUpdated();
		}
		
		// Announce that the thread's effective priority has been changed by
		// asking the queue to update
		void announcePrioChange() {
			
			// starter = true;
			LotteryQueue q;
			Iterator<ThreadQueue> it;
			it = myResource.iterator();
			
			while (it.hasNext()) {
				q = (LotteryQueue) it.next();
				q.updateWaitingThread(this, effectivePriority);
			}
		}
	}
}