package edu.cmu.ml.rtw.generic.data.annotation;

import java.util.List;
import java.util.Random;
import java.util.Set;

import edu.cmu.ml.rtw.generic.util.ThreadMapper;

public interface DocumentSet<E extends Document, I extends E> extends Iterable<E>  {		
	public String getName();
	public E getDocumentByName(String name);
	public Set<String> getDocumentNames();
	<T> List<T> map(ThreadMapper.Fn<E, T> fn, int threads, Random r);
}
