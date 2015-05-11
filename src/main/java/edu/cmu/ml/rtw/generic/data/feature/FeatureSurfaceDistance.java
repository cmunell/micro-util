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

package edu.cmu.ml.rtw.generic.data.feature;

import java.util.Map;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.BidirectionalLookupTable;
import edu.cmu.ml.rtw.generic.util.CounterTable;

/**
 * 
 * @author Jesse Dodge
 *
 * @param <D>
 * @param <L>
 */
public class FeatureSurfaceDistance<D extends Datum<L>, L> extends Feature<D, L>{
	protected BidirectionalLookupTable<String, Integer> vocabulary;

	protected Datum.Tools.TokenSpanExtractor<D, L> sourceTokenExtractor;
	protected Datum.Tools.TokenSpanExtractor<D, L> targetTokenExtractor;
	protected String[] parameterNames = {"sourceTokenExtractor", "targetTokenExtractor", "PoS"};
	
	public FeatureSurfaceDistance() {
		
	}
	
	public FeatureSurfaceDistance(Context<D, L> context) {
		this.vocabulary = new BidirectionalLookupTable<String, Integer>();
		this.context = context;
	}

	@Override
	public boolean init(FeaturizedDataSet<D, L> dataSet) {
		CounterTable<String> counter = new CounterTable<String>();
		
		for (D datum : dataSet) {
			counter.incrementCount(findDistance(datum));
		}
		this.vocabulary = new BidirectionalLookupTable<String, Integer>(counter.buildIndex());
		return true;
	}
	
	private String findDistance(D datum){
		TokenSpan source = sourceTokenExtractor.extract(datum)[0];
		TokenSpan target = targetTokenExtractor.extract(datum)[0];
		return "" + (target.getStartTokenIndex() - source.getStartTokenIndex());
	}

	@Override
	public Map<Integer, Double> computeVector(D datum, int offset, Map<Integer, Double> vector) {
		vector.put(vocabulary.get(findDistance(datum)) + offset, 1.0);
		return vector;
	}

	@Override
	public String getGenericName() {
		return "surfaceDistance";
	}

	@Override
	public int getVocabularySize() {
		return vocabulary.size();
	}

	@Override
	public String getVocabularyTerm(int index) {
		return "" + vocabulary.reverseGet(index);
	}

	@Override
	protected boolean setVocabularyTerm(int index, String term) {
		vocabulary.put(term, index);
		return true;
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("sourceTokenExtractor")) 
			return Obj.stringValue((this.sourceTokenExtractor == null) ? "" : this.sourceTokenExtractor.toString());
		else if (parameter.equals("targetTokenExtractor"))
			return Obj.stringValue((this.targetTokenExtractor == null) ? "" : this.targetTokenExtractor.toString());
		return null;	}

	@Override
	public boolean setParameterValue(String parameter,
			Obj parameterValue) {
		if (parameter.equals("sourceTokenExtractor")) 
			this.sourceTokenExtractor = this.context.getDatumTools().getTokenSpanExtractor(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("targetTokenExtractor")) 
			this.targetTokenExtractor = this.context.getDatumTools().getTokenSpanExtractor(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;

	}

	@Override
	public Feature<D, L> makeInstance(Context<D, L> context) {
		return new FeatureSurfaceDistance<D,L>(context);
	}

	@Override
	protected <T extends Datum<Boolean>> Feature<T, Boolean> makeBinaryHelper(
			Context<T, Boolean> context, LabelIndicator<L> labelIndicator,
			Feature<T, Boolean> binaryFeature) {
		FeatureSurfaceDistance<T, Boolean> binaryFeatureSurf = (FeatureSurfaceDistance<T, Boolean>)binaryFeature;
		
		binaryFeatureSurf.vocabulary = this.vocabulary;
		
		return binaryFeatureSurf;
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
		FeatureSurfaceDistance<D, L> cloneSurf = (FeatureSurfaceDistance<D, L>)clone;
		cloneSurf.vocabulary = this.vocabulary;
		return true;
	}
}
