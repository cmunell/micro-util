package edu.cmu.ml.rtw.generic.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

import edu.cmu.ml.rtw.generic.data.Serializer.Index;
import edu.cmu.ml.rtw.generic.data.store.StoredCollection;
import edu.cmu.ml.rtw.generic.util.MathUtil;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;
import edu.cmu.ml.rtw.generic.util.ThreadMapper.Fn;

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
	
	@Override
	public boolean addItem(I item) {
		if (!this.storedItems.addItem(item))
			return false;
		
		this.items.put(getItemIndexValue(item), new Pair<Object, E>(new Object(), null));
		return true;
	}
	
	public List<StoredItemSetInMemoryLazy<E, I>> makePartition(int parts, Random random) {
		double[] distribution = new double[parts];
		String[] names = new String[distribution.length];
		for (int i = 0; i < distribution.length; i++) {
			names[i] = this.name + "_" + String.valueOf(i);
			distribution[i] = 1.0/parts;
		}
	
		return makePartition(distribution, names, random);
	}
	
	public List<StoredItemSetInMemoryLazy<E, I>> makePartition(double[] distribution, Random random) {
		String[] names = new String[distribution.length];
		for (int i = 0; i < names.length; i++)
			names[i] = this.name + "_" + String.valueOf(i);
	
		return makePartition(distribution, names, random);
	}
	
	public List<StoredItemSetInMemoryLazy<E, I>> makePartition(double[] distribution, String[] names, Random random) {
		List<Entry<String, Pair<Object, E>>> itemList = new ArrayList<Entry<String, Pair<Object, E>>>();
		itemList.addAll(this.items.entrySet());
		
		List<Integer> itemPermutation = new ArrayList<Integer>();
		for (int i = 0; i < itemList.size(); i++)
			itemPermutation.add(i);
		
		itemPermutation = MathUtil.randomPermutation(random, itemPermutation);
		List<StoredItemSetInMemoryLazy<E, I>> partition = new ArrayList<StoredItemSetInMemoryLazy<E, I>>(distribution.length);
		
		int offset = 0;
		for (int i = 0; i < distribution.length; i++) {
			int partSize = (int)Math.floor(itemList.size()*distribution[i]);
			if (i == distribution.length - 1 && offset + partSize < itemList.size())
				partSize = itemList.size() - offset;
			
			StoredItemSetInMemoryLazy<E, I> part = new StoredItemSetInMemoryLazy<E, I>(this.storedItems, -1, new Random(), true);
			part.name = names[i];
			
			for (int j = offset; j < offset + partSize; j++) {
				Entry<String, Pair<Object, E>> entry = itemList.get(itemPermutation.get(j));
				part.items.put(entry.getKey(), entry.getValue());
			}
			
			offset += partSize;
			partition.add(part);
		}
		
		return partition;
	} 
	
	@Override
	public <T> List<T> map(Fn<E, T> fn, int threads, Random r) {
		List<T> results = new ArrayList<T>();
		List<StoredItemSetInMemoryLazy<E, I>> partition = this.makePartition(threads, r);
		ThreadMapper<StoredItemSetInMemoryLazy<E, I>, Boolean> mapper = new ThreadMapper<StoredItemSetInMemoryLazy<E, I>, Boolean>(
			new Fn<StoredItemSetInMemoryLazy<E, I>, Boolean>() {
				@Override
				public Boolean apply(StoredItemSetInMemoryLazy<E, I> item) {
					for (String indexValue : item.items.keySet()) {
						T result = fn.apply(getItemByIndex(indexValue, true));
						synchronized (results) {
							results.add(result);
						}
					}
					
					return true;
				}
			}
		);
		
		mapper.run(partition, threads);
		
		return results;
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
