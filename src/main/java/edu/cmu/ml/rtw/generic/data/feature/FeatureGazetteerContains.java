package edu.cmu.ml.rtw.generic.data.feature;

import java.util.List;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.util.Pair;

/**
 * For datum d, string extractor S, and gazetteer G, 
 * FeatureGazetteerContains computes
 * 
 * max_{g\in G} 1(g=S(d))
 * 
 * Parameters:
 *  gazetteer - the gazetteer G over which to compute the feature
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
 */
public class FeatureGazetteerContains<D extends Datum<L>, L> extends FeatureGazetteer<D, L> { 
	
	public FeatureGazetteerContains() {
		
	}
	
	public FeatureGazetteerContains(DatumContext<D, L> context) {
		super(context);
		this.extremumType = FeatureGazetteer.ExtremumType.Maximum;
	}
	
	@Override
	protected Pair<List<Pair<String,Double>>, Double> computeExtremum(String str) {
		if (this.gazetteer.contains(str)) {
			return new Pair<List<Pair<String,Double>>, Double>(this.gazetteer.getWeightedIds(str), 1.0);
		} else { 
			return new Pair<List<Pair<String,Double>>, Double>(this.gazetteer.getWeightedIds(str), 0.0);
		}
	}
	
	@Override
	public String getGenericName() {
		return "GazetteerContains";
	}
	
	@Override
	public Feature<D, L> makeInstance(DatumContext<D, L> context) {
		return new FeatureGazetteerContains<D, L>(context);
	}
}
