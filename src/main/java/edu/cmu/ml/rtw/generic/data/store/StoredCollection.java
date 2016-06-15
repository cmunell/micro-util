package edu.cmu.ml.rtw.generic.data.store;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Random;
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
	
	public BufferedReader getFirstReaderByIndex(String indexName, Object indexValue) {
		List<BufferedReader> items = getReadersByIndex(indexName, indexValue);
		if (items.size() > 0) {
			for (int i = 1; i < items.size(); i++) {
				try {
					items.get(i).close();
				} catch (IOException e) {
					return null;
				}
			}
			return items.get(0);
		} else 
			return null;
	}
	
	public BufferedReader getFirstReaderByIndices(List<String> indexNames, List<Object> indexValues) {
		List<BufferedReader> items = getReadersByIndices(indexNames, indexValues);
		if (items.size() > 0) {
			for (int i = 1; i < items.size(); i++) {
				try {
					items.get(i).close();
				} catch (IOException e) {
					return null;
				}
			}
			return items.get(0);
		} else 
			return null;
	}
	
	public Set<String> getIndex(String indexField) {
		return getIndex(indexField, -1, new Random());
	}
	
	public String getStorageName() {
		Storage<?, ?> storage = getStorage();
		return (storage == null) ? null : storage.getName();
	}
	
	public abstract Serializer<I, S> getSerializer();
	public abstract Set<String> getIndex(String indexField, int limit, Random r);
	public abstract List<I> getItemsByIndex(String indexField, Object indexValue);
	public abstract List<I> getItemsByIndices(List<String> indexFields, List<Object> indexValues);
	public abstract List<BufferedReader> getReadersByIndex(String indexField, Object indexValue);
	public abstract List<BufferedReader> getReadersByIndices(List<String> indexFields, List<Object> indexValues);
	
	public abstract boolean addItem(I item);
	public abstract boolean addItems(List<I> items);
	public abstract Storage<?, S> getStorage();
}
