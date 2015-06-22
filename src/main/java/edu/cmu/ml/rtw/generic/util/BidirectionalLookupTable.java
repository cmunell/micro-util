package edu.cmu.ml.rtw.generic.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * BidirectionalLookupTable is a data structure that maps objects 
 * of type S to objects of type T.  The structure allows 
 * constant-time lookups of objects in both directions (lookup an
 * S givent a T, or lookup a T given an S).
 * 
 * @author Bill McDowell
 *
 * @param <S> source type
 * @param <T> target type
 */
public class BidirectionalLookupTable<S, T> {
	private Map<S, T> forwardLookup;
	private Map<T, S> reverseLookup;
	
	public BidirectionalLookupTable() {
		this(null);
	}
	
	public BidirectionalLookupTable(Map<S, T> forwardLookup) {
		if (forwardLookup == null) {
			this.forwardLookup = new HashMap<S, T>();
			this.reverseLookup = new HashMap<T, S>();
		} else {
			this.forwardLookup = forwardLookup;
			this.reverseLookup = new HashMap<T, S>();

			for (Entry<S, T> entry : forwardLookup.entrySet())
				this.reverseLookup.put(entry.getValue(), entry.getKey());
		}
	}
	
	public boolean containsKey(S key) {
		return this.forwardLookup.containsKey(key);
	}
	
	public boolean reverseContainsKey(T key) {
		return this.reverseLookup.containsKey(key);
	}
	
	public T get(S key) {
		return this.forwardLookup.get(key);
	}
	
	public S reverseGet(T value) {
		return this.reverseLookup.get(value);
	}
	
	public T put(S key, T value) {
		this.reverseLookup.put(value, key);
		return this.forwardLookup.put(key, value);
	}
	
	public int size() {
		return this.forwardLookup.size();
	}
	
	public Set<S> keySet() {
		return this.forwardLookup.keySet();
	}
	
	public Set<T> reverseKeySet() {
		return this.reverseLookup.keySet();
	}
}
