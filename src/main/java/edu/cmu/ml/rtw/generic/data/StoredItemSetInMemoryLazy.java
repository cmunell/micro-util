package edu.cmu.ml.rtw.generic.data;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import edu.cmu.ml.rtw.generic.data.Serializer.Index;
import edu.cmu.ml.rtw.generic.data.store.StoredCollection;
import edu.cmu.ml.rtw.generic.util.Pair;

public class StoredItemSetInMemoryLazy<E, I extends E> extends StoredItemSet<E, I> {
	protected class ItemSetIterator implements Iterator<E> {
		private Iterator<String> indexIterator;
		
		public ItemSetIterator() {
			this.indexIterator = StoredItemSetInMemoryLazy.this.items.keySet().iterator();
		}
		
		@Override
		public boolean hasNext() {
			return this.indexIterator.hasNext();
		}

		@Override
		public E next() {
			return StoredItemSetInMemoryLazy.this.getItemByIndex(this.indexIterator.next(), false);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	protected ConcurrentSkipListMap<String, Pair<Object, E>> items;
	protected String indexField;
	protected String name;
	
	public StoredItemSetInMemoryLazy(StoredCollection<I, ?> storedItems) {
		this(storedItems, -1);
	}
	
	public StoredItemSetInMemoryLazy(StoredCollection<I, ?> storedItems, int sizeLimit) {
		this(storedItems, sizeLimit, new Random(), false);
	}
	
	public StoredItemSetInMemoryLazy(StoredCollection<I, ?> storedItems, int sizeLimit, Random r, boolean initEmpty) {
		super(storedItems);
		this.items = new ConcurrentSkipListMap<String, Pair<Object, E>>();
		this.indexField = this.storedItems.getSerializer().getIndices().get(0).getField();
		
		if (!initEmpty) {
			Set<String> indexFieldValues = this.storedItems.getIndex(this.indexField, sizeLimit, r);
			for (String indexFieldValue : indexFieldValues)
				this.items.put(indexFieldValue, new Pair<Object, E>(new Object(), null));
		}
		
		this.name = this.storedItems.getName();
	}
	
	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Iterator<E> iterator() {
		return new ItemSetIterator();
	}
	
	public E getItemByIndex(String indexValue, boolean keepInMemory) {
		if (this.items.containsKey(indexValue)) {
			Pair<Object, E> lockAndDocument = this.items.get(indexValue);
		
			if (keepInMemory) {
				synchronized (lockAndDocument.getFirst()) {
					if (lockAndDocument.getSecond() == null)
						lockAndDocument.setSecond(this.storedItems.getFirstItemByIndex(this.indexField, indexValue));
				}
				
				return lockAndDocument.getSecond();
			} else {
				if (lockAndDocument.getSecond() == null)
					return this.storedItems.getFirstItemByIndex(this.indexField, indexValue);
				else
					return lockAndDocument.getSecond();
			}
		} else {
			return null;
		}
	}
	
	public boolean addItem(I item) {
		if (!this.storedItems.addItem(item))
			return false;
		
		this.items.put(getItemIndexValue(item), new Pair<Object, E>(new Object(), null));
		return true;
	}
	
	private String getItemIndexValue(I item) {
		List<Index<I>> indices = this.storedItems.getSerializer().getIndices();
		for (int i = 0; i < this.storedItems.getSerializer().getIndices().size(); i++) {
			if (indices.get(i).getField().equals(this.indexField))
				return indices.get(i).getValue(item).toString();
		}
		return null;
	}
}
