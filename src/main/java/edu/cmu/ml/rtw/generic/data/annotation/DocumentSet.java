package edu.cmu.ml.rtw.generic.data.annotation;

import java.util.Set;

public interface DocumentSet<E extends Document, I extends E> extends Iterable<E>  {		
	public String getName();
	public E getDocumentByName(String name);
	public Set<String> getDocumentNames();
}
