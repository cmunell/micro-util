package edu.cmu.ml.rtw.generic.data;

import edu.cmu.ml.rtw.generic.data.store.StoredCollection;

public abstract class StoredItemSet<E, I extends E> implements Iterable<E>  {
	protected StoredCollection<I, ?> storedItems;
	
	public StoredItemSet(StoredCollection<I, ?> storedItems) {
		this.storedItems = storedItems;	
	}
	
	public StoredCollection<I, ?> getStoredItems() {
		return this.storedItems;
	}
	
	public abstract String getName();
}
