package edu.cmu.ml.rtw.generic.data;

import java.util.List;
import java.util.Random;

import edu.cmu.ml.rtw.generic.data.store.StoredCollection;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;

public abstract class StoredItemSet<E, I extends E> implements Iterable<E>  {
	protected StoredCollection<I, ?> storedItems;
	
	public StoredItemSet(StoredCollection<I, ?> storedItems) {
		this.storedItems = storedItems;	
	}
	
	public StoredCollection<I, ?> getStoredItems() {
		return this.storedItems;
	}
	
	public abstract String getName();
	public abstract <T> List<T> map(ThreadMapper.Fn<E, T> fn, int threads, Random r);
}
