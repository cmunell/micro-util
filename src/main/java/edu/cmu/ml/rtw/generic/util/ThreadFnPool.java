package edu.cmu.ml.rtw.generic.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ThreadFn pool applies a pool of functions 
 * to an object in parallel.
 * 
 * @author Bill McDowell
 *
 * @param <S>
 * @param <T>
 */
public class ThreadFnPool<S, T> {
	public static interface Fn<S, T> {
		T apply(S item);
	}
	
	private class FnThread implements Callable<Boolean> {
		private int index;
		private Fn<S, T> fn;
		
		public FnThread(int index, Fn<S, T> fn) {
			this.fn = fn;
			this.index = index;
		}

		@Override
		public Boolean call()  {
			do {
				try {
					lock.lock();
					threadsWaiting++;
					threadWait.signalAll();
					while (input == null && !stopped) {
						inputReady.await();
					}
					threadsWaiting--;
					lock.unlock();
				
					if (stopped)
						break;
					
					output.set(this.index, this.fn.apply(input));
	
					lock.lock();
					threadsRun++;
					outputReady.signalAll();
					while (input != null && !stopped) {
						allOutputReady.await();
					}
					
					lock.unlock();
				} catch (InterruptedException e) {
					outputReady.signalAll();
					lock.unlock();
					return true;
				}
			} while (!stopped);
			
			lock.lock();
			outputReady.signal();
			lock.unlock();
			
			return true;
		}
	}
	
	private List<Fn<S, T>> fns;
	
	private Lock lock;
	private Condition inputReady;
	private Condition outputReady;
	private Condition allOutputReady;
	private Condition threadWait;
	private boolean stopped;
	
	private S input;
	private List<T> output;
	private int threadsRun = 0;
	private int threadsWaiting = 0;
	
	private ExecutorService threadPool;
	
	public ThreadFnPool(List<Fn<S, T>> fns) {
		this.fns = fns;
		this.lock = new ReentrantLock();
		this.inputReady = this.lock.newCondition();
		this.outputReady = this.lock.newCondition();
		this.allOutputReady = this.lock.newCondition();
		this.threadWait = this.lock.newCondition();
		this.stopped = true;
	}
	
	public synchronized boolean startThreads() {
		this.lock.lock();
		
		if (this.threadPool != null) {
			this.lock.unlock();
			return true;
		}
		
		this.threadPool = Executors.newFixedThreadPool(this.fns.size());
		List<FnThread> fnThreads = new ArrayList<>();
 		for (int i = 0; i < this.fns.size(); i++) {
			fnThreads.add(new FnThread(i, this.fns.get(i)));
		}
 		
 		try {	
 	 		for (int i = 0; i < this.fns.size(); i++) {
 				this.threadPool.submit(fnThreads.get(i));
 			}
		} catch (Exception e) {
			this.stopped = true;
			this.lock.unlock();
			return false;
		}
 		
 		this.stopped = false;
 		
 		this.lock.unlock();
		
		return true;
	}
	
	public synchronized boolean stopThreads() {
		this.lock.lock();
		try {
			this.stopped = true;
			this.threadPool.shutdownNow();
			//this.threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (Exception e) {
			this.stopped = false;
			this.lock.unlock();
			return false;
		}
		
		this.lock.unlock();
	
		return true;
	}
	
	public synchronized List<T> run(S item) {
		if (!startThreads()) {
			return null;
		}
		
		this.lock.lock();
		this.input = item;
		this.output = new ArrayList<>();
		for (int i = 0; i < this.fns.size(); i++)
			this.output.add(null);
		
		try {
			this.inputReady.signalAll();
			while (this.threadsRun < this.fns.size() && !this.stopped) {
				this.outputReady.await();
			}
			
			this.input = null;
			this.allOutputReady.signalAll();
			while (this.threadsWaiting < this.fns.size() && !this.stopped) {
				this.threadWait.await();
			}
		} catch (InterruptedException e) {
			this.lock.unlock();
			return null;
		}
		
		List<T> result = this.output;
		this.input = null;
		this.output = null;
		this.threadsRun = 0;
		if (this.stopped) {
			this.lock.unlock();
			return null;
		}
		
		
		this.lock.unlock();
		
		return result;
	}
}
