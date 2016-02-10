package edu.cmu.ml.rtw.generic.data.annotation;

import java.util.List;
import java.util.Random;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.store.StoredCollection;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;

public abstract class DocumentSet<E extends Document, I extends E> implements Iterable<E>  {
	protected StoredCollection<I, ?> storedDocuments;
	
	public DocumentSet(StoredCollection<I, ?> storedDocuments) {
		this.storedDocuments = storedDocuments;	
	}
	
	public <T> List<T> map(ThreadMapper.Fn<E, T> fn, Random r) {
		return map(fn, 1, r);
	}
	
	public abstract String getName();
	public abstract E getDocumentByName(String name);
	public abstract Set<String> getDocumentNames();
	public abstract <T> List<T> map(ThreadMapper.Fn<E, T> fn, int threads, Random r);
}
