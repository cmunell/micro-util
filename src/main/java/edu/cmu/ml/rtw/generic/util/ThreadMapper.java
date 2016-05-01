package edu.cmu.ml.rtw.generic.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * ThreadMapper maps a function onto a collection
 * of objects on parallel threads.
 * 
 * @author Bill McDowell
 *
 * @param <S>
 * @param <T>
 */
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
	
	private class PartitionTask implements Callable<List<T>> {
		private List<S> items;
		
		public PartitionTask(List<S> items) {
			this.items = items;
		}

		@Override
		public List<T> call() throws Exception {
			List<T> results = new ArrayList<>();
			for (S item : items)
				results.add(fn.apply(item));
			return results;
		}
	}
	
	private Fn<S, T> fn;
	
	public ThreadMapper(Fn<S, T> fn) {
		this.fn = fn;
	}
	
	public List<T> run(Collection<S> items, int maxThreads) {
		return run(items, maxThreads, false);
	}

	public List<T> run(Collection<S> items, int maxThreads, boolean partition) {
		if (partition)
			return runPartitioned(items, maxThreads);
		else
			return runIndividual(items, maxThreads);
	}
	
	private List<T> runPartitioned(Collection<S> items, int maxThreads) {
		List<T> results = new ArrayList<T>(items.size());
		if (items.size() == 0)
			return results;
		
		List<PartitionTask> tasks = new ArrayList<>();
		int itemsPerPart = (int)Math.ceil(items.size() / (double)maxThreads);
		List<S> part = null;
		int i = 0;
		for (S item : items) {
			if (i % itemsPerPart == 0) {
				if (part != null)
					tasks.add(new PartitionTask(part));
				part = new ArrayList<>();
			}
			part.add(item);
			i++;
		}
		
		if (part != null && part.size() != 0)
			tasks.add(new PartitionTask(part));
		
		ExecutorService threadPool = Executors.newFixedThreadPool(maxThreads);
		try {
			List<Future<List<T>>> futureResults = threadPool.invokeAll(tasks);
			threadPool.shutdown();
			threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			for (Future<List<T>> futureResult : futureResults) {
				List<T> result = futureResult.get();
				results.addAll(result);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return results;
	}
	
	private List<T> runIndividual(Collection<S> items, int maxThreads) {
		List<T> results = new ArrayList<T>(items.size());
		if (items.size() == 0)
			return results;
		
		List<Task> tasks = new ArrayList<Task>();
 		for (S item : items) {
			tasks.add(new Task(item));
		}
		
 		ExecutorService threadPool = Executors.newFixedThreadPool(maxThreads);
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
