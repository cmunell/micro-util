package edu.cmu.ml.rtw.generic.data;

import java.util.List;

import edu.cmu.ml.rtw.generic.data.store.StoreReference;

public abstract class Serializer<I, S> {
	public interface Index<I> {
		String getField();
		Object getValue(I item);
	}
	

	public I deserialize(S object) {
		return deserialize(object, null);
	}
	
	public I deserializeFromString(String str) {
		return deserializeFromString(str, null);
	}
	
	public abstract String getName();
	public abstract S serialize(I item);
	public abstract I deserialize(S object, StoreReference storeReference);
	public abstract String serializeToString(I item);
	public abstract I deserializeFromString(String str, StoreReference storeReference);
	
	public abstract List<Index<I>> getIndices();
}
