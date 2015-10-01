package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.data.Context;

/**
 * Fn represents a function from a collection of 
 * type S to a collection of type T.  
 * 
 * These functions are deserialized from a script
 * through a edu.cmu.ml.rtw.generic.data.Context 
 * objecs, and are currently used to construct
 * features of types:
 * 
 * edu.cmu.ml.rtw.generic.data.feature.FeatureTokenSpanFnDataVocab 
 * and edu.cmu.ml.rtw.generic.data.feature.FeatureTokenSpanFnFilteredVocab
 * 
 * This representation of functions was developed in
 * order to allow for a lot of flexibility in defining rules
 * for expanding features vocabularies using the 
 * edu.cmu.ml.rtw.generic.model.SupervisedModelLogistmarGrammression
 * feature grammar model.
 * 
 * @author Bill McDowell
 *
 * @param <S>
 * @param <T>
 */
public abstract class Fn<S, T> extends CtxParsableFunction {
	private int CACHE_SIZE = 20000; // FIXME Set this elsewhere
	
	public enum CacheMode {
		ON,
		OFF
	}
	
	private Map<String, List<T>> listCache;
	private Map<String, Set<T>> setCache;
	
	protected void addToSetCache(String id, Set<T> output) {
		this.setCache.put(id, output);
	}
	
	protected Set<T> lookupSetCache(String id) {
		return this.setCache.get(id);
	}
	
	protected void initializeSetCache() {
		if (this.setCache == null) {
			this.setCache = Collections.synchronizedMap(new LinkedHashMap<String, Set<T>>(CACHE_SIZE, .75F, true) {
				private static final long serialVersionUID = 1L;
	
				// This method is called just after a new entry has been added
			    public boolean removeEldestEntry(Map.Entry<String, Set<T>> eldest) {
			        return size() > CACHE_SIZE;
			    }
			});
		}
	}
	
	protected void addToListCache(String id, List<T> output) {
		this.listCache.put(id, output);
	}
	
	protected List<T> lookupListCache(String id) {
		return this.listCache.get(id);
	}
	
	protected void initializeListCache() {
		if (this.listCache == null) {
			this.listCache = Collections.synchronizedMap(new LinkedHashMap<String, List<T>>(CACHE_SIZE, .75F, true) {
				private static final long serialVersionUID = 1L;
	
				// This method is called just after a new entry has been added
			    public boolean removeEldestEntry(Map.Entry<String, List<T>> eldest) {
			        return size() > CACHE_SIZE;
			    }
			});
		}
	}
	
	public List<T> listCachedCompute(Collection<S> input, String id) {
		initializeListCache();
		
		List<T> output = lookupListCache(id);
		if (output != null)
			return output;
		
		output = listCompute(input);
		
		addToListCache(id, output);
		
		return output;
	}
	
	public Set<T> setCachedCompute(Collection<S> input, String id) {
		initializeSetCache();
		
		Set<T> output = lookupSetCache(id);
		if (output != null) {
			return output;
		}
		
		output = setCompute(input);
		
		addToSetCache(id, output);
		
		return output;
	}
	
	public void clearCaches() {
		this.listCache = null;
		this.setCache = null;
	}
	
	public List<T> listCompute(Collection<S> input, String id, CacheMode cacheMode) {
		if (cacheMode == CacheMode.ON)
			return listCachedCompute(input, id);
		else
			return listCompute(input);
	}

	public Set<T> setCompute(Collection<S> input, String id, CacheMode cacheMode) {
		if (cacheMode == CacheMode.ON)
			return setCachedCompute(input, id);
		else
			return setCompute(input);
	}

	public List<T> listCompute(Collection<S> input) {
		return this.compute(input, new ArrayList<T>());
	}
	
	public Set<T> setCompute(Collection<S> input) {
		return this.compute(input, new HashSet<T>());
	}
	
	/**
	 * @param input
	 * @return output
	 */
	protected abstract <C extends Collection<T>> C compute(Collection<S> input, C output);
	
	/**
	 * @param context
	 * @return a generic instance of the function.  This is used when deserializing
	 * the parameters for the function from a configuration file
	 */
	public abstract Fn<S, T> makeInstance(Context<?, ?> context);

	public List<String> computeRelations(S input, T output) {
		List<String> relations = new ArrayList<String>();
		relations.add(this.referenceName);
		return relations;
	}
}
