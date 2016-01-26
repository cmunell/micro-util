package edu.cmu.ml.rtw.generic.data;

import java.util.List;

public abstract class Serializer<I, S> {
	public interface Index<I> {
		String getField();
		Object getValue(I item);
	}
	
	public abstract String getName();
	public abstract S serialize(I item);
	public abstract I deserialize(S object);
	public abstract String serializeToString(I item);
	public abstract I deserializeFromString(String str);
	
	public abstract List<Index<I>> getIndices();
}
