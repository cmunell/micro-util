package edu.cmu.ml.rtw.generic.data.feature;
/**
 * Copyright 2014 Bill McDowell 
 *
 * This file is part of theMess (https://github.com/forkunited/theMess)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.Gazetteer;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.BidirectionalLookupTable;
import edu.cmu.ml.rtw.generic.util.CounterTable;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;

/**
 * FeatureGazetteer computes gazetteer features.  For a datum d, 
 * the feature computes the maximum or
 * minimum of a function f_{S(d)}:G ->R where G is a gazetteer of names,
 * and f_{S(d)} is a function defined for string extractor S used on
 * datum d.  For examples, see
 * the feature types that extend this class under the edu.cmu.ml.rtw.generic.data.feature
 * package.
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 * 
 */
public abstract class FeatureGazetteer<D extends Datum<L>, L> extends Feature<D, L> {
	/**
	 * ExtremumType determines whether the minimum or maximum
	 * of the appropriate function should be computed
	 *
	 */
	protected enum ExtremumType {
		Minimum,
		Maximum
	}

	protected FeatureGazetteer.ExtremumType extremumType;
	protected BidirectionalLookupTable<String, Integer> vocabulary;
	
	protected Gazetteer gazetteer;
	protected Datum.Tools.StringExtractor<D, L> stringExtractor;
	protected boolean includeIds;
	protected boolean includeWeights;
	protected double weightThreshold;
	
	protected String[] parameterNames = {"gazetteer", "stringExtractor", "includeIds", "includeWeights", "weightThreshold"};
	
	protected abstract Pair<List<Pair<String,Double>>, Double> computeExtremum(String str);
	
	public FeatureGazetteer() {
		
	}
	
	public FeatureGazetteer(Context<D, L> context) {
		this.vocabulary = new BidirectionalLookupTable<String, Integer>(); 
		this.context = context;
	}
	
	@Override
	public boolean init(FeaturizedDataSet<D, L> dataSet) {
		if (!this.includeIds)
			return true;
		
		final CounterTable<String> counter = new CounterTable<String>();
		dataSet.map(new ThreadMapper.Fn<D, Boolean>() {
			@Override
			public Boolean apply(D datum) {
				Pair<List<Pair<String,Double>>, Double> extremum = computeExtremum(datum);
				if (extremum.getFirst() == null)
					return true;
				for (Pair<String, Double> id : extremum.getFirst())
					counter.incrementCount(id.getFirst());
				return true;
			}
		});
		
		this.vocabulary = new BidirectionalLookupTable<String, Integer>(counter.buildIndex());
		
		return true;
	}
	
	
	@Override
	public Map<Integer, Double> computeVector(D datum, int offset, Map<Integer, Double> vector) {
		Pair<List<Pair<String,Double>>, Double> extremum = computeExtremum(datum);
		if (this.includeIds) {
			if (extremum.getFirst() == null) {
				return new HashMap<Integer, Double>();
			}
			
			vector = new HashMap<Integer, Double>(extremum.getFirst().size());
			for (Pair<String, Double> id : extremum.getFirst()) {
				if (!this.vocabulary.containsKey(id.getFirst()))
					continue;
				int index = this.vocabulary.get(id.getFirst());
				if (this.includeWeights && id.getSecond() >= this.weightThreshold) {
					vector.put(index + offset, extremum.getSecond()*id.getSecond());
				} else {
					vector.put(index + offset, extremum.getSecond());
				}
			}
			
		} else {
			vector = new HashMap<Integer, Double>(1);
			vector.put(offset, extremum.getSecond());
		}
		
		return vector;
	}

	
	protected Pair<List<Pair<String,Double>>, Double> computeExtremum(D datum) {
		String[] strs = this.stringExtractor.extract(datum);
		Pair<List<Pair<String,Double>>, Double> extremum = new Pair<List<Pair<String,Double>>, Double>(
				null,
				(this.extremumType == FeatureGazetteer.ExtremumType.Maximum) ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
		for (String str : strs) {
			Pair<List<Pair<String,Double>>, Double> curExtremum = computeExtremum(str);
			if ((this.extremumType == FeatureGazetteer.ExtremumType.Maximum && curExtremum.getSecond() > extremum.getSecond())
					|| (this.extremumType == FeatureGazetteer.ExtremumType.Minimum && curExtremum.getSecond() < extremum.getSecond()))
				extremum = curExtremum;	
		}
		return extremum;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("gazetteer"))
			return Obj.stringValue((this.gazetteer == null) ? "" : this.gazetteer.getName());
		else if (parameter.equals("stringExtractor"))
			return Obj.stringValue((this.stringExtractor == null) ? "" : this.stringExtractor.toString());
		else if (parameter.equals("includeIds"))
			return Obj.stringValue(String.valueOf(this.includeIds));
		else if (parameter.equals("includeWeights"))
			return Obj.stringValue(String.valueOf(this.includeWeights));
		else if (parameter.equals("weightThreshold"))
			return Obj.stringValue(String.valueOf(this.weightThreshold));
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("gazetteer"))
			this.gazetteer = this.context.getDatumTools().getDataTools().getGazetteer(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("stringExtractor"))
			this.stringExtractor = this.context.getDatumTools().getStringExtractor(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("includeIds"))
			this.includeIds = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("includeWeights"))
			this.includeWeights = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("weightThreshold"))
			this.weightThreshold = Double.valueOf(this.context.getMatchValue(parameterValue));
		else 
			return false;
		return true;
	}
	
	@Override
	public String getVocabularyTerm(int index) {
		if (this.vocabulary == null)
			return null;
		return this.vocabulary.reverseGet(index);
	}

	@Override
	protected boolean setVocabularyTerm(int index, String term) {
		if (this.vocabulary == null)
			return true;
		this.vocabulary.put(term, index);
		return true;
	}

	@Override
	public int getVocabularySize() {
		if (this.vocabulary == null)
			return 1;
		return this.vocabulary.size();
	}
	
	@Override
	protected <T extends Datum<Boolean>> Feature<T, Boolean> makeBinaryHelper(
			Context<T, Boolean> context, LabelIndicator<L> labelIndicator,
			Feature<T, Boolean> binaryFeature) {
		FeatureGazetteer<T, Boolean> binaryFeatureGaz = (FeatureGazetteer<T, Boolean>)binaryFeature;
		
		binaryFeatureGaz.vocabulary = this.vocabulary;
		
		return binaryFeatureGaz;
	}

	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternalHelper(
			AssignmentList internalAssignments) {
		return internalAssignments;
	}
	
	@Override
	protected boolean cloneHelper(Feature<D, L> clone) {
		FeatureGazetteer<D, L> cloneGaz = (FeatureGazetteer<D, L>)clone;
		cloneGaz.vocabulary = this.vocabulary;
		return true;
	}
}
