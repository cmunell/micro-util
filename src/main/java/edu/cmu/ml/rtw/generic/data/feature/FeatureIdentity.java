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

import java.util.Iterator;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * FeatureIdentity returns a vector D(d) for double
 * extractor D applied to datum d.
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 */
public class FeatureIdentity<D extends Datum<L>, L> extends Feature<D, L> {
	protected Datum.Tools.DoubleExtractor<D, L> doubleExtractor;
	protected String[] parameterNames = {"doubleExtractor"};
	
	protected int vocabularySize;
	
	public FeatureIdentity() {
		
	}
	
	public FeatureIdentity(Context<D, L> context) {
		this.context = context;
	}
	
	@Override
	public boolean init(FeaturizedDataSet<D, L> dataSet) {
		Iterator<D> dataIter = dataSet.iterator();
		if (!dataIter.hasNext())
			return false;
		
		D datum = dataSet.iterator().next();
		this.vocabularySize = this.doubleExtractor.extract(datum).length;
		
		return true;
	}

	@Override
	public Map<Integer, Double> computeVector(D datum, int offset, Map<Integer, Double> vector) {
		double[] values = this.doubleExtractor.extract(datum);
		for (int i = 0; i < this.vocabularySize; i++)
			if (values[i] != 0)
				vector.put(i + offset, values[i]);
		return vector;
	}


	@Override
	public int getVocabularySize() {
		return this.vocabularySize;
	}


	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("doubleExtractor"))
			return Obj.stringValue((this.doubleExtractor == null) ? "" : this.doubleExtractor.toString());
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("doubleExtractor"))
			this.doubleExtractor = this.context.getDatumTools().getDoubleExtractor(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public String getVocabularyTerm(int index) {
		return String.valueOf(index);
	}

	@Override
	protected boolean setVocabularyTerm(int index, String term) {
		return true;
	}

	
	@Override
	public String getGenericName() {
		return "Identity";
	}
	
	@Override
	public Feature<D, L> makeInstance(Context<D, L> context) {
		return new FeatureIdentity<D, L>(context);
	}

	@Override
	protected <T extends Datum<Boolean>> Feature<T, Boolean> makeBinaryHelper(
			Context<T, Boolean> context, LabelIndicator<L> labelIndicator,
			Feature<T, Boolean> binaryFeature) {
		return binaryFeature;
	}

	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternalHelper(AssignmentList internalAssignments) {
		return internalAssignments;
	}
	
	@Override
	protected boolean cloneHelper(Feature<D, L> clone) {
		return true;
	}
}
