package edu.cmu.ml.rtw.generic.cluster;

import java.util.List;

/**
 * Clusterer represents a method for assigning objects
 * to clusters identified by strings.
 * 
 * @author Bill McDowell
 *
 * @param <T>
 */
public abstract class Clusterer<T> {
	public abstract List<String> getClusters(T obj);
	public abstract String getName();
}
