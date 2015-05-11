package edu.cmu.ml.rtw.generic.cluster;

import java.util.List;

public abstract class Clusterer<T> {
	public abstract List<String> getClusters(T obj);
	public abstract String getName();
}
