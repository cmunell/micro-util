package edu.cmu.ml.rtw.generic.cluster;

import java.util.List;

import edu.cmu.ml.rtw.generic.str.StringTransform;

/**
 * ClustererString represents a method for clustering
 * strings.
 * 
 * @author Bill McDowell
 *
 */
public abstract class ClustererString extends Clusterer<String> {
	protected StringTransform cleanFn;
	
	public ClustererString(StringTransform cleanFn) {
		this.cleanFn = cleanFn;
	}
	
	@Override
	public List<String> getClusters(String obj) {
		if (this.cleanFn != null)
			obj = this.cleanFn.transform(obj);
		return getClustersHelper(obj);
	}
	
	protected abstract List<String> getClustersHelper(String obj);
}
