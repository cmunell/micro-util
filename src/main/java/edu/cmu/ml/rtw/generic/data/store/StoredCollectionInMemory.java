package edu.cmu.ml.rtw.generic.data.store;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.Serializer;
import edu.cmu.ml.rtw.generic.data.Serializer.Index;
import edu.cmu.ml.rtw.generic.util.Pair;


public class StoredCollectionInMemory<I, S> extends StoredCollection<I, S> {
	private class InMemoryIterator implements Iterator<I> {
		private Iterator<Pair<List<Object>, S>> iterator;
		
		public InMemoryIterator(Iterator<Pair<List<Object>, S>> iterator) {
			this.iterator = iterator;
		}
		
		@Override
		public boolean hasNext() {
			return this.iterator.hasNext();
		}

		@Override
		public I next() {
			Pair<List<Object>, S> next = this.iterator.next();
			return StoredCollectionInMemory.this.serializer.deserialize(next.getSecond());
		}
		
	}
	
	private Serializer<I, S> serializer;
	private StorageInMemory<S> storage;
	
	/*
	 * FIXME This data structure makes this class super inefficient, but that's
	 * okay because it's just for testing.
	 */
	private List<Pair<List<Object>, S>> collection;
	
	public StoredCollectionInMemory(String name, StorageInMemory<S> storage) {
		super(name);
		this.storage = storage;
		this.collection = new ArrayList<Pair<List<Object>, S>>();
	}
	
	public StoredCollectionInMemory(String name, Serializer<I, S> serializer) {
		super(name);
		this.serializer = serializer;
		this.collection = new ArrayList<Pair<List<Object>, S>>();
	}

	@Override
	public Iterator<I> iterator() {
		return new InMemoryIterator(this.collection.iterator());
	}

	@Override
	public Serializer<I, S> getSerializer() {
		if (this.serializer == null)
			this.serializer = this.storage.getCollectionSerializer(this.name);
		return this.serializer;
	}

	@Override
	public Set<String> getIndex(String indexField, int limit, Random r) {
		int indexNum = getIndexNum(indexField);
		if (indexNum < 0)
			return null;
		
		Set<String> index = new HashSet<String>();

		for (Pair<List<Object>, S> item : this.collection) {
			index.add(item.getFirst().get(indexNum).toString());
			if (limit > 0 && index.size() >= limit)
				break;
		}
		
		return index;
	}

	@Override
	public List<I> getItemsByIndex(String indexField, Object indexValue) {
		List<String> indexFields = new ArrayList<String>();
		List<Object> indexValues = new ArrayList<Object>();
		indexFields.add(indexField);
		indexValues.add(indexValue);
		return getItemsByIndices(indexFields, indexValues);
	}

	@Override
	public List<I> getItemsByIndices(List<String> indexFields,
			List<Object> indexValues) {
		List<I> items = new ArrayList<I>();
	
		for (Pair<List<Object>, S> item : this.collection) {
			boolean itemMatched = true;
			for (int i = 0; i < indexFields.size(); i++) {
				int indexNum = getIndexNum(indexFields.get(i));
				if (!item.getFirst().get(indexNum).equals(indexValues.get(i))) {
					itemMatched = false;
					break;
				}
			}
			
			if (itemMatched) {
				items.add(this.serializer.deserialize(item.getSecond()));
			}
		}
		
		return items;
	}

	@Override
	public boolean addItem(I item) {
		List<Object> indexValues = new ArrayList<Object>();
		for (Index<I> index : this.serializer.getIndices())
			indexValues.add(index.getValue(item));
		
		// FIXME Note that this allows for multiple items under
		// the same tuple of index values
		this.collection.add(new Pair<List<Object>, S>(indexValues, this.serializer.serialize(item)));
		
		return true;
	}

	@Override
	public boolean addItems(List<I> items) {
		for (I item : items)
			if (!addItem(item))
				return false;
		return true;
	}
	
	private int getIndexNum(String field) {
		List<Index<I>> indices = this.serializer.getIndices();
		for (int i = 0; i < indices.size(); i++)
			if (indices.get(i).getField().equals(field))
				return i;
		return -1;
	}
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		for (Pair<List<Object>, S> obj : this.collection) {
			for (Object indexValue : obj.getFirst())
				str.append(indexValue.toString()).append(" ");
			str.append("\n");
			str.append(obj.getSecond().toString());
			str.append("\n\n");
		}
		
		return str.toString();
	}

	@Override
	public List<BufferedReader> getReadersByIndex(String indexField,
			Object indexValue) {
		// FIXME Implement
		throw new UnsupportedOperationException();
	}

	@Override
	public List<BufferedReader> getReadersByIndices(List<String> indexFields,
			List<Object> indexValues) {
		// FIXME Implement
		throw new UnsupportedOperationException();
	}

	@Override
	public Storage<?, S> getStorage() {
		return this.storage;
	}
}
