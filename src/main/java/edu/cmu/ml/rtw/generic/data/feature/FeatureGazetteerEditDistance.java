package edu.cmu.ml.rtw.generic.data.feature;


import java.util.List;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.StringUtil;

/**
 * For datum d, string extractor S, and gazetteer G, 
 * FeatureGazetteerEditDistance computes
 * 
 * min_{g\in G} E(g, S(d))
 * 
 * Where E is measures the normalized edit-distance 
 * between g and S(d).
 * 
 * Parameters:
 * 	gazetteer - the gazetteer G over which to compute the feature
 * 
 *  stringExtractor - the string extractor S to extract strings from data
 *
 *  includeIds - indicates whether the computed feature vector should contain
 *  a separate component for each id in the gazetteer corresponding to the input
 *  string
 *
 *  includeWeights - indicates whether the computed feature vector should 
 *  multiply the id components of the returned vector by their weights in the
 *  gazetteer G (assuming includeIds=true)
 * 
 *  weightThreshold - the minimum weight threshold necessary for a component
 *  of the returned vector to be non-zero
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 * 
 */
public class FeatureGazetteerEditDistance<D extends Datum<L>, L> extends FeatureGazetteer<D, L> {
	private DataTools.StringPairMeasure editDistanceMeasure;
	
	public FeatureGazetteerEditDistance() {
		
	}
	
	public FeatureGazetteerEditDistance(Context<D, L> context) {
		super(context);
		
		this.extremumType = FeatureGazetteer.ExtremumType.Minimum;
		
		this.editDistanceMeasure = new DataTools.StringPairMeasure() {
			public double compute(String str1, String str2) {
				return StringUtil.levenshteinDistance(str1, str2)/((double)(str1.length()+str2.length()));
			}
		};
	}
	
	@Override
	protected Pair<List<Pair<String,Double>>, Double> computeExtremum(String str) {
		return this.gazetteer.min(str, this.editDistanceMeasure);
	}

	@Override
	public String getGenericName() {
		return "GazetteerEditDistance";
	}

	@Override
	public Feature<D, L> makeInstance(Context<D, L> context) {
		return new FeatureGazetteerEditDistance<D, L>(context);
	}
}
