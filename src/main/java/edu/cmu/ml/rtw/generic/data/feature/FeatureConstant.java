package edu.cmu.ml.rtw.generic.data.feature;

import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class FeatureConstant<D extends Datum<L>, L> extends Feature<D, L> {	

	protected double value = 1.0;
	protected String[] parameterNames = { "value" };
	
	public FeatureConstant() {
		
	}
	
	public FeatureConstant(DatumContext<D, L> context) {
		this.context = context;
	}
	
	@Override
	public boolean init(DataSet<D, L> dataSet) {
		return true;
	}

	@Override
	public Map<Integer, Double> computeVector(D datum, int offset, Map<Integer, Double> vector) {
		vector.put(offset, this.value);
		return vector;
	}

	public Integer getVocabularyIndex(String term) {
		return 0;
	}
	
	@Override
	public String getVocabularyTerm(int index) {
		return "c";
	}

	@Override
	protected boolean setVocabularyTerm(int index, String term) {
		return true;
	}

	@Override
	public int getVocabularySize() {
		return 1;
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("value")) {
			return Obj.stringValue(String.valueOf(this.value));
		}
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("value")) {
			this.value = Double.valueOf(this.context.getMatchValue(parameterValue));
		} else
			return false;
		return true;
	}
	
	@Override
	protected <T extends Datum<Boolean>> Feature<T, Boolean> makeBinaryHelper(
			DatumContext<T, Boolean> context, LabelIndicator<L> labelIndicator,
			Feature<T, Boolean> binaryFeature) {
		return binaryFeature;
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
	public Feature<D, L> makeInstance(DatumContext<D, L> context) {
		return new FeatureConstant<D, L>(context);	
	}

	@Override
	public String getGenericName() {
		return "Constant";
	}

	@Override
	protected boolean cloneHelper(Feature<D, L> clone) {
		return true;
	}
}
