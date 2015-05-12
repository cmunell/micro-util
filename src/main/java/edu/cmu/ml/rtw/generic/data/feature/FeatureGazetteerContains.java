package edu.cmu.ml.rtw.generic.data.feature;

import java.util.List;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.util.Pair;

/**
 * For datum d, string extractor S, and gazetteer G, 
 * FeatureGazetteerContains computes
 * 
 * max_{g\in G} 1(g=S(d))
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 */
public class FeatureGazetteerContains<D extends Datum<L>, L> extends FeatureGazetteer<D, L> { 
	
	public FeatureGazetteerContains() {
		
	}
	
	public FeatureGazetteerContains(Context<D, L> context) {
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
	public Feature<D, L> makeInstance(Context<D, L> context) {
		return new FeatureGazetteerContains<D, L>(context);
	}
}
