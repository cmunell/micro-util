package edu.cmu.ml.rtw.generic.data.store;

import java.util.List;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.Serializer;

public abstract class StoredCollection<I, S> implements Iterable<I> {
	protected String name;
	
	public StoredCollection(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	public I getFirstItemByIndex(String indexName, Object indexValue) {
		List<I> items = getItemsByIndex(indexName, indexValue);
		if (items.size() > 0)
			return items.get(0);
		else 
			return null;
	}
	
	public I getFirstItemByIndices(List<String> indexNames, List<Object> indexValues) {
		List<I> items = getItemsByIndices(indexNames, indexValues);
		if (items.size() > 0)
			return items.get(0);
		else 
			return null;
	}
	
	public abstract Serializer<I, S> getSerializer();
	public abstract Set<String> getIndex(String indexField);
	public abstract List<I> getItemsByIndex(String indexField, Object indexValue);
	public abstract List<I> getItemsByIndices(List<String> indexFields, List<Object> indexValues);
	public abstract boolean addItem(I item);
	public abstract boolean addItems(List<I> items);
}
