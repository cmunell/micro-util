package edu.cmu.ml.rtw.generic.data.feature;

import java.util.Arrays;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.StringUtil;

/**
 * For datum d, string extractor S, and gazetteer G, 
 * FeatureGazetteerInitialism computes
 * 
 * max_{g\in G} 1(S(d) is an initialism for g)
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
 *  allowPrefix - determines whether S(d) must be a full initialism, or just a 
 *  partial (prefix) initialism.
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 *
 */
public class FeatureGazetteerInitialism<D extends Datum<L>, L> extends FeatureGazetteer<D, L> {
	private DataTools.StringPairMeasure initialismMeasure;
	private boolean allowPrefix;
	
	public FeatureGazetteerInitialism() {
		
	}
	
	public FeatureGazetteerInitialism(Context<D, L> context) {
		super(context);
		
		this.extremumType = FeatureGazetteer.ExtremumType.Maximum;
		
		this.initialismMeasure = new DataTools.StringPairMeasure() {
			public double compute(String str1, String str2) {
				if (StringUtil.isInitialism(str1, str2, allowPrefix))
					return 1.0;
				else
					return 0.0;
			}
		};
		
		this.allowPrefix = false;
		
		this.parameterNames = Arrays.copyOf(this.parameterNames, this.parameterNames.length + 1);
		this.parameterNames[this.parameterNames.length - 1] = "allowPrefix";
	}
	
	@Override
	protected Pair<List<Pair<String,Double>>, Double> computeExtremum(String str) {
		return this.gazetteer.max(str, this.initialismMeasure);
	}

	@Override
	public String getGenericName() {
		return "GazetteerInitialism";
	}

	@Override
	public Feature<D, L> makeInstance(Context<D, L> context) {
		return new FeatureGazetteerInitialism<D, L>(context);
	}
	
	@Override
	public Obj getParameterValue(String parameter) {
		Obj parameterValue = super.getParameterValue(parameter);
		if (parameterValue != null)
			return parameterValue;
		else if (parameter.equals("allowPrefix"))
			return Obj.stringValue(String.valueOf(this.allowPrefix));
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (super.setParameterValue(parameter, parameterValue))
			return true;
		else if (parameter.equals("allowPrefix"))
			this.allowPrefix = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		
		return true;
	}
}
