package edu.cmu.ml.rtw.generic.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ThreadMapper<S, T> {
	public static interface Fn<S, T> {
		T apply(S item);
	}
	
	private class Task implements Callable<T> {
		private S item;
		
		public Task(S item) {
			this.item = item;
		}

		@Override
		public T call() throws Exception {
			return fn.apply(this.item);
		}
	}
	
	private Fn<S, T> fn;
	
	public ThreadMapper(Fn<S, T> fn) {
		this.fn = fn;
	}
	
	public List<T> run(Collection<S> items, int maxThreads) {
		List<T> results = new ArrayList<T>(items.size());
		ExecutorService threadPool = Executors.newFixedThreadPool(maxThreads);
		List<Task> tasks = new ArrayList<Task>();
 		for (S item : items) {
			tasks.add(new Task(item));
		}
		
		try {
			List<Future<T>> futureResults = threadPool.invokeAll(tasks);
			threadPool.shutdown();
			threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			for (Future<T> futureResult : futureResults) {
				T result = futureResult.get();
				results.add(result);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return results;
	}
}
